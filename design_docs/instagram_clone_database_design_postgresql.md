# Instagram Clone Database Design

## 1. Mục tiêu thiết kế database

Database được thiết kế cho backend Instagram Clone dùng:

- **PostgreSQL**
- backend **Java Spring Boot**
- media lưu trên **AWS S3**
- hỗ trợ feed, profile, stories, comments, likes, saved posts, messages, notifications

Nguyên tắc thiết kế:

- ưu tiên quan hệ rõ ràng
- tối ưu cho truy vấn feed/profile/post detail
- dùng bảng liên kết cho các quan hệ nhiều-nhiều
- cho phép mở rộng về sau
- lưu metadata media trong DB, không lưu binary trong DB

---

## 2. Tổng quan các nhóm bảng

### 2.1 Identity
- `users`
- `user_auth_providers`
- `user_refresh_tokens`

### 2.2 Social graph
- `follows`
- `follow_requests` (optional, cho private account)

### 2.3 Content
- `posts`
- `media_assets`
- `post_media`
- `comments`
- `post_likes`
- `comment_likes`
- `saved_posts`
- `hashtags`
- `post_hashtags`
- `tagged_posts`

### 2.4 Stories
- `stories`
- `story_views`

### 2.5 Messaging
- `conversations`
- `conversation_participants`
- `messages`

### 2.6 Activity / utility
- `notifications`
- `recent_searches`

---

## 3. Thiết kế bảng chi tiết

## 3.1 users

Lưu thông tin tài khoản chính.

```sql
create table users (
  id uuid primary key default gen_random_uuid(),
  username varchar(30) not null unique,
  full_name varchar(100),
  email varchar(255) unique,
  password_hash text,
  bio varchar(255),
  avatar_url text,
  website_url text,
  is_private boolean not null default false,
  is_verified boolean not null default false,
  status varchar(20) not null default 'active',
  post_count integer not null default 0,
  follower_count integer not null default 0,
  following_count integer not null default 0,
  is_username_set boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
```

### Ghi chú
- `email` có thể nullable nếu user chỉ dùng OAuth
- `post_count`, `follower_count`, `following_count` là denormalized counters

---

## 3.2 user_auth_providers

Liên kết tài khoản người dùng với nhà cung cấp OAuth.

```sql
create table user_auth_providers (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  provider varchar(20) not null,
  provider_user_id varchar(255) not null,
  created_at timestamptz not null default now(),
  unique (provider, provider_user_id)
);
```

### Ví dụ provider
- `facebook`

---

## 3.3 user_refresh_tokens

Lưu refresh token/session.

```sql
create table user_refresh_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  refresh_token_hash text not null,
  ip_address varchar(45),
  user_agent text,
  expires_at timestamptz not null,
  revoked_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
```

---

## 3.4 follows

Quan hệ follow giữa user với user.

```sql
create table follows (
  follower_id uuid not null references users(id) on delete cascade,
  following_id uuid not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, following_id),
  check (follower_id <> following_id)
);
```

---

## 3.5 follow_requests (optional)

Dùng khi hỗ trợ private account.

```sql
create table follow_requests (
  id uuid primary key default gen_random_uuid(),
  requester_id uuid not null references users(id) on delete cascade,
  target_user_id uuid not null references users(id) on delete cascade,
  status varchar(20) not null default 'pending',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (requester_id, target_user_id)
);
```

---

## 3.6 posts

Thông tin chính của bài viết.

```sql
create table posts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  caption text,
  location_name varchar(255),
  visibility varchar(20) not null default 'public',
  comments_enabled boolean not null default true,
  like_count integer not null default 0,
  comment_count integer not null default 0,
  save_count integer not null default 0,
  share_count integer not null default 0,
  media_count integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);
```

### Ghi chú
- `visibility`: `public`, `followers`, `private`
- `deleted_at` hỗ trợ soft delete

---

## 3.7 media_assets

Lưu metadata media trên S3.

```sql
create table media_assets (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid references users(id) on delete set null,
  media_type varchar(20) not null,
  storage_key text not null,
  url text not null,
  thumbnail_url text,
  width integer,
  height integer,
  duration_seconds integer,
  mime_type varchar(100),
  file_size bigint,
  processing_status varchar(20) not null default 'ready',
  created_at timestamptz not null default now()
);
```

### Ghi chú
- `storage_key` là object key trên S3
- `media_type`: `image`, `video`

---

## 3.8 post_media

Liên kết post với nhiều media.

```sql
create table post_media (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references posts(id) on delete cascade,
  media_asset_id uuid not null references media_assets(id) on delete cascade,
  sort_order integer not null default 0,
  unique (post_id, media_asset_id)
);
```

---

