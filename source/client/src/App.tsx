import { useEffect, useRef, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { api, ApiError, errorMessage, requestId, resetCsrf } from './api';
import type { Classroom, JoinResult, Member, User } from './types';
import SpeakingWorkspace from './features/speaking/SpeakingWorkspace';

type IconName = 'book' | 'grid' | 'people' | 'plus' | 'arrow' | 'logout' | 'search' | 'close' | 'copy' | 'check' | 'refresh' | 'user' | 'edit' | 'link' | 'mail' | 'calendar' | 'alert';
const paths: Record<IconName, string> = {
  book: 'M3 4h6a3 3 0 0 1 3 3v14a4 4 0 0 0-4-2H3V4Zm18 0h-6a3 3 0 0 0-3 3v14a4 4 0 0 1 4-2h5V4Z',
  grid: 'M3 3h7v7H3V3Zm11 0h7v7h-7V3ZM3 14h7v7H3v-7Zm11 0h7v7h-7v-7Z',
  people: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M22 21v-2a4 4 0 0 0-3-3.87M15 3.13a4 4 0 0 1 0 7.75M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  plus: 'M12 5v14M5 12h14',
  arrow: 'M5 12h14M13 6l6 6-6 6',
  logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
  search: 'M21 21l-4.4-4.4M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z',
  close: 'M6 6l12 12M6 18 18 6',
  copy: 'M9 9h12v12H9V9ZM5 15H3V3h12v2',
  check: 'M5 12l4 4L19 6',
  refresh: 'M20 7v5h-5M4 17v-5h5M6.1 6.1A8 8 0 0 1 19 8M18 18A8 8 0 0 1 5 16',
  user: 'M20 21v-2a7 7 0 0 0-14 0v2M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  edit: 'm16 3 5 5M3 21l5-1L21 7a2 2 0 0 0-4-4L4 16l-1 5Z',
  link: 'M10 13a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-2 2M14 11a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l2-2',
  mail: 'M3 5h18v14H3V5Zm0 0 9 8 9-8',
  calendar: 'M4 5h16v16H4V5ZM16 3v4M8 3v4M4 11h16',
  alert: 'M12 8v5M12 17h.01M10 3h4l8 17H2l8-17Z',
};
function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>;
}
function initials(name: string) { return name.trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase(); }
function date(value: string) { return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(value)); }
function Brand() { return <div className="brand"><span className="brand-mark"><Icon name="book" size={24} /></span><div>IELTS Study<span>Learn. Together.</span></div></div>; }
function ErrorBox({ message }: { message: string }) { return <div className="error-box" role="alert"><Icon name="alert" /><span>{message}</span></div>; }

function Modal({ title, subtitle, children, onClose, busy = false, wide = false }: { title: string; subtitle?: string; children: ReactNode; onClose: () => void; busy?: boolean; wide?: boolean }) {
  const dialog = useRef<HTMLDivElement>(null);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const panel = dialog.current;
    (panel?.querySelector<HTMLElement>('input, textarea') ?? panel?.querySelector<HTMLElement>('button'))?.focus();
    function key(event: KeyboardEvent) {
      if (event.key === 'Escape' && !busy) closeRef.current();
      if (event.key !== 'Tab' || !panel) return;
      const elements = Array.from(panel.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), textarea:not(:disabled), [tabindex="0"]'));
      const first = elements[0], last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
      if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    }
    document.addEventListener('keydown', key);
    const scroll = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.removeEventListener('keydown', key); document.body.style.overflow = scroll; if (previous?.isConnected) previous.focus(); };
  }, [busy]);
  return <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !busy) onClose(); }}>
    <div className={'modal ' + (wide ? 'modal-wide' : '')} ref={dialog} role="dialog" aria-modal="true" aria-labelledby="dialog-title">
      <div className="modal-heading"><div><h2 id="dialog-title">{title}</h2>{subtitle && <p>{subtitle}</p>}</div><button className="icon-button" aria-label="Đóng" onClick={onClose} disabled={busy}><Icon name="close" /></button></div>
      {children}
    </div>
  </div>;
}

