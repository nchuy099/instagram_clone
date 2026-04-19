import { useState } from 'react';
import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { MediaType } from '../types';
import type { PostMedia } from '../types';

interface MediaCarouselProps {
  media: PostMedia[];
}

export default function MediaCarousel({ media }: MediaCarouselProps) {
  const [currentIndex, setCurrentIndex] = useState(0);

  if (!media || media.length === 0) return null;

  const currentMedia = media[currentIndex];

  const handlePrev = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentIndex(prev => Math.max(0, prev - 1));
  };

  const handleNext = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentIndex(prev => Math.min(media.length - 1, prev + 1));
  };

  return (
    <div className="relative aspect-square w-full bg-black flex items-center justify-center overflow-hidden">
      {currentMedia.type === MediaType.VIDEO ? (
        <video
          src={currentMedia.url}
          poster={currentMedia.thumbnailUrl || undefined}
          autoPlay
          loop
          muted={false}
          playsInline
          className="max-w-full max-h-full object-contain"
        />
      ) : (
        <img
          src={currentMedia.url}
          alt="post media"
          className="max-w-full max-h-full object-contain select-none"
        />
      )}

      {media.length > 1 && (
        <>
          <button
            onClick={handlePrev}
            disabled={currentIndex === 0}
            className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/70 hover:bg-white text-gray-900 rounded-full p-1 shadow-md transition disabled:hidden"
          >
            <FiChevronLeft size={20} />
          </button>
          <button
            onClick={handleNext}
            disabled={currentIndex === media.length - 1}
            className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/70 hover:bg-white text-gray-900 rounded-full p-1 shadow-md transition disabled:hidden"
          >
            <FiChevronRight size={20} />
          </button>

          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex space-x-1.5 px-2 py-1 ">
            {media.map((_, i) => (
              <div
                key={i}
                className={`w-1.5 h-1.5 rounded-full shadow-sm transition-all ${i === currentIndex ? 'bg-white' : 'bg-white/50 scale-75'
                  }`}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
