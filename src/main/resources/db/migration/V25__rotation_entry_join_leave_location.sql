-- Allow specifying where a vehicle joins/leaves a train (partial route)
ALTER TABLE vp_rotation_entries ADD COLUMN join_at_location VARCHAR(255);
ALTER TABLE vp_rotation_entries ADD COLUMN leave_at_location VARCHAR(255);
