<template>
  <div class="detail-page">
    <div class="detail-topbar">
      <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
      <span class="topbar-title">实验详情</span>
      <div class="topbar-placeholder" />
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton animated :rows="8" style="max-width: 1160px; margin: 40px auto; padding: 0 24px" />
    </div>

    <div v-else-if="exp" class="detail-shell">
      <section class="hero-card">
        <div class="hero-copy">
          <div class="hero-badges">
            <el-tag :type="statusTagType(exp.status)" effect="dark" size="large" round>
              {{ statusLabel(exp.status) }}
            </el-tag>
            <el-tag v-if="exp.maxScore" effect="light" type="warning" size="large" round>
              <el-icon><StarFilled /></el-icon>
              满分 {{ exp.maxScore }} 分
            </el-tag>
            <el-tag v-if="exp.allowResubmit === 1" effect="light" type="success" size="large" round>
              <el-icon><Refresh /></el-icon>
              允许重复提交
            </el-tag>
          </div>
          <h1 class="hero-title">{{ exp.title }}</h1>
          <p v-if="exp.objective" class="hero-objective">{{ exp.objective }}</p>
          <div v-if="isStudent" class="hero-action">
            <el-button
              type="primary"
              size="large"
              :icon="EditPen"
              round
              :disabled="isOverdue && exp.allowResubmit !== 1"
              @click="goSubmit"
            >
              {{ myReport?.status === 'SUBMITTED' || myReport?.status === 'REVIEWED'
                ? '修改/重交报告'
                : '提交实验报告' }}
            </el-button>
          </div>
        </div>

        <div class="hero-stats">
          <div class="stat-card">
            <div class="stat-label">开始时间</div>
            <div class="stat-value">{{ fmt(exp.startTime) || '未设置' }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">截止时间</div>
            <div class="stat-value" :class="{ overdue: isOverdue }">{{ fmt(exp.deadline) || '未设置' }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">当前状态</div>
            <div class="stat-value">{{ statusLabel(exp.status) }}</div>
          </div>
        </div>
      </section>

      <div class="detail-layout">
        <main class="detail-main">
          <section v-if="exp.objective" class="content-card">
            <div class="card-label">
              <el-icon><Aim /></el-icon>
              实验目标
            </div>
            <p class="objective-text">{{ exp.objective }}</p>
          </section>

          <section class="content-card">
            <div class="card-label">
              <el-icon><Document /></el-icon>
              实验内容
            </div>
            <div v-if="exp.contentHtml" class="rich-content ql-snow">
              <div class="ql-editor" v-html="exp.contentHtml" />
            </div>
            <el-empty v-else description="暂无实验内容说明" :image-size="80" />
          </section>

          <section class="content-card">
            <div class="card-label">
              <el-icon><Paperclip /></el-icon>
              实验附件
            </div>
            <div v-if="attachments.length" class="attachment-list">
              <a
                v-for="file in attachments"
                :key="file.id"
                :href="file.url"
                target="_blank"
                rel="noopener noreferrer"
                class="attachment-item"
              >
                <div class="attachment-main">
                  <el-icon class="attachment-icon"><Paperclip /></el-icon>
                  <div class="attachment-text">
                    <div class="attachment-name">{{ file.name }}</div>
                    <div class="attachment-meta">
                      <span v-if="file.size">{{ formatFileSize(file.size) }}</span>
                      <span>点击查看或下载</span>
                    </div>
                  </div>
                </div>
                <el-icon class="attachment-arrow"><Download /></el-icon>
              </a>
            </div>
            <el-empty v-else description="暂无实验附件" :image-size="70" />
          </section>
        </main>

        <aside class="detail-sidebar">
          <section class="sidebar-card">
            <div class="sidebar-title">
              <el-icon><Calendar /></el-icon>
              时间与要求
            </div>
            <div class="sidebar-row">
              <span class="sidebar-label">开始</span>
              <span class="sidebar-value">{{ fmt(exp.startTime) || '未设置' }}</span>
            </div>
            <div class="sidebar-row">
              <span class="sidebar-label">截止</span>
              <span class="sidebar-value" :class="{ overdue: isOverdue }">{{ fmt(exp.deadline) || '未设置' }}</span>
            </div>
            <div class="sidebar-row">
              <span class="sidebar-label">重复提交</span>
              <span class="sidebar-value">{{ exp.allowResubmit === 1 ? '允许' : '不允许' }}</span>
            </div>
            <div class="sidebar-row">
              <span class="sidebar-label">附件数量</span>
              <span class="sidebar-value">{{ attachments.length }}</span>
            </div>
          </section>

          <section v-if="myReport" class="sidebar-card report-card">
            <div class="sidebar-title">
              <el-icon><Finished /></el-icon>
              我的报告
            </div>
            <div class="report-status-block">
              <el-tag :type="reportTagType(myReport.status)" effect="light" size="large">
                {{ reportStatusLabel(myReport.status) }}
              </el-tag>
            </div>
            <div class="sidebar-row" v-if="myReport.lastSubmittedAt">
              <span class="sidebar-label">最近提交</span>
              <span class="sidebar-value">{{ fmt(myReport.lastSubmittedAt) }}</span>
            </div>
            <div class="sidebar-row" v-if="myReport.finalScore !== null && myReport.finalScore !== undefined">
              <span class="sidebar-label">得分</span>
              <span class="sidebar-value score">{{ myReport.finalScore }} / {{ exp.maxScore ?? 100 }}</span>
            </div>
          </section>
        </aside>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Aim,
  AlarmClock,
  ArrowLeft,
  Calendar,
  Document,
  Download,
  EditPen,
  Finished,
  Paperclip,
  Refresh,
  StarFilled,
} from '@element-plus/icons-vue'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { getExperimentById, getFileAssetsByIds } from '@/api/experiment'
import { getOrCreateReport } from '@/api/report'
import { getUserInfo } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

const userInfo = computed(() => getUserInfo())
const isStudent = computed(() => userInfo.value.role === 'STUDENT')

const loading = ref(true)
const exp = ref(null)
const myReport = ref(null)
const attachments = ref([])

const parseAttachmentIds = (value) => {
  if (!value) return []
  return String(value)
    .split(',')
    .map(item => Number(item.trim()))
    .filter(id => Number.isFinite(id) && id > 0)
}

const normalizeAttachment = (file) => ({
  id: Number(file.id),
  name: file.originalName || file.name || '未命名文件',
  url: file.storagePath || file.url || '',
  size: file.fileSize ?? file.size ?? 0,
})

const loadData = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const expData = await getExperimentById(id)
    exp.value = expData

    const attachmentIds = parseAttachmentIds(expData?.attachmentIds)
    if (attachmentIds.length) {
      const files = await getFileAssetsByIds(attachmentIds)
      attachments.value = Array.isArray(files) ? files.map(normalizeAttachment) : []
    } else {
      attachments.value = []
    }

    if (isStudent.value && exp.value) {
      try {
        myReport.value = await getOrCreateReport(exp.value.id)
      } catch {
        myReport.value = null
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const isOverdue = computed(() => {
  if (!exp.value?.deadline) return false
  return new Date(exp.value.deadline) < new Date()
})

const STATUS_MAP = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'primary' },
  ONGOING: { label: '进行中', type: 'success' },
  CLOSED: { label: '已截止', type: 'danger' },
}
const statusLabel = (status) => STATUS_MAP[status]?.label || status || '-'
const statusTagType = (status) => STATUS_MAP[status]?.type || ''

const REPORT_STATUS = {
  DRAFT: { label: '草稿（未提交）', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'success' },
  REVIEWED: { label: '已批阅', type: 'warning' },
}
const reportStatusLabel = (status) => REPORT_STATUS[status]?.label || status || '-'
const reportTagType = (status) => REPORT_STATUS[status]?.type || ''

const fmt = (val) => {
  if (!val) return ''
  const d = new Date(Array.isArray(val) ? val.join('-') : val)
  if (isNaN(d)) return String(val).slice(0, 16)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const formatFileSize = (bytes) => {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const goSubmit = () => {
  router.push({
    name: 'SubmitReport',
    params: { experimentId: exp.value.id },
  })
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(255, 190, 92, 0.2), transparent 28%),
    linear-gradient(180deg, #f7f0e8 0%, #f2f5fb 38%, #eef2f8 100%);
  display: flex;
  flex-direction: column;
}

.detail-topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 62px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  box-shadow: 0 1px 10px rgba(15, 23, 42, 0.08);
}
.topbar-title {
  font-size: 15px;
  font-weight: 700;
  color: #16324f;
}
.topbar-placeholder {
  width: 80px;
}
.loading-wrap {
  flex: 1;
}

.detail-shell {
  max-width: 1160px;
  width: 100%;
  margin: 0 auto;
  padding: 28px 24px 120px;
  box-sizing: border-box;
}

.hero-card {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr;
  gap: 18px;
  margin-bottom: 20px;
  padding: 28px;
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(20, 65, 106, 0.96) 0%, rgba(32, 96, 149, 0.92) 55%, rgba(215, 135, 49, 0.9) 100%);
  color: #fff;
  box-shadow: 0 18px 48px rgba(33, 70, 113, 0.22);
}
.hero-badges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.hero-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 800;
  letter-spacing: 0.3px;
}
.hero-objective {
  margin: 16px 0 0;
  max-width: 720px;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.88);
}
.hero-action {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  margin-top: 80px;
}
.hero-stats {
  display: grid;
  gap: 12px;
}
.stat-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
}
.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.74);
  margin-bottom: 8px;
}
.stat-value {
  font-size: 15px;
  line-height: 1.6;
  font-weight: 600;
  color: #fff;
}
.stat-value.overdue {
  color: #ffe0da;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}
.detail-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.detail-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 84px;
}

