import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';

async function login(page: Page, teacher: boolean) {
  await page.goto('/');
  await page.getByLabel('Email', { exact: true }).fill(teacher ? 'teacher@ielts.local' : 'student@ielts.local');
  await page.getByLabel('Mật khẩu', { exact: true }).fill(teacher ? 'Teacher@12345' : 'Student@12345');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Lớp học của tôi.' })).toBeVisible();
}
async function post(page: Page, url: string, data?: unknown) {
  const csrf = await (await page.request.get('/api/auth/csrf')).json();
  const response = await page.request.post(url, { data, headers: { [csrf.headerName]: csrf.token } });
  expect(response.ok()).toBeTruthy();
  return response.json();
}
test('one class receives both skills; teacher banks and student previews keep the correct content', async ({ browser }) => {
  const tc = await browser.newContext(), sc = await browser.newContext();
  const teacher = await tc.newPage(), student = await sc.newPage();
  await login(teacher, true); await login(student, false);
  const room = await post(teacher, '/api/classes', { name: 'Skills E2E ' + Date.now(), description: '', requestId: crypto.randomUUID() });
  await post(student, '/api/classes/join', { code: room.joinCode });
  const speaking = (await (await teacher.request.get('/api/resources?skill=SPEAKING')).json()).find((r: { version: { resourceKind: string } }) => r.version.resourceKind === 'MAIN');
  const writing = (await (await teacher.request.get('/api/resources?skill=WRITING')).json()).find((r: { version: { resourceKind: string; content: { taskType: string } } }) => r.version.resourceKind === 'MAIN' && r.version.content.taskType === 'TASK_2');
  expect(speaking).toBeTruthy(); expect(writing).toBeTruthy();
  for (const resource of [speaking, writing]) {
    await post(teacher, '/api/resources/' + resource.id + '/versions/' + resource.version.id + '/publish');
    await post(teacher, '/api/assignments', { requestId: crypto.randomUUID(), classroomId: room.id,
      resourceVersionId: resource.version.id, purpose: 'PRACTICE', recipientMode: 'CLASS_SNAPSHOT', studentIds: [],
      opensAt: null, dueAt: null, allowLate: false, maxAttempts: 1, feedbackPolicy: 'AFTER_SUBMISSION' });
  }
  await teacher.reload();
  await teacher.getByRole('button', { name: 'Bài Speaking', exact: true }).click();
  await expect(teacher.getByRole('heading', { name: speaking.version.title, exact: true })).toBeVisible();
  await expect(teacher.getByRole('heading', { name: writing.version.title, exact: true })).toHaveCount(0);
  await teacher.getByRole('button', { name: 'Bài Writing', exact: true }).click();
  await expect(teacher.getByRole('heading', { name: writing.version.title, exact: true })).toBeVisible();
  await expect(teacher.getByRole('heading', { name: speaking.version.title, exact: true })).toHaveCount(0);
  await student.getByRole('button', { name: 'Bài được giao', exact: true }).click();
  const speakingCard = student.locator('.learning-card').filter({ has: student.getByRole('heading', { name: speaking.version.title, exact: true }), hasText: room.name });
  const writingCard = student.locator('.learning-card').filter({ has: student.getByRole('heading', { name: writing.version.title, exact: true }), hasText: room.name });
  await expect(speakingCard).toContainText('SPEAKING'); await expect(writingCard).toContainText('WRITING');
  await student.getByRole('combobox', { name: 'Kỹ năng', exact: true }).selectOption('SPEAKING');
  await expect(writingCard).toHaveCount(0);
  await speakingCard.getByRole('button', { name: 'Xem đề bài' }).click();
  await expect(student.getByText(speaking.version.content.prompt, { exact: true })).toBeVisible();
  await expect(student.getByRole('heading', { name: 'Tham khảo riêng cho giảng viên', exact: true })).toHaveCount(0);
  await student.getByRole('button', { name: 'Quay lại', exact: true }).click();
  await student.getByRole('combobox', { name: 'Kỹ năng', exact: true }).selectOption('WRITING');
  await expect(speakingCard).toHaveCount(0);
  await writingCard.getByRole('button', { name: 'Xem đề bài' }).click();
  await expect(student.getByText(writing.version.content.prompt, { exact: true })).toBeVisible();
  await expect(student.getByRole('heading', { name: 'Tham khảo riêng cho giảng viên', exact: true })).toHaveCount(0);
  await student.getByRole('button', { name: 'Quay lại', exact: true }).click();
  await student.getByRole('combobox', { name: 'Kỹ năng', exact: true }).selectOption('ALL');
  await expect(speakingCard).toBeVisible(); await expect(writingCard).toBeVisible();
  await student.setViewportSize({ width: 390, height: 844 });
  expect(await student.evaluate(() => document.documentElement.scrollWidth > innerWidth)).toBe(false);
  await student.screenshot({ path: 'test-results/combined-student-assigned.png', fullPage: true });
  await tc.close(); await sc.close();
});
