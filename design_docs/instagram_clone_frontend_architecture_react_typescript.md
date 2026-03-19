# Frontend Architecture for Instagram Clone (React + TypeScript thuần)

## 1. Mục tiêu thiết kế

Tài liệu này mô tả kiến trúc frontend đầy đủ cho một **Instagram Clone Web** được xây dựng với:

- **React**
- **TypeScript**
- **TailwindCSS**
- **React thuần** (không dùng React Query, Zustand, Redux, MobX)

Thiết kế này bám theo yêu cầu UI:

- Layout 3 cột trên desktop
- Feed là trung tâm trải nghiệm
- Sidebar trái cho điều hướng
- Sidebar phải cho suggestion/activity/shortcuts
- Modal-based interaction cho post detail và create post
- Responsive cho tablet/mobile
- Ưu tiên component tái sử dụng, code rõ ràng, dễ maintain

---

## 2. Nguyên tắc kiến trúc

Frontend nên được thiết kế theo các nguyên tắc sau:

### 2.1 Content-first UI
- Nội dung media là trung tâm
- Feed, stories, modal post detail phải mượt và trực quan

### 2.2 Feature-based organization
- Tổ chức code theo tính năng: auth, feed, posts, profile, messages, search...
- Dễ mở rộng và bảo trì hơn cách gom toàn bộ component vào một chỗ

### 2.3 React thuần, tách rõ loại state
Không dùng thư viện quản lý state ngoài, nên cần chia rõ:

- **Local UI state**: modal open/close, input text, active tab, preview media
- **Lifted state**: state dùng chung trong 1 page hoặc 1 widget lớn
- **Context state**: auth, theme, socket, global modal manager
- **Server state tự quản lý**: loading / error / data / pagination viết bằng hooks custom và useEffect

### 2.4 Reusable components
- PostCard, Avatar, Modal, Input, Tabs, Button, Grid, SidebarItem...
- Dùng được ở nhiều page khác nhau

### 2.5 Responsive-first implementation
- Desktop: 3 cột
- Tablet: 2 cột
- Mobile: 1 cột

---

## 3. Stack đề xuất

### Core
- React
- TypeScript
- React Router
- TailwindCSS
- Vite

### Hỗ trợ
- React Hook Form có thể dùng, nhưng nếu muốn thật sự “thuần React” thì có thể dùng `useState` + validate thủ công
- `react-icon` cho icon
- `date-fns` cho format thời gian
-  Raw WebSocket client cho realtime
- `react-dropzone` có thể dùng cho upload, nhưng không bắt buộc

---

## 4. Kiến trúc tổng thể của frontend

Frontend nên chia thành 6 lớp:

1. **App Layer**
   - bootstrap app
   - router
   - providers
   - global layouts

2. **Shared Layer**
   - UI components tái sử dụng
   - helpers
   - constants
   - api client
   - shared hooks

3. **Feature Layer**
   - auth
   - feed
   - posts
   - profile
   - search
   - messages
   - notifications
   - stories
   - create-post

4. **Widget Layer**
   - left sidebar
   - right sidebar
   - feed container
   - profile header
   - modal containers

5. **Page Layer**
   - HomePage
   - ProfilePage
   - ExplorePage
   - MessagesPage
   - LoginPage

6. **Global Context Layer**
   - AuthContext
   - ModalContext
   - SocketContext
   - AppLayoutContext nếu cần

---

## 5. Cấu trúc thư mục đề xuất