function AuthPage({ onLogin, connectionError, retry }: { onLogin: (user: User) => void; connectionError: string; retry: () => void }) {
  const [tab, setTab] = useState<'login' | 'register'>('login');
  const [fullName, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setSuccess(''); setBusy(true);
    try {
      if (tab === 'register') {
        await api<User>('/api/auth/register', 'POST', { fullName: fullName.trim(), email: email.trim(), password }, true);
        setTab('login'); setPassword(''); setSuccess('Tài khoản học viên đã sẵn sàng. Đăng nhập để tham gia lớp.');
      } else {
        const user = await api<User>('/api/auth/login', 'POST', { email: email.trim(), password }, true);
        resetCsrf(); onLogin(user);
      }
    } catch (ex) {
      setError(ex instanceof ApiError && Object.keys(ex.fields).length ? Object.values(ex.fields).join(' ') : errorMessage(ex));
    } finally { setBusy(false); }
  }
  return <div className="auth-page">
    <aside className="auth-story"><Brand /><div className="story-content"><span className="eyebrow light">YOUR NEXT CHAPTER</span><h1>Mỗi lớp học,<br />một bước tiến mới.</h1><p>Kết nối với giảng viên và bắt đầu hành trình IELTS của bạn trong một không gian học tập chung.</p>
      <div className="story-illustration" aria-hidden="true"><div className="illustration-circle" /><div className="illustration-card card-back"><Icon name="book" size={36} /><span>English, every day.</span></div><div className="illustration-card card-front"><span className="mini-check"><Icon name="check" size={20} /></span><div><strong>Cùng nhau học tốt hơn</strong><span>Một kết nối. Nhiều cơ hội.</span></div></div><span className="floating-symbol symbol-a">Aa</span><span className="floating-symbol symbol-b"><Icon name="people" size={24} /></span></div>
    </div><p className="story-footer">A little progress, every day.</p></aside>
    <main className="auth-main"><div className="auth-card"><span className="eyebrow">CHÀO MỪNG ĐẾN IELTS STUDY</span><h2>{tab === 'login' ? 'Rất vui được gặp bạn.' : 'Bắt đầu hành trình của bạn.'}</h2><p className="muted">{tab === 'login' ? 'Đăng nhập để mở không gian lớp học.' : 'Tạo tài khoản học viên và tham gia bằng mã lớp.'}</p>
      <div className="auth-tabs" role="tablist" aria-label="Đăng nhập hoặc đăng ký">
        <button role="tab" aria-selected={tab === 'login'} className={tab === 'login' ? 'active' : ''} onClick={() => { setTab('login'); setError(''); setSuccess(''); }} disabled={busy}>Đăng nhập</button>
        <button role="tab" aria-selected={tab === 'register'} className={tab === 'register' ? 'active' : ''} onClick={() => { setTab('register'); setError(''); setSuccess(''); }} disabled={busy}>Đăng ký học viên</button>
      </div>
      {connectionError && <div className="connection-note"><ErrorBox message={connectionError} /><button className="text-button" onClick={retry}>Kiểm tra lại kết nối</button></div>}
      {error && <ErrorBox message={error} />}
      {success && <div className="success-box" role="status"><Icon name="check" />{success}</div>}
      <form onSubmit={submit}>
        {tab === 'register' && <label>Họ và tên<input autoComplete="name" value={fullName} onChange={e => setName(e.target.value)} required minLength={2} maxLength={100} placeholder="Nguyễn Văn An" disabled={busy} /></label>}
        <label>Email<input type="email" autoComplete="username" value={email} onChange={e => setEmail(e.target.value)} required maxLength={254} placeholder="ban@example.com" disabled={busy} /></label>
        <label>Mật khẩu<input type="password" autoComplete={tab === 'login' ? 'current-password' : 'new-password'} value={password} onChange={e => setPassword(e.target.value)} required minLength={tab === 'register' ? 8 : undefined} maxLength={72} placeholder={tab === 'register' ? 'Ít nhất 8 ký tự' : 'Nhập mật khẩu'} disabled={busy} /></label>
        <button className="button primary full" type="submit" disabled={busy}>{busy ? 'Đang xử lý…' : tab === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'}<Icon name="arrow" size={18} /></button>
      </form>
      <p className="auth-note"><Icon name="people" size={17} />Giảng viên đăng nhập bằng tài khoản được cấp.</p>
    </div><p className="auth-footer">IELTS Study · Không gian kết nối lớp học</p></main>
  </div>;
}

function ClassroomForm({ room, createRequestId, onClose, onSaved }: { room?: Classroom; createRequestId?: string; onClose: () => void; onSaved: (room: Classroom) => void }) {
  const [name, setName] = useState(room?.name ?? '');
  const [description, setDescription] = useState(room?.description ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const result = await api<Classroom>(room ? '/api/classes/' + room.id : '/api/classes', room ? 'PATCH' : 'POST',
        room ? { name: name.trim(), description: description.trim() } : { name: name.trim(), description: description.trim(), requestId: createRequestId });
      onSaved(result);
    } catch (ex) { setError(ex instanceof ApiError && Object.keys(ex.fields).length ? Object.values(ex.fields).join(' ') : errorMessage(ex)); }
    finally { setBusy(false); }
  }
  return <Modal title={room ? 'Chỉnh sửa lớp học' : 'Tạo lớp học mới'} subtitle="Đặt tên dễ nhớ để học viên nhận ra lớp của bạn." onClose={onClose} busy={busy}>
    <form onSubmit={submit}>{error && <ErrorBox message={error} />}<label>Tên lớp<input value={name} onChange={e => setName(e.target.value)} maxLength={100} required placeholder="Ví dụ: IELTS Reading — Nhóm sáng" disabled={busy} /></label>
      <label>Mô tả <span className="optional">(không bắt buộc)</span><textarea value={description} onChange={e => setDescription(e.target.value)} maxLength={1000} rows={4} placeholder="Giới thiệu ngắn về lớp, lịch học hoặc mục tiêu chung…" disabled={busy} /></label>
      {!room && <div className="form-tip"><Icon name="link" size={18} />Mã tham gia sẽ được tạo sau khi lưu lớp.</div>}
      <div className="modal-actions"><button type="button" className="button secondary" onClick={onClose} disabled={busy}>Hủy</button><button type="submit" className="button primary" disabled={busy}>{busy ? 'Đang lưu…' : room ? 'Lưu thay đổi' : 'Tạo lớp học'}</button></div>
    </form>
  </Modal>;
}

