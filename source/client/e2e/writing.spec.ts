import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import path from 'node:path';

async function login(page: Page, teacher: boolean) {
  await page.goto('/');
  await page.getByLabel('Email', { exact: true }).fill(teacher
    ? process.env.E2E_TEACHER_EMAIL ?? 'teacher@ielts.local' : process.env.E2E_STUDENT_EMAIL ?? 'student@ielts.local');
  await page.getByLabel('Mật khẩu', { exact: true }).fill(teacher
    ? process.env.E2E_TEACHER_PASSWORD ?? 'Teacher@12345' : process.env.E2E_STUDENT_PASSWORD ?? 'Student@12345');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Lớp học của tôi.' })).toBeVisible();
}
async function post(page: Page, url: string, data: unknown) {
  const csrf = await (await page.request.get('/api/auth/csrf')).json();
  const response = await page.request.post(url, { data, headers: { [csrf.headerName]: csrf.token } });
  expect(response.ok()).toBeTruthy();
  return response.json();
}
async function fixture(teacher: Page, student: Page) {
  const room = await post(teacher, '/api/classes', { name: 'Writing E2E ' + Date.now(), description: '', requestId: crypto.randomUUID() });
  await post(student, '/api/classes/join', { code: room.joinCode });
  await teacher.reload();
  return room;
}
function card(page: Page, title: string) {
  return page.locator('.learning-card').filter({ has: page.getByRole('heading', { name: title, exact: true }) });
}
async function assign(page: Page, title: string, room: { id: number }, selected = false) {
  await card(page, title).getByRole('button', { name: 'Giao bài', exact: true }).click();
  await page.getByLabel('Lớp nhận bài').selectOption(String(room.id));
  if (selected) {
    await page.getByRole('combobox', { name: 'Người nhận', exact: true }).selectOption('SELECTED');
    await page.getByRole('checkbox', { name: /Trần Hoàng Nam/ }).check();
  }
  await page.getByRole('button', { name: 'Xác nhận giao bài' }).click();
  await expect(page.getByRole('status')).toContainText('Đã giao bài');
}
async function openStudent(page: Page, title: string) {
  await page.getByRole('button', { name: 'Bài được giao', exact: true }).click();
  await card(page, title).getByRole('button', { name: 'Xem đề bài' }).click();
}

test('Task 2 author, preview, publish, assign and immutable version without private model', async ({ browser }) => {
  const tc = await browser.newContext(), sc = await browser.newContext();
  const teacher = await tc.newPage(), student = await sc.newPage();
  await login(teacher, true); await login(student, false);
  const room = await fixture(teacher, student), title = 'Libraries and parks ' + Date.now();
  const prompt = 'Cities should invest in public libraries. To what extent do you agree?';
  await teacher.getByRole('button', { name: 'Bài Writing', exact: true }).click();
  await teacher.getByRole('button', { name: 'Tạo bài Writing', exact: true }).click();
  await teacher.getByLabel('Tên bài', { exact: true }).fill(title);
  await teacher.getByLabel('Đề bài / yêu cầu').fill(prompt);
  await teacher.getByLabel('Checklist cần đạt').fill('State a clear position.\nSupport the main ideas.');
  await teacher.getByLabel('Bài viết mẫu / phương án tham khảo').fill('PRIVATE_WRITING_MODEL');
  await teacher.getByLabel('Ghi chú riêng của giảng viên').fill('PRIVATE_WRITING_NOTE');
  await teacher.getByRole('button', { name: 'Lưu bản nháp' }).click();
  await expect(card(teacher, title)).toContainText('Bản nháp');
  await card(teacher, title).getByRole('button', { name: 'Xem trước' }).click();
  await expect(teacher.getByText(prompt, { exact: true })).toBeVisible();
  await expect(teacher.getByText('PRIVATE_WRITING_MODEL', { exact: true })).toBeVisible();
  await expect(teacher.getByRole('listitem').filter({ hasText: 'Support the main ideas.' })).toBeVisible();
  await teacher.getByRole('button', { name: 'Quay lại', exact: true }).click();
  await card(teacher, title).getByRole('button', { name: 'Xuất bản' }).click();
  await expect(card(teacher, title)).toContainText('Đã xuất bản');
  await assign(teacher, title, room);
  await openStudent(student, title);
  await expect(student.getByText(prompt, { exact: true })).toBeVisible();
  await expect(student.getByText('PRIVATE_WRITING_MODEL')).toHaveCount(0);
  await expect(student.getByText('PRIVATE_WRITING_NOTE')).toHaveCount(0);
  await teacher.getByRole('button', { name: 'Ngân hàng bài', exact: true }).click();
  await card(teacher, title).getByRole('button', { name: 'Chỉnh sửa', exact: true }).click();
  await teacher.getByLabel('Tên bài', { exact: true }).fill(title + ' revised');
  await teacher.getByLabel('Đề bài / yêu cầu').fill('NEW_WRITING_PROMPT_VERSION_TWO');
  await teacher.getByRole('button', { name: 'Lưu bản nháp' }).click();
  await expect(card(teacher, title + ' revised')).toContainText('Phiên bản 2');
  await card(teacher, title + ' revised').getByRole('button', { name: 'Xuất bản' }).click();
  await student.reload();
  await openStudent(student, title);
  await expect(student.getByText(prompt, { exact: true })).toBeVisible();
  await expect(student.getByText('NEW_WRITING_PROMPT_VERSION_TWO')).toHaveCount(0);
  await teacher.getByRole('button', { name: 'Bài Writing', exact: true }).click();
  await card(teacher, title + ' revised').getByRole('button', { name: 'Xem trước' }).click();
  await teacher.screenshot({ path: 'test-results/writing-teacher-desktop.png', fullPage: true });
  await tc.close(); await sc.close();
});

