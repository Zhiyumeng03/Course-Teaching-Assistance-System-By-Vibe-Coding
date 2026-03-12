<template>
  <div class="publish-page">
    <div class="pub-topbar">
      <el-button :icon="ArrowLeft" text @click="handleBack">返回</el-button>
      <div class="pub-topbar-center">
        <span class="pub-topbar-title">{{ isEdit ? '编辑实验' : '发布实验' }}</span>
        <el-tag v-if="autoSaved" type="success" size="small" effect="light" style="margin-left:10px">
          <el-icon><Check /></el-icon> 已自动保存草稿
        </el-tag>
      </div>
      <div class="pub-topbar-actions">
        <el-button :loading="saving" @click="handleSave('DRAFT')">存为草稿</el-button>
        <el-button type="primary" :loading="publishing" :icon="Promotion" round @click="handleSave('PUBLISHED')">
          {{ isEdit ? '保存修改' : '立即发布' }}
        </el-button>
      </div>
    </div>

    <div class="pub-body">
      <div class="pub-main">
        <div class="pub-section">
          <el-input
            v-model="form.title"
            class="title-input"
            placeholder="请输入实验标题"
            :maxlength="120"
            show-word-limit
            size="large"
          />
        </div>

        <div class="pub-section">
          <div class="section-label">
            <el-icon><Aim /></el-icon> 实验目标
          </div>
          <el-input
            v-model="form.objective"
            type="textarea"
            :rows="3"
            placeholder="简要说明本次实验的学习目标、预期成果（选填）"
            :maxlength="500"
            show-word-limit
            resize="none"
          />
        </div>

        <div class="pub-section">
          <div class="section-label">
            <el-icon><Document /></el-icon> 实验内容
            <span class="section-hint">支持富文本格式，可插入图片</span>
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

        <div class="pub-section">
          <div class="section-label">
            <el-icon><Paperclip /></el-icon> 附件
            <span class="section-hint">支持 PDF、Word、压缩包等，单文件最大 50MB</span>
          </div>

          <div v-if="uploadedFiles.length" class="uploaded-list">
            <div v-for="(file, index) in uploadedFiles" :key="file.id || index" class="upload-file-item">
              <el-icon class="file-icon"><Document /></el-icon>
              <a :href="file.url" target="_blank" rel="noopener noreferrer" class="file-link" :title="file.name">
                {{ file.name }}
              </a>
              <span class="file-size">{{ formatFileSize(file.size) }}</span>
              <el-icon class="file-remove" @click="removeUploadedFile(index)"><CircleClose /></el-icon>
            </div>
          </div>

          <el-upload
            ref="uploadRef"
            class="attachment-upload"
            drag
            multiple
            :show-file-list="false"
            :http-request="uploadAttachmentHandler"
            :before-upload="beforeUpload"
            :limit="10"
            :on-exceed="onUploadExceed"
          >
            <div class="upload-drop-area">
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="upload-text">拖拽文件到此处，或<em>点击上传</em></div>
              <div class="upload-hint">PDF / DOC / DOCX / ZIP / RAR，每次最多 10 个文件</div>
            </div>
          </el-upload>
        </div>
      </div>

      <div class="pub-sidebar">
        <div class="sidebar-card">
          <div class="sidebar-card-title">
            <el-icon><Reading /></el-icon> 所属课程
          </div>
          <el-select
            v-model="form.courseId"
            placeholder="请选择课程"
            style="width: 100%"
            :disabled="!!lockedCourseId"
          >
            <el-option
              v-for="c in myCourses"
              :key="c.id"
              :label="c.courseName"
              :value="c.id"
            />
          </el-select>
          <div v-if="lockedCourseId" class="sidebar-hint">
            课程已固定，若需修改请返回其他入口发布
          </div>
        </div>

        <div class="sidebar-card">
          <div class="sidebar-card-title">
            <el-icon><Timer /></el-icon> 时间设置
          </div>
          <div class="sidebar-field">
            <div class="sidebar-field-label">开始时间</div>
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              placeholder="实验开始时间（选填）"
              style="width: 100%"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :shortcuts="timeShortcuts"
            />
          </div>
          <div class="sidebar-field">
            <div class="sidebar-field-label">截止时间</div>
            <el-date-picker
              v-model="form.deadline"
              type="datetime"
              placeholder="设置提交截止时间（选填）"
              style="width: 100%"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :shortcuts="timeShortcuts"
            />
          </div>
        </div>

        <div class="sidebar-card">
          <div class="sidebar-card-title">
            <el-icon><Star /></el-icon> 评分设置
          </div>
          <div class="sidebar-field">
            <div class="sidebar-field-label">满分分值</div>
            <el-input-number
              v-model="form.maxScore"
              :min="1"
              :max="1000"
              style="width: 100%"
            />
          </div>
        </div>

        <div class="sidebar-card">
          <div class="sidebar-card-title">
            <el-icon><Setting /></el-icon> 提交设置
          </div>
          <div class="sidebar-switch-row">
            <div>
              <div class="switch-label">允许重复提交</div>
              <div class="switch-desc">学生可在截止前多次修改提交</div>
            </div>
            <el-switch v-model="form.allowResubmitBool" />
          </div>
        </div>

        <div class="sidebar-card">
          <div class="sidebar-card-title">
            <el-icon><Flag /></el-icon> 发布状态
          </div>
          <el-segmented
            v-model="form.status"
            :options="statusOptions"
            style="width: 100%"
          />
          <div class="status-desc">
            <template v-if="form.status === 'DRAFT'">草稿不对学生可见，随时可修改</template>
            <template v-else-if="form.status === 'PUBLISHED'">发布后学生可见，可继续编辑</template>
            <template v-else-if="form.status === 'ONGOING'">标记为进行中，提醒学生积极参与</template>
            <template v-else-if="form.status === 'CLOSED'">截止提交，学生无法再提交实验报告</template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Aim,
  ArrowLeft,
  Check,
  CircleClose,
  Document,
  Flag,
  Paperclip,
  Promotion,
  Reading,
  Setting,
  Star,
  Timer,
  UploadFilled,
} from '@element-plus/icons-vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import {
  createExperiment,
  getExperimentById,
  getFileAssetsByIds,
  updateExperiment,
  uploadExperimentAttachment,
} from '@/api/experiment'
import { getMyCourseList } from '@/api/course'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.params.id)
const lockedCourseId = computed(() => (route.query.courseId ? Number(route.query.courseId) : null))

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