function JoinForm({ onClose, onJoined }: { onClose: () => void; onJoined: (result: JoinResult) => void }) {
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    try { onJoined(await api<JoinResult>('/api/classes/join', 'POST', { code: code.trim().toUpperCase() })); }
    catch (ex) { setError(errorMessage(ex)); } finally { setBusy(false); }
  }
  return <Modal title="Tham gia lớp học" subtitle="Nhập mã gồm 8 ký tự mà giảng viên đã chia sẻ." onClose={onClose} busy={busy}>
    <form onSubmit={submit}>{error && <ErrorBox message={error} />}<label>Mã lớp<input className="code-input" value={code} onChange={e => setCode(e.target.value.toUpperCase().replace(/\s/g, ''))} required minLength={8} maxLength={8} placeholder="ABCD2345" autoComplete="off" spellCheck={false} disabled={busy} /></label>
      <div className="form-tip"><Icon name="people" size={18} />Bạn có thể tham gia nhiều lớp bằng cùng tài khoản.</div>
      <div className="modal-actions"><button type="button" className="button secondary" onClick={onClose} disabled={busy}>Hủy</button><button className="button primary" type="submit" disabled={busy}>{busy ? 'Đang tham gia…' : 'Tham gia lớp'}</button></div>
    </form>
  </Modal>;
}

