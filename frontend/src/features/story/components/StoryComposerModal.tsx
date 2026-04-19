import { useEffect, useMemo, useRef, useState } from 'react';
import Cropper from 'react-easy-crop';
import { FiLoader, FiX } from 'react-icons/fi';
import OverlayCanvas from './storyComposer/OverlayCanvas';
import OverlayControls from './storyComposer/OverlayControls';
import TransformControls from './storyComposer/TransformControls';
import { renderImageStoryFile, renderVideoStoryFile } from './storyComposer/renderers';
import type { CropAreaPixels, MediaTransformState, OverlayLayer, StoryMediaType } from './storyComposer/types';
import { validateStoryFile, validateVideoDuration } from './storyComposer/validators';

interface StoryComposerModalProps {
  file: File;
  onClose: () => void;
  onSubmit: (file: File, mediaType: StoryMediaType) => Promise<void>;
}

const DEFAULT_TRANSFORM: MediaTransformState = {
  crop: { x: 0, y: 0 },
  zoom: 1,
  rotation: 0,
  croppedAreaPixels: null
};

function nextLayerId(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `layer-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export default function StoryComposerModal({ file, onClose, onSubmit }: StoryComposerModalProps) {
  const isVideo = file.type.startsWith('video/');
  const [previewUrl, setPreviewUrl] = useState('');
  const [transform, setTransform] = useState<MediaTransformState>(DEFAULT_TRANSFORM);
  const [layers, setLayers] = useState<OverlayLayer[]>([]);
  const [selectedLayerId, setSelectedLayerId] = useState<string | null>(null);
  const [isPublishing, setIsPublishing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const previewContainerRef = useRef<HTMLDivElement>(null);
  const [previewSize, setPreviewSize] = useState({ width: 320, height: 568 });

  useEffect(() => {
    const validation = validateStoryFile(file);
    if (!validation.ok) {
      setError(validation.reason || 'Invalid file for story.');
      return;
    }

    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);

    return () => {
      URL.revokeObjectURL(objectUrl);
    };
  }, [file]);

  useEffect(() => {
    const updateSize = () => {
      const rect = previewContainerRef.current?.getBoundingClientRect();
      if (!rect) {
        return;
      }

      setPreviewSize({
        width: rect.width,
        height: rect.height
      });
    };

    updateSize();
    window.addEventListener('resize', updateSize);
    return () => window.removeEventListener('resize', updateSize);
  }, []);

  const selectedLayer = useMemo(
    () => layers.find((layer) => layer.id === selectedLayerId),
    [layers, selectedLayerId]
  );

  const updateLayer = (id: string, patch: Partial<OverlayLayer>) => {
    setLayers((prev) => prev.map((layer) => (layer.id === id ? { ...layer, ...patch } : layer)));
  };

  const updateSelectedLayer = (patch: Partial<OverlayLayer>) => {
    if (!selectedLayerId) {
      return;
    }

    updateLayer(selectedLayerId, patch);
  };

  const addTextLayer = () => {
    const layer: OverlayLayer = {
      id: nextLayerId(),
      type: 'text',
      content: 'Type here',
      x: previewSize.width / 2,
      y: previewSize.height / 2,
      scale: 1,
      rotation: 0,
      color: '#ffffff'
    };

    setLayers((prev) => [...prev, layer]);
    setSelectedLayerId(layer.id);
  };

  const addStickerLayer = () => {
    const layer: OverlayLayer = {
      id: nextLayerId(),
      type: 'sticker',
      content: '😀',
      x: previewSize.width / 2,
      y: previewSize.height / 2,
      scale: 1,
      rotation: 0
    };

    setLayers((prev) => [...prev, layer]);
    setSelectedLayerId(layer.id);
  };

  const deleteSelectedLayer = () => {
    if (!selectedLayerId) {
      return;
    }

    setLayers((prev) => prev.filter((layer) => layer.id !== selectedLayerId));
    setSelectedLayerId(null);
  };

  const resetTransform = () => {
    setTransform(DEFAULT_TRANSFORM);
  };

  const publishStory = async () => {
    setError(null);

    if (!previewUrl) {
      setError('Preview is not ready yet.');
      return;
    }

    const typeValidation = validateStoryFile(file);
    if (!typeValidation.ok) {
      setError(typeValidation.reason || 'Invalid media file.');
      return;
    }

    if (isVideo) {
      const durationValidation = await validateVideoDuration(file);
      if (!durationValidation.ok) {
        setError(durationValidation.reason || 'Video is not supported.');
        return;
      }
    }

    setIsPublishing(true);

    try {
      const renderedFile = isVideo
        ? await renderVideoStoryFile({
            sourceUrl: previewUrl,
            sourceFile: file,
            transform,
            layers,
            previewSize
          })
        : await renderImageStoryFile({
            sourceUrl: previewUrl,
            sourceFile: file,
            transform,
            layers,
            previewSize
          });

      await onSubmit(renderedFile, isVideo ? 'VIDEO' : 'IMAGE');
      onClose();
    } catch (publishError) {
      console.error('Failed to publish story', publishError);
      setError(
        publishError instanceof Error
          ? publishError.message
          : 'Failed to render and upload story. Please try again.'
      );
    } finally {
      setIsPublishing(false);
    }
  };

  const onCropComplete = (_: CropAreaPixels, croppedAreaPixels: CropAreaPixels) => {
    setTransform((prev) => ({ ...prev, croppedAreaPixels }));
  };

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/80 p-4">
      <div className="relative w-full max-w-5xl rounded-xl bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h3 className="text-base font-semibold text-gray-900">Compose story</h3>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-gray-500 transition hover:bg-gray-100 hover:text-gray-800"
          >
            <FiX size={20} />
          </button>
        </div>

        <div className="grid gap-4 p-4 md:grid-cols-[1fr_320px]">
          <div className="flex min-h-[520px] items-center justify-center rounded-lg bg-gray-900 p-4">
            <div
              ref={previewContainerRef}
              className="relative w-full max-w-[360px] overflow-hidden rounded-lg bg-black"
              style={{ aspectRatio: '9 / 16' }}
            >
              {previewUrl && (
                <>
                  <Cropper
                    image={isVideo ? undefined : previewUrl}
                    video={isVideo ? previewUrl : undefined}
                    crop={transform.crop}
                    zoom={transform.zoom}
                    rotation={transform.rotation}
                    aspect={9 / 16}
                    onCropChange={(crop) => setTransform((prev) => ({ ...prev, crop }))}
                    onZoomChange={(zoom) => setTransform((prev) => ({ ...prev, zoom }))}
                    onRotationChange={(rotation) => setTransform((prev) => ({ ...prev, rotation }))}
                    onCropComplete={onCropComplete}
                    objectFit="cover"
                  />
                  <OverlayCanvas
                    layers={layers}
                    selectedLayerId={selectedLayerId}
                    width={previewSize.width}
                    height={previewSize.height}
                    onSelectLayer={setSelectedLayerId}
                    onUpdateLayer={updateLayer}
                  />
                </>
              )}
            </div>
          </div>

          <div className="space-y-3">
            <TransformControls
              zoom={transform.zoom}
              rotation={transform.rotation}
              onZoomChange={(zoom) => setTransform((prev) => ({ ...prev, zoom }))}
              onRotationChange={(rotation) => setTransform((prev) => ({ ...prev, rotation }))}
              onReset={resetTransform}
            />

            <OverlayControls
              selectedLayer={selectedLayer}
              onAddText={addTextLayer}
              onAddSticker={addStickerLayer}
              onDeleteSelected={deleteSelectedLayer}
              onUpdateSelected={updateSelectedLayer}
            />

            {error && (
              <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
                {error}
              </div>
            )}

            <button
              type="button"
              onClick={publishStory}
              disabled={isPublishing || Boolean(error)}
              className="flex w-full items-center justify-center gap-2 rounded-md bg-[#0095f6] px-4 py-2 text-sm font-semibold text-white transition hover:bg-[#1877f2] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isPublishing ? <FiLoader size={16} className="animate-spin" /> : null}
              {isPublishing ? 'Rendering and uploading...' : 'Share Story'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
