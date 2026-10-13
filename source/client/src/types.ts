export type Role = 'TEACHER' | 'STUDENT';
export interface User { id: number; email: string; fullName: string; role: Role; createdAt: string }
export interface Classroom {
  id: number; teacherId: number; teacherName: string; name: string; description: string;
  joinCode: string | null; memberCount: number; createdAt: string; joinedAt: string | null;
}
export interface Member { id: number; fullName: string; email: string; joinedAt: string }
export interface JoinResult { classroom: Classroom; alreadyJoined: boolean }
