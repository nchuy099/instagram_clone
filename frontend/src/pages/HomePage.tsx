import MainLayout from '../components/layout/MainLayout';
import { usePosts } from '../features/post/hooks/usePosts';
import PostCard from '../features/post/components/PostCard';
import { Loader2 } from 'lucide-react';

export default function HomePage() {
  const { posts, isLoading, error } = usePosts();

  return (
    <MainLayout>
      <div className="max-w-[470px] mx-auto py-8 px-4">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center mt-20 space-y-4">
            <Loader2 className="animate-spin text-gray-400" size={32} />
            <p className="text-gray-500 text-sm">Loading feed...</p>
          </div>
        ) : error ? (
          <div className="text-center mt-20 text-red-500">
            <p>{error}</p>
            <button
              onClick={() => window.location.reload()}
              className="mt-4 text-[#0095f6] font-semibold text-sm"
            >
              Try again
            </button>
          </div>
        ) : posts.length === 0 ? (
          <div className="text-center mt-20 text-gray-500">
            <p className="text-xl font-bold mb-2">Welcome to Instagram</p>
            <p className="text-sm">When you follow people, their posts will show up here.</p>
          </div>
        ) : (
          <div className="space-y-4 pb-12">
            {posts.map(post => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
        )}
      </div>
    </MainLayout>
  );
}
