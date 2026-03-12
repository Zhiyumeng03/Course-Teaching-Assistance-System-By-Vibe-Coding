<template>
  <div class="report-page">
    <section class="page-hero">
      <div>
        <p class="hero-eyebrow">{{ isTeacher ? '教师工作台' : '学生中心' }}</p>
        <h2 class="hero-title">{{ isTeacher ? '实验报告批改与 AI 风险辅助' : '我的实验报告' }}</h2>
        <p class="hero-desc">
          {{ isTeacher
            ? '查看提交进度、快速启动 AI 风险检测，并在同一处完成评分与评语。'
            : '查看自己的报告版本、教师批改结果，以及实验报告的 AI 风险检测结论。' }}
        </p>
      </div>
      <el-button v-if="!isTeacher" type="primary" round @click="goExperimentList">前往实验列表</el-button>
    </section>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton animated :rows="8" />
    </div>

    <template v-else-if="isTeacher">
      <section class="stats-grid">
        <article class="stats-card teal">
          <span>负责实验</span>
          <strong>{{ teacherSummary.taskCount }}</strong>
        </article>
        <article class="stats-card blue">
          <span>已提交人数</span>
          <strong>{{ teacherSummary.submittedCount }}</strong>
        </article>
        <article class="stats-card orange">
          <span>待批改</span>
          <strong>{{ teacherSummary.pendingReviewCount }}</strong>
        </article>
        <article class="stats-card rose">
          <span>需修改</span>
          <strong>{{ teacherSummary.revisionRequiredCount }}</strong>
        </article>
      </section>

      <el-empty v-if="!teacherTasks.length" description="当前没有需要处理的实验报告任务" />

      <div v-else class="task-list">
        <section v-for="task in teacherTasks" :key="task.experimentId" class="task-card">
          <div class="task-head">
            <div>
              <h3>{{ task.experimentTitle }}</h3>
              <p>{{ task.courseName }} · 实验 ID {{ task.experimentId }}</p>
            </div>
            <div class="task-tags">
              <el-tag effect="light">总人数 {{ task.totalStudents || 0 }}</el-tag>
              <el-tag type="success" effect="light">已提交 {{ task.submittedCount || 0 }}</el-tag>
              <el-tag type="warning" effect="light">待批改 {{ task.pendingReviewCount || 0 }}</el-tag>
              <el-tag type="info" effect="light">未提交 {{ task.unsubmittedCount || 0 }}</el-tag>
            </div>
          </div>

          <el-table :data="task.students || []" empty-text="暂无学生数据" class="student-table">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-panel">
                  <div v-if="!row.reportId" class="empty-state">该学生暂未提交本实验报告。</div>
                  <el-collapse v-else class="version-collapse">
                    <el-collapse-item
                      v-for="version in row.versions || []"
                      :key="version.reportVersionId"
                      :name="version.reportVersionId"
                    >
                      <template #title>
                        <div class="version-title">
                          <strong>第 {{ version.versionNo }} 版</strong>
                          <span>{{ fmt(version.submittedAt) }}</span>
                          <el-tag size="small" :type="reviewTagType(version.action)">
                            {{ reviewLabel(version.action) }}
                          </el-tag>
                          <el-tag
                            v-if="version.aiRiskScore != null"
                            size="small"
                            effect="dark"
                            :type="riskTagType(versionRiskLevel(version))"
                          >
                            {{ riskLabel(versionRiskLevel(version)) }} · {{ version.aiRiskScore }}%
                          </el-tag>
                          <el-tag v-else size="small" type="info" effect="plain">未检测</el-tag>
                        </div>
                      </template>

                      <div class="version-body">
                        <div class="version-summary">
                          <span>字数 {{ version.wordCount || 0 }}</span>
                          <span>得分 {{ version.score ?? '-' }}</span>
                          <span v-if="version.teacherName">批改教师 {{ version.teacherName }}</span>
                        </div>

                        <div v-if="aiSummary(version)" class="ai-summary">
                          <el-icon><Warning /></el-icon>
                          <span>{{ aiSummary(version) }}</span>
                        </div>

                        <div v-if="version.commentText" class="info-box">
                          <strong>评语：</strong>{{ version.commentText }}
                        </div>
                        <div v-if="version.revisionRequirement" class="info-box revision">
                          <strong>修改要求：</strong>{{ version.revisionRequirement }}
                        </div>

                        <div v-if="version.contentHtml" class="content-preview ql-snow">
                          <div class="ql-editor" v-html="version.contentHtml" />
                        </div>

                        <div class="version-actions">
                          <el-button
                            type="warning"
                            plain
                            round
                            size="small"
                            :loading="analyzingKey === getAnalyzeKey(row.reportId, version.reportVersionId)"
                            @click="runVersionAnalysis(row.reportId, version.reportVersionId)"
                          >
                            {{ version.aiRiskScore == null ? 'AI 检测' : '重新检测' }}
                          </el-button>
                          <el-button
                            type="primary"
                            plain
                            round
                            size="small"
                            @click="goReviewDetail(row.reportId, version.reportVersionId)"
                          >
                            查看详情
                          </el-button>
                          <el-button type="primary" round size="small" @click="openReviewDialog(task, row, version)">
                            批改此版本
                          </el-button>
                        </div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="学生" min-width="160">
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

            <el-table-column label="版本数" width="100">
              <template #default="{ row }">{{ row.latestVersionNo || 0 }}</template>
            </el-table-column>

            <el-table-column label="最终得分" width="100">
              <template #default="{ row }">{{ row.finalScore ?? '-' }}</template>
            </el-table-column>

            <el-table-column label="最近提交" min-width="180">
              <template #default="{ row }">{{ fmt(row.lastSubmittedAt) || '-' }}</template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </template>

    <template v-else>
      <section class="stats-grid">
        <article class="stats-card teal">
          <span>报告数量</span>
          <strong>{{ studentSummary.reportCount }}</strong>
        </article>
        <article class="stats-card blue">
          <span>提交版本</span>
          <strong>{{ studentSummary.versionCount }}</strong>
        </article>
        <article class="stats-card orange">
          <span>已批改版本</span>
          <strong>{{ studentSummary.reviewedCount }}</strong>
        </article>
        <article class="stats-card slate">
          <span>平均得分</span>
          <strong>{{ studentSummary.avgScore }}</strong>
        </article>
      </section>

      <el-empty v-if="!studentReports.length" description="你还没有实验报告记录" />

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
                <el-tag :type="statusTagType(report.status)" effect="light">
                  {{ statusLabel(report.status) }}
                </el-tag>
                <span>版本 {{ report.latestVersionNo || 0 }}</span>
                <span>最终得分 {{ report.finalScore ?? '-' }}</span>
                <el-button type="primary" plain round size="small" @click.stop="goSubmit(report.experimentId)">
                  继续编辑
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
                    <strong>第 {{ version.versionNo }} 版</strong>
                    <span>{{ fmt(version.submittedAt) }}</span>
                    <el-tag size="small" :type="reviewTagType(version.action)">
                      {{ reviewLabel(version.action) }}
                    </el-tag>
                    <el-tag
                      v-if="version.aiRiskScore != null"
                      size="small"
                      effect="dark"
                      :type="riskTagType(versionRiskLevel(version))"
                    >
                      {{ riskLabel(versionRiskLevel(version)) }} · {{ version.aiRiskScore }}%
                    </el-tag>
                  </div>
                  <div class="version-summary">
                    <span>得分 {{ version.score ?? '-' }}</span>
                    <span>批改时间 {{ fmt(version.reviewedAt) || '未批改' }}</span>
                  </div>
                  <p v-if="aiSummary(version)" class="student-ai-summary">{{ aiSummary(version) }}</p>
                </div>

                <el-button
                  type="primary"
                  plain
                  round
                  size="small"
                  @click="goReviewDetail(report.reportId, version.reportVersionId)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </template>

    <el-dialog v-model="reviewDialog.visible" title="批改报告版本" width="760px" destroy-on-close>
      <div v-if="reviewDialog.version" class="review-dialog-body">
        <div class="dialog-summary">
          <el-tag type="info" effect="light">{{ reviewDialog.task?.experimentTitle }}</el-tag>
          <span>{{ reviewDialog.student?.studentName }}</span>
          <span>第 {{ reviewDialog.version.versionNo }} 版</span>
          <span>{{ fmt(reviewDialog.version.submittedAt) }}</span>
        </div>

        <div class="dialog-ai-bar">
          <el-tag
            v-if="reviewDialog.version.aiRiskScore != null"
            :type="riskTagType(versionRiskLevel(reviewDialog.version))"
            effect="dark"
          >
            {{ riskLabel(versionRiskLevel(reviewDialog.version)) }} · {{ reviewDialog.version.aiRiskScore }}%
          </el-tag>
          <span class="dialog-ai-text">{{ aiSummary(reviewDialog.version) || '当前版本还没有 AI 检测结果。' }}</span>
          <el-button
            type="warning"
            plain
            round
            size="small"
            :loading="analyzingKey === getAnalyzeKey(reviewDialog.student?.reportId, reviewDialog.version?.reportVersionId)"
            @click="runVersionAnalysis(reviewDialog.student?.reportId, reviewDialog.version?.reportVersionId, true)"
          >
            {{ reviewDialog.version.aiRiskScore == null ? 'AI 检测' : '重新检测' }}
          </el-button>
        </div>

        <div v-if="reviewDialog.version.contentHtml" class="dialog-preview ql-snow">
          <div class="ql-editor" v-html="reviewDialog.version.contentHtml" />
        </div>

        <el-form label-width="88px" class="review-form">
          <el-form-item label="评分">
            <el-input-number v-model="reviewForm.score" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="处理结果">
            <el-radio-group v-model="reviewForm.action">
              <el-radio-button label="REVIEWED">通过</el-radio-button>
              <el-radio-button label="REVISION_REQUIRED">需修改</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="评语">
            <el-input
              v-model="reviewForm.commentText"
              type="textarea"
              :rows="4"
              placeholder="填写评分说明、扣分点和修改建议"
            />
          </el-form-item>
          <el-form-item v-if="reviewForm.action === 'REVISION_REQUIRED'" label="修改要求">
            <el-input
              v-model="reviewForm.revisionRequirement"
              type="textarea"
              :rows="3"
              placeholder="明确指出学生需要补充或修改的内容"
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
import { Warning } from '@element-plus/icons-vue'
import {
  analyzeReportVersion,
  getStudentReportDashboard,
  getTeacherReportDashboard,
  scoreReport,
} from '@/api/report'
import { getUserInfo } from '@/utils/auth'