const timeShortcuts = [
  { text: '1天后', value: () => { const d = new Date(); d.setDate(d.getDate() + 1); return d } },
  { text: '3天后', value: () => { const d = new Date(); d.setDate(d.getDate() + 3); return d } },
  { text: '1周后', value: () => { const d = new Date(); d.setDate(d.getDate() + 7); return d } },
  { text: '2周后', value: () => { const d = new Date(); d.setDate(d.getDate() + 14); return d } },
]

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '发布', value: 'PUBLISHED' },
  { label: '进行中', value: 'ONGOING' },
  { label: '已截止', value: 'CLOSED' },
]

const form = ref({
  courseId: lockedCourseId.value,
  title: '',
  objective: '',
  contentHtml: '',
  contentText: '',
  startTime: null,
  deadline: null,
  maxScore: 100,
  allowResubmitBool: true,
  status: 'PUBLISHED',
})

const uploadedFiles = ref([])
const uploadRef = ref(null)
const quillRef = ref(null)
const myCourses = ref([])
const pageLoading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const autoSaved = ref(false)
let autoSaveTimer = null

const attachmentIds = computed(() =>
  uploadedFiles.value
    .map(file => Number(file.id))
    .filter(id => Number.isFinite(id) && id > 0),
)

const normalizeUploadedFile = (file) => ({
  id: file?.id != null ? Number(file.id) : undefined,
  name: file?.originalName || file?.name || '未命名文件',
  url: file?.storagePath || file?.url || '',
  size: file?.fileSize ?? file?.size ?? 0,
})

const parseAttachmentIds = (value) => {
  if (!value) return []
  return String(value)
    .split(',')
    .map(item => Number(item.trim()))
    .filter(id => Number.isFinite(id) && id > 0)
}

const buildAttachmentIdsValue = () => attachmentIds.value.join(',')

const beforeUpload = (file) => {
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(`文件 ${file.name} 超过 50MB 限制，已取消上传`)
    return false
  }
  return true
}

