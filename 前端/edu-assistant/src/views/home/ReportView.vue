<template>
  <div class="report-page">
    <div class="page-hero">
      <div>
        <h2 class="page-title">{{ isTeacher ? '实验报告批改中心' : '我的实验报告' }}</h2>
        <p class="page-desc">
          {{ isTeacher ? '按实验查看提交进度、待批改人数，并直接展开版本进行批阅。' : '查看自己提交的报告，展开后查看版本列表并进入批改详情页。' }}
        </p>
      </div>
      <el-button v-if="!isTeacher" type="primary" round @click="goExperimentList">前往实验列表</el-button>
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton animated :rows="8" />
    </div>

    <template v-else-if="isTeacher">
      <div class="stats-grid">
        <div class="stats-card primary">
          <span class="stats-label">负责实验</span>
          <strong class="stats-value">{{ teacherSummary.taskCount }}</strong>
        </div>
        <div class="stats-card success">
          <span class="stats-label">已提交</span>
          <strong class="stats-value">{{ teacherSummary.submittedCount }}</strong>
        </div>
        <div class="stats-card warning">
          <span class="stats-label">待批改</span>
          <strong class="stats-value">{{ teacherSummary.pendingReviewCount }}</strong>
        </div>
        <div class="stats-card danger">
          <span class="stats-label">需修订</span>
          <strong class="stats-value">{{ teacherSummary.revisionRequiredCount }}</strong>
        </div>
      </div>

      <el-empty v-if="!teacherTasks.length" description="暂无你负责的报告任务" />

      <div v-else class="teacher-task-list">
        <section v-for="task in teacherTasks" :key="task.experimentId" class="task-card">
          <div class="task-header">
            <div>
              <h3 class="task-title">{{ task.experimentTitle }}</h3>
              <p class="task-meta">{{ task.courseName }} · 实验ID {{ task.experimentId }}</p>
            </div>
            <div class="task-tags">
              <el-tag type="info" effect="light">总人数 {{ task.totalStudents || 0 }}</el-tag>
              <el-tag type="success" effect="light">已提交 {{ task.submittedCount || 0 }}</el-tag>
              <el-tag type="warning" effect="light">待批改 {{ task.pendingReviewCount || 0 }}</el-tag>
              <el-tag type="danger" effect="light">未提交 {{ task.unsubmittedCount || 0 }}</el-tag>
            </div>
          </div>

          <el-table :data="task.students || []" class="student-table" empty-text="暂无学生数据">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-panel">
                  <div v-if="!row.reportId" class="empty-version">学生尚未提交该实验报告。</div>
                  <el-collapse v-else>
                    <el-collapse-item
                      v-for="version in row.versions || []"
                      :key="version.reportVersionId"
                      :name="version.reportVersionId"
                    >
                      <template #title>
                        <div class="version-title">
                          <span>第 {{ version.versionNo }} 版</span>
                          <span>{{ fmt(version.submittedAt) }}</span>
                          <el-tag size="small" :type="reviewTagType(version.action)">
                            {{ reviewLabel(version.action) }}
                          </el-tag>
                        </div>
                      </template>

                      <div class="version-body">
                        <div class="version-summary">
                          <span v-if="version.score != null">评分 {{ version.score }}</span>
                          <span v-if="version.teacherName">批改教师 {{ version.teacherName }}</span>
                        </div>
                        <div v-if="version.commentText" class="review-box">
                          <strong>评语：</strong>{{ version.commentText }}
                        </div>
                        <div v-if="version.revisionRequirement" class="review-box revision">
                          <strong>修订要求：</strong>{{ version.revisionRequirement }}
                        </div>
                        <div v-if="version.contentHtml" class="content-preview ql-snow">
                          <div class="ql-editor" v-html="version.contentHtml" />
                        </div>
                        <div class="version-actions">
                          <el-button type="primary" size="small" round @click="openReviewDialog(task, row, version)">
                            批改此版本
                          </el-button>
                        </div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="学生" min-width="150">
              <template #default="{ row }">
                <div class="student-cell">
                  <strong>{{ row.studentName }}</strong>
                  <span>{{ row.studentNo || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" effect="light">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交版本" width="100">
              <template #default="{ row }">{{ row.latestVersionNo || 0 }}</template>
            </el-table-column>
            <el-table-column label="最终得分" width="100">
              <template #default="{ row }">{{ row.finalScore ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="最近提交" min-width="170">
              <template #default="{ row }">{{ fmt(row.lastSubmittedAt) || '-' }}</template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </template>

    <template v-else>
      <div class="stats-grid">
        <div class="stats-card primary">
          <span class="stats-label">报告数量</span>
          <strong class="stats-value">{{ studentSummary.reportCount }}</strong>
        </div>
        <div class="stats-card success">
          <span class="stats-label">已提交版本</span>
          <strong class="stats-value">{{ studentSummary.versionCount }}</strong>
        </div>
        <div class="stats-card warning">
          <span class="stats-label">已批改</span>
          <strong class="stats-value">{{ studentSummary.reviewedCount }}</strong>
        </div>
        <div class="stats-card neutral">
          <span class="stats-label">平均得分</span>
          <strong class="stats-value">{{ studentSummary.avgScore }}</strong>
        </div>
      </div>

      <el-empty v-if="!studentReports.length" description="暂无已创建的报告记录" />

      <el-collapse v-else class="report-collapse">
        <el-collapse-item
          v-for="report in studentReports"
          :key="report.reportId"
          :name="report.reportId"
          class="report-collapse-item"
        >
          <template #title>
            <div class="report-title-row">
              <div>
                <strong>{{ report.experimentTitle }}</strong>
                <p>{{ report.courseName }} · {{ report.teacherName || '未分配教师' }}</p>
              </div>
              <div class="report-title-side">
                <div class="report-title-meta">
                  <el-tag :type="statusTagType(report.status)" effect="light">
                    {{ statusLabel(report.status) }}
                  </el-tag>
                  <span>版本 {{ report.latestVersionNo || 0 }}</span>
                  <span>最终分 {{ report.finalScore ?? '-' }}</span>
                </div>
                <el-button type="primary" plain round size="small" @click.stop="goSubmit(report.experimentId)">
                  重新修改提交
                </el-button>
              </div>
            </div>
          </template>

          <div class="report-panel">
            <div class="report-overview">
              <span>最近提交：{{ fmt(report.lastSubmittedAt) || '暂无' }}</span>
              <span>版本数：{{ (report.versions || []).length }}</span>
            </div>

            <div class="student-version-list">
              <div
                v-for="version in report.versions || []"
                :key="version.reportVersionId"
                class="student-version-item"
              >
                <div class="student-version-main">
                  <div class="version-title">
                    <span>第 {{ version.versionNo }} 版</span>
                    <span>{{ fmt(version.submittedAt) }}</span>
                    <el-tag size="small" :type="reviewTagType(version.action)">
                      {{ reviewLabel(version.action) }}
                    </el-tag>
                  </div>
                  <div class="version-summary">
                    <span>得分 {{ version.score ?? '-' }}</span>
                    <span>批改时间 {{ fmt(version.reviewedAt) || '未批改' }}</span>
                  </div>
                </div>
                <el-button
                  type="primary"
                  plain
                  round
                  size="small"
                  @click="goReviewDetail(report.reportId, version.reportVersionId)"
                >
                  查看批改详情
                </el-button>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </template>

    <el-dialog v-model="reviewDialog.visible" title="批改报告版本" width="680px" destroy-on-close>
      <div v-if="reviewDialog.version" class="review-dialog-body">
        <div class="dialog-summary">
          <el-tag type="info" effect="light">{{ reviewDialog.task?.experimentTitle }}</el-tag>
          <span>{{ reviewDialog.student?.studentName }}</span>
          <span>第 {{ reviewDialog.version.versionNo }} 版</span>
          <span>{{ fmt(reviewDialog.version.submittedAt) }}</span>
        </div>

        <div v-if="reviewDialog.version.contentHtml" class="dialog-preview ql-snow">
          <div class="ql-editor" v-html="reviewDialog.version.contentHtml" />
        </div>

        <el-form label-width="84px" class="review-form">
          <el-form-item label="评分">
            <el-input-number v-model="reviewForm.score" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="处理结果">
            <el-radio-group v-model="reviewForm.action">
              <el-radio-button label="REVIEWED">通过</el-radio-button>
              <el-radio-button label="REVISION_REQUIRED">需修订</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="评语">
            <el-input
              v-model="reviewForm.commentText"
              type="textarea"
              :rows="4"
              placeholder="填写批改意见、扣分说明等"
            />
          </el-form-item>
          <el-form-item label="修订要求" v-if="reviewForm.action === 'REVISION_REQUIRED'">
            <el-input
              v-model="reviewForm.revisionRequirement"
              type="textarea"
              :rows="3"
              placeholder="明确指出需要修改的内容"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交批改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStudentReportDashboard, getTeacherReportDashboard, scoreReport } from '@/api/report'

const router = useRouter()

const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const isTeacher = computed(() => ['TEACHER', 'ADMIN'].includes(userInfo.value.role))
const loading = ref(true)
const studentReports = ref([])
const teacherTasks = ref([])

const reviewDialog = reactive({
  visible: false,
  task: null,
  student: null,
  version: null,
})

const reviewForm = reactive({
  score: 90,
  action: 'REVIEWED',
  commentText: '',
  revisionRequirement: '',
})

const reviewSubmitting = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    if (isTeacher.value) {
      teacherTasks.value = await getTeacherReportDashboard()
    } else {
      studentReports.value = await getStudentReportDashboard()
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const studentSummary = computed(() => {
  const reports = studentReports.value || []
  const versionCount = reports.reduce((sum, item) => sum + (item.versions?.length || 0), 0)
  const reviewedCount = reports.reduce(
    (sum, item) => sum + (item.versions || []).filter(version => version.score != null).length,
    0,
  )
  const scores = reports.map(item => item.finalScore).filter(score => score != null)
  const avg = scores.length ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length) : '-'
  return {
    reportCount: reports.length,
    versionCount,
    reviewedCount,
    avgScore: avg,
  }
})

const teacherSummary = computed(() => {
  const tasks = teacherTasks.value || []
  return tasks.reduce(
    (summary, task) => {
      summary.taskCount += 1
      summary.submittedCount += Number(task.submittedCount || 0)
      summary.pendingReviewCount += Number(task.pendingReviewCount || 0)
      summary.revisionRequiredCount += Number(task.revisionRequiredCount || 0)
      return summary
    },
    {
      taskCount: 0,
      submittedCount: 0,
      pendingReviewCount: 0,
      revisionRequiredCount: 0,
    },
  )
})

const statusMap = {
  DRAFT: { label: '草稿', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'success' },
  REVIEWED: { label: '已批改', type: 'warning' },
  REVISION_REQUIRED: { label: '需修订', type: 'danger' },
  UNSUBMITTED: { label: '未提交', type: 'info' },
}

const reviewMap = {
  REVIEWED: { label: '已批改', type: 'success' },
  REVISION_REQUIRED: { label: '需修订', type: 'danger' },
}

const statusLabel = status => statusMap[status]?.label || status || '-'
const statusTagType = status => statusMap[status]?.type || 'info'
const reviewLabel = action => reviewMap[action]?.label || '未批改'
const reviewTagType = action => reviewMap[action]?.type || 'info'

const fmt = value => {
  if (!value) return ''
  const date = new Date(Array.isArray(value) ? value.join('-') : value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const goSubmit = experimentId => {
  router.push(`/experiment/${experimentId}/submit`)
}

const goReviewDetail = (reportId, reportVersionId) => {
  router.push(`/report/${reportId}/version/${reportVersionId}`)
}

const goExperimentList = () => {
  ElMessage.info('请从左侧“实验”模块进入具体实验后提交报告')
}

const openReviewDialog = (task, student, version) => {
  reviewDialog.visible = true
  reviewDialog.task = task
  reviewDialog.student = student
  reviewDialog.version = version
  reviewForm.score = version.score ?? 90
  reviewForm.action = version.action || 'REVIEWED'
  reviewForm.commentText = version.commentText || ''
  reviewForm.revisionRequirement = version.revisionRequirement || ''
}

const submitReview = async () => {
  if (!reviewDialog.student?.reportId || !reviewDialog.version?.reportVersionId) return
  if (!reviewForm.commentText?.trim()) {
    ElMessage.warning('请填写评语')
    return
  }
  reviewSubmitting.value = true
  try {
    await scoreReport({
      reportId: reviewDialog.student.reportId,
      reportVersionId: reviewDialog.version.reportVersionId,
      score: reviewForm.score,
      commentText: reviewForm.commentText,
      action: reviewForm.action,
      revisionRequirement: reviewForm.action === 'REVISION_REQUIRED' ? reviewForm.revisionRequirement : '',
    })
    ElMessage.success('批改已提交')
    reviewDialog.visible = false
    await loadData()
  } finally {
    reviewSubmitting.value = false
  }
}
</script>

<style scoped>
.report-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.2), transparent 28%),
    linear-gradient(135deg, #183153 0%, #27548a 55%, #4f86c6 100%);
  color: #fff;
}

.page-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.82);
}

