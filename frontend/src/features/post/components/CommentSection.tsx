import { useState, useEffect, useCallback } from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { FiMessageCircle, FiLoader, FiX } from 'react-icons/fi';
import { commentService } from '../services/postService';
import type { Comment } from '../types';
import { useAuth } from '../../../hooks/useAuth';
import { formatRelativePostTime } from '../utils/formatRelativePostTime';

interface CommentItemProps {
  comment: Comment;
  onReply: (comment: Comment) => void;
  depth?: 0 | 1;
}

function renderCommentContent(content: string) {
  const mentionPattern = /@([A-Za-z0-9_.]+)/g;
  const nodes: ReactNode[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = mentionPattern.exec(content)) !== null) {
    if (match.index > lastIndex) {
      nodes.push(content.slice(lastIndex, match.index));
    }

    const username = match[1];
    nodes.push(
      <Link key={`${username}-${match.index}`} to={`/${username}`} className="text-[#0095f6] hover:underline">
        @{username}
      </Link>
    );

    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < content.length) {
    nodes.push(content.slice(lastIndex));
  }

  if (nodes.length === 0) {
    return content;
  }

  return nodes;
}

function CommentItem({ comment, onReply, depth = 0 }: CommentItemProps) {
  const [replies, setReplies] = useState<Comment[]>([]);
  const [showReplies, setShowReplies] = useState(false);
  const [isLoadingReplies, setIsLoadingReplies] = useState(false);

  const fetchReplies = async () => {
    if (depth === 1) {
      return;
    }

    if (showReplies) {
      setShowReplies(false);
      return;
    }

    setIsLoadingReplies(true);
    try {
      const data = await commentService.getReplies(comment.id);
      setReplies(data.content);
      setShowReplies(true);
    } catch (err) {
      console.error('Failed to fetch replies:', err);
    } finally {
      setIsLoadingReplies(false);
    }
  };

  return (
    <div className={depth === 0 ? 'flex flex-col space-y-3 mb-4 last:mb-0' : 'flex flex-col space-y-3'}>
      <div className="flex space-x-3 group">
        <div className="w-8 h-8 rounded-full bg-gray-200 overflow-hidden flex-shrink-0">
          {comment.user.avatarUrl && <img src={comment.user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />}
        </div>
        <div className="flex-1">
          <div className="text-sm">
            <span className="font-semibold mr-2">{comment.user.username}</span>
            <span className="text-gray-900">{renderCommentContent(comment.content)}</span>
          </div>
          <div className="flex items-center space-x-4 mt-1 text-xs text-gray-500 font-semibold">
            <span>{formatRelativePostTime(comment.createdAt)}</span>
            {depth === 0 && comment.replyCount > 0 && (
              <button 
                onClick={fetchReplies}
                className="hover:text-gray-900 transition flex items-center"
              >
                {isLoadingReplies ? <FiLoader size={12} className="animate-spin mr-1" /> : null}
                {showReplies ? 'Hide replies' : `View replies (${comment.replyCount})`}
              </button>
            )}
            <button onClick={() => onReply(comment)} className="hover:text-gray-900 transition font-bold">Reply</button>
          </div>
        </div>
      </div>

      {depth === 0 && showReplies && replies.length > 0 && (
        <div className="ml-11 space-y-4 border-l-2 border-gray-100 pl-4">
          {replies.map(reply => (
            <CommentItem key={reply.id} comment={reply} onReply={onReply} depth={1} />
          ))}
        </div>
      )}
    </div>
  );
}

interface CommentSectionProps {
  postId: string;
}

export default function CommentSection({ postId }: CommentSectionProps) {
  const { user } = useAuth();
  const [comments, setComments] = useState<Comment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [newComment, setNewComment] = useState('');
  const [replyTarget, setReplyTarget] = useState<Comment | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchComments = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await commentService.getComments(postId);
      setComments(data.content);
    } catch (err) {
      console.error('Failed to fetch comments:', err);
    } finally {
      setIsLoading(false);
    }
  }, [postId]);

  const handleReply = (comment: Comment) => {
    const mention = `@${comment.user.username} `;

    setNewComment(currentValue => {
      if (!currentValue.trim()) {
        return mention;
      }

      const previousMention = replyTarget ? `@${replyTarget.user.username} ` : '';
      if (previousMention && currentValue.startsWith(previousMention)) {
        return `${mention}${currentValue.slice(previousMention.length)}`;
      }

      if (currentValue.startsWith(mention)) {
        return currentValue;
      }

      return `${mention}${currentValue}`;
    });

    setReplyTarget(comment);
  };

  useEffect(() => {
    fetchComments();
  }, [fetchComments]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      const parentCommentId = replyTarget?.parentCommentId ?? replyTarget?.id;
      const created = await commentService.createComment(postId, newComment, parentCommentId ?? undefined);
      
      if (replyTarget) {
        // For simplicity, just refetch or manually update the local state if needed.
        // For now, let's just clear and refetch the main comments.
        fetchComments();
      } else {
        setComments(prev => [created, ...prev]);
      }
      
      setNewComment('');
      setReplyTarget(null);
    } catch (err) {
      console.error('Failed to post comment:', err);
      alert('Failed to post comment.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col h-full bg-white max-h-[600px]">
      <div className="p-4 border-b border-gray-100 font-semibold text-center text-sm">Comments</div>
      
      <div className="flex-1 overflow-y-auto p-4 custom-scrollbar">
        {isLoading ? (
          <div className="flex justify-center py-8"><FiLoader className="animate-spin text-gray-300" /></div>
        ) : comments.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <FiMessageCircle size={44} className="mx-auto mb-2 opacity-20" />
            <p className="text-sm">No comments yet.</p>
            <p className="text-xs">Start the conversation.</p>
          </div>
        ) : (
          comments.map(comment => (
            <CommentItem key={comment.id} comment={comment} onReply={handleReply} />
          ))
        )}
      </div>

      <div className="p-4 border-t border-gray-200">
        {replyTarget && (
          <div className="flex items-center justify-between mb-2 px-2 py-1 bg-gray-50 rounded-lg text-xs">
            <span className="text-gray-500">Replying to <span className="font-semibold text-gray-900">@{replyTarget.user.username}</span></span>
            <button onClick={() => setReplyTarget(null)} className="text-gray-400 hover:text-gray-600 transition">
              <FiX size={14} />
            </button>
          </div>
        )}
        <form onSubmit={handleSubmit} className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-full bg-gray-200 overflow-hidden flex-shrink-0">
            {user?.avatarUrl && <img src={user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />}
          </div>
          <input 
            type="text" 
            placeholder="Add a comment..." 
            className="flex-1 text-sm outline-none placeholder:text-gray-400"
            value={newComment}
            onChange={e => setNewComment(e.target.value)}
          />
          <button 
            type="submit"
            disabled={!newComment.trim() || isSubmitting}
            className="text-[#0095f6] font-semibold text-sm hover:text-[#00376b] disabled:opacity-50 transition"
          >
            {isSubmitting ? <FiLoader size={16} className="animate-spin" /> : 'Post'}
          </button>
        </form>
      </div>
    </div>
  );
}
