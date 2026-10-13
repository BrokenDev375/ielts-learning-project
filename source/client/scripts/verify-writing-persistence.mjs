import { request } from '@playwright/test';
import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';

const mode = process.argv[2];
if (!['capture', 'verify'].includes(mode)) throw new Error('Use capture or verify.');
const baseURL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5173';
const state = new URL('../.test-state/writing-persistence.json', import.meta.url);

async function session(teacher) {
  const ctx = await request.newContext({ baseURL });
  const csrf = await (await ctx.get('/api/auth/csrf')).json();
  const response = await ctx.post('/api/auth/login', {
    data: { email: teacher ? process.env.E2E_TEACHER_EMAIL ?? 'teacher@ielts.local' : process.env.E2E_STUDENT_EMAIL ?? 'student@ielts.local',
      password: teacher ? process.env.E2E_TEACHER_PASSWORD ?? 'Teacher@12345' : process.env.E2E_STUDENT_PASSWORD ?? 'Student@12345' },
    headers: { [csrf.headerName]: csrf.token },
  });
  assert.equal(response.status(), 200, 'Login must succeed after restart.');
  return ctx;
}
async function get(ctx, url) {
  const response = await ctx.get(url);
  assert.equal(response.status(), 200, url);
  return response.json();
}
const teacher = await session(true), student = await session(false);
try {
  const resources = await get(teacher, '/api/resources?skill=WRITING');
  const assignments = (await get(teacher, '/api/assignments')).filter(a => a.skill === 'WRITING');
  const received = (await get(student, '/api/assignments')).filter(a => a.skill === 'WRITING');
  assert.equal(resources.filter(r => r.version.source?.note?.startsWith('DEMO_WRITING_V1:')).length, 7, 'Seven Writing demo resources without duplicates.');
  assert.ok(received.length >= 3, 'Run browser fixtures on the disposable test database first.');
  const assets = [];
  for (const id of [...new Set(resources.map(r => r.version.content.visualAssetId).filter(Boolean))].sort((a,b) => a-b)) {
    const response = await teacher.get('/api/assets/' + id);
    assert.equal(response.status(), 200);
    assets.push({ id, sha256: createHash('sha256').update(await response.body()).digest('hex') });
  }
  const details = [];
  for (const a of received) {
    const d = await get(student, '/api/assignments/' + a.id);
    assert.equal(Object.hasOwn(d, 'teaching'), false, 'No private teaching in student responses.');
    if (d.content.visualAssetId) {
      const response = await student.get('/api/assets/' + d.content.visualAssetId);
      assert.equal(response.status(), 200, 'Assigned chart remains accessible.');
    }
    details.push(d);
  }
  const snapshot = { resources, assignments, received, details, assets };
  if (mode === 'capture') {
    await mkdir(new URL('../.test-state/', import.meta.url), { recursive: true });
    await writeFile(state, JSON.stringify(snapshot));
  } else {
    assert.deepEqual(snapshot, JSON.parse(await readFile(state, 'utf8')), 'Saved resource versions, assignments, recipients and image checksums unchanged.');
  }
  console.log(JSON.stringify({ mode, status: 'PASS', resources: resources.length, seeds: 7, assignments: assignments.length,
    received: received.length, imageChecksums: assets.length }));
} finally {
  await teacher.dispose(); await student.dispose();
}
