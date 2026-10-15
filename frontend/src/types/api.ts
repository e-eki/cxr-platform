// ─── Auth ─────────────────────────────────────────────────
export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}

// ─── Cases ────────────────────────────────────────────────
export type CaseStatus =
  | 'UPLOADED'
  | 'PROCESSING'
  | 'READY_FOR_REVIEW'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'FAILED';

export interface Prediction {
  className: string;
  confidence: number;
  xMin: number;
  yMin: number;
  xMax: number;
  yMax: number;
}

export interface CaseResponse {
  id: string;
  patientId: string;
  imageKey: string;
  status: CaseStatus;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
  predictionsCount: number;
  predictions: Prediction[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export type ReviewDecision = 'CONFIRMED' | 'REJECTED';

export interface ReviewRequest {
  decision: ReviewDecision;
  comment?: string;
}

export interface ReviewResponse {
  id: string;
  reviewerId: string;
  decision: ReviewDecision;
  comment: string | null;
  createdAt: string;
}