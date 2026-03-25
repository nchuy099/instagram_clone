import { useEffect, useState, useRef } from 'react';
import { useAuth } from '../../../hooks/useAuth';
import { mediaService } from '../../post/services/mediaService';
import { storyService } from '../services/storyService';
import type { Story as StoryType } from '../services/storyService';
import { Plus, Loader2 } from 'lucide-react';
import StoryViewer from './StoryViewer';

export default function StoriesBar() {
  const { user } = useAuth();
  const [stories, setStories] = useState<any[]>([]);
  const [groupedStories, setGroupedStories] = useState<Record<string, StoryType[]>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [viewerState, setViewerState] = useState<{ isOpen: boolean; username: string | null }>({
    isOpen: false,
    username: null
  });
  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchStories = async () => {
    try {
      const groupedData = await storyService.getGroupedStories();
      setGroupedStories(groupedData);
      
      // Convert grouped data to a list of unique users for the bar
      const usersList = Object.keys(groupedData).map(username => ({
        username,
        userAvatarUrl: groupedData[username][0].userAvatarUrl,
        id: groupedData[username][0].id
      }));
      setStories(usersList);
    } catch (err) {
      console.error('Failed to fetch stories', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchStories();
  }, []);

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsCreating(true);
    try {
      const mediaUrl = await mediaService.uploadFile(file);
      const mediaType = file.type.startsWith('video') ? 'VIDEO' : 'IMAGE';
      
      await storyService.createStory({ mediaUrl, mediaType });
      await fetchStories();
    } catch (err) {
      console.error('Failed to create story', err);
      alert('Failed to create story. Please try again.');
    } finally {
      setIsCreating(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleUserClick = (username: string) => {
    setViewerState({ isOpen: true, username });
  };

  const getUserSequence = () => Object.keys(groupedStories);

  const handleNextUser = () => {
    const sequence = getUserSequence();
    const currentIndex = sequence.indexOf(viewerState.username || '');
    if (currentIndex < sequence.length - 1) {
      setViewerState({ isOpen: true, username: sequence[currentIndex + 1] });
    } else {
      setViewerState({ isOpen: false, username: null });
    }
  };

  const handlePrevUser = () => {
    const sequence = getUserSequence();
    const currentIndex = sequence.indexOf(viewerState.username || '');
    if (currentIndex > 0) {
      setViewerState({ isOpen: true, username: sequence[currentIndex - 1] });
    }
  };

  if (isLoading) {
    return (
      <div className="flex space-x-4 p-4 overflow-x-auto bg-white border border-gray-300 rounded-lg mb-6">
        {[...Array(8)].map((_, i) => (
          <div key={i} className="flex-shrink-0 flex flex-col items-center space-y-1">
            <div className="w-16 h-16 rounded-full bg-gray-200 animate-pulse border-2 border-white ring-2 ring-gray-100" />
            <div className="w-12 h-2 bg-gray-200 animate-pulse rounded" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <>
      <div className="flex space-x-4 p-4 overflow-x-auto bg-white border border-gray-300 rounded-lg mb-6 custom-scrollbar shadow-sm">
        {/* Create Story Button */}
        <div className="flex-shrink-0 flex flex-col items-center space-y-1 cursor-pointer group">
          <div 
            className="relative"
            onClick={() => {
              // If user has stories, view them. If not, trigger upload.
              if (groupedStories[user?.username || '']) {
                handleUserClick(user?.username || '');
              } else {
                fileInputRef.current?.click();
              }
            }}
          >
            <div className={`p-[2px] rounded-full ${groupedStories[user?.username || ''] ? 'bg-gradient-to-tr from-yellow-400 to-fuchsia-600' : 'bg-gray-200 group-hover:bg-gray-300'} transition-all`}>
              {user?.avatarUrl ? (
                <img 
                  src={user.avatarUrl} 
                  alt="Your story"
                  className="w-14 h-14 rounded-full border-2 border-white object-cover"
                />
              ) : (
                <div className="w-14 h-14 rounded-full border-2 border-white bg-gray-100 flex items-center justify-center text-gray-500 font-bold text-xs uppercase">
                  {user?.username?.substring(0, 2)}
                </div>
              )}
            </div>
            {!groupedStories[user?.username || ''] && (
              <div 
                className="absolute bottom-0 right-0 bg-[#0095f6] rounded-full p-1 border-2 border-white shadow-sm"
                onClick={(e) => { e.stopPropagation(); fileInputRef.current?.click(); }}
              >
                {isCreating ? (
                  <Loader2 className="animate-spin text-white" size={10} />
                ) : (
                  <Plus className="text-white" size={12} strokeWidth={4} />
                )}
              </div>
            )}
          </div>
          <span className="text-xs text-gray-500 truncate w-16 text-center">Your story</span>
          <input 
            type="file" 
            ref={fileInputRef} 
            className="hidden" 
            accept="image/*,video/*"
            onChange={handleFileSelect}
          />
        </div>

        {stories.map((story) => (
          // Skip current user if already displayed
          story.username === user?.username ? null : (
            <div 
              key={story.id} 
              className="flex-shrink-0 flex flex-col items-center space-y-1 cursor-pointer group"
              onClick={() => handleUserClick(story.username)}
            >
              <div className="p-[2px] rounded-full bg-gradient-to-tr from-yellow-400 to-fuchsia-600 group-hover:scale-105 transition-transform duration-200">
                {story.userAvatarUrl ? (
                  <img 
                    src={story.userAvatarUrl} 
                    alt={story.username}
                    className="w-14 h-14 rounded-full border-2 border-white object-cover"
                  />
                ) : (
                  <div className="w-14 h-14 rounded-full border-2 border-white bg-gray-100 flex items-center justify-center text-gray-400 font-bold text-xs uppercase">
                    {story.username.substring(0, 2)}
                  </div>
                )}
              </div>
              <span className="text-xs text-gray-700 truncate w-16 text-center">{story.username}</span>
            </div>
          )
        ))}
      </div>

      {viewerState.isOpen && viewerState.username && (
        <StoryViewer 
          stories={groupedStories[viewerState.username]} 
          onClose={() => setViewerState({ isOpen: false, username: null })}
          onNextUser={handleNextUser}
          onPrevUser={handlePrevUser}
        />
      )}
    </>
  );
}


