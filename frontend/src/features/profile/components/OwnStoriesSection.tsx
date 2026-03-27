import { useEffect, useRef, useState } from "react";
import { Plus, Loader2 } from "lucide-react";
import { storyService } from "../../story/services/storyService";
import type { Story } from "../../story/services/storyService";
import { mediaService } from "../../post/services/mediaService";
import StoryViewer from "../../story/components/StoryViewer";
import StoryComposerModal from "../../story/components/StoryComposerModal";
import type { StoryMediaType } from "../../story/components/storyComposer/types";

interface OwnStoriesSectionProps {
  username: string;
}

export default function OwnStoriesSection({ username }: OwnStoriesSectionProps) {
  const [stories, setStories] = useState<Story[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [composerFile, setComposerFile] = useState<File | null>(null);
  const [isViewerOpen, setIsViewerOpen] = useState(false);
  const [selectedStoryIndex, setSelectedStoryIndex] = useState(0);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchOwnStories = async () => {
    try {
      const groupedData = await storyService.getGroupedStories();
      setStories(groupedData[username] || []);
    } catch (error) {
      console.error("Failed to fetch own stories", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchOwnStories();
  }, [username]);

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
      await fetchOwnStories();
    } catch (error) {
      console.error("Failed to create story", error);
      alert("Failed to create story. Please try again.");
      throw error;
    } finally {
      setIsCreating(false);
      setComposerFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
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
              {story.userAvatarUrl ? (
                <img src={story.userAvatarUrl} alt={story.username} className="h-16 w-16 rounded-full border-2 border-white object-cover" />
              ) : (
                <div className="flex h-16 w-16 items-center justify-center rounded-full border-2 border-white bg-gray-100 text-xs font-bold uppercase text-gray-500">
                  {username.substring(0, 2)}
                </div>
              )}
            </div>
            <span className="mt-1 max-w-16 truncate text-xs text-gray-700">Story {index + 1}</span>
          </button>
        ))}

        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="group flex flex-shrink-0 flex-col items-center"
        >
          <div className="flex h-16 w-16 items-center justify-center rounded-full border border-gray-400 bg-gray-100 text-gray-500 transition group-hover:bg-gray-200">
            {isCreating ? <Loader2 size={30} className="animate-spin" /> : <Plus size={30} />}
          </div>
          <span className="mt-1 text-xs text-gray-700">New</span>
        </button>
      </div>

      {isLoading && (
        <div className="mt-3 inline-flex items-center gap-2 text-xs text-gray-500">
          <Loader2 size={14} className="animate-spin" />
          Dang tai story...
        </div>
      )}

      <input
        type="file"
        ref={fileInputRef}
        className="hidden"
        accept="image/*,video/*"
        onChange={handleFileSelect}
      />

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
    </div>
  );
}
