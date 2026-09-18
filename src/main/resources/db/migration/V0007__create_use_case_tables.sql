CREATE SEQUENCE IF NOT EXISTS projects.use_case_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS projects.use_case (
    id integer PRIMARY KEY DEFAULT nextval('projects.use_case_id_seq'::regclass),
    project_branch_id integer NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    description text,
    CONSTRAINT uq_use_case_project_branch_code UNIQUE (project_branch_id, code),
    CONSTRAINT fk_use_case_artifact_branch FOREIGN KEY (project_branch_id)
        REFERENCES projects.artifact_branch(id) ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS projects.us_operation_relation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- req_operation_id / related_req_operation_id — id product.discovered_operation в сервисе
-- fdm-products; это другая БД, поэтому явного FK на них нет (по постановке).
CREATE TABLE IF NOT EXISTS projects.us_operation_relation (
    id integer PRIMARY KEY DEFAULT nextval('projects.us_operation_relation_id_seq'::regclass),
    uc_id integer NOT NULL,
    "order" integer NOT NULL,
    req_operation_id integer,
    related_req_operation_id integer NOT NULL,
    CONSTRAINT fk_us_operation_relation_use_case FOREIGN KEY (uc_id)
        REFERENCES projects.use_case(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_us_operation_relation_uc_id
    ON projects.us_operation_relation(uc_id);
