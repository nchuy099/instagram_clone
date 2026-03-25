create table hashtags (
  id uuid primary key default gen_random_uuid(),
  name varchar(100) not null unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table post_hashtags (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references posts(id) on delete cascade,
  hashtag_id uuid not null references hashtags(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uk_post_hashtag unique (post_id, hashtag_id)
);

create index idx_post_hashtags_post_id on post_hashtags(post_id);
create index idx_post_hashtags_hashtag_id on post_hashtags(hashtag_id);
create index idx_hashtags_name_trgm on hashtags using gin (name gin_trgm_ops);
