import type { OverlayLayer, RenderStoryOptions } from './types';

const DEFAULT_OUTPUT = {
  width: 1080,
  height: 1920
};

function createCanvas(width: number, height: number): HTMLCanvasElement {
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  return canvas;
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.crossOrigin = 'anonymous';
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('Failed to load image source'));
    image.src = src;
  });
}

function loadVideo(src: string): Promise<HTMLVideoElement> {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    video.crossOrigin = 'anonymous';
    video.preload = 'auto';
    video.muted = false;
    video.playsInline = true;
    video.src = src;

    const handleReady = () => resolve(video);
    const handleError = () => reject(new Error('Failed to load video source'));

    video.addEventListener('loadedmetadata', handleReady, { once: true });
    video.addEventListener('error', handleError, { once: true });
  });
}

function toRadian(degree: number): number {
  return (degree * Math.PI) / 180;
}

function drawMediaToCanvas(
  ctx: CanvasRenderingContext2D,
  media: CanvasImageSource,
  mediaWidth: number,
  mediaHeight: number,
  options: RenderStoryOptions,
  outputWidth: number,
  outputHeight: number
) {
  const previewWidth = Math.max(options.previewSize.width, 1);
  const previewHeight = Math.max(options.previewSize.height, 1);
  const scaleX = outputWidth / previewWidth;
  const scaleY = outputHeight / previewHeight;

  const baseScale = Math.max(outputWidth / mediaWidth, outputHeight / mediaHeight);
  const translatedX = options.transform.crop.x * scaleX;
  const translatedY = options.transform.crop.y * scaleY;

  ctx.save();
  ctx.translate(outputWidth / 2 + translatedX, outputHeight / 2 + translatedY);
  ctx.rotate(toRadian(options.transform.rotation));
  ctx.scale(baseScale * options.transform.zoom, baseScale * options.transform.zoom);
  ctx.drawImage(media, -mediaWidth / 2, -mediaHeight / 2, mediaWidth, mediaHeight);
  ctx.restore();
}

function drawOverlayLayers(
  ctx: CanvasRenderingContext2D,
  layers: OverlayLayer[],
  previewSize: { width: number; height: number },
  outputSize: { width: number; height: number }
) {
  const scaleX = outputSize.width / Math.max(previewSize.width, 1);
  const scaleY = outputSize.height / Math.max(previewSize.height, 1);
  const baseFontSize = 48;

  layers.forEach((layer) => {
    const x = layer.x * scaleX;
    const y = layer.y * scaleY;

    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(toRadian(layer.rotation));
    ctx.scale(layer.scale, layer.scale);

    if (layer.type === 'text') {
      ctx.fillStyle = layer.color || '#ffffff';
      ctx.strokeStyle = 'rgba(0, 0, 0, 0.75)';
      ctx.lineWidth = 6;
      ctx.font = `700 ${baseFontSize}px system-ui, -apple-system, Segoe UI, Roboto, sans-serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.strokeText(layer.content, 0, 0);
      ctx.fillText(layer.content, 0, 0);
    } else {
      ctx.font = `${baseFontSize + 24}px "Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", sans-serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(layer.content, 0, 0);
    }

    ctx.restore();
  });
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality = 0.92): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('Failed to export canvas blob'));
        return;
      }
      resolve(blob);
    }, type, quality);
  });
}

function buildFile(blob: Blob, name: string, type: string): File {
  return new File([blob], name, { type });
}

export async function renderImageStoryFile(options: RenderStoryOptions): Promise<File> {
  const outputSize = options.outputSize || DEFAULT_OUTPUT;
  const image = await loadImage(options.sourceUrl);
  const canvas = createCanvas(outputSize.width, outputSize.height);
  const ctx = canvas.getContext('2d');

  if (!ctx) {
    throw new Error('Canvas context is not available');
  }

  drawMediaToCanvas(ctx, image, image.naturalWidth, image.naturalHeight, options, outputSize.width, outputSize.height);
  drawOverlayLayers(ctx, options.layers, options.previewSize, outputSize);

  const blob = await canvasToBlob(canvas, 'image/jpeg', 0.92);
  return buildFile(blob, `story-${Date.now()}.jpg`, 'image/jpeg');
}

