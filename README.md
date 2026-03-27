# 📸 Mini Instagram

A full-stack Instagram clone with complete social networking features — built with **Spring Boot** and **React**.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-blue?logo=typescript)
![TailwindCSS](https://img.shields.io/badge/TailwindCSS-4-38bdf8?logo=tailwindcss)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?logo=postgresql&logoColor=white)

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 **Authentication** | Register / Login via email, username, or phone number. OAuth2 support (Facebook). JWT access & refresh tokens. |
| 👤 **User Profiles** | Edit profile, avatar, bio. View profiles at `/:username`. |
| 📝 **Posts** | Create posts with multiple images/videos (media carousel). Like & unlike posts. |
| 💬 **Comments** | Comment on posts, view comment list via modal. |
| 📖 **Stories** | Post and view stories with progress bars, auto-advance between stories. |
| 🏠 **News Feed** | Feed from followed users, sorted chronologically. |
| 👥 **Follow** | Follow / Unfollow users. View followers & following lists. |
| 🔍 **Search** | Search for users, save recent search history. |
| 🗺️ **Explore** | Explore page showing popular content. |
| 💬 **Messaging** | Real-time messaging via WebSocket (STOMP). |
| #️⃣ **Hashtags** | Hashtag support in posts. |
| 📤 **Media Upload** | Direct upload to AWS S3 with presigned URLs. |

---

## 🏗️ Architecture

```
instagram_clone/
├── backend/          # Spring Boot REST API
│   └── src/main/java/com/nchuy099/mini_instagram/
│       ├── auth/         # Authentication & authorization (JWT, OAuth2)
│       ├── user/         # User management & profiles
│       ├── post/         # Posts & likes
│       ├── comment/      # Comments
│       ├── story/        # Stories
│       ├── feed/         # News Feed
│       ├── media/        # Media upload (S3)
│       ├── message/      # Messaging
│       ├── search/       # Search
│       ├── websocket/    # WebSocket config
│       └── common/       # Shared utilities
│
└── frontend/         # React SPA
    └── src/
        ├── pages/        # Pages: Home, Profile, Explore, Search, Messages
        ├── features/     # Feature modules: auth, post, story, message, profile
        ├── components/   # Shared components
        ├── hooks/        # Custom React hooks
        └── lib/          # Utilities & API clients
```

---

## 🛠️ Tech Stack

### Backend

| Technology | Purpose |
|---|---|
| **Spring Boot 3.5** | Core framework |
| **Java 21** | Programming language |
| **Spring Security** | Authentication & authorization |
| **JWT (jjwt)** | Token-based authentication |
| **OAuth2 Client** | Facebook login |
| **Spring Data JPA** | ORM & database queries |
| **PostgreSQL** | Database |
| **Flyway** | Database migration |
| **AWS S3 SDK** | Media storage |
| **WebSocket (STOMP)** | Real-time messaging |
| **SpringDoc OpenAPI** | API documentation (Swagger UI) |
| **Lombok** | Reduce boilerplate code |

### Frontend

| Technology | Purpose |
|---|---|
| **React 19** | UI library |
| **TypeScript 5.9** | Type-safe JavaScript |
| **Vite 8** | Build tool & dev server |
| **TailwindCSS 4** | Utility-first CSS |
| **React Router 7** | Client-side routing |
| **Axios** | HTTP client |
| **STOMP.js** | WebSocket client |
| **Lucide React** | Icon library |
| **date-fns** | Date formatting |
| **react-easy-crop** | Image cropping |

---

## 🚀 Getting Started

### Prerequisites

- **Java 21+**
- **Node.js 18+** & **npm**
- **PostgreSQL 15+**
- **AWS** account (for S3 upload)
- (Optional) **Facebook App** for OAuth2

### 1. Clone the repository

```bash
git clone https://github.com/nchuy099/instagram_clone.git
cd instagram_clone
```

### 2. Configure Backend

```bash
cd backend
cp .env.example .env
```

Edit the `.env` file:

```env
PG_URL=jdbc:postgresql://localhost:5432/instagram_clone
PG_USERNAME=insta_user
PG_PASSWORD=your_password
JWT_SECRET=your_super_secret_jwt_key_that_is_at_least_32_bytes_long

FACEBOOK_APP_ID=your_facebook_app_id
FACEBOOK_APP_SECRET=your_facebook_app_secret

FRONTEND_URL=http://localhost:5173

AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=us-east-1
AWS_S3_BUCKET=your_bucket_name
```

### 3. Create Database

```bash
psql -U postgres
```

```sql
CREATE DATABASE instagram_clone;
CREATE USER insta_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE instagram_clone TO insta_user;
```

> 💡 Flyway will automatically run migrations on server startup.

### 4. Run Backend

```bash
cd backend
./gradlew bootRun
```

Backend will be available at: `http://localhost:8080`

### 5. Configure & Run Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Frontend will be available at: `http://localhost:5173`

---

## 🗄️ Database Migrations

This project uses **Flyway** for schema management. Migrations are located at:

```
backend/src/main/resources/db/migration/
```

| Migration | Description |
|---|---|
| V1 | Enable PostgreSQL extensions |
| V2 | User & identity tables |
| V3 | Social relationship tables (follow) |
| V4 | Create indexes |
| V5 | Add phone number to users |
| V6 | Post & comment tables |
| V7 | Story table |
| V8 | Recent search history table |
| V9 | Hashtag tables |
| V10 | Add target user to recent searches |
| V11 | Add thumbnail URL to post media |
| V12 | Story interaction tables |
| V13 | Message tables |

---

## 🧪 Testing

### Backend

```bash
cd backend
./gradlew test
```

---

## 📁 Environment Variables

### Backend (`backend/.env`)

| Variable | Description |
|---|---|
| `PG_URL` | PostgreSQL connection URL |
| `PG_USERNAME` | Database username |
| `PG_PASSWORD` | Database password |
| `JWT_SECRET` | JWT secret key (≥ 32 bytes) |
| `FACEBOOK_APP_ID` | Facebook OAuth2 App ID |
| `FACEBOOK_APP_SECRET` | Facebook OAuth2 App Secret |
| `FRONTEND_URL` | Frontend URL (CORS) |
| `AWS_ACCESS_KEY_ID` | AWS Access Key |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Key |
| `AWS_REGION` | AWS Region |
| `AWS_S3_BUCKET` | S3 Bucket name |

### Frontend (`frontend/.env`)

| Variable | Description |
|---|---|
| `VITE_API_URL` | Backend API URL |

---

## 📄 License

This project is developed for learning purposes.
