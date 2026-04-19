import { useState, useEffect, useCallback, useRef } from "react";
import {
  FiChevronLeft,
  FiChevronRight,
  FiHeart,
  FiSend,
  FiX,
} from "react-icons/fi";
import type { Story } from "../services/storyService";
import { useAuth } from "../../../hooks/useAuth";

interface StoryViewerProps {
  stories: Story[];
  initialStoryIndex?: number;
  onClose: () => void;
  onNextUser?: () => void;
  onPrevUser?: () => void;
  onLike?: (storyId: string, currentlyLiked: boolean) => Promise<Story | undefined>;
  onReply?: (storyId: string, content: string) => Promise<Story | undefined>;
  onShare?: (storyId: string) => Promise<Story | undefined>;
}

export default function StoryViewer({
  stories,
  initialStoryIndex = 0,
  onClose,
  onNextUser,
  onPrevUser,
  onLike,
  onReply,
  onShare
}: StoryViewerProps) {
  const { user } = useAuth();
  const [currentIndex, setCurrentIndex] = useState(initialStoryIndex);
  const [progress, setProgress] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [replyText, setReplyText] = useState("");
  const [isReplyFocused, setIsReplyFocused] = useState(false);
  const [isSendingReply, setIsSendingReply] = useState(false);
  const [isSubmittingAction, setIsSubmittingAction] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);

  const STORY_DURATION = 5000;

  const handleNext = useCallback(() => {
    if (currentIndex < stories.length - 1) {
      setCurrentIndex((prev) => prev + 1);
      setProgress(0);
      setReplyText("");
      setIsReplyFocused(false);
    } else if (onNextUser) {
      onNextUser();
    } else {
      onClose();
    }
  }, [currentIndex, stories.length, onNextUser, onClose]);

  const handlePrev = useCallback(() => {
    if (currentIndex > 0) {
      setCurrentIndex((prev) => prev - 1);
      setProgress(0);
      setReplyText("");
      setIsReplyFocused(false);
    } else if (onPrevUser) {
      onPrevUser();
    }
  }, [currentIndex, onPrevUser]);

  const currentStory = stories[currentIndex];

  useEffect(() => {
    if (!currentStory) {
      return;
    }
    setProgress(0);
  }, [currentStory]);

  useEffect(() => {
    if (!currentStory) {
      return;
    }

    if (currentStory.mediaType === "VIDEO") {
      const video = videoRef.current;
      if (video == null) {
        return;
      }

      const syncProgress = () => {
        if (video.duration > 0) {
          setProgress((video.currentTime / video.duration) * 100);
        }
      };

      const handleEnded = () => {
        setProgress(100);
        handleNext();
      };

      video.addEventListener("timeupdate", syncProgress);
      video.addEventListener("loadedmetadata", syncProgress);
      video.addEventListener("ended", handleEnded);

      if (isPaused) {
        video.pause();
      } else {
        void video.play().catch(() => {
          // Browser autoplay policy can block unmuted playback without user gesture.
        });
      }

      return () => {
        video.removeEventListener("timeupdate", syncProgress);
        video.removeEventListener("loadedmetadata", syncProgress);
        video.removeEventListener("ended", handleEnded);
      };
    }

    if (isPaused) {
      return;
    }

    const interval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) {
          handleNext();
          return 0;
        }
        return prev + 100 / (STORY_DURATION / 100);
      });
    }, 100);

    return () => clearInterval(interval);
  }, [currentStory, handleNext, isPaused]);

  if (currentStory == null) {
    return null;
  }

  const isReplyMode = isReplyFocused || replyText.trim().length > 0;
  const isOwnStory =
    user != null &&
    (currentStory.userId === user.id || currentStory.username === user.username);

  const handleLikeClick = async (event: React.MouseEvent) => {
    event.stopPropagation();
    if (onLike == null || isSubmittingAction) {
      return;
    }

    setIsSubmittingAction(true);
    try {
      await onLike(currentStory.id, Boolean(currentStory.likedByCurrentUser));
    } catch (error) {
      console.error("Failed to like story", error);
    } finally {
      setIsSubmittingAction(false);
    }
  };

  const handleShareClick = async (event: React.MouseEvent) => {
    event.stopPropagation();
    if (onShare == null || isSubmittingAction) {
      return;
    }

    setIsSubmittingAction(true);
    try {
      await onShare(currentStory.id);
    } catch (error) {
      console.error("Failed to share story", error);
    } finally {
      setIsSubmittingAction(false);
    }
  };

  const handleReplySubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    event.stopPropagation();
    if (onReply == null || isSendingReply) {
      return;
    }

    const trimmedReply = replyText.trim();
    if (trimmedReply.length === 0) {
      return;
    }

    setIsSendingReply(true);
    try {
      await onReply(currentStory.id, trimmedReply);
      setReplyText("");
      setIsReplyFocused(false);
    } catch (error) {
      console.error("Failed to reply story", error);
    } finally {
      setIsSendingReply(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[100] flex select-none items-center justify-center bg-black"
      onClick={() => setIsPaused((prev) => (prev ? false : true))}
    >
      <div
        className="pointer-events-none absolute inset-0 opacity-50 blur-2xl"
        style={{
          backgroundImage: "url(" + currentStory.mediaUrl + ")",
          backgroundSize: "cover",
          backgroundPosition: "center"
        }}
      />

      <div className="relative aspect-[9/16] w-full max-w-[420px] overflow-hidden rounded-xl bg-black shadow-2xl">
        <div className="absolute left-0 right-0 top-0 z-20 flex space-x-1 p-2">
          {stories.map((_, index) => (
            <div key={index} className="h-1 flex-1 overflow-hidden rounded-full bg-white/30">
              <div
                className="h-full bg-white transition-all duration-100 ease-linear"
                style={{ width: index === currentIndex ? String(progress) + "%" : index < currentIndex ? "100%" : "0%" }}
              />
            </div>
          ))}
        </div>

        <div className="absolute left-0 right-0 top-4 z-20 flex items-center justify-between px-4">
          <div className="flex items-center space-x-3">
            <div className="h-8 w-8 overflow-hidden rounded-full border border-white/20">
              <img src={currentStory.userAvatarUrl} alt={currentStory.username} className="h-full w-full object-cover" />
            </div>
            <span className="text-sm font-semibold text-white drop-shadow-md">{currentStory.username}</span>
          </div>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onClose();
            }}
            className="p-1 text-white drop-shadow-md hover:text-gray-300"
          >
            <FiX size={24} />
          </button>
        </div>

        <div className="flex h-full w-full items-center justify-center">
          {currentStory.mediaType === "VIDEO" ? (
            <video
              ref={videoRef}
              src={currentStory.mediaUrl}
              autoPlay
              muted={false}
              className="max-h-full max-w-full object-contain"
              playsInline
            />
          ) : (
            <img src={currentStory.mediaUrl} alt="Story" className="max-h-full max-w-full object-contain" />
          )}
        </div>

        <div className="absolute inset-0 z-10 flex">
          <div
            className="h-full w-1/3 cursor-pointer"
            onClick={(e) => {
              e.stopPropagation();
              handlePrev();
            }}
          />
          <div className="h-full w-1/3" />
          <div
            className="h-full w-1/3 cursor-pointer"
            onClick={(e) => {
              e.stopPropagation();
              handleNext();
            }}
          />
        </div>

        {!isOwnStory && (
          <div
            className="absolute bottom-0 left-0 right-0 z-30 border-t border-white/15 bg-gradient-to-t from-black/80 to-black/30 p-3"
            onClick={(e) => e.stopPropagation()}
          >
            <form onSubmit={handleReplySubmit} className="flex items-center gap-2">
              <input
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                onFocus={() => {
                  setIsPaused(true);
                  setIsReplyFocused(true);
                }}
                onBlur={() => {
                  setIsPaused(false);
                  setIsReplyFocused(false);
                }}
              placeholder={`Reply to @${currentStory.username}...`}
                className="w-full rounded-full border border-white/20 bg-black/40 px-3 py-2 text-sm text-white placeholder:text-white/60 focus:border-white/40 focus:outline-none"
                maxLength={500}
              />

              {isReplyMode ? (
                <button
                  type="submit"
                  className="rounded-full bg-[#0095f6] p-2 text-white disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={isSendingReply || replyText.trim().length === 0}
                  aria-label="Send reply"
                >
                  <FiSend size={18} />
                </button>
              ) : (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={handleLikeClick}
                    className="rounded-full bg-white/10 p-2 text-white transition hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={isSubmittingAction}
                    aria-label="Like story"
                  >
                    <FiHeart
                      size={18}
                      className={currentStory.likedByCurrentUser ? "text-red-500" : "text-white"}
                    />
                  </button>

                  <button
                    type="button"
                    onClick={handleShareClick}
                    className="rounded-full bg-white/10 p-2 text-white transition hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={isSubmittingAction}
                    aria-label="Share story"
                  >
                    <FiSend size={18} />
                  </button>
                </div>
              )}
            </form>
          </div>
        )}

        {isPaused && (
          <div className="pointer-events-none absolute inset-0 z-20 flex items-center justify-center">
            <div className="rounded-full bg-black/20 p-4 backdrop-blur-sm">
              <span className="text-sm font-semibold text-white opacity-80">Paused</span>
            </div>
          </div>
        )}
      </div>

      <div className="pointer-events-none absolute inset-y-0 left-0 right-0 hidden items-center justify-between px-8 md:flex">
        <button
          onClick={(e) => {
            e.stopPropagation();
            handlePrev();
          }}
          className="pointer-events-auto rounded-full bg-white/10 p-3 text-white transition hover:bg-white/20"
        >
          <FiChevronLeft size={32} />
        </button>
        <button
          onClick={(e) => {
            e.stopPropagation();
            handleNext();
          }}
          className="pointer-events-auto rounded-full bg-white/10 p-3 text-white transition hover:bg-white/20"
        >
          <FiChevronRight size={32} />
        </button>
      </div>
    </div>
  );
}
