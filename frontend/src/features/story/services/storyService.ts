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
}

export const storyService = {
  createStory: async (data: { mediaUrl: string; mediaType: string }) => {
    const response = await api.post('/stories', data);
    return response.data.data;
  },

  getStoriesFeed: async () => {
    const response = await api.get('/stories/feed');
    return response.data.data;
  },

  getGroupedStories: async () => {
    const response = await api.get('/stories/grouped');
    return response.data.data;
  }
};
