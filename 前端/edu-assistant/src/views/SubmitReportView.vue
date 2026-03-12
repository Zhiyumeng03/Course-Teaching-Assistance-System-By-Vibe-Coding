<template>
  <div class="submit-page">
    <!-- ===== 顶部导航栏 ===== -->
    <div class="sub-topbar">
      <el-button :icon="ArrowLeft" text @click="handleBack">返回</el-button>
      <div class="sub-topbar-center">
        <span class="sub-topbar-title">提交实验报告</span>
        <el-tag v-if="autoSaved" type="success" size="small" effect="light" style="margin-left: 10px">
          <el-icon><Check /></el-icon> 草稿已保存
        </el-tag>
        <el-tag v-if="report?.status === 'SUBMITTED'" type="success" size="small" effect="light" style="margin-left: 10px">
          已提交
        </el-tag>
      </div>
      <div class="sub-topbar-actions">
        <el-button :loading="draftLoading" @click="handleSave(true)">
          <el-icon><Document /></el-icon> 保存草稿
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          :icon="Promotion"
          round
          @click="handleSave(false)"
        >
          正式提交
        </el-button>
      </div>
    </div>

    <!-- ===== 加载中 ===== -->
    <div v-if="pageLoading" class="page-loading">
      <el-skeleton animated :rows="5" style="max-width: 900px; margin: 40px auto" />
    </div>

    <!-- ===== 主内容区 ===== -->
    <div v-else class="sub-body">
      <!-- ===== 左侧：报告内容 ===== -->
      <div class="sub-main">
        <!-- 实验信息摘要 -->
        <div v-if="exp" class="exp-summary-card">
          <div class="exp-summary-title">{{ exp.title }}</div>
          <div class="exp-summary-meta">
            <span v-if="exp.deadline" class="meta-chip" :class="{ overdue: isOverdue }">
              <el-icon><AlarmClock /></el-icon>
              截止：{{ fmt(exp.deadline) }}
              <el-tag v-if="isOverdue" type="danger" size="small" style="margin-left:4px">已截止</el-tag>
            </span>
            <span v-if="exp.maxScore" class="meta-chip">
              <el-icon><StarFilled /></el-icon>
              满分 {{ exp.maxScore }} 分
            </span>
          </div>
          <div v-if="exp.contentHtml || exp.objective" class="exp-content-block">
            <div class="exp-content-title">实验内容描述</div>
            <p v-if="exp.objective" class="exp-obj">实验目的：{{ exp.objective }}</p>
            <div v-if="exp.contentHtml" class="ql-snow exp-content-html">
              <div class="ql-editor exp-html" v-html="exp.contentHtml" />
            </div>
          </div>
        </div>

        <!-- 报告内容（Quill 富文本） -->
        <div class="pub-section">
          <div class="section-label">
            <el-icon><EditPen /></el-icon> 报告正文
            <span class="section-hint">请填写实验过程、结论和心得体会</span>
          </div>
          <div class="quill-wrapper">
            <QuillEditor
              ref="quillRef"
              v-model:content="form.contentHtml"
              content-type="html"
              theme="snow"
              :toolbar="quillToolbar"
              @text-change="onQuillChange"
            />
          </div>
        </div>

        <!-- 附件上传（OSS） -->
        <div class="pub-section">
          <div class="section-label">
            <el-icon><Paperclip /></el-icon> 附件
            <span class="section-hint">可上传实验代码、图片、PDF 等，单文件最大 100MB</span>
          </div>

          <!-- 已上传 OSS 附件列表 -->
          <div v-if="uploadedFiles.length" class="uploaded-list">
            <div v-for="(f, i) in uploadedFiles" :key="i" class="uploaded-item">
              <el-icon class="file-icon"><Document /></el-icon>
              <a :href="f.url" target="_blank" class="file-link">{{ f.name }}</a>
              <el-icon class="file-remove" @click="removeUploadedFile(i)"><CircleClose /></el-icon>
            </div>
          </div>

          <!-- 上传区（手动触发 OSS 上传） -->
          <el-upload
            ref="uploadRef"
            class="oss-upload"
            drag
            multiple
            :show-file-list="false"
            :http-request="ossUploadHandler"
            :before-upload="beforeUpload"
            :limit="10"
            :on-exceed="() => ElMessage.warning('最多上传 10 个附件')"
          >
            <div class="upload-drop-area">
              <el-icon v-if="!ossUploading" class="upload-icon"><UploadFilled /></el-icon>
              <el-icon v-else class="upload-icon uploading"><Loading /></el-icon>
              <div class="upload-text">
                {{ ossUploading ? '上传中…' : '拖拽文件到此处，或' }}
                <em v-if="!ossUploading">点击上传</em>
              </div>
              <div class="upload-hint">支持任意格式文件，每次最多 10 个</div>
            </div>
          </el-upload>
        </div>
      </div>

      <!-- ===== 右侧：提交信息 ===== -->
      <div class="sub-sidebar">
        <!-- 提交记录 -->
        <div class="sidebar-card">
          <div class="sidebar-card-title">
            <el-icon><Tickets /></el-icon> 提交记录
          </div>
          <div v-if="!report" class="sidebar-hint">尚未创建报告</div>
          <template v-else>
            <div class="submit-stat-row">
              <span class="stat-label">状态</span>
              <el-tag :type="reportTagType(report.status)" size="small" effect="light">
                {{ reportStatusLabel(report.status) }}
              </el-tag>
            </div>
            <div class="submit-stat-row" v-if="report.latestVersionNo">
              <span class="stat-label">提交次数</span>
              <span class="stat-value">{{ report.latestVersionNo }} 次</span>
            </div>
            <div class="submit-stat-row" v-if="report.lastSubmittedAt">
              <span class="stat-label">最近提交</span>
              <span class="stat-value">{{ fmt(report.lastSubmittedAt) }}</span>
            </div>
            <div class="submit-stat-row" v-if="report.finalScore != null">
              <span class="stat-label">得分</span>
              <span class="stat-value score">{{ report.finalScore }}</span>
            </div>
          </template>
        </div>

        <!-- 提交说明 -->
        <div class="sidebar-card">
          <div class="sidebar-card-title">
            <el-icon><InfoFilled /></el-icon> 提交说明
          </div>
          <ul class="tips-list">
            <li>草稿仅自己可见，不占提交次数</li>
            <li>正式提交后教师即可批阅</li>
            <li>若开放重交，可多次修改后重新提交</li>
            <li>附件通过阿里云 OSS 存储，请勿上传违规内容</li>
          </ul>
        </div>


      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Promotion, Document, Paperclip, UploadFilled,
  CircleClose, AlarmClock, StarFilled, EditPen, Check,
  Tickets, InfoFilled, DataAnalysis, Loading,
} from '@element-plus/icons-vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { getExperimentById } from '@/api/experiment'
import { getOrCreateReport, getLatestDraft, submitWithContent, uploadReportAttachment } from '@/api/report'
import { getUserInfo } from '@/utils/auth'

