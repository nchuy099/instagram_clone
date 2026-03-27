import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import {
  ChevronDown,
  Loader2,
  Search,
  Send,
  SquarePen,
  User,
} from 'lucide-react';
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

  const [isCreatingConversation, setIsCreatingConversation] = useState(false);

  const listBottomRef = useRef<HTMLDivElement | null>(null);
  const conversationsRef = useRef<ConversationItem[]>([]);

  useEffect(() => {
    conversationsRef.current = conversations;
  }, [conversations]);

  const selectedConversation = useMemo(
    () => conversations.find((item) => item.id === selectedConversationId) ?? null,
    [conversations, selectedConversationId]
  );

  const loadConversations = useCallback(async (keepSelection = true) => {
    try {
      const items = await messageService.getConversations();
      setConversations(items);

      if (!keepSelection || !selectedConversationId) {
        setSelectedConversationId(items[0]?.id ?? null);
        return;
      }

      const stillExists = items.some((item) => item.id === selectedConversationId);
      if (!stillExists) {
        setSelectedConversationId(items[0]?.id ?? null);
      }
    } catch {
      setError('Failed to load conversations.');
    } finally {
      setIsLoadingConversations(false);
    }
  }, [selectedConversationId]);

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
    const keyword = leftSearchQuery.trim();
    if (!keyword) {
      setLeftSearchUsers([]);
      return;
    }

    const timeout = setTimeout(async () => {
      setIsSearchingLeftUsers(true);
      try {
        const users = await messageService.searchUsers(keyword);
        setLeftSearchUsers(users.filter((candidate) => candidate.id !== user?.id));
      } catch {
        setLeftSearchUsers([]);
      } finally {
        setIsSearchingLeftUsers(false);
      }
    }, 250);

    return () => clearTimeout(timeout);
  }, [leftSearchQuery, user?.id]);

  useEffect(() => {
    if (!isComposeOpen) {
      return;
    }

    const keyword = composeSearchQuery.trim();
    if (!keyword) {
      setComposeSearchUsers([]);
      return;
    }

    const timeout = setTimeout(async () => {
      setIsSearchingComposeUsers(true);
      try {
        const users = await messageService.searchUsers(keyword);
        setComposeSearchUsers(users.filter((candidate) => candidate.id !== user?.id));
      } catch {
        setComposeSearchUsers([]);
      } finally {
        setIsSearchingComposeUsers(false);
      }
    }, 250);

    return () => clearTimeout(timeout);
  }, [composeSearchQuery, isComposeOpen, user?.id]);

  const startConversationWithUser = async (candidate: SearchUser) => {
    setIsCreatingConversation(true);

    try {
      const conversation = await messageService.createConversation(candidate.id);
      setConversations((prev) => upsertConversation(prev, conversation));
      setSelectedConversationId(conversation.id);

      setLeftSearchQuery('');
      setLeftSearchUsers([]);

      setComposeSearchQuery('');
      setComposeSearchUsers([]);
      setIsComposeOpen(false);

      await loadMessages(conversation.id);
    } catch {
      setError('Failed to start conversation.');
    } finally {
      setIsCreatingConversation(false);
    }
  };

  const handleSend = async () => {
    if (!selectedConversationId || !sendContent.trim()) {
      return;
    }

    try {
      const sent = await messageService.sendMessage(selectedConversationId, sendContent.trim());
      setSendContent('');

      setMessages((prev) => {
        if (prev.some((item) => item.id === sent.id)) {
          return prev;
        }
        return [...prev, sent];
      });

      setConversations((prev) => {
        const target = prev.find((item) => item.id === selectedConversationId);
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

  const showingUserSearch = leftSearchQuery.trim().length > 0;

  return (
    <MainLayout>
      <div className="h-screen px-2 py-2 md:px-0 md:py-0">
        <div className="h-[calc(100vh-16px)] overflow-hidden border border-gray-200 bg-white md:h-screen md:border-0">
          <div className="flex h-full">
            <aside className="w-[350px] shrink-0 border-r border-gray-200 bg-[#fafafa]">
              <div className="h-full overflow-y-auto px-5 py-5">
                <div className="mb-5 flex items-center justify-between">
                  <button type="button" className="flex items-center gap-1 text-[30px] font-semibold leading-none text-gray-900">
                    {user?.username || 'username'}
                    <ChevronDown size={18} className="text-gray-600" />
                  </button>
                  <button
                    type="button"
                    className="rounded-lg p-1.5 text-gray-800 transition hover:bg-gray-200"
                    onClick={() => setIsComposeOpen(true)}
                    aria-label="Compose message"
                  >
                    <SquarePen size={22} />
                  </button>
                </div>

                <div className="mb-5 flex items-center gap-2 rounded-xl bg-gray-200/75 px-3 py-2.5">
                  <Search size={18} className="text-gray-500" />
                  <input
                    value={leftSearchQuery}
                    onChange={(event) => setLeftSearchQuery(event.target.value)}
                    placeholder="Search"
                    className="w-full bg-transparent text-sm outline-none placeholder:text-gray-500"
                  />
                </div>

                <div className="mb-5 flex items-center gap-3">
                  <div className="relative h-16 w-16 overflow-hidden rounded-full border border-gray-300 bg-gray-200">
                    {user?.avatarUrl ? (
                      <img src={user.avatarUrl} alt={user.username} className="h-full w-full object-cover" />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center bg-gray-300">
                        <User size={24} className="text-gray-500" />
                      </div>
                    )}
                    <div className="absolute -top-2 left-1/2 -translate-x-1/2 rounded-xl bg-white px-2 py-0.5 text-[11px] text-gray-500 shadow">
                      Your note
                    </div>
                  </div>
                </div>

                <h2 className="mb-3 text-[32px] font-semibold leading-none tracking-tight text-gray-900">Messages</h2>

                <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
                  {showingUserSearch ? (
                    <>
                      {isSearchingLeftUsers ? (
                        <div className="flex items-center justify-center gap-2 px-4 py-8 text-sm text-gray-500">
                          <Loader2 className="animate-spin" size={16} />
                          Searching users...
                        </div>
                      ) : leftSearchUsers.length === 0 ? (
                        <div className="px-4 py-6 text-sm text-gray-500">No users found.</div>
                      ) : (
                        leftSearchUsers.map((candidate) => (
                          <button
                            key={candidate.id}
                            type="button"
                            className="flex w-full items-center gap-3 border-b border-gray-100 px-3 py-3 text-left transition hover:bg-gray-50 last:border-0"
                            onClick={() => startConversationWithUser(candidate)}
                            disabled={isCreatingConversation}
                          >
                            <div className="h-11 w-11 shrink-0 overflow-hidden rounded-full bg-gray-200">
                              {candidate.avatarUrl ? (
                                <img src={candidate.avatarUrl} alt={candidate.username} className="h-full w-full object-cover" />
                              ) : (
                                <div className="flex h-full w-full items-center justify-center bg-gray-300">
                                  <User size={18} className="text-gray-500" />
                                </div>
                              )}
                            </div>

                            <div className="min-w-0">
                              <p className="truncate text-sm font-semibold text-gray-900">@{candidate.username}</p>
                              {candidate.fullName ? <p className="truncate text-xs text-gray-500">{candidate.fullName}</p> : null}
                            </div>
                          </button>
                        ))
                      )}
                    </>
                  ) : isLoadingConversations ? (
                    <div className="flex items-center justify-center gap-2 px-4 py-8 text-sm text-gray-500">
                      <Loader2 className="animate-spin" size={16} />
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
                                <User size={18} className="text-gray-500" />
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
              </div>
            </aside>

            <section className="flex flex-1 flex-col bg-white">
              {selectedConversation ? (
                <>
                  <div className="flex h-[72px] items-center gap-3 border-b border-gray-200 px-5">
                    <div className="h-10 w-10 overflow-hidden rounded-full bg-gray-200">
                      {selectedConversation.participants[0]?.avatarUrl ? (
                        <img
                          src={selectedConversation.participants[0].avatarUrl}
                          alt={selectedConversation.participants[0].username}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center bg-gray-300">
                          <User size={16} className="text-gray-500" />
                        </div>
                      )}
                    </div>
                    <p className="font-semibold text-gray-900">{getConversationLabel(selectedConversation)}</p>
                  </div>

                  <div className="flex-1 space-y-3 overflow-y-auto bg-[#f6f6f6] px-5 py-4">
                    {isLoadingMessages ? (
                      <div className="flex h-full items-center justify-center gap-2 text-sm text-gray-500">
                        <Loader2 className="animate-spin" size={16} />
                        Loading messages...
                      </div>
                    ) : messages.length === 0 ? (
                      <div className="flex h-full items-center justify-center text-sm text-gray-500">
                        No messages yet. Start the conversation.
                      </div>
                    ) : (
                      messages.map((message) => {
                        const isMine = message.senderId === user?.id;
                        return (
                          <div key={message.id} className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
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
                    <Send size={42} className="text-gray-900" />
                  </div>
                  <h3 className="text-5xl font-semibold leading-tight tracking-tight text-gray-900">Your messages</h3>
                  <p className="mt-2 text-[30px] text-gray-500">Send a message to start a chat.</p>
                  <button
                    type="button"
                    className="mt-6 rounded-xl bg-[#4f5cff] px-6 py-3 text-2xl font-semibold text-white transition hover:bg-[#3f4ef0]"
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
          <div className="w-full max-w-lg overflow-hidden rounded-xl bg-white">
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
                <Search size={16} className="text-gray-400" />
                <input
                  value={composeSearchQuery}
                  onChange={(event) => setComposeSearchQuery(event.target.value)}
                  placeholder="Search by username"
                  className="flex-1 text-sm outline-none"
                />
              </div>
            </div>

            <div className="max-h-[360px] overflow-y-auto">
              {isSearchingComposeUsers ? (
                <div className="flex items-center justify-center gap-2 py-8 text-sm text-gray-500">
                  <Loader2 className="animate-spin" size={14} />
                  Searching users...
                </div>
              ) : composeSearchQuery.trim() && composeSearchUsers.length === 0 ? (
                <p className="py-8 text-center text-sm text-gray-500">No users found.</p>
              ) : composeSearchUsers.length === 0 ? (
                <p className="py-8 text-center text-sm text-gray-500">Type to find a user.</p>
              ) : (
                composeSearchUsers.map((candidate) => (
                  <button
                    key={candidate.id}
                    type="button"
                    className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-gray-50"
                    onClick={() => startConversationWithUser(candidate)}
                    disabled={isCreatingConversation}
                  >
                    <div className="h-10 w-10 shrink-0 overflow-hidden rounded-full bg-gray-200">
                      {candidate.avatarUrl ? (
                        <img src={candidate.avatarUrl} alt={candidate.username} className="h-full w-full object-cover" />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center bg-gray-300">
                          <User size={16} className="text-gray-500" />
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
