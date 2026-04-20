import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import Sidebar from './Sidebar';
import { notificationService } from '../../features/notification/services/notificationService';
import type { NotificationEvent, NotificationItem, NotificationType } from '../../features/notification/types';

const GLOBAL_NOTIFICATION_CREATED_EVENT = 'app:notification-created';
const GLOBAL_NOTIFICATION_MARK_READ_EVENT = 'app:notification-mark-read';

const toWebSocketUrl = () => {
  const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
  const rootUrl = apiUrl.endsWith('/api') ? apiUrl.slice(0, -4) : apiUrl;

  if (rootUrl.startsWith('https://')) {
    return `${rootUrl.replace('https://', 'wss://')}/ws`;
  }

  return `${rootUrl.replace('http://', 'ws://')}/ws`;
};

const getNotificationMessage = (type: NotificationType) => {
  if (type === 'FOLLOW') {
    return 'started following you.';
  }

  if (type === 'POST_COMMENT') {
    return 'commented on your post.';
  }

  return 'liked your post.';
};

export default function MainLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const isMessagesRoute = location.pathname.startsWith('/messages');
  const [unreadCount, setUnreadCount] = useState(0);
  const hasShownSocketErrorToastRef = useRef(false);
  const initialTitleRef = useRef(document.title.replace(/^\(\d+\)\s+/, ''));

  const wsEnabled = useMemo(() => Boolean(localStorage.getItem('accessToken')), []);

  useEffect(() => {
    const baseTitle = initialTitleRef.current || 'Instagram';
    document.title = unreadCount > 0 ? `(${unreadCount}) ${baseTitle}` : baseTitle;
  }, [unreadCount]);

  useEffect(() => {
    if (!wsEnabled) {
      return;
    }

    const loadUnreadCount = async () => {
      try {
        const page = await notificationService.getNotifications(0, 1, true);
        setUnreadCount(page.totalElements);
      } catch {
        // Best effort only.
      }
    };

    void loadUnreadCount();
  }, [wsEnabled]);

  useEffect(() => {
    const markReadHandler = (event: Event) => {
      const customEvent = event as CustomEvent<{ count?: number }>;
      const delta = customEvent.detail?.count ?? 1;
      setUnreadCount((prev) => Math.max(0, prev - delta));
    };

    window.addEventListener(GLOBAL_NOTIFICATION_MARK_READ_EVENT, markReadHandler as EventListener);
    return () => {
      window.removeEventListener(GLOBAL_NOTIFICATION_MARK_READ_EVENT, markReadHandler as EventListener);
    };
  }, []);

  useEffect(() => {
    if (!wsEnabled) {
      return;
    }

    const token = localStorage.getItem('accessToken');
    if (!token) {
      return;
    }

    const client = new Client({
      brokerURL: toWebSocketUrl(),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 3000,
      onStompError: (frame) => {
        console.error('Global notifications STOMP error', frame.headers['message'], frame.body);
        if (!hasShownSocketErrorToastRef.current) {
          toast.error('Realtime notifications disconnected. Please refresh this page.');
          hasShownSocketErrorToastRef.current = true;
        }
      },
      onWebSocketError: (event) => {
        console.error('Global notifications websocket error', event);
        if (!hasShownSocketErrorToastRef.current) {
          toast.error('Unable to connect realtime notifications.');
          hasShownSocketErrorToastRef.current = true;
        }
      },
      onConnect: () => {
        hasShownSocketErrorToastRef.current = false;
        client.subscribe('/user/queue/notifications', (frame) => {
          try {
            const event = JSON.parse(frame.body) as NotificationEvent;
            if (event.type !== 'NOTIFICATION_CREATED' || !event.notification) {
              return;
            }

            const notification = event.notification as NotificationItem;
            window.dispatchEvent(
              new CustomEvent<NotificationItem>(GLOBAL_NOTIFICATION_CREATED_EVENT, { detail: notification })
            );
            setUnreadCount((prev) => prev + 1);
            toast.info(`${notification.actorUsername} ${getNotificationMessage(notification.type)}`);
          } catch {
            // Ignore malformed websocket payloads.
          }
        });
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [wsEnabled]);

  return (
    <div className={`min-h-screen bg-white ${isMessagesRoute ? 'md:pl-20' : 'md:pl-72'}`}>
      <Sidebar />
      <main className="min-h-screen overflow-y-auto">
        {children}
      </main>
    </div>
  );
}
