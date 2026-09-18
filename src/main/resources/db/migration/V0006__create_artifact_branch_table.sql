CREATE SEQUENCE IF NOT EXISTS projects.artifact_branch_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS projects.artifact_branch (
    id integer PRIMARY KEY DEFAULT nextval('projects.artifact_branch_id_seq'::regclass),
    artifact_type text NOT NULL,
    artifact_id integer NOT NULL,
    name text NOT NULL,
    CONSTRAINT uq_artifact_branch_type_artifact_name UNIQUE (artifact_type, artifact_id, name)
);

CREATE INDEX IF NOT EXISTS idx_artifact_branch_artifact
    ON projects.artifact_branch(artifact_type, artifact_id);

-- Ветка main для уже существующих проектов; для новых её заводит ProjectService.createProject.
INSERT INTO projects.artifact_branch (artifact_type, artifact_id, name)
SELECT 'project', p.id, 'main'
FROM projects.project p
ON CONFLICT (artifact_type, artifact_id, name) DO NOTHING;
