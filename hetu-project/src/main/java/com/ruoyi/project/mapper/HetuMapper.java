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
    @Select("SELECT p.project_id id, p.project_code code, p.project_name name, p.owner, p.start_date startDate, p.end_date endDate, p.status, p.priority, p.description, COALESCE(ROUND((SELECT AVG(progress) FROM hetu_task t WHERE t.project_id=p.project_id)),0) progress, p.current_stage stage, p.updated_at updatedAt FROM hetu_project p ORDER BY p.updated_at DESC")
    List<Map<String,Object>> projects();
    @Insert("INSERT INTO hetu_project(project_code,project_name,owner,start_date,end_date,status,priority,description) VALUES(#{code},#{name},#{owner},#{startDate},#{endDate},#{status},#{priority},#{description})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addProject(Map<String,Object> row);
    @Update("UPDATE hetu_project SET project_name=#{name}, owner=#{owner}, start_date=#{startDate}, end_date=#{endDate}, status=#{status}, priority=#{priority}, description=#{description} WHERE project_id=#{id}")
    int updateProject(Map<String,Object> row);
    @Select("SELECT task_id id, project_id projectId, requirement_id requirementId, title, description, assignee, estimate_hours estimateHours, progress, status, priority, due_date dueDate, (SELECT COUNT(*) FROM hetu_bug b WHERE b.task_id=t.task_id AND b.status<>'CLOSED') bugCount FROM hetu_task t ORDER BY FIELD(status,'TODO','IN_PROGRESS','TESTING','DONE'), due_date")
    List<Map<String,Object>> tasks();
    @Insert("INSERT INTO hetu_task(project_id,requirement_id,title,description,assignee,estimate_hours,progress,status,priority,due_date) VALUES(#{projectId},#{requirementId},#{title},#{description},#{assignee},#{estimateHours},#{progress},#{status},#{priority},#{dueDate})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addTask(Map<String,Object> row);
    @Update("UPDATE hetu_task SET title=#{title},description=#{description},assignee=#{assignee},estimate_hours=#{estimateHours},status=#{status},priority=#{priority},due_date=#{dueDate},project_id=#{projectId},requirement_id=#{requirementId} WHERE task_id=#{id}")
    int updateTask(Map<String,Object> row);
    @Select("SELECT requirement_id id, project_id projectId, title, content, status, owner, release_version version, updated_at updatedAt FROM hetu_requirement ORDER BY updated_at DESC")
    List<Map<String,Object>> requirements();
    @Insert("INSERT INTO hetu_requirement(project_id,title,content,status,owner,release_version) VALUES(#{projectId},#{title},#{content},#{status},#{owner},#{version})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addRequirement(Map<String,Object> row);
    @Update("UPDATE hetu_requirement SET title=#{title},content=#{content},status=#{status},owner=#{owner},release_version=#{version},project_id=#{projectId} WHERE requirement_id=#{id}")
    int updateRequirement(Map<String,Object> row);
    @Select("SELECT bug_id id, task_id taskId, title, bug_type type, severity, description, steps, assignee, status, root_cause rootCause, score_hours scoreHours, created_at createdAt FROM hetu_bug ORDER BY FIELD(status,'NEW','IN_PROGRESS','VERIFYING','CLOSED'), created_at DESC")
    List<Map<String,Object>> bugs();
    @Insert("INSERT INTO hetu_bug(task_id,title,bug_type,severity,description,steps,assignee,status,root_cause,score_hours) VALUES(#{taskId},#{title},#{type},#{severity},#{description},#{steps},#{assignee},#{status},#{rootCause},#{scoreHours})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addBug(Map<String,Object> row);
    @Update("UPDATE hetu_bug SET task_id=#{taskId},title=#{title},bug_type=#{type},severity=#{severity},description=#{description},steps=#{steps},assignee=#{assignee},status=#{status},root_cause=#{rootCause},score_hours=#{scoreHours} WHERE bug_id=#{id}")
    int updateBug(Map<String,Object> row);
    @Select("SELECT estimate_hours FROM hetu_task WHERE task_id=#{taskId}") BigDecimal taskEstimateHours(@Param("taskId") long taskId);
    @Insert("INSERT INTO hetu_attachment(entity_type,entity_id,file_name,storage_path,mime_type,file_size,uploaded_by) VALUES(#{entityType},#{entityId},#{fileName},#{storagePath},#{mimeType},#{fileSize},#{uploadedBy})")
    @Options(useGeneratedKeys=true,keyProperty="id") int addAttachment(Map<String,Object> row);
    @Select("SELECT progress FROM hetu_task WHERE task_id=#{id} FOR UPDATE") Integer currentProgress(@Param("id") long id);
    @Update("UPDATE hetu_task SET progress=#{progress}, status=CASE WHEN #{progress}=100 AND status='IN_PROGRESS' THEN 'TESTING' ELSE status END WHERE task_id=#{id}") int progress(@Param("id") long id,@Param("progress") int progress);
    @Update("UPDATE hetu_task SET status=#{status} WHERE task_id=#{id} AND (#{status}<>'TESTING' OR progress=100) AND (#{status}<>'DONE' OR status='TESTING')") int setTaskStatus(@Param("id") long id,@Param("status") String status);
    @Insert("INSERT INTO hetu_task_progress(task_id,progress,delta_hours,actor,note) SELECT #{id},#{progress},estimate_hours*(#{progress}-progress)/100,#{actor},#{note} FROM hetu_task WHERE task_id=#{id}") int recordProgress(@Param("id") long id,@Param("progress") int progress,@Param("actor") String actor,@Param("note") String note);
    @Insert("INSERT INTO hetu_operation_log(actor,entity_type,entity_id,action,detail) VALUES(#{actor},#{type},#{id},#{action},#{detail})") int log(@Param("actor") String actor,@Param("type") String type,@Param("id") long id,@Param("action") String action,@Param("detail") String detail);
    @Select("SELECT (SELECT COUNT(*) FROM hetu_task WHERE status<>'DONE') openTasks,(SELECT COUNT(*) FROM hetu_task WHERE due_date<CURDATE() AND status<>'DONE') overdueTasks,(SELECT COUNT(*) FROM hetu_project WHERE status='ACTIVE') activeProjects,(SELECT COUNT(*) FROM hetu_bug WHERE status<>'CLOSED') openBugs,(SELECT COALESCE(ROUND(AVG(project_progress)),0) FROM (SELECT AVG(progress) project_progress FROM hetu_task GROUP BY project_id) p) avgProgress,(SELECT COALESCE(SUM(estimate_hours*progress/100),0) FROM hetu_task) completedHours")
    Map<String,Object> dashboard();
    @Select("SELECT status,COUNT(*) count FROM hetu_task GROUP BY status") List<Map<String,Object>> taskStats();
    @Select("SELECT DATE_FORMAT(created_at,'%m-%d') day,COUNT(*) count FROM hetu_bug WHERE created_at>=DATE_SUB(CURDATE(),INTERVAL 6 DAY) GROUP BY DATE_FORMAT(created_at,'%m-%d') ORDER BY MIN(DATE(created_at))") List<Map<String,Object>> bugTrend();
    @Select("SELECT actor,ROUND(SUM(delta_hours),1) hours FROM hetu_task_progress WHERE created_at>=DATE_SUB(CURDATE(),INTERVAL 30 DAY) GROUP BY actor ORDER BY hours DESC LIMIT 6") List<Map<String,Object>> contributions();
}
