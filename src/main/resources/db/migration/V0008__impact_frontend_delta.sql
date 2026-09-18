
ALTER TABLE projects.assessment_tc ADD COLUMN IF NOT EXISTS tc_code text;
ALTER TABLE projects.assessment_tc ADD COLUMN IF NOT EXISTS product_alias text;
ALTER TABLE projects.assessment_tc ADD COLUMN IF NOT EXISTS product_name text;
ALTER TABLE projects.assessment_tc ADD COLUMN IF NOT EXISTS parent_bc_code text;

ALTER TABLE projects.assessment_tc DROP CONSTRAINT IF EXISTS uq_existing_tc;
ALTER TABLE projects.assessment_tc
    ADD CONSTRAINT uq_existing_tc UNIQUE (assessment_id, tc_code);

ALTER TABLE projects.assessment_tc DROP COLUMN IF EXISTS tc_id;

COMMENT ON COLUMN projects.assessment_tc.tc_code IS 'Код технической возможности из ландшафта (fdm-search / landscape analyze)';
COMMENT ON COLUMN projects.assessment_tc.product_alias IS 'Код системы из результата поиска на этапе TC';
COMMENT ON COLUMN projects.assessment_tc.product_name IS 'Наименование системы';
COMMENT ON COLUMN projects.assessment_tc.parent_bc_code IS 'Код родительской бизнес-возможности (из landscape analyze)';

COMMENT ON TABLE projects.assessment_tc IS 'Существующие технические возможности (reuse) — код из ландшафта (fdm-search)';

ALTER TABLE projects.assessment_tc_design ADD COLUMN IF NOT EXISTS product_alias text;
ALTER TABLE projects.assessment_tc_design ADD COLUMN IF NOT EXISTS product_name text;
ALTER TABLE projects.assessment_tc_design ADD COLUMN IF NOT EXISTS parent_bc_code text;

COMMENT ON COLUMN projects.assessment_tc_design.product_alias IS 'Код системы, выбранный аналитиком (GET /api/tc/systems)';
COMMENT ON COLUMN projects.assessment_tc_design.product_name IS 'Наименование системы';
COMMENT ON COLUMN projects.assessment_tc_design.parent_bc_code IS 'Код родительской бизнес-возможности (опционально)';

ALTER TABLE projects.assessment_bc DROP CONSTRAINT IF EXISTS fk_assessment_bc_assessment;
ALTER TABLE projects.assessment_bc DROP CONSTRAINT IF EXISTS uq_assessment_bc;
DROP TABLE IF EXISTS projects.assessment_bc;
DROP SEQUENCE IF EXISTS projects.assessment_bc_id_seq;

UPDATE projects.assessments
SET status_id = (SELECT id FROM projects.assessment_status_enum WHERE name = 'TC')
WHERE status_id = (SELECT id FROM projects.assessment_status_enum WHERE name = 'BC');

DELETE FROM projects.assessment_status_enum WHERE name = 'BC';

COMMENT ON COLUMN projects.assessments.impact_level IS 'Уровень влияния: S | M | L | XL';
