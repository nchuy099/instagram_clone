import { useState, useEffect, useCallback } from 'react';
import type { Post } from '../types';
import api from '../../../lib/axios';

export type PostFeedType = 'feed' | 'user' | 'saved';

export function usePosts(options: { type?: PostFeedType; username?: string } = { type: 'feed' }) {
  const { type = 'feed', username } = options;
  const [posts, setPosts] = useState<Post[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPosts = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      let url = '/posts';
      if (type === 'user' && username) {
        url = `/users/${username}/posts`;
      } else if (type === 'saved') {
        url = '/posts/saved';
      }
      
      const response = await api.get(url);
      setPosts(response.data.data.content || []);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to fetch posts');
      }
    } finally {
      setIsLoading(false);
    }
  }, [type, username]);

  useEffect(() => {
    fetchPosts();
  }, [fetchPosts]);

  return { posts, isLoading, error, refetch: fetchPosts };
}
