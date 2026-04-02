-- Add Operational Train Number (OTN) to timetable archives and order positions
-- Free-text field to support patterns like "95345" or "95xxx"
ALTER TABLE public.timetable_archives
    ADD COLUMN operational_train_number VARCHAR(20);

ALTER TABLE public.timetable_archives_aud
    ADD COLUMN operational_train_number VARCHAR(20);

ALTER TABLE public.order_positions
    ADD COLUMN operational_train_number VARCHAR(20);

ALTER TABLE public.order_positions_aud
    ADD COLUMN operational_train_number VARCHAR(20);
