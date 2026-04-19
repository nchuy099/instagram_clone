export interface ConversationParticipant {
  id: string;
  username: string;
  avatarUrl?: string;
}

export interface ConversationItem {
  id: string;
  participants: ConversationParticipant[];
  lastMessagePreview?: string | null;
  lastMessageAt?: string | null;
  unreadCount: number;
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  senderUsername: string;
  senderAvatarUrl?: string;
  content: string;
  sharedPostId?: string;
  sharedPost?: SharedPostPreview;
  sharedStoryId?: string;
  sharedStory?: SharedStoryPreview;
  createdAt: string;
}

export interface SharedPostPreview {
  postId: string;
  ownerUsername: string;
  ownerAvatarUrl?: string;
  mediaUrl?: string;
  mediaType?: 'IMAGE' | 'VIDEO' | string;
  caption: string;
}

export interface SharedStoryPreview {
  storyId: string;
  ownerUsername: string;
  ownerAvatarUrl?: string;
  mediaUrl?: string;
  mediaType?: 'IMAGE' | 'VIDEO' | string;
  expiresAt?: string;
  expired?: boolean;
}

export interface MessageEvent {
  type: 'MESSAGE_CREATED' | 'MESSAGE_READ';
  conversationId: string;
  message?: Message;
  readByUserId?: string;
  readAt?: string;
}

export interface SearchUser {
  id: string;
  username: string;
  fullName?: string;
  avatarUrl?: string;
}