const router = useRouter()
const route  = useRoute()

// ==================== 基础信息 ====================
const experimentId = computed(() => Number(route.params.experimentId))
const userInfo = computed(() => getUserInfo())

// ==================== 数据 ====================
const pageLoading = ref(true)
const exp    = ref(null)
const report = ref(null)

const form = ref({ contentHtml: '', contentText: '' })
const wordCount = ref(0)
const quillRef  = ref(null)
const uploadRef = ref(null)

// ==================== OSS 附件 ====================
const uploadedFiles = ref([])  // [{ id, name, url }]
const ossUploading  = ref(false)

const beforeUpload = (file) => {
  const MAX = 100 * 1024 * 1024  // 100MB
  if (file.size > MAX) {
    ElMessage.error(`文件 ${file.name} 超过 100MB 限制`)
    return false
  }
  return true
}

const ossUploadHandler = async ({ file }) => {
  ossUploading.value = true
  try {
    const res = await uploadReportAttachment(file, report.value?.id)
    uploadedFiles.value.push({
      id: res.id ? Number(res.id) : null,
      name: res.name || file.name,
      url: res.url,
    })
    ElMessage.success(`${file.name} 上传成功`)
  } catch {
    ElMessage.error(`${file.name} 上传失败`)
  } finally {
    ossUploading.value = false
  }
}

const removeUploadedFile = (index) => {
  uploadedFiles.value.splice(index, 1)
}

const buildAttachmentIds = () => uploadedFiles.value
  .map(item => Number(item.id))
  .filter(id => Number.isFinite(id) && id > 0)

// ==================== Quill 工具栏 ====================
const quillToolbar = [
  [{ header: [1, 2, 3, false] }],
  ['bold', 'italic', 'underline', 'strike'],
  ['blockquote', 'code-block'],
  [{ list: 'ordered' }, { list: 'bullet' }],
  [{ indent: '-1' }, { indent: '+1' }],
  [{ color: [] }, { background: [] }],
  ['link', 'image'],
  ['clean'],
]
const onQuillChange = ({ text }) => {
  form.value.contentText = text
  wordCount.value = text ? text.trim().length : 0
  triggerAutoSave()
}

