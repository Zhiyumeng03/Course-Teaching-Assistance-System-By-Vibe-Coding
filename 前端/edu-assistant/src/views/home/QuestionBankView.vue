<template>
  <div class="question-bank-view">
    <section class="hero-panel">
      <div>
        <h2 class="hero-title">题库管理</h2>
        <p class="hero-desc">支持教师直接建题、审批学生题目，并通过 AI 批量生成题目后再预览保存。</p>
      </div>
      <div class="hero-actions">
        <el-button
          v-if="isTeacher"
          class="hero-button secondary"
          :icon="MagicStick"
          :disabled="courseOptions.length === 0"
          @click="openAIDialog"
        >
          AI 生成
        </el-button>
        <el-button
          type="primary"
          class="hero-button"
          :icon="Plus"
          :disabled="courseOptions.length === 0"
          @click="openCreateDialog"
        >
          {{ isTeacher ? '新增题目' : '提交题目' }}
        </el-button>
      </div>
    </section>

    <section class="toolbar-panel">
      <el-select v-model="selectedCourseId" class="toolbar-item" filterable placeholder="选择课程">
        <el-option v-for="course in courseOptions" :key="course.id" :label="course.courseName" :value="course.id" />
      </el-select>

      <el-select v-model="filterType" class="toolbar-item" clearable placeholder="题型筛选">
        <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>

      <el-select v-model="filterStatus" class="toolbar-item" clearable placeholder="状态筛选">
        <el-option v-for="item in reviewStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>

      <el-input v-model="keyword" class="toolbar-item grow" :prefix-icon="Search" clearable placeholder="搜索题干、答案、知识点" />
      <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
    </section>

    <section class="stats-panel">
      <div class="stat-card"><span>当前课程</span><strong>{{ selectedCourseName || '未选择' }}</strong></div>
      <div class="stat-card"><span>已通过</span><strong>{{ approvedCount }}</strong></div>
      <div class="stat-card"><span>待审批</span><strong>{{ pendingCount }}</strong></div>
    </section>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="7" animated />
    </div>

    <el-empty v-else-if="courseOptions.length === 0" description="当前没有可用课程" :image-size="100" />

    <section v-else class="content-grid">
      <article class="question-panel">
        <div class="panel-head">
          <div>
            <h3>题目列表</h3>
            <p>{{ questionTotal }} 条结果</p>
          </div>
          <el-tag effect="plain" type="info">{{ selectedCourseName || '未选择课程' }}</el-tag>
        </div>

        <div v-if="selectedCourseId && questionTotal > 0" class="question-toolbar">
          <div class="question-toolbar-text">分页查询已启用，支持大数据量题库检索</div>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :page-sizes="[5, 10, 20]"
            :total="questionTotal"
            @current-change="handleCurrentChange"
            @size-change="handleSizeChange"
          />
        </div>

        <el-empty v-if="!selectedCourseId" description="请先选择课程" :image-size="90" />
        <el-empty v-else-if="questionTotal === 0" description="当前筛选条件下没有题目" :image-size="90" />

        <div v-else class="question-list">
          <article v-for="question in questions" :key="question.id" class="question-card">
            <div class="question-card-head">
              <div class="question-tags">
                <el-tag :type="typeTagType(question.type)">{{ typeLabel(question.type) }}</el-tag>
                <el-tag effect="plain" :type="reviewTagType(question.reviewStatus)">{{ reviewStatusLabel(question.reviewStatus) }}</el-tag>
                <el-tag effect="plain">难度 {{ question.difficulty || 3 }}</el-tag>
              </div>
            <div class="card-actions">
              <el-button v-if="canEdit(question)" link type="primary" @click="openEditDialog(question)">编辑</el-button>
              <el-button v-if="canReview(question)" link type="success" @click="openReviewDialog(question)">审批</el-button>
              <el-button v-if="canDelete(question)" link type="danger" @click="removeQuestion(question)">删除</el-button>
            </div>
              <div class="question-meta">{{ creatorLabel(question) }} | {{ formatDate(question.createdAt) }}</div>
            </div>

            <div class="question-stem question-stem-clamp">{{ question.stem }}</div>

            <div class="question-card-summary">
              <div class="knowledge-list">
                <span v-for="name in knowledgeNames(question.knowledgePointIds)" :key="name" class="knowledge-chip">{{ name }}</span>
                <span v-if="knowledgeNames(question.knowledgePointIds).length === 0" class="knowledge-chip muted">未关联知识点</span>
              </div>
              <el-button
                class="toggle-detail-button"
                link
                type="primary"
                :icon="isQuestionExpanded(question.id) ? ArrowUp : ArrowDown"
                @click="toggleQuestionExpanded(question.id)"
              >
                {{ isQuestionExpanded(question.id) ? '收起详情' : '展开详情' }}
              </el-button>
            </div>

            <div v-show="isQuestionExpanded(question.id)" class="question-detail-panel">
              <div v-if="renderContentLines(question).length" class="detail-block">
                <div class="detail-title">题目内容</div>
                <ul class="line-list">
                  <li v-for="line in renderContentLines(question)" :key="line">{{ line }}</li>
                </ul>
              </div>

              <div class="detail-block">
                <div class="detail-title">参考答案</div>
                <div class="answer-box">{{ renderAnswer(question) }}</div>
              </div>

              <div v-if="question.analysisText" class="detail-block">
                <div class="detail-title">解析</div>
                <div class="analysis-box">{{ question.analysisText }}</div>
              </div>
            </div>


          </article>
        </div>
      </article>

      <aside v-if="isTeacher" class="review-panel">
        <div class="panel-head">
          <div>
            <h3>审批工作台</h3>
            <p>显示当前课程最近的待审批题目，点击后在弹窗中查看详情并审批。</p>
          </div>
          <el-tag type="warning">{{ pendingReviewCount }}</el-tag>
        </div>

        <el-empty v-if="!selectedCourseId" description="先选择课程" :image-size="90" />
        <el-empty v-else-if="pendingReviewQuestions.length === 0" description="当前没有待审批题目" :image-size="90" />

        <div v-else class="review-list">
          <button v-for="question in pendingReviewQuestions" :key="question.id" class="review-item" type="button" @click="openReviewDialog(question)">
            <div class="review-item-top">
              <el-tag size="small" :type="typeTagType(question.type)">{{ typeLabel(question.type) }}</el-tag>
              <span class="review-item-date">{{ formatDate(question.createdAt) }}</span>
            </div>
            <div class="review-item-stem">{{ question.stem }}</div>
          </button>
        </div>
      </aside>
    </section>

    <el-dialog v-model="createDialogVisible" :title="editingId ? '编辑题目' : (isTeacher ? '新增题目' : '提交题目')" width="720px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px">
        <div class="form-grid two-col">
          <el-form-item label="课程" prop="courseId">
            <el-select v-model="form.courseId" filterable placeholder="选择课程">
              <el-option v-for="course in courseOptions" :key="course.id" :label="course.courseName" :value="course.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="题型" prop="type">
            <el-select v-model="form.type" placeholder="选择题型" @change="handleTypeChange">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="难度">
            <el-slider v-model="form.difficulty" :min="1" :max="5" show-input />
          </el-form-item>

          <el-form-item label="可见性">
            <el-switch v-model="form.visibility" active-text="学生可见" inactive-text="仅教师可见" />
          </el-form-item>
        </div>

        <el-form-item label="题干" prop="stem">
          <el-input v-model="form.stem" type="textarea" :rows="3" placeholder="输入题干" />
        </el-form-item>

        <el-form-item label="知识点">
          <el-select v-model="form.knowledgePointIds" multiple filterable collapse-tags collapse-tags-tooltip placeholder="选择知识点">
            <el-option v-for="item in formKnowledgeList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <template v-if="isOptionType">
          <div class="option-grid">
            <div v-for="option in form.options" :key="option.key" class="option-row">
              <span class="option-key">{{ option.key }}</span>
              <el-input v-model="option.text" placeholder="输入选项内容" />
              <el-radio v-if="form.type === 'SINGLE'" v-model="form.singleAnswerKey" :label="option.key">正确答案</el-radio>
              <el-checkbox v-else v-model="option.correct">正确答案</el-checkbox>
            </div>
          </div>
        </template>

        <template v-else-if="form.type === 'JUDGE'">
          <el-form-item label="正确答案">
            <el-radio-group v-model="form.judgeAnswer">
              <el-radio label="T">正确</el-radio>
              <el-radio label="F">错误</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <template v-else-if="form.type === 'BLANK'">
          <div class="detail-title">填空设置</div>
          <div v-for="(blank, index) in form.blanks" :key="blank.key" class="blank-row">
            <el-input v-model="blank.prompt" :placeholder="`空 ${index + 1} 提示`" />
            <el-input v-model="blank.answer" :placeholder="`空 ${index + 1} 答案`" />
          </div>
          <el-button text type="primary" @click="addBlank">新增空位</el-button>
        </template>

        <template v-else-if="form.type === 'SHORT'">
          <el-form-item label="作答说明">
            <el-input v-model="form.shortGuide" type="textarea" :rows="2" placeholder="可选：给学生的作答说明" />
          </el-form-item>
          <el-form-item label="参考答案">
            <el-input v-model="form.shortAnswer" type="textarea" :rows="4" placeholder="输入参考答案" />
          </el-form-item>
        </template>

        <el-form-item label="解析">
          <el-input v-model="form.analysisText" type="textarea" :rows="3" placeholder="输入解析，可选" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitCreate">保存</el-button>
        </span>
      </template>
    </el-dialog>
    <el-dialog v-model="reviewDialogVisible" title="题目审批" width="720px" @closed="activeReviewQuestion = null">
      <template v-if="activeReviewQuestion">
        <div class="review-dialog-head">
          <div class="question-tags">
            <el-tag :type="typeTagType(activeReviewQuestion.type)">{{ typeLabel(activeReviewQuestion.type) }}</el-tag>
            <el-tag effect="plain" :type="reviewTagType(activeReviewQuestion.reviewStatus)">{{ reviewStatusLabel(activeReviewQuestion.reviewStatus) }}</el-tag>
            <el-tag effect="plain">难度 {{ activeReviewQuestion.difficulty || 3 }}</el-tag>
          </div>
          <div class="question-meta">{{ creatorLabel(activeReviewQuestion) }} | {{ formatDate(activeReviewQuestion.createdAt) }}</div>
        </div>

        <div class="review-section">
          <div class="detail-title">题干</div>
          <div class="question-stem large">{{ activeReviewQuestion.stem }}</div>
        </div>

        <div v-if="renderContentLines(activeReviewQuestion).length" class="review-section">
          <div class="detail-title">题目内容</div>
          <ul class="line-list">
            <li v-for="line in renderContentLines(activeReviewQuestion)" :key="line">{{ line }}</li>
          </ul>
        </div>

        <div class="review-section">
          <div class="detail-title">参考答案</div>
          <div class="answer-box">{{ renderAnswer(activeReviewQuestion) }}</div>
        </div>

        <div v-if="activeReviewQuestion.analysisText" class="review-section">
          <div class="detail-title">解析</div>
          <div class="analysis-box">{{ activeReviewQuestion.analysisText }}</div>
        </div>

        <div class="knowledge-list">
          <span v-for="name in knowledgeNames(activeReviewQuestion.knowledgePointIds)" :key="name" class="knowledge-chip">{{ name }}</span>
          <span v-if="knowledgeNames(activeReviewQuestion.knowledgePointIds).length === 0" class="knowledge-chip muted">未关联知识点</span>
        </div>
      </template>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="reviewDialogVisible = false">关闭</el-button>
          <el-button v-if="activeReviewQuestion && canReview(activeReviewQuestion)" type="danger" plain @click="submitReview(activeReviewQuestion, 'REJECTED')">驳回</el-button>
          <el-button v-if="activeReviewQuestion && canReview(activeReviewQuestion)" type="success" @click="submitReview(activeReviewQuestion, 'APPROVED')">审批通过</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="aiGenerateDialogVisible" title="AI 生成题目" width="680px">
      <el-form :model="aiForm" label-width="108px">
        <div class="form-grid two-col">
          <el-form-item label="课程">
            <el-select v-model="aiForm.courseId" filterable placeholder="选择课程">
              <el-option v-for="course in courseOptions" :key="course.id" :label="course.courseName" :value="course.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="题型">
            <el-select v-model="aiForm.questionType" placeholder="选择题型">
              <el-option v-for="item in aiTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="难度">
            <el-slider v-model="aiForm.difficulty" :min="1" :max="5" show-input />
          </el-form-item>

          <el-form-item label="数量">
            <el-input-number v-model="aiForm.count" :min="1" :max="10" />
          </el-form-item>
        </div>

        <el-form-item label="知识点">
          <el-select v-model="aiForm.knowledgePointIds" multiple filterable collapse-tags collapse-tags-tooltip placeholder="不选则使用课程全部知识点">
            <el-option v-for="item in aiKnowledgeList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="补充要求">
          <el-input v-model="aiForm.extraRequirements" type="textarea" :rows="4" placeholder="例如：偏基础、偏实操、适合课堂小测" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="aiGenerateDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="aiGenerating" @click="submitAIGenerate">开始生成</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="aiPreviewDialogVisible" title="AI 预览结果" width="860px">
      <template #header>
        <div class="preview-header">
          <div>
            <strong>AI 预览结果</strong>
            <span class="preview-subtitle">先预览，再选择单条或批量保存到题库</span>
          </div>
          <el-button type="primary" :loading="aiSaving" @click="saveAllGeneratedQuestions">全部保存</el-button>
        </div>
      </template>

      <el-empty v-if="aiGeneratedQuestions.length === 0" description="当前没有可预览的 AI 题目" :image-size="90" />

      <div v-else class="question-list">
        <article v-for="(item, index) in aiGeneratedQuestions" :key="`${item.stem}-${index}`" class="question-card preview-card">
          <div class="question-card-head">
            <div class="question-tags">
              <el-tag :type="typeTagType(item.type)">{{ typeLabel(item.type) }}</el-tag>
              <el-tag effect="plain">难度 {{ item.difficulty || aiForm.difficulty }}</el-tag>
              <el-tag v-if="item.saved" type="success">已保存</el-tag>
            </div>
            <el-button type="primary" link :disabled="item.saved" :loading="aiSavingSingleIndex === index" @click="saveGeneratedQuestion(item, index)">
              {{ item.saved ? '已保存' : '保存到题库' }}
            </el-button>
          </div>

          <div class="question-stem question-stem-clamp">{{ item.stem }}</div>

          <div class="question-card-summary">
            <div class="knowledge-list">
              <span v-for="name in generatedKnowledgeNames(item)" :key="name" class="knowledge-chip">{{ name }}</span>
              <span v-if="generatedKnowledgeNames(item).length === 0" class="knowledge-chip muted">未关联知识点</span>
            </div>
            <el-button
              class="toggle-detail-button"
              link
              type="primary"
              :icon="isGeneratedQuestionExpanded(index) ? ArrowUp : ArrowDown"
              @click="toggleGeneratedQuestionExpanded(index)"
            >
              {{ isGeneratedQuestionExpanded(index) ? '收起详情' : '展开详情' }}
            </el-button>
          </div>

          <div v-show="isGeneratedQuestionExpanded(index)" class="question-detail-panel">
            <div v-if="renderGeneratedContentLines(item).length" class="detail-block">
              <div class="detail-title">题目内容</div>
              <ul class="line-list">
                <li v-for="line in renderGeneratedContentLines(item)" :key="line">{{ line }}</li>
              </ul>
            </div>

            <div class="detail-block">
              <div class="detail-title">参考答案</div>
              <div class="answer-box">{{ renderGeneratedAnswer(item) }}</div>
            </div>

            <div v-if="item.analysisText" class="detail-block">
              <div class="detail-title">解析</div>
              <div class="analysis-box">{{ item.analysisText }}</div>
            </div>
          </div>
        </article>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onActivated, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowUp, MagicStick, Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { getMyCourseList } from '@/api/course'
