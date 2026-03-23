import { Heart, MessageCircle, Send, Bookmark, MoreHorizontal } from 'lucide-react';
import type { Post } from '../types';
import MediaCarousel from './MediaCarousel';
import { useState } from 'react';
import { postService } from '../services/postService';
import { Link } from 'react-router-dom';
import CommentModal from './CommentModal';

interface PostCardProps {
  post: Post;
}

export default function PostCard({ post: initialPost }: PostCardProps) {
  const [post, setPost] = useState(initialPost);
  const [isLiking, setIsLiking] = useState(false);
  const [isCommentModalOpen, setIsCommentModalOpen] = useState(false);

  const handleToggleLike = async () => {
    if (isLiking) return;
    setIsLiking(true);

    const newIsLiked = !post.isLiked;
    const newLikeCount = newIsLiked ? post.likeCount + 1 : post.likeCount - 1;

    // Optimistic update
    setPost(prev => ({ ...prev, isLiked: newIsLiked, likeCount: newLikeCount }));

    try {
      if (newIsLiked) {
        await postService.likePost(post.id);
      } else {
        await postService.unlikePost(post.id);
      }
    } catch (error) {
      // Rollback on error
      setPost(prev => ({ ...prev, isLiked: !newIsLiked, likeCount: post.likeCount }));
      console.error('Failed to update like:', error);
    } finally {
      setIsLiking(false);
    }
  };

  const handleToggleSave = async () => {
    const newIsSaved = !post.isSaved;

    // Optimistic update
    setPost(prev => ({ ...prev, isSaved: newIsSaved }));

    try {
      if (newIsSaved) {
        await postService.savePost(post.id);
      } else {
        await postService.unsavePost(post.id);
      }
    } catch (error) {
      setPost(prev => ({ ...prev, isSaved: !newIsSaved }));
      console.error('Failed to update save:', error);
    }
  };

  return (
    <>
      <article className="bg-white border border-gray-200 rounded-lg overflow-hidden max-w-[470px] mx-auto mb-4 scale-in duration-300">
        {/* Header */}
        <div className="flex items-center justify-between p-3">
          <div className="flex items-center space-x-3">
            <Link to={`/${post.user.username}`} className="w-8 h-8 rounded-full bg-gray-200 overflow-hidden ring-1 ring-gray-100 flex items-center justify-center font-bold text-gray-400">
              {post.user.avatarUrl ? (
                <img src={post.user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />
              ) : (
                post.user.username[0].toUpperCase()
              )}
            </Link>
            <Link to={`/${post.user.username}`} className="text-sm font-semibold hover:text-gray-500 transition-colors">
              {post.user.username}
            </Link>
            <span className="text-gray-500 text-sm">•</span>
            <span className="text-gray-500 text-sm">Now</span>
          </div>
          <button className="text-gray-900 border-none bg-none p-1 hover:text-gray-500 transition">
            <MoreHorizontal size={20} />
          </button>
        </div>

        {/* Media */}
        <MediaCarousel media={post.media} />

        {/* Actions */}
        <div className="p-3">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center space-x-4">
              <button 
                onClick={handleToggleLike}
                className={`hover:opacity-60 transition ${post.isLiked ? 'text-red-500 animate-in bounce-in' : 'text-gray-900'}`}
              >
                <Heart size={24} fill={post.isLiked ? 'currentColor' : 'none'} />
              </button>
              <button 
                onClick={() => setIsCommentModalOpen(true)}
                className="hover:opacity-60 transition text-gray-900"
              >
                <MessageCircle size={24} />
              </button>
              <button className="hover:opacity-60 transition text-gray-900">
                <Send size={24} />
              </button>
            </div>
            <button 
              onClick={handleToggleSave}
              className={`hover:opacity-60 transition ${post.isSaved ? 'text-black' : 'text-gray-900'}`}
            >
              <Bookmark size={24} fill={post.isSaved ? 'currentColor' : 'none'} />
            </button>
          </div>

          {/* Likes */}
          <div className="text-sm font-semibold mb-2">
            {post.likeCount.toLocaleString()} likes
          </div>

          {/* Caption */}
          {post.caption && (
            <div className="text-sm mb-2">
              <Link to={`/${post.user.username}`} className="font-semibold mr-2">{post.user.username}</Link>
              <span>{post.caption}</span>
            </div>
          )}

          {/* Comment Link */}
          {post.commentCount > 0 && (
            <button 
              onClick={() => setIsCommentModalOpen(true)}
              className="text-gray-500 text-sm mb-2 hover:text-gray-600 transition"
            >
              View all {post.commentCount} comments
            </button>
          )}

          {/* Add Comment (Placeholder) */}
          <button 
            onClick={() => setIsCommentModalOpen(true)}
            className="mt-2 pt-2 border-t border-gray-100 flex items-center justify-between w-full text-left"
          >
            <span className="text-sm text-gray-400">Add a comment...</span>
            <span className="text-[#0095f6] font-semibold text-sm opacity-50">Post</span>
          </button>
        </div>
      </article>

      <CommentModal 
        post={post} 
        isOpen={isCommentModalOpen} 
        onClose={() => setIsCommentModalOpen(false)} 
      />
    </>
  );
}