// ==================== 加载数据 ====================
const loadData = async () => {
  pageLoading.value = true
  try {
    const [expData, reportData] = await Promise.all([
      getExperimentById(experimentId.value),
      getOrCreateReport(experimentId.value),
    ])
    exp.value    = expData
    report.value = reportData

    // 加载草稿内容（如果有）
    if (reportData?.id) {
      try {
        const draft = await getLatestDraft(reportData.id)
        if (Array.isArray(draft?.attachments)) {
          uploadedFiles.value = draft.attachments.map(item => ({
            id: item.id,
            name: item.originalName || '附件',
            url: item.storagePath,
          }))
        }
        if (draft?.contentHtml) {
          form.value.contentHtml = draft.contentHtml
          form.value.contentText = draft.contentText || ''
          wordCount.value = (draft.contentText || '').trim().length
        }
      } catch { /* 无草稿 */ }
    }
  } catch { /* 拦截器处理 */ } finally {
    pageLoading.value = false
  }
}
onMounted(loadData)

// ==================== 自动保存草稿 ====================
const autoSaved = ref(false)
let autoTimer = null

const triggerAutoSave = () => {
  clearTimeout(autoTimer)
  autoTimer = setTimeout(() => {
    if (report.value?.id && form.value.contentHtml) {
      doSave(true, true)
    }
  }, 30000)
}
onBeforeUnmount(() => clearTimeout(autoTimer))

// ==================== 截止判断 ====================
const isOverdue = computed(() => {
  if (!exp.value?.deadline) return false
  return new Date(exp.value.deadline) < new Date()
})

// ==================== 保存/提交 ====================
const draftLoading  = ref(false)
const submitLoading = ref(false)

const handleSave = async (isDraft) => {
  if (!form.value.contentHtml?.trim()) {
    ElMessage.warning('请填写报告正文内容')
    return
  }
  await doSave(isDraft, false)
}

const doSave = async (isDraft, isAuto = false) => {
  if (!report.value?.id) return
  if (!isAuto) {
    isDraft ? (draftLoading.value = true) : (submitLoading.value = true)
  }
  try {
    const updated = await submitWithContent({
      reportId: report.value.id,
      contentHtml: form.value.contentHtml,
      contentText: form.value.contentText,
      attachmentIds: buildAttachmentIds(),
      saveAsDraft: isDraft,
    })
    report.value = { ...report.value, ...updated }

    if (isAuto) {
      autoSaved.value = true
      setTimeout(() => { autoSaved.value = false }, 3000)
    } else if (isDraft) {
      ElMessage.success('草稿已保存')
      autoSaved.value = true
    } else {
      ElMessage.success('报告提交成功！')
      router.back()
    }
  } catch { /* 拦截器处理 */ } finally {
    draftLoading.value  = false
    submitLoading.value = false
  }
}

// ==================== 状态辅助 ====================
const REPORT_STATUS = {
  DRAFT:     { label: '草稿', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'success' },
  REVIEWED:  { label: '已批阅', type: 'warning' },
}
const reportStatusLabel = s => REPORT_STATUS[s]?.label || s || '-'
const reportTagType     = s => REPORT_STATUS[s]?.type  || ''