import { getKnowledgeList } from '@/api/knowledge'
import { createQuestion, deleteQuestion, generateAiQuestions, getQuestionPage, reviewQuestion, updateQuestion } from '@/api/question'
import { getUserInfo } from '@/utils/auth'

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const aiGenerating = ref(false)
const aiSaving = ref(false)
const aiSavingSingleIndex = ref(null)

const createDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const aiGenerateDialogVisible = ref(false)
const aiPreviewDialogVisible = ref(false)

const editingId = ref(null)
const activeReviewQuestion = ref(null)

const courses = ref([])
const selectedCourseId = ref(null)
const knowledgeCache = ref({})
const questions = ref([])
const questionTotal = ref(0)
const approvedCount = ref(0)
const pendingCount = ref(0)
const pendingReviewCount = ref(0)
const pendingReviewQuestions = ref([])
const aiGeneratedQuestions = ref([])
const expandedQuestionIds = ref([])
const expandedGeneratedQuestionIndexes = ref([])

const filterType = ref('')
const filterStatus = ref('')
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
let coursesLoaded = false

const typeOptions = [
  { label: '单选题', value: 'SINGLE' },
  { label: '多选题', value: 'MULTI' },
  { label: '判断题', value: 'JUDGE' },
  { label: '填空题', value: 'BLANK' },
  { label: '简答题', value: 'SHORT' },
]

