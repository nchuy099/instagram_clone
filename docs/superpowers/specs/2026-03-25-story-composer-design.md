# Story Composer Design (Image + Video)

Date: 2026-03-25  
Scope: Frontend story creation flow (no backend API change)

## 1. Goals

- After selecting media for story, show the media immediately in an editor.
- Support image and video stories.
- Add media transform tools with `react-easy-crop`: crop/zoom/rotate.
- Add custom React overlays: text and sticker (emoji) layers.
- Export final composed media on frontend, upload only the final file, then call create story API.

## 2. Non-Goals

- No backend schema/API contract changes.
- No advanced timeline/video editing (trim, multiple clips, audio mix).
- No layer ordering UI (bring front/back) in v1.
- No persistent editable project state after close/reload.

## 3. Current State

- Story creation is in `frontend/src/features/story/components/StoriesBar.tsx`.
- Current flow: file select -> upload to S3 -> `createStory`.
- No editing step before upload.

## 4. Proposed UX Flow

1. User clicks `Your story` add button and selects image/video.
2. Open `StoryComposerModal` with immediate preview.
3. User edits media transform and overlays.
4. User clicks `Share Story`.
5. Frontend renders final media file.
6. Upload final file via existing `mediaService.uploadFile`.
7. Create story via existing `storyService.createStory`.
8. Refresh story list and close modal.

## 5. Architecture Changes

## 5.1 Components

- Update: `frontend/src/features/story/components/StoriesBar.tsx`
  - Replace direct upload flow with open-editor flow.
  - Keep final publish path (`upload -> createStory -> refresh`).

- New: `frontend/src/features/story/components/StoryComposerModal.tsx`
  - Main editor UI and state orchestration.
  - Hosts preview area, transform controls, overlay controls, and publish action.

- New: `frontend/src/features/story/components/storyComposer/OverlayCanvas.tsx`
  - Renders interactive text/sticker overlays over preview.
  - Handles select/drag/scale/rotate layer interactions.

- New: `frontend/src/features/story/components/storyComposer/TransformControls.tsx`
  - Zoom slider, rotate slider, and reset transform controls.

- New: `frontend/src/features/story/components/storyComposer/OverlayControls.tsx`
  - Add text button.
  - Add sticker (emoji) button.
  - Delete selected layer action.

## 5.2 Utility / Render Modules

- New: `frontend/src/features/story/components/storyComposer/types.ts`
  - Shared types for overlays and transforms.

- New: `frontend/src/features/story/components/storyComposer/renderers.ts`
  - `renderImageStoryFile(...)`
  - `renderVideoStoryFile(...)`
  - Shared draw helpers for media transform + overlays.

- New: `frontend/src/features/story/components/storyComposer/validators.ts`
  - File validation (type, max size, max duration).

## 6. Data Model (Frontend)

```ts
type OverlayType = 'text' | 'sticker';

interface OverlayLayer {
  id: string;
  type: OverlayType;
  content: string; // text or emoji
  x: number;       // preview-space position
  y: number;
  scale: number;
  rotation: number;
}

interface MediaTransformState {
  crop: { x: number; y: number };
  zoom: number;
  rotation: number;
  croppedAreaPixels: { x: number; y: number; width: number; height: number } | null;
}
```

## 7. Editor Interaction Design

- Preview target ratio: `9:16`.
- Use `react-easy-crop` for base media transform:
  - `crop`
  - `zoom`
  - `rotation`
  - `onCropComplete` to capture pixel crop data.

- Overlay interactions (custom React):
  - Click layer to select.
  - Drag selected layer to move.
  - Scale selected layer (wheel or +/- control).
  - Rotate selected layer with handle.
  - Delete selected layer.

- Initial layer defaults:
  - Text layer: `"Type here"`, centered.
  - Sticker layer: `"😀"`, centered.

## 8. Rendering Strategy (Final File Export)

## 8.1 Image Story

1. Load source image.
2. Create target canvas (9:16 output size).
3. Draw transformed/cropped image onto canvas.
4. Draw overlays on top.
5. Export to blob (`image/jpeg`, quality 0.92).
6. Wrap blob as `File` and upload.

## 8.2 Video Story

1. Load source video in hidden element.
2. Create target canvas (9:16).
3. Start `canvas.captureStream(30)`.
4. Use `MediaRecorder` to capture canvas stream.
5. During playback, draw each frame:
  - transformed video frame
  - all overlays
6. Stop recorder at video end.
7. Build final `File` from recorded blob and upload.

## 9. Error Handling

- Unsupported type -> block with message.
- Exceed size/duration limit -> block with message.
- `MediaRecorder` unsupported/failure -> show browser unsupported warning.
- Render failure -> keep modal open; allow retry.
- Upload/API failure -> keep state and allow retry.
- Cleanup:
  - revoke object URLs on close/unmount.
  - stop recorder/streams safely.

## 10. API Contract

- Reuse existing calls:
  - `mediaService.uploadFile(file)`
  - `storyService.createStory({ mediaUrl, mediaType })`
- `mediaType` inferred from final file MIME (`video/*` => `VIDEO`, else `IMAGE`).

## 11. Testing Plan (TDD)

## 11.1 Unit Tests

- `validators.ts`
  - accepts valid image/video
  - rejects oversize file
  - rejects invalid type
  - rejects too-long video duration

- `renderers.ts` helpers
  - transform math for draw area
  - overlay serialization/normalization

## 11.2 Component Tests

- `StoriesBar`
  - selecting file opens composer instead of direct upload.

- `StoryComposerModal`
  - can add text layer.
  - can add sticker layer.
  - can select and delete layer.
  - share action calls pipeline in order: render -> upload -> createStory.
  - handles render/upload failure states.

## 11.3 Integration Boundary

- Mock browser APIs where needed:
  - `HTMLCanvasElement`
  - `MediaRecorder`
  - `HTMLVideoElement` metadata/events
- No full real video encoding E2E in unit test environment.

## 12. Rollout Notes

- Keep existing viewer unchanged for this iteration.
- If video rendering performance is poor on lower-end devices:
  - reduce output resolution and/or FPS in renderer module.
  - show clear in-progress indicator during export.
