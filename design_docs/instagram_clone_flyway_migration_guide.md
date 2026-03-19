# Hướng dẫn viết migration Flyway cho backend Spring Boot

## Mục tiêu

Tài liệu này hướng dẫn cách tổ chức và viết migration Flyway cho backend Spring Boot của project Instagram clone theo hướng **migration-first**:

- Database schema là **source of truth**
- Backend entity chỉ dùng để **map** vào schema đã được migrate
- Không dùng Hibernate để tự động sinh/chỉnh sửa schema trong production

---

## 1. Nguyên tắc tổng thể

Trong project này, cách làm nên là:

1. Thiết kế schema trước ở mức bảng, khóa, index, ràng buộc
2. Viết migration Flyway để tạo và thay đổi schema
3. Viết JPA Entity/Repository/Service map theo schema đó
4. Để Hibernate ở chế độ `validate` hoặc `none`, không để `update/create`

### Vì sao chọn migration-first?

Vì database của hệ thống không chỉ là nơi lưu dữ liệu, mà còn liên quan đến:

- tính đúng đắn của ràng buộc dữ liệu
- hiệu năng truy vấn
- index
- lịch sử thay đổi schema
- kiểm soát deploy giữa nhiều môi trường
- khả năng review và trace thay đổi

---

## 2. Dependency cần có

### Maven

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>

<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

---

## 3. Cấu hình Spring Boot

### `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/instagram_clone
    username: postgres
    password: postgres

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
    open-in-view: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Ý nghĩa các cấu hình quan trọng

- `ddl-auto: validate`
  - Hibernate chỉ kiểm tra schema có khớp entity không
  - Không tự tạo bảng
  - Không tự sửa cột

- `flyway.enabled: true`
  - Bật Flyway khi ứng dụng khởi động

- `locations: classpath:db/migration`
  - Thư mục chứa migration SQL

- `baseline-on-migrate: true`
  - Hữu ích khi tích hợp Flyway vào DB đã tồn tại trước đó
  - Với project mới hoàn toàn vẫn có thể giữ để an toàn

---

## 4. Cấu trúc thư mục nên dùng

```text
src/main/resources/
  db/
    migration/
      V1__enable_extensions.sql
      V2__create_identity_tables.sql
      V3__create_social_tables.sql
      V4__create_content_tables.sql
      V5__create_story_tables.sql
      V6__create_messaging_tables.sql
      V7__create_activity_tables.sql
      V8__create_indexes.sql
```

### Vì sao chia theo domain?

Với backend Spring Boot chia module theo domain như user, post, story, message, notification, thì migration cũng nên chia tương ứng để:

- dễ review
- dễ debug lỗi foreign key
- dễ mở rộng feature mới
- dễ giải thích khi phỏng vấn hoặc bảo vệ đồ án

---

## 5. Quy tắc đặt tên file migration

Flyway dùng chuẩn:

```text
V1__init.sql
V2__create_users.sql
V3__create_posts.sql
V4__add_indexes.sql
```

### Quy tắc

- Bắt đầu bằng `V`
- Theo sau là số version tăng dần
- Sau đó là `__`
- Cuối cùng là mô tả ngắn, rõ nghĩa

### Ví dụ tốt

```text
V1__enable_extensions.sql
V2__create_users_and_auth.sql
V3__create_follows.sql
V4__create_posts_and_media.sql
V5__create_comments_and_likes.sql
```

### Không nên

```text
V1__test.sql
V2__fix.sql
V3__new_update.sql
```

Tên file nên cho thấy **mục đích thay đổi schema**, không đặt mơ hồ.

---

## 6. Thứ tự migration hợp lý cho project này

Do có foreign key phụ thuộc qua lại giữa nhiều bảng, nên cần tạo theo thứ tự dependency.

### Đề xuất thứ tự

1. `V1__enable_extensions.sql`
2. `V2__create_identity_tables.sql`
3. `V3__create_social_tables.sql`
4. `V4__create_content_tables.sql`
5. `V5__create_story_tables.sql`
6. `V6__create_messaging_tables.sql`
7. `V7__create_activity_tables.sql`
8. `V8__create_indexes.sql`

### Giải thích

