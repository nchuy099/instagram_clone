import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiBell, FiCheck, FiLoader, FiUser } from 'react-icons/fi';
import { toast } from 'react-toastify';
import MainLayout from '../components/layout/MainLayout';
import { notificationService } from '../features/notification/services/notificationService';
import type {
  NotificationItem,
  NotificationType,
} from '../features/notification/types';
import PostDetailModal from '../features/post/components/PostDetailModal';

const PAGE_SIZE = 20;
const GLOBAL_NOTIFICATION_CREATED_EVENT = 'app:notification-created';
const GLOBAL_NOTIFICATION_MARK_READ_EVENT = 'app:notification-mark-read';

const formatNotificationTime = (value?: string | null) => {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return date.toLocaleString([], {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
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

const dedupeNotifications = (items: NotificationItem[]) => {
  const seen = new Set<string>();
  return items.filter((item) => {
    if (seen.has(item.id)) {
      return false;
    }

    seen.add(item.id);
    return true;
  });
};

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [isLastPage, setIsLastPage] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [markingNotificationIds, setMarkingNotificationIds] = useState<string[]>([]);
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const [selectedCommentId, setSelectedCommentId] = useState<string | null>(null);

  const markingSet = useMemo(() => new Set(markingNotificationIds), [markingNotificationIds]);

  const loadNotifications = useCallback(async (page: number, append = false) => {
    setError(null);

    if (append) {
      setIsLoadingMore(true);
    } else {
      setIsLoading(true);
    }

    try {
      const paged = await notificationService.getNotifications(page, PAGE_SIZE);
      setNotifications((prev) => {
        const merged = append ? [...prev, ...paged.content] : paged.content;
        return dedupeNotifications(merged);
      });
      setCurrentPage(paged.pageNumber);
      setIsLastPage(paged.last);
    } catch {
      if (!append) {
        setNotifications([]);
      }
      setError('Failed to load notifications.');
    } finally {
      if (append) {
        setIsLoadingMore(false);
      } else {
        setIsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    void loadNotifications(0);
  }, [loadNotifications]);

  useEffect(() => {
    const handleGlobalNotification = (event: Event) => {
      const customEvent = event as CustomEvent<NotificationItem>;
      if (!customEvent.detail) {
        return;
      }
      setNotifications((prev) => dedupeNotifications([customEvent.detail, ...prev]));
    };

    window.addEventListener(GLOBAL_NOTIFICATION_CREATED_EVENT, handleGlobalNotification as EventListener);
    return () => {
      window.removeEventListener(GLOBAL_NOTIFICATION_CREATED_EVENT, handleGlobalNotification as EventListener);
    };
  }, []);

  const handleLoadMore = () => {
    if (isLoadingMore || isLastPage) {
      return;
    }

    void loadNotifications(currentPage + 1, true);
  };

  const handleMarkAsRead = async (notificationId: string) => {
    const target = notifications.find((item) => item.id === notificationId);
    if (!target || target.isRead || markingSet.has(notificationId)) {
      return;
    }

    setMarkingNotificationIds((prev) => [...prev, notificationId]);

    try {
      await notificationService.markAsRead(notificationId);
      setNotifications((prev) => prev.filter((item) => item.id !== notificationId));
      window.dispatchEvent(new CustomEvent(GLOBAL_NOTIFICATION_MARK_READ_EVENT, { detail: { count: 1 } }));
    } catch {
      toast.error('Failed to mark this notification as read.');
    } finally {
      setMarkingNotificationIds((prev) => prev.filter((id) => id !== notificationId));
    }
  };

  const handleMarkAllAsRead = async () => {
    if (!notifications.length) {
      return;
    }

    const unreadCount = notifications.length;
    try {
      await notificationService.markAllAsRead();
      setNotifications([]);
      window.dispatchEvent(new CustomEvent(GLOBAL_NOTIFICATION_MARK_READ_EVENT, { detail: { count: unreadCount } }));
      toast.success('Marked all notifications as read.');
    } catch {
      toast.error('Failed to mark all notifications as read.');
    }
  };

  const handleNotificationClick = (notification: NotificationItem) => {
    if (!notification.isRead && !markingSet.has(notification.id)) {
      void handleMarkAsRead(notification.id);
    }

    if (notification.type === 'FOLLOW') {
      navigate(`/${notification.actorUsername}`);
      return;
    }

    if (notification.postId) {
      setSelectedPostId(notification.postId);
      setSelectedCommentId(notification.type === 'POST_COMMENT' ? (notification.commentId ?? null) : null);
    }
  };

  return (
    <MainLayout>
      <div className="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6">
        <div className="mb-6 flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-full bg-gray-100 text-gray-700">
            <FiBell size={20} />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Notifications</h1>
            <p className="text-sm text-gray-500">Like, comment, and follow updates appear here in realtime.</p>
          </div>
          <button
            type="button"
            onClick={handleMarkAllAsRead}
            disabled={notifications.length === 0}
            className="ml-auto rounded-full border border-gray-300 px-4 py-2 text-xs font-semibold text-gray-700 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            Mark all as read
          </button>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center gap-2 rounded-xl border border-gray-200 py-10 text-sm text-gray-500">
            <FiLoader size={16} className="animate-spin" />
            Loading notifications...
          </div>
        ) : notifications.length === 0 ? (
          <div className="rounded-xl border border-gray-200 py-10 text-center">
            <p className="text-base font-semibold text-gray-900">No notifications yet</p>
            <p className="mt-1 text-sm text-gray-500">When people interact with your posts, you will see it here.</p>
            {error ? (
              <button
                type="button"
                className="mt-4 rounded-full border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 transition hover:bg-gray-50"
                onClick={() => {
                  void loadNotifications(0);
                }}
              >
                Retry
              </button>
            ) : null}
          </div>
        ) : (
          <>
            <div className="overflow-hidden rounded-xl border border-gray-200">
              {notifications.map((notification) => {
                const isReading = markingSet.has(notification.id);
                const actorProfilePath = `/${notification.actorUsername}`;

                return (
                  <article
                    key={notification.id}
                    className={`cursor-pointer border-b px-4 py-4 last:border-b-0 sm:px-5 ${
                      notification.isRead
                        ? 'border-gray-100 bg-white'
                        : 'border-blue-100 bg-blue-50/60 shadow-[inset_3px_0_0_0_rgb(59,130,246)]'
                    }`}
                    onClick={() => handleNotificationClick(notification)}
                  >
                    <div className="flex items-start gap-3">
                      <Link to={actorProfilePath} className="h-10 w-10 overflow-hidden rounded-full bg-gray-200">
                        {notification.actorAvatarUrl ? (
                          <img
                            src={notification.actorAvatarUrl}
                            alt={notification.actorUsername}
                            className="h-full w-full object-cover"
                          />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center text-gray-500">
                            <FiUser size={16} />
                          </div>
                        )}
                      </Link>

                      <div className="min-w-0 flex-1">
                        <p className={`text-sm ${notification.isRead ? 'text-gray-800' : 'font-semibold text-gray-900'}`}>
                          <Link to={actorProfilePath} className="font-semibold text-gray-900 hover:underline">
                            {notification.actorUsername}
                          </Link>{' '}
                          {getNotificationMessage(notification.type)}
                        </p>
                        <p className="mt-1 text-xs text-gray-500">{formatNotificationTime(notification.createdAt)}</p>
                      </div>

                      <button
                        type="button"
                        className="inline-flex shrink-0 items-center gap-1 rounded-full border border-gray-300 px-3 py-1.5 text-xs font-semibold text-gray-700 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
                        onClick={(event) => {
                          // Do not trigger row navigation when pressing mark button.
                          event.stopPropagation();
                          void handleMarkAsRead(notification.id);
                        }}
                        disabled={isReading}
                      >
                        {isReading ? <FiLoader size={12} className="animate-spin" /> : <FiCheck size={12} />}
                        Mark as read
                      </button>
                    </div>
                  </article>
                );
              })}
            </div>

            {error ? <p className="mt-3 text-sm text-red-500">{error}</p> : null}

            {!isLastPage ? (
              <div className="mt-5 flex justify-center">
                <button
                  type="button"
                  onClick={handleLoadMore}
                  disabled={isLoadingMore}
                  className="inline-flex items-center gap-2 rounded-full border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isLoadingMore ? <FiLoader size={14} className="animate-spin" /> : null}
                  {isLoadingMore ? 'Loading more...' : 'Load more'}
                </button>
              </div>
            ) : null}
          </>
        )}
      </div>

      {selectedPostId ? (
        <PostDetailModal
          postId={selectedPostId}
          highlightCommentId={selectedCommentId}
          onClose={() => {
            setSelectedPostId(null);
            setSelectedCommentId(null);
          }}
        />
      ) : null}
    </MainLayout>
  );
}