const router = useRouter()

const userInfo = computed(() => getUserInfo())

const isTeacher = computed(() => ['TEACHER', 'ADMIN'].includes(userInfo.value.role))
const loading = ref(true)
const analyzingKey = ref('')
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
  const avgScore = scores.length
    ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
    : '-'
  return {
    reportCount: reports.length,
    versionCount,
    reviewedCount,
    avgScore,
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
  REVISION_REQUIRED: { label: '需修改', type: 'danger' },
  UNSUBMITTED: { label: '未提交', type: 'info' },
}

const reviewMap = {
  REVIEWED: { label: '已批改', type: 'success' },
  REVISION_REQUIRED: { label: '需修改', type: 'danger' },
}

const riskMap = {
  LOW: { label: '低风险', type: 'success' },
  MEDIUM: { label: '中风险', type: 'warning' },
  HIGH: { label: '高风险', type: 'danger' },
}

const statusLabel = status => statusMap[status]?.label || status || '-'
const statusTagType = status => statusMap[status]?.type || 'info'
const reviewLabel = action => reviewMap[action]?.label || '未批改'
const reviewTagType = action => reviewMap[action]?.type || 'info'
const riskLabel = level => riskMap[level]?.label || '待判断'
const riskTagType = level => riskMap[level]?.type || 'info'

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

