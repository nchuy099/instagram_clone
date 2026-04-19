import { FiUser } from 'react-icons/fi';

interface StoryThumbnailProps {
  mediaUrl?: string;
  mediaType?: 'IMAGE' | 'VIDEO' | string;
  fallbackAvatarUrl?: string;
  fallbackLabel?: string;
  alt: string;
  className: string;
}

export default function StoryThumbnail({
  mediaUrl,
  mediaType,
  fallbackAvatarUrl,
  fallbackLabel,
  alt,
  className,
}: StoryThumbnailProps) {
  if (mediaUrl) {
    if (mediaType === 'VIDEO') {
      return (
        <video
          src={mediaUrl}
          muted
          playsInline
          preload="metadata"
          className={className}
          onLoadedMetadata={(event) => {
            const video = event.currentTarget;
            if (video.readyState >= 1) {
              try {
                video.currentTime = 0.01;
              } catch {
                // keep default frame when seeking is blocked.
              }
            }
          }}
          onSeeked={(event) => {
            event.currentTarget.pause();
          }}
        />
      );
    }

    return <img src={mediaUrl} alt={alt} className={className} />;
  }

  if (fallbackAvatarUrl) {
    return <img src={fallbackAvatarUrl} alt={alt} className={className} />;
  }

  return (
    <div className={`flex items-center justify-center bg-gray-100 text-gray-500 ${className}`}>
      {fallbackLabel ? (
        <span className="text-sm font-bold uppercase">{fallbackLabel.substring(0, 2)}</span>
      ) : (
        <FiUser size={18} />
      )}
    </div>
  );
}
