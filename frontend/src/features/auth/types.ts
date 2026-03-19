export interface User {
  id: number;
  username: string;
  fullName: string;
  avatarUrl: string;
  bio: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
}
