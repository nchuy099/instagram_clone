import type { OverlayLayer } from './types';

interface OverlayControlsProps {
  selectedLayer?: OverlayLayer;
  onAddText: () => void;
  onAddSticker: () => void;
  onDeleteSelected: () => void;
  onUpdateSelected: (patch: Partial<OverlayLayer>) => void;
}

export default function OverlayControls({
  selectedLayer,
  onAddText,
  onAddSticker,
  onDeleteSelected,
  onUpdateSelected
}: OverlayControlsProps) {
  return (
    <div className="space-y-3 rounded-lg border border-gray-200 bg-white p-3">
      <div className="grid grid-cols-2 gap-2">
        <button
          type="button"
          onClick={onAddText}
          className="rounded-md border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-50"
        >
          Add text
        </button>
        <button
          type="button"
          onClick={onAddSticker}
          className="rounded-md border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-50"
        >
          Add sticker
        </button>
      </div>

      {selectedLayer ? (
        <>
          {selectedLayer.type === 'text' && (
            <input
              type="text"
              value={selectedLayer.content}
              onChange={(event) => onUpdateSelected({ content: event.target.value })}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
              placeholder="Text"
            />
          )}

          <div className="grid grid-cols-2 gap-2 text-xs text-gray-700">
            <button
              type="button"
              className="rounded-md border border-gray-300 px-2 py-1 hover:bg-gray-50"
              onClick={() => onUpdateSelected({ scale: Math.max(0.5, selectedLayer.scale - 0.1) })}
            >
              Scale -
            </button>
            <button
              type="button"
              className="rounded-md border border-gray-300 px-2 py-1 hover:bg-gray-50"
              onClick={() => onUpdateSelected({ scale: Math.min(3, selectedLayer.scale + 0.1) })}
            >
              Scale +
            </button>
            <button
              type="button"
              className="rounded-md border border-gray-300 px-2 py-1 hover:bg-gray-50"
              onClick={() => onUpdateSelected({ rotation: selectedLayer.rotation - 15 })}
            >
              Rotate -15
            </button>
            <button
              type="button"
              className="rounded-md border border-gray-300 px-2 py-1 hover:bg-gray-50"
              onClick={() => onUpdateSelected({ rotation: selectedLayer.rotation + 15 })}
            >
              Rotate +15
            </button>
          </div>

          <button
            type="button"
            onClick={onDeleteSelected}
            className="w-full rounded-md border border-red-200 px-3 py-2 text-sm font-medium text-red-600 transition hover:bg-red-50"
          >
            Delete selected layer
          </button>
        </>
      ) : (
        <p className="text-xs text-gray-500">Select a text/sticker layer to edit it.</p>
      )}
    </div>
  );
}