```txt
src/
  app/
    App.tsx
    main.tsx
    router/
      index.tsx
      ProtectedRoute.tsx
      PublicRoute.tsx
    layouts/
      MainLayout.tsx
      AuthLayout.tsx
      FeedLayout.tsx
    providers/
      AppProviders.tsx

  contexts/
    AuthContext.tsx
    ModalContext.tsx
    SocketContext.tsx

  shared/
    api/
      http.ts
      endpoints.ts
      auth.ts
    components/
      ui/
        Button.tsx
        Input.tsx
        TextArea.tsx
        Modal.tsx
        Avatar.tsx
        Tabs.tsx
        Dropdown.tsx
        Spinner.tsx
        Skeleton.tsx
        IconButton.tsx
      common/
        ErrorState.tsx
        EmptyState.tsx
        ConfirmDialog.tsx
        InfiniteScrollTrigger.tsx
        PageLoader.tsx
    hooks/
      useDebounce.ts
      useInfiniteScroll.ts
      useClickOutside.ts
      useMediaQuery.ts
      useAsync.ts
    lib/
      cn.ts
      format.ts
      storage.ts
      validators.ts
    constants/
      routes.ts
      breakpoints.ts
      app.ts
    types/
      api.ts
      common.ts
      pagination.ts

  features/
    auth/
      api/
        login.ts
        register.ts
        logout.ts
        getMe.ts
      components/
        LoginForm.tsx
        RegisterForm.tsx
        OAuthButtons.tsx
      hooks/
        useAuth.ts
      types/
        auth.types.ts
      utils/
        authMapper.ts

    feed/
      api/
        getFeed.ts
      components/
        FeedList.tsx
        FeedItem.tsx
        FeedSkeleton.tsx
      hooks/
        useFeed.ts
      types/
        feed.types.ts

    posts/
      api/
        getPostDetail.ts
        createPost.ts
        updatePost.ts
        deletePost.ts
        likePost.ts
        unlikePost.ts
        savePost.ts
        unsavePost.ts
      components/
        PostCard.tsx
        PostHeader.tsx
        PostMedia.tsx
        PostActions.tsx
        PostCaption.tsx
        PostCommentPreview.tsx
        PostGridItem.tsx
      hooks/
        usePostDetail.ts
        usePostActions.ts
      types/
        post.types.ts

    comments/
      api/
        getComments.ts
        createComment.ts
        deleteComment.ts
      components/
        CommentList.tsx
        CommentItem.tsx
        CommentInput.tsx
      hooks/
        useComments.ts
      types/
        comment.types.ts

    profile/
      api/
        getProfile.ts
        getUserPosts.ts
        getSavedPosts.ts
        getTaggedPosts.ts
        updateProfile.ts
      components/
        ProfileHeader.tsx
        ProfileTabs.tsx
        ProfileGrid.tsx
        EditProfileForm.tsx
      hooks/
        useProfile.ts
      types/
        profile.types.ts

    follow/
      api/
        followUser.ts
        unfollowUser.ts
        getFollowers.ts
        getFollowing.ts
      hooks/
        useFollowActions.ts

    stories/
      api/
        getStoriesFeed.ts
        createStory.ts
        markStoryViewed.ts
      components/
        StoriesBar.tsx
        StoryItem.tsx
        StoryViewer.tsx
      hooks/
        useStories.ts
      types/
        story.types.ts

    search/
      api/
        searchAll.ts
        searchUsers.ts
        searchHashtags.ts
        searchPosts.ts
        getRecentSearches.ts
      components/
        SearchInput.tsx
        SearchResults.tsx
        SearchTabs.tsx
        RecentSearchList.tsx
      hooks/
        useSearch.ts
      types/
        search.types.ts

    messages/
      api/
        getConversations.ts
        getMessages.ts
        sendMessage.ts
        createConversation.ts
      components/
        ConversationList.tsx
        ConversationItem.tsx
        ChatRoom.tsx
        MessageList.tsx
        MessageInput.tsx
      hooks/
        useConversations.ts
        useMessages.ts
      types/
        message.types.ts

    notifications/
      api/
        getNotifications.ts
        markNotificationRead.ts
        markAllRead.ts
      components/
        NotificationList.tsx
        NotificationItem.tsx
      hooks/
        useNotifications.ts
      types/
        notification.types.ts

    create-post/
      components/
        CreatePostModal.tsx
        UploadDropzone.tsx
        MediaPreview.tsx
        CaptionEditor.tsx
      hooks/
        useCreatePost.ts
      types/
        create-post.types.ts

  widgets/
    sidebar-left/
      LeftSidebar.tsx
      NavMenu.tsx
      NavItem.tsx
    sidebar-right/
      RightSidebar.tsx
      SuggestionsCard.tsx
      ActivityCard.tsx
    feed/
      FeedContainer.tsx
    profile/
      ProfileContent.tsx
    modals/
      GlobalModalRenderer.tsx
      PostDetailModal.tsx
      StoryViewerModal.tsx
      CreatePostRootModal.tsx

  pages/
    home/
      HomePage.tsx
    auth/
      LoginPage.tsx
      RegisterPage.tsx
    profile/
      ProfilePage.tsx
    explore/
      ExplorePage.tsx
    search/
      SearchPage.tsx
    messages/
      MessagesPage.tsx
    notifications/
      NotificationsPage.tsx
    saved/
      SavedPage.tsx
    settings/
      SettingsPage.tsx
    not-found/
      NotFoundPage.tsx

  styles/
    globals.css
```

