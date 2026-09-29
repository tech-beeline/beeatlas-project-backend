ALTER TABLE projects.use_case
    ADD COLUMN IF NOT EXISTS bi_step_code TEXT;

COMMENT ON COLUMN projects.use_case.bi_step_code IS 'Код шага бизнес-взаимодействия (biStepCode из публикации use case)';
