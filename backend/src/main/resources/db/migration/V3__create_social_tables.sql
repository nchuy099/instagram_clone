create table follows (
  id uuid primary key default gen_random_uuid(),
  follower_id uuid not null references users(id) on delete cascade,
  following_id uuid not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_follows_not_self check (follower_id <> following_id),
  unique (follower_id, following_id)
);
