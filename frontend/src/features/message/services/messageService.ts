import api from '../../../lib/axios';
import type { ConversationItem, Message, SearchUser } from '../types';

export const messageService = {
  getConversations: async (): Promise<ConversationItem[]> => {
    const response = await api.get('/conversations');
    return response.data.data;
  },

  createConversation: async (participantId: string): Promise<ConversationItem> => {
    const response = await api.post('/conversations', { participantId });
    return response.data.data;
  },

  getMessages: async (conversationId: string): Promise<Message[]> => {
    const response = await api.get(`/conversations/${conversationId}/messages`);
    return response.data.data;
  },

  sendMessage: async (
    conversationId: string,
    payload: string | { content?: string; sharedPostId?: string }
  ): Promise<Message> => {
    const requestBody =
      typeof payload === 'string'
        ? { content: payload }
        : {
            content: payload.content ?? '',
            sharedPostId: payload.sharedPostId,
          };
    const response = await api.post(`/conversations/${conversationId}/messages`, requestBody);
    return response.data.data;
  },

  markConversationRead: async (conversationId: string): Promise<void> => {
    await api.post(`/conversations/${conversationId}/read`);
  },

  getSearchCandidates: async (query?: string): Promise<SearchUser[]> => {
    const trimmed = query?.trim() ?? '';
    const response = await api.get('/conversations/search-candidates', {
      params: {
        q: trimmed || undefined,
        limit: 30,
      },
    });

    return response.data?.data ?? [];
  },

  searchUsers: async (query: string): Promise<SearchUser[]> => {
    const trimmed = query.trim();
    if (!trimmed) {
      return [];
    }

    try {
      const response = await api.get('/search/users', { params: { q: trimmed } });
      return response.data?.data ?? [];
    } catch {
      const fallback = await api.get('/search', { params: { q: trimmed } });
      return fallback.data?.data?.users ?? [];
    }
  },
};
