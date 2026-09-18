CREATE SCHEMA IF NOT EXISTS projects;

CREATE SEQUENCE IF NOT EXISTS projects.project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS projects.project (
    id integer PRIMARY KEY DEFAULT nextval('projects.project_id_seq'::regclass),
    name text NOT NULL UNIQUE,
    description text,
    source text NOT NULL,
    created_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone,
    delete_date timestamp without time zone
);

CREATE INDEX IF NOT EXISTS idx_project_name ON projects.project(name);
CREATE INDEX IF NOT EXISTS idx_project_source ON projects.project(source);
CREATE INDEX IF NOT EXISTS idx_project_delete_date ON projects.project(delete_date) WHERE delete_date IS NULL; 