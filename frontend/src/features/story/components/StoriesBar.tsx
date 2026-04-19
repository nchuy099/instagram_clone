import { useEffect, useRef, useState } from "react";
import { FiPlus, FiLoader } from "react-icons/fi";
import { useAuth } from "../../../hooks/useAuth";
import { mediaService } from "../../post/services/mediaService";
import StoryViewer from "./StoryViewer";
import StoryDurationModal from "./StoryDurationModal";
import { storyService } from "../services/storyService";
import type { Story as StoryType } from "../services/storyService";
import { validateStoryFile, validateVideoDuration } from "./storyComposer/validators";
import StoryThumbnail from "./StoryThumbnail";

interface StoryUserPreview {
  id: string;
  username: string;
  userAvatarUrl: string;
  mediaUrl: string;
  mediaType: "IMAGE" | "VIDEO";
}

export default function StoriesBar() {
  const { user } = useAuth();
  const [stories, setStories] = useState<StoryUserPreview[]>([]);
  const [groupedStories, setGroupedStories] = useState<Record<string, StoryType[]>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [selectedStoryFile, setSelectedStoryFile] = useState<File | null>(null);
  const [isDurationModalOpen, setIsDurationModalOpen] = useState(false);
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
        id: groupedData[username][0].id,
        mediaUrl: groupedData[username][0].mediaUrl,
        mediaType: groupedData[username][0].mediaType
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

  const handleCreateStoryFromFile = async (file: File, durationHours: 6 | 12 | 24) => {
    setIsCreating(true);
    try {
      const mediaUrl = await mediaService.uploadFile(file);
      const mediaType = file.type.startsWith("video/") ? "VIDEO" : "IMAGE";
      await storyService.createStory({ mediaUrl, mediaType, durationHours });
      await fetchStories();
    } catch (err) {
      console.error("Failed to create story", err);
      alert("Failed to create story. Please try again.");
    } finally {
      setIsCreating(false);
      setSelectedStoryFile(null);
      setIsDurationModalOpen(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file == null) {
      return;
    }
    const typeValidation = validateStoryFile(file);
    if (!typeValidation.ok) {
      alert(typeValidation.reason || "Invalid media file.");
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      return;
    }

    if (file.type.startsWith("video/")) {
      void validateVideoDuration(file).then((durationValidation) => {
        if (!durationValidation.ok) {
          alert(durationValidation.reason || "Video is not supported.");
          if (fileInputRef.current) {
            fileInputRef.current.value = "";
          }
          return;
        }
        setSelectedStoryFile(file);
        setIsDurationModalOpen(true);
      });
      return;
    }

    setSelectedStoryFile(file);
    setIsDurationModalOpen(true);
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
      <div className="mb-6 flex space-x-5 overflow-x-auto px-1 py-2">
        {[...Array(8)].map((_, index) => (
          <div key={index} className="flex flex-shrink-0 flex-col items-center space-y-1">
            <div className="h-20 w-20 animate-pulse rounded-full border-2 border-white bg-gray-200 ring-2 ring-gray-100" />
            <div className="h-2 w-16 animate-pulse rounded bg-gray-200" />
          </div>
        ))}
      </div>
    );
  }

  const ownUsername = user?.username || "";
  const hasOwnStory = groupedStories[ownUsername] != null;

  return (
    <>
      <div className="custom-scrollbar mb-6 flex space-x-5 overflow-x-auto px-1 py-2">
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
              <StoryThumbnail
                mediaUrl={hasOwnStory ? groupedStories[ownUsername]?.[0]?.mediaUrl : undefined}
                mediaType={hasOwnStory ? groupedStories[ownUsername]?.[0]?.mediaType : undefined}
                fallbackAvatarUrl={user?.avatarUrl}
                fallbackLabel={user?.username}
                alt="Your story"
                className="h-20 w-20 rounded-full border-2 border-white object-cover"
              />
            </div>
            {hasOwnStory ? null : (
              <div
                className="absolute bottom-1 right-1 rounded-full border-2 border-white bg-[#0095f6] p-1.5 shadow-sm"
                onClick={(event) => {
                  event.stopPropagation();
                  fileInputRef.current?.click();
                }}
              >
                {isCreating ? (
                  <FiLoader className="animate-spin text-white" size={12} />
                ) : (
                  <FiPlus className="text-white" size={12} />
                )}
              </div>
            )}
          </div>
          <span className="w-20 truncate text-center text-sm text-gray-500">Your story</span>
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
                <StoryThumbnail
                  mediaUrl={story.mediaUrl}
                  mediaType={story.mediaType}
                  fallbackAvatarUrl={story.userAvatarUrl}
                  fallbackLabel={story.username}
                  alt={story.username}
                  className="h-20 w-20 rounded-full border-2 border-white object-cover"
                />
              </div>
              <span className="w-20 truncate text-center text-sm text-gray-700">{story.username}</span>
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

      {isDurationModalOpen && selectedStoryFile ? (
        <StoryDurationModal
          file={selectedStoryFile}
          isOpen={isDurationModalOpen}
          isSubmitting={isCreating}
          onConfirm={(hours) => void handleCreateStoryFromFile(selectedStoryFile, hours)}
          onClose={() => {
            setIsDurationModalOpen(false);
            setSelectedStoryFile(null);
            if (fileInputRef.current) {
              fileInputRef.current.value = "";
            }
          }}
        />
      ) : null}

    </>
  );
}