---

## 6. Tổ chức theo feature thay vì theo technical layer thuần

Thay vì nhét tất cả component chung một thư mục lớn, app này nên tổ chức theo **business domain**.

Ví dụ:

- `features/posts` chứa tất cả logic liên quan post
- `features/profile` chứa toàn bộ logic profile
- `features/messages` chứa toàn bộ logic chat

Lợi ích:

- dễ mở rộng
- dễ đọc code
- dễ phân chia công việc theo team
- tránh file API, type, hook bị rời rạc

---

## 7. App layer

## 7.1 App.tsx
Đây là entry chính của ứng dụng.

Nhiệm vụ:

- mount router
- mount các context providers
- mount global modal renderer
- nạp global styles

## 7.2 AppProviders
Gom toàn bộ provider vào 1 chỗ:

- AuthProvider
- ModalProvider
- SocketProvider

Lợi ích:

- code main gọn
- dễ chỉnh sửa thứ tự provider

---

## 8. Routing architecture

Sử dụng `react-router-dom`.

### Route chính

```txt
/
/explore
/search
/messages
/notifications
/saved
/profile/:username
/login
/register
/settings
```

### Route strategy

- `AuthLayout` cho login/register
- `MainLayout` cho app sau khi đăng nhập
- `ProtectedRoute` để chặn route yêu cầu đăng nhập
- `PublicRoute` để tránh user đã login quay lại login/register

### Ví dụ router tree

```txt
AuthLayout
  /login
  /register

MainLayout
  /
  /explore
  /search
  /messages
  /notifications
  /saved
  /profile/:username
  /settings
```

---

## 9. Layout architecture

## 9.1 MainLayout
Đây là layout chính của app.

### Desktop
- Left sidebar cố định
- Main content ở giữa
- Right sidebar hiển thị suggestions, activity, shortcuts

### Tablet
- Left sidebar thu gọn
- Right sidebar ẩn bớt hoặc ẩn hoàn toàn

### Mobile
- Single column
- Sidebar có thể chuyển thành drawer hoặc top nav

### Trách nhiệm của MainLayout
- xác định responsive shell
- render left/right column
- render nội dung trang ở giữa thông qua `<Outlet />`

## 9.2 AuthLayout
Dành cho login/register:
- centered form
- background đơn giản
- ít distraction

## 9.3 FeedLayout
Dành riêng cho Home:
- stories phía trên
- feed phía dưới
- modal vẫn có thể phủ lên layout

---

## 10. Context architecture

Vì không dùng Zustand/Redux, state dùng chung nên đưa vào Context ở mức hợp lý.

## 10.1 AuthContext
Quản lý:
- currentUser
- isAuthenticated
- isLoadingAuth
- login()
- logout()
- refreshMe()

### Khi app khởi động
- đọc token từ localStorage hoặc cookie
- gọi `/me`
- set currentUser

## 10.2 ModalContext
Quản lý global modal:
- modal type đang mở
- payload modal
- openModal(type, payload)
- closeModal()

Ví dụ modal type:
- `postDetail`
- `createPost`
- `storyViewer`
- `confirmDelete`

## 10.3 SocketContext
Quản lý:
- socket instance
- connect/disconnect
- subscribe handlers
- expose send event nếu cần

Context này nên chỉ chứa phần kết nối và listener cơ bản, không nên ôm toàn bộ state chat/notification.

---

## 11. Thiết kế state khi không dùng React Query/Zustand

Đây là phần quan trọng nhất.

## 11.1 Phân loại state

### Local component state
Dùng `useState` cho:
- input text
- tab hiện tại
- hover state
- dropdown open/close
- selected image index
- form values đơn giản