.loading-wrap {
  padding: 16px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stats-card {
  padding: 18px 20px;
  border-radius: 20px;
  color: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 108px;
  box-shadow: 0 14px 30px rgba(24, 49, 83, 0.12);
}

.stats-card.primary { background: linear-gradient(135deg, #0f766e, #14b8a6); }
.stats-card.success { background: linear-gradient(135deg, #1d4ed8, #3b82f6); }
.stats-card.warning { background: linear-gradient(135deg, #c2410c, #fb923c); }
.stats-card.danger { background: linear-gradient(135deg, #be123c, #fb7185); }
.stats-card.neutral { background: linear-gradient(135deg, #334155, #64748b); }

.stats-label {
  font-size: 13px;
  opacity: 0.86;
}

.stats-value {
  font-size: 32px;
  line-height: 1;
}

.teacher-task-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.task-card,
.report-collapse,
.student-table,
.report-collapse-item {
  border-radius: 20px;
}

.task-card {
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 30%);
  border: 1px solid #e5eef8;
  padding: 18px;
}

.task-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.task-title {
  margin: 0 0 6px;
  font-size: 20px;
  color: #16324f;
}

.task-meta {
  margin: 0;
  color: #6b7b8d;
  font-size: 13px;
}

.task-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.student-table :deep(.el-table__inner-wrapper) {
  border-radius: 16px;
}

.student-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.student-cell span {
  color: #7b8794;
  font-size: 12px;
}

.expand-panel,
.report-panel {
  padding: 6px 8px 10px;
}

.empty-version {
  padding: 12px 14px;
  border-radius: 12px;
  background: #f7f8fa;
  color: #8a94a6;
}

.version-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  color: #243b53;
}

.version-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 8px;
}

.version-summary,
.report-overview,
.dialog-summary {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: #52606d;
  font-size: 13px;
}

.review-box {
  padding: 12px 14px;
  border-radius: 14px;
  background: #eff6ff;
  color: #1e3a5f;
  line-height: 1.7;
}

.review-box.revision {
  background: #fff1f2;
  color: #9f1239;
}

.content-preview,
.dialog-preview {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
}

.content-preview :deep(.ql-editor),
.dialog-preview :deep(.ql-editor) {
  min-height: 140px;
  max-height: 320px;
  overflow: auto;
}

.version-actions {
  display: flex;
  justify-content: flex-end;
}

.report-collapse :deep(.el-collapse-item__wrap),
.report-collapse :deep(.el-collapse-item__header),
.expand-panel :deep(.el-collapse-item__wrap),
.expand-panel :deep(.el-collapse-item__header) {
  background: transparent;
}

.report-collapse :deep(.el-collapse-item__header) {
  min-height: 88px;
  padding: 0 6px;
}

.report-title-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.report-title-row p {
  margin: 6px 0 0;
  color: #7c8798;
  font-size: 13px;
}

.report-title-side {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.report-title-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  color: #52606d;
  font-size: 13px;
}

.review-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-form {
  padding-top: 8px;
}

.student-version-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.student-version-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #e5edf5;
  border-radius: 16px;
  background: #fbfdff;
}

.student-version-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-header,
  .report-title-row,
  .report-title-side,
  .page-hero {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .student-version-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
