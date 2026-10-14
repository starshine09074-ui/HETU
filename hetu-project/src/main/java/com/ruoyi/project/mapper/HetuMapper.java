package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HetuMapper {
    @Select("SELECT nick_name FROM sys_user WHERE user_name=#{username} AND del_flag='0' LIMIT 1") String userDisplayName(@Param("username") String username);
    @Select("SELECT p.project_id id, p.project_code code, p.project_name name, p.owner, p.start_date startDate, p.end_date endDate, p.status, p.priority, p.description, COALESCE(ROUND((SELECT AVG(progress) FROM hetu_task t WHERE t.project_id=p.project_id)),0) progress, p.current_stage stage, p.updated_at updatedAt FROM hetu_project p ORDER BY p.updated_at DESC")
    List<Map<String,Object>> projects();
    @Insert("INSERT INTO hetu_project(project_code,project_name,owner,start_date,end_date,status,priority,description) VALUES(#{code},#{name},#{owner},#{startDate},#{endDate},#{status},#{priority},#{description})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addProject(Map<String,Object> row);
    @Update("UPDATE hetu_project SET project_name=#{name}, owner=#{owner}, start_date=#{startDate}, end_date=#{endDate}, status=#{status}, priority=#{priority}, description=#{description} WHERE project_id=#{id}")
    int updateProject(Map<String,Object> row);
    @Select("SELECT task_id id, project_id projectId, requirement_id requirementId, title, description, assignee, tester, estimate_hours estimateHours, progress, status, priority, due_date dueDate, (SELECT COUNT(*) FROM hetu_bug b WHERE b.task_id=t.task_id AND b.status<>'CLOSED') bugCount FROM hetu_task t ORDER BY FIELD(status,'TODO','IN_PROGRESS','TESTING','DONE'), due_date")
    List<Map<String,Object>> tasks();
    @Insert("INSERT INTO hetu_task(project_id,requirement_id,title,description,assignee,estimate_hours,progress,status,priority,due_date) VALUES(#{projectId},#{requirementId},#{title},#{description},#{assignee},#{estimateHours},#{progress},#{status},#{priority},#{dueDate})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addTask(Map<String,Object> row);
    @Update("UPDATE hetu_task SET title=#{title},description=#{description},assignee=#{assignee},estimate_hours=#{estimateHours},status=#{status},priority=#{priority},due_date=#{dueDate},project_id=#{projectId},requirement_id=#{requirementId} WHERE task_id=#{id}")
    int updateTask(Map<String,Object> row);
    @Update("UPDATE hetu_task SET assignee=#{actor} WHERE task_id=#{id} AND (assignee IS NULL OR assignee='') AND status='TODO'") int claimTask(@Param("id") long id,@Param("actor") String actor);
    @Update("UPDATE hetu_task SET tester=#{actor} WHERE task_id=#{id} AND status='TESTING' AND (tester IS NULL OR tester='')") int acceptTesting(@Param("id") long id,@Param("actor") String actor);
    @Select("SELECT requirement_id id, project_id projectId, title, content, status, owner, release_version version, updated_at updatedAt FROM hetu_requirement ORDER BY updated_at DESC")
    List<Map<String,Object>> requirements();
    @Insert("INSERT INTO hetu_requirement(project_id,title,content,status,owner,release_version) VALUES(#{projectId},#{title},#{content},#{status},#{owner},#{version})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addRequirement(Map<String,Object> row);
    @Update("UPDATE hetu_requirement SET title=#{title},content=#{content},status=#{status},owner=#{owner},release_version=#{version},project_id=#{projectId} WHERE requirement_id=#{id}")
    int updateRequirement(Map<String,Object> row);
    @Select("SELECT status FROM hetu_requirement WHERE requirement_id=#{id}") String requirementStatus(@Param("id") long id);
    @Update("UPDATE hetu_requirement SET status=#{status} WHERE requirement_id=#{id}") int setRequirementStatus(@Param("id") long id,@Param("status") String status);
    @Select("SELECT bug_id id, task_id taskId, title, bug_type type, severity, description, steps, assignee, status, root_cause rootCause, score_hours scoreHours, created_at createdAt FROM hetu_bug ORDER BY FIELD(status,'NEW','IN_PROGRESS','VERIFYING','CLOSED'), created_at DESC")
    List<Map<String,Object>> bugs();
    @Insert("INSERT INTO hetu_bug(task_id,title,bug_type,severity,description,steps,assignee,status,root_cause,score_hours) VALUES(#{taskId},#{title},#{type},#{severity},#{description},#{steps},#{assignee},#{status},#{rootCause},#{scoreHours})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addBug(Map<String,Object> row);
    @Update("UPDATE hetu_bug SET task_id=#{taskId},title=#{title},bug_type=#{type},severity=#{severity},description=#{description},steps=#{steps},assignee=#{assignee},status=#{status},root_cause=#{rootCause},score_hours=#{scoreHours} WHERE bug_id=#{id}")
    int updateBug(Map<String,Object> row);
    @Update("UPDATE hetu_bug SET assignee=#{assignee} WHERE bug_id=#{id}") int updateBugAssignee(@Param("id") long id,@Param("assignee") String assignee);
    @Select("SELECT status FROM hetu_bug WHERE bug_id=#{id}") String bugStatus(@Param("id") long id);
    @Select("SELECT bug_id id,task_id taskId,severity,root_cause rootCause,status FROM hetu_bug WHERE bug_id=#{id}") Map<String,Object> bugStatusInfo(@Param("id") long id);
    @Update("UPDATE hetu_bug SET status=#{status},score_hours=#{scoreHours} WHERE bug_id=#{id}") int setBugStatus(@Param("id") long id,@Param("status") String status,@Param("scoreHours") BigDecimal scoreHours);
    @Select("SELECT estimate_hours FROM hetu_task WHERE task_id=#{taskId}") BigDecimal taskEstimateHours(@Param("taskId") long taskId);
    @Insert("INSERT INTO hetu_attachment(entity_type,entity_id,file_name,storage_path,mime_type,file_size,uploaded_by) VALUES(#{entityType},#{entityId},#{fileName},#{storagePath},#{mimeType},#{fileSize},#{uploadedBy})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addAttachment(Map<String,Object> row);
    @Select("SELECT progress FROM hetu_task WHERE task_id=#{id} FOR UPDATE") Integer currentProgress(@Param("id") long id);
    @Update("UPDATE hetu_task SET progress=#{progress}, status=CASE WHEN #{progress}=100 AND status='IN_PROGRESS' THEN 'TESTING' ELSE status END WHERE task_id=#{id}") int progress(@Param("id") long id,@Param("progress") int progress);
    @Update("UPDATE hetu_task SET status=#{status} WHERE task_id=#{id} AND (#{status}<>'TESTING' OR progress=100) AND (#{status}<>'DONE' OR status='TESTING')") int setTaskStatus(@Param("id") long id,@Param("status") String status);
    @Select("SELECT status FROM hetu_task WHERE task_id=#{id}") String taskStatus(@Param("id") long id);
    @Insert("INSERT INTO hetu_task_progress(task_id,progress,delta_hours,actor,note,work_date) SELECT #{id},#{progress},0,#{actor},#{note},#{workDate} FROM hetu_task WHERE task_id=#{id}") int recordProgress(@Param("id") long id,@Param("progress") int progress,@Param("actor") String actor,@Param("note") String note,@Param("workDate") String workDate);
    @Insert("INSERT INTO hetu_task_progress(task_id,progress,delta_hours,actor,note,work_date) SELECT task_id,progress,#{hours},#{actor},#{note},#{workDate} FROM hetu_task WHERE task_id=#{id} AND assignee=#{actor} AND status='IN_PROGRESS'") int recordWorkHours(@Param("id") long id,@Param("hours") BigDecimal hours,@Param("actor") String actor,@Param("note") String note,@Param("workDate") String workDate);
    @Select("SELECT p.progress_id id,p.task_id taskId,t.title taskTitle,p.progress,p.delta_hours hours,p.actor,p.note,p.work_date workDate FROM hetu_task_progress p JOIN hetu_task t ON t.task_id=p.task_id WHERE p.work_date BETWEEN #{from} AND #{to} AND (#{projectId} IS NULL OR t.project_id=#{projectId}) ORDER BY p.work_date DESC,p.progress_id DESC") List<Map<String,Object>> workLogs(@Param("from") String from,@Param("to") String to,@Param("projectId") Long projectId);
    @Select("SELECT p.actor,ROUND(SUM(p.delta_hours),2) hours,COUNT(*) submissions FROM hetu_task_progress p JOIN hetu_task t ON t.task_id=p.task_id WHERE p.work_date BETWEEN #{from} AND #{to} AND (#{projectId} IS NULL OR t.project_id=#{projectId}) GROUP BY p.actor ORDER BY hours DESC,p.actor") List<Map<String,Object>> workRanking(@Param("from") String from,@Param("to") String to,@Param("projectId") Long projectId);
    @Insert("INSERT INTO hetu_operation_log(actor,entity_type,entity_id,action,detail) VALUES(#{actor},#{type},#{id},#{action},#{detail})") int log(@Param("actor") String actor,@Param("type") String type,@Param("id") long id,@Param("action") String action,@Param("detail") String detail);
    @Select("SELECT (SELECT COUNT(*) FROM hetu_task WHERE status<>'DONE') openTasks,(SELECT COUNT(*) FROM hetu_task WHERE due_date<CURDATE() AND status<>'DONE') overdueTasks,(SELECT COUNT(*) FROM hetu_project WHERE status='ACTIVE') activeProjects,(SELECT COUNT(*) FROM hetu_bug WHERE status<>'CLOSED') openBugs,(SELECT COALESCE(ROUND(AVG(project_progress)),0) FROM (SELECT AVG(progress) project_progress FROM hetu_task GROUP BY project_id) p) avgProgress,(SELECT COALESCE(SUM(delta_hours),0) FROM hetu_task_progress) completedHours,(SELECT COUNT(*) FROM hetu_project WHERE created_at>=DATE_FORMAT(CURDATE(),'%Y-%m-01')) newProjectsThisMonth")
    Map<String,Object> dashboard();
    @Select("SELECT status,COUNT(*) count FROM hetu_task GROUP BY status") List<Map<String,Object>> taskStats();
    @Select("SELECT DATE_FORMAT(created_at,'%m-%d') day,COUNT(*) count FROM hetu_bug WHERE created_at>=DATE_SUB(CURDATE(),INTERVAL 6 DAY) GROUP BY DATE_FORMAT(created_at,'%m-%d') ORDER BY MIN(DATE(created_at))") List<Map<String,Object>> bugTrend();
    @Select("SELECT actor,ROUND(SUM(delta_hours),1) hours FROM hetu_task_progress WHERE created_at>=DATE_SUB(CURDATE(),INTERVAL 30 DAY) GROUP BY actor ORDER BY hours DESC LIMIT 6") List<Map<String,Object>> contributions();
    @Select("SELECT version_id id,project_id projectId,version_name name,description,start_date startDate,release_date releaseDate,status,test_submitted_by testSubmittedBy,test_submitted_at testSubmittedAt,released_by releasedBy,released_at releasedAt,verified_by verifiedBy,verified_at verifiedAt,verification_note verificationNote,created_at createdAt FROM hetu_release_version WHERE project_id=#{projectId} ORDER BY release_date DESC,version_id DESC") List<Map<String,Object>> releases(@Param("projectId") long projectId);
    @Insert("INSERT INTO hetu_release_version(project_id,version_name,description,start_date,release_date,status) VALUES(#{projectId},#{name},#{description},#{startDate},#{releaseDate},#{status})") @Options(useGeneratedKeys=true,keyProperty="id") int addRelease(Map<String,Object> row);
    @Select("SELECT item_id id,entity_type entityType,entity_id entityId,title,status FROM (SELECT i.item_id,i.entity_type,i.entity_id,CASE WHEN i.entity_type='requirement' THEN r.title ELSE b.title END title,CASE WHEN i.entity_type='requirement' THEN r.status ELSE b.status END status FROM hetu_release_item i LEFT JOIN hetu_requirement r ON i.entity_type='requirement' AND r.requirement_id=i.entity_id LEFT JOIN hetu_bug b ON i.entity_type='bug' AND b.bug_id=i.entity_id WHERE i.version_id=#{versionId}) q ORDER BY entity_type,title") List<Map<String,Object>> releaseItems(@Param("versionId") long versionId);
    @Insert("INSERT IGNORE INTO hetu_release_item(version_id,entity_type,entity_id) VALUES(#{versionId},#{entityType},#{entityId})") int addReleaseItem(@Param("versionId") long versionId,@Param("entityType") String entityType,@Param("entityId") long entityId);
    @Select("SELECT version_id id,project_id projectId,status,verified_by verifiedBy FROM hetu_release_version WHERE version_id=#{id}") Map<String,Object> release(@Param("id") long id);
    @Select("SELECT verified_by FROM hetu_release_version WHERE project_id=#{projectId} AND status='NEEDS_IMPROVEMENT' AND verified_by IS NOT NULL ORDER BY version_id DESC LIMIT 1") String pendingReleaseVerifier(@Param("projectId") long projectId);
    @Update("UPDATE hetu_release_version SET status=#{status},test_submitted_by=#{actor},test_submitted_at=NOW() WHERE version_id=#{id} AND status='PLANNED'") int submitReleaseTest(@Param("id") long id,@Param("actor") String actor);
    @Update("UPDATE hetu_release_version SET status='RELEASED',released_by=#{actor},released_at=NOW() WHERE version_id=#{id} AND status='READY'") int publishRelease(@Param("id") long id,@Param("actor") String actor);
    @Update("UPDATE hetu_release_version SET status=#{status},verified_by=#{actor},verified_at=NOW(),verification_note=#{note} WHERE version_id=#{id} AND status='RELEASED'") int verifyRelease(@Param("id") long id,@Param("status") String status,@Param("actor") String actor,@Param("note") String note);
    @Update("UPDATE hetu_release_version SET status='CLOSED' WHERE version_id=#{id} AND status IN ('NEEDS_IMPROVEMENT','VERIFIED')") int closeRelease(@Param("id") long id);
    @Update("UPDATE hetu_release_version SET release_date=#{releaseDate} WHERE version_id=#{id} AND status IN ('PLANNED','READY')") int scheduleRelease(@Param("id") long id,@Param("releaseDate") String releaseDate);
    @Select("SELECT deduction_id id,person,category,source_type sourceType,source_id sourceId,hours,note,occurred_on occurredOn,created_at createdAt FROM hetu_work_deduction WHERE occurred_on BETWEEN #{from} AND #{to} AND (#{projectId} IS NULL OR project_id=#{projectId}) ORDER BY occurred_on DESC,deduction_id DESC") List<Map<String,Object>> deductions(@Param("from") String from,@Param("to") String to,@Param("projectId") Long projectId);
    @Insert("INSERT INTO hetu_work_deduction(person,project_id,category,source_type,source_id,hours,note,occurred_on) VALUES(#{person},#{projectId},#{category},#{sourceType},#{sourceId},#{hours},#{note},#{occurredOn}) ON DUPLICATE KEY UPDATE person=VALUES(person),project_id=VALUES(project_id),category=VALUES(category),hours=VALUES(hours),note=VALUES(note),occurred_on=VALUES(occurred_on)") int addDeduction(Map<String,Object> row);
    @Select("SELECT b.task_id taskId,t.project_id projectId,b.assignee FROM hetu_bug b LEFT JOIN hetu_task t ON t.task_id=b.task_id WHERE b.bug_id=#{id}") Map<String,Object> bugAssignment(@Param("id") long id);
}
