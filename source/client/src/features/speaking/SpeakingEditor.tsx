import { useState } from 'react';
import type { FormEvent } from 'react';
import { api, errorMessage } from '../../api';
import type { Asset } from '../learning/types';
import type { LearningResource, ResourceInput, SpeakingContent } from './types';

export const taskNames = { MAIN: 'Bài nói chính / test', DEVELOPMENT: 'Phát triển câu trả lời', PRONUNCIATION: 'Phát âm theo âm' };
export const criteria = ['Fluency and coherence', 'Lexical resource', 'Grammatical range and accuracy', 'Pronunciation'];
const lines = (text: string) => text.split('\n').map(s => s.trim()).filter(Boolean);
const initial = (): ResourceInput => ({
  title: '', skill: 'SPEAKING', resourceKind: 'MAIN', activityType: 'SPEAKING_RECORDING', topic: '',
  levelFramework: 'IELTS_PRACTICE', levelLabel: '', supportLevel: 'INDEPENDENT',
  content: { taskType: 'MAIN', part: 2, prompt: '', instructions: '', preparationSeconds: 60, speakingSeconds: 120,
    checklist: [], phonemes: [], items: [], assessmentCriteria: criteria },
  teaching: { outlineExamples: [], teacherNotes: '' }, source: { url: '', note: '' },
});
function inputOf(resource?: LearningResource): ResourceInput {
  if (!resource) return initial();
  const { title, skill, resourceKind, activityType, topic, levelFramework, levelLabel, supportLevel, content, teaching, source } = resource.version;
  return structuredClone({ title, skill, resourceKind, activityType, topic: topic ?? '', levelFramework, levelLabel: levelLabel ?? '',
    supportLevel, content, teaching: teaching ?? { outlineExamples: [], teacherNotes: '' }, source: source ?? { url: '', note: '' } });
}
export default function SpeakingEditor({ resource, onSaved, onCancel }: {
  resource?: LearningResource; onSaved: (resource: LearningResource) => void; onCancel: () => void;
}) {
  const [value, setValue] = useState(() => inputOf(resource));
  const [checklist, setChecklist] = useState(value.content.checklist.join('\n'));
  const [rubric, setRubric] = useState(value.content.assessmentCriteria.join('\n'));
  const [phonemes, setPhonemes] = useState(value.content.phonemes.join(', '));
  const [examples, setExamples] = useState(value.teaching?.outlineExamples.join('\n') ?? '');
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState<number | null>(null);
  const [error, setError] = useState('');
  function content(patch: Partial<SpeakingContent>) { setValue(v => ({ ...v, content: { ...v.content, ...patch } })); }
  function task(taskType: SpeakingContent['taskType']) {
    setValue(v => ({ ...v, resourceKind: taskType === 'MAIN' ? 'MAIN' : 'REMEDIAL',
      activityType: taskType === 'MAIN' ? 'SPEAKING_RECORDING' : taskType === 'DEVELOPMENT' ? 'OUTLINE' : 'PRONUNCIATION',
      supportLevel: taskType === 'MAIN' ? 'INDEPENDENT' : 'GUIDED', content: { ...v.content, taskType, part: taskType === 'MAIN' ? 2 : 0 } }));
  }
  async function upload(index: number, file?: File) {
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) { setError('Audio cần nhỏ hơn hoặc bằng 5 MB.'); return; }
    setUploading(index); setError('');
    try {
      const data = new FormData(); data.append('file', file);
      const asset = await api<Asset>('/api/assets', 'POST', data);
      setValue(v => ({ ...v, content: { ...v.content, items: v.content.items.map((item, n) => n === index ? { ...item, audioAssetId: asset.id } : item) } }));
    } catch (ex) { setError(errorMessage(ex)); } finally { setUploading(null); }
  }
  async function save(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    const payload = { ...value, title: value.title.trim(),
      content: { ...value.content, checklist: lines(checklist), assessmentCriteria: lines(rubric),
        phonemes: phonemes.split(',').map(s => s.trim()).filter(Boolean) },
      teaching: { ...value.teaching!, outlineExamples: lines(examples) } };
    try { onSaved(await api<LearningResource>(resource ? '/api/resources/' + resource.id + '/draft' : '/api/resources', resource ? 'PUT' : 'POST', payload)); }
    catch (ex) { setError(errorMessage(ex)); } finally { setBusy(false); }
  }
  return <section className="learning-panel">
    <div className="learning-section-heading"><div><span className="eyebrow">SOẠN BÀI SPEAKING</span><h2>{resource ? 'Chỉnh sửa bài nói' : 'Tạo bài nói mới'}</h2></div><button className="button secondary" onClick={onCancel} disabled={busy || uploading !== null}>Quay lại</button></div>
    {resource?.version.status === 'PUBLISHED' && <p className="learning-note">Lưu thay đổi sẽ tạo bản nháp mới. Học viên đã được giao bài tiếp tục nhận phiên bản trước.</p>}
    {error && <div className="error-box" role="alert">{error}</div>}
    <form onSubmit={save}><fieldset disabled={busy || uploading !== null}>
      <div className="learning-columns"><label>Tên bài<input required maxLength={200} value={value.title} onChange={e => setValue(v => ({ ...v, title: e.target.value }))} /></label>
        <label>Dạng bài<select value={value.content.taskType} onChange={e => task(e.target.value as SpeakingContent['taskType'])}>{Object.entries(taskNames).map(([id, name]) => <option key={id} value={id}>{name}</option>)}</select></label>
        <label>Chủ đề<input maxLength={150} value={value.topic} onChange={e => setValue(v => ({ ...v, topic: e.target.value }))} /></label>
        <label>Phần Speaking<select value={value.content.part} onChange={e => content({ part: Number(e.target.value) })}><option value={0}>Tình huống / bài luyện riêng</option>{[1, 2, 3].map(p => <option key={p} value={p}>Part {p}</option>)}</select></label>
      </div>
      <label>Đề bài / tình huống<textarea rows={4} maxLength={15000} value={value.content.prompt} onChange={e => content({ prompt: e.target.value })} /></label>
      <label>Hướng dẫn cho học viên<textarea rows={3} maxLength={10000} value={value.content.instructions} onChange={e => content({ instructions: e.target.value })} /></label>
      <div className="learning-columns"><label>Thời gian chuẩn bị (giây)<input type="number" min={0} max={600} required value={value.content.preparationSeconds} onChange={e => content({ preparationSeconds: Number(e.target.value) })} /></label>
        <label>Thời gian nói (giây)<input type="number" min={1} max={1800} required value={value.content.speakingSeconds} onChange={e => content({ speakingSeconds: Number(e.target.value) })} /></label>
        <label>Khung mức độ<select value={value.levelFramework ?? ''} onChange={e => setValue(v => ({ ...v, levelFramework: (e.target.value || null) as ResourceInput['levelFramework'] }))}><option value="">Không đặt</option><option value="IELTS_PRACTICE">IELTS luyện tập</option><option value="CEFR">CEFR</option><option value="RAW">Điểm thô</option></select></label>
        <label>Mức luyện tham khảo<input maxLength={32} value={value.levelLabel} placeholder="Ví dụ: B1 nếu chọn CEFR" onChange={e => setValue(v => ({ ...v, levelLabel: e.target.value }))} /></label>
        <label>Mức hỗ trợ<select value={value.supportLevel} onChange={e => setValue(v => ({ ...v, supportLevel: e.target.value as ResourceInput['supportLevel'] }))}><option value="MODEL">Có mẫu hướng dẫn</option><option value="GUIDED">Có hướng dẫn từng bước</option><option value="INDEPENDENT">Làm độc lập</option><option value="TRANSFER">Vận dụng sang bài mới</option></select></label>
      </div>
      <label>Checklist cần đạt <span className="optional">mỗi dòng một mục</span><textarea rows={3} value={checklist} onChange={e => setChecklist(e.target.value)} /></label>
      <label>Tiêu chí đánh giá <span className="optional">mỗi dòng một tiêu chí</span><textarea rows={4} value={rubric} onChange={e => setRubric(e.target.value)} /></label>
      {value.content.taskType === 'DEVELOPMENT' && <label>Outline tham khảo cho giảng viên / AI <span className="optional">mỗi dòng một outline; không phải đáp án duy nhất</span><textarea rows={4} value={examples} onChange={e => setExamples(e.target.value)} /></label>}
      {value.content.taskType === 'PRONUNCIATION' && <section className="learning-phonetics"><h3>Từ và câu luyện phát âm</h3>
        <label>Âm mục tiêu <span className="optional">ngăn cách bằng dấu phẩy</span><input maxLength={600} placeholder="/θ/, /s/" value={phonemes} onChange={e => setPhonemes(e.target.value)} /></label>
        {value.content.items.map((item, index) => <div className="learning-word" key={index}><h4>Từ / câu {index + 1}</h4>
          <label>Nội dung từ / câu<input maxLength={1000} value={item.text} onChange={e => content({ items: value.content.items.map((i, n) => n === index ? { ...i, text: e.target.value } : i) })} /></label>
          <label>Phiên âm<input maxLength={300} value={item.ipa} onChange={e => content({ items: value.content.items.map((i, n) => n === index ? { ...i, ipa: e.target.value } : i) })} /></label>
          <label>Audio mẫu (WAV, MP3, OGG; tối đa 5 MB)<input type="file" accept=".wav,.mp3,.ogg,audio/wav,audio/mpeg,audio/ogg" onChange={e => void upload(index, e.target.files?.[0])} /></label>
          {uploading === index && <p className="muted" role="status">Đang tải audio…</p>}
          {item.audioAssetId && <audio controls preload="none" src={'/api/assets/' + item.audioAssetId} />}
          <button type="button" className="text-button danger" onClick={() => content({ items: value.content.items.filter((_, n) => n !== index) })}>Xóa từ / câu {index + 1}</button>
        </div>)}
        <button type="button" className="button secondary" disabled={value.content.items.length >= 100} onClick={() => content({ items: [...value.content.items, { text: '', ipa: '', audioAssetId: null }] })}>Thêm từ / câu</button>
      </section>}
      <label>Ghi chú riêng của giảng viên<textarea rows={3} maxLength={10000} value={value.teaching?.teacherNotes ?? ''} onChange={e => setValue(v => ({ ...v, teaching: { ...v.teaching!, teacherNotes: e.target.value } }))} /></label>
      <div className="learning-columns"><label>Link nguồn tham khảo<input type="url" maxLength={2000} value={value.source?.url ?? ''} onChange={e => setValue(v => ({ ...v, source: { ...v.source!, url: e.target.value } }))} /></label>
        <label>Ghi chú nguồn<input maxLength={2000} value={value.source?.note ?? ''} onChange={e => setValue(v => ({ ...v, source: { ...v.source!, note: e.target.value } }))} /></label></div>
      <div className="learning-actions"><button type="button" className="button secondary" onClick={onCancel}>Hủy</button><button className="button primary" type="submit">{busy ? 'Đang lưu…' : 'Lưu bản nháp'}</button></div>
    </fieldset></form>
  </section>;
}
