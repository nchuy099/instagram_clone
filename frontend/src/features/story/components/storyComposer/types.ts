export type StoryMediaType = 'IMAGE' | 'VIDEO';

export type OverlayType = 'text' | 'sticker';

export interface OverlayLayer {
  id: string;
  type: OverlayType;
  content: string;
  x: number;
  y: number;
  scale: number;
  rotation: number;
  color?: string;
}

export interface CropAreaPixels {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface MediaTransformState {
  crop: { x: number; y: number };
  zoom: number;
  rotation: number;
  croppedAreaPixels: CropAreaPixels | null;
}

export interface RenderStoryOptions {
  sourceUrl: string;
  transform: MediaTransformState;
  layers: OverlayLayer[];
  previewSize: {
    width: number;
    height: number;
  };
  outputSize?: {
    width: number;
    height: number;
  };
}
