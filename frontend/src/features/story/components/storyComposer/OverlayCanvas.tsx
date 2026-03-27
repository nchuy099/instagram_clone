import { useRef, useState } from 'react';
import type { OverlayLayer } from './types';

interface OverlayCanvasProps {
  layers: OverlayLayer[];
  selectedLayerId: string | null;
  width: number;
  height: number;
  onSelectLayer: (id: string) => void;
  onUpdateLayer: (id: string, patch: Partial<OverlayLayer>) => void;
}

interface DragState {
  id: string;
  pointerId: number;
  offsetX: number;
  offsetY: number;
}

export default function OverlayCanvas({
  layers,
  selectedLayerId,
  width,
  height,
  onSelectLayer,
  onUpdateLayer
}: OverlayCanvasProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [dragState, setDragState] = useState<DragState | null>(null);

  const onPointerDown = (event: React.PointerEvent<HTMLButtonElement>, layer: OverlayLayer) => {
    event.preventDefault();
    event.stopPropagation();

    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) {
      return;
    }

    onSelectLayer(layer.id);

    const offsetX = event.clientX - rect.left - layer.x;
    const offsetY = event.clientY - rect.top - layer.y;

    setDragState({
      id: layer.id,
      pointerId: event.pointerId,
      offsetX,
      offsetY
    });

    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const onPointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!dragState) {
      return;
    }

    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) {
      return;
    }

    const nextX = Math.min(Math.max(event.clientX - rect.left - dragState.offsetX, 0), width);
    const nextY = Math.min(Math.max(event.clientY - rect.top - dragState.offsetY, 0), height);

    onUpdateLayer(dragState.id, { x: nextX, y: nextY });
  };

  const onPointerUp = (event: React.PointerEvent<HTMLDivElement>) => {
    if (dragState && event.pointerId === dragState.pointerId) {
      setDragState(null);
    }
  };

  return (
    <div
      ref={containerRef}
      className="absolute inset-0 z-20"
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerUp}
      onClick={(event) => event.stopPropagation()}
    >
      {layers.map((layer) => {
        const isSelected = layer.id === selectedLayerId;

        return (
          <button
            key={layer.id}
            type="button"
            className={`absolute -translate-x-1/2 -translate-y-1/2 rounded px-2 py-1 ${
              isSelected ? 'ring-2 ring-blue-400' : ''
            }`}
            style={{
              left: layer.x,
              top: layer.y,
              transform: `translate(-50%, -50%) rotate(${layer.rotation}deg) scale(${layer.scale})`,
              color: layer.type === 'text' ? layer.color || '#ffffff' : undefined,
              textShadow: '0 1px 4px rgba(0, 0, 0, 0.65)',
              fontWeight: layer.type === 'text' ? 700 : 500,
              fontSize: layer.type === 'text' ? 30 : 48,
              lineHeight: 1,
              cursor: 'move',
              userSelect: 'none'
            }}
            onClick={(event) => {
              event.preventDefault();
              event.stopPropagation();
              onSelectLayer(layer.id);
            }}
            onPointerDown={(event) => onPointerDown(event, layer)}
          >
            {layer.content}
          </button>
        );
      })}
    </div>
  );
}