test('Task 1 protected PNG upload, selected recipient and decoded chart', async ({ browser }) => {
  const tc = await browser.newContext(), sc = await browser.newContext();
  const teacher = await tc.newPage(), student = await sc.newPage();
  await login(teacher, true); await login(student, false);
  const room = await fixture(teacher, student), title = 'Transport chart ' + Date.now();
  await teacher.getByRole('button', { name: 'Bài Writing', exact: true }).click();
  await teacher.getByRole('button', { name: 'Tạo bài Writing', exact: true }).click();
  await teacher.getByLabel('Tên bài', { exact: true }).fill(title);
  await teacher.getByRole('combobox', { name: 'Nhiệm vụ', exact: true }).selectOption('TASK_1');
  await expect(teacher.getByLabel('Số từ tối thiểu')).toHaveValue('150');
  await teacher.getByLabel('Đề bài / yêu cầu').fill('Summarise the main changes in transport to work in Riverton.');
  await teacher.getByLabel('Hình đề bài (PNG/JPEG, tối đa 5 MB)').setInputFiles(path.resolve('../server/src/main/resources/demo/writing-transport.png'));
  await expect(teacher.getByRole('img', { name: 'Hình đề bài Task 1' })).toHaveAttribute('src', /\/api\/assets\/\d+/);
  await teacher.getByLabel('Đơn vị dữ liệu').fill('% of commuters');
  await teacher.getByLabel('Nhóm so sánh').fill('Bus\nBicycle\nCar\nWalking');
  await teacher.getByRole('checkbox', { name: 'Có diễn biến theo thời gian' }).check();
  await teacher.getByLabel('Mốc thời gian').fill('2010\n2020');
  await teacher.getByRole('button', { name: 'Lưu bản nháp' }).click();
  await expect(card(teacher, title)).toBeVisible();
  await card(teacher, title).getByRole('button', { name: 'Xuất bản' }).click();
  await expect(card(teacher, title)).toContainText('Đã xuất bản');
  await assign(teacher, title, room, true);
  await openStudent(student, title);
  const chart = student.getByRole('img', { name: 'Hình đề bài Task 1' });
  await expect.poll(() => chart.evaluate((img: HTMLImageElement) => img.naturalWidth)).toBe(900);
  await expect(student.getByText(/Tối thiểu 150 từ/)).toBeVisible();
  await student.screenshot({ path: 'test-results/writing-student-chart.png', fullPage: true });
  await tc.close(); await sc.close();
});

