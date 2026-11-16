alter table registry_link_day drop constraint registry_link_day_channel_check;
alter table registry_link_day add constraint registry_link_day_channel_check check(channel in ('direct','whatsapp','qr'));
