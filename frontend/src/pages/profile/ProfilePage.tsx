import { useEffect, useState } from 'react';
import { useParams, Navigate } from 'react-router-dom';
import api from '../../lib/axios';
import MainLayout from '../../components/layout/MainLayout';
import ProfileHeader from '../../features/profile/components/ProfileHeader';
import ProfileTabs from '../../features/profile/components/ProfileTabs';
import { useAuth } from '../../hooks/useAuth';

export default function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { user } = useAuth();
  
  const [profile, setProfile] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchUsername = username === 'me' ? user?.username : username;

  useEffect(() => {
    if (!fetchUsername) return;
    
    setIsLoading(true);
    api.get(`/users/${fetchUsername}`)
      .then(res => setProfile(res.data.data))
      .catch(err => console.error(err))
      .finally(() => setIsLoading(false));
  }, [fetchUsername]);

  const handleToggleFollow = async () => {
    if (!profile) return;
    try {
      if (profile.isFollowing) {
        await api.delete(`/users/${profile.id}/follow`);
        setProfile((prev: any) => ({ ...prev, isFollowing: false, followerCount: prev.followerCount - 1 }));
      } else {
        await api.post(`/users/${profile.id}/follow`);
        setProfile((prev: any) => ({ ...prev, isFollowing: true, followerCount: prev.followerCount + 1 }));
      }
    } catch (e) {
      console.error(e);
    }
  };

  if (!fetchUsername) return <Navigate to="/login" />;

  return (
    <MainLayout>
      {isLoading ? (
        <div className="flex justify-center mt-20 text-gray-500">Loading profile...</div>
      ) : profile ? (
        <div className="max-w-4xl mx-auto py-8 px-4 sm:px-8">
          <ProfileHeader profile={profile} onToggleFollow={handleToggleFollow} />
          <ProfileTabs />
        </div>
      ) : (
        <div className="flex justify-center mt-20 text-red-500">User not found</div>
      )}
    </MainLayout>
  );
}
