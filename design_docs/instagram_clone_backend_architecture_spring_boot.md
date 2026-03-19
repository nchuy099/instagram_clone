# Instagram Clone Backend Architecture

## 1. Mục tiêu

Tài liệu này mô tả thiết kế backend cho Instagram Clone tương ứng với frontend web đã có, với công nghệ chính:

- **Java Spring Boot**
- **PostgreSQL**
- **AWS S3**
- **WebSocket thuần (Spring WebSocket / STOMP hoặc raw WebSocket), không dùng Socket.IO**
- **Không dùng Redis**
- **Không dùng message queue**
- **Xử lý bất đồng bộ bằng Java `@Async` / `CompletableFuture` khi cần**

Mục tiêu thiết kế:

- Phù hợp với web UI dạng 3 cột: sidebar trái, feed giữa, utilities bên phải
- Hỗ trợ tốt cho các feature: auth, feed, profile, post detail modal, create post, stories, search, messages, notifications
- Dễ phát triển theo hướng đồ án hoặc production-lite
- Kiến trúc sạch, chia theo domain rõ ràng

---

## 2. Phạm vi chức năng backend

Backend phục vụ các nhóm chức năng sau:

1. **Authentication**
   - Đăng ký
   - Đăng nhập email/password
   - OAuth2 login (Google/Facebook)
   - Đăng xuất
   - Lấy thông tin người dùng hiện tại

2. **User / Profile**
   - Xem profile
   - Cập nhật profile
   - Follow / unfollow
   - Danh sách followers / following

3. **Posts**
   - Tạo bài viết
   - Xem chi tiết bài viết
   - Sửa / xóa bài viết
   - Like / unlike
   - Save / unsave
   - Comment / reply comment

4. **Feed / Explore**
   - Feed từ người đang follow
   - Explore posts
   - Stories feed

5. **Search**
   - Search users
   - Search hashtags
   - Search posts
   - Recent searches

6. **Messages**
   - Danh sách cuộc trò chuyện
   - Gửi / nhận tin nhắn realtime
   - Đánh dấu đã đọc

7. **Notifications**
   - Follow notification
   - Like / comment notification
   - Message notification
   - Mark as read

---

## 3. Kiến trúc tổng thể

### 3.1 Phong cách kiến trúc

Nên chọn **Modular Monolith**.

Tức là:
- chỉ có **một ứng dụng Spring Boot**
- chia module theo domain rõ ràng
- mọi module dùng chung database PostgreSQL
- có thể mở rộng sau này nếu cần tách service

Lý do phù hợp:
- đơn giản hơn microservices
- triển khai nhanh hơn
- dễ debug
- phù hợp với scope Instagram clone
- vẫn đủ sạch để scale vừa phải

---

## 4. Công nghệ chính

### 4.1 Backend framework
- **Spring Boot**
- **Spring Web MVC** cho REST API
- **Spring Security** cho auth + authorization
- **Spring Data JPA** hoặc kết hợp JPA + custom query
- **Spring WebSocket** cho realtime
- **Spring Validation** cho request validation

### 4.2 Database
- **PostgreSQL**

### 4.3 Lưu trữ media
- **AWS S3**

### 4.4 Async processing
- **`@Async` + `CompletableFuture`**

Dùng cho:
- gửi notification không đồng bộ
- xử lý upload metadata
- cleanup stories hết hạn
- cập nhật một số denormalized counters nếu muốn tách khỏi request chính

---

## 5. Các thành phần hệ thống

### 5.1 REST API Server
Phục vụ:
- authentication
- profile
- posts
- comments
- feed
- search
- messages
- notifications

### 5.2 WebSocket Gateway
Phục vụ realtime cho:
- messages
- notifications
- cập nhật comment/like khi đang mở post detail modal

### 5.3 PostgreSQL
Lưu dữ liệu chính:
- users
- follows
- posts
- post_media
- comments
- likes
- saves
- stories
- messages
- notifications

### 5.4 AWS S3
Lưu:
- avatar
- ảnh / video của post
- story media
- media đính kèm trong chat

### 5.5 Async executor
Dùng Java async để xử lý tác vụ phụ:
- tạo notification sau khi like/comment/follow
- background cleanup stories
- xử lý thumbnail metadata nếu có pipeline đơn giản

---

## 6. Cấu trúc module backend

Đề xuất package structure:

