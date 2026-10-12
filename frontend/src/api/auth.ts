import { apiClient, tokenStorage } from './client';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/api';

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', request);
  tokenStorage.set(data.token);
  return data;
}

export async function register(request: RegisterRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/register', request);
  tokenStorage.set(data.token);
  return data;
}

export function logout(): void {
  tokenStorage.clear();
}