const uploadAttachmentHandler = async ({ file }) => {
  try {
    const response = await uploadExperimentAttachment(file, isEdit.value ? Number(route.params.id) : undefined)
    uploadedFiles.value.push(normalizeUploadedFile(response))
    ElMessage.success(`${file.name} 上传成功`)
  } catch {
    ElMessage.error(`${file.name} 上传失败`)
  }
}

const onUploadExceed = () => {
  ElMessage.warning('最多同时上传 10 个附件')
}

const removeUploadedFile = (index) => {
  uploadedFiles.value.splice(index, 1)
}

const formatFileSize = (bytes) => {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const onQuillChange = ({ text }) => {
  form.value.contentText = text
  triggerAutoSave()
}

const loadExperiment = async () => {
  const id = Number(route.params.id)
  if (!id) return

  const exp = await getExperimentById(id)
  if (!exp) return

  form.value = {
    courseId: exp.courseId,
    title: exp.title || '',
    objective: exp.objective || '',
    contentHtml: exp.contentHtml || '',
    contentText: exp.contentText || '',
    startTime: exp.startTime || null,
    deadline: exp.deadline || null,
    maxScore: exp.maxScore ?? 100,
    allowResubmitBool: exp.allowResubmit !== 0,
    status: exp.status || 'PUBLISHED',
  }

  const ids = parseAttachmentIds(exp.attachmentIds)
  if (ids.length) {
    const files = await getFileAssetsByIds(ids)
    uploadedFiles.value = Array.isArray(files) ? files.map(normalizeUploadedFile) : []
  } else {
    uploadedFiles.value = []
  }
}

const loadData = async () => {
  pageLoading.value = true
  try {
    const [courseList] = await Promise.all([
      getMyCourseList(),
      isEdit.value ? loadExperiment() : Promise.resolve(),
    ])
    myCourses.value = courseList || []
    if (lockedCourseId.value && !form.value.courseId) {
      form.value.courseId = lockedCourseId.value
    }
    if (!form.value.courseId && myCourses.value.length === 1) {
      form.value.courseId = myCourses.value[0].id
    }
  } finally {
    pageLoading.value = false
  }
}

onMounted(loadData)

const triggerAutoSave = () => {
  clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(async () => {
    if (!form.value.title || !form.value.courseId) return
    await doSave('DRAFT', true)
  }, 30000)
}

onBeforeUnmount(() => clearTimeout(autoSaveTimer))

const handleSave = async (status) => {
  if (!form.value.courseId) {
    ElMessage.warning('请选择所属课程')
    return
  }
  if (!form.value.title?.trim()) {
    ElMessage.warning('请输入实验标题')
    return
  }
  await doSave(status, false)
}

const doSave = async (status, isAuto = false) => {
  if (!isAuto) {
    status === 'DRAFT' ? (saving.value = true) : (publishing.value = true)
  }

  try {
    const payload = {
      courseId: form.value.courseId,
      title: form.value.title,
      objective: form.value.objective,
      contentHtml: form.value.contentHtml,
      contentText: form.value.contentText,
      startTime: form.value.startTime,
      deadline: form.value.deadline,
      maxScore: form.value.maxScore,
      allowResubmit: form.value.allowResubmitBool ? 1 : 0,
      attachmentIds: buildAttachmentIdsValue(),
      status,
    }

    if (isEdit.value) {
      await updateExperiment({ ...payload, id: Number(route.params.id) })
      if (!isAuto) {
        ElMessage.success('实验已更新')
        router.back()
      } else {
        autoSaved.value = true
      }
    } else {
      const created = await createExperiment(payload)
      if (!isAuto) {
        ElMessage.success(status === 'DRAFT'
          ? `草稿《${payload.title}》已保存`
          : `实验《${payload.title}》发布成功`)
        router.back()
      } else {
        autoSaved.value = true
        if (created?.id) {
          router.replace({
            name: 'ExperimentEdit',
            params: { id: created.id },
            query: lockedCourseId.value ? { courseId: lockedCourseId.value } : {},
          })
        }
      }
    }
  } finally {
    saving.value = false
    publishing.value = false
  }
}

const handleBack = async () => {
  if (form.value.title || form.value.contentHtml) {
    try {
      await ElMessageBox.confirm(
        '当前编辑内容尚未保存，确定要离开吗？',
        '提示',
        {
          confirmButtonText: '离开',
          cancelButtonText: '继续编辑',
          type: 'warning',
        },
      )
      router.back()
    } catch {
      // ignore
    }
  } else {
    router.back()
  }
}
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  background: #f2f3f8;
  display: flex;
  flex-direction: column;
}

