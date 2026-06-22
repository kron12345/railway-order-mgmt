-- Planning status reported back from RailOpt rotation/resource planning.
-- Orthogonal to the TTT path-process state (processState): a train can be BOOKED yet UNPLANNED.
ALTER TABLE pm_reference_trains ADD COLUMN IF NOT EXISTS planning_status VARCHAR(30) NOT NULL DEFAULT 'UNPLANNED';
ALTER TABLE pm_reference_trains_audit ADD COLUMN IF NOT EXISTS planning_status VARCHAR(30);