### Shared page state
Dùng `useState` ở parent component và truyền xuống props cho:
- filter state của Explore
- active profile tab
- create post draft trong phạm vi modal
- selected conversation

### Global state qua Context
Dùng cho:
- auth
- global modal
- socket connection
- theme nếu có

### Async server state tự quản lý
Dùng custom hooks + `useEffect` + `useState`:
- loading
- error
- data
- pagination
- refetch

---

## 12. Mẫu hook quản lý async data

Vì không dùng React Query, nên mỗi feature cần custom hook rõ ràng.

### Pattern chung

```ts
{
  data,
  loading,
  error,
  fetchData,
  refetch,
  reset,
}
```

### Ví dụ useFeed
Quản lý:
- danh sách post feed
- loading page đầu
- loading thêm trang
- cursor hiện tại
- hasMore
- error

### Ví dụ useProfile
Quản lý:
- profile data
- loading
- error
- refetch profile

### Ví dụ useComments
Quản lý:
- comments theo postId
- adding state
- deleting state
- pagination comments nếu có

---

## 13. Shared API layer

## 13.1 http.ts
Tạo wrapper cho `fetch` hoặc `axios`.

Nên chuẩn hóa các hàm như:

- `get<T>()`
- `post<T>()`
- `patch<T>()`
- `deleteRequest<T>()`

Nhiệm vụ:
- set base URL
- attach auth token
- parse JSON
- throw error thống nhất
- support timeout nếu muốn

## 13.2 endpoints.ts
Lưu route backend ở 1 nơi:

- auth endpoints
- posts endpoints
- users endpoints
- messages endpoints

Lợi ích:
- tránh hard-code URL khắp app

---

## 14. Shared hooks

Các hook dùng nhiều nơi:

### useDebounce
- search input
- filter typing

### useInfiniteScroll
- feed
- comments
- profile grid

### useClickOutside
- dropdown
- modal dismiss logic

### useMediaQuery
- detect mobile/tablet/desktop

### useAsync
Có thể tạo hook generic để quản lý async action dạng:
- loading
- error
- run(asyncFn)

---

## 15. Shared UI components

Các component này không chứa business logic.

### Nhóm cơ bản
- Button
- Input
- TextArea
- IconButton
- Avatar
- Modal
- Tabs
- Dropdown
- Spinner
- Skeleton

### Nhóm common
- ErrorState
- EmptyState
- ConfirmDialog
- PageLoader
- InfiniteScrollTrigger

### Quy tắc
- nhận props rõ ràng
- không gọi API trực tiếp
- không biết domain business

---

## 16. Feature architecture chi tiết

## 16.1 Auth feature

### Thành phần
- LoginForm
- RegisterForm
- OAuthButtons

### State cần quản lý
- email
- password
- validation errors
- loading submit
- auth result

### Logic
- submit form
- lưu token
- gọi `refreshMe()` từ AuthContext
- redirect user sau login thành công

### Hook useAuth
Expose:
- `login`
- `register`
- `logout`
- `me`

---

## 16.2 Feed feature

### Thành phần
- FeedList
- FeedItem
- FeedSkeleton

### Nhiệm vụ
- load feed theo cursor
- render PostCard list
- append post khi scroll
- refresh khi user tạo post mới

### Hook useFeed
Quản lý:
- `items`
- `cursor`
- `hasMore`
- `loadingInitial`
- `loadingMore`
- `error`
- `fetchInitial()`
- `fetchMore()`
- `prependPost()` nếu vừa tạo post mới

---

## 16.3 Posts feature

### Thành phần chính
- PostCard
- PostHeader
- PostMedia
- PostActions
- PostCaption
- PostCommentPreview
- PostGridItem

### PostCard dùng ở đâu
- Home Feed
- Explore
- Profile grid preview mở modal
- Saved page

### Hook usePostActions
Quản lý các thao tác:
- like/unlike
- save/unsave
- delete post
- optimistic UI nhẹ bằng `useState`

### Hook usePostDetail
Dùng cho modal chi tiết post:
- lấy post detail
- load comments preview nếu cần
- update local counts

---

## 16.4 Comments feature

### Thành phần
- CommentList
- CommentItem
- CommentInput