```text
src/main/java/com/example/instagram/
  common/
    config/
    exception/
    security/
    util/
    response/
  auth/
    controller/
    service/
    dto/
    entity/
    repository/
  user/
    controller/
    service/
    dto/
    entity/
    repository/
  follow/
    controller/
    service/
    entity/
    repository/
  post/
    controller/
    service/
    dto/
    entity/
    repository/
  comment/
    controller/
    service/
    dto/
    entity/
    repository/
  story/
    controller/
    service/
    dto/
    entity/
    repository/
  feed/
    controller/
    service/
    dto/
  search/
    controller/
    service/
    dto/
  message/
    controller/
    service/
    dto/
    entity/
    repository/
  notification/
    controller/
    service/
    dto/
    entity/
    repository/
  media/
    controller/
    service/
    dto/
  websocket/
    config/
    handler/
    dto/
```

---

## 7. Kiến trúc lớp trong mỗi module

Mỗi module nên có các lớp sau:

### 7.1 Controller layer
- expose REST API
- nhận request
- validate DTO
- trả response chuẩn

### 7.2 Service layer
- xử lý business logic
- điều phối repository
- gọi async task khi cần
- kiểm tra quyền truy cập

### 7.3 Repository layer
- truy vấn dữ liệu PostgreSQL
- dùng JPA repository hoặc custom query

### 7.4 DTO layer
- request DTO
- response DTO
- mapper DTO/entity

### 7.5 Entity layer
- mô hình hóa bảng dữ liệu

---

## 8. Thiết kế bảo mật

### 8.1 Authentication strategy

Dùng:
- **JWT access token**
- **refresh token** lưu trong database

Flow:
1. User login thành công
2. Backend cấp access token ngắn hạn
3. Backend cấp refresh token dài hạn
4. Refresh token được hash và lưu trong bảng session/token

### 8.2 Spring Security
Dùng để:
- xác thực request
- phân quyền endpoint
- inject current user vào business logic

### 8.3 OAuth2
Dùng Spring Security OAuth2 Client hoặc flow custom để hỗ trợ:
- Facebook login

---

## 9. Thiết kế API response chuẩn

Nên chuẩn hóa response:

```json
{
  "success": true,
  "data": {},
  "message": "Success"
}
```

Lỗi:

```json
{
  "success": false,
  "message": "Error",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Caption is required",
  }
}
```

Điều này giúp frontend dễ xử lý bằng custom hooks.

---

## 10. Thiết kế phân trang

### 10.1 Nên dùng cursor pagination
Áp dụng cho:
- feed
- profile posts
- comments
- notifications
- messages

Ví dụ response:

```json
{
  "success": true,
  "data": {
    "items": [],
    "nextCursor": "2026-03-18T10:00:00Z_101",
    "hasMore": true
  }
}
```

Không nên dùng offset cho feed lớn.

---

## 11. Thiết kế các module chính

### 11.1 Auth module

Chức năng:
- register
- login
- refresh token
- logout
- get current user
- OAuth2 callback/login

API:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/oauth/google`
- `POST /api/auth/oauth/facebook`

---

### 11.2 User/Profile module

Chức năng:
- xem profile user
- cập nhật profile
- lấy danh sách posts của user
- lấy saved posts
- lấy tagged posts

API:
- `GET /api/users/{username}`
- `PATCH /api/me/profile`
- `GET /api/users/{username}/posts`
- `GET /api/me/saved-posts`
- `GET /api/users/{username}/tagged-posts`

---

### 11.3 Follow module

Chức năng:
- follow / unfollow
- danh sách followers / following
- xử lý private account nếu thêm sau

API:
- `POST /api/users/{userId}/follow`
- `DELETE /api/users/{userId}/follow`
- `GET /api/users/{userId}/followers`
- `GET /api/users/{userId}/following`

---

### 11.4 Post module

Chức năng:
- tạo bài viết
- xem chi tiết bài viết
- sửa / xóa bài viết
- like / unlike
- save / unsave

API:
- `POST /api/posts`
- `GET /api/posts/{postId}`
- `PATCH /api/posts/{postId}`
- `DELETE /api/posts/{postId}`
- `POST /api/posts/{postId}/like`
- `DELETE /api/posts/{postId}/like`
- `POST /api/posts/{postId}/save`
- `DELETE /api/posts/{postId}/save`

---

### 11.5 Comment module

Chức năng:
- lấy comments của bài viết
- tạo comment
- reply comment
- xóa comment

API:
- `GET /api/posts/{postId}/comments`
- `POST /api/posts/{postId}/comments`
- `DELETE /api/comments/{commentId}`

---

### 11.6 Feed module

Chức năng:
- feed từ followed users
- explore feed đơn giản
- stories feed

API:
- `GET /api/feed`
- `GET /api/explore`
- `GET /api/stories/feed`

#### Feed strategy cho MVP
Dùng **fan-out on read**:
- khi user mở feed, query danh sách người đang follow
- lấy posts gần nhất từ họ
- join author, media, viewer state
- sort giảm dần theo thời gian

Đây là phương án đơn giản nhất khi chưa dùng Redis hay queue.

---

### 11.7 Story module

Chức năng:
- tạo story
- lấy stories còn hiệu lực
- mark viewed

API:
- `POST /api/stories`
- `GET /api/stories/feed`
- `POST /api/stories/{storyId}/view`

Story có hiệu lực 24 giờ.

Cleanup story hết hạn có thể chạy bằng:
- `@Scheduled`
- kết hợp update trạng thái hoặc lọc bằng `expires_at > now()`

---

### 11.8 Search module

Chức năng:
- search users
- search hashtags
- search posts
- recent searches

API:
- `GET /api/search?q=...`
- `GET /api/search/users?q=...`
- `GET /api/search/hashtags?q=...`
- `GET /api/search/posts?q=...`
- `GET /api/search/recent`
- `DELETE /api/search/recent/{id}`

Triển khai ban đầu bằng PostgreSQL:
- `ILIKE`
- trigram index
- full-text search khi cần

---

### 11.9 Message module

Chức năng:
- conversation list
- message history
- send message
- mark read

API:
- `GET /api/conversations`
- `POST /api/conversations`
- `GET /api/conversations/{id}/messages`
- `POST /api/conversations/{id}/messages`
- `POST /api/conversations/{id}/read`

Message realtime sẽ đi qua WebSocket.

---

### 11.10 Notification module

Chức năng:
- lấy notifications
- mark read
- mark all read

API:
- `GET /api/notifications`
- `PATCH /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`

Notification có thể được tạo async bằng `@Async` sau các hành động:
- follow
- like
- comment
- message mới

---

### 11.11 Media module

Chức năng:
- upload avatar/post/story media qua AWS S3
- tạo metadata media

API gợi ý:
- `POST /api/media/presign`
- `POST /api/media/complete`

Flow:
1. Frontend gọi backend để lấy presigned URL
2. Frontend upload trực tiếp file lên S3
3. Frontend gọi complete endpoint để lưu metadata vào database
4. Khi publish post/story, backend dùng `mediaAssetId`

---

## 12. Thiết kế WebSocket

### 12.1 Công nghệ
Dùng:
- **Spring WebSocket**
- có thể dùng **STOMP over WebSocket** để đơn giản hóa subscribe/publish
- hoặc raw WebSocket nếu muốn kiểm soát chi tiết hơn

Khuyến nghị cho project này:
- **Spring WebSocket + STOMP**

### 12.2 Use cases realtime

1. Tin nhắn mới
2. Notification mới
3. Cập nhật comment trong post detail modal
4. Cập nhật like count khi đang mở post detail

### 12.3 Các kênh subscribe gợi ý
- `/topic/conversations/{conversationId}`
- `/topic/posts/{postId}`
- `/user/queue/notifications`
- `/user/queue/messages`

### 12.4 Event examples
- `MESSAGE_CREATED`
- `MESSAGE_READ`
- `NOTIFICATION_CREATED`
- `COMMENT_CREATED`
- `POST_LIKE_UPDATED`

### 12.5 Auth cho WebSocket
- dùng JWT khi connect WebSocket
- validate token trong handshake interceptor
- gắn principal vào session websocket

---

## 13. Xử lý async bằng Java

Chưa dùng Redis hay queue thì vẫn có thể xử lý bất đồng bộ nội bộ bằng Java.

### 13.1 Dùng `@EnableAsync`

Tạo executor riêng:
- `notificationExecutor`
- `mediaExecutor`

### 13.2 Use cases phù hợp
- tạo notification sau like/comment/follow
- gửi websocket event sau khi commit dữ liệu
- background cleanup stories
- xử lý các tác vụ non-blocking không quá nặng

### 13.3 Lưu ý
- `@Async` không thay thế queue khi cần độ bền cao
- chỉ phù hợp cho scope hiện tại
- nếu sau này cần scale lớn thì mới bổ sung message broker

---

## 14. AWS S3 integration

### 14.1 Dùng S3 cho
- avatar
- post image/video
- story media
- chat attachments

### 14.2 Nên lưu trong DB
- object key
- public URL hoặc signed URL strategy
- mime type
- size
- width/height
- duration nếu là video

### 14.3 Upload flow
1. Frontend yêu cầu presigned URL
2. Backend trả URL + object key
3. Frontend upload lên S3
4. Backend lưu metadata

### 14.4 Ưu điểm
- giảm tải backend
- upload nhanh hơn
- dễ mở rộng media storage

---

## 15. Thiết kế data contract cho frontend

Backend nên trả DTO phù hợp với UI.

### 15.1 Feed item DTO
Nên chứa:
- post id
- author info
- media list
- caption
- like/comment counts
- viewer state: liked/saved
- preview comments
- createdAt

### 15.2 Profile header DTO
- user id
- username
- full name
- bio
- avatar URL
- posts/followers/following count
- follow state

### 15.3 Post detail DTO
- post info
- full media
- author
- stats
- viewer state
- first page comments

### 15.4 Conversation item DTO
- conversation id
- participants preview
- last message preview
- unread count
- updated at

### 15.5 Notification DTO
- actor
- type
- entity preview
- isRead
- createdAt

---

## 16. Validation và business rules

### 16.1 Validation
- username hợp lệ và unique
- email hợp lệ
- password đủ mạnh
- caption length
- comment không rỗng
- media type hợp lệ
- user chỉ được thao tác trên resource mình có quyền

### 16.2 Business rules
- chỉ owner được sửa/xóa post
- chỉ owner hoặc người tạo comment mới được xóa comment của mình
- chỉ participant được xem conversation
- story chỉ xem được khi chưa hết hạn
- save/like không được trùng

---

## 17. Exception handling

Tạo global exception handler bằng `@RestControllerAdvice`.

Chuẩn hóa các nhóm lỗi:
- validation error
- auth error
- forbidden error
- not found
- conflict
- internal server error

---

## 18. Performance và tối ưu cơ bản

Chưa dùng Redis thì vẫn cần tối ưu bằng cách:

1. Dùng **cursor pagination**
2. Dùng **index chuẩn** trong PostgreSQL
3. Dùng **DTO projection** thay vì load entity quá nặng
4. Hạn chế N+1 query bằng fetch join hoặc query tối ưu
5. Lưu các counter như:
   - follower_count
   - following_count
   - post_count
   - like_count
   - comment_count

---

## 19. Tác vụ scheduled

Có thể dùng `@Scheduled` cho:
- cleanup stories hết hạn
- đối soát counters định kỳ nếu cần
- dọn session/revoked token cũ

---

## 20. Thiết kế endpoint tổng hợp

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### Users / Profile
- `GET /api/users/{username}`
- `PATCH /api/me/profile`
- `GET /api/users/{username}/posts`
- `GET /api/me/saved-posts`
- `GET /api/users/{username}/tagged-posts`

### Follow
- `POST /api/users/{userId}/follow`
- `DELETE /api/users/{userId}/follow`
- `GET /api/users/{userId}/followers`
- `GET /api/users/{userId}/following`

### Feed / Explore / Stories
- `GET /api/feed`
- `GET /api/explore`
- `GET /api/stories/feed`
- `POST /api/stories`
- `POST /api/stories/{storyId}/view`

### Posts / Comments
- `POST /api/posts`
- `GET /api/posts/{postId}`
- `PATCH /api/posts/{postId}`
- `DELETE /api/posts/{postId}`
- `POST /api/posts/{postId}/like`
- `DELETE /api/posts/{postId}/like`
- `POST /api/posts/{postId}/save`
- `DELETE /api/posts/{postId}/save`
- `GET /api/posts/{postId}/comments`
- `POST /api/posts/{postId}/comments`
- `DELETE /api/comments/{commentId}`

### Search
- `GET /api/search`
- `GET /api/search/users`
- `GET /api/search/hashtags`
- `GET /api/search/posts`
- `GET /api/search/recent`
- `DELETE /api/search/recent/{id}`

### Messages / Notifications
- `GET /api/conversations`
- `POST /api/conversations`
- `GET /api/conversations/{id}/messages`
- `POST /api/conversations/{id}/messages`
- `POST /api/conversations/{id}/read`
- `GET /api/notifications`
- `PATCH /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`

### Media
- `POST /api/media/presign`
- `POST /api/media/complete`

---

## 21. Kết luận

Thiết kế backend phù hợp nhất cho giai đoạn này là:

- **Spring Boot modular monolith**
- **PostgreSQL** làm database chính
- **AWS S3** cho media
- **Spring WebSocket** cho realtime
- **Java async** thay cho queue/broker trong giai đoạn đầu

Thiết kế này đủ để:
- bám sát frontend architecture hiện tại
- phát triển tương đối nhanh
- dễ trình bày trong đồ án
- vẫn có khả năng mở rộng sau này

Tài liệu database chi tiết được tách riêng ở file khác.