## 3.9 comments

Lưu comment và reply comment.

```sql
create table comments (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references posts(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  parent_comment_id uuid references comments(id) on delete cascade,
  content text not null,
  like_count integer not null default 0,
  reply_count integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);
```

### Ghi chú
- `parent_comment_id` null nếu là comment gốc
- có thể hỗ trợ thread reply đơn giản

---

## 3.10 post_likes

Lưu like trên post.

```sql
create table post_likes (
  user_id uuid not null references users(id) on delete cascade,
  post_id uuid not null references posts(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, post_id)
);
```

---

## 3.11 comment_likes

Lưu like trên comment.

```sql
create table comment_likes (
  user_id uuid not null references users(id) on delete cascade,
  comment_id uuid not null references comments(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, comment_id)
);
```

---

## 3.12 saved_posts

Lưu các bài viết user đã save.

```sql
create table saved_posts (
  user_id uuid not null references users(id) on delete cascade,
  post_id uuid not null references posts(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, post_id)
);
```

---

## 3.13 hashtags

Danh mục hashtag.

```sql
create table hashtags (
  id uuid primary key default gen_random_uuid(),
  name varchar(100) not null unique,
  post_count integer not null default 0,
  created_at timestamptz not null default now()
);
```

---

## 3.14 post_hashtags

Liên kết nhiều-nhiều giữa posts và hashtags.

```sql
create table post_hashtags (
  post_id uuid not null references posts(id) on delete cascade,
  hashtag_id uuid not null references hashtags(id) on delete cascade,
  primary key (post_id, hashtag_id)
);
```

---

## 3.15 tagged_posts

Lưu các user được tag trong post.

```sql
create table tagged_posts (
  post_id uuid not null references posts(id) on delete cascade,
  tagged_user_id uuid not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, tagged_user_id)
);
```

---

## 3.16 stories

Lưu story.

```sql
create table stories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  media_asset_id uuid not null references media_assets(id) on delete cascade,
  caption varchar(255),
  view_count integer not null default 0,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  deleted_at timestamptz
);
```

### Ghi chú
- story hết hạn sau 24 giờ
- không nhất thiết phải xóa vật lý ngay

---

## 3.17 story_views

Theo dõi ai đã xem story.

```sql
create table story_views (
  story_id uuid not null references stories(id) on delete cascade,
  viewer_user_id uuid not null references users(id) on delete cascade,
  viewed_at timestamptz not null default now(),
  primary key (story_id, viewer_user_id)
);
```

---

## 3.18 conversations

Lưu thông tin cuộc trò chuyện.

```sql
create table conversations (
  id uuid primary key default gen_random_uuid(),
  conversation_type varchar(20) not null default 'direct',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
```

### Ghi chú
- giai đoạn đầu có thể chỉ cần `direct`

---

## 3.19 conversation_participants

Danh sách user trong conversation.

```sql
create table conversation_participants (
  conversation_id uuid not null references conversations(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  joined_at timestamptz not null default now(),
  last_read_message_id uuid,
  primary key (conversation_id, user_id)
);
```

### Ghi chú
- `last_read_message_id` hỗ trợ unread count

---

## 3.20 messages

Tin nhắn trong conversation.

```sql
create table messages (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references conversations(id) on delete cascade,
  sender_user_id uuid not null references users(id) on delete cascade,
  message_type varchar(20) not null default 'text',
  content text,
  media_asset_id uuid references media_assets(id) on delete set null,
  created_at timestamptz not null default now(),
  deleted_at timestamptz
);
```

### Ghi chú
- `message_type`: `text`, `image`, `video`
- có thể soft delete message

---

## 3.21 notifications

Lưu notifications cho user.

```sql
create table notifications (
  id uuid primary key default gen_random_uuid(),
  recipient_user_id uuid not null references users(id) on delete cascade,
  actor_user_id uuid references users(id) on delete set null,
  type varchar(30) not null,
  entity_type varchar(30),
  entity_id bigint,
  is_read boolean not null default false,
  created_at timestamptz not null default now()
);
```

### Ví dụ type
- `follow_user`
- `like_post`
- `comment_post`
- `message_received`

---

## 3.22 recent_searches

Lưu lịch sử tìm kiếm.

```sql
create table recent_searches (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  search_type varchar(20) not null,
  keyword varchar(255),
  target_id uuid,
  created_at timestamptz not null default now()
);
```

### search_type
- `user`
- `hashtag`
- `place`
- `keyword`

---

## 4. Quan hệ chính

### User-related
- một `user` có nhiều `posts`
- một `user` có nhiều `stories`
- một `user` follow nhiều `users` khác qua `follows`
- một `user` like nhiều `posts`
- một `user` save nhiều `posts`
- một `user` tham gia nhiều `conversations`

