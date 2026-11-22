CREATE TABLE app_user (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 owner_subject text NOT NULL UNIQUE,
 display_name varchar(200), email varchar(254),
 first_seen timestamptz NOT NULL DEFAULT now(), last_login timestamptz
);
INSERT INTO app_user(owner_subject, first_seen)
 SELECT owner_subject,min(created_at) FROM wishlist GROUP BY owner_subject;
INSERT INTO app_user(owner_subject) SELECT owner_subject FROM onboarding ON CONFLICT DO NOTHING;
CREATE INDEX app_user_seen_idx ON app_user(first_seen DESC,id);
CREATE TABLE admin_audit (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 actor text NOT NULL, action varchar(40) NOT NULL, target uuid,
 occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX admin_audit_actor_time_idx ON admin_audit(actor,occurred_at);
