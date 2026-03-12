<template>
  <div class="detail-page">
    <div class="detail-topbar">
      <el-button text @click="goBack">返回</el-button>
      <div class="topbar-meta">
        <span>{{ detail?.experimentTitle || '报告详情' }}</span>
        <el-tag v-if="detail" :type="reviewTagType(detail.action)" effect="light">
          {{ reviewLabel(detail.action) }}
        </el-tag>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton animated :rows="8" />
    </div>

    <el-empty v-else-if="!detail" description="未找到对应的报告版本" />

    <div v-else class="detail-body">
      <section class="hero-card">
        <div>
          <h2 class="hero-title">{{ detail.experimentTitle }}</h2>
          <p class="hero-meta">
            {{ detail.courseName }} · {{ detail.studentName }} · 报告 ID {{ detail.reportId }} · 第
            {{ detail.versionNo }} 版
          </p>
        </div>
        <div class="hero-stats">
          <div class="hero-stat">
            <span>得分</span>
            <strong>{{ detail.score ?? '-' }}</strong>
          </div>
          <div class="hero-stat">
            <span>提交时间</span>
            <strong>{{ fmt(detail.submittedAt) || '-' }}</strong>
          </div>
          <div class="hero-stat">
            <span>批改时间</span>
            <strong>{{ fmt(detail.reviewedAt) || '未批改' }}</strong>
          </div>
        </div>
      </section>

      <section class="panel ai-panel">
        <div class="panel-head">
          <div>
            <h3>AI 生成风险分析</h3>
            <p>这是用于教师辅助判断的本科版轻量检测结果，结论仍需人工复核。</p>
          </div>
          <el-button
            v-if="canAnalyze"
            type="warning"
            round
            :loading="analyzing"
            @click="runAnalysis"
          >
            {{ hasAiResult ? '重新检测' : '开始检测' }}
          </el-button>
        </div>

        <template v-if="hasAiResult">
          <div class="ai-overview">
            <article class="risk-card">
              <div class="risk-header">
                <span>AI 生成占比估计</span>
                <el-tag :type="riskTagType(riskLevel)" effect="dark">{{ riskLabel(riskLevel) }}</el-tag>
              </div>
              <div class="risk-score">{{ aiRiskScore }}%</div>
              <div class="risk-bar">
                <div class="risk-bar-inner" :style="{ width: `${aiRiskScore}%` }" />
              </div>
              <p class="risk-summary">{{ aiSummary }}</p>
            </article>

            <article class="metric-card">
              <span>置信度</span>
              <strong>{{ aiConfidence }}%</strong>
              <small>{{ aiResult.model || '模型信息不可用' }}</small>
            </article>

            <article class="metric-card">
              <span>人工复核</span>
              <strong>{{ aiResult.needsHumanReview ? '建议复核' : '风险较低' }}</strong>
              <small>{{ aiResult.generatedAt || '无时间信息' }}</small>
            </article>
          </div>

          <div class="feature-grid">
            <article class="feature-card">
              <span>模板相似度</span>
              <strong>{{ percent(featureSummary.maxTemplateSimilarity) }}</strong>
            </article>
            <article class="feature-card">
              <span>同题同学相似度</span>
              <strong>{{ percent(featureSummary.maxPeerSimilarity) }}</strong>
            </article>
            <article class="feature-card">
              <span>过程风险</span>
              <strong>{{ percent(featureSummary.processRisk) }}</strong>
            </article>
            <article class="feature-card">
              <span>已提取附件</span>
              <strong>{{ featureSummary.attachmentExtractedCount ?? 0 }}</strong>
            </article>
          </div>

          <div v-if="aiExplanations.length" class="explanation-list">
            <div v-for="(item, index) in aiExplanations" :key="index" class="explanation-item">
              {{ item }}
            </div>
          </div>

          <div class="suspicious-section">
            <div class="section-title-row">
              <h4>可疑片段定位</h4>
              <span>{{ suspiciousChunks.length }} 段</span>
            </div>

            <el-empty
              v-if="!suspiciousChunks.length"
              description="本次检测没有返回需要重点关注的片段"
            />

            <div v-else class="chunk-list">
              <article v-for="chunk in suspiciousChunks" :key="chunk.chunkIndex" class="chunk-card">
                <div class="chunk-head">
                  <div class="chunk-meta">
                    <strong>片段 {{ Number(chunk.chunkIndex) + 1 }}</strong>
                    <el-tag size="small" :type="riskTagType(chunk.riskLevel)">
                      {{ riskLabel(chunk.riskLevel) }}
                    </el-tag>
                    <span>AI 分值 {{ chunk.aiScore ?? 0 }}%</span>
                    <span>置信度 {{ chunk.confidence ?? 0 }}%</span>
                  </div>
                </div>

                <blockquote class="chunk-text">{{ chunk.chunkText }}</blockquote>

                <div v-if="Array.isArray(chunk.reasons) && chunk.reasons.length" class="reason-row">
                  <el-tag
                    v-for="(reason, index) in chunk.reasons"
                    :key="index"
                    size="small"
                    effect="plain"
                    type="warning"
                  >
                    {{ reason }}
                  </el-tag>
                </div>

                <div
                  v-if="Array.isArray(chunk.matchedRefs) && chunk.matchedRefs.length"
                  class="ref-list"
                >
                  <div v-for="(ref, index) in chunk.matchedRefs" :key="index" class="ref-item">
                    <div class="ref-head">
                      <strong>{{ ref.title }}</strong>
                      <span>{{ ref.sourceType }} · {{ percent(ref.similarity) }}</span>
                    </div>
                    <p v-if="ref.note" class="ref-note">{{ ref.note }}</p>
                    <p v-if="ref.excerpt" class="ref-excerpt">{{ ref.excerpt }}</p>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </template>

        <el-empty
          v-else
          description="当前版本还没有 AI 检测结果"
        />
      </section>

      <section class="info-grid">
        <article class="panel">
          <h3>教师评语</h3>
          <div class="review-box">
            {{ detail.commentText || '教师暂未填写评语。' }}
          </div>
        </article>
        <article class="panel">
          <h3>修改要求</h3>
          <div class="review-box revision">
            {{ detail.revisionRequirement || '当前版本暂无额外修改要求。' }}
          </div>
        </article>
      </section>

      <section v-if="detail.attachments?.length" class="panel">
        <div class="panel-head simple">
          <h3>报告附件</h3>
        </div>
        <div class="attachment-list">
          <a
            v-for="file in detail.attachments"
            :key="file.id"
            :href="file.storagePath"
            target="_blank"
            class="attachment-item"
          >
            <strong>{{ file.originalName }}</strong>
            <span>{{ file.mimeType || '未知类型' }}</span>
          </a>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head simple">
          <h3>报告正文</h3>
        </div>
        <div class="content-preview ql-snow">
          <div class="ql-editor" v-html="detail.contentHtml || '<p>暂无正文内容</p>'" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { analyzeReportVersion, getReportVersionDetail } from '@/api/report'