const aiTypeOptions = [...typeOptions, { label: '混合题型', value: 'MIXED' }]
const reviewStatusOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
]

const userInfo = computed(() => getUserInfo())

const isTeacher = computed(() => ['TEACHER', 'ADMIN'].includes(userInfo.value.role))
const courseOptions = computed(() => (Array.isArray(courses.value) ? courses.value : []).map((course) => ({ id: course.id, courseName: course.courseName || `课程 #${course.id}` })))
const selectedCourseName = computed(() => courseOptions.value.find((item) => item.id === selectedCourseId.value)?.courseName || '')
const currentKnowledgeList = computed(() => knowledgeCache.value[selectedCourseId.value] || [])
const formKnowledgeList = computed(() => knowledgeCache.value[form.value.courseId] || [])
const aiKnowledgeList = computed(() => knowledgeCache.value[aiForm.value.courseId] || [])
const knowledgeMap = computed(() => {
  const map = {}
  currentKnowledgeList.value.forEach((item) => {
    map[item.id] = item.name || `知识点 #${item.id}`
  })
  return map
})

const createDefaultOptions = () => ['A', 'B', 'C', 'D'].map((key) => ({ key, text: '', correct: key === 'A' }))
const createBlank = (index) => ({ key: String(index), prompt: `空 ${index}`, answer: '' })
const createEmptyForm = () => ({ courseId: selectedCourseId.value || courseOptions.value[0]?.id || null, type: 'SINGLE', stem: '', difficulty: 3, visibility: true, knowledgePointIds: [], analysisText: '', options: createDefaultOptions(), singleAnswerKey: 'A', judgeAnswer: 'T', blanks: [createBlank(1), createBlank(2)], shortGuide: '', shortAnswer: '' })
const createEmptyAIForm = () => ({ courseId: selectedCourseId.value || courseOptions.value[0]?.id || null, questionType: 'SINGLE', difficulty: 3, count: 2, knowledgePointIds: [], extraRequirements: '' })

