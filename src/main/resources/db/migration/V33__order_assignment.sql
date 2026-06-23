-- Order assignment (Auftrag-Zuweisung), mirroring the existing Business assignment fields.
-- assignment_type: USER (Keycloak person) | GROUP (free-text team); assignment_name: canonical value.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS assignment_type VARCHAR(30);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS assignment_name VARCHAR(255);
ALTER TABLE orders_audit ADD COLUMN IF NOT EXISTS assignment_type VARCHAR(30);
ALTER TABLE orders_audit ADD COLUMN IF NOT EXISTS assignment_name VARCHAR(255);
