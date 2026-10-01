
ALTER TABLE projects.use_case
    ADD COLUMN IF NOT EXISTS created_date timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE projects.use_case
    ALTER COLUMN created_date DROP DEFAULT;

ALTER TABLE projects.use_case
    ADD COLUMN IF NOT EXISTS updated_date timestamp without time zone;

ALTER TABLE projects.use_case
    ADD COLUMN IF NOT EXISTS deleted_date timestamp without time zone;

CREATE INDEX IF NOT EXISTS idx_use_case_deleted_date
    ON projects.use_case (deleted_date)
    WHERE deleted_date IS NULL;