### State
- comments array
- loading comments
- submitting comment
- deleting comment

### Hook useComments
Expose:
- `comments`
- `loading`
- `error`
- `addComment(content)`
- `deleteComment(commentId)`
- `loadMore()` nếu có pagination

### Realtime
Khi đang mở post modal:
- socket nhận comment mới
- append vào list
- cập nhật count

---

## 16.5 Profile feature

### Thành phần
- ProfileHeader
- ProfileTabs
- ProfileGrid
- EditProfileForm

### Trách nhiệm
- load profile header
- hiển thị counts
- tab Posts / Saved / Tagged
- grid responsive

### State
- activeTab
- profile data
- post grid data theo tab
- loading từng tab

### Hook useProfile
Quản lý:
- profile info
- update profile
- fetch tab content

---

## 16.6 Follow feature

### Logic chính
- follow/unfollow user
- update local follower count ngay trên UI
- đồng bộ button state ở profile và suggestion item

### Hook useFollowActions
Expose:
- `isFollowing`
- `loading`
- `follow()`
- `unfollow()`

---

## 16.7 Stories feature

### Thành phần
- StoriesBar
- StoryItem
- StoryViewer

### Nhiệm vụ
- hiển thị horizontal story list ở đầu Home
- mở viewer dạng modal/fullscreen
- mark viewed

### State
- stories list
- active story index
- viewed status
- auto advance timer

### Hook useStories
Expose:
- `stories`
- `loading`
- `openStory(userId/storyId)`
- `markViewed(storyId)`

---

## 16.8 Search feature

### Thành phần
- SearchInput
- SearchTabs
- SearchResults
- RecentSearchList

### Flow
- user gõ keyword
- debounce 300–500ms
- gọi API search
- hiển thị grouped results hoặc theo tab

### State
- keyword
- debouncedKeyword
- activeTab
- loading
- results
- recent searches

### Hook useSearch
Expose:
- `keyword`
- `setKeyword`
- `activeTab`
- `setActiveTab`
- `results`
- `loading`
- `clearRecent()`

---

## 16.9 Messages feature

### Thành phần
- ConversationList
- ConversationItem
- ChatRoom
- MessageList
- MessageInput

### Layout
Desktop:
- trái: conversation list
- phải: chat room

Mobile:
- màn list riêng
- click conversation sang màn chat

### State
- conversations
- selectedConversationId
- messages
- sending state
- unread counts

### Hook useConversations
- fetch danh sách conversation
- update preview khi có tin mới

### Hook useMessages
- fetch messages theo conversationId
- append tin nhắn mới
- send message
- mark read

---

## 16.10 Notifications feature

### Thành phần
- NotificationList
- NotificationItem

### State
- notifications list
- unread count
- loading

### Hook useNotifications
- fetch list
- prepend notification mới từ socket
- mark read / mark all read

---

## 16.11 Create Post feature

### Thành phần
- CreatePostModal
- UploadDropzone
- MediaPreview
- CaptionEditor

### State
- selected files
- preview urls
- caption
- upload progress
- submitting
- error

### Hook useCreatePost
Quản lý toàn bộ flow:
1. chọn file
2. preview
3. upload media
4. submit post
5. reset state sau khi thành công

---

## 17. Widgets layer

Widgets là các khối UI lớn được ghép từ nhiều feature.

## 17.1 LeftSidebar
Chứa:
- logo
- nav menu
- create post button
- profile shortcut

## 17.2 RightSidebar
Chứa:
- suggested users
- activity snippets
- shortcuts

## 17.3 FeedContainer
Chứa:
- StoriesBar
- FeedList

## 17.4 GlobalModalRenderer
Đọc state từ ModalContext và render đúng modal.

Ví dụ:
- nếu modal type là `postDetail` → render PostDetailModal
- nếu là `createPost` → render CreatePostRootModal

---

## 18. Page architecture

Page chỉ nên làm các việc sau:

- nhận route params
- load widget chính của trang
- nối context nếu cần
- rất ít business logic trực tiếp

## 18.1 HomePage
- render FeedContainer
- optionally render right sidebar content theo layout

## 18.2 ProfilePage
- lấy `username` từ route
- load profile
- render ProfileHeader + ProfileTabs + ProfileGrid

