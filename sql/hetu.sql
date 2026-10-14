SET NAMES utf8mb4;
-- Hetu business schema. Import after sql/ry_20260417.sql into the same database.
CREATE TABLE IF NOT EXISTS hetu_project (
  project_id BIGINT NOT NULL AUTO_INCREMENT, project_code VARCHAR(40) NOT NULL, project_name VARCHAR(120) NOT NULL,
  owner VARCHAR(64), start_date DATE, end_date DATE, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM', description TEXT, current_stage VARCHAR(40) DEFAULT '需求规划',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(project_id), UNIQUE KEY uk_hetu_project_code(project_code), KEY idx_hetu_project_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hetu_release_version (
  version_id BIGINT NOT NULL AUTO_INCREMENT, project_id BIGINT NOT NULL, version_name VARCHAR(80) NOT NULL,
  description TEXT, start_date DATE, release_date DATE, status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(version_id), KEY idx_hetu_version_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hetu_requirement (
  requirement_id BIGINT NOT NULL AUTO_INCREMENT, project_id BIGINT, title VARCHAR(180) NOT NULL, content LONGTEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', owner VARCHAR(64), release_version VARCHAR(80),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(requirement_id), KEY idx_hetu_requirement_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hetu_task (
  task_id BIGINT NOT NULL AUTO_INCREMENT, project_id BIGINT, requirement_id BIGINT, title VARCHAR(180) NOT NULL, description TEXT,
  assignee VARCHAR(64), estimate_hours DECIMAL(8,2) NOT NULL DEFAULT 0, progress TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'TODO', priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM', due_date DATE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(task_id), KEY idx_hetu_task_project(project_id), KEY idx_hetu_task_status(status), KEY idx_hetu_task_due(due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hetu_task_progress (
  progress_id BIGINT NOT NULL AUTO_INCREMENT, task_id BIGINT NOT NULL, progress TINYINT NOT NULL, delta_hours DECIMAL(8,2) NOT NULL DEFAULT 0,
  actor VARCHAR(64) NOT NULL, note VARCHAR(500), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(progress_id), KEY idx_hetu_progress_task(task_id), KEY idx_hetu_progress_actor(actor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hetu_bug (
  bug_id BIGINT NOT NULL AUTO_INCREMENT, task_id BIGINT, title VARCHAR(180) NOT NULL, bug_type VARCHAR(24) NOT NULL DEFAULT 'FUNCTIONAL',
  severity VARCHAR(16) NOT NULL DEFAULT 'MEDIUM', description TEXT, steps TEXT, assignee VARCHAR(64),
  status VARCHAR(20) NOT NULL DEFAULT 'NEW', root_cause VARCHAR(32) NOT NULL DEFAULT 'PRODUCT_DEFECT',
  score_hours DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(bug_id), KEY idx_hetu_bug_task(task_id), KEY idx_hetu_bug_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET @has_root_cause = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hetu_bug' AND COLUMN_NAME='root_cause');
SET @sql = IF(@has_root_cause=0, 'ALTER TABLE hetu_bug ADD COLUMN root_cause VARCHAR(32) NOT NULL DEFAULT ''PRODUCT_DEFECT''', 'SELECT 1');
PREPARE hetu_stmt FROM @sql; EXECUTE hetu_stmt; DEALLOCATE PREPARE hetu_stmt;
SET @has_score_hours = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hetu_bug' AND COLUMN_NAME='score_hours');
SET @sql = IF(@has_score_hours=0, 'ALTER TABLE hetu_bug ADD COLUMN score_hours DECIMAL(8,2) NOT NULL DEFAULT 0.00', 'SELECT 1');
PREPARE hetu_stmt FROM @sql; EXECUTE hetu_stmt; DEALLOCATE PREPARE hetu_stmt;

CREATE TABLE IF NOT EXISTS hetu_comment (
  comment_id BIGINT NOT NULL AUTO_INCREMENT, entity_type VARCHAR(24) NOT NULL, entity_id BIGINT NOT NULL,
  content TEXT NOT NULL, author VARCHAR(64) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(comment_id), KEY idx_hetu_comment_entity(entity_type,entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hetu_attachment (
  attachment_id BIGINT NOT NULL AUTO_INCREMENT, entity_type VARCHAR(24) NOT NULL, entity_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL, storage_path VARCHAR(500) NOT NULL, mime_type VARCHAR(120), file_size BIGINT NOT NULL DEFAULT 0,
  uploaded_by VARCHAR(64), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(attachment_id), KEY idx_hetu_attachment_entity(entity_type,entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hetu_operation_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT, actor VARCHAR(64) NOT NULL, entity_type VARCHAR(24) NOT NULL,
  entity_id BIGINT NOT NULL, action VARCHAR(32) NOT NULL, detail VARCHAR(1000), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(log_id), KEY idx_hetu_log_entity(entity_type,entity_id), KEY idx_hetu_log_time(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO hetu_project(project_id,project_code,project_name,owner,start_date,end_date,status,priority,description,current_stage)
VALUES (1,'HT-2026-001','星河研发协作平台','林晓','2026-09-01','2026-12-20','ACTIVE','HIGH','统一管理研发需求、交付进度和质量反馈。','核心功能开发');
INSERT IGNORE INTO hetu_requirement(requirement_id,project_id,title,content,status,owner,release_version)
VALUES (1,1,'研发工作台与交付看板','# 目标\n让团队快速了解项目进度、我的待办和质量风险。\n\n## 验收标准\n- 支持看板拖拽\n- 进度变化自动记录贡献','APPROVED','林晓','v1.0');
INSERT IGNORE INTO hetu_task(task_id,project_id,requirement_id,title,description,assignee,estimate_hours,progress,status,priority,due_date)
VALUES (1,1,1,'搭建研发工作台','完善项目总览与关键指标','周可',24,80,'IN_PROGRESS','HIGH','2026-10-20'),
(2,1,1,'实现任务进度历史','记录每次累计进度并计算投入贡献','陈予',16,45,'IN_PROGRESS','MEDIUM','2026-10-24'),
(3,1,1,'补齐测试提交流程','完成开发到验收的状态流转','许安',12,0,'TODO','MEDIUM','2026-10-28');
INSERT IGNORE INTO hetu_bug(bug_id,task_id,title,bug_type,severity,description,steps,assignee,status)
VALUES (1,1,'移动端看板列宽显示不完整','UI','LOW','窄屏下列标题会被截断','将浏览器宽度缩至 900px','周可','IN_PROGRESS');

-- Give the built-in RuoYi administrator access to Hetu endpoints while retaining RuoYi auth/menu controls.
INSERT IGNORE INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
VALUES (2000,'河图工作台',0,5,'hetu','hetu/index',1,0,'C','0','0','hetu:dashboard:list','dashboard','admin',NOW(),'','', 'Hetu研发协作平台');
INSERT IGNORE INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
VALUES (2001,'项目查看',2000,1,'#','',1,0,'F','0','0','hetu:project:list','#','admin',NOW(),'','', ''),
(2002,'项目新增',2000,2,'#','',1,0,'F','0','0','hetu:project:add','#','admin',NOW(),'','', ''),
(2003,'项目编辑',2000,3,'#','',1,0,'F','0','0','hetu:project:edit','#','admin',NOW(),'','', ''),
(2004,'需求查看',2000,4,'#','',1,0,'F','0','0','hetu:requirement:list','#','admin',NOW(),'','', ''),
(2005,'需求新增',2000,5,'#','',1,0,'F','0','0','hetu:requirement:add','#','admin',NOW(),'','', ''),
(2006,'需求编辑',2000,6,'#','',1,0,'F','0','0','hetu:requirement:edit','#','admin',NOW(),'','', ''),
(2007,'任务查看',2000,7,'#','',1,0,'F','0','0','hetu:task:list','#','admin',NOW(),'','', ''),
(2008,'任务新增',2000,8,'#','',1,0,'F','0','0','hetu:task:add','#','admin',NOW(),'','', ''),
(2009,'任务编辑',2000,9,'#','',1,0,'F','0','0','hetu:task:edit','#','admin',NOW(),'','', ''),
(2010,'缺陷查看',2000,10,'#','',1,0,'F','0','0','hetu:bug:list','#','admin',NOW(),'','', ''),
(2011,'缺陷新增',2000,11,'#','',1,0,'F','0','0','hetu:bug:add','#','admin',NOW(),'','', ''),
(2012,'缺陷编辑',2000,12,'#','',1,0,'F','0','0','hetu:bug:edit','#','admin',NOW(),'','', ''),
(2013,'附件上传',2000,13,'#','',1,0,'F','0','0','hetu:attachment:upload','#','admin',NOW(),'','', '');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT 1,menu_id FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2013;

-- Four working roles with separate demo accounts (password for each: admin123).
INSERT IGNORE INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,status,del_flag,create_by,create_time,remark) VALUES
(10,'产品经理','hetu_pm',10,'1','0','0','admin',NOW(),'负责项目规划、需求与交付范围'),
(11,'技术总监','hetu_director',11,'1','0','0','admin',NOW(),'负责技术交付与团队工作流'),
(12,'开发人员','hetu_developer',12,'1','0','0','admin',NOW(),'负责研发任务与缺陷修复'),
(13,'测试人员','hetu_tester',13,'1','0','0','admin',NOW(),'负责测试验收与缺陷跟踪');

INSERT IGNORE INTO sys_user(user_id,dept_id,user_name,nick_name,user_type,email,phonenumber,sex,password,status,del_flag,create_by,create_time,remark) VALUES
(110,103,'pm','产品经理','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'河图演示账号'),
(111,103,'director','技术总监','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'河图演示账号'),
(112,103,'developer','开发人员','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'河图演示账号'),
(113,103,'tester','测试人员','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'河图演示账号');
INSERT IGNORE INTO sys_user_role(user_id,role_id) VALUES (110,10),(111,11),(112,12),(113,13);

-- Every role can open the workspace and read the shared work inventory.
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id IN (2000,2001,2004,2007,2010)
WHERE r.role_id BETWEEN 10 AND 13;
-- Product managers plan projects, requirements, and tasks.
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) VALUES (10,2002),(10,2003),(10,2005),(10,2006),(10,2008),(10,2009);
-- Directors have full workspace authority.
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT 11,menu_id FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2013;
-- Developers work on tasks and submit defects; testers triage and update defects.
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) VALUES (12,2009),(12,2011),(12,2013),(13,2009),(13,2011),(13,2012),(13,2013);
