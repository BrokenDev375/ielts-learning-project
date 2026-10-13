export type Skill = 'READING' | 'LISTENING' | 'SPEAKING' | 'WRITING';
export interface ContentSource { url: string; note: string }
export interface ResourceInput<Content = unknown, Teaching = unknown> {
  title: string; skill: Skill; resourceKind: 'MAIN' | 'REMEDIAL'; activityType: string; topic: string;
  levelFramework: 'CEFR' | 'IELTS_PRACTICE' | 'RAW' | null; levelLabel: string;
  supportLevel: 'MODEL' | 'GUIDED' | 'INDEPENDENT' | 'TRANSFER';
  content: Content; teaching?: Teaching; source?: ContentSource;
}
export interface ResourceVersion<Content = unknown, Teaching = unknown> extends ResourceInput<Content, Teaching> {
  id: number; resourceId: number; versionNo: number; status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'; publishedAt?: string;
}
export interface LearningResource<Content = unknown, Teaching = unknown> { id: number; skill: Skill; version: ResourceVersion<Content, Teaching> }
export interface Assignment {
  id: number; classroomId: number; classroomName: string; resourceVersionId: number; title: string;
  skill: Skill; purpose: string; status: string; opensAt: string | null; dueAt: string | null;
  allowLate: boolean; maxAttempts: number; feedbackPolicy: string; recipientCount: number; createdAt: string;
}
export interface AssignedDetail<Content = unknown, Teaching = unknown> extends Omit<ResourceVersion<Content, Teaching>, 'id'> {
  assignmentId: number; resourceVersionId: number; dueAt: string | null; questions: unknown[]; assignment: Assignment;
}
export interface Asset { id: number; originalName: string; mimeType: string; sizeBytes: number; url: string }
export const skillNames: Record<Skill, string> = { READING: 'Reading', LISTENING: 'Listening', SPEAKING: 'Speaking', WRITING: 'Writing' };
