TRUNCATE TABLE projects.artifact_branch CASCADE;

ALTER TABLE projects.use_case
    DROP CONSTRAINT IF EXISTS uq_use_case_project_branch_code;
ALTER TABLE projects.use_case
    DROP CONSTRAINT IF EXISTS fk_use_case_artifact_branch;
ALTER TABLE projects.use_case
    DROP COLUMN IF EXISTS project_branch_id;
ALTER TABLE projects.use_case
    ADD COLUMN project_id integer NOT NULL;
ALTER TABLE projects.use_case
    ADD CONSTRAINT fk_use_case_project
        FOREIGN KEY (project_id) REFERENCES projects.project(id) ON DELETE CASCADE;
ALTER TABLE projects.use_case
    ADD CONSTRAINT uq_use_case_project_code UNIQUE (project_id, code);
CREATE INDEX IF NOT EXISTS idx_use_case_project_id
    ON projects.use_case(project_id);

ALTER TABLE projects.us_operation_relation
    DROP CONSTRAINT IF EXISTS fk_us_operation_relation_use_case;
DROP INDEX IF EXISTS projects.idx_us_operation_relation_uc_id;
ALTER TABLE projects.us_operation_relation
    DROP COLUMN IF EXISTS uc_id;
ALTER TABLE projects.us_operation_relation
    ADD COLUMN use_case_branch_id integer NOT NULL;
ALTER TABLE projects.us_operation_relation
    ADD CONSTRAINT fk_us_operation_relation_use_case_branch
        FOREIGN KEY (use_case_branch_id) REFERENCES projects.artifact_branch(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_us_operation_relation_use_case_branch_id
    ON projects.us_operation_relation(use_case_branch_id);
