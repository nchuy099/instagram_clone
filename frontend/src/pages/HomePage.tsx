import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FiLoader } from 'react-icons/fi';
import MainLayout from '../components/layout/MainLayout';
import { usePosts } from '../features/post/hooks/usePosts';
import PostCard from '../features/post/components/PostCard';
import StoriesBar from '../features/story/components/StoriesBar';
import api from '../lib/axios';
import { useAuth } from '../hooks/useAuth';

interface HomeSuggestion {
  id: string;
  username: string;
  fullName?: string;
  avatarUrl?: string;
  subtitle?: string;
}

export default function HomePage() {
  const { posts, isLoading, error } = usePosts({ type: 'feed' });
  const { user } = useAuth();
  const [suggestions, setSuggestions] = useState<HomeSuggestion[]>([]);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(true);

  useEffect(() => {
    const fetchSuggestions = async () => {
      setIsLoadingSuggestions(true);
      try {
        const response = await api.get('/users/suggestions');
        setSuggestions(response.data?.data ?? []);
      } catch {
        setSuggestions([]);
      } finally {
        setIsLoadingSuggestions(false);
      }
    };

    fetchSuggestions();
  }, []);

  return (
    <MainLayout>
      <div className="mx-auto flex w-full max-w-[980px] gap-10 px-4 py-8">
        <div className="w-full max-w-[470px]">
          <StoriesBar />
          {isLoading ? (
            <div className="mt-20 flex flex-col items-center justify-center space-y-4">
              <FiLoader className="animate-spin text-gray-400" size={32} />
              <p className="text-sm text-gray-500">Loading feed...</p>
            </div>
          ) : error ? (
            <div className="mt-20 text-center text-red-500">
              <p>{error}</p>
              <button
                onClick={() => window.location.reload()}
                className="mt-4 text-sm font-semibold text-[#0095f6]"
              >
                Try again
              </button>
            </div>
          ) : posts.length === 0 ? (
            <div className="mt-20 text-center text-gray-500">
              <p className="mb-2 text-xl font-bold">Welcome to Instagram</p>
              <p className="text-sm">When you follow people, their posts will show up here.</p>
            </div>
          ) : (
            <div className="space-y-4 pb-12">
              {posts.map((post) => (
                <PostCard key={post.id} post={post} />
              ))}
            </div>
          )}
        </div>

        <aside className="sticky top-8 hidden h-fit w-[320px] lg:block">
          <div className="mb-5 flex items-center justify-between">
            <div className="flex min-w-0 items-center gap-3">
              <div className="h-11 w-11 overflow-hidden rounded-full bg-gray-200">
                {user?.avatarUrl ? (
                  <img src={user.avatarUrl} alt={user.username} className="h-full w-full object-cover" />
                ) : (
                  <div className="flex h-full w-full items-center justify-center text-xs text-gray-500">U</div>
                )}
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-gray-900">{user?.username || 'username'}</p>
                <p className="truncate text-sm text-gray-500">{user?.fullName || ''}</p>
              </div>
            </div>
            <button type="button" className="text-xs font-semibold text-[#0095f6]">
              Switch
            </button>
          </div>

          <div className="mb-4 flex items-center justify-between">
            <p className="text-sm font-semibold text-gray-500">Suggested for you</p>
            <button type="button" className="text-xs font-semibold text-gray-900">
              See all
            </button>
          </div>

          {isLoadingSuggestions ? (
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <FiLoader size={14} className="animate-spin" />
              Loading suggestions...
            </div>
          ) : (
            <div className="space-y-3">
              {suggestions.map((suggestion) => (
                <div key={suggestion.id} className="flex items-center justify-between gap-3">
                  <Link to={`/${suggestion.username}`} className="flex min-w-0 items-center gap-3">
                    <div className="h-11 w-11 overflow-hidden rounded-full bg-gray-200">
                      {suggestion.avatarUrl ? (
                        <img src={suggestion.avatarUrl} alt={suggestion.username} className="h-full w-full object-cover" />
                      ) : null}
                    </div>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-gray-900">{suggestion.username}</p>
                      <p className="truncate text-xs text-gray-500">{suggestion.subtitle || 'Suggested for you'}</p>
                    </div>
                  </Link>
                  <button type="button" className="shrink-0 text-xs font-semibold text-[#0095f6]">
                    Follow
                  </button>
                </div>
              ))}
            </div>
          )}
        </aside>
      </div>
    </MainLayout>
  );
}

