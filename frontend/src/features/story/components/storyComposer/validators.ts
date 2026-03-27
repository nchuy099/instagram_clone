const SUPPORTED_PREFIXES = ['image/', 'video/'];
const MAX_FILE_SIZE_MB = 100;
const MAX_VIDEO_DURATION_SECONDS = 30;

export interface ValidationResult {
  ok: boolean;
  reason?: string;
}

export function validateStoryFile(file: File): ValidationResult {
  const isSupported = SUPPORTED_PREFIXES.some((prefix) => file.type.startsWith(prefix));
  if (!isSupported) {
    return { ok: false, reason: 'Unsupported file type. Please choose image or video.' };
  }

  const maxBytes = MAX_FILE_SIZE_MB * 1024 * 1024;
  if (file.size > maxBytes) {
    return { ok: false, reason: `File is too large. Maximum allowed size is ${MAX_FILE_SIZE_MB}MB.` };
  }

  return { ok: true };
}

export async function validateVideoDuration(file: File): Promise<ValidationResult> {
  if (!file.type.startsWith('video/')) {
    return { ok: true };
  }

  const video = document.createElement('video');
  const objectUrl = URL.createObjectURL(file);

  try {
    const duration = await new Promise<number>((resolve, reject) => {
      video.preload = 'metadata';
      video.src = objectUrl;
      video.onloadedmetadata = () => resolve(video.duration);
      video.onerror = () => reject(new Error('Unable to read video metadata'));
    });

    if (!Number.isFinite(duration) || duration <= 0) {
      return { ok: false, reason: 'Invalid video duration.' };
    }

    if (duration > MAX_VIDEO_DURATION_SECONDS) {
      return {
        ok: false,
        reason: `Video is too long. Maximum duration is ${MAX_VIDEO_DURATION_SECONDS} seconds.`
      };
    }

    return { ok: true };
  } catch {
    return { ok: false, reason: 'Cannot read video metadata on this browser.' };
  } finally {
    URL.revokeObjectURL(objectUrl);
    video.removeAttribute('src');
    video.load();
  }
}