## 18.3 ExplorePage
- render masonry/grid content từ post discovery

## 18.4 MessagesPage
- render conversation list + chat room

## 18.5 LoginPage / RegisterPage
- dùng AuthLayout
- render form centered

---

## 19. Modal architecture

App này dùng nhiều modal, nên cần một hệ thống quản lý thống nhất.

## 19.1 ModalContext shape

```ts
{
  modalType: string | null,
  modalProps: Record<string, unknown> | null,
  openModal: (type, props?) => void,
  closeModal: () => void,
}
```

## 19.2 Modal types
- postDetail
- createPost
- storyViewer
- confirmDelete
- editProfile

## 19.3 Lợi ích
- mở modal từ bất kỳ đâu
- tránh prop drilling
- giữ behavior đồng nhất

---

## 20. Realtime architecture ở frontend

## 20.1 SocketProvider
Socket nên được khởi tạo sau khi user đăng nhập.

Nhiệm vụ:
- kết nối socket
- đăng ký listeners toàn cục
- cleanup khi logout

## 20.2 Những event quan trọng
- `notification:new`
- `message:new`
- `message:read`
- `post:liked`
- `comment:created`
- `story:viewed`

## 20.3 Cách cập nhật UI
Vì không dùng global state manager phức tạp, nên có 3 cách:

1. update local state trong page đang mở
2. đẩy event vào context callback
3. refetch dữ liệu liên quan khi cần

### Ví dụ
- nếu đang mở MessagesPage và có tin nhắn mới → append local state
- nếu đang ở page khác → chỉ update unread count global

---

## 21. Data fetching pattern không dùng React Query

Vì không dùng React Query, cần chuẩn hóa logic fetch.

## 21.1 Pattern fetch cho page dữ liệu

Mỗi custom hook nên có:

- `data`
- `loading`
- `error`
- `refetch`

## 21.2 Cleanup race conditions
Khi user đổi route nhanh hoặc keyword search thay đổi liên tục:
- dùng `AbortController`
- hoặc flag `isMounted`

## 21.3 Retry strategy
Có thể tự viết retry đơn giản cho:
- get feed
- get profile
- get messages

---

## 22. Pagination strategy ở frontend

## 22.1 Feed
Dùng **cursor pagination**.

State cần có:
- `items`
- `nextCursor`
- `hasMore`
- `loadingMore`

## 22.2 Profile grid
Cũng nên dùng cursor để load thêm posts

## 22.3 Comments
Có thể dùng:
- load 10–20 comments đầu
- nhấn “view more” để fetch tiếp

---

## 23. Form architecture không dùng thư viện state ngoài

## 23.1 Với form đơn giản
Dùng `useState`:
- login
- register
- comment input
- search input

## 23.2 Với form phức tạp hơn
Có thể tạo custom hook riêng.

Ví dụ `useEditProfileForm`:
- values
- errors
- touched
- handleChange
- validate
- handleSubmit

## 23.3 Validation
Tách validation vào `shared/lib/validators.ts`

Ví dụ:
- validate email
- validate password
- validate username
- validate caption length

---

## 24. TailwindCSS architecture

## 24.1 Cách tổ chức styling
- dùng Tailwind utility classes trực tiếp trong component
- tránh viết CSS riêng nhiều nếu không cần
- dùng `globals.css` cho reset, scrollbar, base styles

## 24.2 Design tokens nên thống nhất
Trong Tailwind config, define:
- colors
- border radius
- spacing scale
- max width cho layout
- breakpoint custom nếu cần

## 24.3 Helper cn()
Tạo helper gộp className:

```ts
export function cn(...classes: Array<string | undefined | false | null>) {
  return classes.filter(Boolean).join(' ');
}
```

## 24.4 Quy tắc styling
- component nhỏ: class inline
- component phức tạp: tách subcomponent thay vì class quá dài

---

## 25. Responsive architecture

## 25.1 Desktop
- full 3-column layout
- content centered
- modal không làm mất context

## 25.2 Tablet
- 2-column layout
- ẩn right sidebar hoặc rút gọn nội dung phụ
- nav trái có thể icon-only

