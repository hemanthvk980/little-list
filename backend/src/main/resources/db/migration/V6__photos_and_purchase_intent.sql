alter table wishlist add column photo bytea;
alter table wishlist add column photo_version uuid;
alter table claim_request add column intent varchar(12) not null default 'reserve' check(intent in ('reserve','purchase'));