- **Identity** trước: `users`, `user_sessions`, `user_auth_providers`
- **Social** sau: `follows` phụ thuộc `users`
- **Content** tiếp theo: `posts`, `media_assets`, `comments`, `hashtags`, `likes`, `saved_posts`
- **Story** phụ thuộc `users` và `media_assets`
- **Messaging** phụ thuộc `users`, `media_assets`
- **Activity/notification/search** phụ thuộc nhiều entity đã có
- **Indexes** nên để riêng để migration create table dễ đọc hơn

---

## 7. Mẫu migration cho project

## 7.1. `V1__enable_extensions.sql`

```sql
create extension if not exists pg_trgm;
```

### Dùng khi nào?

Dùng cho fuzzy search:

- username
- full name
- hashtag
- caption

---

## 7.2. `V2__create_identity_tables.sql`

```sql
create table users (
  id bigserial primary key,
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
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table user_auth_providers (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  provider varchar(20) not null,
  provider_user_id varchar(255) not null,
  created_at timestamptz not null default now(),
  unique (provider, provider_user_id)
);

create table user_sessions (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  refresh_token_hash text not null,
  user_agent text,
  ip_address inet,
  expires_at timestamptz not null,
  revoked_at timestamptz,
  created_at timestamptz not null default now()
);
```

---

## 7.3. `V3__create_social_tables.sql`

```sql
create table follows (
  id bigserial primary key,
  follower_id bigint not null references users(id) on delete cascade,
  following_id bigint not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint chk_follows_not_self check (follower_id <> following_id),
  unique (follower_id, following_id)
);
```

---

## 7.4. `V4__create_content_tables.sql`

```sql
create table posts (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  caption text,
  like_count integer not null default 0,
  comment_count integer not null default 0,
  save_count integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table media_assets (
  id bigserial primary key,
  owner_user_id bigint not null references users(id) on delete cascade,
  media_type varchar(20) not null,
  storage_key text not null,
  url text not null,
  mime_type varchar(100),
  size_bytes bigint,
  width integer,
  height integer,
  duration_seconds integer,
  created_at timestamptz not null default now()
);

create table post_media (
  id bigserial primary key,
  post_id bigint not null references posts(id) on delete cascade,
  media_asset_id bigint not null references media_assets(id) on delete cascade,
  sort_order integer not null default 0,
  unique (post_id, media_asset_id)
);

create table hashtags (
  id bigserial primary key,
  name varchar(100) not null unique,
  created_at timestamptz not null default now()
);

create table post_hashtags (
  post_id bigint not null references posts(id) on delete cascade,
  hashtag_id bigint not null references hashtags(id) on delete cascade,
  primary key (post_id, hashtag_id)
);

create table tagged_posts (
  id bigserial primary key,
  post_id bigint not null references posts(id) on delete cascade,
  tagged_user_id bigint not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (post_id, tagged_user_id)
);

create table comments (
  id bigserial primary key,
  post_id bigint not null references posts(id) on delete cascade,
  user_id bigint not null references users(id) on delete cascade,
  parent_comment_id bigint references comments(id) on delete cascade,
  content varchar(1000) not null,
  like_count integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table post_likes (
  id bigserial primary key,
  post_id bigint not null references posts(id) on delete cascade,
  user_id bigint not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (post_id, user_id)
);

create table comment_likes (
  id bigserial primary key,
  comment_id bigint not null references comments(id) on delete cascade,
  user_id bigint not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (comment_id, user_id)
);

create table saved_posts (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  post_id bigint not null references posts(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (user_id, post_id)
);
```

---

## 7.5. `V5__create_story_tables.sql`

```sql
create table stories (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  media_asset_id bigint not null references media_assets(id) on delete cascade,
  caption varchar(255),
  view_count integer not null default 0,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  deleted_at timestamptz
);

create table story_views (
  id bigserial primary key,
  story_id bigint not null references stories(id) on delete cascade,
  viewer_user_id bigint not null references users(id) on delete cascade,
  viewed_at timestamptz not null default now(),
  unique (story_id, viewer_user_id)
);
```

---

## 7.6. `V6__create_messaging_tables.sql`

