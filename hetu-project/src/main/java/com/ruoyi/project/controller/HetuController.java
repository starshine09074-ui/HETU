package com.ruoyi.project.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
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
    @PatchMapping("/tasks/{id}/progress") @PreAuthorize("@ss.hasPermi('hetu:task:edit')") @Transactional public AjaxResult progress(@PathVariable long id,@RequestBody Map<String,Object> row) {
        int progress=((Number)row.getOrDefault("progress",0)).intValue(); if(progress<0||progress>100) return AjaxResult.error("进度必须在 0 到 100 之间");
        Integer previous=mapper.currentProgress(id); if(previous==null) return AjaxResult.error("任务不存在");
        if(progress<previous) return AjaxResult.error("累计进度不能低于当前进度（"+previous+"%）");
        mapper.recordProgress(id,progress,SecurityUtils.getUsername(),String.valueOf(row.getOrDefault("note","进度更新")));
        mapper.progress(id,progress);
        audit("task",id,"PROGRESS",progress+"%"); return AjaxResult.success();
    }
    @PatchMapping("/tasks/{id}/status") @PreAuthorize("@ss.hasPermi('hetu:task:edit')") public AjaxResult taskStatus(@PathVariable long id,@RequestBody Map<String,Object> row) {
        String status=String.valueOf(row.get("status")); if(!java.util.Set.of("TODO","IN_PROGRESS","TESTING","DONE").contains(status)) return AjaxResult.error("无效的任务状态");
        row.put("id",id); row.put("title","status"); row.put("description",""); row.put("assignee",""); row.put("estimateHours",0); row.put("priority","MEDIUM"); row.put("projectId",0); row.put("status",status);
        // A dedicated status update keeps task attributes intact.
        int changed=mapper.setTaskStatus(id,status); if(changed>0) audit("task",id,"STATUS",status); return changed>0?AjaxResult.success():AjaxResult.error("任务不存在");
    }
    @GetMapping("/requirements") @PreAuthorize("@ss.hasPermi('hetu:requirement:list')") public AjaxResult requirements() { return AjaxResult.success(mapper.requirements()); }
    @PostMapping("/requirements") @PreAuthorize("@ss.hasPermi('hetu:requirement:add')") public AjaxResult addRequirement(@RequestBody Map<String,Object> row) {
        required(row,"title"); row.putIfAbsent("content",""); row.putIfAbsent("status","DRAFT"); mapper.addRequirement(row); audit("requirement",id(row),"CREATE",String.valueOf(row.get("title"))); return AjaxResult.success(row);
    }
    @PutMapping("/requirements/{id}") @PreAuthorize("@ss.hasPermi('hetu:requirement:edit')") public AjaxResult updateRequirement(@PathVariable long id,@RequestBody Map<String,Object> row) {
        row.put("id",id); required(row,"title"); int changed=mapper.updateRequirement(row); if(changed>0) audit("requirement",id,"UPDATE",String.valueOf(row.get("title"))); return changed>0?AjaxResult.success():AjaxResult.error("需求不存在");
    }
    @GetMapping("/bugs") @PreAuthorize("@ss.hasPermi('hetu:bug:list')") public AjaxResult bugs() { return AjaxResult.success(mapper.bugs()); }
    @PostMapping("/bugs") @PreAuthorize("@ss.hasPermi('hetu:bug:add')") public AjaxResult addBug(@RequestBody Map<String,Object> row) {
        required(row,"title"); row.putIfAbsent("type","FUNCTIONAL"); row.putIfAbsent("severity","MEDIUM"); row.putIfAbsent("status","NEW"); row.putIfAbsent("rootCause","PRODUCT_DEFECT"); scoreBug(row); mapper.addBug(row); audit("bug",id(row),"CREATE",String.valueOf(row.get("title"))); return AjaxResult.success(row);
    }
    @PutMapping("/bugs/{id}") @PreAuthorize("@ss.hasPermi('hetu:bug:edit')") public AjaxResult updateBug(@PathVariable long id,@RequestBody Map<String,Object> row) {
        row.put("id",id); required(row,"title"); row.putIfAbsent("rootCause","PRODUCT_DEFECT"); scoreBug(row); int changed=mapper.updateBug(row); if(changed>0) audit("bug",id,"UPDATE",String.valueOf(row.get("status"))+", score="+row.get("scorePoints")); return changed>0?AjaxResult.success():AjaxResult.error("缺陷不存在");
    }
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
    private static void scoreBug(Map<String,Object> row) {
        String cause=String.valueOf(row.get("rootCause"));
        if(!Set.of("PRODUCT_DEFECT","REQUIREMENT_CHANGE","ENVIRONMENT","DUPLICATE","NOT_REPRODUCIBLE").contains(cause)) throw new IllegalArgumentException("无效的缺陷归因");
        String severity=String.valueOf(row.get("severity"));
        if(!Set.of("CRITICAL","HIGH","MEDIUM","LOW").contains(severity)) throw new IllegalArgumentException("无效的缺陷等级");
        int points=0;
        if("CLOSED".equals(row.get("status")) && "PRODUCT_DEFECT".equals(cause)) points=switch(severity) { case "CRITICAL" -> 5; case "HIGH" -> 3; case "MEDIUM" -> 1; default -> 0; };
        row.put("scorePoints",-points);
    }
    private static long id(Map<String,Object> row) { Object id=row.get("id"); return id instanceof Number n?n.longValue():0L; }
    private void audit(String type,long id,String action,String detail) { mapper.log(SecurityUtils.getUsername(),type,id,action,detail); }
}