## 25.3 Mobile
- single column
- feed-first
- modals có thể full-screen
- messages chuyển sang stacked navigation

## 25.4 Công cụ áp dụng
- Tailwind responsive classes: `sm:`, `md:`, `lg:`, `xl:`
- `useMediaQuery()` cho logic JS khi cần

---

## 26. Component tree theo từng màn hình

## 26.1 HomePage

```txt
HomePage
  FeedLayout
    StoriesBar
      StoryItem[]
    FeedList
      PostCard[]
        PostHeader
        PostMedia
        PostActions
        PostCaption
        PostCommentPreview
```

## 26.2 ProfilePage

```txt
ProfilePage
  ProfileHeader
  ProfileTabs
  ProfileGrid
    PostGridItem[]
```

## 26.3 MessagesPage

```txt
MessagesPage
  ConversationList
    ConversationItem[]
  ChatRoom
    MessageList
      MessageBubble[]
    MessageInput
```

## 26.4 SearchPage

```txt
SearchPage
  SearchInput
  RecentSearchList
  SearchTabs
  SearchResults
```

## 26.5 CreatePostModal

```txt
CreatePostModal
  UploadDropzone
  MediaPreview
  CaptionEditor
  PublishActions
```

---

## 27. Cách truyền dữ liệu giữa component

Vì không dùng global state manager lớn, nên nên theo quy tắc:

### Props khi dữ liệu chỉ đi 1–2 cấp
Ví dụ:
- PostCard → PostActions
- ProfilePage → ProfileTabs

### Context khi dữ liệu dùng toàn app hoặc nhiều tầng sâu
Ví dụ:
- auth
- modal
- socket

### Callback upward
Ví dụ:
- CommentInput submit xong báo parent append comment
- CreatePostModal publish xong báo HomePage prepend post

---

## 28. Error handling architecture

## 28.1 Cấp component/hook
Mỗi hook fetch cần có:
- loading
- error
- retry

## 28.2 Cấp page
Page nên render:
- loader nếu loading
- ErrorState nếu fail
- EmptyState nếu không có dữ liệu

## 28.3 API error normalization
Trong `http.ts`, nên chuẩn hóa response lỗi:
- message
- statusCode
- details nếu có

---

## 29. Loading state architecture

Nên có 3 loại loading:

### Page loading
- khi mở trang mới
- dùng skeleton page hoặc PageLoader

### Section loading
- load comments
- load stories
- load sidebar suggestions

### Action loading
- login button loading
- like button loading
- publish post loading

---

## 30. Optimistic UI không dùng thư viện ngoài

Vẫn có thể làm bằng `useState`.

### Ví dụ like post
1. user bấm like
2. update local `liked = true`, `likeCount + 1`
3. gọi API
4. nếu fail thì rollback

### Ví dụ add comment
1. append comment tạm thời vào list
2. gọi API
3. replace bằng server comment thật nếu thành công
4. rollback nếu fail

---

## 31. Authentication flow ở frontend

## 31.1 Login flow
1. user nhập email/password
2. submit lên backend
3. nhận access token + refresh token/cookie
4. lưu token nếu cần
5. gọi `/me`
6. set AuthContext
7. redirect về Home

## 31.2 OAuth flow
1. user bấm Google/Facebook
2. redirect tới OAuth provider hoặc popup
3. backend xử lý callback
4. frontend nhận phiên đăng nhập
5. gọi `/me`

## 31.3 Logout flow
1. gọi API logout
2. clear local auth info
3. disconnect socket
4. redirect `/login`

---

## 32. Caching tối thiểu ở frontend

Không dùng React Query vẫn có thể cache nhẹ bằng:

- in-memory state ở page còn mounted
- `sessionStorage` cho recent search hoặc draft đơn giản
- `localStorage` cho auth token hoặc preference

### Không nên lạm dụng localStorage cho dữ liệu feed/profile lớn
Vì dễ stale và khó đồng bộ.

---

## 33. Accessibility và UX principles

Nên đảm bảo:

- button có aria-label nếu chỉ có icon
- modal trap focus
- keyboard close modal bằng ESC
- form input có label rõ ràng
- hover/focus states rõ
- skeleton/loading không gây layout shift mạnh

---

