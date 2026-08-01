 

import * as admin from "firebase-admin";
import {logger} from "firebase-functions";
import {setGlobalOptions} from "firebase-functions/v2";
import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {onValueCreated} from "firebase-functions/v2/database";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {defineSecret} from "firebase-functions/params";
import {RtcTokenBuilder, RtcRole} from "agora-token";

admin.initializeApp();

setGlobalOptions({region: "europe-west1"});

const db = admin.firestore();
const messaging = admin.messaging();

interface ConversationDoc {
  participantIds?: string[];
  participantNames?: string[];
}

interface MessageDoc {
  senderId?: string;
  text?: string;
  timestamp?: number;
  type?: string;
  enc?: number;
  ciphertext?: string;
  nonce?: string;
  senderEpoch?: number;
  recipientEpoch?: number;
  recipientId?: string;
}

interface UserDoc {
  username?: string;
  fcmToken?: string;
  avatarUrl?: string;
}

export const onMessageCreated = onDocumentCreated(
  "conversations/{conversationId}/messages/{messageId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const message = snapshot.data() as MessageDoc;
    const conversationId = event.params.conversationId;
    const messageId = event.params.messageId;
    const senderId = message.senderId ?? "";
    if (!senderId) {
      logger.warn("Skipping message with no senderId", {conversationId, messageId});
      return;
    }

    const convRef = db.collection("conversations").doc(conversationId);
    const convSnap = await convRef.get();
    const conversation = (convSnap.data() ?? {}) as ConversationDoc;
    const recipients = (conversation.participantIds ?? []).filter((id) => id !== senderId);
    if (recipients.length === 0) return;

    const updates: {[key: string]: admin.firestore.FieldValue} = {};
    recipients.forEach((uid) => {
      updates[`unreadCounts.${uid}`] = admin.firestore.FieldValue.increment(1);
    });
    const allParticipants = conversation.participantIds ?? [];
    if (allParticipants.length > 0) {
      updates["visibleTo"] = admin.firestore.FieldValue.arrayUnion(...allParticipants);
    }
    await convRef.update(updates);

    const [senderSnap, ...recipientSnaps] = await Promise.all([
      db.collection("users").doc(senderId).get(),
      ...recipients.map((uid) => db.collection("users").doc(uid).get()),
    ]);
    const sender = (senderSnap.data() ?? {}) as UserDoc;
    const senderName = sender.username ?? "New message";
    const senderAvatar = sender.avatarUrl ?? "";

    const tokens = recipientSnaps
      .map((s) => (s.data() as UserDoc | undefined)?.fcmToken)
      .filter((t): t is string => typeof t === "string" && t.length > 0);
    if (tokens.length === 0) {
      logger.info("No FCM tokens for recipients", {conversationId, recipients});
      return;
    }

    const encrypted = message.enc === 1;
    const preview = encrypted ? "" : (message.text ?? "").slice(0, 200);
    const timestamp = (message.timestamp ?? Date.now()).toString();

    const data: {[key: string]: string} = {
      type: "message",
      conversationId,
      messageId,
      senderId,
      senderName,
      senderAvatar,
      preview,
      timestamp,
      msgType: message.type ?? "TEXT",
    };
    if (encrypted) {
      data.enc = "1";
      data.senderEpoch = String(message.senderEpoch ?? 0);
      data.recipientEpoch = String(message.recipientEpoch ?? 0);
      data.recipientId = message.recipientId ?? "";
      const ciphertext = message.ciphertext ?? "";
      if (ciphertext.length > 0 && ciphertext.length <= 2800) {
        data.ciphertext = ciphertext;
        data.nonce = message.nonce ?? "";
      }
    }

    const response = await messaging.sendEachForMulticast({
      tokens,
      data,
      android: {priority: "high"},
    });

    if (response.failureCount > 0) {
      response.responses.forEach((r, i) => {
        if (!r.success) {
          logger.warn("FCM send failed", {token: tokens[i], error: r.error?.message});
        }
      });
    }
  },
);

interface CallSignalDoc {
  callId?: string;
  callerId?: string;
  calleeId?: string;
  channelName?: string;
  status?: string;
}

export const onCallSignalCreated = onValueCreated(
  "/calls/{calleeId}/{callId}",
  async (event) => {
    const signal = (event.data.val() ?? {}) as CallSignalDoc;
    if (signal.status !== "RINGING") return;
    const calleeId = event.params.calleeId;
    const callId = signal.callId ?? event.params.callId;
    const callerId = signal.callerId ?? "";
    const channelName = signal.channelName ?? "";
    if (!calleeId || !callId || !channelName) return;

    const [calleeSnap, callerSnap] = await Promise.all([
      db.collection("users").doc(calleeId).get(),
      callerId ? db.collection("users").doc(callerId).get() : Promise.resolve(null),
    ]);
    const callee = (calleeSnap.data() ?? {}) as UserDoc;
    const token = callee.fcmToken;
    if (!token) {
      logger.info("No FCM token for callee — cannot wake device", {calleeId});
      return;
    }
    const callerName = (callerSnap?.data() as UserDoc | undefined)?.username ?? callerId;

    await messaging.send({
      token,
      data: {
        type: "call",
        callId,
        callerId,
        callerName,
        channelName,
      },
      android: {priority: "high"},
    });
  },
);

const agoraAppId = defineSecret("AGORA_APP_ID");
const agoraAppCertificate = defineSecret("AGORA_APP_CERTIFICATE");

const RTC_TOKEN_TTL_SECONDS = 3600;

const CRC32_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    table[n] = c >>> 0;
  }
  return table;
})();

function agoraUidFromUserId(userId: string): number {
  const bytes = Buffer.from(userId, "utf8");
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc = CRC32_TABLE[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  }
  crc = (crc ^ 0xffffffff) >>> 0;
  return crc & 0x7fffffff;
}

export const getRtcToken = onCall(
  {secrets: [agoraAppId, agoraAppCertificate]},
  (request) => {
    const userId = request.auth?.uid;
    if (!userId) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }
    const channel = request.data?.channel;
    if (typeof channel !== "string" || !/^call-[0-9a-fA-F-]{36}$/.test(channel)) {
      throw new HttpsError("invalid-argument", "Bad channel name.");
    }
    const appId = agoraAppId.value();
    const certificate = agoraAppCertificate.value();
    if (!appId || !certificate) {
      throw new HttpsError("failed-precondition", "RTC token service not configured.");
    }
    const uid = agoraUidFromUserId(userId);
    const token = RtcTokenBuilder.buildTokenWithUid(
      appId,
      certificate,
      channel,
      uid,
      RtcRole.PUBLISHER,
      RTC_TOKEN_TTL_SECONDS,
      RTC_TOKEN_TTL_SECONDS,
    );
    logger.info("getRtcToken issued", {channel, uid});
    return {token, uid, expiresInSeconds: RTC_TOKEN_TTL_SECONDS};
  },
);
