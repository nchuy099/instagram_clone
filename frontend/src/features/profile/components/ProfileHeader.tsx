import { User } from 'lucide-react';

interface ProfileHeaderProps {
  profile: any;
  onToggleFollow: () => void;
}

export default function ProfileHeader({ profile, onToggleFollow }: ProfileHeaderProps) {
  return (
    <div className="flex flex-col md:flex-row mb-12 border-b border-gray-300 pb-10">
      <div className="md:w-1/3 flex justify-center items-start mb-6 md:mb-0">
        <div className="w-36 h-36 rounded-full overflow-hidden bg-gray-200 border border-gray-300 flex items-center justify-center">
          {profile.avatarUrl ? (
            <img src={profile.avatarUrl} alt="avatar" className="w-full h-full object-cover" />
          ) : (
            <User size={64} className="text-gray-400" />
          )}
        </div>
      </div>
      
      <div className="md:w-2/3 md:ml-8 flex flex-col justify-center">
        <div className="flex flex-col sm:flex-row items-start sm:items-center space-y-4 sm:space-y-0 sm:space-x-6 mb-4">
          <h2 className="text-xl font-medium text-gray-900">{profile.username}</h2>
          
          {profile.isOwnProfile ? (
            <button className="px-4 py-1.5 bg-gray-100 hover:bg-gray-200 text-sm font-semibold text-gray-900 rounded-lg">
              Edit profile
            </button>
          ) : (
            <button 
              onClick={onToggleFollow}
              className={`px-6 py-1.5 text-sm font-semibold rounded-lg ${
                profile.isFollowing 
                ? 'bg-gray-100 hover:bg-gray-200 text-gray-900' 
                : 'bg-[#0095f6] hover:bg-[#1877f2] text-white'
              }`}
            >
              {profile.isFollowing ? 'Following' : 'Follow'}
            </button>
          )}
        </div>
        
        <div className="flex space-x-8 mb-4">
          <div><span className="font-semibold">{profile.postCount || 0}</span> posts</div>
          <div><span className="font-semibold">{profile.followerCount}</span> followers</div>
          <div><span className="font-semibold">{profile.followingCount}</span> following</div>
        </div>
        
        <div className="text-sm">
          <div className="font-semibold">{profile.fullName}</div>
          {profile.bio && <div className="mt-1 whitespace-pre-line">{profile.bio}</div>}
        </div>
      </div>
    </div>
  );
}
