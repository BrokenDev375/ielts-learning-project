import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api';
import type { Classroom, User } from '../../types';
import type { Assignment } from '../learning/types';
import type { AssignedDetail, LearningResource, ResourceVersion } from './types';
import SpeakingPreview from './SpeakingPreview';
import AssignmentForm from '../learning/AssignmentForm';
import SpeakingEditor, { taskNames } from './SpeakingEditor';
import './speaking.css';

function when(value: string | null) { return value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : 'Không giới hạn'; }
export default function SpeakingWorkspace({ user, rooms }: { user: User; rooms: Classroom[] }) {
  const teacher = user.role === 'TEACHER';
  const [tab, setTab] = useState<'bank' | 'assigned'>(teacher ? 'bank' : 'assigned');
  const [resources, setResources] = useState<LearningResource[]>([]);
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [editing, setEditing] = useState<LearningResource | 'new' | null>(null);
  const [preview, setPreview] = useState<ResourceVersion | null>(null);
  const [history, setHistory] = useState<ResourceVersion[]>([]);
  const [assignedDetail, setAssignedDetail] = useState<AssignedDetail | null>(null);
  const [giving, setGiving] = useState<ResourceVersion | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [query, setQuery] = useState('');
  const [reload, setReload] = useState(0);
  useEffect(() => {
    let alive = true; setLoading(true); setError('');
    Promise.all([teacher ? api<LearningResource[]>('/api/resources?skill=SPEAKING') : Promise.resolve([]), api<Assignment[]>('/api/assignments')])
      .then(([r, a]) => { if (alive) { setResources(r); setAssignments(a.filter(item => item.skill === 'SPEAKING')); } })
      .catch(ex => { if (alive) setError(errorMessage(ex)); }).finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [teacher, reload]);
  async function open(resource: LearningResource) {
    setError(''); setBusy(true);
    try { const versions = await api<ResourceVersion[]>('/api/resources/' + resource.id + '/versions'); setHistory(versions); setPreview(versions[0]); }
    catch (ex) { setError(errorMessage(ex)); } finally { setBusy(false); }
  }
  async function openAssignment(id: number) {
    setError(''); setBusy(true);
    try { setAssignedDetail(await api<AssignedDetail>('/api/assignments/' + id)); } catch (ex) { setError(errorMessage(ex)); } finally { setBusy(false); }
  }
  async function publish(resource: LearningResource) {
    setBusy(true); setError(''); setMessage('');
    try { await api('/api/resources/' + resource.id + '/versions/' + resource.version.id + '/publish', 'POST'); setMessage('Đã xuất bản bài. Bạn có thể giao bài cho học viên.'); setReload(v => v + 1); }
    catch (ex) { setError(errorMessage(ex)); } finally { setBusy(false); }
  }
  if (editing) return <SpeakingEditor key={editing === 'new' ? 'new' : editing.id} resource={editing === 'new' ? undefined : editing} onCancel={() => setEditing(null)} onSaved={() => { setEditing(null); setMessage('Đã lưu bản nháp. Xem trước và kiểm tra nội dung trước khi xuất bản.'); setReload(v => v + 1); }} />;
  if (giving) return <AssignmentForm version={giving} rooms={rooms} onCancel={() => setGiving(null)} onDone={() => { setGiving(null); setTab('assigned'); setMessage('Đã giao bài cho học viên.'); setReload(v => v + 1); }} />;
  if (preview || assignedDetail) return <section className="learning-panel">
    <div className="learning-section-heading"><span className="eyebrow">{teacher ? 'XEM TRƯỚC BÀI SPEAKING' : 'BÀI SPEAKING ĐƯỢC GIAO'}</span><button className="button secondary" onClick={() => { setPreview(null); setAssignedDetail(null); }}>Quay lại</button></div>
    {preview && <label>Phiên bản bài<select value={preview.id} onChange={e => setPreview(history.find(v => v.id === Number(e.target.value))!)}>{history.map(v => <option key={v.id} value={v.id}>Phiên bản {v.versionNo} · {v.status === 'DRAFT' ? 'Bản nháp' : 'Đã xuất bản'}</option>)}</select></label>}
    {assignedDetail && <p className="learning-note">{assignedDetail.assignment.classroomName} · Hạn nộp: {when(assignedDetail.dueAt)} · Phiên bản {assignedDetail.versionNo}</p>}
    <SpeakingPreview version={preview ?? { ...assignedDetail!, id: assignedDetail!.resourceVersionId }} teacher={teacher} />
    {teacher && preview?.status === 'PUBLISHED' && <div className="learning-actions"><button className="button primary" onClick={() => { setGiving(preview); setPreview(null); }}>Giao phiên bản này</button></div>}
  </section>;
  const filteredResources = resources.filter(r => (r.version.title + ' ' + (r.version.topic ?? '')).toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi')));
  const filteredAssignments = assignments.filter(a => (a.title + ' ' + a.classroomName).toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi')));
  return <section className="speaking-workspace">
    <div className="page-heading"><div><span className="eyebrow">{teacher ? 'NGÂN HÀNG & PHÂN PHỐI BÀI TẬP' : 'HỌC CÙNG LỚP CỦA BẠN'}</span><h1>{teacher ? 'Bài Speaking' : 'Bài được giao'}<span className="heading-dot">.</span></h1><p>{teacher ? 'Chuẩn bị bài nói, bài bổ trợ và giao đúng học viên.' : 'Xem đề bài và hướng dẫn từ giảng viên.'}</p></div>{teacher && <button className="button primary" onClick={() => { setEditing('new'); setMessage(''); }}>Tạo bài Speaking</button>}</div>
    {teacher && <div className="learning-tabs"><button className={tab === 'bank' ? 'active' : ''} onClick={() => { setTab('bank'); setQuery(''); }}>Ngân hàng bài</button><button className={tab === 'assigned' ? 'active' : ''} onClick={() => { setTab('assigned'); setQuery(''); }}>Bài đã giao</button></div>}
    {error && <div className="error-box" role="alert">{error}</div>}{message && <div className="success-box" role="status">{message}</div>}
    <div className="learning-toolbar"><label><input aria-label="Tìm bài Speaking" placeholder="Tìm bài hoặc chủ đề…" value={query} onChange={e => setQuery(e.target.value)} /></label><button className="button secondary" onClick={() => setReload(v => v + 1)} disabled={loading || busy}>Làm mới bài</button></div>
    {loading ? <p className="muted">Đang tải bài tập…</p> : tab === 'bank' ? <div className="learning-grid">
      {filteredResources.map(resource => { const v = resource.version; return <article className="learning-card" key={resource.id}>
        <div className="learning-card-heading"><span className="learning-skill">SPEAKING</span><span className={'learning-status ' + (v.status === 'PUBLISHED' ? 'published' : '')}>{v.status === 'DRAFT' ? 'Bản nháp' : 'Đã xuất bản'}</span></div>
        <h2>{v.title}</h2><p className="muted">{taskNames[v.content.taskType]}</p><p className="learning-meta">{v.topic || 'Chưa đặt chủ đề'} · Phiên bản {v.versionNo}</p>
        <div className="learning-card-actions"><button className="text-button" disabled={busy} onClick={() => void open(resource)}>Xem trước</button><button className="text-button" disabled={busy} onClick={() => setEditing(resource)}>Chỉnh sửa</button>
          {v.status === 'DRAFT' ? <button className="button secondary" disabled={busy} onClick={() => void publish(resource)}>Xuất bản</button> : <button className="button primary" disabled={busy} onClick={() => setGiving(v)}>Giao bài</button>}</div>
      </article>; })}
      {!filteredResources.length && <p className="learning-empty">Chưa có bài phù hợp. Tạo bài Speaking để bắt đầu.</p>}
    </div> : <div className="learning-grid">
      {filteredAssignments.map(a => <article className="learning-card" key={a.id}><div className="learning-card-heading"><span className="learning-skill">SPEAKING</span><span className="learning-status published">Đã giao</span></div><h2>{a.title}</h2><p className="muted">{a.classroomName}</p><p className="learning-meta">Hạn nộp: {when(a.dueAt)}</p>{a.opensAt && <p className="learning-meta">Mở bài: {when(a.opensAt)}</p>}{teacher && <p className="learning-meta">{a.recipientCount} học viên nhận bài</p>}<div className="learning-card-actions"><button className="button secondary" disabled={busy} onClick={() => void openAssignment(a.id)}>Xem đề bài</button></div></article>)}
      {!filteredAssignments.length && <p className="learning-empty">{teacher ? 'Chưa có bài được giao. Chọn bài đã xuất bản trong ngân hàng để giao.' : 'Chưa có bài Speaking được giao cho bạn.'}</p>}
    </div>}
  </section>;
}