const fmt = val => {
  if (!val) return ''
  const d = new Date(Array.isArray(val) ? val.join('-') : val)
  if (isNaN(d)) return String(val).slice(0, 16)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// ==================== 返回确认 ====================
const handleBack = async () => {
  if (form.value.contentHtml) {
    try {
      await ElMessageBox.confirm('当前报告内容尚未提交，确定要离开吗？', '提示', {
        confirmButtonText: '离开',
        cancelButtonText: '继续编辑',
        type: 'warning',
      })
      router.back()
    } catch { /* 取消 */ }
  } else {
    router.back()
  }
}
</script>

<style scoped>
/* ===== 页面 ===== */
.submit-page {
  min-height: 100vh;
  background: #f2f3f8;
  display: flex;
  flex-direction: column;
}

/* ===== 顶部栏 ===== */
.sub-topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 8px rgba(0,0,0,0.08);
}
.sub-topbar-center { display: flex; align-items: center; }
.sub-topbar-title { font-size: 16px; font-weight: 700; color: #1a1a2e; }
.sub-topbar-actions { display: flex; gap: 10px; }

/* ===== 主体 ===== */
.sub-body {
  flex: 1;
  display: flex;
  gap: 22px;
  padding: 24px 28px;
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}
.sub-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.page-loading { flex: 1; padding: 24px 28px; }

/* ===== 实验信息摘要卡片 ===== */
.exp-summary-card {
  background: linear-gradient(135deg, #6c63ff 0%, #3b82f6 100%);
  border-radius: 16px;
  padding: 20px 24px;
  color: #fff;
}
.exp-summary-title { font-size: 17px; font-weight: 700; margin-bottom: 10px; }
.exp-summary-meta { display: flex; gap: 12px; flex-wrap: wrap; }
.exp-content-block {
  margin-top: 12px;
  padding: 14px 16px;
  background: rgba(255,255,255,0.96);
  border-radius: 12px;
}
.exp-content-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #2d3a4b;
}
.exp-obj {
  font-size: 13px; color: #555;
  padding: 8px 12px; background: #f8f8fc; border-radius: 8px; margin: 8px 0;
}
.exp-html { padding: 8px 0; font-size: 14px; }
.exp-content-html {
  border-radius: 8px;
  color: #333;
}

/* ===== 报告表单区 ===== */
.pub-section {
  background: #fff;
  border-radius: 18px;
  padding: 22px 26px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.section-label {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  font-weight: 600;
  color: #444;
  margin-bottom: 12px;
}
.section-label .el-icon { color: #6c63ff; }
.section-hint { font-size: 12px; font-weight: 400; color: #aaa; margin-left: 6px; }

/* Quill */
.quill-wrapper { border-radius: 10px; overflow: auto; border: 1px solid #e8e8f0 }
:deep(.ql-toolbar.ql-snow) {
  border: none; border-bottom: 1px solid #e8e8f0;
  background: #fafafa; padding: 10px 12px;
}
:deep(.ql-container.ql-snow) { border: none; font-size: 15px; line-height: 1.7; }
:deep(.ql-editor) { min-height: 300px; padding: 20px 24px; color: #333; }
:deep(.ql-editor.ql-blank::before) { color: #c0c4cc; font-style: normal; }

/* 已上传文件 */
.uploaded-list { margin-bottom: 12px; display: flex; flex-direction: column; gap: 8px; }
.uploaded-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px;
  background: #f0efff; border-radius: 8px;
  border: 1px solid #dddaff;
}
.file-icon { color: #6c63ff; font-size: 18px; flex-shrink: 0; }
.file-link { flex: 1; font-size: 13px; color: #6c63ff; text-decoration: none; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-link:hover { text-decoration: underline; }
.file-remove { font-size: 16px; color: #ccc; cursor: pointer; transition: color 0.2s; flex-shrink: 0; }
.file-remove:hover { color: #f56c6c; }

/* OSS 上传区 */
.oss-upload { width: 100%; }
:deep(.el-upload-dragger) {
  width: 100%; height: auto; padding: 0;
  border: 2px dashed #d5d8e8; border-radius: 10px;
  background: #fafbff; transition: all 0.2s;
}
:deep(.el-upload-dragger:hover) { border-color: #6c63ff; background: #f3f2ff; }
.upload-drop-area { padding: 24px 20px; text-align: center; }
.upload-icon { font-size: 36px; color: #c0c4d0; margin-bottom: 10px; }
.upload-icon.uploading { color: #6c63ff; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.upload-text { font-size: 14px; color: #666; margin-bottom: 5px; }
.upload-text em { color: #6c63ff; font-style: normal; cursor: pointer; }
.upload-hint { font-size: 12px; color: #bbb; }

/* ===== 侧边栏 ===== */
.sub-sidebar { width: 290px; flex-shrink: 0; display: flex; flex-direction: column; gap: 14px; }
.sidebar-card { background: #fff; border-radius: 16px; padding: 18px 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
.sidebar-card-title {
  display: flex; align-items: center; gap: 7px;
  font-size: 13px; font-weight: 700; color: #555;
  margin-bottom: 14px;
}
.sidebar-card-title .el-icon { color: #6c63ff; }

.submit-stat-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-size: 13px; }
.stat-label { color: #999; }
.stat-value { color: #333; font-weight: 500; }
.stat-value.score { color: #6c63ff; font-size: 18px; font-weight: 700; }
.sidebar-hint { font-size: 12px; color: #bbb; }

.tips-list { margin: 0; padding-left: 18px; }
.tips-list li { font-size: 12px; color: #888; line-height: 1.8; }

.word-count { text-align: center; padding: 6px 0; }
.wc-num { font-size: 32px; font-weight: 700; color: #6c63ff; }
.wc-label { font-size: 14px; color: #aaa; }

/* 摘要卡片元数据 */
.meta-chip {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 12px; background: rgba(255,255,255,0.18);
  padding: 3px 10px; border-radius: 20px; color: #fff;
}
.meta-chip.overdue { background: rgba(255,80,80,0.25); }

/* Quill 展示 */
:deep(.ql-editor.exp-html) { min-height: unset; padding: 0; font-size: 13px; }

/* 响应式 */
@media (max-width: 900px) {
  .sub-body { flex-direction: column; padding: 16px; }
  .sub-sidebar { width: 100%; }
  .sub-topbar { padding: 0 16px; }
  .sub-topbar-center { display: none; }
}
</style>
