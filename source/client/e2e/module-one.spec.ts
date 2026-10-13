import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';

async function login(page: Page, email: string, password: string) {
  await page.goto('/');
  await page.getByLabel('Email', { exact: true }).fill(email);
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Lớp học của tôi.' })).toBeVisible();
}
test('teacher creates, student joins, membership survives refresh, teacher removes member', async ({ browser }) => {
  const teacherContext = await browser.newContext();
  const studentContext = await browser.newContext();
  const teacher = await teacherContext.newPage();
  const student = await studentContext.newPage();
  const email = 'e2e-' + Date.now() + '@test.local';
  const name = 'Học viên Demo';
  const className = 'IELTS Demo ' + Date.now();
  await login(teacher, process.env.E2E_TEACHER_EMAIL ?? 'teacher@ielts.local', process.env.E2E_TEACHER_PASSWORD ?? 'Teacher@12345');
  await teacher.getByRole('button', { name: 'Tạo lớp mới', exact: true }).first().click();
  await teacher.getByLabel('Tên lớp', { exact: true }).fill(className);
  await teacher.getByLabel('Mô tả', { exact: false }).fill('Lớp kiểm thử luồng giữa hai người dùng.');
  await teacher.getByRole('button', { name: 'Tạo lớp học', exact: true }).click();
  const card = teacher.locator('.class-card').filter({ has: teacher.getByRole('heading', { name: className, exact: true }) });
  await expect(card).toBeVisible();
  const code = (await card.locator('.card-code strong').innerText()).trim();
  expect(code).toMatch(/^[A-HJ-NP-Z2-9]{8}$/);
  await student.goto('/');
  await student.getByRole('tab', { name: 'Đăng ký học viên' }).click();
  await student.getByLabel('Họ và tên').fill(name);
  await student.getByLabel('Email', { exact: true }).fill(email);
  await student.getByLabel('Mật khẩu', { exact: true }).fill('StudentDemo@123');
  await student.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(student.getByRole('status')).toContainText('Tài khoản học viên đã sẵn sàng');
  await student.getByLabel('Mật khẩu', { exact: true }).fill('StudentDemo@123');
  await student.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await student.getByRole('button', { name: 'Tham gia lớp', exact: true }).first().click();
  await student.getByLabel('Mã lớp', { exact: true }).fill(code.toLowerCase());
  await student.getByRole('dialog').getByRole('button', { name: 'Tham gia lớp', exact: true }).click();
  await expect(student.getByRole('heading', { name: className, exact: true })).toBeVisible();
  await student.reload();
  await expect(student.getByRole('heading', { name: className, exact: true })).toBeVisible();
  await expect(student.getByRole('button', { name: 'Tạo lớp mới', exact: true })).toHaveCount(0);
  await teacher.getByRole('button', { name: 'Tải lại lớp học', exact: true }).click();
  await card.getByRole('button', { name: 'Xem lớp' }).click();
  const dialog = teacher.getByRole('dialog');
  await expect(dialog.getByText(email, { exact: true })).toBeVisible();
  await dialog.getByRole('button', { name: 'Xóa ' + name + ' khỏi lớp' }).click();
  await teacher.getByRole('button', { name: 'Xác nhận', exact: true }).click();
  await expect(teacher.getByRole('status')).toContainText('Đã xóa thành viên khỏi lớp.');
  await student.getByRole('button', { name: 'Tải lại lớp học', exact: true }).click();
  await expect(student.getByRole('heading', { name: className, exact: true })).toHaveCount(0);
  await teacherContext.close();
  await studentContext.close();
});

test('mobile login and class list fit the screen', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, process.env.E2E_STUDENT_EMAIL ?? 'student@ielts.local', process.env.E2E_STUDENT_PASSWORD ?? 'Student@12345');
  await expect(page.getByRole('button', { name: 'Tham gia lớp', exact: true }).first()).toBeVisible();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
  expect(overflow).toBe(false);
  await page.screenshot({ path: 'test-results/mobile-classes.png', fullPage: true });
});

test('wrong credentials show an error and preserve the email', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Email', { exact: true }).fill('student@ielts.local');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('wrong-password');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Email hoặc mật khẩu chưa đúng');
  await expect(page.getByLabel('Email', { exact: true })).toHaveValue('student@ielts.local');
});
