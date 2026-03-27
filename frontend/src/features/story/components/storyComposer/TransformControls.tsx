interface TransformControlsProps {
  zoom: number;
  rotation: number;
  onZoomChange: (zoom: number) => void;
  onRotationChange: (rotation: number) => void;
  onReset: () => void;
}

export default function TransformControls({
  zoom,
  rotation,
  onZoomChange,
  onRotationChange,
  onReset
}: TransformControlsProps) {
  return (
    <div className="space-y-3 rounded-lg border border-gray-200 bg-white p-3">
      <div>
        <div className="mb-1 flex items-center justify-between text-xs text-gray-600">
          <span>Zoom</span>
          <span>{zoom.toFixed(2)}x</span>
        </div>
        <input
          type="range"
          min={1}
          max={4}
          step={0.01}
          value={zoom}
          onChange={(event) => onZoomChange(Number(event.target.value))}
          className="w-full"
        />
      </div>

      <div>
        <div className="mb-1 flex items-center justify-between text-xs text-gray-600">
          <span>Rotate</span>
          <span>{Math.round(rotation)}deg</span>
        </div>
        <input
          type="range"
          min={-180}
          max={180}
          step={1}
          value={rotation}
          onChange={(event) => onRotationChange(Number(event.target.value))}
          className="w-full"
        />
      </div>

      <button
        type="button"
        onClick={onReset}
        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-50"
      >
        Reset transform
      </button>
    </div>
  );
}
