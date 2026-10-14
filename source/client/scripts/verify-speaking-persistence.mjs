import { request } from '@playwright/test';
import { createHash } from 'node:crypto';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { dirname } from 'node:path';
import assert from 'node:assert/strict';

const baseURL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5173';
const stateFile = process.env.SPEAKING_STATE_FILE ?? '.test-state/speaking-persistence.json';
const mode = process.argv[2];
if (!['capture', 'verify'].includes(mode)) throw new Error('Use capture or verify');
async function login(email, password) {
  const context = await request.newContext({ baseURL });
  const csrf = await (await context.get('/api/auth/csrf')).json();
  const result = await context.post('/api/auth/login', { data: { email, password }, headers: { [csrf.headerName]: csrf.token } });
  assert.equal(result.status(), 200);
  return context;
}
const teacher = await login(process.env.E2E_TEACHER_EMAIL ?? 'teacher@ielts.local', process.env.E2E_TEACHER_PASSWORD ?? 'Teacher@12345');
const student = await login(process.env.E2E_STUDENT_EMAIL ?? 'student@ielts.local', process.env.E2E_STUDENT_PASSWORD ?? 'Student@12345');
try {
  const resourceResponse = await teacher.get('/api/resources?skill=SPEAKING'); assert.equal(resourceResponse.status(), 200);
  const resources = await resourceResponse.json();
  const seed = resources.filter(r => r.version.source?.note.startsWith('DEMO_SPEAKING_V1'));
  assert.equal(seed.length, 3, 'All three demo types exist');
  const assignmentResponse = await student.get('/api/assignments'); assert.equal(assignmentResponse.status(), 200);
  const assignments = await assignmentResponse.json();
  const audioIds = [...new Set(resources.flatMap(r => r.version.content.items.map(i => i.audioAssetId).filter(Boolean)))].sort((a, b) => a - b);
  const audio = {};
  for (const id of audioIds) {
    const response = await teacher.get('/api/assets/' + id); assert.equal(response.status(), 200);
    audio[id] = createHash('sha256').update(await response.body()).digest('hex');
  }
  const state = { resources, assignments, audio };
  if (mode === 'capture') { await mkdir(dirname(stateFile), { recursive: true }); await writeFile(stateFile, JSON.stringify(state, null, 2)); }
  else assert.deepEqual(state, JSON.parse(await readFile(stateFile, 'utf8')));
  console.log(JSON.stringify({ mode, resources: resources.length, assignments: assignments.length, audio: audioIds.length, seed: seed.length, result: 'PASS' }));
} finally { await teacher.dispose(); await student.dispose(); }
