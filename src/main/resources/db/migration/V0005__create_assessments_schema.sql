CREATE SCHEMA IF NOT EXISTS projects;


CREATE SEQUENCE IF NOT EXISTS projects.assessments_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS projects.project_status_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS projects.assessment_status_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS projects.requirements_func_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS projects.requirements_non_func_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS projects.open_questions_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS projects.assessment_bc_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS projects.assessment_tc_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS projects.assessment_new_tcs_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;


CREATE TABLE IF NOT EXISTS projects.project (
    id           serial PRIMARY KEY,
    name         text NOT NULL,
    description  text,
    source       text NOT NULL,
    created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date  timestamp,
    delete_date  timestamp
);

ALTER TABLE projects.project ADD COLUMN IF NOT EXISTS unique_ident text NOT NULL;
ALTER TABLE projects.project ADD COLUMN IF NOT EXISTS doc_link text;
ALTER TABLE projects.project ADD COLUMN IF NOT EXISTS owner_id      int4 NOT NULL;
ALTER TABLE projects.project ADD COLUMN IF NOT EXISTS status_id    int4 NOT NULL;

ALTER TABLE projects.project DROP CONSTRAINT IF EXISTS uq_project_unique_ident;
ALTER TABLE projects.project
    ADD CONSTRAINT uq_project_unique_ident UNIQUE (unique_ident);


CREATE TABLE IF NOT EXISTS projects.project_status_enum (
    id          int4 PRIMARY KEY DEFAULT nextval('projects.project_status_id_seq'::regclass),
    name        text NOT NULL,
    description text
);

ALTER TABLE projects.project_status_enum
    DROP CONSTRAINT IF EXISTS project_status_enum_name_unique;
ALTER TABLE projects.project_status_enum
    ADD CONSTRAINT project_status_enum_name_unique UNIQUE (name);

COMMENT ON TABLE  projects.project_status_enum IS 'Справочник статусов проекта';
COMMENT ON COLUMN projects.project_status_enum.id IS 'Суррогатный ключ (int4, автоинкремент)';
COMMENT ON COLUMN projects.project_status_enum.name IS 'Название статуса: Backlog | InWork | Done';
COMMENT ON COLUMN projects.project_status_enum.description IS 'Описание статуса';


CREATE TABLE IF NOT EXISTS projects.assessment_status_enum (
    id          int4 PRIMARY KEY DEFAULT nextval('projects.assessment_status_id_seq'::regclass),
    name        text NOT NULL,
    description text
);

ALTER TABLE projects.assessment_status_enum
    DROP CONSTRAINT IF EXISTS assessment_status_enum_name_unique;
ALTER TABLE projects.assessment_status_enum
    ADD CONSTRAINT assessment_status_enum_name_unique UNIQUE (name);

COMMENT ON TABLE  projects.assessment_status_enum IS 'Справочник статусов оценки';
COMMENT ON COLUMN projects.assessment_status_enum.id IS 'Суррогатный ключ (int4, автоинкремент)';
COMMENT ON COLUMN projects.assessment_status_enum.name IS 'Название статуса: Draft | REQ | BC | TC | Done';
COMMENT ON COLUMN projects.assessment_status_enum.description IS 'Описание статуса';


INSERT INTO projects.project_status_enum (name, description) VALUES
    ('Backlog', 'Проект в бэклоге, ожидает начала работ'),
    ('InWork',  'Проект в работе'),
    ('Done',    'Проект завершен')
ON CONFLICT (name) DO NOTHING;


INSERT INTO projects.assessment_status_enum (name, description) VALUES
    ('Draft', 'Черновик оценки'),
    ('REQ',   'Требования структурированы'),
    ('BC',    'Выбор релевантных бизнес-возможностей'),
    ('TC',    'Выбор релевантных технических возможностей'),
    ('Done',  'Оценка завершена')
ON CONFLICT (name) DO NOTHING;


ALTER TABLE projects.project DROP CONSTRAINT IF EXISTS fk_project_status;
ALTER TABLE projects.project
    ADD CONSTRAINT fk_project_status
    FOREIGN KEY (status_id) REFERENCES projects.project_status_enum(id);

COMMENT ON COLUMN projects.project.unique_ident IS 'Уникальный идентификатор проекта';
COMMENT ON COLUMN projects.project.doc_link   IS 'Ссылка на документ проекта (Confluence и т.п.)';
COMMENT ON COLUMN projects.project.owner_id    IS 'ID владельца проекта (int4 из системы авторизации)';
COMMENT ON COLUMN projects.project.status_id   IS 'FK на project_status_enum.id';


