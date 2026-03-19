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