const form = ref(createEmptyForm())
const aiForm = ref(createEmptyAIForm())

const formRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  stem: [{ required: true, message: '请输入题干', trigger: 'blur' }],
}

const isOptionType = computed(() => ['SINGLE', 'MULTI'].includes(form.value.type))

async function loadCourses() {
  const data = await getMyCourseList()
  courses.value = Array.isArray(data) ? data : []
  coursesLoaded = true
  if (!selectedCourseId.value && courseOptions.value[0]) {
    selectedCourseId.value = courseOptions.value[0].id
  }
}

async function loadKnowledgeForCourse(courseId) {
  if (!courseId || knowledgeCache.value[courseId]) return
  const data = await getKnowledgeList(courseId)
  knowledgeCache.value = { ...knowledgeCache.value, [courseId]: Array.isArray(data) ? data : [] }
}

async function loadQuestions() {
  if (!selectedCourseId.value) {
    questions.value = []
    questionTotal.value = 0
    approvedCount.value = 0
    pendingCount.value = 0
    pendingReviewCount.value = 0
    pendingReviewQuestions.value = []
    expandedQuestionIds.value = []
    return
  }
  const data = await getQuestionPage({
    courseId: selectedCourseId.value,
    current: currentPage.value,
    size: pageSize.value,
    type: filterType.value || undefined,
    reviewStatus: filterStatus.value || undefined,
    keyword: keyword.value.trim() || undefined,
  })
  questions.value = (Array.isArray(data?.records) ? data.records : []).map((item) => ({
    ...item,
    knowledgePointIds: Array.isArray(item.knowledgePointIds) ? item.knowledgePointIds : [],
    contentParsed: parseJson(item.contentJson, {}),
    answerParsed: parseJson(item.answerJson, {}),
  }))
  questionTotal.value = Number(data?.total || 0)
  approvedCount.value = Number(data?.approvedCount || 0)
  pendingCount.value = Number(data?.pendingCount || 0)
  pendingReviewCount.value = Number(data?.pendingReviewCount || 0)
  pendingReviewQuestions.value = (Array.isArray(data?.pendingReviewQuestions) ? data.pendingReviewQuestions : []).map((item) => ({
    ...item,
    knowledgePointIds: Array.isArray(item.knowledgePointIds) ? item.knowledgePointIds : [],
    contentParsed: parseJson(item.contentJson, {}),
    answerParsed: parseJson(item.answerJson, {}),
  }))
  currentPage.value = Number(data?.current || currentPage.value)
  expandedQuestionIds.value = []
}

