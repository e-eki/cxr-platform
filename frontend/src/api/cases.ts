import { apiClient } from './client';
import type { CaseResponse, CaseStatus, Page, ReviewRequest, ReviewResponse } from '../types/api';

export async function listCases(params: {
  status?: CaseStatus;
  page?: number;
  size?: number;
}): Promise<Page<CaseResponse>> {
  const { data } = await apiClient.get<Page<CaseResponse>>('/cases', { params });
  return data;
}

export async function getCase(id: string): Promise<CaseResponse> {
  const { data } = await apiClient.get<CaseResponse>(`/cases/${id}`);
  return data;
}

export async function createCase(patientId: string, file: File): Promise<CaseResponse> {
  const formData = new FormData();
  formData.append('patientId', patientId);
  formData.append('file', file);

  const { data } = await apiClient.post<CaseResponse>('/cases', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

/** Прямая ссылка на изображение кейса (для <img src>). */
export function getCaseImageUrl(id: string): string {
  return `/api/cases/${id}/image`;
}

export async function reviewCase(
  id: string,
  request: ReviewRequest
): Promise<ReviewResponse> {
  const { data } = await apiClient.post<ReviewResponse>(
    `/cases/${id}/review`,
    request
  );
  return data;
}