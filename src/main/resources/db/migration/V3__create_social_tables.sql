create table follows (
  id bigserial primary key,
  follower_id bigint not null references users(id) on delete cascade,
  following_id bigint not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint chk_follows_not_self check (follower_id <> following_id),
  unique (follower_id, following_id)
);
