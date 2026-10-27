export interface FormCheck {
  id: string
  exerciseName: string
  /** What the player is built from; null only if the stored address cannot be read. */
  video: { id: string; startSeconds: number } | null
  note: string | null
  coachReply: string | null
  coachRepliedAt: string | null
  createdAt: string
}

export interface CreateFormCheckInput {
  exerciseName: string
  videoUrl: string
  note?: string | null
}
