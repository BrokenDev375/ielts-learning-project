import type { ResourceVersion } from './types';
import { taskNames } from './SpeakingEditor';
export default function SpeakingPreview({ version, teacher }: { version: ResourceVersion; teacher: boolean }) {
  const c = version.content;
  return <div className="learning-preview">
    <div className="learning-badges"><span className="role-pill">{taskNames[c.taskType]}</span><span className="role-pill">{c.part ? 'Part ' + c.part : 'Bài luyện riêng'}</span>{version.levelLabel && <span className="role-pill">{version.levelFramework}: {version.levelLabel}</span>}</div>
    <h2>{version.title}</h2>{version.topic && <p className="muted">{version.topic}</p>}
    <div className="learning-prompt"><h3>Đề bài</h3><p>{c.prompt || 'Chưa có đề bài.'}</p></div>
    {c.instructions && <section><h3>Hướng dẫn</h3><p>{c.instructions}</p></section>}
    <p className="learning-timing">Chuẩn bị: {c.preparationSeconds} giây · Nói: {c.speakingSeconds} giây</p>
    {!!c.checklist.length && <section><h3>Checklist cần đạt</h3><ul>{c.checklist.map((s, n) => <li key={n}>{s}</li>)}</ul></section>}
    {!!c.assessmentCriteria.length && <section><h3>Tiêu chí đánh giá</h3><ul>{c.assessmentCriteria.map((s, n) => <li key={n}>{s}</li>)}</ul></section>}
    {c.taskType === 'PRONUNCIATION' && <section><h3>Âm mục tiêu: {c.phonemes.join(', ')}</h3>{c.items.map((item, n) => <div className="learning-word" key={n}><strong>{item.text}</strong><p>{item.ipa}</p>{item.audioAssetId && <audio controls preload="none" src={'/api/assets/' + item.audioAssetId} />}</div>)}</section>}
    {teacher && version.teaching && <section className="learning-teacher-notes"><h3>Tham khảo riêng cho giảng viên</h3>{version.teaching.outlineExamples.map((s, n) => <p key={n}>{s}</p>)}<p>{version.teaching.teacherNotes}</p></section>}
    {version.source?.url && <p><a href={version.source.url} target="_blank" rel="noreferrer">Nguồn tham khảo</a></p>}
  </div>;
}
