import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api';
import type { AssignedDetail, Assignment } from './types';
import type { AssignedDetail as SpeakingAssigned } from '../speaking/types';
import type { WritingAssigned } from '../writing/types';
import { skillNames } from './types';
import SpeakingPreview from '../speaking/SpeakingPreview';
import WritingPreview from '../writing/WritingPreview';
import './learning.css';

function when(value: string | null) {
  return value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : 'Không giới hạn';
}
export default function AssignedWorkspace() {
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [detail, setDetail] = useState<AssignedDetail | null>(null);
  const [query, setQuery] = useState(''), [skill, setSkill] = useState('ALL');
  const [loading, setLoading] = useState(true), [busy, setBusy] = useState(false), [error, setError] = useState('');
  const [reload, setReload] = useState(0);
  useEffect(() => {
    let alive = true;
    setLoading(true); setError('');
    api<Assignment[]>('/api/assignments').then(data => { if (alive) setAssignments(data); })
      .catch(ex => { if (alive) setError(errorMessage(ex)); }).finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [reload]);
  async function open(id: number) {
    setBusy(true); setError('');
    try { setDetail(await api<AssignedDetail>('/api/assignments/' + id)); }
    catch (ex) { setError(errorMessage(ex)); } finally { setBusy(false); }
  }
  if (detail) return <section className="learning-panel">
    <div className="learning-section-heading"><span className="eyebrow">BÀI {skillNames[detail.skill].toUpperCase()} ĐƯỢC GIAO</span><button className="button secondary" onClick={() => setDetail(null)}>Quay lại</button></div>
    <p className="learning-note">{detail.assignment.classroomName} · Hạn nộp: {when(detail.dueAt)} · Phiên bản {detail.versionNo}</p>
    {detail.skill === 'SPEAKING'
      ? <SpeakingPreview version={{ ...detail as SpeakingAssigned, id: detail.resourceVersionId }} teacher={false} />
      : detail.skill === 'WRITING'
        ? <WritingPreview version={{ ...detail as WritingAssigned, id: detail.resourceVersionId }} teacher={false} />
        : <p className="muted">Nội dung kỹ năng này đang được chuẩn bị.</p>}
  </section>;
  const filtered = assignments.filter(a => (skill === 'ALL' || a.skill === skill) &&
    (a.title + ' ' + a.classroomName).toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi')));
  return <section className="assigned-workspace">
    <div className="page-heading"><div><span className="eyebrow">HỌC CÙNG LỚP CỦA BẠN</span><h1>Bài được giao<span className="heading-dot">.</span></h1><p>Xem bài Speaking và Writing từ các lớp bạn tham gia.</p></div></div>
    {error && <div className="error-box" role="alert">{error}</div>}
    <div className="learning-toolbar"><label><input aria-label="Tìm bài được giao" placeholder="Tìm bài hoặc lớp…" value={query} onChange={e => setQuery(e.target.value)} /></label><button className="button secondary" disabled={loading || busy} onClick={() => setReload(v => v + 1)}>Làm mới bài</button></div>
    <div className="learning-assignment-filters"><label>Kỹ năng<select value={skill} onChange={e => setSkill(e.target.value)}><option value="ALL">Tất cả kỹ năng</option><option value="SPEAKING">Speaking</option><option value="WRITING">Writing</option></select></label></div>
    {loading ? <p className="muted">Đang tải bài tập…</p> : <div className="learning-grid">
      {filtered.map(a => <article className="learning-card" key={a.id}>
        <div className="learning-card-heading"><span className="learning-skill">{a.skill}</span><span className="learning-status published">Đã giao</span></div>
        <h2>{a.title}</h2><p className="muted">{a.classroomName}</p><p className="learning-meta">Hạn nộp: {when(a.dueAt)}</p>{a.opensAt && <p className="learning-meta">Mở bài: {when(a.opensAt)}</p>}
        <div className="learning-card-actions"><button className="button secondary" disabled={busy} onClick={() => void open(a.id)}>Xem đề bài</button></div>
      </article>)}
      {!filtered.length && <p className="learning-empty">Chưa có bài phù hợp được giao cho bạn.</p>}
    </div>}
  </section>;
}
