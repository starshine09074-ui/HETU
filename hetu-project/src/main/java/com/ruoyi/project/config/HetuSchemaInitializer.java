package com.ruoyi.project.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class HetuSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public HetuSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!hasColumn("hetu_task", "tester")) {
            jdbc.execute("ALTER TABLE hetu_task ADD COLUMN tester VARCHAR(64) NULL");
        }
        if (!hasColumn("hetu_task_progress", "work_date")) {
            jdbc.execute("ALTER TABLE hetu_task_progress ADD COLUMN work_date DATE NULL");
        }
        jdbc.execute("UPDATE hetu_task_progress SET work_date=DATE(created_at) WHERE work_date IS NULL");
        jdbc.execute("ALTER TABLE hetu_task_progress MODIFY COLUMN work_date DATE NOT NULL DEFAULT '1970-01-01'");
        if (!hasIndex("hetu_task_progress", "idx_hetu_progress_date")) {
            jdbc.execute("ALTER TABLE hetu_task_progress ADD KEY idx_hetu_progress_date(work_date)");
        }
        jdbc.execute("CREATE TABLE IF NOT EXISTS hetu_release_item (item_id BIGINT NOT NULL AUTO_INCREMENT, version_id BIGINT NOT NULL, entity_type VARCHAR(20) NOT NULL, entity_id BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(item_id), UNIQUE KEY uk_hetu_release_item(version_id,entity_type,entity_id), KEY idx_hetu_release_item_version(version_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        jdbc.execute("CREATE TABLE IF NOT EXISTS hetu_work_deduction (deduction_id BIGINT NOT NULL AUTO_INCREMENT, person VARCHAR(64) NOT NULL, project_id BIGINT NULL, category VARCHAR(120) NOT NULL, source_type VARCHAR(20) NOT NULL, source_id BIGINT NULL, hours DECIMAL(8,2) NOT NULL, note VARCHAR(1000), occurred_on DATE NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(deduction_id), UNIQUE KEY uk_hetu_deduction_source(source_type,source_id), KEY idx_hetu_deduction_person_date(person,occurred_on), KEY idx_hetu_deduction_project(project_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        addColumn("hetu_release_version", "test_submitted_by", "VARCHAR(64) NULL");
        addColumn("hetu_release_version", "test_submitted_at", "DATETIME NULL");
        addColumn("hetu_release_version", "released_by", "VARCHAR(64) NULL");
        addColumn("hetu_release_version", "released_at", "DATETIME NULL");
        addColumn("hetu_release_version", "verified_by", "VARCHAR(64) NULL");
        addColumn("hetu_release_version", "verified_at", "DATETIME NULL");
        addColumn("hetu_release_version", "verification_note", "VARCHAR(1000) NULL");
        seedReleaseWorkflow();
    }

    private boolean hasColumn(String table, String column) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?", Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean hasIndex(String table, String index) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND INDEX_NAME=?", Integer.class, table, index);
        return count != null && count > 0;
    }

    private void addColumn(String table,String column,String definition){if(!hasColumn(table,column))jdbc.execute("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}
    private void seedReleaseWorkflow(){
        String hash="$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";
        jdbc.update("INSERT IGNORE INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,status,del_flag,create_by,create_time,remark) VALUES(14,'运维工程师','hetu_ops',14,'1','0','0','admin',NOW(),'负责发布与版本维护')");
        jdbc.update("INSERT IGNORE INTO sys_user(user_id,dept_id,user_name,nick_name,user_type,email,phonenumber,sex,password,status,del_flag,create_by,create_time,remark) VALUES(114,103,'ops','运维工程师','00','','','0',?,'0','0','admin',NOW(),'河图演示账号')",hash);
        jdbc.update("INSERT IGNORE INTO sys_user_role(user_id,role_id) VALUES(114,14)");
        jdbc.update("INSERT IGNORE INTO sys_user(user_id,dept_id,user_name,nick_name,user_type,email,phonenumber,sex,password,status,del_flag,create_by,create_time,remark) VALUES(115,103,'frontend','前端开发','00','','','0',?,'0','0','admin',NOW(),'河图演示账号'),(116,103,'backend','后端开发','00','','','0',?,'0','0','admin',NOW(),'河图演示账号')",hash,hash);
        jdbc.update("INSERT IGNORE INTO sys_user_role(user_id,role_id) VALUES(115,12),(116,12)");
        String[] perms={"hetu:release:add","hetu:release:schedule","hetu:release:test","hetu:release:publish","hetu:release:verify","hetu:release:close"};
        String[] names={"发布计划新增","发布排期修改","测试提交发布","执行版本发布","产品验证发布","手动关闭发布计划"};
        for(int i=0;i<perms.length;i++){int menuId=2014+i;jdbc.update("INSERT IGNORE INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark) VALUES(?,?,2000,?, '#','',1,0,'F','0','0',?,'#','admin',NOW(),'','', '')",menuId,names[i],14+i,perms[i]);}
        jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT role_id,2000 FROM sys_role WHERE role_key IN ('hetu_ops')");
        jdbc.update("DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.role_id=rm.role_id WHERE r.role_key='hetu_ops' AND rm.menu_id BETWEEN 2002 AND 2013");
        jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT role_id,menu_id FROM sys_role JOIN sys_menu ON sys_menu.menu_id IN (2001,2004,2007,2010) WHERE role_key='hetu_ops'");
        jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id IN (2014,2015,2017,2019) WHERE r.role_key='hetu_ops'");
        jdbc.update("DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.role_id=rm.role_id WHERE r.role_key='hetu_ops' AND rm.menu_id IN (2016,2018)");
        jdbc.update("DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.role_id=rm.role_id WHERE r.role_key='hetu_tester' AND rm.menu_id=2015");
        jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id=2016 WHERE r.role_key='hetu_tester'");
        jdbc.update("DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.role_id=rm.role_id WHERE r.role_key='hetu_pm' AND rm.menu_id IN (2014,2015,2019)");
        jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id=2018 WHERE r.role_key='hetu_pm'");
        jdbc.update("DELETE rm FROM sys_role_menu rm JOIN sys_role r ON r.role_id=rm.role_id WHERE r.role_key='hetu_pm' AND rm.menu_id IN (2002,2003,2008,2009)");
        jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT role_id,2013 FROM sys_role WHERE role_key IN ('hetu_pm','hetu_tester')");
        jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT role_id,2012 FROM sys_role WHERE role_key='hetu_developer'");
        jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT role_id,2011 FROM sys_role WHERE role_key='hetu_pm'");
        jdbc.update("INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id BETWEEN 2014 AND 2019 WHERE r.role_key='hetu_director'");
    }
}
