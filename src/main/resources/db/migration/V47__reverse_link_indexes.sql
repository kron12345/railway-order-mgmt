-- V47: Inverse-direction indexes for the business link join tables and the reference-train source
-- link (tech-audit). The join tables' composite PK leads with business_id, so reverse lookups
-- (find businesses linked to a given order/purchase position — findByLinkedOrderPositionId,
-- findBusinessesBy*PositionIds, the OrderDetail business tab) cannot use it. pm_reference_trains is
-- queried by source_position_id (findBySourcePositionId / ...IsNull) with no index. Idempotent.

CREATE INDEX IF NOT EXISTS idx_biz_order_positions_op
    ON business_order_positions (order_position_id);
CREATE INDEX IF NOT EXISTS idx_biz_purchase_positions_pp
    ON business_purchase_positions (purchase_position_id);
CREATE INDEX IF NOT EXISTS idx_pm_reference_trains_source_position
    ON pm_reference_trains (source_position_id);
