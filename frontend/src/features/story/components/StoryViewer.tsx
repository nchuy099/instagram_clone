import { useState, useEffect, useCallback } from 'react';
import { X, ChevronLeft, ChevronRight } from 'lucide-react';
import type { Story } from '../services/storyService';

interface StoryViewerProps {
  stories: Story[];
  initialStoryIndex?: number;
  onClose: () => void;
  onNextUser?: () => void;
  onPrevUser?: () => void;
}

export default function StoryViewer({ stories, initialStoryIndex = 0, onClose, onNextUser, onPrevUser }: StoryViewerProps) {
  const [currentIndex, setCurrentIndex] = useState(initialStoryIndex);
  const [progress, setProgress] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  
  const STORY_DURATION = 5000; // 5 seconds per story

  const handleNext = useCallback(() => {
    if (currentIndex < stories.length - 1) {
      setCurrentIndex(prev => prev + 1);
      setProgress(0);
    } else if (onNextUser) {
      onNextUser();
    } else {
      onClose();
    }
  }, [currentIndex, stories.length, onNextUser, onClose]);

  const handlePrev = useCallback(() => {
    if (currentIndex > 0) {
      setCurrentIndex(prev => prev - 1);
      setProgress(0);
    } else if (onPrevUser) {
      onPrevUser();
    }
  }, [currentIndex, onPrevUser]);

  useEffect(() => {
    if (isPaused) return;

    const interval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 100) {
          handleNext();
          return 0;
        }
        return prev + (100 / (STORY_DURATION / 100));
      });
    }, 100);

    return () => clearInterval(interval);
  }, [isPaused, handleNext]);

  const currentStory = stories[currentIndex];

  if (!currentStory) return null;

  return (
    <div 
      className="fixed inset-0 z-[100] bg-black flex items-center justify-center select-none"
      onClick={() => setIsPaused(prev => !prev)}
    >
      {/* Background with blur */}
      <div 
        className="absolute inset-0 opacity-50 blur-2xl pointer-events-none"
        style={{ backgroundImage: `url(${currentStory.mediaUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' }}
      />

      <div className="relative w-full max-w-[420px] aspect-[9/16] bg-black shadow-2xl overflow-hidden rounded-xl">
        {/* Progress Bars */}
        <div className="absolute top-0 left-0 right-0 z-20 p-2 flex space-x-1">
          {stories.map((_, index) => (
            <div key={index} className="h-1 flex-1 bg-white/30 rounded-full overflow-hidden">
              <div 
                className="h-full bg-white transition-all duration-100 ease-linear"
                style={{ 
                  width: index === currentIndex ? `${progress}%` : index < currentIndex ? '100%' : '0%' 
                }}
              />
            </div>
          ))}
        </div>

        {/* Header */}
        <div className="absolute top-4 left-0 right-0 z-20 px-4 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 rounded-full border border-white/20 overflow-hidden">
              <img src={currentStory.userAvatarUrl} alt={currentStory.username} className="w-full h-full object-cover" />
            </div>
            <span className="text-white font-semibold text-sm drop-shadow-md">{currentStory.username}</span>
            <span className="text-white/60 text-xs drop-shadow-md">2h</span> {/* Mock time */}
          </div>
          <button 
            onClick={(e) => { e.stopPropagation(); onClose(); }} 
            className="text-white hover:text-gray-300 drop-shadow-md p-1"
          >
            <X size={24} />
          </button>
        </div>

        {/* Media */}
        <div className="w-full h-full flex items-center justify-center">
          {currentStory.mediaType === 'VIDEO' ? (
            <video 
              src={currentStory.mediaUrl} 
              autoPlay 
              muted 
              className="max-w-full max-h-full object-contain"
              onEnded={handleNext}
            />
          ) : (
            <img 
              src={currentStory.mediaUrl} 
              alt="Story" 
              className="max-w-full max-h-full object-contain"
            />
          )}
        </div>

        {/* Navigation Overlays */}
        <div className="absolute inset-0 z-10 flex">
          <div 
            className="w-1/3 h-full cursor-pointer"
            onClick={(e) => { e.stopPropagation(); handlePrev(); }}
          />
          <div className="w-1/3 h-full" /> {/* Center area for pausing */}
          <div 
            className="w-1/3 h-full cursor-pointer"
            onClick={(e) => { e.stopPropagation(); handleNext(); }}
          />
        </div>

        {/* Loading state if needed */}
        {isPaused && (
          <div className="absolute inset-0 flex items-center justify-center z-30 pointer-events-none">
            <div className="bg-black/20 p-4 rounded-full backdrop-blur-sm">
              <span className="text-white text-sm font-semibold opacity-80">Paused</span>
            </div>
          </div>
        )}
      </div>

      {/* External Navigation Arrows for large screens */}
      <div className="hidden md:flex absolute inset-y-0 left-0 right-0 items-center justify-between px-8 pointer-events-none">
        <button 
          onClick={(e) => { e.stopPropagation(); handlePrev(); }}
          className="p-3 bg-white/10 hover:bg-white/20 text-white rounded-full transition pointer-events-auto"
        >
          <ChevronLeft size={32} />
        </button>
        <button 
          onClick={(e) => { e.stopPropagation(); handleNext(); }}
          className="p-3 bg-white/10 hover:bg-white/20 text-white rounded-full transition pointer-events-auto"
        >
          <ChevronRight size={32} />
        </button>
      </div>
    </div>
  );
}
