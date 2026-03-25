import { useState } from 'react';
import MainLayout from '../components/layout/MainLayout';
import { usePosts } from '../features/post/hooks/usePosts';
import PostGrid from '../features/post/components/PostGrid';
import PostDetailModal from '../features/post/components/PostDetailModal';

export default function ExplorePage() {
  const { posts, isLoading } = usePosts({ type: 'explore' });
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);

  return (
    <MainLayout>
      <div className="max-w-4xl mx-auto py-8 px-4">
        <h1 className="text-2xl font-bold mb-6">Explore</h1>
        
        <PostGrid 
          posts={posts} 
          isLoading={isLoading} 
          onPostClick={setSelectedPostId} 
        />

        {selectedPostId && (
          <PostDetailModal 
            postId={selectedPostId} 
            onClose={() => setSelectedPostId(null)} 
          />
        )}
      </div>
    </MainLayout>
  );
}