CREATE TABLE IF NOT EXISTS projects.assessments (
    id                       int4 PRIMARY KEY DEFAULT nextval('projects.assessments_id_seq'::regclass),
    project_id               int4          NOT NULL,
    owner_id                 int4          NOT NULL,
    status_id                int4          NOT NULL,
    source                   varchar(20)   NOT NULL,
    source_url               varchar(1000),
    raw_text                 text          NOT NULL,
    task_description         text,
    impact_level             text,
    created_date              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date               timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delete_date               timestamp
);

COMMENT ON TABLE  projects.assessments IS 'Реестр оценок: одна бизнес-постановка = одна оценка, привязанная к проекту';
COMMENT ON COLUMN projects.assessments.id IS 'Суррогатный ключ (int4, автоинкремент)';
COMMENT ON COLUMN projects.assessments.project_id IS 'FK на projects.project.id';
COMMENT ON COLUMN projects.assessments.owner_id IS 'ID автора (int4 из системы авторизации)';
COMMENT ON COLUMN projects.assessments.status_id IS 'FK на assessment_status_enum.id';
COMMENT ON COLUMN projects.assessments.source IS 'Источник постановки: text | confluence';
COMMENT ON COLUMN projects.assessments.source_url IS 'URL страницы Confluence (если source = confluence)';
COMMENT ON COLUMN projects.assessments.raw_text IS 'Исходный текст бизнес-постановки';
COMMENT ON COLUMN projects.assessments.task_description IS 'AI-сгенерированное краткое описание задачи';
COMMENT ON COLUMN projects.assessments.impact_level IS 'Уровень влияния: S | M | L | XL (Скоринг 2.0)';
COMMENT ON COLUMN projects.assessments.created_date IS 'Дата создания оценки';
COMMENT ON COLUMN projects.assessments.update_date IS 'Дата последнего обновления (обновляется приложением)';
COMMENT ON COLUMN projects.assessments.delete_date IS 'Дата мягкого удаления';


ALTER TABLE projects.assessments DROP CONSTRAINT IF EXISTS fk_assessments_project;
ALTER TABLE projects.assessments
    ADD CONSTRAINT fk_assessments_project
    FOREIGN KEY (project_id) REFERENCES projects.project(id);

ALTER TABLE projects.assessments DROP CONSTRAINT IF EXISTS fk_assessments_status;
ALTER TABLE projects.assessments
    ADD CONSTRAINT fk_assessments_status
    FOREIGN KEY (status_id) REFERENCES projects.assessment_status_enum(id);


CREATE TABLE IF NOT EXISTS projects.requirements_func (
    id            int4 PRIMARY KEY DEFAULT nextval('projects.requirements_func_id_seq'::regclass),
    assessment_id int4         NOT NULL,
    unique_ident  varchar(20)  NOT NULL,
    title         text         NOT NULL,
    description   text
);

ALTER TABLE projects.requirements_func DROP CONSTRAINT IF EXISTS fk_req_f_assessment;
ALTER TABLE projects.requirements_func
    ADD CONSTRAINT fk_req_f_assessment
    FOREIGN KEY (assessment_id) REFERENCES projects.assessments(id) ON DELETE CASCADE;

ALTER TABLE projects.requirements_func DROP CONSTRAINT IF EXISTS uq_requirements_func_assessment_ident;
ALTER TABLE projects.requirements_func
    ADD CONSTRAINT uq_requirements_func_assessment_ident UNIQUE (assessment_id, unique_ident);

CREATE INDEX IF NOT EXISTS idx_requirements_func_assessment ON projects.requirements_func(assessment_id);

COMMENT ON TABLE  projects.requirements_func IS 'Функциональные требования (FR) — нормализованное хранение';
COMMENT ON COLUMN projects.requirements_func.id IS 'Суррогатный ключ (int4, автоинкремент)';
COMMENT ON COLUMN projects.requirements_func.assessment_id IS 'FK на assessments.id';
COMMENT ON COLUMN projects.requirements_func.unique_ident IS 'Естественный идентификатор: FR-1, FR-2, ... (уникален в рамках оценки)';
COMMENT ON COLUMN projects.requirements_func.title IS 'Заголовок требования';
COMMENT ON COLUMN projects.requirements_func.description IS 'Описание требования';


