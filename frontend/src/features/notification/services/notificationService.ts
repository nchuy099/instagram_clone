import api from '../../../lib/axios';
import type { NotificationItem, PagedResponse } from '../types';

export const notificationService = {
  getNotifications: async (page = 0, size = 20, unreadOnly = true): Promise<PagedResponse<NotificationItem>> => {
    const response = await api.get('/notifications', {
      params: { page, size, unreadOnly },
    });

    return response.data.data;
  },

  markAsRead: async (notificationId: string): Promise<void> => {
    await api.patch(`/notifications/${notificationId}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await api.patch('/notifications/read-all');
  },
};