import { getUserInfo } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const userInfo = computed(() => getUserInfo())

const canAnalyze = computed(() => ['TEACHER', 'ADMIN'].includes(userInfo.value.role))
const reportId = computed(() => Number(route.params.reportId))
const reportVersionId = computed(() => Number(route.params.reportVersionId))

const loading = ref(true)
const analyzing = ref(false)
const detail = ref(null)

const reviewMap = {
  REVIEWED: { label: '已批改', type: 'success' },
  REVISION_REQUIRED: { label: '需修改', type: 'danger' },
}

const riskMap = {
  LOW: { label: '低风险', type: 'success' },
  MEDIUM: { label: '中风险', type: 'warning' },
  HIGH: { label: '高风险', type: 'danger' },
}

const reviewLabel = action => reviewMap[action]?.label || '未批改'
const reviewTagType = action => reviewMap[action]?.type || 'info'
const riskLabel = level => riskMap[level]?.label || '待判断'
const riskTagType = level => riskMap[level]?.type || 'info'

const aiResult = computed(() => (detail.value?.aiResult && typeof detail.value.aiResult === 'object'
  ? detail.value.aiResult
  : {}))

const featureSummary = computed(() => aiResult.value?.featureSummary || {})
const suspiciousChunks = computed(() => Array.isArray(aiResult.value?.suspiciousChunks) ? aiResult.value.suspiciousChunks : [])
const aiExplanations = computed(() => Array.isArray(aiResult.value?.explanations) ? aiResult.value.explanations : [])
const aiRiskScore = computed(() => Number(detail.value?.aiRiskScore ?? aiResult.value?.overallAiRatio ?? 0))
const aiConfidence = computed(() => Number(aiResult.value?.confidence ?? 0))
const aiSummary = computed(() => aiResult.value?.summary || '已生成实验报告 AI 风险分析。')
const riskLevel = computed(() => {
  const level = aiResult.value?.riskLevel
  if (level) return level
  if (aiRiskScore.value >= 75) return 'HIGH'
  if (aiRiskScore.value >= 45) return 'MEDIUM'
  return 'LOW'
})
const hasAiResult = computed(() => Boolean(detail.value?.aiRiskScore != null || aiResult.value?.summary))

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