function extensionFromMimeType(mimeType: string): string {
  const normalized = mimeType.split(';')[0].trim().toLowerCase();

  if (normalized === 'video/mp4') {
    return 'mp4';
  }
  if (normalized === 'video/quicktime') {
    return 'mov';
  }
  if (normalized === 'video/x-matroska') {
    return 'mkv';
  }
  if (normalized === 'video/webm') {
    return 'webm';
  }

  return 'webm';
}

function buildVideoMimeCandidates(sourceMimeType?: string): string[] {
  const fallback = ['video/webm;codecs=vp9', 'video/webm;codecs=vp8', 'video/webm'];
  if (!sourceMimeType) {
    return fallback;
  }

  const normalized = sourceMimeType.split(';')[0].trim().toLowerCase();
  const preferred: string[] = [];

  if (normalized === 'video/mp4') {
    preferred.push('video/mp4;codecs=avc1.42E01E,mp4a.40.2', 'video/mp4');
  } else if (normalized === 'video/quicktime') {
    preferred.push('video/quicktime');
  } else if (normalized === 'video/webm') {
    preferred.push('video/webm;codecs=vp9', 'video/webm;codecs=vp8', 'video/webm');
  } else {
    preferred.push(normalized);
  }

  return [...preferred, ...fallback];
}

function getSupportedMimeType(sourceMimeType?: string): string {
  const candidates = buildVideoMimeCandidates(sourceMimeType);

  for (const candidate of candidates) {
    if (typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported(candidate)) {
      return candidate;
    }
  }

  return '';
}

function getCapturedMediaStream(video: HTMLVideoElement): MediaStream | null {
  const mediaElement = video as HTMLVideoElement & {
    captureStream?: () => MediaStream;
    mozCaptureStream?: () => MediaStream;
  };

  if (typeof mediaElement.captureStream === 'function') {
    return mediaElement.captureStream();
  }

  if (typeof mediaElement.mozCaptureStream === 'function') {
    return mediaElement.mozCaptureStream();
  }

  return null;
}

export async function renderVideoStoryFile(options: RenderStoryOptions): Promise<File> {
  if (typeof MediaRecorder === 'undefined') {
    throw new Error('MediaRecorder is not supported on this browser');
  }

  const outputSize = options.outputSize || DEFAULT_OUTPUT;
  const canvas = createCanvas(outputSize.width, outputSize.height);
  const ctx = canvas.getContext('2d');

  if (!ctx) {
    throw new Error('Canvas context is not available');
  }

  const video = await loadVideo(options.sourceUrl);

  const canvasStream = canvas.captureStream(30);
  const sourceStream = getCapturedMediaStream(video);
  const combinedStream = new MediaStream();

  canvasStream.getVideoTracks().forEach((track) => combinedStream.addTrack(track));
  sourceStream?.getAudioTracks().forEach((track) => combinedStream.addTrack(track));

  const mimeType = getSupportedMimeType(options.sourceFile?.type);
  if (!mimeType) {
    throw new Error('No supported video output format found for this browser');
  }

  const recorder = new MediaRecorder(combinedStream, { mimeType });
  const chunks: BlobPart[] = [];

  recorder.ondataavailable = (event) => {
    if (event.data.size > 0) {
      chunks.push(event.data);
    }
  };

  const recordedBlobPromise = new Promise<Blob>((resolve, reject) => {
    recorder.onerror = () => reject(new Error('Video recorder failed'));
    recorder.onstop = () => {
      const blob = new Blob(chunks, { type: mimeType });
      resolve(blob);
    };
  });

  const drawFrame = () => {
    ctx.clearRect(0, 0, outputSize.width, outputSize.height);
    drawMediaToCanvas(ctx, video, video.videoWidth, video.videoHeight, options, outputSize.width, outputSize.height);
    drawOverlayLayers(ctx, options.layers, options.previewSize, outputSize);
  };

  recorder.start(250);
  await video.play();

  await new Promise<void>((resolve) => {
    let rafId = 0;

    const render = () => {
      drawFrame();
      if (video.ended || video.paused) {
        resolve();
        return;
      }
      rafId = window.requestAnimationFrame(render);
    };

    video.onended = () => {
      window.cancelAnimationFrame(rafId);
      resolve();
    };

    render();
  });

  recorder.stop();
  combinedStream.getTracks().forEach((track) => track.stop());

  const finalBlob = await recordedBlobPromise;
  const extension = extensionFromMimeType(mimeType);
  return buildFile(finalBlob, `story-${Date.now()}.${extension}`, mimeType);
}
