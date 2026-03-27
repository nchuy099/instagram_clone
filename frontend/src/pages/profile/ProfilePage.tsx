import { useEffect, useState } from 'react';
import { useParams, Navigate } from 'react-router-dom';
import api from '../../lib/axios';
import MainLayout from '../../components/layout/MainLayout';
import ProfileHeader from '../../features/profile/components/ProfileHeader';
import ProfileTabs from '../../features/profile/components/ProfileTabs';
import OwnStoriesSection from '../../features/profile/components/OwnStoriesSection';
import { useAuth } from '../../hooks/useAuth';
import { usePosts } from '../../features/post/hooks/usePosts';
import PostGrid from '../../features/post/components/PostGrid';
import PostDetailModal from '../../features/post/components/PostDetailModal';

export default function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { user } = useAuth();
  
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [profile, setProfile] = useState<any>(null);
  const [isProfileLoading, setIsProfileLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'posts' | 'saved' | 'tagged'>('posts');
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);

  const fetchUsername = username === 'me' ? user?.username : username;
  const isOwnProfile = user?.username === fetchUsername;
  
  const { posts, isLoading: isPostsLoading } = usePosts({ 
    type: activeTab === 'saved' ? 'saved' : (activeTab === 'posts' ? 'user' : 'feed'), 
    username: fetchUsername 
  });

  useEffect(() => {
    if (!fetchUsername) return;
    
    const fetchProfile = async () => {
      setIsProfileLoading(true);
      try {
        const res = await api.get(`/users/${fetchUsername}`);
        setProfile(res.data.data);
      } catch (err) {
        console.error(err);
      } finally {
        setIsProfileLoading(false);
      }
    };
    fetchProfile();
  }, [fetchUsername]);

  const handleToggleFollow = async () => {
    if (!profile) return;
    try {
      if (profile.isFollowing) {
        await api.delete(`/users/${profile.id}/follow`);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        setProfile((prev: any) => ({ ...prev, isFollowing: false, followerCount: prev.followerCount - 1 }));
      } else {
        await api.post(`/users/${profile.id}/follow`);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        setProfile((prev: any) => ({ ...prev, isFollowing: true, followerCount: prev.followerCount + 1 }));
      }
    } catch (e) {
      console.error(e);
    }
  };

  if (!fetchUsername && !user) return <Navigate to="/login" />;

  return (
    <MainLayout>
      {isProfileLoading ? (
        <div className="flex justify-center mt-20 text-gray-500">Loading profile...</div>
      ) : profile ? (
        <div className="max-w-4xl mx-auto py-8 px-4 sm:px-8">
          <ProfileHeader profile={profile} onToggleFollow={handleToggleFollow} />
          {isOwnProfile ? <OwnStoriesSection username={profile.username} /> : null}
          <ProfileTabs 
            activeTab={activeTab} 
            onTabChange={setActiveTab} 
            showSaved={isOwnProfile}
          />
          <div className="mt-4">
            <PostGrid 
              posts={posts} 
              isLoading={isPostsLoading} 
              onPostClick={setSelectedPostId} 
            />
          </div>

          {selectedPostId && (
            <PostDetailModal 
              postId={selectedPostId} 
              onClose={() => setSelectedPostId(null)} 
            />
          )}
        </div>
      ) : (
        <div className="flex justify-center mt-20 text-red-500">User not found</div>
      )}
    </MainLayout>
  );
}
