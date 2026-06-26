-- V46: Indexes backing the lazy order/business lists (P7 closeout). The lazy lists sort by
-- order_number / title (+ id tie-breaker) and filter by status, validity range and assignee. These
-- btree indexes make the paged Slice queries scale once the tables grow. Text filters use leading
-- wildcards (LIKE '%x%'), which a btree cannot serve, so no index is added for those columns.
-- IF NOT EXISTS keeps the migration idempotent.

-- Orders: range/equality filter columns. order_number (sort key) is already indexed by V1
-- (idx_orders_order_number + the UNIQUE constraint) and process_status by V2 (idx_orders_process_status).
CREATE INDEX IF NOT EXISTS idx_orders_internal_status ON orders (internal_status);
CREATE INDEX IF NOT EXISTS idx_orders_valid_from ON orders (valid_from);
CREATE INDEX IF NOT EXISTS idx_orders_valid_to ON orders (valid_to);
CREATE INDEX IF NOT EXISTS idx_orders_assignment ON orders (assignment_type, assignment_name);

-- Businesses: default sort key + range/equality filter columns.
CREATE INDEX IF NOT EXISTS idx_businesses_title ON businesses (title);
CREATE INDEX IF NOT EXISTS idx_businesses_status ON businesses (status);
CREATE INDEX IF NOT EXISTS idx_businesses_valid_from ON businesses (valid_from);
CREATE INDEX IF NOT EXISTS idx_businesses_valid_to ON businesses (valid_to);
CREATE INDEX IF NOT EXISTS idx_businesses_assignment ON businesses (assignment_type, assignment_name);