async function syncCurrentCourseData() {
  if (!selectedCourseId.value) {
    questions.value = []
    return
  }
  loading.value = true
  try {
    await loadKnowledgeForCourse(selectedCourseId.value)
    await loadQuestions()
  } finally {
    loading.value = false
  }
}

async function refreshPage() {
  const previousCourseId = selectedCourseId.value
  await loadCourses()
  if (selectedCourseId.value && selectedCourseId.value === previousCourseId) {
    await syncCurrentCourseData()
  }
}

onMounted(async () => {
  await refreshPage()
  window.addEventListener('course-list-updated', refreshPage)
})

onActivated(async () => {
  if (coursesLoaded) await refreshPage()
})

onBeforeUnmount(() => {
  if (filterReloadTimer) clearTimeout(filterReloadTimer)
  window.removeEventListener('course-list-updated', refreshPage)
})

watch(selectedCourseId, async (courseId) => {
  currentPage.value = 1
  if (!courseId) {
    questions.value = []
    questionTotal.value = 0
    pendingReviewQuestions.value = []
    return
  }
  await syncCurrentCourseData()
  if (!form.value.courseId) form.value.courseId = courseId
  if (!aiForm.value.courseId) aiForm.value.courseId = courseId
})

watch(() => form.value.courseId, async (courseId) => {
  if (!courseId) {
    form.value.knowledgePointIds = []
    return
  }
  await loadKnowledgeForCourse(courseId)
  const validIds = new Set((knowledgeCache.value[courseId] || []).map((item) => item.id))
  form.value.knowledgePointIds = form.value.knowledgePointIds.filter((id) => validIds.has(id))
})

watch(() => aiForm.value.courseId, async (courseId) => {
  if (!courseId) {
    aiForm.value.knowledgePointIds = []
    return
  }
  await loadKnowledgeForCourse(courseId)
  const validIds = new Set((knowledgeCache.value[courseId] || []).map((item) => item.id))
  aiForm.value.knowledgePointIds = aiForm.value.knowledgePointIds.filter((id) => validIds.has(id))
})

let filterReloadTimer = null

watch([filterType, filterStatus, keyword], () => {
  currentPage.value = 1
  if (filterReloadTimer) clearTimeout(filterReloadTimer)
  filterReloadTimer = setTimeout(async () => {
    await syncCurrentCourseData()
  }, 250)
})

function resetFilters() {
  filterType.value = ''
  filterStatus.value = ''
  keyword.value = ''
}

async function handleCurrentChange(page) {
  currentPage.value = page
  await syncCurrentCourseData()
}

async function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  await syncCurrentCourseData()
}

function toggleQuestionExpanded(questionId) {
  expandedQuestionIds.value = expandedQuestionIds.value.includes(questionId)
    ? expandedQuestionIds.value.filter((id) => id !== questionId)
    : [...expandedQuestionIds.value, questionId]
}

function isQuestionExpanded(questionId) {
  return expandedQuestionIds.value.includes(questionId)
}

function toggleGeneratedQuestionExpanded(index) {
  expandedGeneratedQuestionIndexes.value = expandedGeneratedQuestionIndexes.value.includes(index)
    ? expandedGeneratedQuestionIndexes.value.filter((item) => item !== index)
    : [...expandedGeneratedQuestionIndexes.value, index]
}

function isGeneratedQuestionExpanded(index) {
  return expandedGeneratedQuestionIndexes.value.includes(index)
}

