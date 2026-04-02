import { useState } from 'react';
import { FiCamera, FiLoader, FiSettings, FiUser } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import UserListModal from './UserListModal';

interface ProfileHeaderProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  profile: any;
  onToggleFollow: () => void;
  onAvatarClick?: () => void;
  isAvatarUploading?: boolean;
}

export default function ProfileHeader({
  profile,
  onToggleFollow,
  onAvatarClick,
  isAvatarUploading = false,
}: ProfileHeaderProps) {
  const [activeModal, setActiveModal] = useState<'followers' | 'following' | null>(null);

  return (
    <div className="flex flex-col md:flex-row mb-12 border-b border-gray-300 pb-10 font-sans">
      <div className="md:w-1/3 flex justify-center items-start mb-6 md:mb-0">
        <button
          type="button"
          onClick={profile.isOwnProfile ? onAvatarClick : undefined}
          className={`group relative w-20 h-20 sm:w-36 sm:h-36 rounded-full overflow-hidden bg-gray-200 border border-gray-100 flex items-center justify-center flex-shrink-0 shadow-sm ${
            profile.isOwnProfile ? 'cursor-pointer hover:opacity-90 transition-opacity' : 'cursor-default'
          }`}
          aria-label={profile.isOwnProfile ? 'Change avatar' : 'User avatar'}
        >
          {profile.avatarUrl ? (
            <img src={profile.avatarUrl} alt="avatar" className="w-full h-full object-cover" />
          ) : (
            <FiUser size={48} className="text-gray-400" />
          )}
          {isAvatarUploading ? (
            <span className="absolute inset-0 flex items-center justify-center bg-black/35 text-white">
              <FiLoader size={22} className="animate-spin" />
            </span>
          ) : null}
          {profile.isOwnProfile && !isAvatarUploading ? (
            <>
              <span className="absolute bottom-1 right-1 rounded-full bg-black/70 p-1 text-white">
                <FiCamera size={12} />
              </span>
              <span className="absolute inset-0 hidden items-center justify-center bg-black/45 text-[10px] font-semibold uppercase tracking-wide text-white group-hover:flex sm:text-xs">
                Change photo
              </span>
            </>
          ) : null}
        </button>
      </div>
      
      <div className="md:w-2/3 md:ml-8 flex flex-col justify-center">
        <div className="flex flex-col sm:flex-row items-start sm:items-center space-y-4 sm:space-y-0 sm:space-x-4 mb-6">
          <h2 className="text-xl font-normal text-gray-900">{profile.username}</h2>
          
          <div className="flex items-center space-x-2">
            {profile.isOwnProfile ? (
              <>
                <Link 
                  to="/accounts/edit"
                  className="px-4 py-1.5 bg-[#efefef] hover:bg-[#dbdbdb] text-sm font-semibold text-gray-900 rounded-lg transition-colors"
                >
                  Edit profile
                </Link>
                <button className="p-1 hover:bg-gray-100 rounded-lg">
                  <FiSettings size={18} className="text-gray-900" />
                </button>
              </>
            ) : (
              <button 
                onClick={onToggleFollow}
                className={`px-6 py-1.5 text-sm font-semibold rounded-lg transition-colors ${
                  profile.isFollowing 
                  ? 'bg-[#efefef] hover:bg-[#dbdbdb] text-gray-900' 
                  : 'bg-[#0095f6] hover:bg-[#1877f2] text-white'
                }`}
              >
                {profile.isFollowing ? 'Following' : 'Follow'}
              </button>
            )}
          </div>
        </div>
        
        <div className="flex space-x-10 mb-6">
          <div className="text-base"><span className="font-semibold">{profile.postCount || 0}</span> posts</div>
          <button 
            onClick={() => setActiveModal('followers')}
            className="text-base hover:opacity-70 transition-opacity"
          >
            <span className="font-semibold">{profile.followerCount}</span> followers
          </button>
          <button 
            onClick={() => setActiveModal('following')}
            className="text-base hover:opacity-70 transition-opacity"
          >
            <span className="font-semibold">{profile.followingCount}</span> following
          </button>
        </div>
        
        <div className="text-sm">
          <div className="font-semibold">{profile.fullName}</div>
          {profile.bio && <div className="mt-1 whitespace-pre-line text-gray-900">{profile.bio}</div>}
          {profile.websiteUrl && (
            <a href={profile.websiteUrl} target="_blank" rel="noopener noreferrer" className="text-[#00376b] font-semibold mt-1 block">
              {profile.websiteUrl.replace(/^https?:\/\//, '')}
            </a>
          )}
        </div>
      </div>

      <UserListModal 
        isOpen={activeModal === 'followers'} 
        onClose={() => setActiveModal(null)}
        title="Followers"
        username={profile.username}
        type="followers"
      />
      <UserListModal 
        isOpen={activeModal === 'following'} 
        onClose={() => setActiveModal(null)}
        title="Following"
        username={profile.username}
        type="following"
      />
    </div>
  );
}
