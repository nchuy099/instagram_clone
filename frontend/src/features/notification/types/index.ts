export const NotificationType = {
  FOLLOW: 'FOLLOW',
  POST_LIKE: 'POST_LIKE',
  POST_COMMENT: 'POST_COMMENT',
} as const;

export type NotificationType = (typeof NotificationType)[keyof typeof NotificationType];

export interface NotificationItem {
  id: string;
  type: NotificationType;
  createdAt: string;
  isRead: boolean;
  readAt?: string | null;
  actorId: string;
  actorUsername: string;
  actorAvatarUrl?: string | null;
  postId?: string | null;
  commentId?: string | null;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface NotificationEvent {
  type: 'NOTIFICATION_CREATED' | string;
  notification?: NotificationItem;
}
