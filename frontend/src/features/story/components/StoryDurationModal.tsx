import { useEffect, useMemo, useState } from "react";

interface StoryDurationModalProps {
  file: File;
  isOpen: boolean;
  isSubmitting?: boolean;
  onClose: () => void;
  onConfirm: (durationHours: 6 | 12 | 24) => void | Promise<void>;
}

const DURATION_OPTIONS: Array<6 | 12 | 24> = [6, 12, 24];

export default function StoryDurationModal({
  file,
  isOpen,
  isSubmitting = false,
  onClose,
  onConfirm
}: StoryDurationModalProps) {
  const [selectedDuration, setSelectedDuration] = useState<6 | 12 | 24>(24);

  const previewUrl = useMemo(() => {
    if (!isOpen) {
      return "";
    }
    return URL.createObjectURL(file);
  }, [file, isOpen]);

  useEffect(
    () => () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    },
    [previewUrl]
  );

  if (!isOpen) {
    return null;
  }

  const isVideo = file.type.startsWith("video/");

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl">
        <h3 className="text-base font-semibold text-gray-900">Preview story</h3>
        <p className="mt-1 text-sm text-gray-500">Choose how long this story should stay visible.</p>

        <div className="mt-4 flex justify-center">
          <div className="w-full max-w-[220px] overflow-hidden rounded-lg bg-black" style={{ aspectRatio: "9 / 16" }}>
            {previewUrl ? (
              isVideo ? (
                <video
                  src={previewUrl}
                  className="h-full w-full object-cover"
                  controls
                  muted
                  loop
                  autoPlay
                  playsInline
                />
              ) : (
                <img src={previewUrl} alt="Story preview" className="h-full w-full object-cover" />
              )
            ) : null}
          </div>
        </div>

        <div className="mt-4 grid grid-cols-3 gap-2">
          {DURATION_OPTIONS.map((hours) => (
            <button
              key={hours}
              type="button"
              className={`rounded-lg border px-3 py-2 text-sm font-medium transition disabled:opacity-50 ${
                selectedDuration === hours
                  ? "border-[#0095f6] bg-[#0095f6] text-white"
                  : "border-gray-300 text-gray-800 hover:bg-gray-100"
              }`}
              disabled={isSubmitting}
              onClick={() => setSelectedDuration(hours)}
            >
              {hours}h
            </button>
          ))}
        </div>

        <p className="mt-3 text-sm text-gray-600">Story will expire in {selectedDuration} hours.</p>

        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="flex-1 rounded-lg px-3 py-2 text-sm text-gray-600 transition hover:bg-gray-100 disabled:opacity-50"
            disabled={isSubmitting}
            onClick={onClose}
          >
            Cancel
          </button>
          <button
            type="button"
            className="flex-1 rounded-lg bg-[#0095f6] px-3 py-2 text-sm font-semibold text-white transition hover:bg-[#1877f2] disabled:opacity-50"
            disabled={isSubmitting}
            onClick={() => void onConfirm(selectedDuration)}
          >
            {isSubmitting ? "Posting..." : "Post story"}
          </button>
        </div>
      </div>
    </div>
  );
}
