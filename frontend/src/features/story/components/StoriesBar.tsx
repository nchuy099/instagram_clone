import { useEffect, useRef, useState } from "react";
import { FiPlus, FiLoader } from "react-icons/fi";
import { useAuth } from "../../../hooks/useAuth";
import { mediaService } from "../../post/services/mediaService";
import StoryViewer from "./StoryViewer";
import StoryComposerModal from "./StoryComposerModal";
import { storyService } from "../services/storyService";
import type { Story as StoryType } from "../services/storyService";
import type { StoryMediaType } from "./storyComposer/types";

interface StoryUserPreview {
  id: string;
  username: string;
  userAvatarUrl: string;
}

export default function StoriesBar() {
  const { user } = useAuth();
  const [stories, setStories] = useState<StoryUserPreview[]>([]);
  const [groupedStories, setGroupedStories] = useState<Record<string, StoryType[]>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [composerFile, setComposerFile] = useState<File | null>(null);
  const [viewerState, setViewerState] = useState<{ isOpen: boolean; username: string | null }>({
    isOpen: false,
    username: null
  });

  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchStories = async () => {
    try {
      const groupedData = await storyService.getGroupedStories();
      setGroupedStories(groupedData);

      const usersList = Object.keys(groupedData).map((username) => ({
        username,
        userAvatarUrl: groupedData[username][0].userAvatarUrl,
        id: groupedData[username][0].id
      }));
      setStories(usersList);
    } catch (err) {
      console.error("Failed to fetch stories", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchStories();
  }, []);

  const applyStoryUpdate = (updatedStory: StoryType) => {
    setGroupedStories((prev) => {
      const next: Record<string, StoryType[]> = {};
      for (const [username, userStories] of Object.entries(prev)) {
        next[username] = userStories.map((story) => (story.id === updatedStory.id ? updatedStory : story));
      }
      return next;
    });
  };

  const handleLikeStory = async (storyId: string, currentlyLiked: boolean) => {
    const updatedStory = currentlyLiked
      ? await storyService.unlikeStory(storyId)
      : await storyService.likeStory(storyId);

    applyStoryUpdate(updatedStory);
    return updatedStory;
  };

  const handleReplyStory = async (storyId: string, content: string) => {
    const updatedStory = await storyService.replyStory(storyId, content);
    applyStoryUpdate(updatedStory);
    return updatedStory;
  };

  const handleShareStory = async (storyId: string) => {
    const updatedStory = await storyService.shareStory(storyId);
    applyStoryUpdate(updatedStory);
    return updatedStory;
  };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file == null) {
      return;
    }

    setComposerFile(file);
  };

  const handleComposerSubmit = async (renderedFile: File, mediaType: StoryMediaType) => {
    setIsCreating(true);

    try {
      const mediaUrl = await mediaService.uploadFile(renderedFile);
      await storyService.createStory({ mediaUrl, mediaType });
      await fetchStories();
    } catch (err) {
      console.error("Failed to create story", err);
      alert("Failed to create story. Please try again.");
      throw err;
    } finally {
      setIsCreating(false);
      setComposerFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleUserClick = (username: string) => {
    setViewerState({ isOpen: true, username });
  };

  const getUserSequence = () => Object.keys(groupedStories);

  const handleNextUser = () => {
    const sequence = getUserSequence();
    const currentIndex = sequence.indexOf(viewerState.username || "");
    if (currentIndex < sequence.length - 1) {
      setViewerState({ isOpen: true, username: sequence[currentIndex + 1] });
    } else {
      setViewerState({ isOpen: false, username: null });
    }
  };

  const handlePrevUser = () => {
    const sequence = getUserSequence();
    const currentIndex = sequence.indexOf(viewerState.username || "");
    if (currentIndex > 0) {
      setViewerState({ isOpen: true, username: sequence[currentIndex - 1] });
    }
  };

  if (isLoading) {
    return (
      <div className="mb-6 flex space-x-4 overflow-x-auto rounded-lg border border-gray-300 bg-white p-4">
        {[...Array(8)].map((_, index) => (
          <div key={index} className="flex flex-shrink-0 flex-col items-center space-y-1">
            <div className="h-16 w-16 animate-pulse rounded-full border-2 border-white bg-gray-200 ring-2 ring-gray-100" />
            <div className="h-2 w-12 animate-pulse rounded bg-gray-200" />
          </div>
        ))}
      </div>
    );
  }

  const ownUsername = user?.username || "";
  const hasOwnStory = groupedStories[ownUsername] != null;

  return (
    <>
      <div className="custom-scrollbar mb-6 flex space-x-4 overflow-x-auto rounded-lg border border-gray-300 bg-white p-4 shadow-sm">
        <div className="group flex flex-shrink-0 cursor-pointer flex-col items-center space-y-1">
          <div
            className="relative"
            onClick={() => {
              if (hasOwnStory) {
                handleUserClick(ownUsername);
              } else {
                fileInputRef.current?.click();
              }
            }}
          >
            <div
              className={
                hasOwnStory
                  ? "rounded-full bg-gradient-to-tr from-yellow-400 to-fuchsia-600 p-[2px] transition-all"
                  : "rounded-full bg-gray-200 p-[2px] transition-all group-hover:bg-gray-300"
              }
            >
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt="Your story" className="h-14 w-14 rounded-full border-2 border-white object-cover" />
              ) : (
                <div className="flex h-14 w-14 items-center justify-center rounded-full border-2 border-white bg-gray-100 text-xs font-bold uppercase text-gray-500">
                  {user?.username?.substring(0, 2)}
                </div>
              )}
            </div>
            {hasOwnStory ? null : (
              <div
                className="absolute bottom-0 right-0 rounded-full border-2 border-white bg-[#0095f6] p-1 shadow-sm"
                onClick={(event) => {
                  event.stopPropagation();
                  fileInputRef.current?.click();
                }}
              >
                {isCreating ? (
                  <FiLoader className="animate-spin text-white" size={10} />
                ) : (
                  <FiPlus className="text-white" size={10} />
                )}
              </div>
            )}
          </div>
          <span className="w-16 truncate text-center text-xs text-gray-500">Your story</span>
          <input
            type="file"
            ref={fileInputRef}
            className="hidden"
            accept="image/*,video/*"
            onChange={handleFileSelect}
          />
        </div>

        {stories.map((story) =>
          story.username === user?.username ? null : (
            <div
              key={story.id}
              className="group flex flex-shrink-0 cursor-pointer flex-col items-center space-y-1"
              onClick={() => handleUserClick(story.username)}
            >
              <div className="rounded-full bg-gradient-to-tr from-yellow-400 to-fuchsia-600 p-[2px] transition-transform duration-200 group-hover:scale-105">
                {story.userAvatarUrl ? (
                  <img
                    src={story.userAvatarUrl}
                    alt={story.username}
                    className="h-14 w-14 rounded-full border-2 border-white object-cover"
                  />
                ) : (
                  <div className="flex h-14 w-14 items-center justify-center rounded-full border-2 border-white bg-gray-100 text-xs font-bold uppercase text-gray-400">
                    {story.username.substring(0, 2)}
                  </div>
                )}
              </div>
              <span className="w-16 truncate text-center text-xs text-gray-700">{story.username}</span>
            </div>
          )
        )}
      </div>

      {viewerState.isOpen && viewerState.username && (
        <StoryViewer
          stories={groupedStories[viewerState.username] || []}
          onClose={() => setViewerState({ isOpen: false, username: null })}
          onNextUser={handleNextUser}
          onPrevUser={handlePrevUser}
          onLike={handleLikeStory}
          onReply={handleReplyStory}
          onShare={handleShareStory}
        />
      )}

      {composerFile && (
        <StoryComposerModal
          file={composerFile}
          onClose={() => {
            setComposerFile(null);
            if (fileInputRef.current) {
              fileInputRef.current.value = "";
            }
          }}
          onSubmit={handleComposerSubmit}
        />
      )}
    </>
  );
}