CREATE TABLE IF NOT EXISTS projects.requirements_non_func (
    id            int4 PRIMARY KEY DEFAULT nextval('projects.requirements_non_func_id_seq'::regclass),
    assessment_id int4         NOT NULL,
    unique_ident  varchar(20)  NOT NULL,
    title         text         NOT NULL,
    description   text
);

ALTER TABLE projects.requirements_non_func DROP CONSTRAINT IF EXISTS fk_req_nf_assessment;
ALTER TABLE projects.requirements_non_func
    ADD CONSTRAINT fk_req_nfr_assessment
    FOREIGN KEY (assessment_id) REFERENCES projects.assessments(id) ON DELETE CASCADE;

ALTER TABLE projects.requirements_non_func DROP CONSTRAINT IF EXISTS uq_requirements_non_func_assessment_ident;
ALTER TABLE projects.requirements_non_func
    ADD CONSTRAINT uq_requirements_non_func_assessment_ident UNIQUE (assessment_id, unique_ident);

CREATE INDEX IF NOT EXISTS idx_requirements_non_func_assessment ON projects.requirements_non_func(assessment_id);

COMMENT ON TABLE  projects.requirements_non_func IS 'Нефункциональные требования (NFR) — нормализованное хранение';
COMMENT ON COLUMN projects.requirements_non_func.id IS 'Суррогатный ключ (int4, автоинкремент)';
COMMENT ON COLUMN projects.requirements_non_func.assessment_id IS 'FK на assessments.id';
COMMENT ON COLUMN projects.requirements_non_func.unique_ident IS 'Естественный идентификатор: NFR-1, NFR-91, ... (уникален в рамках оценки)';
COMMENT ON COLUMN projects.requirements_non_func.title IS 'Заголовок требования';
COMMENT ON COLUMN projects.requirements_non_func.description IS 'Описание требования';


CREATE TABLE IF NOT EXISTS projects.open_questions (
    id            int4 PRIMARY KEY DEFAULT nextval('projects.open_questions_id_seq'::regclass),
    assessment_id int4         NOT NULL,
    unique_ident  varchar(20)  NOT NULL,
    question_text text         NOT NULL

);

ALTER TABLE projects.open_questions DROP CONSTRAINT IF EXISTS fk_oq_assessment;
ALTER TABLE projects.open_questions
    ADD CONSTRAINT fk_oq_assessment
    FOREIGN KEY (assessment_id) REFERENCES projects.assessments(id) ON DELETE CASCADE;

ALTER TABLE projects.open_questions DROP CONSTRAINT IF EXISTS uq_open_questions_assessment_ident;
ALTER TABLE projects.open_questions
    ADD CONSTRAINT uq_open_questions_assessment_ident UNIQUE (assessment_id, unique_ident);

CREATE INDEX IF NOT EXISTS idx_open_questions_assessment ON projects.open_questions(assessment_id);

COMMENT ON TABLE  projects.open_questions IS 'Открытые вопросы (OQ) из структурированного вывода LLM';
COMMENT ON COLUMN projects.open_questions.id IS 'Суррогатный ключ (int4, автоинкремент)';
COMMENT ON COLUMN projects.open_questions.assessment_id IS 'FK на assessments.id';
COMMENT ON COLUMN projects.open_questions.unique_ident IS 'Естественный идентификатор: OQ-1, OQ-2, ... (уникален в рамках оценки)';
COMMENT ON COLUMN projects.open_questions.question_text IS 'Текст вопроса';


CREATE TABLE IF NOT EXISTS projects.assessment_bc (
    id                       int4 PRIMARY KEY DEFAULT nextval('projects.assessment_bc_id_seq'::regclass),
    assessment_id            int4 NOT NULL,
    bc_id   int4 NOT NULL,
    relevance                int4,
    reason                   text
);

ALTER TABLE projects.assessment_bc DROP CONSTRAINT IF EXISTS fk_assessment_bc_assessment;
ALTER TABLE projects.assessment_bc
    ADD CONSTRAINT fk_assessment_bc_assessment
    FOREIGN KEY (assessment_id) REFERENCES projects.assessments(id) ON DELETE CASCADE;

ALTER TABLE projects.assessment_bc DROP CONSTRAINT IF EXISTS uq_assessment_bc;
ALTER TABLE projects.assessment_bc
    ADD CONSTRAINT uq_assessment_bc UNIQUE (assessment_id, bc_id);