function ClassDetail({ room, teacher, onClose, onEdit, onRemove, onLeave, onCopy }: { room: Classroom; teacher: boolean; onClose: () => void; onEdit: () => void; onRemove: (member: Member) => void; onLeave: () => void; onCopy: (code: string) => void }) {
  const [members, setMembers] = useState<Member[]>([]);
  const [current, setCurrent] = useState(room);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reload, setReload] = useState(0);
  useEffect(() => {
    let alive = true;
    setLoading(true); setError('');
    Promise.all([api<Classroom>('/api/classes/' + room.id), teacher ? api<Member[]>('/api/classes/' + room.id + '/members') : Promise.resolve([])])
      .then(([result, roster]) => { if (alive) { setCurrent(result); setMembers(roster); } })
      .catch(ex => { if (alive) setError(errorMessage(ex)); }).finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [room.id, teacher, reload]);
  return <Modal title={current.name} subtitle={'Giảng viên: ' + current.teacherName} onClose={onClose} wide>
    {error && <ErrorBox message={error} />}
    {current.description && <p className="detail-description">{current.description}</p>}
    <div className="detail-meta"><span><Icon name="calendar" size={17} />Tạo ngày {date(current.createdAt)}</span><span><Icon name="people" size={17} />{current.memberCount} học viên</span></div>
    {teacher && current.joinCode && <div className="join-code-panel"><div><span>MÃ THAM GIA LỚP</span><strong>{current.joinCode}</strong></div><button className="button secondary" onClick={() => onCopy(current.joinCode!)}><Icon name="copy" size={17} />Sao chép mã</button></div>}
    {teacher && <section className="roster"><div className="section-heading"><h3>Thành viên lớp</h3><button className="icon-button" aria-label="Tải lại thành viên" onClick={() => setReload(v => v + 1)} disabled={loading}><Icon name="refresh" size={18} /></button></div>
      {loading ? <p className="muted">Đang tải danh sách…</p> : members.length ? <div className="member-list">{members.map(member => <div className="member-row" key={member.id}><span className="avatar small">{initials(member.fullName)}</span><div className="member-info"><strong>{member.fullName}</strong><span>{member.email}</span></div><span className="joined-date">{date(member.joinedAt)}</span><button className="text-button danger" onClick={() => onRemove(member)} aria-label={'Xóa ' + member.fullName + ' khỏi lớp'}>Xóa khỏi lớp</button></div>)}</div> : <div className="roster-empty">Chưa có học viên. Chia sẻ mã lớp để bắt đầu kết nối.</div>}
    </section>}
    <div className="modal-actions">{teacher ? <button className="button secondary" onClick={onEdit}><Icon name="edit" size={17} />Chỉnh sửa lớp</button> : <button className="button danger-soft" onClick={onLeave}>Rời lớp</button>}<button className="button primary" onClick={onClose}>Đóng</button></div>
  </Modal>;
}

