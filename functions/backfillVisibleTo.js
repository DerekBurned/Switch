const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();

async function run() {
  const snapshot = await db.collection("conversations").get();
  let updated = 0;
  let batch = db.batch();
  let pending = 0;

  for (const doc of snapshot.docs) {
    const data = doc.data();
    const visibleTo = data.visibleTo;
    if (Array.isArray(visibleTo) && visibleTo.length > 0) continue;
    const participantIds = data.participantIds || [];
    if (participantIds.length === 0) continue;
    batch.update(doc.ref, { visibleTo: participantIds });
    updated += 1;
    pending += 1;
    if (pending === 400) {
      await batch.commit();
      batch = db.batch();
      pending = 0;
    }
  }

  if (pending > 0) await batch.commit();
  console.log(`Backfilled visibleTo on ${updated} conversation(s).`);
}

run().then(() => process.exit(0)).catch((e) => {
  console.error(e);
  process.exit(1);
});