CREATE INDEX IF NOT EXISTS idx_assessment_bc_assessment ON projects.assessment_bc(assessment_id);

COMMENT ON TABLE  projects.assessment_bc IS 'Связь оценки с бизнес-способностями (BC)';
COMMENT ON COLUMN projects.assessment_bc.id IS 'Суррогатный ключ (int4, автоинкремент)';
COMMENT ON COLUMN projects.assessment_bc.assessment_id IS 'FK на assessments.id';
COMMENT ON COLUMN projects.assessment_bc.bc_id IS 'ID бизнес-способности в справочнике BC другого сервиса (FK не создаётся — таблица в другой БД)';
COMMENT ON COLUMN projects.assessment_bc.relevance IS 'Релевантность BC (0-10, оценка LLM)';
COMMENT ON COLUMN projects.assessment_bc.reason IS 'Обоснование выбора BC (от LLM)';


CREATE TABLE IF NOT EXISTS projects.assessment_tc (
    id                       int4 PRIMARY KEY DEFAULT nextval('projects.assessment_tc_id_seq'::regclass),
    assessment_id            int4         NOT NULL,
    tc_id  int4         NOT NULL,
    fr_ids                   jsonb
);

ALTER TABLE projects.assessment_tc DROP CONSTRAINT IF EXISTS fk_existing_tcs_assessment;
ALTER TABLE projects.assessment_tc
    ADD CONSTRAINT fk_existing_tcs_assessment
    FOREIGN KEY (assessment_id) REFERENCES projects.assessments(id) ON DELETE CASCADE;

ALTER TABLE projects.assessment_tc DROP CONSTRAINT IF EXISTS uq_existing_tc;
ALTER TABLE projects.assessment_tc
    ADD CONSTRAINT uq_existing_tc UNIQUE (assessment_id, tc_id);

CREATE INDEX IF NOT EXISTS idx_existing_tcs_assessment ON projects.assessment_tc(assessment_id);

COMMENT ON TABLE  projects.assessment_tc IS 'Существующие технические возможности — ссылка на TC в BeeAtlas';
COMMENT ON COLUMN projects.assessment_tc.id IS 'Суррогатный ключ (int4, автоинкремент)';
COMMENT ON COLUMN projects.assessment_tc.assessment_id IS 'FK на assessments.id';
COMMENT ON COLUMN projects.assessment_tc.tc_id IS 'ID технической возможности в BeeAtlas (FK не создаётся — таблица в другом сервисе)';
COMMENT ON COLUMN projects.assessment_tc.fr_ids IS 'Массив unique_ident требований, покрываемых TC: ["FR-1","FR-2","NFR-3"] (jsonb)';


CREATE TABLE IF NOT EXISTS projects.assessment_tc_design (
    id               int4 PRIMARY KEY DEFAULT nextval('projects.assessment_new_tcs_id_seq'::regclass),
    assessment_id    int4         NOT NULL,
    name             text         NOT NULL,
    description      text,
    fr_ids           jsonb
);

ALTER TABLE projects.assessment_tc_design DROP CONSTRAINT IF EXISTS fk_new_tcs_assessment;
ALTER TABLE projects.assessment_tc_design
    ADD CONSTRAINT fk_new_tcs_assessment
    FOREIGN KEY (assessment_id) REFERENCES projects.assessments(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_new_tcs_assessment ON projects.assessment_tc_design(assessment_id);

COMMENT ON TABLE  projects.assessment_tc_design IS 'Новые технические возможности — нет в BeeAtlas, создаются пользователем';
COMMENT ON COLUMN projects.assessment_tc_design.id IS 'Суррогатный ключ (int4, автоинкремент)';
COMMENT ON COLUMN projects.assessment_tc_design.assessment_id IS 'FK на assessments.id';
COMMENT ON COLUMN projects.assessment_tc_design.name IS 'Название новой TC';
COMMENT ON COLUMN projects.assessment_tc_design.description IS 'Описание новой TC';
COMMENT ON COLUMN projects.assessment_tc_design.fr_ids IS 'Массив unique_ident требований, покрываемых TC: ["FR-1","FR-2","NFR-3"] (jsonb)';


CREATE INDEX IF NOT EXISTS idx_assessments_owner   ON projects.assessments(owner_id);
CREATE INDEX IF NOT EXISTS idx_assessments_status  ON projects.assessments(status_id);
CREATE INDEX IF NOT EXISTS idx_assessments_project ON projects.assessments(project_id);
