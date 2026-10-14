package com.ruoyi.project.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ruoyi.common.config.RuoYiConfig;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.mapper.HetuMapper;

@RestController
@RequestMapping("/hetu")
public class HetuController {
    private final HetuMapper mapper;
    public HetuController(HetuMapper mapper) { this.mapper = mapper; }

    @GetMapping("/dashboard") @PreAuthorize("@ss.hasPermi('hetu:dashboard:list')")
    public AjaxResult dashboard() {
        Map<String,Object> result = new LinkedHashMap<>(mapper.dashboard());
        result.put("taskStats", mapper.taskStats()); result.put("bugTrend", mapper.bugTrend()); result.put("contributions", mapper.contributions());
        result.put("projects", mapper.projects()); return AjaxResult.success(result);
    }
    @GetMapping("/projects") @PreAuthorize("@ss.hasPermi('hetu:project:list')") public AjaxResult projects() { return AjaxResult.success(mapper.projects()); }
    @PostMapping("/projects") @PreAuthorize("@ss.hasPermi('hetu:project:add')") public AjaxResult addProject(@RequestBody Map<String,Object> row) {
        required(row,"name"); row.putIfAbsent("code","HT-"+System.currentTimeMillis()); row.putIfAbsent("status","ACTIVE"); row.putIfAbsent("priority","MEDIUM");
        mapper.addProject(row); audit("project",id(row),"CREATE",String.valueOf(row.get("name"))); return AjaxResult.success(row);
    }
    @PutMapping("/projects/{id}") @PreAuthorize("@ss.hasPermi('hetu:project:edit')") public AjaxResult updateProject(@PathVariable long id,@RequestBody Map<String,Object> row) {
        row.put("id",id); required(row,"name"); int changed=mapper.updateProject(row); if(changed>0) audit("project",id,"UPDATE",String.valueOf(row.get("name"))); return changed>0?AjaxResult.success():AjaxResult.error("项目不存在");
    }
    @GetMapping("/tasks") @PreAuthorize("@ss.hasPermi('hetu:task:list')") public AjaxResult tasks() { return AjaxResult.success(mapper.tasks()); }
    @PostMapping("/tasks") @PreAuthorize("@ss.hasPermi('hetu:task:add')") public AjaxResult addTask(@RequestBody Map<String,Object> row) {
        required(row,"title"); row.putIfAbsent("status","TODO"); row.putIfAbsent("priority","MEDIUM"); row.putIfAbsent("progress",0); row.putIfAbsent("estimateHours",0);
        mapper.addTask(row); audit("task",id(row),"CREATE",String.valueOf(row.get("title"))); return AjaxResult.success(row);
    }
    @PutMapping("/tasks/{id}") @PreAuthorize("@ss.hasPermi('hetu:task:edit')") public AjaxResult updateTask(@PathVariable long id,@RequestBody Map<String,Object> row) {
        row.put("id",id); required(row,"title"); int changed=mapper.updateTask(row); if(changed>0) audit("task",id,"UPDATE",String.valueOf(row.get("title"))); return changed>0?AjaxResult.success():AjaxResult.error("任务不存在");
    }
    @PostMapping("/tasks/{id}/claim") @PreAuthorize("@ss.hasPermi('hetu:task:edit')") public AjaxResult claimTask(@PathVariable long id) {
        String actor=displayName();int changed=mapper.claimTask(id,actor);if(changed>0)audit("task",id,"CLAIM",actor+" 从任务池领取");
        return changed>0?AjaxResult.success():AjaxResult.error("任务已被领取或当前不可领取");
    }
    @PostMapping("/tasks/{id}/accept-testing") @PreAuthorize("@ss.hasPermi('hetu:task:edit')") public AjaxResult acceptTesting(@PathVariable long id) {
        String actor=displayName();int changed=mapper.acceptTesting(id,actor);if(changed>0)audit("task",id,"TEST_ACCEPT",actor+" 接收测试任务");
        return changed>0?AjaxResult.success():AjaxResult.error("测试任务已被接收或当前状态不可接收");
    }
    @PatchMapping("/tasks/{id}/progress") @PreAuthorize("@ss.hasPermi('hetu:task:edit')") @Transactional public AjaxResult progress(@PathVariable long id,@RequestBody Map<String,Object> row) {
        int progress=((Number)row.getOrDefault("progress",0)).intValue(); if(progress<0||progress>100) return AjaxResult.error("进度必须在 0 到 100 之间");
        Integer previous=mapper.currentProgress(id); if(previous==null) return AjaxResult.error("任务不存在");
        if(progress<previous) return AjaxResult.error("累计进度不能低于当前进度（"+previous+"%）");
        String workDate=String.valueOf(row.getOrDefault("workDate",LocalDate.now().toString()));try{LocalDate.parse(workDate);}catch(Exception ex){return AjaxResult.error("工作日期格式应为 YYYY-MM-DD");}
        mapper.recordProgress(id,progress,displayName(),String.valueOf(row.getOrDefault("note","进度更新")),workDate);
        mapper.progress(id,progress);
        audit("task",id,"PROGRESS",progress+"%"); return AjaxResult.success();
    }
    @GetMapping("/worklog") @PreAuthorize("@ss.hasPermi('hetu:task:list')") public AjaxResult worklog(@RequestParam(required=false) String from,@RequestParam(required=false) String to,@RequestParam(required=false) Long projectId) {
        LocalDate today=LocalDate.now();String start=from==null||from.isBlank()?today.withDayOfMonth(1).toString():from;String end=to==null||to.isBlank()?today.toString():to;
        try{if(LocalDate.parse(start).isAfter(LocalDate.parse(end)))return AjaxResult.error("开始日期不能晚于结束日期");}catch(Exception ex){return AjaxResult.error("日期格式应为 YYYY-MM-DD");}
        Map<String,Object> result=new LinkedHashMap<>();result.put("entries",mapper.workLogs(start,end,projectId));result.put("ranking",mapper.workRanking(start,end,projectId));result.put("from",start);result.put("to",end);return AjaxResult.success(result);
    }
    @PostMapping("/worklog") @PreAuthorize("@ss.hasPermi('hetu:task:edit')") public AjaxResult addWorkHours(@RequestBody Map<String,Object> row){required(row,"taskId");required(row,"hours");BigDecimal hours=new BigDecimal(String.valueOf(row.get("hours")));if(hours.signum()<=0||hours.compareTo(new BigDecimal("24"))>0)return AjaxResult.error("单次工时须大于 0 且不超过 24 小时");String workDate=String.valueOf(row.getOrDefault("workDate",LocalDate.now().toString()));try{LocalDate.parse(workDate);}catch(Exception ex){return AjaxResult.error("工作日期格式应为 YYYY-MM-DD");}int changed=mapper.recordWorkHours(Long.parseLong(String.valueOf(row.get("taskId"))),hours,displayName(),String.valueOf(row.getOrDefault("note","")),workDate);if(changed==0)return AjaxResult.error("只能为自己负责且进行中的任务登记工时");audit("task",Long.parseLong(String.valueOf(row.get("taskId"))),"WORKLOG",hours+"h");return AjaxResult.success();}
    @PatchMapping("/tasks/{id}/status") @PreAuthorize("@ss.hasPermi('hetu:task:edit')") public AjaxResult taskStatus(@PathVariable long id,@RequestBody Map<String,Object> row) {
        String next=String.valueOf(row.get("status")),previous=mapper.taskStatus(id); if(previous==null)return AjaxResult.error("任务不存在");
        boolean allowed=switch(previous){case "TODO"->Set.of("IN_PROGRESS").contains(next);case "IN_PROGRESS"->Set.of("TODO","TESTING").contains(next);case "TESTING"->Set.of("IN_PROGRESS","DONE").contains(next);case "DONE"->"IN_PROGRESS".equals(next);default->false;};
        if(!allowed)return AjaxResult.error("不允许从当前状态流转到目标状态");
        int changed=mapper.setTaskStatus(id,next); if(changed>0)audit("task",id,"STATUS",transitionDetail(previous,next,row)); return changed>0?AjaxResult.success():AjaxResult.error("状态更新失败，请检查任务进度");
    }
    @GetMapping("/requirements") @PreAuthorize("@ss.hasPermi('hetu:requirement:list')") public AjaxResult requirements() { return AjaxResult.success(mapper.requirements()); }
    @PostMapping("/requirements") @PreAuthorize("@ss.hasPermi('hetu:requirement:add')") public AjaxResult addRequirement(@RequestBody Map<String,Object> row) {
        required(row,"title"); row.putIfAbsent("content",""); row.putIfAbsent("status","DRAFT"); mapper.addRequirement(row); audit("requirement",id(row),"CREATE",String.valueOf(row.get("title"))); return AjaxResult.success(row);
    }
    @PutMapping("/requirements/{id}") @PreAuthorize("@ss.hasPermi('hetu:requirement:edit')") public AjaxResult updateRequirement(@PathVariable long id,@RequestBody Map<String,Object> row) {
        row.put("id",id); required(row,"title"); int changed=mapper.updateRequirement(row); if(changed>0) audit("requirement",id,"UPDATE",String.valueOf(row.get("title"))); return changed>0?AjaxResult.success():AjaxResult.error("需求不存在");
    }
    @PatchMapping("/requirements/{id}/status") @PreAuthorize("@ss.hasPermi('hetu:requirement:edit')") public AjaxResult requirementStatus(@PathVariable long id,@RequestBody Map<String,Object> row) {
        String previous=mapper.requirementStatus(id),next=String.valueOf(row.get("status")); if(previous==null)return AjaxResult.error("需求不存在");
        boolean allowed=("DRAFT".equals(previous)&&"IN_PROGRESS".equals(next))||("IN_PROGRESS".equals(previous)&&"APPROVED".equals(next))||("APPROVED".equals(previous)&&"IN_PROGRESS".equals(next));
        if(!allowed)return AjaxResult.error("不允许从当前状态流转到目标状态");int changed=mapper.setRequirementStatus(id,next);if(changed>0)audit("requirement",id,"STATUS",transitionDetail(previous,next,row));return changed>0?AjaxResult.success():AjaxResult.error("需求状态更新失败");
    }
    @GetMapping("/bugs") @PreAuthorize("@ss.hasPermi('hetu:bug:list')") public AjaxResult bugs() { return AjaxResult.success(mapper.bugs()); }
    @PostMapping("/bugs") @PreAuthorize("@ss.hasPermi('hetu:bug:add')") public AjaxResult addBug(@RequestBody Map<String,Object> row) {
        required(row,"title"); row.putIfAbsent("type","FUNCTIONAL"); row.putIfAbsent("severity","MEDIUM"); row.putIfAbsent("status","NEW"); row.putIfAbsent("rootCause","NO_DEDUCTION"); scoreBug(row); mapper.addBug(row); audit("bug",id(row),"CREATE",String.valueOf(row.get("title"))); return AjaxResult.success(row);
    }
    @PutMapping("/bugs/{id}") @PreAuthorize("@ss.hasPermi('hetu:bug:edit')") public AjaxResult updateBug(@PathVariable long id,@RequestBody Map<String,Object> row) {
        row.put("id",id); required(row,"title"); row.putIfAbsent("rootCause","NO_DEDUCTION"); scoreBug(row); int changed=mapper.updateBug(row); if(changed>0) audit("bug",id,"UPDATE",String.valueOf(row.get("status"))+", scoreHours="+row.get("scoreHours")); return changed>0?AjaxResult.success():AjaxResult.error("缺陷不存在");
    }
    @PatchMapping("/bugs/{id}/status") @PreAuthorize("@ss.hasPermi('hetu:bug:edit')") @Transactional public AjaxResult bugStatus(@PathVariable long id,@RequestBody Map<String,Object> row) {
        Map<String,Object> bug=mapper.bugStatusInfo(id);if(bug==null)return AjaxResult.error("缺陷不存在");String previous=String.valueOf(bug.get("status")),next=String.valueOf(row.get("status"));
        boolean allowed=("NEW".equals(previous)&&"IN_PROGRESS".equals(next))||("IN_PROGRESS".equals(previous)&&"VERIFYING".equals(next))||("VERIFYING".equals(previous)&&Set.of("CLOSED","IN_PROGRESS").contains(next))||("CLOSED".equals(previous)&&"NEW".equals(next));
        if(!allowed)return AjaxResult.error("不允许从当前状态流转到目标状态");Map<String,Object> assignment=null;String person="";if("CLOSED".equals(next)){assignment=mapper.bugAssignment(id);person=String.valueOf(assignment.getOrDefault("assignee",""));if(person.isBlank()||"null".equals(person))return AjaxResult.error("关闭缺陷前请先指定责任人");}
        bug.put("status",next);scoreBug(bug);BigDecimal score=(BigDecimal)bug.get("scoreHours");int changed=mapper.setBugStatus(id,next,score);
        if(changed>0&&"CLOSED".equals(next)){Map<String,Object> deduction=new LinkedHashMap<>();deduction.put("person",person);deduction.put("projectId",assignment.get("projectId"));deduction.put("category","缺陷 · "+bug.get("rootCause")+" · "+bug.get("severity"));deduction.put("sourceType","BUG");deduction.put("sourceId",id);deduction.put("hours",score);deduction.put("note",transitionDetail(previous,next,row));deduction.put("occurredOn",LocalDate.now().toString());mapper.addDeduction(deduction);}
        if(changed>0)audit("bug",id,"STATUS",transitionDetail(previous,next,row)+("CLOSED".equals(next)?", scoreHours="+score:""));return changed>0?AjaxResult.success():AjaxResult.error("缺陷状态更新失败");
    }
    @PostMapping("/bugs/{id}/assign") @PreAuthorize("@ss.hasPermi('hetu:project:edit')") public AjaxResult assignBug(@PathVariable long id,@RequestBody Map<String,Object> row){String person=String.valueOf(row.getOrDefault("assignee","")).trim();if(person.isBlank())return AjaxResult.error("请指定缺陷责任人");int changed=mapper.updateBugAssignee(id,person);if(changed>0)audit("bug",id,"ASSIGN",person);return changed>0?AjaxResult.success():AjaxResult.error("缺陷不存在或责任人更新失败");}
    @PostMapping("/deductions") @PreAuthorize("@ss.hasPermi('hetu:project:edit')") public AjaxResult addDeduction(@RequestBody Map<String,Object> row){required(row,"person");required(row,"category");required(row,"hours");BigDecimal hours=new BigDecimal(String.valueOf(row.get("hours")));if(hours.signum()<=0||hours.compareTo(new BigDecimal("1000"))>0)return AjaxResult.error("扣减工时须大于 0 且不超过 1000");String occurred=String.valueOf(row.getOrDefault("occurredOn",LocalDate.now().toString()));try{LocalDate.parse(occurred);}catch(Exception ex){return AjaxResult.error("日期格式应为 YYYY-MM-DD");}row.put("hours",hours.negate());row.put("sourceType","DAILY");row.put("occurredOn",occurred);mapper.addDeduction(row);long logId=System.currentTimeMillis();audit("deduction",logId,"CREATE",row.get("person")+" · "+row.get("category")+" · -"+hours+"h");return AjaxResult.success();}
    @GetMapping("/deductions") @PreAuthorize("@ss.hasPermi('hetu:bug:list')") public AjaxResult deductions(@RequestParam(required=false) String from,@RequestParam(required=false) String to,@RequestParam(required=false) Long projectId){LocalDate today=LocalDate.now();String start=from==null||from.isBlank()?today.withDayOfMonth(1).toString():from;String end=to==null||to.isBlank()?today.toString():to;try{if(LocalDate.parse(start).isAfter(LocalDate.parse(end)))return AjaxResult.error("开始日期不能晚于结束日期");}catch(Exception ex){return AjaxResult.error("日期格式应为 YYYY-MM-DD");}return AjaxResult.success(mapper.deductions(start,end,projectId));}
    @GetMapping("/releases") @PreAuthorize("@ss.hasPermi('hetu:project:list')") public AjaxResult releases(@RequestParam long projectId){return AjaxResult.success(mapper.releases(projectId).stream().map(v->{v.put("items",mapper.releaseItems(((Number)v.get("id")).longValue()));return v;}).toList());}
    @PostMapping("/releases") @PreAuthorize("@ss.hasPermi('hetu:project:edit')") public AjaxResult addRelease(@RequestBody Map<String,Object> row){required(row,"name");required(row,"projectId");row.putIfAbsent("status","PLANNED");mapper.addRelease(row);audit("release",id(row),"CREATE",String.valueOf(row.get("name")));return AjaxResult.success(row);}
    @PostMapping("/releases/{id}/items") @PreAuthorize("@ss.hasPermi('hetu:project:edit')") public AjaxResult addReleaseItem(@PathVariable long id,@RequestBody Map<String,Object> row){String type=String.valueOf(row.get("entityType"));if(!Set.of("requirement","bug").contains(type))return AjaxResult.error("发布内容仅支持需求或缺陷");required(row,"entityId");int changed=mapper.addReleaseItem(id,type,Long.parseLong(String.valueOf(row.get("entityId"))));audit("release",id,"ADD_ITEM",type+"#"+row.get("entityId"));return AjaxResult.success(changed);}
    @PostMapping("/releases/{id}/submit-test") @PreAuthorize("@ss.hasPermi('hetu:release:test')") public AjaxResult submitReleaseTest(@PathVariable long id){int changed=mapper.submitReleaseTest(id,displayName());if(changed>0)audit("release",id,"TEST_SUBMIT",displayName()+" 提交测试完成");return changed>0?AjaxResult.success():AjaxResult.error("发布计划不在待测试状态");}
    @PostMapping("/releases/{id}/publish") @PreAuthorize("@ss.hasPermi('hetu:release:publish')") public AjaxResult publishRelease(@PathVariable long id){int changed=mapper.publishRelease(id,displayName());if(changed>0)audit("release",id,"PUBLISH",displayName()+" 完成发布并提交产品验证");return changed>0?AjaxResult.success():AjaxResult.error("请先由测试提交发布计划");}
    @PostMapping("/releases/{id}/verify") @PreAuthorize("@ss.hasPermi('hetu:release:verify')") public AjaxResult verifyRelease(@PathVariable long id,@RequestBody Map<String,Object> row){String status=String.valueOf(row.get("status"));if(!Set.of("NEEDS_IMPROVEMENT","VERIFIED").contains(status))return AjaxResult.error("验证结果无效");Map<String,Object> release=mapper.release(id);if(release==null)return AjaxResult.error("发布计划不存在");if("VERIFIED".equals(status)){Object projectId=release.get("projectId");String expected=mapper.pendingReleaseVerifier(((Number)projectId).longValue());if(expected!=null&&!expected.equals(displayName()))return AjaxResult.error("该项目待完善计划需由原产品验证人完成确认");}String note=String.valueOf(row.getOrDefault("note",""));int changed=mapper.verifyRelease(id,status,displayName(),note);if(changed>0)audit("release",id,"VERIFY",status+" · "+note);return changed>0?AjaxResult.success():AjaxResult.error("仅已发布计划可以进行产品验证");}
    @PostMapping("/releases/{id}/close") @PreAuthorize("@ss.hasPermi('hetu:release:close')") public AjaxResult closeRelease(@PathVariable long id,@RequestBody(required=false) Map<String,Object> row){int changed=mapper.closeRelease(id);if(changed>0)audit("release",id,"CLOSE",displayName()+" 手动关闭发布计划"+(row==null||row.get("note")==null?"":" · "+row.get("note")));return changed>0?AjaxResult.success():AjaxResult.error("仅待完善或已验证的发布计划可以手动关闭");}
    @PatchMapping("/releases/{id}/schedule") @PreAuthorize("@ss.hasAnyPermi('hetu:release:schedule','hetu:release:add')") public AjaxResult scheduleRelease(@PathVariable long id,@RequestBody Map<String,Object> row){String date=String.valueOf(row.getOrDefault("releaseDate",""));try{LocalDate.parse(date);}catch(Exception ex){return AjaxResult.error("发布日期格式应为 YYYY-MM-DD");}int changed=mapper.scheduleRelease(id,date);if(changed>0)audit("release",id,"SCHEDULE",date);return changed>0?AjaxResult.success():AjaxResult.error("发布后不能修改排期");}
    @PostMapping(value="/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("@ss.hasPermi('hetu:attachment:upload')")
    public AjaxResult uploadImage(@RequestParam String entityType,@RequestParam long entityId,@RequestParam("file") MultipartFile file) throws java.io.IOException {
        if(!Set.of("project","requirement","task","bug").contains(entityType)) return AjaxResult.error("无效的附件类型");
        if(file.isEmpty()||file.getSize()>10L*1024*1024) return AjaxResult.error("图片不能为空且不能超过 10MB");
        String mime=file.getContentType(); Map<String,String> extensions=Map.of("image/png",".png","image/jpeg",".jpg","image/gif",".gif","image/webp",".webp");
        if(!extensions.containsKey(mime)) return AjaxResult.error("仅支持 PNG、JPEG、GIF、WebP 图片");
        String name=UUID.randomUUID()+extensions.get(mime); Path dir=Path.of(RuoYiConfig.getProfile(),"hetu"); Files.createDirectories(dir); Path destination=dir.resolve(name);
        file.transferTo(destination);
        Map<String,Object> row=new LinkedHashMap<>(); row.put("entityType",entityType); row.put("entityId",entityId); row.put("fileName",file.getOriginalFilename());
        row.put("storagePath","/profile/hetu/"+name); row.put("mimeType",mime); row.put("fileSize",file.getSize()); row.put("uploadedBy",SecurityUtils.getUsername());
        mapper.addAttachment(row); audit(entityType,entityId,"ATTACH",String.valueOf(file.getOriginalFilename())); row.put("url",row.get("storagePath")); return AjaxResult.success(row);
    }
    private static void required(Map<String,Object> row,String key) { if(row.get(key)==null||String.valueOf(row.get(key)).isBlank()) throw new IllegalArgumentException(key+" 不能为空"); }
    private static String transitionDetail(String previous,String next,Map<String,Object> row){String note=String.valueOf(row.getOrDefault("note","")).trim();if(note.length()>800)note=note.substring(0,800);return previous+" -> "+next+(note.isEmpty()?"":"；说明："+note);}
    private void scoreBug(Map<String,Object> row) {
        String cause=String.valueOf(row.get("rootCause"));
        if(!Set.of("NO_DEDUCTION","PRODUCT_DEFECT","REQUIREMENT_CHANGE","ENVIRONMENT","DUPLICATE","NOT_REPRODUCIBLE").contains(cause)) throw new IllegalArgumentException("无效的缺陷归因");
        String severity=String.valueOf(row.get("severity"));
        if(!Set.of("CRITICAL","HIGH","MEDIUM","LOW").contains(severity)) throw new IllegalArgumentException("无效的缺陷等级");
        BigDecimal deduction=BigDecimal.ZERO;
        if("CLOSED".equals(row.get("status"))) {
            if("PRODUCT_DEFECT".equals(cause)) {
                Object taskId=row.get("taskId");
                Long taskKey=taskId instanceof Number taskNumber?taskNumber.longValue():parseTaskId(taskId);
                if(taskKey==null) throw new IllegalArgumentException("开发类 Bug 关闭前必须关联开发任务，才能按任务预估工时计算扣减");
                BigDecimal estimate=mapper.taskEstimateHours(taskKey);
                if(estimate==null) throw new IllegalArgumentException("关联的开发任务不存在，无法计算扣减工时");
                BigDecimal ratio=switch(severity) { case "CRITICAL" -> new BigDecimal("0.50"); case "HIGH" -> new BigDecimal("0.30"); case "MEDIUM" -> new BigDecimal("0.10"); case "LOW" -> new BigDecimal("0.05"); default -> BigDecimal.ZERO; };
                deduction=estimate.multiply(ratio);
            } else if("REQUIREMENT_CHANGE".equals(cause)||"ENVIRONMENT".equals(cause)) {
                deduction=switch(severity) { case "CRITICAL" -> new BigDecimal("5"); case "HIGH" -> new BigDecimal("3"); case "MEDIUM" -> BigDecimal.ONE; default -> BigDecimal.ZERO; };
            }
        }
        row.put("scoreHours",deduction.setScale(2,RoundingMode.HALF_UP).negate());
    }
    private static Long parseTaskId(Object value) { try { return value==null?null:Long.valueOf(String.valueOf(value)); } catch(NumberFormatException ex) { return null; } }
    private static long id(Map<String,Object> row) { Object id=row.get("id"); return id instanceof Number n?n.longValue():0L; }
    private String displayName(){String username=SecurityUtils.getUsername(),name=mapper.userDisplayName(username);return name==null||name.isBlank()?username:name;}
    private void audit(String type,long id,String action,String detail) { mapper.log(SecurityUtils.getUsername(),type,id,action,detail); }
}