function resetForm() {
  editingId.value = null
  form.value = createEmptyForm()
}

function openCreateDialog() {
  resetForm()
  createDialogVisible.value = true
}

function openEditDialog(question) {
  editingId.value = question.id
  form.value = {
    courseId: question.courseId,
    type: question.type,
    stem: question.stem || '',
    difficulty: question.difficulty || 3,
    visibility: question.visibility !== 'false',
    knowledgePointIds: Array.isArray(question.knowledgePointIds) ? [...question.knowledgePointIds] : [],
    analysisText: question.analysisText || '',
    options: createDefaultOptions(),
    singleAnswerKey: 'A',
    judgeAnswer: 'T',
    blanks: [createBlank(1), createBlank(2)],
    shortGuide: '',
    shortAnswer: '',
  }

  const content = question.contentParsed || {}
  const answer = question.answerParsed || {}
  if (question.type === 'SINGLE' || question.type === 'MULTI') {
    const keys = Object.keys(content)
    form.value.options = keys.map((key) => ({ key, text: content[key] || '', correct: Object.prototype.hasOwnProperty.call(answer, key) }))
    if (question.type === 'SINGLE') form.value.singleAnswerKey = Object.keys(answer)[0] || keys[0] || 'A'
  } else if (question.type === 'JUDGE') {
    form.value.judgeAnswer = Object.keys(answer)[0] || 'T'
  } else if (question.type === 'BLANK') {
    form.value.blanks = Object.keys(content).map((key, index) => ({ key, prompt: content[key] || `空 ${index + 1}`, answer: answer[key] || '' }))
  } else if (question.type === 'SHORT') {
    form.value.shortGuide = content.guide || ''
    form.value.shortAnswer = answer.text || ''
  }

  createDialogVisible.value = true
}

function openReviewDialog(question) {
  activeReviewQuestion.value = question
  reviewDialogVisible.value = true
}

function openAIDialog() {
  aiForm.value = createEmptyAIForm()
  aiGenerateDialogVisible.value = true
}

function handleTypeChange(type) {
  if (type === 'SINGLE' || type === 'MULTI') {
    form.value.options = createDefaultOptions()
    form.value.singleAnswerKey = 'A'
  } else if (type === 'JUDGE') {
    form.value.judgeAnswer = 'T'
  } else if (type === 'BLANK') {
    form.value.blanks = [createBlank(1), createBlank(2)]
  } else if (type === 'SHORT') {
    form.value.shortGuide = ''
    form.value.shortAnswer = ''
  }
}

function addBlank() {
  form.value.blanks.push(createBlank(form.value.blanks.length + 1))
}
function buildPayload() {
  const base = {
    courseId: form.value.courseId,
    type: form.value.type,
    stem: form.value.stem.trim(),
    difficulty: form.value.difficulty || 3,
    visibility: String(!!form.value.visibility),
    analysisText: form.value.analysisText?.trim() || null,
    knowledgePointIds: form.value.knowledgePointIds,
  }

  if (form.value.type === 'SINGLE' || form.value.type === 'MULTI') {
    const validOptions = form.value.options.filter((item) => item.text.trim()).map((item) => ({ ...item, text: item.text.trim() }))
    if (validOptions.length < 2) throw new Error('请至少填写两个选项')
    const content = {}
    validOptions.forEach((item) => {
      content[item.key] = item.text
    })
    const answer = {}
    if (form.value.type === 'SINGLE') {
      const selected = validOptions.find((item) => item.key === form.value.singleAnswerKey)
      if (!selected) throw new Error('请选择正确答案')
      answer[selected.key] = selected.text
    } else {
      const selected = validOptions.filter((item) => item.correct)
      if (selected.length === 0) throw new Error('请至少勾选一个正确答案')
      selected.forEach((item) => {
        answer[item.key] = item.text
      })
    }
    return { ...base, content, answer }
  }

  if (form.value.type === 'JUDGE') {
    const content = { T: '正确', F: '错误' }
    const answer = form.value.judgeAnswer === 'F' ? { F: '错误' } : { T: '正确' }
    return { ...base, content, answer }
  }

  if (form.value.type === 'BLANK') {
    const content = {}
    const answer = {}
    form.value.blanks.forEach((item, index) => {
      const key = String(index + 1)
      const prompt = item.prompt?.trim() || `空 ${key}`
      const blankAnswer = item.answer?.trim()
      if (!blankAnswer) throw new Error(`请填写空 ${key} 的答案`)
      content[key] = prompt
      answer[key] = blankAnswer
    })
    return { ...base, content, answer }
  }

  const content = form.value.shortGuide?.trim() ? { guide: form.value.shortGuide.trim() } : {}
  const answer = { text: form.value.shortAnswer?.trim() || '' }
  if (!answer.text) throw new Error('请填写参考答案')
  return { ...base, content, answer }
}