## 34. Quy tắc viết code

### 34.1 Component nên nhỏ và đơn trách nhiệm
Không viết một file PostCard 500 dòng nếu có thể tách thành:
- PostHeader
- PostMedia
- PostActions
- PostCaption

### 34.2 Không nhét logic fetch trực tiếp vào page quá nhiều
Nên bọc trong custom hooks.

### 34.3 Không để business logic nằm trong shared components
Shared components phải generic.

### 34.4 Type rõ ràng
Mỗi feature nên có file type riêng:
- DTO từ backend
- UI model nếu cần mapping

---

## 35. Thiết kế types với TypeScript

## 35.1 Shared types
- API response
- paginated response
- common option type

## 35.2 Feature types
Ví dụ `post.types.ts`:
- PostAuthor
- PostMedia
- PostStats
- Post
- CreatePostPayload

## 35.3 Tách DTO và UI model nếu cần
Ví dụ backend trả field hơi khác UI cần, có thể map:
- `PostDto`
- `PostViewModel`

---

## 36. Gợi ý file responsibilities cụ thể

### `shared/api/http.ts`
- tạo request wrapper
- attach token
- parse response
- normalize error

### `contexts/AuthContext.tsx`
- currentUser
- login/logout/refreshMe

### `features/feed/hooks/useFeed.ts`
- fetch feed
- pagination
- refetch

### `widgets/modals/GlobalModalRenderer.tsx`
- switch modal type
- render modal tương ứng

### `features/messages/hooks/useMessages.ts`
- load messages
- append/send message
- integrate socket callback

---

## 37. Frontend data flow ví dụ

## 37.1 Home Feed load
1. HomePage mount
2. `useFeed()` gọi API
3. set `loading = true`
4. nhận data → set `items`
5. scroll xuống cuối → `fetchMore()`

## 37.2 Like post
1. click like tại PostActions
2. `usePostActions.likePost(postId)`
3. update local UI
4. gọi API
5. rollback nếu fail

## 37.3 Mở post detail modal
1. click vào PostCard hoặc comment preview
2. `openModal('postDetail', { postId })`
3. GlobalModalRenderer render PostDetailModal
4. `usePostDetail(postId)` fetch detail + comments

---

## 38. Kiến trúc phù hợp nhất cho project này

### Frontend recommendation cuối cùng
- **React + TypeScript + TailwindCSS**
- **React Router** cho navigation
- **Context API** cho auth, modal, socket
- **Custom hooks + useEffect + useState** cho fetch và state async
- **Feature-based folder structure**
- **Shared UI component library nội bộ**

Đây là kiến trúc phù hợp khi:
- không dùng React Query/Zustand
- vẫn muốn code rõ ràng
- vẫn đủ mạnh cho project Instagram clone có nhiều màn hình

---

## 39. Bản chốt ngắn gọn

### Global state nên có qua Context
- AuthContext
- ModalContext
- SocketContext

### Data state nên quản lý bằng custom hooks
- useFeed
- useProfile
- usePostDetail
- useComments
- useSearch
- useMessages
- useNotifications

### Shared components nên có
- Button
- Input
- Modal
- Avatar
- Tabs
- Spinner
- Skeleton
- ErrorState
- EmptyState

### Pages chính
- HomePage
- LoginPage
- RegisterPage
- ProfilePage
- ExplorePage
- SearchPage
- MessagesPage
- NotificationsPage
- SavedPage

---

## 40. Kết luận

Kiến trúc frontend này giúp ứng dụng đạt được các mục tiêu:

- bám sát UI Instagram clone dạng web
- dễ phát triển theo module
- không phụ thuộc thư viện state/server-state phức tạp
- phù hợp cho đồ án, dự án học tập, hoặc project thực hành nghiêm túc
- sẵn sàng mở rộng thêm realtime, stories, notifications, messages, explore

Nếu cần bước tiếp theo, có thể triển khai tiếp một trong các tài liệu sau:

1. **Component tree chi tiết cho từng page**
2. **Boilerplate folder structure với file mẫu**
3. **TypeScript interfaces đầy đủ cho frontend**
4. **Frontend coding conventions cho team**
5. **Sequence flow giữa frontend và backend cho từng chức năng**

