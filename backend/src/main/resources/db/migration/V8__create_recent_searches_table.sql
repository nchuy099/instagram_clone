create table recent_searches (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  search_type varchar(20) not null,
  query_text varchar(255) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index uq_recent_search_user_type_query
  on recent_searches(user_id, search_type, lower(query_text));

create index idx_recent_searches_user_updated
  on recent_searches(user_id, updated_at desc);

create index idx_posts_caption_trgm
  on posts using gin (caption gin_trgm_ops);
