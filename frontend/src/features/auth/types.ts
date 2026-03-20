export interface User {
  id: string;
  username: string;
  fullName: string;
  avatarUrl: string;
  bio: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
}
