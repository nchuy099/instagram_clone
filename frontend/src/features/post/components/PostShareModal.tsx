import { useEffect, useMemo, useState } from 'react';
import { FiCheck, FiLoader, FiSearch, FiUser, FiX } from 'react-icons/fi';
import { toast } from 'react-toastify';
import { useAuth } from '../../../hooks/useAuth';
import { messageService } from '../../message/services/messageService';
import type { SearchUser } from '../../message/types';
import type { Post } from '../types';

interface PostShareModalProps {
  isOpen: boolean;
  post: Post;
  onClose: () => void;
}

export default function PostShareModal({ isOpen, post, onClose }: PostShareModalProps) {
  const { user } = useAuth();
  const [searchQuery, setSearchQuery] = useState('');
  const [candidates, setCandidates] = useState<SearchUser[]>([]);
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);
  const [messageText, setMessageText] = useState('');
  const [isLoadingCandidates, setIsLoadingCandidates] = useState(false);
  const [isSending, setIsSending] = useState(false);

  const resetState = () => {
    setSearchQuery('');
    setCandidates([]);
    setSelectedUserIds([]);
    setMessageText('');
    setIsLoadingCandidates(false);
    setIsSending(false);
  };

  useEffect(() => {
    if (!isOpen) {
      resetState();
      return;
    }

    const timeout = setTimeout(async () => {
      setIsLoadingCandidates(true);
      try {
        const users = await messageService.getSearchCandidates(searchQuery);
        setCandidates(users.filter((candidate) => candidate.id !== user?.id));
      } catch {
        setCandidates([]);
      } finally {
        setIsLoadingCandidates(false);
      }
    }, 250);

    return () => clearTimeout(timeout);
  }, [isOpen, searchQuery, user?.id]);

  const selectedUsers = useMemo(
    () => candidates.filter((candidate) => selectedUserIds.includes(candidate.id)),
    [candidates, selectedUserIds]
  );

  const toggleUserSelection = (candidateId: string) => {
    setSelectedUserIds((prev) =>
      prev.includes(candidateId) ? prev.filter((id) => id !== candidateId) : [...prev, candidateId]
    );
  };

  const handleClose = () => {
    resetState();
    onClose();
  };

  const handleSend = async () => {
    if (selectedUserIds.length === 0 || isSending) {
      return;
    }

    setIsSending(true);
    const trimmedMessage = messageText.trim();

    try {
      await Promise.all(
        selectedUserIds.map(async (participantId) => {
          const conversation = await messageService.createConversation(participantId);
          await messageService.sendMessage(conversation.id, {
            content: trimmedMessage || undefined,
            sharedPostId: post.id,
          });
        })
      );

      toast.success(
        selectedUserIds.length > 1 ? `Sent to ${selectedUserIds.length} chats.` : 'Post shared successfully.'
      );
      handleClose();
    } catch {
      toast.error('Failed to share post. Please try again.');
    } finally {
      setIsSending(false);
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60 p-4"
      onClick={(event) => {
        event.stopPropagation();
        handleClose();
      }}
    >
      <div
        className="flex h-[76vh] w-full max-w-md flex-col overflow-hidden rounded-2xl bg-white shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h2 className="text-sm font-semibold text-gray-900">Share</h2>
          <button
            type="button"
            className="rounded-md p-1.5 text-gray-500 transition hover:bg-gray-100 hover:text-gray-700"
            onClick={handleClose}
            aria-label="Close share modal"
          >
            <FiX size={18} />
          </button>
        </div>

        <div className="border-b border-gray-200 px-4 py-3">
          <div className="flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2">
            <FiSearch size={16} className="text-gray-400" />
            <input
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search"
              className="w-full text-sm outline-none"
              autoFocus
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto">
          {isLoadingCandidates ? (
            <div className="flex items-center justify-center gap-2 py-8 text-sm text-gray-500">
              <FiLoader className="animate-spin" size={14} />
              Loading users...
            </div>
          ) : candidates.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-500">
              {searchQuery.trim() ? 'No users found.' : 'No users available.'}
            </p>
          ) : (
            candidates.map((candidate) => {
              const isSelected = selectedUserIds.includes(candidate.id);

              return (
                <button
                  key={candidate.id}
                  type="button"
                  className="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-gray-50"
                  onClick={() => toggleUserSelection(candidate.id)}
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

                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-gray-900">{candidate.fullName || candidate.username}</p>
                    <p className="truncate text-xs text-gray-500">@{candidate.username}</p>
                  </div>

                  <span
                    className={`flex h-6 w-6 items-center justify-center rounded-full border transition ${
                      isSelected ? 'border-[#0095f6] bg-[#0095f6] text-white' : 'border-gray-300 bg-white text-transparent'
                    }`}
                  >
                    <FiCheck size={14} />
                  </span>
                </button>
              );
            })
          )}
        </div>

        <div className="border-t border-gray-200 p-4">
          {selectedUsers.length > 0 ? (
            <p className="mb-2 truncate text-xs text-gray-500">
              To: {selectedUsers.map((item) => item.username).join(', ')}
            </p>
          ) : null}

          <textarea
            value={messageText}
            onChange={(event) => setMessageText(event.target.value)}
            placeholder="Write a message"
            rows={3}
            className="mb-3 w-full resize-none rounded-xl border border-gray-300 px-3 py-2 text-sm outline-none transition focus:border-[#0095f6]"
          />

          <button
            type="button"
            onClick={handleSend}
            disabled={selectedUserIds.length === 0 || isSending}
            className="w-full rounded-xl bg-[#0095f6] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[#1877f2] disabled:cursor-not-allowed disabled:bg-gray-300"
          >
            {isSending ? 'Sending...' : 'Send'}
          </button>
        </div>
      </div>
    </div>
  );
}
