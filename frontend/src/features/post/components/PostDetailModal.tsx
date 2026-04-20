import { useState, useEffect } from 'react';
import {
  FiBookmark,
  FiMoreHorizontal,
  FiHeart,
  FiMessageCircle,
  FiSend,
  FiLoader,
  FiX,
} from 'react-icons/fi';
import { BsBookmarkFill } from 'react-icons/bs';
import { postService } from '../services/postService';
import type { Post } from '../types';
import MediaCarousel from './MediaCarousel';
import CommentSection from './CommentSection';
import { formatRelativePostTime } from '../utils/formatRelativePostTime';
import PostShareModal from './PostShareModal';

interface PostDetailModalProps {
  postId: string;
  highlightCommentId?: string | null;
  onClose: () => void;
}

export default function PostDetailModal({ postId, highlightCommentId = null, onClose }: PostDetailModalProps) {
  const [post, setPost] = useState<Post | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isLiked, setIsLiked] = useState(false);
  const [isSaved, setIsSaved] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);

  useEffect(() => {
    const fetchPost = async () => {
      setIsLoading(true);
      try {
        const data = await postService.getPost(postId);
        setPost(data);
        setIsLiked(data.isLiked);
        setIsSaved(data.isSaved);
        setLikeCount(data.likeCount);
      } catch (err) {
        console.error('Failed to fetch post detail:', err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchPost();

    // Prevent scrolling on body
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [postId]);

  const handleLike = async () => {
    try {
      if (isLiked) {
        await postService.unlikePost(postId);
        setLikeCount(prev => prev - 1);
      } else {
        await postService.likePost(postId);
        setLikeCount(prev => prev + 1);
      }
      setIsLiked(!isLiked);
    } catch (err) {
      console.error('Failed to like/unlike:', err);
    }
  };

  const handleSave = async () => {
    try {
      if (isSaved) {
        await postService.unsavePost(postId);
      } else {
        await postService.savePost(postId);
      }
      setIsSaved(!isSaved);
    } catch (err) {
      console.error('Failed to save/unsave:', err);
    }
  };

  if (isLoading) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
        <FiLoader className="text-white animate-spin" size={48} />
      </div>
    );
  }

  if (!post) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 sm:p-10" onClick={onClose}>
      <button 
        className="absolute top-4 right-4 text-white hover:text-gray-300 transition z-[60]"
        onClick={onClose}
      >
        <FiX size={32} />
      </button>

      <div 
        className="bg-white w-full max-w-6xl h-full max-h-[90vh] flex flex-col md:flex-row rounded-sm overflow-hidden"
        onClick={e => e.stopPropagation()}
      >
        {/* Left: Media */}
        <div className="md:w-[60%] bg-black flex items-center justify-center">
          <MediaCarousel media={post.media} />
        </div>

        {/* Right: Info & Comments */}
        <div className="md:w-[40%] flex flex-col h-full border-l border-gray-200">
          {/* Header */}
          <div className="p-4 border-b border-gray-100 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 rounded-full bg-gray-200 overflow-hidden">
                {post.user.avatarUrl && <img src={post.user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />}
              </div>
              <span className="font-semibold text-sm">{post.user.username}</span>
            </div>
            <button className="text-gray-500 hover:text-gray-900 transition">
              <FiMoreHorizontal size={20} />
            </button>
          </div>

          {/* Caption & Comments Area */}
          <div className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-4">
            {/* Caption */}
            <div className="flex space-x-3">
              <div className="w-8 h-8 rounded-full bg-gray-200 overflow-hidden flex-shrink-0">
                {post.user.avatarUrl && <img src={post.user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />}
              </div>
              <div className="text-sm">
                <span className="font-semibold mr-2">{post.user.username}</span>
                <span>{post.caption}</span>
                <div className="text-gray-500 text-xs mt-2 uppercase">
                  {formatRelativePostTime(post.createdAt)}
                </div>
              </div>
            </div>

            {/* Comments Component */}
            <CommentSection postId={postId} highlightCommentId={highlightCommentId} />
          </div>

          {/* Social Actions */}
          <div className="p-4 border-t border-gray-100 space-y-3 bg-white">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <button 
                  onClick={handleLike}
                  className={`transition hover:opacity-50 ${isLiked ? 'text-red-500' : 'text-gray-900'}`}
                >
                  <FiHeart size={24} />
                </button>
                <button className="text-gray-900 hover:opacity-50 transition">
                  <FiMessageCircle size={24} />
                </button>
                <button
                  onClick={() => setIsShareModalOpen(true)}
                  className="text-gray-900 hover:opacity-50 transition"
                  aria-label="Share post"
                >
                  <FiSend size={24} />
                </button>
              </div>
              <button 
                onClick={handleSave}
                className={`transition hover:opacity-50 ${isSaved ? 'text-gray-900' : 'text-gray-900'}`}
              >
                {isSaved ? <BsBookmarkFill size={22} /> : <FiBookmark size={24} />}
              </button>
            </div>
            
            <div>
              <p className="text-sm font-semibold">{likeCount.toLocaleString()} likes</p>
              <p className="text-gray-500 text-[10px] uppercase mt-1">
                {formatRelativePostTime(post.createdAt)}
              </p>
            </div>
          </div>
        </div>
      </div>

      <PostShareModal
        post={post}
        isOpen={isShareModalOpen}
        onClose={() => setIsShareModalOpen(false)}
      />
    </div>
  );
}
