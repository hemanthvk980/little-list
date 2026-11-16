CREATE TABLE wishlist (
    id uuid PRIMARY KEY,
    owner_subject text NOT NULL,
    title varchar(100) NOT NULL,
    occasion varchar(80),
    share_id uuid UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX wishlist_owner_idx ON wishlist (owner_subject, created_at DESC);

CREATE TABLE gift (
    id uuid PRIMARY KEY,
    wishlist_id uuid NOT NULL REFERENCES wishlist(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    category varchar(40) NOT NULL,
    note varchar(300),
    retailer_url varchar(2048),
    price_paise integer CHECK (price_paise >= 0),
    quantity integer NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX gift_list_idx ON gift (wishlist_id, created_at);

CREATE TABLE claim_request (
    token_hash char(64) PRIMARY KEY,
    gift_id uuid NOT NULL REFERENCES gift(id) ON DELETE CASCADE,
    share_id uuid NOT NULL,
    recipient_hash char(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz
);
CREATE INDEX claim_request_quota_idx ON claim_request (recipient_hash, created_at DESC);
CREATE INDEX claim_request_gift_idx ON claim_request (gift_id, created_at DESC);

CREATE TABLE gift_claim (
    id uuid PRIMARY KEY,
    gift_id uuid NOT NULL REFERENCES gift(id) ON DELETE CASCADE,
    token_hash char(64) NOT NULL UNIQUE,
    status varchar(12) NOT NULL CHECK (status IN ('held','purchased','released')),
    held_until timestamptz NOT NULL
);
CREATE INDEX gift_claim_inventory_idx ON gift_claim (gift_id, status, held_until);