.content-card,
.sidebar-card {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 22px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}
.content-card {
  padding: 24px 26px;
}
.sidebar-card {
  padding: 20px;
}

.card-label,
.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 700;
  color: #1b3d5a;
}
.card-label .el-icon,
.sidebar-title .el-icon {
  color: #d17a22;
}

.objective-text {
  margin: 0;
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, #fffaf2 0%, #f8f3eb 100%);
  color: #4b5563;
  font-size: 14px;
  line-height: 1.8;
}

.rich-content {
  border: 1px solid #e8edf3;
  border-radius: 16px;
  background: #fff;
}
:deep(.ql-editor) {
  min-height: 80px;
  padding: 22px 24px;
  font-size: 15px;
  line-height: 1.85;
  color: #334155;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 16px;
  background: linear-gradient(180deg, #fcf8f1 0%, #f7f2e9 100%);
  border: 1px solid #efe2cf;
  text-decoration: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.attachment-item:hover {
  transform: translateY(-1px);
  border-color: #d9b98f;
  box-shadow: 0 10px 24px rgba(209, 122, 34, 0.12);
}
.attachment-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.attachment-icon {
  flex-shrink: 0;
  color: #d17a22;
  font-size: 18px;
}
.attachment-text {
  min-width: 0;
}
.attachment-name {
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.attachment-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  color: #7b8794;
  font-size: 12px;
  flex-wrap: wrap;
}
.attachment-arrow {
  flex-shrink: 0;
  color: #8a6a43;
  font-size: 18px;
}

.sidebar-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #e5e7eb;
}
.sidebar-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.sidebar-label {
  color: #6b7280;
  font-size: 13px;
}
.sidebar-value {
  text-align: right;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.6;
  font-weight: 500;
}
.sidebar-value.overdue {
  color: #c2410c;
}
.sidebar-value.score {
  color: #b45309;
  font-weight: 700;
}
.report-card {
  border-left: 4px solid #d17a22;
}
.report-status-block {
  margin-bottom: 10px;
}

.submit-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  color: rgba(255, 255, 255, 0.88);
  font-size: 13px;
}

@media (max-width: 980px) {
  .hero-card {
    grid-template-columns: 1fr;
  }
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .detail-sidebar {
    position: static;
  }
}

@media (max-width: 768px) {
  .detail-shell {
    padding: 16px 16px 100px;
  }
  .detail-topbar {
    padding: 0 16px;
  }
  .hero-card {
    padding: 20px;
    border-radius: 22px;
  }
  .hero-title {
    font-size: 24px;
  }
  .content-card,
  .sidebar-card {
    border-radius: 18px;
  }
}
</style>
