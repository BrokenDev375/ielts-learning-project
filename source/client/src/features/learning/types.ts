export interface SpeakingItem { text: string; ipa: string; audioAssetId: number | null }
export interface SpeakingContent {
  taskType: 'MAIN' | 'DEVELOPMENT' | 'PRONUNCIATION'; part: number; prompt: string; instructions: string;
  preparationSeconds: number; speakingSeconds: number; checklist: string[]; phonemes: string[];
  items: SpeakingItem[]; assessmentCriteria: string[];
}
export interface Teaching { outlineExamples: string[]; teacherNotes: string }
export interface ContentSource { url: string; note: string }
export interface ResourceInput {
  title: string; skill: 'SPEAKING'; resourceKind: 'MAIN' | 'REMEDIAL';
  activityType: 'SPEAKING_RECORDING' | 'OUTLINE' | 'PRONUNCIATION'; topic: string;
  levelFramework: 'CEFR' | 'IELTS_PRACTICE' | 'RAW' | null; levelLabel: string;
  supportLevel: 'MODEL' | 'GUIDED' | 'INDEPENDENT' | 'TRANSFER';
  content: SpeakingContent; teaching?: Teaching; source?: ContentSource;
}
export interface ResourceVersion extends ResourceInput {
  id: number; resourceId: number; versionNo: number; status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'; publishedAt?: string;
}
export interface LearningResource { id: number; skill: string; version: ResourceVersion }
export interface Assignment {
  id: number; classroomId: number; classroomName: string; resourceVersionId: number; title: string;
  skill: string; purpose: string; status: string; opensAt: string | null; dueAt: string | null;
  allowLate: boolean; maxAttempts: number; feedbackPolicy: string; recipientCount: number; createdAt: string;
}
export interface AssignedDetail extends Omit<ResourceVersion, 'id'> {
  assignmentId: number; resourceVersionId: number; dueAt: string | null; questions: unknown[]; assignment: Assignment;
}
export interface Asset { id: number; originalName: string; mimeType: string; sizeBytes: number; url: string }