function buildGeneratedQuestionPayload(question) {
  return {
    courseId: aiForm.value.courseId,
    type: question.type,
    stem: question.stem,
    difficulty: question.difficulty || aiForm.value.difficulty || 3,
    visibility: question.visibility || 'true',
    analysisText: question.analysisText || null,
    knowledgePointIds: Array.isArray(question.knowledgePointIds) ? question.knowledgePointIds : [],
    sourceType: question.sourceType || 'AI',
    reviewStatus: question.reviewStatus || 'APPROVED',
    content: question.content || {},
    answer: question.answer || {},
  }
}

async function submitAIGenerate() {
  if (!aiForm.value.courseId) {
    ElMessage.warning('请先选择课程')
    return
  }
  aiGenerating.value = true
  try {
    const data = await generateAiQuestions({
      courseId: aiForm.value.courseId,
      questionType: aiForm.value.questionType,
      difficulty: aiForm.value.difficulty || 3,
      count: aiForm.value.count || 1,
      knowledgePointIds: aiForm.value.knowledgePointIds,
      extraRequirements: aiForm.value.extraRequirements?.trim() || '',
    })
    const list = Array.isArray(data?.questions) ? data.questions : []
    aiGeneratedQuestions.value = list.map((item) => ({ ...item, saved: false }))
    expandedGeneratedQuestionIndexes.value = []
    if (aiGeneratedQuestions.value.length === 0) {
      ElMessage.warning('AI 没有返回可用题目')
      return
    }
    aiGenerateDialogVisible.value = false
    aiPreviewDialogVisible.value = true
    ElMessage.success(`已生成 ${aiGeneratedQuestions.value.length} 道题目`)
  } finally {
    aiGenerating.value = false
  }
}

async function saveGeneratedQuestion(question, index) {
  if (question.saved) return
  aiSavingSingleIndex.value = index
  try {
    await createQuestion(buildGeneratedQuestionPayload(question))
    question.saved = true
    ElMessage.success('AI 题目已保存到题库')
    if (selectedCourseId.value === aiForm.value.courseId) await syncCurrentCourseData()
  } finally {
    aiSavingSingleIndex.value = null
  }
}

async function saveAllGeneratedQuestions() {
  const pendingItems = aiGeneratedQuestions.value.filter((item) => !item.saved)
  if (pendingItems.length === 0) {
    ElMessage.info('当前题目都已保存')
    return
  }
  aiSaving.value = true
  try {
    for (const item of pendingItems) {
      await createQuestion(buildGeneratedQuestionPayload(item))
      item.saved = true
    }
    ElMessage.success(`已保存 ${pendingItems.length} 道 AI 题目`)
    if (selectedCourseId.value === aiForm.value.courseId) await syncCurrentCourseData()
  } finally {
    aiSaving.value = false
  }
}

async function submitCreate() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = buildPayload()
    if (editingId.value) {
      await updateQuestion({ id: editingId.value, ...payload })
      ElMessage.success('题目已更新')
    } else {
      await createQuestion(payload)
      ElMessage.success(isTeacher.value ? '题目已保存' : '题目已提交，等待教师审批')
    }
    createDialogVisible.value = false
    await syncCurrentCourseData()
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

async function submitReview(question, status) {
  await reviewQuestion(question.id, status)
  ElMessage.success(status === 'APPROVED' ? '审批通过' : '已驳回')
  reviewDialogVisible.value = false
  activeReviewQuestion.value = null
  await syncCurrentCourseData()
}

async function removeQuestion(question) {
  await ElMessageBox.confirm(`确认删除题目“${question.stem}”吗？`, '删除题目', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteQuestion(question.id)
  ElMessage.success('题目已删除')
  await syncCurrentCourseData()
}

function parseJson(text, fallback) {
  if (!text) return fallback
  try {
    return JSON.parse(text)
  } catch {
    return fallback
  }
}

function typeLabel(type) {
  return typeOptions.find((item) => item.value === type)?.label || type || '未知题型'
}

function reviewStatusLabel(status) {
  return reviewStatusOptions.find((item) => item.value === status)?.label || status || '未知状态'
}

function typeTagType(type) {
  return { SINGLE: 'primary', MULTI: 'success', JUDGE: 'warning', BLANK: 'info', SHORT: 'danger' }[type] || 'info'
}

function reviewTagType(status) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info'
}

function creatorLabel(question) {
  if(question.sourceType=="AI")
    return "AI生成"
  return question.creatorRole === 'STUDENT' ? '学生提交' : '教师创建'
}

function renderContentLines(question) {
  const content = question.contentParsed || {}
  if (['SINGLE', 'MULTI', 'JUDGE'].includes(question.type)) return Object.entries(content).map(([key, value]) => `${key}. ${value}`)
  if (question.type === 'BLANK') return Object.entries(content).map(([key, value]) => `空 ${key}: ${value}`)
  if (question.type === 'SHORT') return content.guide ? [`作答说明: ${content.guide}`] : []
  return []
}

function renderGeneratedContentLines(question) {
  const content = question.content || {}
  if (['SINGLE', 'MULTI', 'JUDGE'].includes(question.type)) return Object.entries(content).map(([key, value]) => `${key}. ${value}`)
  if (question.type === 'BLANK') return Object.entries(content).map(([key, value]) => `空 ${key}: ${value}`)
  if (question.type === 'SHORT') return content.guide ? [`作答说明: ${content.guide}`] : []
  return []
}

