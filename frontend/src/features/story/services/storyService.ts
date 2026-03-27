import api from '../../../lib/axios';

export interface Story {
  id: string;
  userId: string;
  username: string;
  userAvatarUrl: string;
  mediaUrl: string;
  mediaType: 'IMAGE' | 'VIDEO';
  createdAt: string;
  expiresAt: string;
  likeCount: number;
  replyCount: number;
  shareCount: number;
  likedByCurrentUser: boolean;
}

export const storyService = {
  createStory: async (data: { mediaUrl: string; mediaType: string }) => {
    const response = await api.post('/stories', data);
    return response.data.data as Story;
  },

  getStoriesFeed: async () => {
    const response = await api.get('/stories/feed');
    return response.data.data as Story[];
  },

  getGroupedStories: async () => {
    const response = await api.get('/stories/grouped');
    return response.data.data as Record<string, Story[]>;
  },

  likeStory: async (storyId: string) => {
    const response = await api.post('/stories/' + storyId + '/like');
    return response.data.data as Story;
  },

  unlikeStory: async (storyId: string) => {
    const response = await api.delete('/stories/' + storyId + '/like');
    return response.data.data as Story;
  },

  replyStory: async (storyId: string, content: string) => {
    const response = await api.post('/stories/' + storyId + '/replies', { content });
    return response.data.data as Story;
  },

  shareStory: async (storyId: string) => {
    const response = await api.post('/stories/' + storyId + '/share');
    return response.data.data as Story;
  }
};