function ConfirmAction({ title, text, path, onClose, onDone }: { title: string; text: string; path: string; onClose: () => void; onDone: () => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function confirm() {
    setBusy(true); setError('');
    try { await api<void>(path, 'DELETE'); onDone(); } catch (ex) { setError(errorMessage(ex)); } finally { setBusy(false); }
  }
  return <Modal title={title} onClose={onClose} busy={busy}>{error && <ErrorBox message={error} />}<p className="confirmation-text">{text}</p><div className="modal-actions"><button className="button secondary" onClick={onClose} disabled={busy}>Hủy</button><button className="button danger-solid" onClick={confirm} disabled={busy}>{busy ? 'Đang xử lý…' : 'Xác nhận'}</button></div></Modal>;
}

type Dialog = { type: 'create'; requestId: string } | { type: 'edit' | 'details' | 'leave'; room: Classroom } | { type: 'remove'; room: Classroom; member: Member } | { type: 'join' } | null;
export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [booting, setBooting] = useState(true);
  const [bootError, setBootError] = useState('');
  const [bootRetry, setBootRetry] = useState(0);
  const [rooms, setRooms] = useState<Classroom[]>([]);
  const [roomsLoading, setRoomsLoading] = useState(false);
  const [roomsError, setRoomsError] = useState('');
  const [query, setQuery] = useState('');
  const [view, setView] = useState<'classes' | 'account' | 'speaking'>('classes');
  const [dialog, setDialog] = useState<Dialog>(null);
  const [logoutBusy, setLogoutBusy] = useState(false);
  const [toast, setToast] = useState<{ message: string; error: boolean } | null>(null);
  const teacher = user?.role === 'TEACHER';
  useEffect(() => {
    let alive = true;
    setBooting(true); setBootError('');
    api<User>('/api/auth/me', 'GET', undefined, true).then(result => { if (alive) setUser(result); })
      .catch(ex => { if (alive && !(ex instanceof ApiError && ex.status === 401)) setBootError(errorMessage(ex)); })
      .finally(() => { if (alive) setBooting(false); });
    return () => { alive = false; };
  }, [bootRetry]);
  useEffect(() => {
    function expired() {
      resetCsrf(); setUser(null); setRooms([]); setDialog(null);
      setToast({ message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', error: true });
    }
    window.addEventListener('session-expired', expired);
    return () => window.removeEventListener('session-expired', expired);
  }, []);
  useEffect(() => {
    if (!toast) return;
    const timeout = window.setTimeout(() => setToast(null), 6000);
    return () => window.clearTimeout(timeout);
  }, [toast]);
  const activeUser = useRef(user);
  activeUser.current = user;
  async function loadRooms() {
    const actor = activeUser.current?.id;
    if (!actor) return;
    setRoomsLoading(true); setRoomsError('');
    try { const result = await api<Classroom[]>('/api/classes'); if (activeUser.current?.id === actor) setRooms(result); }
    catch (ex) { if (activeUser.current?.id === actor) setRoomsError(errorMessage(ex)); }
    finally { if (activeUser.current?.id === actor) setRoomsLoading(false); }
  }
  useEffect(() => { if (user) void loadRooms(); }, [user?.id]);
  function login(result: User) { setRooms([]); setRoomsError(''); setQuery(''); setView('classes'); setBootError(''); setUser(result); }
  function done(message: string) { setDialog(null); setToast({ message, error: false }); void loadRooms(); }
  async function logout() {
    setLogoutBusy(true);
    try { await api<void>('/api/auth/logout', 'POST'); resetCsrf(); setUser(null); setRooms([]); setDialog(null); setQuery(''); }
    catch (ex) { setToast({ message: errorMessage(ex), error: true }); } finally { setLogoutBusy(false); }
  }
  async function copy(code: string) {
    try {
      if (!navigator.clipboard) throw new Error('clipboard unavailable');
      await navigator.clipboard.writeText(code); setToast({ message: 'Đã sao chép mã lớp.', error: false });
    } catch { setToast({ message: 'Mã lớp: ' + code + '. Bạn có thể chọn và sao chép mã trực tiếp.', error: false }); }
  }
  const filtered = rooms.filter(room => (room.name + ' ' + room.teacherName).toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi')));
  const toastView = toast && <div className={'toast ' + (toast.error ? 'toast-error' : '')} role={toast.error ? 'alert' : 'status'}><Icon name={toast.error ? 'alert' : 'check'} />{toast.message}<button aria-label="Ẩn thông báo" onClick={() => setToast(null)}><Icon name="close" size={16} /></button></div>;
  if (booting) return <div className="loading-screen"><Brand /><span className="spinner" /><p>Đang mở không gian học tập…</p></div>;
  if (!user) return <><AuthPage onLogin={login} connectionError={bootError} retry={() => setBootRetry(v => v + 1)} />{toastView}</>;
  return <div className="app-shell">
    <aside className="sidebar"><Brand /><span className="sidebar-label">KHÔNG GIAN CỦA BẠN</span><nav aria-label="Điều hướng chính"><button className={view === 'classes' ? 'active' : ''} onClick={() => setView('classes')}><Icon name="grid" />Lớp học của tôi</button><button className={view === 'speaking' ? 'active' : ''} onClick={() => setView('speaking')}><Icon name="book" />{teacher ? 'Bài Speaking' : 'Bài được giao'}</button><button className={view === 'account' ? 'active' : ''} onClick={() => setView('account')}><Icon name="user" />Tài khoản</button></nav>
      <div className="sidebar-tip"><span className="tip-icon"><Icon name={teacher ? 'link' : 'book'} size={22} /></span><strong>{teacher ? 'Kết nối lớp học' : 'Bắt đầu cùng nhau'}</strong><p>{teacher ? 'Chia sẻ mã lớp với học viên để cùng bắt đầu.' : 'Nhận mã từ giảng viên để tham gia lớp học của bạn.'}</p></div>
      <div className="sidebar-user"><span className="avatar">{initials(user.fullName)}</span><div><strong>{user.fullName}</strong><span>{teacher ? 'Giảng viên' : 'Học viên'}</span></div></div>
    </aside>
    <div className="workspace"><header className="topbar"><span className="breadcrumb">Không gian học tập <span>/</span> <strong>{view === 'classes' ? 'Lớp học' : view === 'speaking' ? 'Speaking' : 'Tài khoản'}</strong></span><div className="topbar-actions"><span className="role-pill">{teacher ? 'Giảng viên' : 'Học viên'}</span><button className="text-button logout-button" onClick={logout} disabled={logoutBusy}><Icon name="logout" size={18} />{logoutBusy ? 'Đang thoát…' : 'Đăng xuất'}</button></div></header>
      <main className="main-content">{view === 'classes' ? <>
        <section className="page-heading"><div><span className="eyebrow">HELLO, {user.fullName.split(' ').at(-1)?.toUpperCase()}</span><h1>Lớp học của tôi<span className="heading-dot">.</span></h1><p>{teacher ? 'Tạo lớp, kết nối học viên và quản lý thành viên của bạn.' : 'Một nơi để kết nối với các lớp và giảng viên của bạn.'}</p></div><button className="button primary" onClick={() => setDialog(teacher ? { type: 'create', requestId: requestId() } : { type: 'join' })}><Icon name="plus" size={18} />{teacher ? 'Tạo lớp mới' : 'Tham gia lớp'}</button></section>
        <section className="stats" aria-label="Thông tin lớp học"><div className="stat-card"><span className="stat-icon green"><Icon name="book" size={23} /></span><div><span>{teacher ? 'Lớp đang quản lý' : 'Lớp đã tham gia'}</span><strong>{rooms.length.toString().padStart(2, '0')}</strong></div></div><div className="stat-card"><span className="stat-icon peach"><Icon name="people" size={23} /></span><div><span>{teacher ? 'Lượt tham gia lớp' : 'Giảng viên kết nối'}</span><strong>{(teacher ? rooms.reduce((sum, room) => sum + room.memberCount, 0) : new Set(rooms.map(room => room.teacherId)).size).toString().padStart(2, '0')}</strong></div></div><div className="welcome-card"><span className="small-label">GÓC LỚP HỌC</span><p>{teacher ? 'Một lớp học tốt bắt đầu từ sự kết nối.' : 'Mỗi ngày một chút. Mỗi bài một bước.'}</p><Icon name="book" size={45} /></div></section>
        <section className="classes-section"><div className="section-toolbar"><h2>Danh sách lớp <span className="count-badge">{rooms.length}</span></h2><div className="toolbar-controls"><label className="search-box"><Icon name="search" size={18} /><input aria-label="Tìm lớp học" placeholder="Tìm lớp học…" value={query} onChange={e => setQuery(e.target.value)} /></label><button className="icon-button refresh-button" onClick={() => void loadRooms()} aria-label="Tải lại lớp học" disabled={roomsLoading}><Icon name="refresh" size={19} /></button></div></div>
          {roomsError && <ErrorBox message={roomsError} />}
          {roomsLoading && !rooms.length ? <div className="empty-state"><span className="spinner" /><p>Đang tải lớp học…</p></div> : !filtered.length ? <div className="empty-state"><span className="empty-icon"><Icon name="book" size={34} /></span><h3>{query ? 'Chưa tìm thấy lớp phù hợp' : teacher ? 'Tạo lớp học đầu tiên của bạn' : 'Lớp học tiếp theo đang chờ bạn'}</h3><p>{query ? 'Thử tìm bằng tên lớp hoặc tên giảng viên.' : teacher ? 'Đặt tên lớp và chia sẻ mã tham gia với học viên.' : 'Nhập mã mà giảng viên cung cấp để bắt đầu tham gia.'}</p>{!query && <button className="button secondary" onClick={() => setDialog(teacher ? { type: 'create', requestId: requestId() } : { type: 'join' })}><Icon name="plus" size={17} />{teacher ? 'Tạo lớp mới' : 'Nhập mã lớp'}</button>}</div> : <div className="class-grid">{filtered.map(room => <article className={'class-card color-' + (room.id % 4)} key={room.id}><div className="class-banner"><span className="class-symbol"><Icon name="book" size={27} /></span><span className="class-category">IELTS STUDY</span><span className="banner-decoration" /></div><div className="class-body"><h3>{room.name}</h3><p className="class-description">{room.description || 'Cùng nhau xây dựng thói quen học tập mỗi ngày.'}</p><div className="class-teacher"><span className="avatar tiny">{initials(room.teacherName)}</span><span>{room.teacherName}</span></div>{teacher && room.joinCode && <div className="card-code"><span>Mã lớp</span><strong>{room.joinCode}</strong><button className="icon-button" aria-label={'Sao chép mã lớp ' + room.name} onClick={() => void copy(room.joinCode!)}><Icon name="copy" size={16} /></button></div>}<div className="class-footer"><span><Icon name="people" size={16} />{room.memberCount} học viên</span><button className="text-button" onClick={() => setDialog({ type: 'details', room })}>Xem lớp<Icon name="arrow" size={17} /></button></div></div></article>)}</div>}
        </section>
      </> : view === 'speaking' ? <SpeakingWorkspace key={user.id} user={user} rooms={rooms} /> : <section className="account-page"><span className="eyebrow">THÔNG TIN CÁ NHÂN</span><h1>Tài khoản của bạn<span className="heading-dot">.</span></h1><div className="account-card"><span className="avatar large">{initials(user.fullName)}</span><h2>{user.fullName}</h2><span className="role-pill">{teacher ? 'Giảng viên' : 'Học viên'}</span><dl><div><dt><Icon name="mail" size={18} />Email</dt><dd>{user.email}</dd></div><div><dt><Icon name="calendar" size={18} />Ngày tạo tài khoản</dt><dd>{date(user.createdAt)}</dd></div><div><dt><Icon name="book" size={18} />Lớp học</dt><dd>{rooms.length} lớp {teacher ? 'đang quản lý' : 'đã tham gia'}</dd></div></dl></div></section>}</main>
      <footer className="workspace-footer">IELTS Study <span>·</span> Cùng nhau tiến bộ mỗi ngày.</footer>
    </div>
    {dialog?.type === 'create' && <ClassroomForm createRequestId={dialog.requestId} onClose={() => setDialog(null)} onSaved={room => done('Đã tạo lớp ' + room.name + '.')} />}
    {dialog?.type === 'edit' && <ClassroomForm room={dialog.room} onClose={() => setDialog(null)} onSaved={() => done('Đã lưu thông tin lớp học.')} />}
    {dialog?.type === 'join' && <JoinForm onClose={() => setDialog(null)} onJoined={result => done(result.alreadyJoined ? 'Bạn đã là thành viên lớp ' + result.classroom.name + '.' : 'Đã tham gia lớp ' + result.classroom.name + '.')} />}
    {dialog?.type === 'details' && <ClassDetail room={dialog.room} teacher={teacher} onClose={() => setDialog(null)} onEdit={() => setDialog({ type: 'edit', room: dialog.room })} onRemove={member => setDialog({ type: 'remove', room: dialog.room, member })} onLeave={() => setDialog({ type: 'leave', room: dialog.room })} onCopy={code => void copy(code)} />}
    {dialog?.type === 'leave' && <ConfirmAction title="Rời lớp học?" text={'Bạn sẽ rời lớp “' + dialog.room.name + '”. Bạn có thể tham gia lại bằng mã lớp.'} path={'/api/classes/' + dialog.room.id + '/membership'} onClose={() => setDialog(null)} onDone={() => done('Đã rời lớp học.')} />}
    {dialog?.type === 'remove' && <ConfirmAction title="Xóa thành viên khỏi lớp?" text={'“' + dialog.member.fullName + '” sẽ không còn truy cập được lớp “' + dialog.room.name + '”.'} path={'/api/classes/' + dialog.room.id + '/members/' + dialog.member.id} onClose={() => setDialog(null)} onDone={() => done('Đã xóa thành viên khỏi lớp.')} />}
    {toastView}
  </div>;
}
