import type { ResourceInput, ResourceVersion, LearningResource, AssignedDetail } from '../learning/types';
export type Task = 'TASK_1' | 'TASK_2';
export type ActivityKind = 'MAIN' | 'OVERVIEW_SELECTION' | 'DESCRIPTIVE_LANGUAGE' | 'TOPIC_VOCABULARY' | 'IDEA_LINKING' | 'GRAMMAR_REPAIR';
export type StepType = 'SELECT_FEATURES' | 'COMPARE_OVERVIEWS' | 'PARAPHRASE' | 'VOCAB_CONTEXT' | 'LINK_IDEAS' | 'REPAIR_SENTENCE' | 'TRANSFER';
export interface Vocabulary { term: string; meaning: string; collocation: string; example: string; level: string }
export interface WritingStep { id: string; type: StepType; stage: 'MODEL' | 'GUIDED' | 'INDEPENDENT' | 'TRANSFER'; instruction: string; context: string; items: string[]; example: string }
export interface SampleAnswer { activityId: string; acceptedExamples: string[]; explanation: string }
export interface WritingContent {
  taskType: Task; activity: ActivityKind; prompt: string; instructions: string; minimumWords: number; writingSeconds: number;
  visualAssetId: number | null; visualType: 'NONE' | 'BAR_CHART' | 'LINE_GRAPH' | 'PIE_CHART' | 'TABLE' | 'MIXED' | 'PROCESS' | 'MAP';
  hasTimeDimension: boolean; visualUnit: string; visualGroups: string[]; visualPeriods: string[];
  checklist: string[]; assessmentCriteria: string[]; vocabulary: Vocabulary[]; activities: WritingStep[];
  originalSentence: string; originalMeaning: string; grammarSourceKind: 'DEMO_AUTHORING' | 'TEACHER_SUPPLIED'; teacherConfirmed: boolean;
}
export interface WritingTeaching { teacherNotes: string; modelResponse: string; sampleAnswers: SampleAnswer[]; rubricSourceUrl: string }
export type WritingInput = ResourceInput<WritingContent, WritingTeaching>;
export type WritingVersion = ResourceVersion<WritingContent, WritingTeaching>;
export type WritingResource = LearningResource<WritingContent, WritingTeaching>;
export type WritingAssigned = AssignedDetail<WritingContent, WritingTeaching>;
export const activityNames: Record<ActivityKind, string> = {
  MAIN: 'Bài viết chính / test', OVERVIEW_SELECTION: 'Chọn thông tin và viết overview',
  DESCRIPTIVE_LANGUAGE: 'Từ vựng và cách diễn đạt mô tả', TOPIC_VOCABULARY: 'Từ vựng theo chủ đề',
  IDEA_LINKING: 'Liên kết ý', GRAMMAR_REPAIR: 'Sửa lỗi ngữ pháp',
};
export const stepNames: Record<StepType, string> = {
  SELECT_FEATURES: 'Chọn đặc điểm nổi bật', COMPARE_OVERVIEWS: 'Đối chiếu overview', PARAPHRASE: 'Biến đổi câu mô tả',
  VOCAB_CONTEXT: 'Dùng từ theo ngữ cảnh', LINK_IDEAS: 'Nối câu / ý', REPAIR_SENTENCE: 'Sửa câu lỗi', TRANSFER: 'Vận dụng vào tình huống mới',
};
export const supportNames = { MODEL: 'Xem mẫu', GUIDED: 'Có hướng dẫn', INDEPENDENT: 'Tự luyện', TRANSFER: 'Vận dụng' } as const;
export const rubricURL = 'https://ielts.org/cdn/ielts-guides/ielts-writing-band-descriptors.pdf';
export function criteria(task: Task) { return [task === 'TASK_1' ? 'Task Achievement' : 'Task Response', 'Coherence and Cohesion', 'Lexical Resource', 'Grammatical Range and Accuracy']; }
export function allowedActivities(task: Task): ActivityKind[] { return task === 'TASK_1' ? ['MAIN', 'OVERVIEW_SELECTION', 'DESCRIPTIVE_LANGUAGE', 'IDEA_LINKING', 'GRAMMAR_REPAIR'] : ['MAIN', 'TOPIC_VOCABULARY', 'IDEA_LINKING', 'GRAMMAR_REPAIR']; }
export const targetStep: Record<Exclude<ActivityKind, 'MAIN'>, StepType> = {
  OVERVIEW_SELECTION: 'SELECT_FEATURES', DESCRIPTIVE_LANGUAGE: 'PARAPHRASE', TOPIC_VOCABULARY: 'VOCAB_CONTEXT', IDEA_LINKING: 'LINK_IDEAS', GRAMMAR_REPAIR: 'REPAIR_SENTENCE',
};