test('idea linking guided step and transfer preserve teacher-only feedback', async ({ browser }) => {
  const tc = await browser.newContext(), sc = await browser.newContext();
  const teacher = await tc.newPage(), student = await sc.newPage();
  await login(teacher, true); await login(student, false);
  const room = await fixture(teacher, student), title = 'Link ideas ' + Date.now();
  await teacher.getByRole('button', { name: 'Bài Writing', exact: true }).click();
  await teacher.getByRole('button', { name: 'Tạo bài Writing', exact: true }).click();
  await teacher.getByLabel('Tên bài', { exact: true }).fill(title);
  await teacher.getByRole('combobox', { name: 'Nhóm bài', exact: true }).selectOption('IDEA_LINKING');
  await teacher.getByLabel('Đề bài / yêu cầu').fill('Link two ideas while preserving their logical relationship.');
  await teacher.getByLabel('Checklist cần đạt').fill('Identify the relationship before choosing a connector.');
  const guided = teacher.locator('.writing-item').filter({ has: teacher.getByRole('heading', { name: 'Hoạt động 1', exact: true }) });
  const transfer = teacher.locator('.writing-item').filter({ has: teacher.getByRole('heading', { name: 'Hoạt động 2', exact: true }) });
  await guided.getByRole('textbox', { name: 'Yêu cầu hoạt động', exact: true }).fill('Combine the ideas using a contrast.');
  await guided.getByRole('textbox', { name: 'Ngữ cảnh hoạt động', exact: true }).fill('Libraries cost money. They provide free access to books.');
  await guided.getByLabel('Ý / ghi chú').fill('Consider although.\nAvoid although plus but.');
  await guided.getByLabel('Phương án tham khảo riêng').fill('PRIVATE_ACCEPTED_EXAMPLE');
  await guided.getByLabel('Giải thích phản hồi riêng').fill('PRIVATE_EXPLANATION');
  await transfer.getByRole('textbox', { name: 'Yêu cầu hoạt động', exact: true }).fill('Transfer: link these new ideas in your own sentence.');
  await transfer.getByRole('textbox', { name: 'Ngữ cảnh hoạt động', exact: true }).fill('Buses reduce congestion. Some journeys take longer.');
  await teacher.getByRole('button', { name: 'Lưu bản nháp' }).click();
  await expect(card(teacher, title)).toBeVisible();
  await card(teacher, title).getByRole('button', { name: 'Xuất bản' }).click();
  await expect(card(teacher, title)).toContainText('Đã xuất bản');
  await assign(teacher, title, room);
  await openStudent(student, title);
  await expect(student.getByText('Combine the ideas using a contrast.', { exact: true })).toBeVisible();
  await expect(student.getByText('Transfer: link these new ideas in your own sentence.', { exact: true })).toBeVisible();
  await expect(student.getByText('Avoid although plus but.', { exact: true })).toBeVisible();
  await expect(student.getByText('PRIVATE_ACCEPTED_EXAMPLE')).toHaveCount(0);
  await expect(student.getByText('PRIVATE_EXPLANATION')).toHaveCount(0);
  await student.screenshot({ path: 'test-results/writing-student-remedial.png', fullPage: true });
  await tc.close(); await sc.close();
});

test('mobile bank and chart/remedial editor fit the screen', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, true);
  await page.getByRole('button', { name: 'Bài Writing', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Bài Writing.' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)).toBe(false);
  await page.screenshot({ path: 'test-results/writing-mobile-bank.png', fullPage: true });
  await page.getByRole('button', { name: 'Tạo bài Writing', exact: true }).click();
  await page.getByRole('combobox', { name: 'Nhiệm vụ', exact: true }).selectOption('TASK_1');
  await page.getByRole('combobox', { name: 'Nhóm bài', exact: true }).selectOption('DESCRIPTIVE_LANGUAGE');
  await page.getByRole('button', { name: 'Thêm từ / cụm' }).click();
  expect(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)).toBe(false);
});