const parseAiResult = version => {
  if (version?.aiResult && typeof version.aiResult === 'object') {
    return version.aiResult
  }
  if (!version?.aiResultJson || typeof version.aiResultJson !== 'string') {
    return {}
  }
  try {
    return JSON.parse(version.aiResultJson)
  } catch {
    return {}
  }
}

const versionRiskLevel = version => {
  const aiResult = parseAiResult(version)
  if (aiResult?.riskLevel) return aiResult.riskLevel
  if (version?.aiRiskLevel) return version.aiRiskLevel
  const score = Number(version?.aiRiskScore)
  if (!Number.isFinite(score)) return ''
  if (score >= 75) return 'HIGH'
  if (score >= 45) return 'MEDIUM'
  return 'LOW'
}

const aiSummary = version => {
  const aiResult = parseAiResult(version)
  return aiResult?.summary || ''
}

const getAnalyzeKey = (reportId, reportVersionId) => `${reportId || ''}-${reportVersionId || ''}`

const goSubmit = experimentId => {
  router.push(`/experiment/${experimentId}/submit`)
}

const goReviewDetail = (reportId, reportVersionId) => {
  router.push(`/report/${reportId}/version/${reportVersionId}`)
}

const goExperimentList = () => {
  ElMessage.info('请从左侧“实验”模块进入具体实验后继续编辑和提交。')
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

const runVersionAnalysis = async (reportId, reportVersionId, syncDialog = false) => {
  if (!reportId || !reportVersionId) return
  const key = getAnalyzeKey(reportId, reportVersionId)
  analyzingKey.value = key
  try {
    const detail = await analyzeReportVersion(reportId, reportVersionId)
    ElMessage.success('AI 风险检测已完成')

    if (syncDialog && reviewDialog.version?.reportVersionId === reportVersionId) {
      reviewDialog.version = {
        ...reviewDialog.version,
        aiRiskScore: detail.aiRiskScore,
        aiResult: detail.aiResult,
        aiResultJson: detail.aiResultJson,
      }
    }

    await loadData()
  } finally {
    analyzingKey.value = ''
  }
}

const submitReview = async () => {
  if (!reviewDialog.student?.reportId || !reviewDialog.version?.reportVersionId) return
  if (!reviewForm.commentText?.trim()) {
    ElMessage.warning('请先填写评语')
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
      revisionRequirement:
        reviewForm.action === 'REVISION_REQUIRED' ? reviewForm.revisionRequirement : '',
    })
    ElMessage.success('批改结果已提交')
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
  gap: 18px;
  padding: 28px 30px;
  border-radius: 26px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.2), transparent 28%),
    linear-gradient(135deg, #0f3d4d 0%, #1c6b7d 52%, #52a6b8 100%);
  color: #fff;
}

