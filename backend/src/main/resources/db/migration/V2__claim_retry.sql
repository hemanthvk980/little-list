ALTER TABLE claim_request ADD COLUMN claim_id uuid REFERENCES gift_claim(id) ON DELETE SET NULL;
ALTER TABLE gift ADD COLUMN archived boolean NOT NULL DEFAULT false;
