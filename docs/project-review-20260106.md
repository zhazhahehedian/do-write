# Project Progress Review - 2026-01-06

## 1. Overview
The **do-write** project has made significant progress in both backend and frontend development. The core infrastructure for user authentication, project management, wizard-based creation, and chapter generation is in place. However, there are critical integration issues between the frontend API clients and backend controllers that need immediate attention.

## 2. Backend Status (`write-server`)
The backend implementation follows the architecture described in `GEMINI.md` and `CLAUDE.md`.

### Implemented Modules
*   **Authentication**: `AuthController` (Login, Register), `OAuthController`, `SaToken` integration.
*   **User Management**: `UserApiConfigController` for managing encrypted API keys.
*   **Novel Core**:
    *   `ProjectController`: Project CRUD.
    *   `WizardController`: Creation wizard (World, Character, Outline) with SSE support.
    *   `ChapterController`: Chapter generation, regeneration, polishing with SSE support.
    *   `StoryMemoryController`: Vector store interactions (RAG).
*   **AI Service**: `ChatClientFactory` for dynamic model selection, `WritingStyleManager`.
*   **Data Access**: MyBatis-Plus mappers and entities (`NovelProject`, `NovelChapter`, etc.) are complete.

### Key Observations
*   **Controller Structure**: Well-organized `auth`, `novel`, `user` packages.
*   **Streaming**: SSE is correctly implemented in `WizardController` and `ChapterController` using `Flux<ServerSentEvent<String>>`.
*   **Database**: Entities align with the provided SQL schema.

## 3. Frontend Status (`frontend`)
The frontend is built with Next.js 16 and uses modern React patterns (Zustand, React Query, Lucide icons).

### Implemented Features
*   **API Clients**: `lib/api/` contains typed clients for `auth`, `chapter`, `wizard`, etc.
*   **Pages**:
    *   Auth: Login/Register pages.
    *   Project: List, Detail, Wizard, Chapter List.
*   **Components**: Rich UI components using Radix UI and Tailwind.

## 4. Discrepancies & Critical Issues

### 🔴 CRITICAL: Chapter List Endpoint Mismatch
There is a fundamental mismatch between how the Frontend calls the Chapter List API and how the Backend expects it.

*   **Frontend (`lib/api/chapter.ts`)**:
    ```typescript
    list: (projectId: string, params?: PageRequest) =>
      request<PageResult<Chapter>>({
        method: 'GET',
        url: '/novel/chapter/list',
        params: { projectId, ...params }, // Sends as Query Parameters
      }),
    ```
*   **Backend (`ChapterController.java`)**:
    ```java
    @PostMapping("/list")
    public Result<PageResult<ChapterVO>> list(
            @RequestBody @Valid ChapterQueryRequest request) { // Expects JSON Body
    ```

**Impact**: The Chapter List page will fail with `405 Method Not Allowed` or `400 Bad Request`.

### 🟡 WARNING: API Feature Gaps
1.  **AI Denoise**: Backend has `POST /novel/chapter/denoise/{chapterId}`, but this is missing from `frontend/lib/api/chapter.ts`.
2.  **Context Preview**: Backend has `GET /novel/chapter/context`, missing in frontend API.

## 5. Recommendations

### Immediate Actions
1.  **Fix Chapter List Endpoint**:
    *   **Option A (Recommended)**: Change Frontend to use `POST`.
        *   Update `lib/api/chapter.ts` to use `method: 'POST'` and send `data` instead of `params`.
    *   **Option B**: Change Backend to use `GET`.
        *   Update `ChapterController` to `@GetMapping("/list")` and use `@ModelAttribute` or individual `@RequestParam` (Note: complex objects in GET requests can be tricky).

### Next Steps
1.  **Sync API Definitions**: Update `frontend/lib/api/` to include missing endpoints (`denoise`, `context`).
2.  **Integration Testing**: Manually verify the Wizard flow -> Outline Generation -> Chapter List flow to ensure data flows correctly.
3.  **Error Handling**: Ensure Frontend gracefully handles SSE errors (the backend sends specific error events).

## 6. Documentation
*   This review has been saved to `docs/project-review-20260106.md`.
