CREATE SEQUENCE IF NOT EXISTS projects.project_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS projects.project_user (
    id integer PRIMARY KEY DEFAULT nextval('projects.project_user_id_seq'::regclass),
    project_id integer NOT NULL,
    user_id integer NOT NULL,
    CONSTRAINT fk_project_user_project FOREIGN KEY (project_id)
        REFERENCES projects.project(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_project_user_unique
    ON projects.project_user(project_id, user_id);
