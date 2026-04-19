import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { Link } from 'react-router-dom';
import {
  FiArrowLeft,
  FiChevronDown,
  FiInfo,
  FiSearch,
  FiPhone,
  FiSend,
  FiEdit3,
  FiLoader,
  FiUser,
  FiVideo,
} from 'react-icons/fi';
import { toast } from 'react-toastify';
import MainLayout from '../components/layout/MainLayout';
import { useAuth } from '../hooks/useAuth';
import { messageService } from '../features/message/services/messageService';
import type { ConversationItem, Message, MessageEvent, SearchUser, SharedStoryPreview } from '../features/message/types';
import PostDetailModal from '../features/post/components/PostDetailModal';
import StoryViewer from '../features/story/components/StoryViewer';
import { storyService } from '../features/story/services/storyService';
import type { Story } from '../features/story/services/storyService';

const formatConversationTime = (value?: string | null) => {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return date.toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
  });
};

const getConversationLabel = (conversation: ConversationItem) => {
  if (!conversation.participants.length) {
    return 'Unknown user';
  }

  return conversation.participants.map((participant) => participant.username).join(', ');
};

const MESSAGE_TIME_GROUP_THRESHOLD_MS = 10 * 60 * 1000;

const formatMessageGroupTime = (value?: string | null) => {
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

const formatMessageTooltipTime = (value?: string | null) => {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return date.toLocaleString([], {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const shouldShowMessageTimeMarker = (current?: string, previous?: string) => {
  if (!current) {
    return false;
  }

  if (!previous) {
    return true;
  }

  const currentDate = new Date(current);
  const previousDate = new Date(previous);

  if (Number.isNaN(currentDate.getTime()) || Number.isNaN(previousDate.getTime())) {
    return true;
  }

  return currentDate.getTime() - previousDate.getTime() >= MESSAGE_TIME_GROUP_THRESHOLD_MS;
};

const toWebSocketUrl = () => {
  const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
  const rootUrl = apiUrl.endsWith('/api') ? apiUrl.slice(0, -4) : apiUrl;

  if (rootUrl.startsWith('https://')) {
    return `${rootUrl.replace('https://', 'wss://')}/ws`;
  }

  return `${rootUrl.replace('http://', 'ws://')}/ws`;
};

const upsertConversation = (items: ConversationItem[], next: ConversationItem) => {
  const withoutCurrent = items.filter((item) => item.id !== next.id);
  return [next, ...withoutCurrent];
};

const getMessagePreviewText = (
  message: Pick<Message, 'content' | 'sharedPostId' | 'sharedPost' | 'sharedStoryId' | 'sharedStory'>
) => {
  const trimmed = message.content?.trim();
  if (trimmed) {
    return trimmed;
  }

  if (message.sharedPostId || message.sharedPost) {
    return 'Shared a post';
  }

  if (message.sharedStoryId || message.sharedStory) {
    if (message.sharedStory?.expired) {
      return 'Story expired';
    }
    return 'Replied to your story';
  }

  return 'Say hi';
};

const isSelfConversation = (conversation: ConversationItem) => conversation.participants.length === 0;

const getPrimaryConversationUser = (
  conversation: ConversationItem | null,
  pendingUser: SearchUser | null
) => {
  if (pendingUser) {
    return {
      username: pendingUser.username,
      displayName: pendingUser.fullName || pendingUser.username,
      avatarUrl: pendingUser.avatarUrl,
    };
  }

  const participant = conversation?.participants[0];
  if (!participant) {
    return null;
  }

  return {
    username: participant.username,
    displayName: participant.username,
    avatarUrl: participant.avatarUrl,
  };
};

const isSharedStoryExpired = (story?: SharedStoryPreview) => {
  if (!story) {
    return false;
  }

  if (story.expired) {
    return true;
  }

  if (!story.expiresAt) {
    return false;
  }

  const expiry = new Date(story.expiresAt);
  if (Number.isNaN(expiry.getTime())) {
    return false;
  }
  return expiry.getTime() <= Date.now();
};

const mapSharedStoryToViewerStory = (sharedStory: SharedStoryPreview): Story => {
  const nowIso = new Date().toISOString();
  return {
    id: sharedStory.storyId,
    userId: '',
    username: sharedStory.ownerUsername,
    userAvatarUrl: sharedStory.ownerAvatarUrl ?? '',
    mediaUrl: sharedStory.mediaUrl ?? '',
    mediaType: sharedStory.mediaType === 'VIDEO' ? 'VIDEO' : 'IMAGE',
    createdAt: nowIso,
    expiresAt: sharedStory.expiresAt ?? nowIso,
    likeCount: 0,
    replyCount: 0,
    shareCount: 0,
    likedByCurrentUser: false,
  };
};

export default function MessagesPage() {
  const { user } = useAuth();

  const [conversations, setConversations] = useState<ConversationItem[]>([]);
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [pendingConversationUser, setPendingConversationUser] = useState<SearchUser | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [selectedSharedPostId, setSelectedSharedPostId] = useState<string | null>(null);
  const [selectedSharedStoryViewer, setSelectedSharedStoryViewer] = useState<Story | null>(null);

  const [isLoadingConversations, setIsLoadingConversations] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [sendContent, setSendContent] = useState('');
  const [error, setError] = useState<string | null>(null);

  const [isComposeOpen, setIsComposeOpen] = useState(false);
  const [composeSearchQuery, setComposeSearchQuery] = useState('');
  const [composeSearchUsers, setComposeSearchUsers] = useState<SearchUser[]>([]);
  const [isSearchingComposeUsers, setIsSearchingComposeUsers] = useState(false);

  const [leftSearchQuery, setLeftSearchQuery] = useState('');
  const [leftSearchUsers, setLeftSearchUsers] = useState<SearchUser[]>([]);
  const [isSearchingLeftUsers, setIsSearchingLeftUsers] = useState(false);
  const [isLeftSearchOpen, setIsLeftSearchOpen] = useState(false);
  const [quickChatCandidates, setQuickChatCandidates] = useState<SearchUser[]>([]);
  const [isLoadingQuickChatCandidates, setIsLoadingQuickChatCandidates] = useState(true);

  const listBottomRef = useRef<HTMLDivElement | null>(null);
  const conversationsRef = useRef<ConversationItem[]>([]);
  const selectedConversationIdRef = useRef<string | null>(null);
  const leftSearchWrapperRef = useRef<HTMLDivElement | null>(null);
  const leftSearchResultsRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    conversationsRef.current = conversations;
  }, [conversations]);

  useEffect(() => {
    selectedConversationIdRef.current = selectedConversationId;
  }, [selectedConversationId]);

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      const target = event.target as Node;
      const clickedInsideSearchInput = leftSearchWrapperRef.current?.contains(target) ?? false;
      const clickedInsideSearchResults = leftSearchResultsRef.current?.contains(target) ?? false;

      if (!clickedInsideSearchInput && !clickedInsideSearchResults) {
        setIsLeftSearchOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
    };
  }, []);

  const selectedConversation = useMemo(
    () => conversations.find((item) => item.id === selectedConversationId) ?? null,
    [conversations, selectedConversationId]
  );

  const normalizedQuickChatCandidates = useMemo(
    () => quickChatCandidates.filter((candidate) => candidate.id !== user?.id),
    [quickChatCandidates, user?.id]
  );

  const conversationHeaderUser = useMemo(
    () => getPrimaryConversationUser(selectedConversation, pendingConversationUser),
    [pendingConversationUser, selectedConversation]
  );
  const shouldShowCenteredProfileCard = !isLoadingMessages && Boolean(conversationHeaderUser);

  const loadConversations = useCallback(async (keepSelection = true) => {
    try {
      const items = await messageService.getConversations();
      const visibleItems = items.filter((item) => !isSelfConversation(item));
      setConversations(visibleItems);

      if (!keepSelection) {
        return;
      }

      const currentSelection = selectedConversationIdRef.current;
      if (!currentSelection) {
        return;
      }

      const stillExists = visibleItems.some((item) => item.id === currentSelection);
      if (!stillExists) {
        setSelectedConversationId(null);
      }
    } catch {
      setError('Failed to load conversations.');
    } finally {
      setIsLoadingConversations(false);
    }
  }, []);

  const loadMessages = async (conversationId: string) => {
    setIsLoadingMessages(true);
    setError(null);

    try {
      const conversationMessages = await messageService.getMessages(conversationId);
      setMessages(conversationMessages);
      await messageService.markConversationRead(conversationId);
      setConversations((prev) =>
        prev.map((item) => (item.id === conversationId ? { ...item, unreadCount: 0 } : item))
      );
    } catch {
      setError('Failed to load messages.');
    } finally {
      setIsLoadingMessages(false);
    }
  };

  useEffect(() => {
    loadConversations(false);
  }, [loadConversations]);

  useEffect(() => {
    if (!selectedConversationId) {
      setMessages([]);
      return;
    }

    loadMessages(selectedConversationId);
  }, [selectedConversationId]);

  useEffect(() => {
    listBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
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
      onConnect: () => {
        client.subscribe('/user/queue/messages', (frame) => {
          const event = JSON.parse(frame.body) as MessageEvent;

          if (event.type === 'MESSAGE_CREATED') {
            const incoming = event.message;
            if (!incoming) {
              return;
            }

            const exists = conversationsRef.current.some((item) => item.id === event.conversationId);
            if (!exists) {
              void loadConversations(true);
            }

            setConversations((prev) => {
              const target = prev.find((item) => item.id === event.conversationId);
              if (!target) {
                return prev;
              }

              const nextUnread = incoming.senderId === user?.id ? target.unreadCount : target.unreadCount + 1;
              const updatedConversation: ConversationItem = {
                ...target,
                lastMessagePreview: getMessagePreviewText(incoming),
                lastMessageAt: incoming.createdAt,
                unreadCount: selectedConversationId === target.id ? 0 : nextUnread,
              };

              return upsertConversation(prev, updatedConversation);
            });

            if (selectedConversationId === event.conversationId) {
              setMessages((prev) => {
                if (prev.some((item) => item.id === incoming.id)) {
                  return prev;
                }
                return [...prev, incoming];
              });

              if (incoming.senderId !== user?.id) {
                void messageService.markConversationRead(event.conversationId);
              }
            }
          }
        });
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [loadConversations, selectedConversationId, user?.id]);

  useEffect(() => {
    if (!isLeftSearchOpen) {
      return;
    }

    const timeout = setTimeout(async () => {
      setIsSearchingLeftUsers(true);
      try {
        const users = await messageService.getSearchCandidates(leftSearchQuery);
        setLeftSearchUsers(users.filter((candidate) => candidate.id !== user?.id));
      } catch {
        setLeftSearchUsers([]);
      } finally {
        setIsSearchingLeftUsers(false);
      }
    }, 250);

    return () => clearTimeout(timeout);
  }, [isLeftSearchOpen, leftSearchQuery, user?.id]);

  useEffect(() => {
    if (isLeftSearchOpen) {
      return;
    }

    let cancelled = false;

    const loadQuickChatCandidates = async () => {
      setIsLoadingQuickChatCandidates(true);
      try {
        const users = await messageService.getSearchCandidates('');
        if (!cancelled) {
          setQuickChatCandidates(users);
        }
      } catch {
        if (!cancelled) {
          setQuickChatCandidates([]);
        }
      } finally {
        if (!cancelled) {
          setIsLoadingQuickChatCandidates(false);
        }
      }
    };

    void loadQuickChatCandidates();

    return () => {
      cancelled = true;
    };
  }, [isLeftSearchOpen]);

  useEffect(() => {
    if (!isComposeOpen) {
      return;
    }

    const timeout = setTimeout(async () => {
      setIsSearchingComposeUsers(true);
      try {
        const users = await messageService.getSearchCandidates(composeSearchQuery);
        setComposeSearchUsers(users.filter((candidate) => candidate.id !== user?.id));
      } catch {
        setComposeSearchUsers([]);
      } finally {
        setIsSearchingComposeUsers(false);
      }
    }, 250);

    return () => clearTimeout(timeout);
  }, [composeSearchQuery, isComposeOpen, user?.id]);

  const openConversationFromLeftSearch = (candidate: SearchUser) => {
    if (candidate.id === user?.id) {
      toast.info('Không thể nhắn với chính mình.');
      return;
    }

    const existingConversation = conversations.find((conversation) =>
      conversation.participants.some((participant) => participant.id === candidate.id)
    );

    if (existingConversation) {
      setPendingConversationUser(null);
      setSelectedConversationId(existingConversation.id);
      return;
    }

    setPendingConversationUser(candidate);
    setSelectedConversationId(null);
    setMessages([]);
  };

  const handleSend = async () => {
    const trimmed = sendContent.trim();
    if (!trimmed) {
      return;
    }

    if (!selectedConversationId && !pendingConversationUser) {
      return;
    }

    if (!selectedConversationId && pendingConversationUser) {
      if (pendingConversationUser.id === user?.id) {
        toast.info('Không thể nhắn với chính mình.');
        return;
      }

      try {
        const conversation = await messageService.createConversation(pendingConversationUser.id);
        const sent = await messageService.sendMessage(conversation.id, trimmed);
        setSendContent('');
        setPendingConversationUser(null);

        setConversations((prev) => {
          const existing = prev.find((item) => item.id === conversation.id);
          const baseConversation = existing ?? conversation;
          const updated: ConversationItem = {
            ...baseConversation,
            lastMessagePreview: getMessagePreviewText(sent),
            lastMessageAt: sent.createdAt,
            unreadCount: 0,
          };
          return upsertConversation(prev, updated);
        });

        setMessages((prev) => {
          if (prev.some((item) => item.id === sent.id)) {
            return prev;
          }
          return [...prev, sent];
        });

        setSelectedConversationId(conversation.id);
      } catch {
        setError('Failed to send message.');
      }
      return;
    }

    const conversationId = selectedConversationId;
    if (!conversationId) {
      return;
    }

    const selected = conversations.find((item) => item.id === conversationId);
    if (selected && isSelfConversation(selected)) {
      toast.info('Không thể nhắn với chính mình.');
      return;
    }

    try {
      const sent = await messageService.sendMessage(conversationId, trimmed);
      setSendContent('');

      setMessages((prev) => {
        if (prev.some((item) => item.id === sent.id)) {
          return prev;
        }
        return [...prev, sent];
      });

      setConversations((prev) => {
        const target = prev.find((item) => item.id === conversationId);
        if (!target) {
          return prev;
        }

        const updated: ConversationItem = {
          ...target,
          lastMessagePreview: getMessagePreviewText(sent),
          lastMessageAt: sent.createdAt,
          unreadCount: 0,
        };

        return upsertConversation(prev, updated);
      });
    } catch {
      setError('Failed to send message.');
    }
  };

  const handleStoryViewerLike = async (storyId: string, currentlyLiked: boolean) => {
    try {
      const updated = currentlyLiked
        ? await storyService.unlikeStory(storyId)
        : await storyService.likeStory(storyId);
      setSelectedSharedStoryViewer(updated);
      return updated;
    } catch {
      toast.error('Không thể tương tác story lúc này.');
      return undefined;
    }
  };

  const handleStoryViewerReply = async (storyId: string, content: string) => {
    try {
      const updated = await storyService.replyStory(storyId, content);
      return updated;
    } catch {
      toast.error('Không thể trả lời story lúc này.');
      return undefined;
    }
  };

  const handleStoryViewerShare = async (storyId: string) => {
    try {
      const updated = await storyService.shareStory(storyId);
      return updated;
    } catch {
      toast.error('Không thể share story lúc này.');
      return undefined;
    }
  };

  const showingLeftSearchDropdown = isLeftSearchOpen;
  const showFeatureInProgressNotice = () => {
    toast.info('Chức năng đang phát triển.');
  };

  return (
    <MainLayout>
      <div className="h-screen px-2 py-2 md:px-0 md:py-0">
        <div className="h-[calc(100vh-16px)] overflow-hidden border border-gray-200 bg-white md:h-screen md:border-0">
          <div className="flex h-full">
            <aside className="w-[350px] shrink-0 border-r border-gray-200 bg-[#fafafa]">
              <div className="h-full overflow-y-auto px-5 py-5">
                <div className="mb-5 flex items-center justify-between">
                  <button type="button" className="flex items-center gap-1 text-lg font-semibold leading-none text-gray-900 md:text-xl">
                    {user?.username || 'username'}
                    <FiChevronDown size={18} className="text-gray-600" />
                  </button>
                  <button
                    type="button"
                    className="rounded-lg p-1.5 text-gray-800 transition hover:bg-gray-200"
                    onClick={() => setIsComposeOpen(true)}
                    aria-label="Compose message"
                  >
                    <FiEdit3 size={22} />
                  </button>
                </div>

                <div ref={leftSearchWrapperRef} className="mb-5">
                  {showingLeftSearchDropdown ? (
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          setIsLeftSearchOpen(false);
                          setLeftSearchQuery('');
                          setLeftSearchUsers([]);
                        }}
                        className="rounded-md p-1.5 text-gray-700 transition hover:bg-gray-200"
                        aria-label="Close search"
                      >
                        <FiArrowLeft size={18} />
                      </button>
                      <div className="flex flex-1 items-center gap-2 rounded-xl bg-gray-200/75 px-3 py-2.5">
                        <FiSearch size={18} className="text-gray-500" />
                        <input
                          value={leftSearchQuery}
                          onChange={(event) => setLeftSearchQuery(event.target.value)}
                          placeholder="Search"
                          className="w-full bg-transparent text-sm outline-none placeholder:text-gray-500"
                          autoFocus
                        />
                      </div>
                    </div>
                  ) : (
                    <div className="flex items-center gap-2 rounded-xl bg-gray-200/75 px-3 py-2.5">
                      <FiSearch size={18} className="text-gray-500" />
                      <input
                        value={leftSearchQuery}
                        onFocus={() => setIsLeftSearchOpen(true)}
                        onChange={(event) => {
                          if (!isLeftSearchOpen) {
                            setIsLeftSearchOpen(true);
                          }
                          setLeftSearchQuery(event.target.value);
                        }}
                        placeholder="Search"
                        className="w-full bg-transparent text-sm outline-none placeholder:text-gray-500"
                      />
                    </div>
                  )}
                </div>

                {showingLeftSearchDropdown ? (
                  <div ref={leftSearchResultsRef} className="overflow-hidden rounded-xl bg-[#fafafa]">
                    {isSearchingLeftUsers ? (
                      <div className="flex items-center justify-center gap-2 px-4 py-8 text-sm text-gray-500">
                        <FiLoader className="animate-spin" size={16} />
                        Searching users...
                      </div>
                    ) : leftSearchUsers.length === 0 ? (
                      <div className="px-4 py-6 text-sm text-gray-500">
                        {leftSearchQuery.trim().length === 0 ? 'No following users yet.' : 'No users found.'}
                      </div>
                    ) : (
                      leftSearchUsers.map((candidate) => (
                        <button
                          key={candidate.id}
                          type="button"
                          className="flex w-full items-center gap-3 border-b border-gray-100 px-3 py-3 text-left transition hover:bg-gray-50 last:border-0"
                          onClick={() => {
                            setIsLeftSearchOpen(false);
                            openConversationFromLeftSearch(candidate);
                          }}
                        >
                          <div className="h-11 w-11 shrink-0 overflow-hidden rounded-full bg-gray-200">
                            {candidate.avatarUrl ? (
                              <img src={candidate.avatarUrl} alt={candidate.username} className="h-full w-full object-cover" />
                            ) : (
                              <div className="flex h-full w-full items-center justify-center bg-gray-300">
                                <FiUser size={18} className="text-gray-500" />
                              </div>
                            )}
                          </div>

                          <div className="min-w-0">
                            <p className="truncate text-sm font-semibold text-gray-900">{candidate.fullName || candidate.username}</p>
                            <p className="truncate text-sm text-gray-500">{candidate.username}</p>
                          </div>
                        </button>
                      ))
                    )}
                  </div>
                ) : (
                  <>
                    <div className="mb-5">
                      {isLoadingQuickChatCandidates ? (
                        <div className="flex items-center gap-2 rounded-xl bg-[#fafafa] px-4 py-4 text-sm text-gray-500">
                          <FiLoader className="animate-spin" size={16} />
                          Loading quick chats...
                        </div>
                      ) : normalizedQuickChatCandidates.length === 0 ? (
                        <div className="rounded-xl bg-[#fafafa] px-4 py-4 text-sm text-gray-500">
                          No users available yet.
                        </div>
                      ) : (
                        <div className="overflow-x-auto">
                          <div className="flex min-w-max items-start gap-4 pb-1">
                            {normalizedQuickChatCandidates.map((candidate) => {
                              const isSelf = candidate.id === user?.id;

                              return (
                                <button
                                  key={candidate.id}
                                  type="button"
                                  className="flex w-20 shrink-0 flex-col items-center gap-2 text-center"
                                  onClick={() => openConversationFromLeftSearch(candidate)}
                                >
                                  <div className="h-16 w-16 overflow-hidden rounded-full border border-gray-300 bg-gray-200 transition hover:scale-[1.02]">
                                    {candidate.avatarUrl ? (
                                      <img src={candidate.avatarUrl} alt={candidate.username} className="h-full w-full object-cover" />
                                    ) : (
                                      <div className="flex h-full w-full items-center justify-center bg-gray-300">
                                        <FiUser size={24} className="text-gray-500" />
                                      </div>
                                    )}
                                  </div>
                                  <div className="min-w-0">
                                    <p className="truncate text-xs font-medium text-gray-700">{candidate.fullName || candidate.username}</p>
                                    {isSelf ? <p className="text-[10px] uppercase tracking-wide text-gray-400">You</p> : null}
                                  </div>
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </div>

                    <h2 className="mb-3 text-xl font-semibold leading-none tracking-tight text-gray-900 md:text-2xl">Messages</h2>

                    <div className="overflow-hidden rounded-xl bg-[#fafafa]">
                      {isLoadingConversations ? (
                        <div className="flex items-center justify-center gap-2 px-4 py-8 text-sm text-gray-500">
                          <FiLoader className="animate-spin" size={16} />
                          Loading chats...
                        </div>
                      ) : conversations.length === 0 ? (
                        <div className="px-4 py-6 text-base leading-relaxed text-gray-500">
                          Chats will appear here after you send or receive a message
                        </div>
                      ) : (
                        conversations.map((conversation) => {
                          const participant = conversation.participants[0];
                          const isActive = conversation.id === selectedConversationId;

                          return (
                            <button
                              key={conversation.id}
                              type="button"
                              onClick={() => setSelectedConversationId(conversation.id)}
                              className={`flex w-full items-center gap-3 border-b border-gray-100 px-3 py-3 text-left transition last:border-0 ${
                                isActive ? 'bg-gray-100' : 'hover:bg-gray-50'
                              }`}
                            >
                              <div className="h-12 w-12 shrink-0 overflow-hidden rounded-full bg-gray-200">
                                {participant?.avatarUrl ? (
                                  <img src={participant.avatarUrl} alt={participant.username} className="h-full w-full object-cover" />
                                ) : (
                                  <div className="flex h-full w-full items-center justify-center bg-gray-300">
                                    <FiUser size={20} className="text-gray-500" />
                                  </div>
                                )}
                              </div>

                              <div className="min-w-0 flex-1">
                                <p className="truncate text-sm font-semibold text-gray-900">{getConversationLabel(conversation)}</p>
                                <p className="truncate text-sm text-gray-500">{conversation.lastMessagePreview || 'Say hi'}</p>
                              </div>

                              <div className="shrink-0 text-right">
                                <p className="text-xs text-gray-400">{formatConversationTime(conversation.lastMessageAt)}</p>
                                {conversation.unreadCount > 0 && !isActive ? (
                                  <span className="mt-1 inline-flex min-h-5 min-w-5 items-center justify-center rounded-full bg-[#0095f6] px-1 text-[11px] text-white">
                                    {conversation.unreadCount}
                                  </span>
                                ) : null}
                              </div>
                            </button>
                          );
                        })
                      )}
                    </div>
                  </>
                )}
              </div>
            </aside>

            <section className="flex flex-1 flex-col bg-white">
              {selectedConversation || pendingConversationUser ? (
                <>
                  <div className="flex h-[84px] items-center justify-between border-b border-gray-200 px-5">
                    <div className="flex items-center gap-3">
                      <div className="h-12 w-12 overflow-hidden rounded-full bg-gray-200">
                        {conversationHeaderUser?.avatarUrl ? (
                          <img
                            src={conversationHeaderUser.avatarUrl}
                            alt={conversationHeaderUser.username}
                            className="h-full w-full object-cover"
                          />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center bg-gray-300">
                            <FiUser size={18} className="text-gray-500" />
                          </div>
                        )}
                      </div>
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-gray-900">
                          {conversationHeaderUser?.displayName || 'Unknown user'}
                        </p>
                        {conversationHeaderUser?.username ? (
                          <p className="truncate text-xs text-gray-500">@{conversationHeaderUser.username}</p>
                        ) : null}
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <div className="flex items-center gap-3 text-gray-700">
                        <button
                          type="button"
                          onClick={showFeatureInProgressNotice}
                          className="rounded-md p-1.5 transition hover:bg-gray-100"
                          aria-label="Call"
                        >
                          <FiPhone size={18} />
                        </button>
                        <button
                          type="button"
                          onClick={showFeatureInProgressNotice}
                          className="rounded-md p-1.5 transition hover:bg-gray-100"
                          aria-label="Video call"
                        >
                          <FiVideo size={18} />
                        </button>
                        <button
                          type="button"
                          onClick={showFeatureInProgressNotice}
                          className="rounded-md p-1.5 transition hover:bg-gray-100"
                          aria-label="Conversation info"
                        >
                          <FiInfo size={18} />
                        </button>
                      </div>
                    </div>
                  </div>

                  <div className="flex-1 space-y-0.5 overflow-y-auto bg-[#f6f6f6] px-5 py-4">
                    {isLoadingMessages ? (
                      <div className="flex h-full items-center justify-center gap-2 text-sm text-gray-500">
                        <FiLoader className="animate-spin" size={16} />
                        Loading messages...
                      </div>
                    ) : (
                      <>
                        {shouldShowCenteredProfileCard ? (
                          <div className={`flex items-center justify-center ${messages.length === 0 ? 'h-full' : 'py-6'}`}>
                            <div className="flex flex-col items-center text-center">
                              <div className="mb-4 h-24 w-24 overflow-hidden rounded-full bg-gray-200">
                                {conversationHeaderUser?.avatarUrl ? (
                                  <img
                                    src={conversationHeaderUser.avatarUrl}
                                    alt={conversationHeaderUser.username}
                                    className="h-full w-full object-cover"
                                  />
                                ) : (
                                  <div className="flex h-full w-full items-center justify-center bg-gray-300">
                                    <FiUser size={36} className="text-gray-500" />
                                  </div>
                                )}
                              </div>
                              <p className="text-2xl font-semibold tracking-tight text-gray-900">{conversationHeaderUser?.displayName}</p>
                              <p className="mt-1 text-sm text-gray-500">{conversationHeaderUser?.username} · Instagram</p>
                              {conversationHeaderUser?.username ? (
                                <Link
                                  to={`/${conversationHeaderUser.username}`}
                                  className="mt-4 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-800 transition hover:bg-gray-100"
                                >
                                  View profile
                                </Link>
                              ) : null}
                            </div>
                          </div>
                        ) : null}

                        {messages.length === 0 ? (
                      <div className="flex h-full items-center justify-center text-sm text-gray-500">
                        No messages yet. Start the conversation.
                      </div>
                        ) : (
                          messages.map((message, index) => {
                            const isMine = message.senderId === user?.id;
                            const previous = index > 0 ? messages[index - 1] : undefined;
                            const showTimeMarker = shouldShowMessageTimeMarker(message.createdAt, previous?.createdAt);
                            const markerText = formatMessageGroupTime(message.createdAt);
                            const tooltipTimeText = formatMessageTooltipTime(message.createdAt);
                            const isIncomingGroupStart =
                              !isMine &&
                              (!previous || previous.senderId !== message.senderId || showTimeMarker);
                            const incomingAvatarUrl = message.senderAvatarUrl || conversationHeaderUser?.avatarUrl;
                            const sharedStoryExpired = isSharedStoryExpired(message.sharedStory);

                            return (
                              <div key={message.id}>
                                {showTimeMarker && markerText ? (
                                  <div className="mb-2 mt-1 text-center text-[11px] text-gray-400">
                                    {markerText}
                                  </div>
                                ) : null}
                                <div className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
                                  <div className={`flex items-end gap-2 ${isMine ? '' : 'max-w-[75%]'}`}>
                                    {!isMine ? (
                                      isIncomingGroupStart ? (
                                        <div className="h-7 w-7 shrink-0 overflow-hidden rounded-full bg-gray-200">
                                          {incomingAvatarUrl ? (
                                            <img
                                              src={incomingAvatarUrl}
                                              alt={message.senderUsername}
                                              className="h-full w-full object-cover"
                                            />
                                          ) : (
                                            <div className="flex h-full w-full items-center justify-center bg-gray-300">
                                              <FiUser size={12} className="text-gray-500" />
                                            </div>
                                          )}
                                        </div>
                                      ) : (
                                        <div className="w-7 shrink-0" />
                                      )
                                    ) : null}
                                    <div className={`group relative flex w-fit flex-col gap-0.5 text-sm ${isMine ? 'items-end' : 'items-start'}`}>
                                      {message.sharedStory ? (
                                        <button
                                          type="button"
                                          onClick={() => {
                                            if (sharedStoryExpired) {
                                              return;
                                            }
                                            if (message.sharedStory) {
                                              setSelectedSharedStoryViewer(mapSharedStoryToViewerStory(message.sharedStory));
                                            }
                                          }}
                                          disabled={sharedStoryExpired}
                                          className={`w-[240px] max-w-[65vw] overflow-hidden rounded-2xl border ${
                                            isMine
                                              ? 'border-white/25 bg-[#1f1f24] text-white'
                                              : 'border-gray-200 bg-[#1f1f24] text-white'
                                          } ${sharedStoryExpired ? 'cursor-default opacity-70' : 'cursor-pointer'}`}
                                        >
                                          <div className="border-b border-white/10 px-3 py-2 text-[12px] font-semibold text-white/90">
                                            {sharedStoryExpired ? 'Story expired' : 'Replied to story'}
                                          </div>
                                          {message.sharedStory.mediaUrl ? (
                                            <div className="relative h-[140px] w-full bg-black">
                                              {message.sharedStory.mediaType === 'VIDEO' ? (
                                                <video
                                                  src={message.sharedStory.mediaUrl}
                                                  autoPlay
                                                  loop
                                                  muted
                                                  playsInline
                                                  className="h-full w-full object-cover"
                                                />
                                              ) : (
                                                <img
                                                  src={message.sharedStory.mediaUrl}
                                                  alt="Story preview"
                                                  className="h-full w-full object-cover"
                                                />
                                              )}
                                              {sharedStoryExpired ? (
                                                <div className="absolute inset-0 flex items-center justify-center bg-black/60">
                                                  <span className="rounded-full bg-black/70 px-3 py-1 text-xs font-semibold text-white">
                                                    Story đã hết hạn
                                                  </span>
                                                </div>
                                              ) : null}
                                            </div>
                                          ) : null}
                                          <div className="px-3 py-2 text-xs text-gray-200">
                                            @{message.sharedStory.ownerUsername}
                                          </div>
                                        </button>
                                      ) : null}

                                      {!message.sharedStory && message.sharedStoryId ? (
                                        <div
                                          className={`whitespace-pre-wrap break-words rounded-2xl px-3 py-2 ${
                                            isMine
                                              ? 'max-w-[75%] rounded-br-md bg-[#0095f6] text-white'
                                              : 'rounded-bl-md border border-gray-200 bg-white text-gray-900'
                                          }`}
                                        >
                                          Story expired
                                        </div>
                                      ) : null}

                                      {message.sharedPost ? (
                                        <button
                                          type="button"
                                          onClick={() => setSelectedSharedPostId(message.sharedPost?.postId ?? null)}
                                          className={`w-[300px] max-w-[70vw] cursor-pointer overflow-hidden rounded-2xl border ${
                                            isMine
                                              ? 'border-white/25 bg-[#2b2b2f] text-white'
                                              : 'border-gray-200 bg-[#2b2b2f] text-white'
                                          }`}
                                        >
                                          <div className="flex items-center gap-2 border-b border-white/10 px-3 py-2.5">
                                            <div className="h-7 w-7 shrink-0 overflow-hidden rounded-full bg-gray-500/40">
                                              {message.sharedPost.ownerAvatarUrl ? (
                                                <img
                                                  src={message.sharedPost.ownerAvatarUrl}
                                                  alt={message.sharedPost.ownerUsername}
                                                  className="h-full w-full object-cover"
                                                />
                                              ) : (
                                                <div className="flex h-full w-full items-center justify-center bg-gray-500/40">
                                                  <FiUser size={10} className="text-gray-200" />
                                                </div>
                                              )}
                                            </div>
                                            <p className="truncate text-sm font-semibold">{message.sharedPost.ownerUsername}</p>
                                          </div>

                                          {message.sharedPost.mediaUrl ? (
                                            <div className="relative h-[180px] w-full bg-black">
                                              {message.sharedPost.mediaType === 'VIDEO' ? (
                                                <video
                                                  src={message.sharedPost.mediaUrl}
                                                  autoPlay
                                                  loop
                                                  muted
                                                  playsInline
                                                  className="h-full w-full object-cover"
                                                />
                                              ) : (
                                                <img
                                                  src={message.sharedPost.mediaUrl}
                                                  alt="Shared post preview"
                                                  className="h-full w-full object-cover"
                                                />
                                              )}
                                              {message.sharedPost.mediaType === 'VIDEO' ? (
                                                <span className="absolute right-2 top-2 rounded-full bg-black/60 px-2 py-0.5 text-[10px] font-semibold text-white">
                                                  Video
                                                </span>
                                              ) : null}
                                            </div>
                                          ) : null}

                                          <div className="px-3 py-2.5">
                                            <p className="line-clamp-2 text-sm text-gray-100">
                                              {message.sharedPost.caption?.trim() || 'No caption'}
                                            </p>
                                          </div>
                                        </button>
                                      ) : null}

                                      {!message.sharedPost && message.sharedPostId ? (
                                        <div
                                          className={`whitespace-pre-wrap break-words rounded-2xl px-3 py-2 ${
                                            isMine
                                              ? 'max-w-[75%] rounded-br-md bg-[#0095f6] text-white'
                                              : 'rounded-bl-md border border-gray-200 bg-white text-gray-900'
                                          }`}
                                        >
                                          Shared a post
                                        </div>
                                      ) : null}

                                      {message.content?.trim() ? (
                                        <div
                                          className={`whitespace-pre-wrap break-words rounded-2xl px-3 py-2 ${
                                            isMine
                                              ? 'max-w-[75%] rounded-br-md bg-[#0095f6] text-white'
                                              : 'rounded-bl-md border border-gray-200 bg-white text-gray-900'
                                          }`}
                                        >
                                          {message.content}
                                        </div>
                                      ) : null}
                                      {tooltipTimeText ? (
                                        <div
                                          className={`pointer-events-none absolute -top-8 z-10 hidden whitespace-nowrap rounded-md bg-black/80 px-2 py-1 text-[11px] text-white shadow group-hover:block ${
                                            isMine ? 'right-0' : 'left-0'
                                          }`}
                                        >
                                          {tooltipTimeText}
                                        </div>
                                      ) : null}
                                    </div>
                                  </div>
                                </div>
                              </div>
                            );
                          })
                        )}
                      </>
                    )}
                    <div ref={listBottomRef} />
                  </div>

                  <div className="border-t border-gray-200 p-3">
                    <div className="flex items-center gap-2 rounded-full border border-gray-300 px-3 py-1.5">
                      <input
                        value={sendContent}
                        onChange={(event) => setSendContent(event.target.value)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter') {
                            event.preventDefault();
                            handleSend();
                          }
                        }}
                        placeholder="Message..."
                        className="flex-1 bg-transparent text-sm outline-none"
                      />
                      <button
                        type="button"
                        className="text-sm font-semibold text-[#0095f6] disabled:text-gray-300"
                        onClick={handleSend}
                        disabled={!sendContent.trim()}
                      >
                        Send
                      </button>
                    </div>
                  </div>
                </>
              ) : (
                <div className="flex h-full flex-col items-center justify-center px-6 text-center">
                  <div className="mb-4 flex h-24 w-24 items-center justify-center rounded-full border-[3px] border-gray-900">
                    <FiSend size={42} className="text-gray-900" />
                  </div>
                  <h3 className="text-xl font-semibold leading-tight tracking-tight text-gray-900 md:text-2xl">Your messages</h3>
                  <p className="mt-2 text-xs text-gray-500 md:text-sm">Send a message to start a chat.</p>
                  <button
                    type="button"
                    className="mt-6 rounded-xl bg-[#4f5cff] px-6 py-3 text-[11px] font-semibold text-white transition hover:bg-[#3f4ef0] md:text-xs"
                    onClick={() => setIsComposeOpen(true)}
                  >
                    Send message
                  </button>
                </div>
              )}
            </section>
          </div>
        </div>

        {error ? <p className="mt-2 text-sm text-red-500">{error}</p> : null}
      </div>

      {isComposeOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="flex h-[60vh] w-full max-w-lg flex-col overflow-hidden rounded-xl bg-white">
            <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
              <h2 className="font-semibold">New message</h2>
              <button
                type="button"
                className="text-sm text-gray-500 hover:text-gray-700"
                onClick={() => {
                  setIsComposeOpen(false);
                  setComposeSearchQuery('');
                  setComposeSearchUsers([]);
                }}
              >
                Close
              </button>
            </div>

            <div className="border-b border-gray-200 p-4">
              <div className="flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2">
                <FiSearch size={16} className="text-gray-400" />
                <input
                  value={composeSearchQuery}
                  onChange={(event) => setComposeSearchQuery(event.target.value)}
                  placeholder="Search by username"
                  className="flex-1 text-sm outline-none"
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto">
              {isSearchingComposeUsers ? (
                <div className="flex items-center justify-center gap-2 py-8 text-sm text-gray-500">
                  <FiLoader className="animate-spin" size={14} />
                  Searching users...
                </div>
              ) : composeSearchQuery.trim() && composeSearchUsers.length === 0 ? (
                <p className="py-8 text-center text-sm text-gray-500">No users found.</p>
              ) : composeSearchUsers.length === 0 ? (
                <p className="py-8 text-center text-sm text-gray-500">
                  {composeSearchQuery.trim().length === 0 ? 'No following users yet.' : 'No users found.'}
                </p>
              ) : (
                composeSearchUsers.map((candidate) => (
                  <button
                    key={candidate.id}
                    type="button"
                    className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-gray-50"
                    onClick={() => {
                      setComposeSearchQuery('');
                      setComposeSearchUsers([]);
                      setIsComposeOpen(false);
                      openConversationFromLeftSearch(candidate);
                    }}
                  >
                    <div className="h-10 w-10 shrink-0 overflow-hidden rounded-full bg-gray-200">
                      {candidate.avatarUrl ? (
                        <img src={candidate.avatarUrl} alt={candidate.username} className="h-full w-full object-cover" />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center bg-gray-300">
                          <FiUser size={16} className="text-gray-500" />
                        </div>
                      )}
                    </div>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">@{candidate.username}</p>
                      {candidate.fullName ? <p className="truncate text-xs text-gray-500">{candidate.fullName}</p> : null}
                    </div>
                  </button>
                ))
              )}
            </div>
          </div>
        </div>
      ) : null}

      {selectedSharedStoryViewer ? (
        <StoryViewer
          stories={[selectedSharedStoryViewer]}
          onClose={() => setSelectedSharedStoryViewer(null)}
          onLike={handleStoryViewerLike}
          onReply={handleStoryViewerReply}
          onShare={handleStoryViewerShare}
        />
      ) : null}

      {selectedSharedPostId ? (
        <PostDetailModal
          postId={selectedSharedPostId}
          onClose={() => setSelectedSharedPostId(null)}
        />
      ) : null}
    </MainLayout>
  );
}
