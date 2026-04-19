import { useCallback, useEffect, useRef, useState } from "react";
import { FiPlus, FiLoader } from "react-icons/fi";
import { storyService } from "../../story/services/storyService";
import type { Story } from "../../story/services/storyService";
import { mediaService } from "../../post/services/mediaService";
import StoryViewer from "../../story/components/StoryViewer";
import StoryDurationModal from "../../story/components/StoryDurationModal";
import { validateStoryFile, validateVideoDuration } from "../../story/components/storyComposer/validators";
import StoryThumbnail from "../../story/components/StoryThumbnail";

interface OwnStoriesSectionProps {
  username: string;
  canCreate?: boolean;
}

export default function OwnStoriesSection({ username, canCreate = true }: OwnStoriesSectionProps) {
  const [stories, setStories] = useState<Story[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [selectedStoryFile, setSelectedStoryFile] = useState<File | null>(null);
  const [isDurationModalOpen, setIsDurationModalOpen] = useState(false);
  const [isViewerOpen, setIsViewerOpen] = useState(false);
  const [selectedStoryIndex, setSelectedStoryIndex] = useState(0);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchOwnStories = useCallback(async () => {
    try {
      const userStories = await storyService.getUserStories(username);
      setStories(userStories || []);
    } catch (error) {
      console.error("Failed to fetch own stories", error);
    } finally {
      setIsLoading(false);
    }
  }, [username]);

  useEffect(() => {
    fetchOwnStories();
  }, [fetchOwnStories]);

  const applyStoryUpdate = (updatedStory: Story) => {
    setStories((prev) => prev.map((story) => (story.id === updatedStory.id ? updatedStory : story)));
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
      await fetchOwnStories();
    } catch (error) {
      console.error("Failed to create story", error);
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

  return (
    <div className="mb-8 pb-2">
      <div className="custom-scrollbar flex items-start gap-5 overflow-x-auto">
        {stories.map((story, index) => (
          <button
            key={story.id}
            type="button"
            onClick={() => {
              setSelectedStoryIndex(index);
              setIsViewerOpen(true);
            }}
            className="group flex flex-shrink-0 flex-col items-center"
          >
            <div className="rounded-full bg-gradient-to-tr from-yellow-400 to-fuchsia-600 p-[2px]">
              <StoryThumbnail
                mediaUrl={story.mediaUrl}
                mediaType={story.mediaType}
                fallbackAvatarUrl={story.userAvatarUrl}
                fallbackLabel={story.username}
                alt={story.username}
                className="h-16 w-16 rounded-full border-2 border-white object-cover"
              />
            </div>
            <span className="mt-1 max-w-16 truncate text-xs text-gray-700">Story {index + 1}</span>
          </button>
        ))}

        {canCreate ? (
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="group flex flex-shrink-0 flex-col items-center"
          >
            <div className="flex h-16 w-16 items-center justify-center rounded-full border border-gray-400 bg-gray-100 text-gray-500 transition group-hover:bg-gray-200">
              {isCreating ? <FiLoader size={24} className="animate-spin" /> : <FiPlus size={24} />}
            </div>
            <span className="mt-1 text-xs text-gray-700">New</span>
          </button>
        ) : null}
      </div>

      {isLoading && (
        <div className="mt-3 inline-flex items-center gap-2 text-xs text-gray-500">
          <FiLoader size={14} className="animate-spin" />
          Loading stories...
        </div>
      )}

      {canCreate ? (
        <input
          type="file"
          ref={fileInputRef}
          className="hidden"
          accept="image/*,video/*"
          onChange={handleFileSelect}
        />
      ) : null}

      {isViewerOpen && stories.length > 0 && (
        <StoryViewer
          stories={stories}
          initialStoryIndex={selectedStoryIndex}
          onClose={() => setIsViewerOpen(false)}
          onLike={handleLikeStory}
          onReply={handleReplyStory}
          onShare={handleShareStory}
        />
      )}

      {canCreate && isDurationModalOpen && selectedStoryFile ? (
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
    </div>
  );
}
