import { useState } from 'react';
import type { FormEvent } from 'react';
import { api, errorMessage, requestId } from '../../api';
import type { Asset } from '../learning/types';
import type { ActivityKind, Task, WritingContent, WritingInput, WritingResource, WritingStep, Vocabulary, SampleAnswer } from './types';
import { activityNames, allowedActivities, criteria, rubricURL, stepNames, supportNames, targetStep } from './types';

function blank(): WritingInput {
  return { title: '', skill: 'WRITING', resourceKind: 'MAIN', activityType: 'WRITING_ESSAY', topic: '',
    levelFramework: 'IELTS_PRACTICE', levelLabel: '', supportLevel: 'INDEPENDENT',
    content: { taskType: 'TASK_2', activity: 'MAIN', prompt: '', instructions: '', minimumWords: 250, writingSeconds: 2400,
      visualAssetId: null, visualType: 'NONE', hasTimeDimension: false, visualUnit: '', visualGroups: [], visualPeriods: [],
      checklist: [], assessmentCriteria: criteria('TASK_2'), vocabulary: [], activities: [],
      originalSentence: '', originalMeaning: '', grammarSourceKind: 'DEMO_AUTHORING', teacherConfirmed: false },
    teaching: { teacherNotes: '', modelResponse: '', sampleAnswers: [], rubricSourceUrl: rubricURL }, source: { url: '', note: '' } };
}
function fromResource(resource?: WritingResource): WritingInput {
  if (!resource) return blank();
  const v = resource.version;
  return structuredClone({ title: v.title, skill: v.skill, resourceKind: v.resourceKind, activityType: v.activityType, topic: v.topic ?? '',
    levelFramework: v.levelFramework, levelLabel: v.levelLabel ?? '', supportLevel: v.supportLevel, content: v.content,
    teaching: v.teaching ?? blank().teaching, source: v.source ?? { url: '', note: '' } });
}
function lines(value: string) { return value.split('\n'); }
function cleaned(values: string[]) { return values.map(s => s.trim()).filter(Boolean); }
function newStep(type: WritingStep['type'], stage: WritingStep['stage']): WritingStep {
  return { id: requestId(), type, stage, instruction: '', context: '', items: [], example: '' };
}
export default function WritingEditor({ resource, onSaved, onCancel }: {
  resource?: WritingResource; onSaved: () => void; onCancel: () => void;
}) {
  const [value, setValue] = useState<WritingInput>(() => fromResource(resource));
  const [busy, setBusy] = useState(false), [uploading, setUploading] = useState(false), [error, setError] = useState('');
  const c = value.content, t = value.teaching!, main = c.activity === 'MAIN';
  function content(patch: Partial<WritingContent>) { setValue(v => ({ ...v, content: { ...v.content, ...patch } })); }
  function selectActivity(activity: ActivityKind, task = c.taskType) {
    setValue(v => {
      const isMain = activity === 'MAIN';
      const steps = isMain ? [] : [newStep(targetStep[activity], 'GUIDED'), newStep('TRANSFER', 'TRANSFER')];
      return { ...v, resourceKind: isMain ? 'MAIN' : 'REMEDIAL', activityType: isMain ? 'WRITING_ESSAY' : 'GUIDED',
        supportLevel: isMain ? 'INDEPENDENT' : 'GUIDED',
        content: { ...v.content, taskType: task, activity, minimumWords: isMain ? task === 'TASK_1' ? 150 : 250 : 0,
          writingSeconds: isMain ? task === 'TASK_1' ? 1200 : 2400 : 600,
          assessmentCriteria: criteria(task), activities: steps, teacherConfirmed: false,
          ...(task === 'TASK_2' ? { visualAssetId: null, visualType: 'NONE' as const, hasTimeDimension: false, visualGroups: [], visualPeriods: [], visualUnit: '' } : { visualType: v.content.visualType === 'NONE' ? 'BAR_CHART' as const : v.content.visualType }) },
        teaching: { ...v.teaching!, sampleAnswers: steps.slice(0, 1).map(s => ({ activityId: s.id, acceptedExamples: [], explanation: '' })) } };
    });
  }
  function selectTask(task: Task) {
    selectActivity(allowedActivities(task).includes(c.activity) ? c.activity : 'MAIN', task);
  }
  function word(index: number, patch: Partial<Vocabulary>) { content({ vocabulary: c.vocabulary.map((w, n) => n === index ? { ...w, ...patch } : w) }); }
  function step(index: number, patch: Partial<WritingStep>) { content({ activities: c.activities.map((s, n) => n === index ? { ...s, ...patch } : s) }); }
  function answer(id: string, patch: Partial<SampleAnswer>) {
    setValue(v => {
      const current = v.teaching!.sampleAnswers.find(a => a.activityId === id) ?? { activityId: id, acceptedExamples: [], explanation: '' };
      return { ...v, teaching: { ...v.teaching!, sampleAnswers: [...v.teaching!.sampleAnswers.filter(a => a.activityId !== id), { ...current, ...patch }] } };
    });
  }
  async function uploadImage(file?: File, control?: HTMLInputElement) {
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) { setError('Hình tối đa 5 MB.'); return; }
    setUploading(true); setError('');
    try { const data = new FormData(); data.append('file', file); const result = await api<Asset>('/api/assets/images', 'POST', data); content({ visualAssetId: result.id }); }
    catch (ex) { setError(errorMessage(ex)); } finally { setUploading(false); if (control) control.value = ''; }
  }
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const payload: WritingInput = { ...value, content: { ...c, visualGroups: cleaned(c.visualGroups), visualPeriods: cleaned(c.visualPeriods),
        checklist: cleaned(c.checklist), assessmentCriteria: cleaned(c.assessmentCriteria), activities: c.activities.map(s => ({ ...s, items: cleaned(s.items) })) },
        teaching: { ...t, sampleAnswers: t.sampleAnswers.map(a => ({ ...a, acceptedExamples: cleaned(a.acceptedExamples) })) } };
      await api(resource ? '/api/resources/' + resource.id + '/draft' : '/api/resources', resource ? 'PUT' : 'POST', payload); onSaved();
    }
    catch (ex) { setError(errorMessage(ex)); } finally { setBusy(false); }
  }
  return <section className="learning-panel writing-editor">
    <div className="learning-section-heading"><div><span className="eyebrow">CHUẨN BỊ HỌC LIỆU</span><h2>{resource ? 'Chỉnh sửa bài Writing' : 'Tạo bài Writing'}</h2></div><button className="button secondary" disabled={busy || uploading} onClick={onCancel}>Quay lại</button></div>
    {resource?.version.status === 'PUBLISHED' && <p className="learning-note">Lưu thay đổi sẽ tạo phiên bản nháp mới. Bài đã giao giữ nguyên phiên bản.</p>}
    {error && <div className="error-box" role="alert">{error}</div>}
    <form onSubmit={submit}><fieldset disabled={busy || uploading}>
      <div className="learning-columns">
        <label>Tên bài<input required maxLength={200} value={value.title} onChange={e => setValue(v => ({ ...v, title: e.target.value }))} /></label>
        <label>Chủ đề<input maxLength={150} value={value.topic} onChange={e => setValue(v => ({ ...v, topic: e.target.value }))} /></label>
        <label>Nhiệm vụ<select value={c.taskType} onChange={e => selectTask(e.target.value as Task)}><option value="TASK_1">Task 1 Academic</option><option value="TASK_2">Task 2</option></select></label>
        <label>Nhóm bài<select value={c.activity} onChange={e => selectActivity(e.target.value as ActivityKind)}>{allowedActivities(c.taskType).map(a => <option value={a} key={a}>{activityNames[a]}</option>)}</select></label>
        <label>Mức hỗ trợ<select value={value.supportLevel} onChange={e => setValue(v => ({ ...v, supportLevel: e.target.value as WritingInput['supportLevel'] }))}>{Object.entries(supportNames).map(([k, label]) => <option key={k} value={k}>{label}</option>)}</select></label>
        <label>Khung mức<select value={value.levelFramework ?? 'IELTS_PRACTICE'} onChange={e => setValue(v => ({ ...v, levelFramework: e.target.value as WritingInput['levelFramework'] }))}><option value="IELTS_PRACTICE">Luyện IELTS</option><option value="CEFR">CEFR</option><option value="RAW">Thang nội bộ</option></select></label>
        <label>Mức luyện <span className="optional">B1/B2 nếu chọn CEFR, hoặc để trống</span><input maxLength={32} value={value.levelLabel} onChange={e => setValue(v => ({ ...v, levelLabel: e.target.value }))} /></label>
      </div>
      <label>Đề bài / yêu cầu<textarea rows={5} maxLength={15000} value={c.prompt} onChange={e => content({ prompt: e.target.value })} /></label>
      <label>Hướng dẫn cho học viên<textarea rows={3} maxLength={10000} value={c.instructions} onChange={e => content({ instructions: e.target.value })} /></label>
      <div className="learning-columns">
        <label>Số từ tối thiểu<input type="number" min={0} max={2000} required readOnly={main} value={c.minimumWords} onChange={e => content({ minimumWords: Number(e.target.value) })} /></label>
        <label>Thời gian luyện (phút)<input type="number" min={1 / 60} max={120} step={1 / 60} required value={c.writingSeconds / 60} onChange={e => content({ writingSeconds: Math.round(Number(e.target.value) * 60) })} /></label>
      </div>
      {c.taskType === 'TASK_1' && <section className="writing-form-section"><h3>Hình và thông tin Task 1</h3>
        <div className="learning-columns">
          <label>Loại hình<select value={c.visualType} onChange={e => content({ visualType: e.target.value as WritingContent['visualType'] })}><option value="NONE">Chưa chọn</option><option value="BAR_CHART">Biểu đồ cột</option><option value="LINE_GRAPH">Biểu đồ đường</option><option value="PIE_CHART">Biểu đồ tròn</option><option value="TABLE">Bảng</option><option value="MIXED">Kết hợp</option><option value="PROCESS">Quy trình</option><option value="MAP">Bản đồ</option></select></label>
          <label>Hình đề bài (PNG/JPEG, tối đa 5 MB)<input type="file" accept="image/png,image/jpeg" onChange={e => void uploadImage(e.target.files?.[0], e.target)} /></label>
          <label>Đơn vị dữ liệu<input maxLength={100} value={c.visualUnit} onChange={e => content({ visualUnit: e.target.value })} /></label>
          <label>Nhóm so sánh <span className="optional">mỗi nhóm một dòng</span><textarea rows={3} value={c.visualGroups.join('\n')} onChange={e => content({ visualGroups: lines(e.target.value) })} /></label>
        </div>
        <label className="learning-checkbox"><input type="checkbox" checked={c.hasTimeDimension} onChange={e => content({ hasTimeDimension: e.target.checked })} />Có diễn biến theo thời gian</label>
        {c.hasTimeDimension && <label>Mốc thời gian <span className="optional">mỗi mốc một dòng</span><textarea rows={3} value={c.visualPeriods.join('\n')} onChange={e => content({ visualPeriods: lines(e.target.value) })} /></label>}
        {c.visualAssetId && <div className="writing-image-preview"><img src={'/api/assets/' + c.visualAssetId} alt="Hình đề bài Task 1" /><button type="button" className="text-button" onClick={() => content({ visualAssetId: null })}>Bỏ hình</button></div>}
      </section>}
      <label>Checklist cần đạt <span className="optional">mỗi mục một dòng</span><textarea rows={3} value={c.checklist.join('\n')} onChange={e => content({ checklist: lines(e.target.value) })} /></label>
      {main ? <div className="writing-criteria"><h3>Tiêu chí đánh giá</h3><ul>{c.assessmentCriteria.map(item => <li key={item}>{item}</li>)}</ul><a href={rubricURL} target="_blank" rel="noreferrer">Writing band descriptors — IELTS</a></div>
        : <label>Tiêu chí phản hồi bài bổ trợ <span className="optional">mỗi mục một dòng</span><textarea rows={3} value={c.assessmentCriteria.join('\n')} onChange={e => content({ assessmentCriteria: lines(e.target.value) })} /></label>}
      <section className="writing-form-section"><div className="writing-subheading"><h3>Bảng từ gợi ý</h3><button type="button" className="button secondary" disabled={c.vocabulary.length >= 50} onClick={() => setValue(v => ({ ...v, supportLevel: 'GUIDED', content: { ...v.content, vocabulary: [...v.content.vocabulary, { term: '', meaning: '', collocation: '', example: '', level: '' }] } }))}>Thêm từ / cụm</button></div>
        <p className="muted">Chọn từ phù hợp đề, kiểm tra nghĩa và ví dụ trước khi xuất bản. Đây là hỗ trợ luyện tập.</p>
        {c.vocabulary.map((w, n) => <div className="writing-item" key={n}><div className="writing-subheading"><h4>Từ / cụm {n + 1}</h4><button type="button" className="text-button" onClick={() => content({ vocabulary: c.vocabulary.filter((_, index) => index !== n) })}>Xóa từ</button></div><div className="learning-columns">
          <label>Từ / cụm<input maxLength={150} value={w.term} onChange={e => word(n, { term: e.target.value })} /></label><label>Nghĩa trong ngữ cảnh<input maxLength={500} value={w.meaning} onChange={e => word(n, { meaning: e.target.value })} /></label>
          <label>Kết hợp từ<input maxLength={300} value={w.collocation} onChange={e => word(n, { collocation: e.target.value })} /></label><label>Mức từ (CEFR, tùy chọn)<input maxLength={30} value={w.level} onChange={e => word(n, { level: e.target.value })} /></label>
        </div><label>Câu ví dụ<textarea rows={2} maxLength={1000} value={w.example} onChange={e => word(n, { example: e.target.value })} /></label></div>)}
      </section>
      {!main && <section className="writing-form-section"><div className="writing-subheading"><h3>Các bước luyện bổ trợ</h3><button type="button" className="button secondary" disabled={c.activities.length >= 20} onClick={() => content({ activities: [...c.activities, newStep(targetStep[c.activity as Exclude<ActivityKind, 'MAIN'>], 'GUIDED')] })}>Thêm hoạt động</button></div>
        <p className="muted">Soạn bước có hướng dẫn, rồi một bước vận dụng vào tình huống mới. Gợi ý đáp án chỉ dành cho giảng viên.</p>
        {c.activities.map((s, n) => {
          const sample = t.sampleAnswers.find(a => a.activityId === s.id);
          return <div className="writing-item" key={s.id}><div className="writing-subheading"><h4>Hoạt động {n + 1}</h4><button type="button" className="text-button" onClick={() => setValue(v => ({ ...v, content: { ...v.content, activities: v.content.activities.filter(a => a.id !== s.id) }, teaching: { ...v.teaching!, sampleAnswers: v.teaching!.sampleAnswers.filter(a => a.activityId !== s.id) } }))}>Xóa hoạt động</button></div>
            <div className="learning-columns"><label>Loại hoạt động<select value={s.type} onChange={e => step(n, { type: e.target.value as WritingStep['type'] })}>{Object.entries(stepNames).map(([type, name]) => <option key={type} value={type}>{name}</option>)}</select></label><label>Mức hỗ trợ bước<select value={s.stage} onChange={e => step(n, { stage: e.target.value as WritingStep['stage'] })}>{Object.entries(supportNames).map(([k, name]) => <option key={k} value={k}>{name}</option>)}</select></label></div>
            <label>Yêu cầu hoạt động<textarea rows={2} maxLength={2000} value={s.instruction} onChange={e => step(n, { instruction: e.target.value })} /></label>
            <label>Ngữ cảnh hoạt động<textarea rows={3} maxLength={6000} value={s.context} onChange={e => step(n, { context: e.target.value })} /></label>
            <label>Ý / ghi chú <span className="optional">mỗi ý một dòng</span><textarea rows={3} value={s.items.join('\n')} onChange={e => step(n, { items: lines(e.target.value) })} /></label>
            <label>Ví dụ hướng dẫn được cấp cho học viên<textarea rows={2} maxLength={3000} value={s.example} onChange={e => step(n, { example: e.target.value })} /></label>
            <div className="writing-private"><label>Phương án tham khảo riêng <span className="optional">mỗi phương án một dòng</span><textarea rows={3} value={sample?.acceptedExamples.join('\n') ?? ''} onChange={e => answer(s.id, { acceptedExamples: lines(e.target.value) })} /></label>
              <label>Giải thích phản hồi riêng<textarea rows={3} maxLength={5000} value={sample?.explanation ?? ''} onChange={e => answer(s.id, { explanation: e.target.value })} /></label></div>
          </div>;
        })}
      </section>}
      {c.activity === 'GRAMMAR_REPAIR' && <section className="writing-form-section"><h3>Câu lỗi và xác nhận</h3>
        <label>Nguồn câu<select value={c.grammarSourceKind} onChange={e => content({ grammarSourceKind: e.target.value as WritingContent['grammarSourceKind'], teacherConfirmed: false })}><option value="DEMO_AUTHORING">Câu tự biên soạn cho demo</option><option value="TEACHER_SUPPLIED">Câu do giảng viên cung cấp</option></select></label>
        <label>Câu lỗi gốc<textarea rows={2} maxLength={3000} value={c.originalSentence} onChange={e => content({ originalSentence: e.target.value, teacherConfirmed: false })} /></label>
        <label>Ngữ cảnh / ý định gốc<textarea rows={3} maxLength={3000} value={c.originalMeaning} onChange={e => content({ originalMeaning: e.target.value, teacherConfirmed: false })} /></label>
        <label className="learning-checkbox"><input type="checkbox" checked={c.teacherConfirmed} onChange={e => content({ teacherConfirmed: e.target.checked })} />Tôi đã kiểm tra câu lỗi và ngữ cảnh gốc</label>
      </section>}
      <section className="writing-form-section writing-private"><h3>Tham khảo riêng cho giảng viên</h3>
        <label>Bài viết mẫu / phương án tham khảo<textarea rows={7} maxLength={15000} value={t.modelResponse} onChange={e => setValue(v => ({ ...v, teaching: { ...v.teaching!, modelResponse: e.target.value } }))} /></label>
        <label>Ghi chú riêng của giảng viên<textarea rows={3} maxLength={10000} value={t.teacherNotes} onChange={e => setValue(v => ({ ...v, teaching: { ...v.teaching!, teacherNotes: e.target.value } }))} /></label>
      </section>
      <div className="learning-columns"><label>Link nguồn tham khảo<input type="url" maxLength={2000} value={value.source?.url ?? ''} onChange={e => setValue(v => ({ ...v, source: { ...v.source!, url: e.target.value } }))} /></label><label>Ghi chú nguồn được cấp cho học viên<input maxLength={2000} value={value.source?.note ?? ''} onChange={e => setValue(v => ({ ...v, source: { ...v.source!, note: e.target.value } }))} /></label></div>
      <div className="learning-actions"><button type="button" className="button secondary" onClick={onCancel}>Hủy</button><button type="submit" className="button primary">{busy ? 'Đang lưu…' : uploading ? 'Đang tải hình…' : 'Lưu bản nháp'}</button></div>
    </fieldset></form>
  </section>;
}
