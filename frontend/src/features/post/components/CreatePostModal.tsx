import React, { useRef, useState } from 'react';
import {
  FiChevronLeft,
  FiChevronRight,
  FiImage,
  FiLoader,
  FiX,
} from 'react-icons/fi';
import { mediaService } from '../services/mediaService';
import { postService } from '../services/postService';
import { MediaType } from '../types';
import { useAuth } from '../../../hooks/useAuth';

interface CreatePostModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function CreatePostModal({ isOpen, onClose, onSuccess }: CreatePostModalProps) {
  const { user } = useAuth();
  const [step, setStep] = useState<'select' | 'preview' | 'caption'>('select');
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const [caption, setCaption] = useState('');
  const [location, setLocation] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [videoThumbnailFiles, setVideoThumbnailFiles] = useState<Record<number, File>>({});
  const [videoThumbnailPreviews, setVideoThumbnailPreviews] = useState<Record<number, string>>({});
  const [thumbnailError, setThumbnailError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const thumbnailFileInputRef = useRef<HTMLInputElement>(null);
  const previewVideoRef = useRef<HTMLVideoElement>(null);

  if (!isOpen) return null;

  const currentFile = selectedFiles[currentImageIndex];
  const isCurrentVideo = Boolean(currentFile?.type.startsWith('video'));
  const currentThumbnailPreview = videoThumbnailPreviews[currentImageIndex];

  const updateVideoThumbnail = (index: number, file: File) => {
    const previewUrl = URL.createObjectURL(file);

    setVideoThumbnailFiles((prev) => ({ ...prev, [index]: file }));
    setVideoThumbnailPreviews((prev) => {
      if (prev[index]) {
        URL.revokeObjectURL(prev[index]);
      }
      return { ...prev, [index]: previewUrl };
    });
    setThumbnailError(null);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;

    setSelectedFiles((prev) => [...prev, ...files]);
    const newPreviews = files.map((file) => URL.createObjectURL(file));
    setPreviews((prev) => [...prev, ...newPreviews]);
    setStep('preview');
    e.target.value = '';
  };

  const handleSelectThumbnailFromComputer = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setThumbnailError('Please choose an image file for thumbnail.');
      e.target.value = '';
      return;
    }

