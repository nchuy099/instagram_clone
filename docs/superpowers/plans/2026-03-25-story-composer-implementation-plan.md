# Story Composer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a story composer step that previews selected image/video immediately, supports crop/zoom/rotate via `react-easy-crop`, supports custom React text/sticker overlays, and uploads only the final rendered media file.

**Architecture:** Keep backend contracts unchanged and move story creation to a two-step frontend flow: select file in `StoriesBar` then edit/publish in a new `StoryComposerModal`. Use `react-easy-crop` for base media transform and custom overlay state/components for text/sticker interaction. Render final media in-browser (Canvas for images, Canvas + MediaRecorder for videos), then reuse existing upload + createStory service calls.

**Tech Stack:** React 19 + TypeScript + Vite, `react-easy-crop`, HTML Canvas API, MediaRecorder API, existing `mediaService` and `storyService`.

---

### Task 1: Add Story Composer domain types and validation helpers

**Files:**
- Create: `frontend/src/features/story/components/storyComposer/types.ts`
- Create: `frontend/src/features/story/components/storyComposer/validators.ts`
- Create: `frontend/src/features/story/components/storyComposer/types.test.ts`
- Create: `frontend/src/features/story/components/storyComposer/validators.test.ts`

- [ ] **Step 1: Write the failing tests for overlay data and file validation helpers**

```ts
// validators.test.ts
it('rejects unsupported file type', async () => {
  const file = new File(['x'], 'x.txt', { type: 'text/plain' });
  expect(validateStoryFile(file).ok).toBe(false);
});
```

- [ ] **Step 2: Run test command to confirm RED**

Run: `cd frontend && npm run test -- src/features/story/components/storyComposer/validators.test.ts`
Expected: FAIL because test runner/config/helper functions do not exist yet.

- [ ] **Step 3: Implement minimal helper code in `types.ts` and `validators.ts`**

```ts
export function validateStoryFile(file: File): ValidationResult {
  if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
    return { ok: false, reason: 'Unsupported file type' };
  }
  return { ok: true };
}
```

- [ ] **Step 4: Run tests and confirm GREEN**

Run: `cd frontend && npm run test -- src/features/story/components/storyComposer/validators.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/story/components/storyComposer/types.ts frontend/src/features/story/components/storyComposer/validators.ts frontend/src/features/story/components/storyComposer/types.test.ts frontend/src/features/story/components/storyComposer/validators.test.ts
git commit -m "feat(story): add composer domain types and validation helpers"
```

### Task 2: Add story render pipeline helpers (image/video)

**Files:**
- Create: `frontend/src/features/story/components/storyComposer/renderers.ts`
- Create: `frontend/src/features/story/components/storyComposer/renderers.test.ts`

- [ ] **Step 1: Write failing tests for image render and MediaRecorder guard paths**
- [ ] **Step 2: Run RED via** `cd frontend && npm run test -- src/features/story/components/storyComposer/renderers.test.ts`
- [ ] **Step 3: Implement minimal render helpers**

```ts
export async function renderImageStoryFile(...) { /* canvas draw + toBlob */ }
export async function renderVideoStoryFile(...) { /* canvas.captureStream + MediaRecorder */ }
```

- [ ] **Step 4: Run GREEN tests**
- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/story/components/storyComposer/renderers.ts frontend/src/features/story/components/storyComposer/renderers.test.ts
git commit -m "feat(story): add story image/video render helpers"
```

### Task 3: Build Story Composer UI controls

**Files:**
- Create: `frontend/src/features/story/components/storyComposer/TransformControls.tsx`
- Create: `frontend/src/features/story/components/storyComposer/OverlayControls.tsx`
- Create: `frontend/src/features/story/components/storyComposer/OverlayCanvas.tsx`
- Create: `frontend/src/features/story/components/storyComposer/OverlayCanvas.test.tsx`

- [ ] **Step 1: Write failing tests for add/select/delete layer behavior**
- [ ] **Step 2: Run RED via** `cd frontend && npm run test -- src/features/story/components/storyComposer/OverlayCanvas.test.tsx`
- [ ] **Step 3: Implement minimal components and event handlers**
- [ ] **Step 4: Run GREEN tests**
- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/story/components/storyComposer/TransformControls.tsx frontend/src/features/story/components/storyComposer/OverlayControls.tsx frontend/src/features/story/components/storyComposer/OverlayCanvas.tsx frontend/src/features/story/components/storyComposer/OverlayCanvas.test.tsx
git commit -m "feat(story): add composer transform and overlay controls"
```

### Task 4: Implement StoryComposerModal orchestration

**Files:**
- Create: `frontend/src/features/story/components/StoryComposerModal.tsx`
- Create: `frontend/src/features/story/components/StoryComposerModal.test.tsx`

- [ ] **Step 1: Write failing modal tests for open -> edit -> share pipeline**
- [ ] **Step 2: Run RED via** `cd frontend && npm run test -- src/features/story/components/StoryComposerModal.test.tsx`
- [ ] **Step 3: Implement minimal modal state, crop integration (`react-easy-crop`), and publish handler**
- [ ] **Step 4: Run GREEN tests**
- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/story/components/StoryComposerModal.tsx frontend/src/features/story/components/StoryComposerModal.test.tsx
git commit -m "feat(story): add story composer modal with edit and publish flow"
```

### Task 5: Wire StoriesBar to editor flow

**Files:**
- Modify: `frontend/src/features/story/components/StoriesBar.tsx`
- Create: `frontend/src/features/story/components/StoriesBar.story-composer.test.tsx`

- [ ] **Step 1: Write failing test ensuring file select opens composer instead of direct upload**
- [ ] **Step 2: Run RED via** `cd frontend && npm run test -- src/features/story/components/StoriesBar.story-composer.test.tsx`
- [ ] **Step 3: Implement StoriesBar integration with `StoryComposerModal` and refresh callback**
- [ ] **Step 4: Run GREEN tests**
- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/story/components/StoriesBar.tsx frontend/src/features/story/components/StoriesBar.story-composer.test.tsx
git commit -m "feat(story): route story upload through composer editor"
```

### Task 6: Verification and cleanup

**Files:**
- Modify (if needed): `frontend/src/features/story/components/*.tsx`

- [ ] **Step 1: Run targeted tests**

Run:
- `cd frontend && npm run test -- src/features/story/components/storyComposer/validators.test.ts`
- `cd frontend && npm run test -- src/features/story/components/storyComposer/renderers.test.ts`
- `cd frontend && npm run test -- src/features/story/components/StoryComposerModal.test.tsx`

Expected: PASS.

- [ ] **Step 2: Run static checks**

Run:
- `cd frontend && npm run lint`
- `cd frontend && npm run build`

Expected: PASS with no blocking errors.

- [ ] **Step 3: Commit any final fixes**

```bash
git add frontend/src/features/story/components frontend/src/features/story/components/storyComposer
git commit -m "chore(story): finalize composer integration checks"
```
