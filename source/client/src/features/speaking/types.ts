import type { ResourceInput as SharedInput, ResourceVersion as SharedVersion,
  LearningResource as SharedResource, AssignedDetail as SharedAssigned } from '../learning/types';

export interface SpeakingItem { text: string; ipa: string; audioAssetId: number | null }
export interface SpeakingContent {
  taskType: 'MAIN' | 'DEVELOPMENT' | 'PRONUNCIATION'; part: number; prompt: string; instructions: string;
  preparationSeconds: number; speakingSeconds: number; checklist: string[]; phonemes: string[];
  items: SpeakingItem[]; assessmentCriteria: string[];
}
export interface Teaching { outlineExamples: string[]; teacherNotes: string }
export type ResourceInput = SharedInput<SpeakingContent, Teaching>;
export type ResourceVersion = SharedVersion<SpeakingContent, Teaching>;
export type LearningResource = SharedResource<SpeakingContent, Teaching>;
export type AssignedDetail = SharedAssigned<SpeakingContent, Teaching>;
