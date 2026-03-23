import { X } from 'lucide-react';
import type { Post } from '../types';
import MediaCarousel from './MediaCarousel';
import CommentSection from './CommentSection';
import { Link } from 'react-router-dom';

interface CommentModalProps {
  post: Post;
  isOpen: boolean;
  onClose: () => void;
}

export default function CommentModal({ post, isOpen, onClose }: CommentModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <button 
        onClick={onClose}
        className="absolute top-4 right-4 text-white hover:text-gray-300 transition"
      >
        <X size={32} />
      </button>

      <div className="bg-white w-full max-w-5xl h-[90vh] flex flex-col md:flex-row rounded-lg overflow-hidden shadow-2xl animate-in zoom-in-95 duration-200">
        {/* Left: Media */}
        <div className="flex-1 bg-black flex items-center justify-center min-h-[300px] md:min-h-0">
          <MediaCarousel media={post.media} />
        </div>

        {/* Right: Comments & Details */}
        <div className="w-full md:w-[400px] flex flex-col h-full bg-white border-l border-gray-100">
          {/* Post Owner Header */}
          <div className="p-4 border-b border-gray-100 flex items-center space-x-3">
             <Link to={`/${post.user.username}`} className="w-8 h-8 rounded-full bg-gray-200 overflow-hidden flex-shrink-0">
               {post.user.avatarUrl && <img src={post.user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />}
             </Link>
             <div className="flex-1 min-w-0">
               <div className="flex items-center space-x-2">
                 <Link to={`/${post.user.username}`} className="font-semibold text-sm hover:underline truncate">
                   {post.user.username}
                 </Link>
                 <span className="text-gray-500">•</span>
                 <button className="text-[#0095f6] font-semibold text-xs hover:text-[#00376b]">Follow</button>
               </div>
               {post.location && <p className="text-xs text-gray-500 truncate">{post.location}</p>}
             </div>
          </div>

          {/* Caption (rendered as first comment) */}
          <div className="p-4 flex space-x-3 border-b border-gray-50/50">
            <div className="w-8 h-8 rounded-full bg-gray-200 overflow-hidden flex-shrink-0">
               {post.user.avatarUrl && <img src={post.user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />}
            </div>
            <div>
              <p className="text-sm">
                <span className="font-semibold mr-2">{post.user.username}</span>
                {post.caption}
              </p>
              <p className="text-xs text-gray-500 mt-2 font-medium uppercase tracking-tighter">1h</p>
            </div>
          </div>

          {/* Comments List */}
          <div className="flex-1 overflow-hidden">
            <CommentSection postId={post.id} />
          </div>
        </div>
      </div>
    </div>
  );
}
