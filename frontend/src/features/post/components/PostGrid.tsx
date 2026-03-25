import { Grid, Heart, MessageCircle } from 'lucide-react';
import type { Post } from '../types';
import { MediaType } from '../types';

interface PostGridProps {
  posts: Post[];
  isLoading?: boolean;
  onPostClick?: (postId: string) => void;
}

export default function PostGrid({ posts, isLoading, onPostClick }: PostGridProps) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-3 gap-1 sm:gap-8 mt-4">
        {[...Array(6)].map((_, i) => (
          <div key={i} className="aspect-square bg-gray-100 animate-pulse" />
        ))}
      </div>
    );
  }

  if (posts.length === 0) {
    return (
      <div className="flex flex-col items-center mt-16 text-center text-gray-500">
        <div className="w-16 h-16 border-2 border-gray-800 rounded-full flex items-center justify-center mx-auto mb-4">
          <Grid size={32} className="text-gray-800" />
        </div>
        <h2 className="text-2xl font-extrabold text-gray-900 mb-2 mt-6">No Posts Yet</h2>
        <p className="max-w-xs mx-auto text-sm">When they share posts, they will appear on their profile.</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-3 gap-1 sm:gap-8 mt-4">
      {posts.map((post) => {
        const firstMedia = post.media[0];
        const isVideo = firstMedia?.type === MediaType.VIDEO;
        const thumbnailSrc = isVideo ? (firstMedia.thumbnailUrl || firstMedia.url) : firstMedia?.url;

        return (
          <div
            key={post.id}
            className="relative aspect-square group cursor-pointer overflow-hidden bg-gray-200"
            onClick={() => onPostClick?.(post.id)}
          >
            {thumbnailSrc ? (
              <img
                src={thumbnailSrc}
                alt="post thumbnail"
                className="w-full h-full object-cover transition duration-300 group-hover:brightness-50"
              />
            ) : null}

            {post.media.length > 1 && (
              <div className="absolute top-2 right-2 shadow-sm">
                <svg aria-label="Carousel" className="text-white fill-current drop-shadow-md" height="20" role="img" viewBox="0 0 48 48" width="20">
                  <path d="M34.8 29.8V5.1c0-.1 0-.2.1-.4.1-.1.2-.2.3-.3.1-.1.3-.1.4-.1h2.4c.2 0 .3.1.4.1.1.1.2.2.3.3.1.1.1.3.1.4v24.7c0 .3-.1.6-.3.9-.2.2-.5.3-.8.3h-2.4c-.1 0-.3-.1-.4-.1-.1-.1-.2-.2-.3-.3-.1-.1-.1-.2-.1-.4zm7.6 0V5.1c0-.1 0-.2.1-.4.1-.1.2-.2.3-.3.1-.1.3-.1.4-.1h2.4c.2 0 .3.1.4.1.1.1.2.2.3.3.1.1.1.3.1.4v24.7c0 .3-.1.6-.3.9-.2.2-.5.3-.8.3h-2.4c-.1 0-.3-.1-.4-.1-.1-.1-.2-.2-.3-.3-.1-.1-.1-.2-.1-.4zM3.4 44.1c-.2 0-.3-.1-.4-.1-.1-.1-.2-.2-.3-.3-.1-.1-.1-.3-.1-.4V5.1c0-.1 0-.2.1-.4.1-.1.2-.2.3-.3.1-.1.3-.1.4-.1h28.7c.1 0 .3.1.4.1.1.1.2.2.3.3.1.1.1.3.1.4v38.3c0 .1 0 .2-.1.4-.1.1-.2.2-.3.3-.1.1-.3.1-.4.1H3.4z"></path>
                </svg>
              </div>
            )}

            <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition duration-200 text-white font-bold space-x-6">
              <div className="flex items-center space-x-1.5">
                <Heart size={20} fill="currentColor" />
                <span>{post.likeCount}</span>
              </div>
              <div className="flex items-center space-x-1.5">
                <MessageCircle size={20} fill="currentColor" />
                <span>{post.commentCount}</span>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
