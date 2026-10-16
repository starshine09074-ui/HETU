SET NAMES utf8mb4;
-- LOCAL DEMO ONLY. Imports users with the published password admin123.
-- Never run this file against a shared or production database.
UPDATE sys_user SET status='0',password='$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2' WHERE user_name IN ('admin','ry') AND del_flag='0';

INSERT IGNORE INTO sys_user(user_id,dept_id,user_name,nick_name,user_type,email,phonenumber,sex,password,status,del_flag,create_by,create_time,remark) VALUES
(110,103,'pm','产品经理','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'本地演示账号'),
(111,103,'director','技术总监','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'本地演示账号'),
(112,103,'developer','开发人员','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'本地演示账号'),
(113,103,'tester','测试人员','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'本地演示账号'),
(114,103,'ops','运维工程师','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'本地演示账号'),
(115,103,'frontend','前端开发','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'本地演示账号'),
(116,103,'backend','后端开发','00','','','0','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','admin',NOW(),'本地演示账号');

INSERT IGNORE INTO sys_user_role(user_id,role_id) VALUES
(110,10),(111,11),(112,12),(113,13),(114,14),(115,12),(116,12);
