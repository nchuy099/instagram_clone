# Guide kiểm thử backend bằng JUnit cho Spring Boot

## 1. Mục tiêu của guide

Guide này giúp bạn kiểm thử backend Spring Boot theo đúng hướng thực tế, dễ bảo trì, và phù hợp với project Instagram Clone của bạn. Mục tiêu là giúp bạn:

- đảm bảo business logic đúng
- tránh bug khi refactor
- kiểm tra API trả đúng contract
- kiểm tra repository/query hoạt động đúng với database
- tăng độ tin cậy trước khi demo, nộp đồ án, hoặc triển khai

---

## 2. Nên test những gì trong backend

Trong backend Spring Boot, thường nên chia test theo 4 nhóm chính:

### 2.1 Unit test
Test từng class độc lập, thường là **service**, **validator**, **mapper**, **utility**.

Ví dụ:

- `AuthService`
- `PostService`
- `CommentService`
- `FollowService`
- `NotificationService`

Đây là nhóm test quan trọng nhất.

### 2.2 Controller test
Test riêng API layer bằng `MockMvc`.

Mục đích:

- kiểm tra endpoint
- kiểm tra status code
- kiểm tra request validation
- kiểm tra JSON response

### 2.3 Repository test
Test JPA repository, custom query, pagination, join, native query.

Nhóm này đặc biệt quan trọng nếu có:

- query feed
- query comments
- query notifications
- query conversations/messages
- search với query phức tạp

### 2.4 Integration test
Test luồng hoàn chỉnh, ví dụ:

- đăng ký -> đăng nhập -> lấy thông tin user
- tạo post -> lấy post detail
- follow user -> load feed
- tạo comment -> lấy danh sách comment

---

## 3. Công cụ nên dùng

Bộ công cụ khuyến nghị:

- **JUnit 5**
- **Mockito**
- **Spring Boot Test**
- **MockMvc**
- **AssertJ**
- **Testcontainers** cho PostgreSQL

---

## 4. Dependency cần có trong `pom.xml`

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 5. Cấu trúc thư mục test nên dùng

Nên mirror theo package backend:

```text
src/
  test/
    java/
      com/example/instagram/
        auth/
          service/
            AuthServiceTest.java
          controller/
            AuthControllerTest.java
        post/
          service/
            PostServiceTest.java
          controller/
            PostControllerTest.java
          repository/
            PostRepositoryTest.java
        comment/
          service/
            CommentServiceTest.java
        follow/
          service/
            FollowServiceTest.java
        integration/
          AuthIntegrationTest.java
          PostFlowIntegrationTest.java
```

Quy ước:

- `*ServiceTest`: unit test
- `*ControllerTest`: web layer test
- `*RepositoryTest`: JPA/repository test
- `*IntegrationTest`: integration test

---

## 6. Nguyên tắc viết test

### 6.1 Một test chỉ nên kiểm tra một hành vi chính
Không nên nhồi nhiều mục tiêu vào cùng một test case.

### 6.2 Tên test phải rõ nghĩa
Ví dụ:

- `shouldCreatePostSuccessfullyWhenInputValid`
- `shouldThrowExceptionWhenUserDeletesOthersPost`
- `shouldReturnUnauthorizedWhenTokenMissing`

### 6.3 Dùng cấu trúc Given - When - Then
Giúp test dễ đọc hơn.

### 6.4 Test hành vi, không test implementation detail
Tập trung vào input/output, exception, state change.

### 6.5 Unit test phải chạy nhanh
Không load full Spring context khi chỉ test service.

---

## 7. Khi nào dùng annotation nào

### 7.1 `@ExtendWith(MockitoExtension.class)`
Dùng cho unit test với Mockito.

```java
@ExtendWith(MockitoExtension.class)
class PostServiceTest {
}
```

### 7.2 `@WebMvcTest`
Dùng để test controller layer.

```java
@WebMvcTest(PostController.class)
class PostControllerTest {
}
```

### 7.3 `@DataJpaTest`
Dùng để test repository.

```java
@DataJpaTest
class PostRepositoryTest {
}
```

