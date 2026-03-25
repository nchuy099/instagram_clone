alter table recent_searches
  add column if not exists target_user_id uuid references users(id) on delete set null;

create index if not exists idx_recent_searches_target_user
  on recent_searches(target_user_id);

create unique index if not exists uq_recent_search_user_target
  on recent_searches(user_id, search_type, target_user_id)
  where search_type = 'USER' and target_user_id is not null;
