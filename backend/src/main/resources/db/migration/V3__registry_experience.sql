ALTER TABLE wishlist ADD COLUMN greeting varchar(500);
ALTER TABLE gift ADD COLUMN variant varchar(120);
ALTER TABLE gift ADD COLUMN priority varchar(20) NOT NULL DEFAULT 'nice-to-have' CHECK (priority IN ('essential','nice-to-have'));
ALTER TABLE gift ADD COLUMN capture_source varchar(20) NOT NULL DEFAULT 'manual' CHECK (capture_source IN ('manual','link','extension','template'));
CREATE TABLE onboarding (owner_subject text PRIMARY KEY, draft text NOT NULL);
CREATE TABLE onboarding_result (owner_subject text NOT NULL, request_id uuid NOT NULL, body_hash char(64) NOT NULL, wishlist_id uuid NOT NULL REFERENCES wishlist(id) ON DELETE CASCADE, PRIMARY KEY(owner_subject,request_id));
CREATE TABLE gift_save (wishlist_id uuid NOT NULL REFERENCES wishlist(id) ON DELETE CASCADE, request_id uuid NOT NULL, body_hash char(64) NOT NULL, PRIMARY KEY(wishlist_id,request_id));
CREATE TABLE checklist_entry (wishlist_id uuid NOT NULL REFERENCES wishlist(id) ON DELETE CASCADE, item_key varchar(40) NOT NULL, state varchar(20) NOT NULL CHECK(state IN ('undecided','need','have','not-needed')), PRIMARY KEY(wishlist_id,item_key));