### Post-related
- một `post` có nhiều `post_media`
- một `post` có nhiều `comments`
- một `post` có nhiều `post_likes`
- một `post` có nhiều `hashtags` qua `post_hashtags`
- một `post` có nhiều tagged users qua `tagged_posts`

### Messaging-related
- một `conversation` có nhiều `participants`
- một `conversation` có nhiều `messages`

---

## 5. Chỉ mục (indexes) quan trọng

## 5.1 users
```sql
create unique index idx_users_username on users(username);
create unique index idx_users_email on users(email);
```

## 5.2 follows
```sql
create index idx_follows_following_id on follows(following_id);
create index idx_follows_follower_id on follows(follower_id);
```

## 5.3 posts
```sql
create index idx_posts_user_id_created_at on posts(user_id, created_at desc);
create index idx_posts_created_at on posts(created_at desc);
create index idx_posts_deleted_at on posts(deleted_at);
```

## 5.4 comments
```sql
create index idx_comments_post_id_created_at on comments(post_id, created_at asc);
create index idx_comments_parent_comment_id on comments(parent_comment_id);
```

## 5.5 stories
```sql
create index idx_stories_user_id_expires_at on stories(user_id, expires_at desc);
create index idx_stories_expires_at on stories(expires_at);
```

## 5.6 messages
```sql
create index idx_messages_conversation_id_created_at on messages(conversation_id, created_at desc);
```

## 5.7 notifications
```sql
create index idx_notifications_recipient_is_read_created
on notifications(recipient_user_id, is_read, created_at desc);
```

## 5.8 saved_posts
```sql
create index idx_saved_posts_post_id on saved_posts(post_id);
```

---

## 6. Tối ưu search trong PostgreSQL

Nếu muốn search user/hashtag/caption tốt hơn, nên bật `pg_trgm`.

```sql
create extension if not exists pg_trgm;
```

Indexes gợi ý:

```sql
create index idx_users_username_trgm on users using gin (username gin_trgm_ops);
create index idx_users_full_name_trgm on users using gin (full_name gin_trgm_ops);
create index idx_hashtags_name_trgm on hashtags using gin (name gin_trgm_ops);
create index idx_posts_caption_trgm on posts using gin (caption gin_trgm_ops);
```

---

## 7. Chiến lược counter

Để đọc nhanh, nên lưu counter dạng denormalized:

### 7.1 Trong `users`
- `post_count`
- `follower_count`
- `following_count`

### 7.2 Trong `posts`
- `like_count`
- `comment_count`
- `save_count`

### 7.3 Trong `stories`
- `view_count`

### Cách cập nhật
- cập nhật trong transaction nếu đơn giản
- hoặc tách sang `@Async` nếu muốn giảm thời gian response

---

## 8. Soft delete

Nên dùng soft delete cho:
- `posts`
- `comments`
- `messages`
- `stories` (tùy nhu cầu)

Lý do:
- hỗ trợ audit đơn giản
- tránh mất dữ liệu ngay
- dễ rollback ở giai đoạn phát triển

---

## 9. Gợi ý mapping với JPA

### Ví dụ entity relationships
- `User` 1-n `Post`
- `Post` 1-n `PostMedia`
- `Post` 1-n `Comment`
- `Conversation` 1-n `Message`

### Lưu ý
- không nên lạm dụng `EAGER`
- ưu tiên `LAZY`
- dùng DTO projection cho feed/profile list để tránh N+1 query

---

## 10. Thứ tự tạo bảng hợp lý

1. `users`
2. `user_auth_providers`
3. `user_refresh_tokens`
4. `follows`
5. `posts`
6. `media_assets`
7. `post_media`
8. `comments`
9. `post_likes`
10. `comment_likes`
11. `saved_posts`
12. `hashtags`
13. `post_hashtags`
14. `tagged_posts`
15. `stories`
16. `story_views`
17. `conversations`
18. `conversation_participants`
19. `messages`
20. `notifications`
21. `recent_searches`

---

## 11. Kết luận

Thiết kế database này phù hợp với backend:

- **Java Spring Boot**
- **PostgreSQL**
- **AWS S3**
- **Spring WebSocket**
- **Async nội bộ bằng Java**

Ưu điểm:
- mô hình quan hệ rõ ràng
- support tốt cho feed/profile/post detail/messages
- dễ mapping với JPA
- dễ mở rộng thêm private account, tagged content, explore ranking

Nếu triển khai thực tế, nên tiếp tục bước sau:
- viết migration bằng Flyway hoặc Liquibase
- viết entity + repository tương ứng
- viết query tối ưu cho feed và profile grid
