CREATE TABLE password_account (
 id uuid PRIMARY KEY, username varchar(32) NOT NULL UNIQUE,
 email varchar(254) NOT NULL UNIQUE, display_name varchar(100) NOT NULL,
 password_hash text NOT NULL, verified boolean NOT NULL DEFAULT false,
 version integer NOT NULL DEFAULT 1, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE account_token (
 token_hash char(64) PRIMARY KEY, account_id uuid NOT NULL REFERENCES password_account(id) ON DELETE CASCADE,
 purpose varchar(10) NOT NULL CHECK(purpose IN ('verify','reset')), expires_at timestamptz NOT NULL
);
CREATE TABLE auth_attempt (
 bucket char(64) PRIMARY KEY, attempts integer NOT NULL, window_start timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE registry_journey (
 wishlist_id uuid PRIMARY KEY REFERENCES wishlist(id) ON DELETE CASCADE,
 chapter varchar(40) NOT NULL DEFAULT '', arrival_date date,
 multiple boolean NOT NULL DEFAULT false, experience varchar(20) NOT NULL DEFAULT '',
 feeling varchar(20) NOT NULL DEFAULT '', countdown_enabled boolean NOT NULL DEFAULT true
);
