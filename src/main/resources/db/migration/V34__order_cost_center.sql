-- Kostenträger / PSP-Element on the order (SOB §5.7: mandatory before status "freigegeben").
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cost_center VARCHAR(100);
ALTER TABLE orders_audit ADD COLUMN IF NOT EXISTS cost_center VARCHAR(100);

-- orders.internal_status was historically a free-text field (removed from the form in e0a0c61) and
-- is now mapped as an @Enumerated(STRING) PositionStatus. Any legacy free-text value that is not a
-- valid enum constant would crash entity loading, so null out everything that does not match one of
-- the seven PositionStatus names. Same for the Envers audit table.
UPDATE orders SET internal_status = NULL
 WHERE internal_status IS NOT NULL
   AND internal_status NOT IN ('IN_BEARBEITUNG', 'FREIGEGEBEN', 'UEBERARBEITEN',
                               'UEBERMITTELT', 'BEANTRAGT', 'ABGESCHLOSSEN', 'ANNULLIERT');
UPDATE orders_audit SET internal_status = NULL
 WHERE internal_status IS NOT NULL
   AND internal_status NOT IN ('IN_BEARBEITUNG', 'FREIGEGEBEN', 'UEBERARBEITEN',
                               'UEBERMITTELT', 'BEANTRAGT', 'ABGESCHLOSSEN', 'ANNULLIERT');
