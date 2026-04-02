import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
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
import type { ConversationItem, Message, MessageEvent, SearchUser } from '../features/message/types';

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

export default function MessagesPage() {
  const { user } = useAuth();

  const [conversations, setConversations] = useState<ConversationItem[]>([]);
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [pendingConversationUser, setPendingConversationUser] = useState<SearchUser | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);

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

  const loadConversations = useCallback(async (keepSelection = true) => {
    try {
      const items = await messageService.getConversations();
      setConversations(items);

      if (!keepSelection) {
        return;
      }

      const currentSelection = selectedConversationIdRef.current;
      if (!currentSelection) {
        return;
      }

      const stillExists = items.some((item) => item.id === currentSelection);
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
                lastMessagePreview: incoming.content,
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
            lastMessagePreview: sent.content,
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
          lastMessagePreview: sent.content,
          lastMessageAt: sent.createdAt,
          unreadCount: 0,
        };

        return upsertConversation(prev, updated);
      });
    } catch {
      setError('Failed to send message.');
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
                    <div className="mb-5 flex items-center gap-3">
                      <div className="relative h-16 w-16 overflow-hidden rounded-full border border-gray-300 bg-gray-200">
                        {user?.avatarUrl ? (
                          <img src={user.avatarUrl} alt={user.username} className="h-full w-full object-cover" />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center bg-gray-300">
                            <FiUser size={24} className="text-gray-500" />
                          </div>
                        )}
                        <div className="absolute -top-2 left-1/2 -translate-x-1/2 rounded-xl bg-white px-2 py-0.5 text-[11px] text-gray-500 shadow">
                          Your note
                        </div>
                      </div>
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
                              <div className="h-11 w-11 shrink-0 overflow-hidden rounded-full bg-gray-200">
                                {participant?.avatarUrl ? (
                                  <img src={participant.avatarUrl} alt={participant.username} className="h-full w-full object-cover" />
                                ) : (
                                  <div className="flex h-full w-full items-center justify-center bg-gray-300">
                                    <FiUser size={18} className="text-gray-500" />
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
                  <div className="flex h-[72px] items-center justify-between border-b border-gray-200 px-5">
                    <div className="flex items-center gap-3">
                      <div className="h-10 w-10 overflow-hidden rounded-full bg-gray-200">
                        {(selectedConversation?.participants[0]?.avatarUrl || pendingConversationUser?.avatarUrl) ? (
                          <img
                            src={selectedConversation?.participants[0]?.avatarUrl || pendingConversationUser?.avatarUrl}
                            alt={selectedConversation?.participants[0]?.username || pendingConversationUser?.username}
                            className="h-full w-full object-cover"
                          />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center bg-gray-300">
                            <FiUser size={16} className="text-gray-500" />
                          </div>
                        )}
                      </div>
                      <p className="font-semibold text-gray-900">
                        {selectedConversation
                          ? getConversationLabel(selectedConversation)
                          : (pendingConversationUser?.fullName || pendingConversationUser?.username)}
                      </p>
                    </div>

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

                  <div className="flex-1 space-y-0.5 overflow-y-auto bg-[#f6f6f6] px-5 py-4">
                    {isLoadingMessages ? (
                      <div className="flex h-full items-center justify-center gap-2 text-sm text-gray-500">
                        <FiLoader className="animate-spin" size={16} />
                        Loading messages...
                      </div>
                    ) : messages.length === 0 ? (
                      <div className="flex h-full items-center justify-center text-sm text-gray-500">
                        No messages yet. Start the conversation.
                      </div>
                    ) : (
                      messages.map((message, index) => {
                        const isMine = message.senderId === user?.id;
                        const previous = index > 0 ? messages[index - 1] : undefined;
                        const showTimeMarker = shouldShowMessageTimeMarker(message.createdAt, previous?.createdAt);
                        const markerText = formatMessageGroupTime(message.createdAt);

                        return (
                          <div key={message.id}>
                            {showTimeMarker && markerText ? (
                              <div className="mb-2 mt-1 text-center text-[11px] text-gray-400">
                                {markerText}
                              </div>
                            ) : null}
                            <div className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
                              <div
                                className={`max-w-[75%] whitespace-pre-wrap break-words rounded-2xl px-3 py-2 text-sm ${
                                  isMine
                                    ? 'rounded-br-md bg-[#0095f6] text-white'
                                    : 'rounded-bl-md border border-gray-200 bg-white text-gray-900'
                                }`}
                              >
                                {message.content}
                              </div>
                            </div>
                          </div>
                        );
                      })
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
    </MainLayout>
  );
}