```sql
create table conversations (
  id bigserial primary key,
  conversation_type varchar(20) not null default 'direct',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table messages (
  id bigserial primary key,
  conversation_id bigint not null references conversations(id) on delete cascade,
  sender_user_id bigint not null references users(id) on delete cascade,
  message_type varchar(20) not null default 'text',
  content text,
  media_asset_id bigint references media_assets(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table conversation_participants (
  id bigserial primary key,
  conversation_id bigint not null references conversations(id) on delete cascade,
  user_id bigint not null references users(id) on delete cascade,
  last_read_message_id bigint,
  joined_at timestamptz not null default now(),
  unique (conversation_id, user_id)
);

alter table conversation_participants
  add constraint fk_conversation_participants_last_read_message
  foreign key (last_read_message_id) references messages(id) on delete set null;
```

### Lưu ý

Ở đây foreign key `last_read_message_id` được thêm sau bằng `alter table` để tránh lỗi dependency khi tạo bảng.

---

## 7.7. `V7__create_activity_tables.sql`

```sql
create table notifications (
  id bigserial primary key,
  recipient_user_id bigint not null references users(id) on delete cascade,
  actor_user_id bigint references users(id) on delete set null,
  type varchar(30) not null,
  entity_type varchar(30),
  entity_id bigint,
  is_read boolean not null default false,
  created_at timestamptz not null default now()
);

create table recent_searches (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  target_type varchar(20) not null,
  target_id bigint not null,
  created_at timestamptz not null default now()
);
```

---

## 7.8. `V8__create_indexes.sql`

```sql
create index idx_follows_following_id on follows(following_id);
create index idx_follows_follower_id on follows(follower_id);

create index idx_posts_user_id_created_at on posts(user_id, created_at desc);
create index idx_posts_created_at on posts(created_at desc);
create index idx_posts_deleted_at on posts(deleted_at);

create index idx_comments_post_id_created_at on comments(post_id, created_at asc);
create index idx_comments_parent_comment_id on comments(parent_comment_id);

create index idx_stories_user_id_expires_at on stories(user_id, expires_at desc);
create index idx_stories_expires_at on stories(expires_at);

create index idx_messages_conversation_id_created_at
  on messages(conversation_id, created_at desc);

create index idx_notifications_recipient_is_read_created
  on notifications(recipient_user_id, is_read, created_at desc);

create index idx_saved_posts_post_id on saved_posts(post_id);

create index idx_users_username_trgm on users using gin (username gin_trgm_ops);
create index idx_users_full_name_trgm on users using gin (full_name gin_trgm_ops);
create index idx_hashtags_name_trgm on hashtags using gin (name gin_trgm_ops);
create index idx_posts_caption_trgm on posts using gin (caption gin_trgm_ops);
```

---

## 8. Cách thêm migration mới khi có thay đổi schema

### Ví dụ

Giả sử cần thêm bảng `follow_requests` cho private account:

```text
V9__create_follow_requests.sql
```

```sql
create table follow_requests (
  id bigserial primary key,
  requester_id bigint not null references users(id) on delete cascade,
  target_user_id bigint not null references users(id) on delete cascade,
  status varchar(20) not null default 'pending',
  created_at timestamptz not null default now(),
  responded_at timestamptz,
  unique (requester_id, target_user_id)
);
```

### Quy tắc rất quan trọng

**Không sửa file migration cũ đã chạy trên môi trường dùng chung.**

Thay vào đó:

- cần thêm cột → tạo migration mới
- cần thêm bảng → tạo migration mới
- cần thêm index → tạo migration mới
- cần sửa dữ liệu → tạo migration mới

---

## 9. Quy ước viết SQL migration

## 9.1. Nên dùng lowercase thống nhất

```sql
create table users (
  id bigserial primary key
);
```

Điều này giúp SQL nhất quán, dễ đọc, dễ review.

## 9.2. Luôn khai báo `not null` khi hợp lý

Không nên để cột nullable nếu nghiệp vụ bắt buộc phải có dữ liệu.

Ví dụ:

```sql
username varchar(30) not null unique
```

## 9.3. Đặt tên constraint có chủ đích khi cần

Ví dụ:

```sql
constraint chk_follows_not_self check (follower_id <> following_id)
```