### 7.4 `@SpringBootTest`
Dùng cho integration test.

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {
}
```

---

## 8. Strategy test khuyến nghị

Ưu tiên theo thứ tự:

### Mức 1: Service unit test
Đây là nơi chứa business logic, phải test kỹ nhất.

### Mức 2: Controller test
Kiểm tra request mapping, validation, response contract.

### Mức 3: Repository test
Dùng cho query quan trọng hoặc query phức tạp.

### Mức 4: Integration test
Chỉ cần phủ các flow quan trọng nhất, không cần phủ mọi thứ.

---

## 9. Ví dụ unit test cho service bằng JUnit + Mockito

```java
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void shouldCreatePostSuccessfullyWhenInputValid() {
        // Given
        Long userId = 1L;
        CreatePostRequest request = new CreatePostRequest();
        request.setCaption("hello");
        request.setMediaAssetIds(List.of(10L, 11L));

        User user = new User();
        user.setId(userId);

        MediaAsset media1 = new MediaAsset();
        media1.setId(10L);

        MediaAsset media2 = new MediaAsset();
        media2.setId(11L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mediaAssetRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(media1, media2));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Post result = postService.createPost(userId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCaption()).isEqualTo("hello");
        verify(postRepository).save(any(Post.class));
    }
}
```

---

## 10. Các case bắt buộc nên viết unit test cho service

### AuthService
- đăng ký thành công
- email đã tồn tại
- username đã tồn tại
- password sai khi login
- refresh token không hợp lệ
- logout thành công

### FollowService
- follow thành công
- không được follow chính mình
- không tạo follow trùng
- unfollow thành công

### PostService
- tạo post thành công
- update post thành công
- xóa post khi là owner
- xóa post khi không phải owner
- like/unlike
- save/unsave

### CommentService
- tạo comment thành công
- reply comment thành công
- không comment rỗng
- xóa comment khi không có quyền

### MessageService
- gửi message thành công
- user không thuộc conversation thì bị từ chối
- mark read cập nhật đúng

### NotificationService
- tạo notification đúng type
- mark read
- mark all read

---

## 11. Ví dụ controller test với MockMvc

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnOkWhenLoginValid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("123456");

        AuthResponse response = new AuthResponse("access-token", "refresh-token");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }
}
```

---

## 12. Các case nên có ở controller test

### AuthController
- login valid trả 200
- login thiếu field trả 400
- register invalid email trả 400
- `/me` thiếu token trả 401

### PostController
- create post valid trả 200 hoặc 201
- caption quá dài trả 400
- delete post không có quyền trả 403
- get post không tồn tại trả 404

### CommentController
- comment rỗng trả 400
- create comment thành công
- delete comment không có quyền trả 403

### NotificationController
- get notifications trả 200
- mark read cho notification không tồn tại trả 404

---

## 13. Repository test

Khi repository có custom query, join nhiều bảng, pagination hoặc native query, nên viết repository test.

Ví dụ:

```java
@DataJpaTest
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void shouldFindPostsByUserIdOrderByCreatedAtDesc() {
        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        em.persist(user);

        Post p1 = new Post();
        p1.setUser(user);
        p1.setCaption("first");
        em.persist(p1);

        Post p2 = new Post();
        p2.setUser(user);
        p2.setCaption("second");
        em.persist(p2);

        em.flush();

        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        assertThat(posts).hasSize(2);
    }
}
```

---

## 14. Với PostgreSQL, nên dùng Testcontainers

Nếu project dùng PostgreSQL thật và có query đặc thù, nên dùng Testcontainers thay vì H2.

```java
@Testcontainers
@SpringBootTest
class PostRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

## 15. Integration test

Dùng integration test để kiểm tra các flow hoàn chỉnh.

### Những flow nên test
- register -> login -> `/me`
- create post -> get post detail
- follow user -> load feed
- create comment -> get comments
- create conversation -> send message -> get messages
- mark notification read

Ví dụ:

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterAndLoginSuccessfully() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("alice");
        registerRequest.setEmail("alice@example.com");
        registerRequest.setPassword("StrongPass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@example.com");
        loginRequest.setPassword("StrongPass123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

---

## 16. Tổ chức test data

Nên tạo helper hoặc factory để tránh lặp code.

```java
public class TestDataFactory {

    public static User user(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }

    public static Post post(Long id, User user, String caption) {
        Post post = new Post();
        post.setId(id);
        post.setUser(user);
        post.setCaption(caption);
        return post;
    }
}
```

---

## 17. Nên mock cái gì, không nên mock cái gì

### Nên mock
- repository trong unit test service
- external service như S3 client
- JWT provider
- websocket publisher
- async notification sender

### Không nên mock
- chính class đang test
- business logic cốt lõi
- repository trong repository test
- database trong integration test

---

## 18. Test exception và authorization

Ví dụ:

```java
@Test
void shouldThrowExceptionWhenDeletingOthersPost() {
    Long currentUserId = 1L;
    Long postId = 100L;

    User owner = new User();
    owner.setId(2L);

    Post post = new Post();
    post.setId(postId);
    post.setUser(owner);

    when(postRepository.findById(postId)).thenReturn(Optional.of(post));

    assertThatThrownBy(() -> postService.deletePost(currentUserId, postId))
            .isInstanceOf(ForbiddenException.class);
}
```

Các mã lỗi nên phủ:

- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict

---

## 19. Test validation

Ví dụ:

```java
@Test
void shouldReturnBadRequestWhenCommentEmpty() throws Exception {
    CreateCommentRequest request = new CreateCommentRequest();
    request.setContent("");

    mockMvc.perform(post("/api/posts/1/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}
```

Nên test các validation như:

- email hợp lệ
- password đủ mạnh
- username unique
- comment không rỗng
- caption length
- media type hợp lệ

---

## 20. Naming convention cho test

Khuyến nghị dùng format:

- `should + expectedResult + when + condition`

Ví dụ:

- `shouldReturnUserProfileWhenUsernameExists`
- `shouldThrowNotFoundWhenPostDoesNotExist`
- `shouldRejectFollowWhenUserFollowsHimself`

Hoặc BDD style:

- `givenValidInput_whenCreatePost_thenReturnCreatedPost`

---

## 21. Coverage bao nhiêu là hợp lý

Không cần chạy theo 100% coverage.

Khuyến nghị:

- Service: 80% trở lên cho phần logic quan trọng
- Controller: phủ các endpoint chính
- Repository: phủ query quan trọng
- Integration: phủ các flow quan trọng nhất

Ưu tiên coverage ở:

- auth
- quyền truy cập
- follow
- post/comment/message
- notification

---

## 22. Những lỗi thường gặp khi test Spring Boot

### 22.1 Dùng `@SpringBootTest` quá nhiều
Làm test chậm không cần thiết.

### 22.2 Unit test nhưng lại load full context
Sai mục tiêu.

### 22.3 Test phụ thuộc dữ liệu có sẵn trong DB
Dễ flaky.

### 22.4 Dùng H2 cho query PostgreSQL đặc thù
Có thể pass test nhưng fail production.

### 22.5 Một test làm ảnh hưởng test khác
Cần cô lập dữ liệu tốt.

---

## 23. Lộ trình áp dụng ngay cho project

### Giai đoạn 1
Viết unit test cho:

- `AuthService`
- `PostService`
- `CommentService`
- `FollowService`

### Giai đoạn 2
Viết controller test cho:

- `AuthController`
- `PostController`
- `CommentController`

### Giai đoạn 3
Viết repository test cho:

- feed query
- comments query
- notification query
- conversation/message query

### Giai đoạn 4
Viết integration test cho các flow chính:

- auth flow
- create post flow
- comment flow
- follow -> feed flow
- message flow

---

## 24. Checklist khi viết test cho từng module

### Auth
- đăng ký
- login
- refresh
- logout
- `/me`

### Post
- create
- get detail
- update
- delete
- like/unlike
- save/unsave

### Comment
- list
- create
- delete
- reply

### Follow
- follow
- unfollow
- list followers/following

### Message
- create conversation
- get messages
- send message
- mark read

### Notification
- get list
- mark read
- mark all read

---

## 25. Kết luận

Cách kiểm thử phù hợp cho backend Spring Boot là:

- **JUnit 5 + Mockito** cho unit test service
- **MockMvc** cho controller test
- **`@DataJpaTest` + Testcontainers PostgreSQL** cho repository test
- **`@SpringBootTest`** cho integration flow quan trọng

Nếu phải ưu tiên, hãy bắt đầu từ **service unit test**, vì đây là nơi chứa phần lớn business logic. Sau đó mới bổ sung controller test, repository test, và integration test.

