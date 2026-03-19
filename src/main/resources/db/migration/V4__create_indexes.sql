create index idx_follows_following_id on follows(following_id);
create index idx_follows_follower_id on follows(follower_id);

create index idx_users_username_trgm on users using gin (username gin_trgm_ops);
create index idx_users_full_name_trgm on users using gin (full_name gin_trgm_ops);
