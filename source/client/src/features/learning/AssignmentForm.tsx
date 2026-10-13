import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api, errorMessage, requestId } from '../../api';
import type { Classroom, Member } from '../../types';
import type { Assignment, ResourceVersion } from './types';

export default function AssignmentForm({ version, rooms, onDone, onCancel }: {
  version: ResourceVersion; rooms: Classroom[]; onDone: (assignment: Assignment) => void; onCancel: () => void;
}) {
  const [roomId, setRoomId] = useState(rooms[0]?.id ?? 0);
  const [mode, setMode] = useState('CLASS_SNAPSHOT');
  const [members, setMembers] = useState<Member[]>([]);
  const [students, setStudents] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);
  const [purpose, setPurpose] = useState(version.resourceKind === 'REMEDIAL' ? 'REMEDIAL' : 'PRACTICE');
  const [opens, setOpens] = useState('');
  const [due, setDue] = useState('');
  const [late, setLate] = useState(false);
  const [attempts, setAttempts] = useState(1);
  const [feedback, setFeedback] = useState('AFTER_SUBMISSION');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const id = useRef(requestId());
  useEffect(() => {
    if (!roomId) return;
    let alive = true; setLoading(true); setStudents([]); setMembers([]); setError('');
    api<Member[]>('/api/classes/' + roomId + '/members').then(data => { if (alive) setMembers(data); })
      .catch(ex => { if (alive) setError(errorMessage(ex)); }).finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [roomId]);
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      onDone(await api<Assignment>('/api/assignments', 'POST', { requestId: id.current, classroomId: roomId,
        resourceVersionId: version.id, purpose, recipientMode: mode, studentIds: mode === 'SELECTED' ? students : [],
        opensAt: opens ? new Date(opens).toISOString() : null, dueAt: due ? new Date(due).toISOString() : null,
        allowLate: late, maxAttempts: attempts, feedbackPolicy: feedback }));
    } catch (ex) { setError(errorMessage(ex)); } finally { setBusy(false); }
  }
  return <section className="learning-panel"><div className="learning-section-heading"><div><span className="eyebrow">PHÂN PHỐI BÀI TẬP</span><h2>Giao bài Speaking</h2></div><button className="button secondary" disabled={busy} onClick={onCancel}>Quay lại</button></div>
    <p className="learning-note"><strong>{version.title}</strong> · Phiên bản {version.versionNo}</p>
    {error && <div className="error-box" role="alert">{error}</div>}
    {!rooms.length ? <p className="muted">Tạo lớp và thêm học viên trước khi giao bài.</p> : <form onSubmit={submit} onChange={() => { id.current = requestId(); }}><fieldset disabled={busy}>
      <div className="learning-columns"><label>Lớp nhận bài<select value={roomId} onChange={e => setRoomId(Number(e.target.value))}>{rooms.map(room => <option key={room.id} value={room.id}>{room.name}</option>)}</select></label>
        <label>Người nhận<select value={mode} onChange={e => setMode(e.target.value)}><option value="CLASS_SNAPSHOT">Toàn bộ học viên hiện tại</option><option value="SELECTED">Chọn học viên</option></select></label>
        <label>Mục đích giao bài<select value={purpose} onChange={e => setPurpose(e.target.value)}>{version.resourceKind === 'REMEDIAL' ? <option value="REMEDIAL">Bổ trợ</option> : <><option value="PRACTICE">Luyện tập</option><option value="TEST_INITIAL">Test đầu vào</option><option value="TEST_PERIODIC">Test định kỳ</option><option value="RECHECK">Kiểm tra lại</option></>}</select></label>
      </div>
      {loading ? <p className="muted">Đang tải học viên…</p> : <p className="muted">{members.length} học viên hiện đang thuộc lớp.</p>}
      {mode === 'SELECTED' && <div className="learning-roster">{members.map(m => <label className="learning-checkbox" key={m.id}><input type="checkbox" checked={students.includes(m.id)} onChange={e => setStudents(ids => e.target.checked ? [...ids, m.id] : ids.filter(id => id !== m.id))} /><span>{m.fullName}<small>{m.email}</small></span></label>)}</div>}
      <div className="learning-columns"><label>Thời gian mở bài <span className="optional">bỏ trống để mở ngay</span><input type="datetime-local" value={opens} onChange={e => setOpens(e.target.value)} /></label>
        <label>Hạn nộp <span className="optional">không bắt buộc</span><input type="datetime-local" min={opens || undefined} value={due} onChange={e => setDue(e.target.value)} /></label>
        <label>Số lần làm tối đa<input type="number" min={1} max={100} required value={attempts} onChange={e => setAttempts(Number(e.target.value))} /></label>
        <label>Thời điểm mở phản hồi<select value={feedback} onChange={e => setFeedback(e.target.value)}><option value="AFTER_SUBMISSION">Sau khi nộp bài</option><option value="AFTER_DUE">Sau hạn nộp</option><option value="TEACHER_RELEASE">Giảng viên quyết định</option></select></label>
      </div>
      <label className="learning-checkbox"><input type="checkbox" checked={late} onChange={e => setLate(e.target.checked)} />Cho phép nộp muộn</label>
      <p className="learning-note">Danh sách người nhận được lưu lúc giao bài. Học viên tham gia lớp sau đó cần được giao bổ sung.</p>
      <div className="learning-actions"><button type="button" className="button secondary" onClick={onCancel}>Hủy</button><button type="submit" className="button primary" disabled={loading || !members.length || (mode === 'SELECTED' && !students.length)}>{busy ? 'Đang giao…' : 'Xác nhận giao bài'}</button></div>
    </fieldset></form>}
  </section>;
}