.pub-topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.08);
}
.pub-topbar-center {
  display: flex;
  align-items: center;
}
.pub-topbar-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: 0.3px;
}
.pub-topbar-actions {
  display: flex;
  gap: 10px;
}

.pub-body {
  flex: 1;
  display: flex;
  gap: 22px;
  padding: 26px 28px;
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.pub-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.pub-section {
  background: #fff;
  border-radius: 18px;
  padding: 22px 26px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.title-input :deep(.el-input__inner) {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  border: none;
  box-shadow: none;
  padding: 0;
}
.title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  padding: 0;
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
.section-label .el-icon {
  color: #6c63ff;
}
.section-hint {
  font-size: 12px;
  font-weight: 400;
  color: #aaa;
  margin-left: 6px;
}

.quill-wrapper {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e8e8f0;
}
:deep(.ql-toolbar.ql-snow) {
  border: none;
  border-bottom: 1px solid #e8e8f0;
  border-radius: 10px 10px 0 0;
  background: #fafafa;
  padding: 10px 12px;
}
:deep(.ql-container.ql-snow) {
  border: none;
  border-radius: 0 0 10px 10px;
  font-size: 15px;
  line-height: 1.7;
}
:deep(.ql-editor) {
  min-height: 360px;
  padding: 20px 24px;
  color: #333;
}
:deep(.ql-editor.ql-blank::before) {
  color: #c0c4cc;
  font-style: normal;
}

.attachment-upload {
  width: 100%;
}
:deep(.el-upload-dragger) {
  width: 100%;
  height: auto;
  padding: 0;
  border: 2px dashed #d5d8e8;
  border-radius: 10px;
  background: #fafbff;
  transition: all 0.2s;
}
:deep(.el-upload-dragger:hover),
:deep(.el-upload-dragger.is-dragover) {
  border-color: #6c63ff;
  background: #f3f2ff;
}
.upload-drop-area {
  padding: 28px 20px;
  text-align: center;
}
.upload-icon {
  font-size: 40px;
  color: #c0c4d0;
  margin-bottom: 12px;
}
.upload-text {
  font-size: 14px;
  color: #666;
  margin-bottom: 6px;
}
.upload-text em {
  color: #6c63ff;
  font-style: normal;
  cursor: pointer;
}
.upload-hint {
  font-size: 12px;
  color: #bbb;
}

.uploaded-list {
  margin-bottom: 12px;
}
.upload-file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f8f8fc;
  border-radius: 8px;
  border: 1px solid #ebebf5;
  margin-bottom: 8px;
  transition: background 0.2s;
}
.upload-file-item:hover { background: #f0efff; }
.file-icon { color: #6c63ff; font-size: 18px; flex-shrink: 0; }
.file-link {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #444;
  text-decoration: none;
}
.file-link:hover { text-decoration: underline; }
.file-size { font-size: 12px; color: #aaa; flex-shrink: 0; }
.file-remove {
  font-size: 16px;
  color: #ccc;
  cursor: pointer;
  transition: color 0.2s;
  flex-shrink: 0;
}
.file-remove:hover { color: #f56c6c; }

.pub-sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sidebar-card {
  background: #fff;
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.sidebar-card-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 700;
  color: #555;
  margin-bottom: 14px;
  letter-spacing: 0.2px;
}
.sidebar-card-title .el-icon { color: #6c63ff; }

.sidebar-field { margin-bottom: 12px; }
.sidebar-field:last-child { margin-bottom: 0; }
.sidebar-field-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}
.sidebar-hint {
  font-size: 11px;
  color: #bbb;
  margin-top: 6px;
  line-height: 1.4;
}

.sidebar-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.switch-label { font-size: 13px; color: #444; font-weight: 500; margin-bottom: 2px; }
.switch-desc  { font-size: 11px; color: #aaa; }

.status-desc {
  margin-top: 10px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  min-height: 18px;
}

@media (max-width: 900px) {
  .pub-body { flex-direction: column; padding: 16px; }
  .pub-sidebar { width: 100%; }
  .pub-topbar { padding: 0 16px; }
  .pub-topbar-center { display: none; }
}
</style>
