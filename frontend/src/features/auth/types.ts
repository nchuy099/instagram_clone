export interface User {
  id: string;
  username: string;
  fullName: string;
  avatarUrl: string;
  bio: string;
  websiteUrl?: string;
  isPrivate?: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
}
