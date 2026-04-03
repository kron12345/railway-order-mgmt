-- Fix audit table country_code_iso too (V12 only fixed main table)
ALTER TABLE pm_journey_locations_aud ALTER COLUMN country_code_iso TYPE VARCHAR(5);
