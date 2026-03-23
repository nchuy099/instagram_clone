import React, { useState, useRef } from 'react';
import { X, Image as ImageIcon, ChevronLeft, ChevronRight, Loader2 } from 'lucide-react';
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
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;

    setSelectedFiles(prev => [...prev, ...files]);
    const newPreviews = files.map(file => URL.createObjectURL(file));
    setPreviews(prev => [...prev, ...newPreviews]);
    setStep('preview');
  };

  const handleCreatePost = async () => {
    if (selectedFiles.length === 0) return;
    setIsUploading(true);

    try {
      // 1. Upload all media to S3
      const mediaPromises = selectedFiles.map(async (file, index) => {
        const url = await mediaService.uploadFile(file);
        return {
          url,
          type: file.type.startsWith('video') ? MediaType.VIDEO : MediaType.IMAGE,
          orderIndex: index
        };
      });

      const uploadedMedia = await Promise.all(mediaPromises);

      // 2. Create the post in the backend
      await postService.createPost({
        caption,
        location,
        allowComments: true,
        media: uploadedMedia
      });

      // 3. Cleanup and close
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
    previews.forEach(url => URL.revokeObjectURL(url));
    setPreviews([]);
    setCaption('');
    setLocation('');
    setStep('select');
    setCurrentImageIndex(0);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={handleClose}>
      <button 
        className="absolute top-4 right-4 text-white hover:text-gray-300"
        onClick={handleClose}
      >
        <X size={32} />
      </button>

      <div 
        className="bg-white rounded-xl max-w-4xl w-full max-h-[90vh] flex flex-col overflow-hidden"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-2 border-b border-gray-200 min-h-[42px]">
          {step !== 'select' && (
            <button onClick={() => setStep(step === 'caption' ? 'preview' : 'select')} className="text-gray-900">
              <ChevronLeft size={24} />
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
              {isUploading ? <Loader2 className="animate-spin" size={20} /> : step === 'preview' ? 'Next' : 'Share'}
            </button>
          )}
        </div>

        {/* Content */}
        <div className="flex-1 flex flex-col md:flex-row min-h-0">
          {step === 'select' ? (
            <div className="flex-1 flex flex-col items-center justify-center p-12 min-h-[400px]">
              <ImageIcon size={96} strokeWidth={1} className="text-gray-400 mb-4" />
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
              {/* Media Section */}
              <div className="relative aspect-square md:w-3/5 bg-black flex items-center justify-center group">
                <img 
                  src={previews[currentImageIndex]} 
                  alt="preview" 
                  className="max-w-full max-h-full object-contain"
                />
                
                {previews.length > 1 && (
                  <>
                    <button 
                      onClick={() => setCurrentImageIndex(prev => Math.max(0, prev - 1))}
                      disabled={currentImageIndex === 0}
                      className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/50 text-white rounded-full p-1.5 hover:bg-black/80 disabled:hidden"
                    >
                      <ChevronLeft size={20} />
                    </button>
                    <button 
                      onClick={() => setCurrentImageIndex(prev => Math.min(previews.length - 1, prev + 1))}
                      disabled={currentImageIndex === previews.length - 1}
                      className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/50 text-white rounded-full p-1.5 hover:bg-black/80 disabled:hidden"
                    >
                      <ChevronRight size={20} />
                    </button>
                    
                    <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex space-x-1.5">
                      {previews.map((_, i) => (
                        <div 
                          key={i} 
                          className={`w-1.5 h-1.5 rounded-full ${i === currentImageIndex ? 'bg-white' : 'bg-white/50'}`}
                        />
                      ))}
                    </div>
                  </>
                )}
              </div>

              {/* Caption Section (only for 'caption' step) */}
              {step === 'caption' && (
                <div className="flex-1 md:w-2/5 flex flex-col h-full bg-white border-l border-gray-200">
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
                    onChange={e => setCaption(e.target.value)}
                  />
                  
                  <div className="p-4 border-t border-gray-200">
                    <input 
                      type="text" 
                      placeholder="Add location" 
                      className="w-full text-sm outline-none"
                      value={location}
                      onChange={e => setLocation(e.target.value)}
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
