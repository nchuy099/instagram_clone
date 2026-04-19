export const MediaType = {
  IMAGE: 'IMAGE',
  VIDEO: 'VIDEO'
} as const;

export type MediaType = (typeof MediaType)[keyof typeof MediaType]

export interface PostMedia {
  id: string;
  url: string;
  thumbnailUrl?: string;
  type: MediaType;
  orderIndex: number;
}

export interface Post {
  id: string;
  caption: string;
  location?: string;
  user: {
    id: string;
    username: string;
    avatarUrl?: string;
  };
  media: PostMedia[];
  likeCount: number;
  commentCount: number;
  isLiked: boolean;
  isSaved: boolean;
  isFollowing: boolean;
  allowComments: boolean;
  createdAt: string;
}

export interface CreatePostRequest {
  caption?: string;
  location?: string;
  allowComments: boolean;
  media: {
    url: string;
    thumbnailUrl?: string;
    type: MediaType;
    orderIndex: number;
  }[];
}

export interface Comment {
  id: string;
  content: string;
  user: {
    id: string;
    username: string;
    avatarUrl?: string;
  };
  parentCommentId?: string | null;
  createdAt: string;
  replyCount: number;
}