Đặt tên giúp debug lỗi ở production dễ hơn.

## 9.4. Tách index sang file riêng khi schema lớn

Tạo bảng và tạo index là hai mục đích khác nhau. Tách riêng giúp:

- file create table gọn hơn
- review dễ hơn
- tối ưu index về sau không làm nhiễu phần schema chính

## 9.5. Với foreign key vòng phụ thuộc, dùng `alter table`

Khi hai bảng phụ thuộc nhau, tạo bảng trước rồi mới thêm FK sau.

---

## 10. Cách chạy Flyway

Khi app Spring Boot khởi động, Flyway sẽ tự chạy migration nếu được bật.

Ngoài ra có thể chạy qua Maven plugin nếu project cấu hình thêm plugin Flyway.

Trong hầu hết trường hợp của project này, chỉ cần:

1. Tạo DB rỗng
2. Cấu hình datasource
3. Chạy application
4. Flyway tự apply các file `V...sql`

---

## 11. Kết hợp Flyway với JPA Entity như thế nào?

### Nguyên tắc

- Flyway tạo schema
- Entity map theo schema
- Hibernate chỉ validate

### Ví dụ Entity `User`

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(unique = true)
    private String email;
}
```

### Lưu ý

Entity không nên được xem là nơi quyết định schema chính thức. Entity chỉ phản ánh schema đã được thiết kế và migrate.

---

## 12. Quy trình làm việc đề xuất cho team/backend developer

Khi thêm một feature mới:

1. Phân tích thay đổi nghiệp vụ
2. Xác định thay đổi schema cần thiết
3. Viết migration mới
4. Review migration
5. Cập nhật entity/repository/service
6. Test local trên DB sạch
7. Merge

### Ví dụ với feature mới

Feature: lưu lịch sử báo cáo bài viết

Quy trình:

1. Thêm bảng `post_reports`
2. Tạo migration `V10__create_post_reports.sql`
3. Viết entity `PostReport`
4. Viết repository/service tương ứng

---

## 13. Những lỗi thường gặp

## 13.1. Dùng `ddl-auto=update` trong production

Đây không phải best practice vì:

- khó kiểm soát thay đổi
- khó review
- có thể làm schema drift giữa môi trường
- nguy hiểm khi đổi cấu trúc bảng lớn

## 13.2. Nhét tất cả schema vào một file rất lớn

Dẫn đến:

- khó review
- khó debug
- khó xác định migration nào gây lỗi

## 13.3. Sửa migration cũ đã chạy

Điều này làm checksum thay đổi và có thể phá môi trường khác.

## 13.4. Quên index cho bảng truy vấn nhiều

Ví dụ các bảng như:

- follows
- posts
- comments
- messages
- notifications

nếu thiếu index sẽ xuống hiệu năng rất nhanh.

## 13.5. Không nghĩ đến soft delete ngay từ đầu

Các bảng như `posts`, `comments`, `stories`, `messages` thường nên có `deleted_at` nếu nghiệp vụ cần ẩn dữ liệu thay vì xóa cứng.

---

## 14. Mẫu checklist trước khi merge migration

- Migration có tên rõ ràng chưa?
- Thứ tự version có đúng chưa?
- Có foreign key nào gây vòng dependency không?
- Cột bắt buộc đã `not null` chưa?
- Constraint unique/check đã đủ chưa?
- Có index cho query chính chưa?
- Có tách phần index riêng nếu file quá lớn không?
- Entity đã map đúng theo schema chưa?
- Local migrate trên DB sạch có pass không?
- Hibernate `validate` có pass không?

---

## 15. Kết luận

Với backend Spring Boot của project Instagram clone, cách làm đúng và dễ defend nhất là:

- Dùng **Flyway** để quản lý schema
- Chia migration theo **domain + dependency order**
- Dùng **PostgreSQL extension/index** đúng nhu cầu query
- Để **Hibernate chỉ validate** thay vì auto-generate
- Xem migration là **source of truth** của database

Câu chốt có thể dùng khi phỏng vấn:

> Database schema của em được quản lý bằng Flyway migration. Hibernate chỉ dùng để validate entity mapping, không dùng để tự động sinh hoặc thay đổi schema trong production.

