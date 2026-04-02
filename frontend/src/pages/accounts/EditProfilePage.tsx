import { useState, useEffect, useRef } from 'react';

import api from '../../lib/axios';
import { useAuth } from '../../hooks/useAuth';
import MainLayout from '../../components/layout/MainLayout';
import { FiLoader, FiUser } from 'react-icons/fi';
import { mediaService } from '../../features/post/services/mediaService';

export default function EditProfilePage() {
  const { user, setUser } = useAuth();


  const [fullName, setFullName] = useState(user?.fullName || '');
  const [username, setUsername] = useState(user?.username || '');
  const [bio, setBio] = useState(user?.bio || '');
  const [website, setWebsite] = useState(user?.websiteUrl || '');
  const [isPrivate, setIsPrivate] = useState(user?.isPrivate || false);
  const [isLoading, setIsLoading] = useState(false);
  const [isAvatarUploading, setIsAvatarUploading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);
  const avatarInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setUsername(user.username || '');
      setBio(user.bio || '');
      setWebsite(user.websiteUrl || '');
      setIsPrivate(user.isPrivate || false);
    }
  }, [user]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setMessage(null);

    try {
      // If username changed, update it separately first (based on current backend controller structure)
      if (username !== user?.username) {
        await api.patch('/users/me/username', { username });
      }

      const response = await api.patch('/me/profile', {
        fullName,
        bio,
        websiteUrl: website,
        isPrivate
      });

      setUser(response.data.data);
      setMessage({ type: 'success', text: 'Profile saved.' });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      setMessage({ 
        type: 'error', 
        text: err.response?.data?.message || 'Failed to update profile.' 
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleAvatarFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || !user) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      setMessage({ type: 'error', text: 'Please choose an image file.' });
      return;
    }

    setIsAvatarUploading(true);
    setMessage(null);

    try {
      const avatarUrl = await mediaService.uploadFile(file);
      const response = await api.patch('/me/profile', { avatarUrl });
      setUser({
        ...user,
        ...response.data.data,
        avatarUrl,
      });
      setMessage({ type: 'success', text: 'Avatar updated.' });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      setMessage({
        type: 'error',
        text: err.response?.data?.message || 'Failed to upload avatar.',
      });
    } finally {
      setIsAvatarUploading(false);
    }
  };

  return (
    <MainLayout>
      <div className="max-w-2xl mx-auto py-8 px-4">
        <h1 className="text-xl font-bold mb-8">Edit profile</h1>

        <div className="bg-white border border-gray-300 rounded-lg p-6 md:p-10">
          <div className="flex items-center mb-8">
            <div className="w-10 h-10 rounded-full overflow-hidden bg-gray-100 flex-shrink-0">
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center bg-gray-200">
                  <FiUser size={18} className="text-gray-400" />
                </div>
              )}
            </div>
            <div className="ml-4">
              <p className="font-bold text-sm">{user?.username}</p>
              <button
                type="button"
                className="inline-flex items-center gap-1 text-[#0095f6] text-sm font-bold hover:text-blue-900 transition-colors disabled:opacity-60"
                onClick={() => avatarInputRef.current?.click()}
                disabled={isAvatarUploading}
              >
                {isAvatarUploading ? <FiLoader size={14} className="animate-spin" /> : null}
                {isAvatarUploading ? 'Uploading...' : 'Change profile photo'}
              </button>
              <input
                ref={avatarInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleAvatarFileChange}
              />
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="flex flex-col md:flex-row md:items-center">
              <label className="md:w-1/4 font-bold text-sm mb-2 md:mb-0">Name</label>
              <div className="md:w-3/4">
                <input
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:border-gray-500 outline-none text-sm"
                  placeholder="Name"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                />
                <p className="text-xs text-gray-500 mt-2">
                  Help people discover your account by using the name you're known by: either your full name, nickname, or business name.
                </p>
              </div>
            </div>

            <div className="flex flex-col md:flex-row md:items-center">
              <label className="md:w-1/4 font-bold text-sm mb-2 md:mb-0">Username</label>
              <div className="md:w-3/4">
                <input
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:border-gray-500 outline-none text-sm"
                  placeholder="Username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </div>
            </div>

            <div className="flex flex-col md:flex-row md:items-center">
              <label className="md:w-1/4 font-bold text-sm mb-2 md:mb-0">Website</label>
              <div className="md:w-3/4">
                <input
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:border-gray-500 outline-none text-sm"
                  placeholder="Website"
                  value={website}
                  onChange={(e) => setWebsite(e.target.value)}
                />
              </div>
            </div>

            <div className="flex flex-col md:flex-row">
              <label className="md:w-1/4 font-bold text-sm mb-2 md:mb-0 mt-2">Bio</label>
              <div className="md:w-3/4">
                <textarea
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:border-gray-500 outline-none text-sm min-h-[80px]"
                  placeholder="Bio"
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                />
                <p className="text-xs text-gray-500 mt-1">{bio.length} / 150</p>
              </div>
            </div>

            <div className="flex flex-col md:flex-row md:items-center">
              <div className="md:w-1/4"></div>
              <div className="md:w-3/4 flex items-center">
                <input
                  type="checkbox"
                  id="isPrivate"
                  className="mr-2"
                  checked={isPrivate}
                  onChange={(e) => setIsPrivate(e.target.checked)}
                />
                <label htmlFor="isPrivate" className="font-bold text-sm">Private account</label>
              </div>
            </div>

            <div className="flex flex-col md:flex-row md:items-center">
              <div className="md:w-1/4"></div>
              <div className="md:w-3/4">
                <button
                  type="submit"
                  disabled={isLoading || isAvatarUploading}
                  className="bg-[#0095f6] hover:bg-[#1877f2] text-white px-4 py-2 rounded font-bold text-sm disabled:opacity-50"
                >
                  {isLoading ? 'Saving...' : 'Submit'}
                </button>
              </div>
            </div>

            {message && (
              <div className={`mt-4 text-sm text-center ${message.type === 'success' ? 'text-green-600' : 'text-red-600'}`}>
                {message.text}
              </div>
            )}
          </form>
        </div>
      </div>
    </MainLayout>
  );
}