    updateVideoThumbnail(currentImageIndex, file);
    e.target.value = '';
  };

  const handleCaptureThumbnailFromVideo = () => {
    const video = previewVideoRef.current;
    if (!video) {
      setThumbnailError('Video preview is not ready.');
      return;
    }

    if (video.videoWidth === 0 || video.videoHeight === 0) {
      setThumbnailError('Please wait for the video to load before capturing thumbnail.');
      return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      setThumbnailError('Cannot capture thumbnail on this browser.');
      return;
    }

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    canvas.toBlob(
      (blob) => {
        if (!blob) {
          setThumbnailError('Failed to create thumbnail image.');
          return;
        }

        const thumbnailFile = new File([blob], `video-thumbnail-${Date.now()}.jpg`, {
          type: 'image/jpeg',
        });

        updateVideoThumbnail(currentImageIndex, thumbnailFile);
      },
      'image/jpeg',
      0.9,
    );
  };

  const handleCreatePost = async () => {
    if (selectedFiles.length === 0) return;
    setIsUploading(true);

    try {
      const mediaPromises = selectedFiles.map(async (file, index) => {
        const url = await mediaService.uploadFile(file);
        const isVideo = file.type.startsWith('video');
        const thumbnailFile = isVideo ? videoThumbnailFiles[index] : undefined;
        const thumbnailUrl = thumbnailFile ? await mediaService.uploadFile(thumbnailFile) : undefined;

        return {
          url,
          thumbnailUrl,
          type: isVideo ? MediaType.VIDEO : MediaType.IMAGE,
          orderIndex: index,
        };
      });

      const uploadedMedia = await Promise.all(mediaPromises);

      await postService.createPost({
        caption,
        location,
        allowComments: true,
        media: uploadedMedia,
      });

      onSuccess?.();
      handleClose();
    } catch (error) {
      console.error('Failed to create post:', error);
      alert('Failed to create post. Please try again.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleClose = () => {
    setSelectedFiles([]);
    previews.forEach((url) => URL.revokeObjectURL(url));
    Object.values(videoThumbnailPreviews).forEach((url) => URL.revokeObjectURL(url));

    setPreviews([]);
    setVideoThumbnailFiles({});
    setVideoThumbnailPreviews({});
    setThumbnailError(null);
    setCaption('');
    setLocation('');
    setStep('select');
    setCurrentImageIndex(0);
    onClose();
  };

  const renderThumbnailPicker = () => {
    if (!isCurrentVideo) {
      return (
        <p className="text-sm text-gray-500">
          Media hiện tại là ảnh. Chuyển sang video để chọn thumbnail.
        </p>
      );
    }

    return (
      <div className="space-y-3">
        <div>
          <p className="text-sm font-semibold text-gray-900">Video thumbnail</p>
          <p className="text-xs text-gray-500">Chọn từ máy hoặc chụp frame hiện tại của video.</p>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => thumbnailFileInputRef.current?.click()}
            className="text-xs bg-gray-100 hover:bg-gray-200 text-gray-900 px-3 py-1.5 rounded-md transition"
          >
            Upload from computer
          </button>
          <button
            type="button"
            onClick={handleCaptureThumbnailFromVideo}
            className="text-xs bg-gray-100 hover:bg-gray-200 text-gray-900 px-3 py-1.5 rounded-md transition"
          >
            Use current video frame
          </button>
        </div>

        {currentThumbnailPreview && (
          <div className="flex items-center gap-2">
            <img src={currentThumbnailPreview} alt="thumbnail preview" className="w-14 h-14 rounded object-cover border border-gray-200" />
            <span className="text-xs text-gray-600">Đã chọn thumbnail</span>
          </div>
        )}

        {thumbnailError && <p className="text-xs text-red-500">{thumbnailError}</p>}

        <input
          type="file"
          ref={thumbnailFileInputRef}
          accept="image/*"
          className="hidden"
          onChange={handleSelectThumbnailFromComputer}
        />
      </div>
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={handleClose}>
      <button className="absolute top-4 right-4 text-white hover:text-gray-300" onClick={handleClose}>
        <FiX size={32} />
      </button>

      <div className="bg-white rounded-xl max-w-4xl w-full max-h-[90vh] flex flex-col overflow-hidden" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between px-4 py-2 border-b border-gray-200 min-h-[42px]">
          {step !== 'select' && (
            <button onClick={() => setStep(step === 'caption' ? 'preview' : 'select')} className="text-gray-900">
              <FiChevronLeft size={24} />
            </button>
          )}
          <h2 className="flex-1 text-center font-semibold text-gray-900">
            {step === 'select' ? 'Create new post' : step === 'preview' ? 'Preview' : 'Create new post'}
          </h2>
          {step !== 'select' && (
            <button
              onClick={step === 'preview' ? () => setStep('caption') : handleCreatePost}
              disabled={isUploading}
              className="text-[#0095f6] font-semibold hover:text-[#00376b] disabled:opacity-50"
            >
              {isUploading ? <FiLoader className="animate-spin" size={18} /> : step === 'preview' ? 'Next' : 'Share'}
            </button>
          )}
        </div>

        <div className="flex-1 flex flex-col md:flex-row min-h-0">
          {step === 'select' ? (
            <div className="flex-1 flex flex-col items-center justify-center p-12 min-h-[400px]">
              <FiImage size={88} className="text-gray-400 mb-4" />
              <p className="text-xl text-gray-900 mb-6">Drag photos and videos here</p>
              <button
                onClick={() => fileInputRef.current?.click()}
                className="bg-[#0095f6] hover:bg-[#1877f2] text-white font-semibold px-4 py-1.5 rounded-lg transition"
              >
                Select from computer
              </button>
              <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                multiple
                accept="image/*,video/*"
                onChange={handleFileSelect}
              />
            </div>
          ) : (
            <>
              <div className="md:w-3/5 bg-black flex items-center justify-center relative aspect-square">
                {isCurrentVideo ? (
                  <video ref={previewVideoRef} src={previews[currentImageIndex]} controls className="max-w-full max-h-full object-contain" />
                ) : (
                  <img src={previews[currentImageIndex]} alt="preview" className="max-w-full max-h-full object-contain" />
                )}

                {previews.length > 1 && (
                  <>
                    <button
                      onClick={() => setCurrentImageIndex((prev) => Math.max(0, prev - 1))}
                      disabled={currentImageIndex === 0}
                      className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/50 text-white rounded-full p-1.5 hover:bg-black/80 disabled:hidden"
                    >
                      <FiChevronLeft size={20} />
                    </button>
                    <button
                      onClick={() => setCurrentImageIndex((prev) => Math.min(previews.length - 1, prev + 1))}
                      disabled={currentImageIndex === previews.length - 1}
                      className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/50 text-white rounded-full p-1.5 hover:bg-black/80 disabled:hidden"
                    >
                      <FiChevronRight size={20} />
                    </button>

                    <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex space-x-1.5">
                      {previews.map((_, i) => (
                        <div key={i} className={`w-1.5 h-1.5 rounded-full ${i === currentImageIndex ? 'bg-white' : 'bg-white/50'}`} />
                      ))}
                    </div>
                  </>
                )}
              </div>

              {step === 'preview' ? (
                <div className="flex-1 md:w-2/5 p-4 border-l border-gray-200 bg-white">
                  {renderThumbnailPicker()}
                </div>
              ) : (
                <div className="flex-1 md:w-2/5 flex flex-col h-full bg-white border-l border-gray-200">
                  <div className="p-4 border-b border-gray-100">{renderThumbnailPicker()}</div>

                  <div className="p-4 flex items-center space-x-3">
                    <div className="w-8 h-8 rounded-full bg-gray-200 overflow-hidden">
                      {user?.avatarUrl && <img src={user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />}
                    </div>
                    <span className="font-semibold text-sm">{user?.username}</span>
                  </div>

                  <textarea
                    placeholder="Write a caption..."
                    className="flex-1 p-4 text-gray-900 resize-none outline-none text-sm placeholder:text-gray-400"
                    value={caption}
                    onChange={(e) => setCaption(e.target.value)}
                  />

                  <div className="p-4 border-t border-gray-200">
                    <input
                      type="text"
                      placeholder="Add location"
                      className="w-full text-sm outline-none"
                      value={location}
                      onChange={(e) => setLocation(e.target.value)}
                    />
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
