import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import MainLayout from '../components/layout/MainLayout';
import api from '../lib/axios';
import PostDetailModal from '../features/post/components/PostDetailModal';

type SearchUser = {
  id: string;
  username: string;
  fullName?: string;
  avatarUrl?: string;
};

type SearchHashtag = {
  name: string;
};

type SearchPost = {
  id: string;
  caption?: string;
  thumbnailUrl?: string;
  username?: string;
};

type SearchResponse = {
  users: SearchUser[];
  hashtags: SearchHashtag[];
  posts: SearchPost[];
};

type RecentSearchItem = {
  id: string;
  queryText: string;
  searchType: 'ALL' | 'USER' | 'HASHTAG' | 'POST';
  user?: SearchUser;
};

const EMPTY_RESULT: SearchResponse = {
  users: [],
  hashtags: [],
  posts: [],
};

export default function SearchPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResponse>(EMPTY_RESULT);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const [recentSearches, setRecentSearches] = useState<RecentSearchItem[]>([]);
  const [isLoadingRecent, setIsLoadingRecent] = useState(false);

  const trimmedQuery = useMemo(() => query.trim(), [query]);
  const isHashtagMode = useMemo(() => trimmedQuery.includes('#'), [trimmedQuery]);

  const fetchRecentSearches = async () => {
    setIsLoadingRecent(true);
    try {
      const response = await api.get('/search/recent');
      const recentItems: RecentSearchItem[] = (response.data?.data ?? []).slice(0, 7);
      setRecentSearches(recentItems);
    } catch {
      setRecentSearches([]);
    } finally {
      setIsLoadingRecent(false);
    }
  };

  const handleUserResultClick = async (user: SearchUser) => {
    try {
      await api.post(`/search/recent/users/${user.id}`, null, {
        params: { q: user.username },
      });
      fetchRecentSearches();
    } catch {
      // no-op
    } finally {
      navigate(`/${user.username}`);
    }
  };

  useEffect(() => {
    fetchRecentSearches();
  }, []);

  useEffect(() => {
    if (!trimmedQuery) {
      setResults(EMPTY_RESULT);
      setError(null);
      setIsLoading(false);
      return;
    }

    const timeout = setTimeout(async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await api.get('/search', { params: { q: trimmedQuery } });
        setResults(response.data?.data ?? EMPTY_RESULT);
        fetchRecentSearches();
      } catch (err: unknown) {
        setResults(EMPTY_RESULT);
        if (err instanceof Error) {
          setError(err.message);
        } else {
          setError('Failed to search');
        }
      } finally {
        setIsLoading(false);
      }
    }, 300);

    return () => clearTimeout(timeout);
  }, [trimmedQuery]);

  return (
    <MainLayout>
      <div className="max-w-4xl mx-auto py-8 px-4">
        <h1 className="text-2xl font-bold mb-4">Search</h1>

        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search users, hashtags, posts"
          className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-gray-300"
        />

        {!trimmedQuery && <p className="text-gray-500 mt-4">Type to start searching.</p>}
        {isLoading && <p className="text-gray-500 mt-4">Searching...</p>}
        {error && <p className="text-red-500 mt-4">{error}</p>}

        {!!trimmedQuery && !isLoading && !error && (
          <div className="mt-6 space-y-8">
            <section>
              <h2 className="text-lg font-semibold mb-3">Users</h2>
              {!isHashtagMode && (results.users.length === 0 ? (
                <p className="text-gray-500">No users found.</p>
              ) : (
                <div className="space-y-3">
                  {results.users.map((user) => (
                    <button
                      type="button"
                      key={user.id}
                      className="w-full text-left flex items-center gap-3 hover:bg-gray-50 rounded-md p-2 -m-2 transition"
                      onClick={() => handleUserResultClick(user)}
                    >
                      <div className="w-9 h-9 rounded-full bg-gray-200 overflow-hidden">
                        {user.avatarUrl ? (
                          <img src={user.avatarUrl} alt={user.username} className="w-full h-full object-cover" />
                        ) : null}
                      </div>
                      <div>
                        <p className="font-medium">@{user.username}</p>
                        {user.fullName ? <p className="text-sm text-gray-500">{user.fullName}</p> : null}
                      </div>
                    </button>
                  ))}
                </div>
              ))}
            </section>

            <section>
              <h2 className="text-lg font-semibold mb-3">Hashtags</h2>
              {results.hashtags.length === 0 ? (
                <p className="text-gray-500">No hashtags found.</p>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {results.hashtags.map((tag) => (
                    <button
                      type="button"
                      key={tag.name}
                      className="px-3 py-1 rounded-full bg-gray-100 text-sm hover:bg-gray-200 transition"
                      onClick={() => setQuery(`#${tag.name}`)}
                    >
                      #{tag.name}
                    </button>
                  ))}
                </div>
              )}
            </section>

            <section>
              <h2 className="text-lg font-semibold mb-3">Posts</h2>
              {results.posts.length === 0 ? (
                <p className="text-gray-500">No posts found.</p>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {results.posts.map((post) => (
                    <button
                      type="button"
                      key={post.id}
                      className="border border-gray-200 rounded-lg overflow-hidden text-left hover:shadow-sm transition"
                      onClick={() => setSelectedPostId(post.id)}
                    >
                      <div className="aspect-square bg-gray-100">
                        {post.thumbnailUrl ? (
                          <img src={post.thumbnailUrl} alt={post.caption || 'post'} className="w-full h-full object-cover" />
                        ) : null}
                      </div>
                      <div className="p-2">
                        <p className="text-xs text-gray-500">{post.username ? `@${post.username}` : ''}</p>
                        <p className="text-sm line-clamp-2">{post.caption || '(No caption)'}</p>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}

        {!trimmedQuery && !isLoading && !error && (
          <section className="mt-6">
            <h2 className="text-lg font-semibold mb-3">Recent searches</h2>
            {isLoadingRecent ? (
              <p className="text-gray-500">Loading recent searches...</p>
            ) : recentSearches.length === 0 ? (
              <p className="text-gray-500">No recent searches.</p>
            ) : (
              <div className="space-y-2">
                {recentSearches.map((item) => (
                  <div key={item.id} className="flex items-center justify-between border border-gray-200 rounded-lg px-3 py-2">
                    {item.searchType === 'USER' && item.user ? (
                      <Link to={`/${item.user.username}`} className="flex items-center gap-3 min-w-0">
                        <div className="w-8 h-8 rounded-full bg-gray-200 overflow-hidden shrink-0">
                          {item.user.avatarUrl ? (
                            <img src={item.user.avatarUrl} alt={item.user.username} className="w-full h-full object-cover" />
                          ) : null}
                        </div>
                        <div className="min-w-0">
                          <p className="font-medium truncate">@{item.user.username}</p>
                          {item.user.fullName ? <p className="text-sm text-gray-500 truncate">{item.user.fullName}</p> : null}
                        </div>
                      </Link>
                    ) : (
                      <button
                        type="button"
                        className="text-left hover:underline"
                        onClick={() => {
                          const nextQuery = item.searchType === 'HASHTAG' && !item.queryText.startsWith('#')
                            ? `#${item.queryText}`
                            : item.queryText;
                          setQuery(nextQuery);
                        }}
                      >
                        {item.searchType === 'HASHTAG' && !item.queryText.startsWith('#') ? `#${item.queryText}` : item.queryText}
                      </button>
                    )}
                    <button
                      type="button"
                      className="text-sm text-gray-500 hover:text-red-500"
                      onClick={async () => {
                        try {
                          await api.delete(`/search/recent/${item.id}`);
                          setRecentSearches((prev) => prev.filter((x) => x.id !== item.id));
                        } catch {
                          // no-op
                        }
                      }}
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

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
