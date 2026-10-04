-- Unknown historical context and transport settings stay NULL; no backfill.
ALTER TABLE report_generation_attempt ADD COLUMN model_settings JSONB;
