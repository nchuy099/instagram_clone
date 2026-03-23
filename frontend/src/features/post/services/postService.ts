import api from '../../../lib/axios';
import type { Post, CreatePostRequest, Comment } from '../types';

export const postService = {
  createPost: async (data: CreatePostRequest): Promise<Post> => {
    const response = await api.post('/posts', data);
    return response.data.data;
  },

  getPost: async (postId: string): Promise<Post> => {
    const response = await api.get(`/posts/${postId}`);
    return response.data.data;
  },

  updatePost: async (postId: string, data: Partial<CreatePostRequest>): Promise<Post> => {
    const response = await api.patch(`/posts/${postId}`, data);
    return response.data.data;
  },

  deletePost: async (postId: string): Promise<void> => {
    await api.delete(`/posts/${postId}`);
  },

  likePost: async (postId: string): Promise<void> => {
    await api.post(`/posts/${postId}/like`);
  },

  unlikePost: async (postId: string): Promise<void> => {
    await api.delete(`/posts/${postId}/like`);
  },

  savePost: async (postId: string): Promise<void> => {
    await api.post(`/posts/${postId}/save`);
  },

  unsavePost: async (postId: string): Promise<void> => {
    await api.delete(`/posts/${postId}/save`);
  },
};

export const commentService = {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getComments: async (postId: string, page = 0, size = 20): Promise<any> => {
    const response = await api.get(`/posts/${postId}/comments`, {
      params: { page, size },
    });
    return response.data.data;
  },

  createComment: async (postId: string, content: string, parentCommentId?: string): Promise<Comment> => {
    const response = await api.post(`/posts/${postId}/comments`, {
      content,
      parentCommentId,
    });
    return response.data.data;
  },

  deleteComment: async (commentId: string): Promise<void> => {
    await api.delete(`/comments/${commentId}`);
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getReplies: async (commentId: string, page = 0, size = 10): Promise<any> => {
    const response = await api.get(`/comments/${commentId}/replies`, {
      params: { page, size },
    });
    return response.data.data;
  },
};
