import { useEffect, useRef, useState } from 'react';
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
import { mediaService } from '../../features/post/services/mediaService';
import { toast } from 'react-toastify';

export default function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { user, setUser } = useAuth();
  
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [profile, setProfile] = useState<any>(null);
  const [isProfileLoading, setIsProfileLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'posts' | 'saved' | 'tagged'>('posts');
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const [isAvatarUploading, setIsAvatarUploading] = useState(false);
  const avatarInputRef = useRef<HTMLInputElement>(null);

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

  const handleAvatarSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';

    if (!file || !isOwnProfile || !user) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      toast.error('Please choose an image file.');
      return;
    }

    setIsAvatarUploading(true);
    try {
      const avatarUrl = await mediaService.uploadFile(file);
      const response = await api.patch('/me/profile', { avatarUrl });

      setProfile((prev: typeof profile) => (prev ? { ...prev, avatarUrl } : prev));
      setUser({
        ...user,
        ...response.data.data,
        avatarUrl,
      });
      toast.success('Avatar updated.');
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to update avatar.');
    } finally {
      setIsAvatarUploading(false);
    }
  };

  if (!fetchUsername && !user) return <Navigate to="/login" />;

  return (
    <MainLayout>
      {isProfileLoading ? (
        <div className="flex justify-center mt-20 text-gray-500">Loading profile...</div>
      ) : profile ? (
        <div className="max-w-4xl mx-auto py-8 px-4 sm:px-8">
          <ProfileHeader
            profile={profile}
            onToggleFollow={handleToggleFollow}
            onAvatarClick={() => {
              if (isOwnProfile && !isAvatarUploading) {
                avatarInputRef.current?.click();
              }
            }}
            isAvatarUploading={isAvatarUploading}
          />
          {isOwnProfile ? (
            <input
              ref={avatarInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handleAvatarSelect}
            />
          ) : null}
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
