SET @has_tester = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hetu_task' AND COLUMN_NAME='tester');
SET @sql = IF(@has_tester=0, 'ALTER TABLE hetu_task ADD COLUMN tester VARCHAR(64) NULL', 'SELECT 1');
PREPARE hetu_stmt FROM @sql; EXECUTE hetu_stmt; DEALLOCATE PREPARE hetu_stmt;

SET @has_work_date = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hetu_task_progress' AND COLUMN_NAME='work_date');
SET @sql = IF(@has_work_date=0, 'ALTER TABLE hetu_task_progress ADD COLUMN work_date DATE NULL', 'SELECT 1');
PREPARE hetu_stmt FROM @sql; EXECUTE hetu_stmt; DEALLOCATE PREPARE hetu_stmt;
UPDATE hetu_task_progress SET work_date=DATE(created_at) WHERE work_date IS NULL;
SET @has_work_date_index = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hetu_task_progress' AND INDEX_NAME='idx_hetu_progress_date');
SET @sql = IF(@has_work_date_index=0, 'ALTER TABLE hetu_task_progress MODIFY COLUMN work_date DATE NOT NULL DEFAULT ''1970-01-01'', ADD KEY idx_hetu_progress_date(work_date)', 'SELECT 1');
PREPARE hetu_stmt FROM @sql; EXECUTE hetu_stmt; DEALLOCATE PREPARE hetu_stmt;

CREATE TABLE IF NOT EXISTS hetu_release_item (
  item_id BIGINT NOT NULL AUTO_INCREMENT, version_id BIGINT NOT NULL, entity_type VARCHAR(20) NOT NULL, entity_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(item_id),
  UNIQUE KEY uk_hetu_release_item(version_id,entity_type,entity_id), KEY idx_hetu_release_item_version(version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hetu_work_deduction (
  deduction_id BIGINT NOT NULL AUTO_INCREMENT, person VARCHAR(64) NOT NULL, project_id BIGINT NULL, category VARCHAR(120) NOT NULL,
  source_type VARCHAR(20) NOT NULL, source_id BIGINT NULL, hours DECIMAL(8,2) NOT NULL, note VARCHAR(1000), occurred_on DATE NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(deduction_id), UNIQUE KEY uk_hetu_deduction_source(source_type,source_id),
  KEY idx_hetu_deduction_person_date(person,occurred_on), KEY idx_hetu_deduction_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