.hero-eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.hero-title {
  margin: 0 0 8px;
  font-size: 30px;
  line-height: 1.2;
}

.hero-desc {
  margin: 0;
  max-width: 720px;
  color: rgba(255, 255, 255, 0.84);
  line-height: 1.7;
}

.loading-wrap {
  padding: 12px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stats-card {
  min-height: 112px;
  padding: 20px 22px;
  border-radius: 20px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.1);
}

.stats-card span {
  font-size: 13px;
  opacity: 0.86;
}

.stats-card strong {
  font-size: 34px;
  line-height: 1;
}

.stats-card.teal { background: linear-gradient(135deg, #0f766e, #14b8a6); }
.stats-card.blue { background: linear-gradient(135deg, #1d4ed8, #3b82f6); }
.stats-card.orange { background: linear-gradient(135deg, #c2410c, #fb923c); }
.stats-card.rose { background: linear-gradient(135deg, #be123c, #fb7185); }
.stats-card.slate { background: linear-gradient(135deg, #334155, #64748b); }

.task-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.task-card,
.student-table,
.report-collapse,
.report-collapse-item {
  border-radius: 22px;
}

.task-card {
  padding: 20px;
  border: 1px solid #dbe9f5;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 24%);
}

.task-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.task-head h3 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #12324a;
}

.task-head p {
  margin: 0;
  color: #708090;
  font-size: 13px;
}

.task-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
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
  font-size: 12px;
  color: #7b8794;
}

.expand-panel,
.report-panel {
  padding: 8px 8px 12px;
}

.empty-state {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f6f8fb;
  color: #8a94a6;
}

.version-collapse :deep(.el-collapse-item__header),
.version-collapse :deep(.el-collapse-item__wrap),
.report-collapse :deep(.el-collapse-item__header),
.report-collapse :deep(.el-collapse-item__wrap) {
  background: transparent;
}

.version-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: #1f2937;
}

.version-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 10px;
}

.version-summary,
.report-overview,
.dialog-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #52606d;
  font-size: 13px;
}

.ai-summary,
.dialog-ai-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff7ed;
  color: #9a3412;
}

.dialog-ai-bar {
  justify-content: space-between;
  flex-wrap: wrap;
}

.dialog-ai-text {
  flex: 1;
  min-width: 220px;
}

.info-box {
  padding: 12px 14px;
  border-radius: 14px;
  background: #eff6ff;
  color: #1e3a5f;
  line-height: 1.7;
}

.info-box.revision {
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
  flex-wrap: wrap;
  gap: 10px;
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
  color: #52606d;
  font-size: 13px;
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
  border: 1px solid #e4ecf5;
  border-radius: 16px;
  background: #fbfdff;
}

.student-version-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.student-ai-summary {
  margin: 0;
  color: #9a3412;
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

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .page-hero,
  .task-head,
  .report-title-row,
  .report-title-side {
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
