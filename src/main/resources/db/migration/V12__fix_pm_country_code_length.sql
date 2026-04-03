-- Fix country_code_iso column: varchar(2) too short for some RINF data (e.g. "CHE", "DEU")
ALTER TABLE pm_journey_locations ALTER COLUMN country_code_iso TYPE VARCHAR(5);
ALTER TABLE pm_journey_locations_aud ALTER COLUMN country_code_iso TYPE VARCHAR(5);
