-- Link rotation planning vehicles to rolling stock master data
ALTER TABLE vp_vehicles ADD COLUMN rolling_stock_id UUID
    REFERENCES rs_rolling_stock(id);

CREATE INDEX idx_vp_vehicles_rolling_stock ON vp_vehicles(rolling_stock_id);