const percent = value => {
  const num = Number(value)
  if (!Number.isFinite(num)) return '0%'
  if (num <= 1) return `${Math.round(num * 100)}%`
  return `${Math.round(num)}%`
}

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await getReportVersionDetail(reportId.value, reportVersionId.value)
  } finally {
    loading.value = false
  }
}

const runAnalysis = async () => {
  analyzing.value = true
  try {
    detail.value = await analyzeReportVersion(reportId.value, reportVersionId.value)
    ElMessage.success('AI 风险检测已更新')
  } finally {
    analyzing.value = false
  }
}

const goBack = () => {
  router.push({ path: '/home', query: { menu: 'report' } })
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(180deg, #eef5ff 0%, #f8fbff 28%, #ffffff 100%);
}

.detail-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.topbar-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  color: #334155;
}

.loading-wrap,
.detail-body {
  max-width: 1240px;
  margin: 0 auto;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-card,
.panel {
  border: 1px solid #dce8f6;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 45px rgba(31, 41, 55, 0.08);
}

.hero-card {
  padding: 28px 30px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 30px;
  color: #102a43;
}

.hero-meta {
  margin: 0;
  color: #64748b;
}

.hero-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-stat {
  min-width: 150px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #143d6b, #2f6ea9);
  color: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-stat span {
  font-size: 12px;
  opacity: 0.82;
}

.hero-stat strong {
  font-size: 18px;
}

.panel {
  padding: 22px 24px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.panel-head h3,
.panel h3 {
  margin: 0 0 8px;
  color: #102a43;
  font-size: 20px;
}

.panel-head p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.panel-head.simple {
  margin-bottom: 14px;
}

.panel-head.simple h3 {
  margin-bottom: 0;
}

.ai-overview {
  display: grid;
  grid-template-columns: 1.8fr repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.risk-card,
.metric-card,
.feature-card,
.chunk-card,
.ref-item {
  border-radius: 20px;
  border: 1px solid #e7eef6;
  background: #fbfdff;
}

.risk-card {
  padding: 18px 20px;
}

.risk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.risk-score {
  margin: 16px 0 10px;
  font-size: 44px;
  line-height: 1;
  font-weight: 700;
  color: #b45309;
}

.risk-bar {
  height: 10px;
  border-radius: 999px;
  background: #f3f4f6;
  overflow: hidden;
}

.risk-bar-inner {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #f59e0b, #ef4444);
}

.risk-summary {
  margin: 14px 0 0;
  color: #5b6472;
  line-height: 1.7;
}

.metric-card,
.feature-card {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-card span,
.feature-card span {
  color: #64748b;
  font-size: 13px;
}

.metric-card strong,
.feature-card strong {
  font-size: 28px;
  color: #102a43;
}

.metric-card small {
  color: #94a3b8;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.explanation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.explanation-item {
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  color: #334155;
  line-height: 1.7;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-title-row h4 {
  margin: 0;
  font-size: 18px;
  color: #102a43;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chunk-card {
  padding: 16px 18px;
}

.chunk-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.chunk-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  color: #52606d;
  font-size: 13px;
}

.chunk-text {
  margin: 0;
  padding: 14px 16px;
  border-radius: 16px;
  background: #fff7ed;
  color: #7c2d12;
  line-height: 1.8;
  white-space: pre-wrap;
}

.reason-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.ref-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.ref-item {
  padding: 12px 14px;
}

.ref-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #1e293b;
}

.ref-note,
.ref-excerpt {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.review-box {
  min-height: 90px;
  padding: 14px 16px;
  border-radius: 18px;
  background: #eff6ff;
  color: #1e3a5f;
  line-height: 1.8;
  white-space: pre-wrap;
}

.review-box.revision {
  background: #fff1f2;
  color: #9f1239;
}

.attachment-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.attachment-item {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid #e5edf5;
  background: #fbfdff;
  color: inherit;
  text-decoration: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attachment-item strong {
  color: #102a43;
}

.attachment-item span {
  color: #64748b;
  font-size: 13px;
}

.content-preview {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  overflow: hidden;
}

.content-preview :deep(.ql-editor) {
  min-height: 360px;
  font-size: 15px;
  line-height: 1.8;
}

@media (max-width: 980px) {
  .detail-page {
    padding: 16px;
  }

  .hero-card,
  .panel-head {
    flex-direction: column;
  }

  .ai-overview,
  .feature-grid,
  .info-grid,
  .attachment-list {
    grid-template-columns: 1fr;
  }
}
</style>
