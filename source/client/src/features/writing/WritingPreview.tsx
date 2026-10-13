import type { WritingVersion } from './types';
import { activityNames, stepNames, supportNames } from './types';
export default function WritingPreview({ version, teacher }: { version: WritingVersion; teacher: boolean }) {
  const c = version.content;
  return <div className="learning-preview writing-preview">
    <div className="learning-badges"><span className="role-pill">{c.taskType === 'TASK_1' ? 'Task 1 Academic' : 'Task 2'}</span><span className="role-pill">{activityNames[c.activity]}</span><span className="role-pill">{supportNames[version.supportLevel]}</span>{version.levelLabel && <span className="role-pill">{version.levelLabel}</span>}</div>
    <h2>{version.title}</h2>{version.topic && <p className="muted">{version.topic}</p>}
    <div className="learning-prompt"><h3>Đề bài</h3><p>{c.prompt || 'Chưa có đề bài.'}</p></div>
    {c.visualAssetId && <figure className="writing-visual"><img src={'/api/assets/' + c.visualAssetId} alt="Hình đề bài Task 1" /><figcaption>{c.visualUnit && 'Đơn vị: ' + c.visualUnit}{c.visualPeriods.length > 0 && ' · Mốc: ' + c.visualPeriods.join(', ')}</figcaption></figure>}
    <p className="learning-timing">{c.minimumWords > 0 ? 'Tối thiểu ' + c.minimumWords + ' từ' : 'Luyện tập theo yêu cầu từng bước'} · Thời gian luyện: {Math.round(c.writingSeconds / 60)} phút</p>
    {c.instructions && <section><h3>Hướng dẫn</h3><p>{c.instructions}</p></section>}
    {!!c.checklist.length && <section><h3>Checklist cần đạt</h3><ul>{c.checklist.map((item, index) => <li key={index}>{item}</li>)}</ul></section>}
    {!!c.assessmentCriteria.length && <section><h3>Tiêu chí phản hồi</h3><ul>{c.assessmentCriteria.map(item => <li key={item}>{item}</li>)}</ul></section>}
    {!!c.vocabulary.length && <section><h3>Bảng từ gợi ý</h3><p className="muted">Học cách dùng phù hợp với ý của bạn; không cần đưa tất cả từ vào bài.</p><div className="writing-vocabulary">{c.vocabulary.map((word, index) => <article className="writing-vocab-card" key={index}><strong>{word.term}</strong><p>{word.meaning}</p>{word.collocation && <p><span className="muted">Kết hợp: </span>{word.collocation}</p>}<p className="writing-example">{word.example}</p>{word.level && <small>Mức từ tham khảo: {word.level}</small>}</article>)}</div></section>}
    {c.activity === 'GRAMMAR_REPAIR' && <section className="writing-item"><h3>Câu cần sửa</h3><p>{c.originalSentence}</p><h3>Ngữ cảnh và ý định gốc</h3><p>{c.originalMeaning}</p>{c.grammarSourceKind === 'DEMO_AUTHORING' && <p className="muted">Câu tự biên soạn dùng luyện tập.</p>}</section>}
    {!!c.activities.length && <section><h3>Các bước luyện tập</h3>{c.activities.map((step, index) => <article className="writing-item" key={step.id}><div className="writing-step-heading"><h4>{index + 1}. {stepNames[step.type]}</h4><span className="role-pill">{supportNames[step.stage]}</span></div><p>{step.instruction}</p>{step.context && <p>{step.context}</p>}{!!step.items.length && <ul>{step.items.map((item, i) => <li key={i}>{item}</li>)}</ul>}{step.example && <div className="writing-example"><strong>Ví dụ hướng dẫn</strong><p>{step.example}</p></div>}</article>)}</section>}
    {teacher && version.teaching && <section className="learning-teacher-notes"><h3>Tham khảo riêng cho giảng viên</h3>
      {version.teaching.modelResponse && <><h4>Bài mẫu / phương án tham khảo</h4><p>{version.teaching.modelResponse}</p></>}
      {version.teaching.sampleAnswers.map(answer => <div className="writing-private" key={answer.activityId}><h4>Gợi ý cho hoạt động {c.activities.findIndex(s => s.id === answer.activityId) + 1}</h4><ul>{answer.acceptedExamples.map((text, n) => <li key={n}>{text}</li>)}</ul><p>{answer.explanation}</p></div>)}
      <p>{version.teaching.teacherNotes}</p>
      {version.teaching.rubricSourceUrl && <a href={version.teaching.rubricSourceUrl} target="_blank" rel="noreferrer">Nguồn rubric tham khảo</a>}
    </section>}
    {version.source?.url && <p><a href={version.source.url} target="_blank" rel="noreferrer">Nguồn tham khảo</a></p>}
  </div>;
}