function renderAnswer(question) {
  const answer = question.answerParsed || {}
  if (question.type === 'SHORT') return answer.text || '未设置'
  return Object.entries(answer).map(([key, value]) => `${key}: ${value}`).join('\n') || '未设置'
}

function renderGeneratedAnswer(question) {
  const answer = question.answer || {}
  if (question.type === 'SHORT') return answer.text || '未设置'
  return Object.entries(answer).map(([key, value]) => `${key}: ${value}`).join('\n') || '未设置'
}
function knowledgeNames(ids = []) {
  return ids.map((id) => knowledgeMap.value[id]).filter(Boolean)
}

function generatedKnowledgeNames(question) {
  if (Array.isArray(question.knowledgePointNames) && question.knowledgePointNames.length > 0) return question.knowledgePointNames
  return knowledgeNames(question.knowledgePointIds || [])
}

function formatDate(value) {
  if (!value) return '刚刚'
  return String(value).replace('T', ' ').slice(0, 16)
}

function canReview(question) {
  return isTeacher.value && question.creatorRole === 'STUDENT' && question.reviewStatus === 'PENDING'
}

function canEdit(question) {
  if (isTeacher.value) return true
  return question.creatorId === userInfo.value.id && question.reviewStatus !== 'APPROVED'
}

function canDelete(question) {
  if (isTeacher.value) return true
  return question.creatorId === userInfo.value.id && question.reviewStatus !== 'APPROVED'
}
</script>

<style scoped>
.question-bank-view {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-panel,
.toolbar-panel,
.stats-panel,
.question-panel,
.review-panel {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.hero-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
  background: linear-gradient(135deg, #143d60 0%, #285b7a 55%, #f4a261 100%);
  color: #fffaf3;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 28px;
}

.hero-desc {
  margin: 0;
  max-width: 760px;
  line-height: 1.7;
  color: rgba(255, 250, 243, 0.88);
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-button.secondary {
  border-color: rgba(255, 255, 255, 0.4);
  color: #fffaf3;
  background: rgba(255, 255, 255, 0.12);
}

.toolbar-panel,
.stats-panel,
.question-panel,
.review-panel {
  padding: 20px;
}

.toolbar-panel {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.toolbar-item {
  width: 180px;
}

.toolbar-item.grow {
  flex: 1;
  min-width: 220px;
}

.stats-panel {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stat-card {
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.stat-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 24px;
}

.content-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1.8fr) minmax(300px, 0.9fr);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0 0 6px;
  font-size: 20px;
}

.panel-head p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.question-list,
.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.question-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.question-toolbar-text {
  font-size: 13px;
  color: #64748b;
}

.question-card,
.review-item {
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 16px;
  background: #f8fafc;
}

.question-card {
  max-height: 500px;
  overflow: hidden;
}

.review-item {
  width: 100%;
  text-align: left;
  cursor: pointer;
}

.review-item:hover {
  border-color: #60a5fa;
  background: #eff6ff;
}

.question-card-head,
.review-item-top,
.review-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.question-tags,
.knowledge-list{
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.card-actions {
  margin-left: 100px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.question-card-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.question-meta,
.review-item-date,
.preview-subtitle {
  color: #64748b;
  font-size: 13px;
}

.question-stem {
  margin: 14px 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.7;
  color: #0f172a;
}

.question-stem.large {
  margin-top: 0;
  font-size: 18px;
}

.question-stem-clamp {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.review-item-stem {
  margin-top: 10px;
  line-height: 1.7;
  color: #0f172a;
}

.knowledge-chip {
  padding: 5px 10px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0f766e;
  font-size: 12px;
}

.knowledge-chip.muted {
  background: #e5e7eb;
  color: #6b7280;
}

.detail-block,
.review-section {
  margin-top: 14px;
}

.question-detail-panel {
  margin-top: 14px;
  padding-top: 4px;
  max-height: 200px;
  overflow-y: auto;
  border-top: 1px solid #e2e8f0;
}

.detail-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #475569;
}

.line-list {
  margin: 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.8;
}

.answer-box,
.analysis-box {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #334155;
}

.form-grid.two-col {
  display: grid;
  gap: 12px 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.option-grid,
.blank-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.option-row {
  display: grid;
  gap: 12px;
  align-items: center;
  grid-template-columns: 40px minmax(0, 1fr) 110px;
}

.option-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 700;
}

.dialog-footer,
.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.loading-wrap {
  padding: 24px;
  background: #ffffff;
  border-radius: 20px;
}

.toggle-detail-button {
  flex-shrink: 0;
  padding-top: 2px;
}

@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .hero-panel,
  .panel-head,
  .review-item-top,
  .question-card-head,
  .question-card-summary,
  .review-dialog-head,
  .preview-header,
  .dialog-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-panel,
  .form-grid.two-col {
    grid-template-columns: 1fr;
  }

  .toolbar-item {
    width: 100%;
  }

  .option-row {
    grid-template-columns: 1fr;
  }

  .question-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .question-toolbar :deep(.el-pagination) {
    overflow-x: auto;
  }
}
</style>
