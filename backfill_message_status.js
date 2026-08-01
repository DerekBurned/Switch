/**
 * One-time backfill: rewrites any Firestore message documents that still carry
 * the legacy "SENDING" or "DELIVERED" status to "SENT".
 *
 * Run with:  node backfill_message_status.js
 * Requires the Firebase CLI to be logged in (firebase login).
 */

const https = require('https');
const fs    = require('fs');
const path  = require('path');

const PROJECT_ID = 'messenger-eb6b0';
const DATABASE   = '(default)';
const BASE       = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/${DATABASE}`;

const credsPath = path.join(
  process.env.HOME || process.env.USERPROFILE || '',
  '.config/configstore/firebase-tools.json',
);
const creds = JSON.parse(fs.readFileSync(credsPath, 'utf8'));
const TOKEN = creds.tokens?.access_token;
if (!TOKEN) { console.error('No access_token — run: firebase login'); process.exit(1); }

function req(method, url, body) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const r = https.request(
      { hostname: u.hostname, path: u.pathname + u.search, method,
        headers: { Authorization: `Bearer ${TOKEN}`, 'Content-Type': 'application/json' } },
      res => { let d = ''; res.on('data', c => d += c); res.on('end', () => { try { resolve(JSON.parse(d)); } catch { resolve(d); } }); }
    );
    r.on('error', reject);
    if (body) r.write(JSON.stringify(body));
    r.end();
  });
}

async function listDocuments(collectionPath) {
  const docs = [];
  let pageToken = null;
  do {
    const url = `${BASE}/documents/${collectionPath}?pageSize=300${pageToken ? '&pageToken=' + pageToken : ''}`;
    const res = await req('GET', url);
    if (res.error) throw new Error(`List ${collectionPath}: ${res.error.message}`);
    (res.documents || []).forEach(d => docs.push(d));
    pageToken = res.nextPageToken || null;
  } while (pageToken);
  return docs;
}

async function queryMessages(conversationId, status) {
  // Parent document goes in the URL; collectionId selects the subcollection.
  const url = `${BASE}/documents/conversations/${conversationId}:runQuery`;
  const body = {
    structuredQuery: {
      from: [{ collectionId: 'messages' }],
      where: { fieldFilter: { field: { fieldPath: 'status' }, op: 'EQUAL', value: { stringValue: status } } },
    },
  };
  const res = await req('POST', url, body);
  return Array.isArray(res) ? res.filter(r => r.document) : [];
}

async function main() {
  console.log('Listing conversations...');
  const conversations = await listDocuments('conversations');
  console.log(`Found ${conversations.length} conversation(s).`);

  let total = 0;
  let updated = 0;

  for (const conv of conversations) {
    const convId = conv.name.split('/').pop();
    for (const legacyStatus of ['SENDING', 'DELIVERED']) {
      const hits = await queryMessages(convId, legacyStatus);
      total += hits.length;
      for (const { document } of hits) {
        const patchUrl = `https://firestore.googleapis.com/v1/${document.name}?updateMask.fieldPaths=status`;
        const result = await req('PATCH', patchUrl, { fields: { status: { stringValue: 'SENT' } } });
        if (result.error) {
          console.error(`\nFailed to patch ${document.name}: ${result.error.message}`);
        } else {
          updated++;
          process.stdout.write(`\r  Updated ${updated} document(s)...`);
        }
      }
    }
  }

  if (total === 0) {
    console.log('Nothing to fix — all messages already have valid status values.');
  } else {
    console.log(`\nDone. Updated ${updated}/${total} document(s).`);
  }
}

main().catch(err => { console.error(err); process.exit(1); });
