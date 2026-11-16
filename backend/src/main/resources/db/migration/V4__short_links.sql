create table registry_link (
  code varchar(22) primary key,
  wishlist_id uuid not null references wishlist(id) on delete cascade,
  share_id uuid not null unique
);
insert into registry_link(code,wishlist_id,share_id)
select rtrim(translate(encode(uuid_send(gen_random_uuid()),'base64'),'+/','-_'),'='),id,share_id
from wishlist where share_id is not null;
create table registry_link_day (
  code varchar(22) not null references registry_link(code) on delete cascade,
  day date not null,
  channel varchar(12) not null check(channel in ('direct','whatsapp')),
  opens bigint not null default 0,
  primary key(code,day,channel)
);
