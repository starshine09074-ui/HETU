<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  kind: { type: String, default: 'requirement' },
  item: { type: Object, default: () => ({}) },
  projectName: { type: String, default: '' },
  relatedTask: { type: Object, default: null },
  relatedRequirement: { type: Object, default: null },
  canEdit: Boolean,
  safeContent: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'edit'])
const title = computed(() => ({ requirement: '需求详情', task: '任务详情', bug: '缺陷详情' }[props.kind] || '详情'))
const status = computed(() => ({
  DRAFT: '待澄清', IN_PROGRESS: '进行中', APPROVED: '已确认', TODO: '待处理', TESTING: '正在测试', DONE: '已完成',
  NEW: '新建', VERIFYING: '待验证', CLOSED: '已关闭',
}[props.item?.status] || props.item?.status || '—'))
const severity = computed(() => ({ CRITICAL: '致命', HIGH: '严重', MEDIUM: '一般', LOW: '轻微' }[props.item?.severity] || props.item?.severity || '—'))
const priority = computed(() => ({ HIGH: '高', MEDIUM: '常规', LOW: '低' }[props.item?.priority] || props.item?.priority || '—'))
const cause = computed(() => ({ NO_DEDUCTION: '不扣工时', PRODUCT_DEFECT: '开发问题', REQUIREMENT_CHANGE: '需求变更', ENVIRONMENT: '环境或第三方', DUPLICATE: '重复问题', NOT_REPRODUCIBLE: '无法复现' }[props.item?.rootCause] || props.item?.rootCause || '—'))
</script>

<template>
  <el-dialog :model-value="modelValue" :title="title" width="min(760px, 92vw)" class="hetu-dialog record-detail-dialog" destroy-on-close @close="emit('update:modelValue', false)">
    <div v-if="item" class="record-detail">
      <div class="record-detail-heading">
        <div>
          <span class="record-detail-id">{{ kind === 'bug' ? `BUG-${String(item.id).padStart(3, '0')}` : kind === 'task' ? `TASK-${item.id}` : `REQ-${item.id}` }}</span>
          <h2>{{ item.title || '未命名' }}</h2>
        </div>
        <span class="record-detail-status">{{ status }}</span>
      </div>

      <div class="record-detail-grid">
        <div><small>所属项目</small><b>{{ projectName || '—' }}</b></div>
        <template v-if="kind === 'requirement'">
          <div><small>负责人</small><b>{{ item.owner || '未分配' }}</b></div>
          <div><small>版本</small><b>{{ item.version || '未规划' }}</b></div>
          <div><small>创建时间</small><b>{{ item.createdAt || '—' }}</b></div>
        </template>
        <template v-else-if="kind === 'task'">
          <div><small>负责人</small><b>{{ item.assignee || '未分配' }}</b></div>
          <div><small>关联需求</small><b>{{ relatedRequirement?.title || '未关联' }}</b></div>
          <div><small>预计工时</small><b>{{ item.estimateHours || 0 }} 小时</b></div>
          <div><small>完成进度</small><b>{{ item.progress || 0 }}%</b></div>
          <div><small>优先级</small><b>{{ priority }}</b></div>
          <div><small>截止日期</small><b>{{ item.dueDate || '未设置' }}</b></div>
        </template>
        <template v-else>
          <div><small>等级</small><b>{{ severity }}</b></div>
          <div><small>缺陷类型</small><b>{{ item.type === 'UI' ? '界面' : item.type === 'PERFORMANCE' ? '性能' : '功能' }}</b></div>
          <div><small>责任人</small><b>{{ item.assignee || '未分配' }}</b></div>
          <div><small>关联任务</small><b>{{ relatedTask?.title || '手动登记 / 未关联' }}</b></div>
          <div><small>关联需求</small><b>{{ relatedRequirement?.title || '未关联' }}</b></div>
          <div><small>缺陷归因</small><b>{{ cause }}</b></div>
          <div><small>质量扣减</small><b>{{ Number(item.scoreHours) < 0 ? `${Math.abs(Number(item.scoreHours))} 小时` : '不扣工时' }}</b></div>
        </template>
      </div>

      <section v-if="kind === 'requirement'" class="record-detail-section">
        <h3>需求正文</h3>
        <div v-if="item.content" class="markdown-body" v-html="safeContent"></div>
        <p v-else class="record-detail-empty">暂无需求正文</p>
      </section>
      <section v-if="kind === 'task'" class="record-detail-section">
        <h3>任务说明</h3><p>{{ item.description || '暂无任务说明' }}</p>
      </section>
      <template v-if="kind === 'bug'">
        <section class="record-detail-section"><h3>问题描述</h3><p>{{ item.description || '暂无问题描述' }}</p></section>
        <section class="record-detail-section"><h3>复现步骤</h3><p>{{ item.steps || '暂无复现步骤' }}</p></section>
      </template>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button v-if="canEdit" type="primary" @click="emit('edit')">编辑</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.record-detail{color:#344057}.record-detail-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:18px;padding:2px 0 18px;border-bottom:1px solid #edf0f5}.record-detail-id{font-size:11px;color:#929daf}.record-detail-heading h2{font-size:20px;margin:7px 0 0;color:#253149}.record-detail-status{font-size:11px;padding:6px 10px;border-radius:16px;color:#5d6fe2;background:#eef1ff;white-space:nowrap}.record-detail-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px 24px;padding:20px 0}.record-detail-grid>div{display:flex;flex-direction:column;gap:6px;min-width:0}.record-detail-grid small{font-size:11px;color:#98a2b2}.record-detail-grid b{font-size:13px;font-weight:500;color:#46536a;overflow-wrap:anywhere}.record-detail-section{padding:15px 0;border-top:1px solid #edf0f5}.record-detail-section h3{font-size:13px;margin:0 0 10px;color:#35425a}.record-detail-section p{white-space:pre-wrap;line-height:1.7;font-size:13px;color:#647188;margin:0}.record-detail-empty{color:#9ca6b5!important}.record-detail-section :deep(.markdown-body){font-size:13px;color:#647188;line-height:1.7}.record-detail-section :deep(.markdown-body img){max-width:100%;height:auto}@media(max-width:650px){.record-detail-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
