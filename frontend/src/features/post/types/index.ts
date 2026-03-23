export enum MediaType {
  IMAGE = 'IMAGE',
  VIDEO = 'VIDEO',
}

export interface PostMedia {
  id: string;
  url: string;
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
  allowComments: boolean;
  createdAt: string;
}

export interface CreatePostRequest {
  caption?: string;
  location?: string;
  allowComments: boolean;
  media: {
    url: string;
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
  createdAt: string;
  replyCount: number;
}
