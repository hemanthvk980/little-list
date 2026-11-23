ALTER TABLE claim_request ADD COLUMN guest_name varchar(100), ADD COLUMN guest_email varchar(254), ADD COLUMN gift_message varchar(500), ADD COLUMN purchase_store varchar(100);
ALTER TABLE gift_claim ADD COLUMN guest_name varchar(100), ADD COLUMN guest_email varchar(254), ADD COLUMN gift_message varchar(500), ADD COLUMN purchase_store varchar(100), ADD COLUMN purchased_at timestamptz;
CREATE INDEX gift_claim_gift_order ON gift_claim(gift_id, id);
