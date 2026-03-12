<template>
  <div class="module-view">
    <section class="hero-panel">
      <div class="module-header">
        <div>
          <h2 class="module-title">知识点目录</h2>
          <p class="module-desc">
            默认展示全部课程的知识树。课程筛选和搜索只在需要时使用，不影响首次浏览完整知识点。
          </p>
        </div>
        <el-button
          v-if="isTeacher"
          type="primary"
          :icon="Plus"
          round
          class="add-button"
          :disabled="courseOptions.length === 0"
          @click="openCreateDialog()"
        >
          添加知识点
        </el-button>
      </div>

      <div class="hero-stats">
        <div class="stat-card">
          <div class="stat-label">展示范围</div>
          <div class="stat-value">{{ selectedCourseName || '全部课程' }}</div>
          <div class="stat-hint">默认聚合所有课程知识点</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">知识点总数</div>
          <div class="stat-value">{{ knowledgeList.length }}</div>
          <div class="stat-hint">当前账号可见课程下的全部节点</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">树中节点</div>
          <div class="stat-value">{{ filteredNodeCount }}</div>
          <div class="stat-hint">包含课程目录节点与匹配后的知识点</div>
        </div>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-head">
        <div>
          <div class="filter-title">浏览与过滤</div>
          <div class="filter-subtitle">进入页面先看全量知识树，再按课程或关键词缩小范围。</div>
        </div>
        <el-button text @click="resetFilters">重置</el-button>
      </div>

      <div class="filter-grid">
        <el-select
          v-model="selectedCourseId"
          class="filter-item"
          filterable
          clearable
          placeholder="全部课程"
        >
          <el-option label="全部课程" :value="null" />
          <el-option
            v-for="course in courseOptions"
            :key="course.id"
            :label="course.courseName"
            :value="course.id"
          />
        </el-select>

        <el-input
          v-model="searchKeyword"
          class="filter-item"
          :prefix-icon="Search"
          clearable
          placeholder="搜索知识点名称、描述或路径"
        />
      </div>

      <div class="filter-summary">
        <span class="summary-chip">
          <el-icon><Reading /></el-icon>
          范围：{{ selectedCourseName || '全部课程' }}
        </span>
        <span class="summary-chip">
          <el-icon><Search /></el-icon>
          关键词：{{ searchKeyword.trim() || '未过滤' }}
        </span>
        <span class="summary-chip" v-if="selectedTreeNode">
          <el-icon><CollectionTag /></el-icon>
          当前选中：{{ selectedTreeNode.name }}
        </span>
      </div>
    </section>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="8" animated />
    </div>

    <el-empty
      v-else-if="courseOptions.length === 0"
      description="当前没有可用课程"
      :image-size="120"
    />

    <section v-else-if="knowledgeList.length === 0" class="empty-state">
      <el-empty description="当前账号下还没有知识点数据" :image-size="110" />
      <el-button v-if="isTeacher" type="primary" plain @click="openCreateDialog()">
        添加首个知识点
      </el-button>
    </section>

    <section v-else class="content-grid">
      <article class="tree-panel">
        <div class="panel-head">
          <div>
            <h3>知识树</h3>
            <p>全部课程按课程目录分组展示，搜索时会自动保留命中的祖先节点。</p>
          </div>
          <div class="panel-meta">{{ filteredNodeCount }} 个节点</div>
        </div>

        <el-empty
          v-if="filteredTree.length === 0"
          description="没有匹配的知识点"
          :image-size="90"
        />

        <el-tree
          v-else
          :data="filteredTree"
          node-key="key"
          highlight-current
          :expand-on-click-node="false"
          :props="treeProps"
          @node-click="handleNodeClick"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <div class="tree-node-main">
                <el-icon class="tree-icon">
                  <Reading v-if="data.nodeType === 'course'" />
                  <FolderOpened v-else-if="data.children?.length" />
                  <Document v-else />
                </el-icon>
                <span class="tree-label">{{ data.name }}</span>
              </div>
              <div class="tree-tags">
                <el-tag v-if="data.nodeType === 'course'" size="small" type="success" effect="plain">
                  课程
                </el-tag>
                <el-tag v-else size="small" effect="plain">L{{ data.level || 1 }}</el-tag>
                <el-tag size="small" type="info" effect="plain">
                  {{ data.children?.length ? `${data.children.length} 子节点` : '叶子节点' }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-tree>
      </article>

      <aside class="detail-panel">
        <div class="panel-head">
          <div>
            <h3>节点详情</h3>
            <p>可查看课程分组或具体知识点信息，教师可在任意课程或节点下继续添加。</p>
          </div>
        </div>

        <div v-if="selectedTreeNode" class="detail-card">
          <div class="detail-title-row">
            <h4>{{ selectedTreeNode.name }}</h4>
            <el-button
              v-if="isTeacher"
              type="primary"
              link
              @click="openCreateDialog(createParentNode)"
            >
              {{ selectedTreeNode.nodeType === 'course' ? '在该课程下添加根节点' : '在此节点下添加' }}
            </el-button>
          </div>

          <template v-if="selectedTreeNode.nodeType === 'course'">
            <div class="detail-tags">
              <el-tag effect="dark" type="success">课程目录</el-tag>
              <el-tag effect="plain">课程 ID {{ selectedTreeNode.courseId }}</el-tag>
            </div>
            <div class="detail-block">
              <div class="detail-label">课程名称</div>
              <div class="detail-value">{{ selectedTreeNode.name }}</div>
            </div>
            <div class="detail-block">
              <div class="detail-label">知识点数量</div>
              <div class="detail-value">{{ courseKnowledgeCount(selectedTreeNode.courseId) }} 个</div>
            </div>
            <div class="detail-block">
              <div class="detail-label">说明</div>
              <div class="detail-value">该课程下的知识点以树状结构统一展示在左侧。</div>
            </div>
          </template>

          <template v-else>
            <div class="detail-tags">
              <el-tag effect="dark" type="success">层级 {{ selectedTreeNode.level || 1 }}</el-tag>
              <el-tag effect="plain">ID {{ selectedTreeNode.id }}</el-tag>
              <el-tag effect="plain">路径 {{ selectedTreeNode.path || '-' }}</el-tag>
            </div>

            <div class="detail-block">
              <div class="detail-label">所属课程</div>
              <div class="detail-value">{{ courseNameById(selectedTreeNode.courseId) }}</div>
            </div>

            <div class="detail-block">
              <div class="detail-label">上级节点</div>
              <div class="detail-value">{{ parentName(selectedTreeNode.parentId) }}</div>
            </div>

            <div class="detail-block">
              <div class="detail-label">说明</div>
              <div class="detail-value multiline">
                {{ selectedTreeNode.description || '暂无描述' }}
              </div>
            </div>
          </template>
        </div>

        <el-empty v-else description="请选择一个节点查看详情" :image-size="90" />
      </aside>
    </section>

    <el-dialog
      v-model="createDialogVisible"
      title="添加知识点"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="createForm" :rules="formRules" label-width="92px">
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="createForm.courseId" placeholder="选择课程" filterable>
            <el-option
              v-for="course in courseOptions"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="父级目录">
          <el-select
            v-model="createForm.parentId"
            clearable
            filterable
            placeholder="不选则新增为根节点"
          >
            <el-option label="作为根节点" :value="null" />
            <el-option
              v-for="option in parentOptions"
              :key="option.id"
              :label="option.label"
              :value="option.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="知识点名称" prop="name">
          <el-input v-model="createForm.name" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="节点说明">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="可选，补充该知识点的内容说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onActivated, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CollectionTag, Document, FolderOpened, Plus, Reading, Search } from '@element-plus/icons-vue'
import { getMyCourseList } from '@/api/course'
import { createKnowledgePoint, getKnowledgeList } from '@/api/knowledge'
import { getUserInfo } from '@/utils/auth'

const formRef = ref(null)
const treeProps = { label: 'name', children: 'children' }

const userInfo = computed(() => getUserInfo())

const isTeacher = computed(() => ['TEACHER', 'ADMIN'].includes(userInfo.value.role))

const loading = ref(false)
const submitting = ref(false)
const courses = ref([])
const selectedCourseId = ref(null)
const knowledgeList = ref([])
const selectedNodeKey = ref(null)
const searchKeyword = ref('')
const createDialogVisible = ref(false)
const createForm = ref({
  courseId: null,
  parentId: null,
  name: '',
  description: '',
})

const formRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  name: [{ required: true, message: '请输入知识点名称', trigger: 'blur' }],
}

let coursesLoaded = false

const courseOptions = computed(() =>
  courses.value
    .map((course) => ({
      id: course.id,
      courseName: course.courseName || `课程 #${course.id}`,
    }))
    .sort((a, b) => String(a.courseName).localeCompare(String(b.courseName), 'zh-CN')),
)

const courseNameMap = computed(() => {
  const map = {}
  courseOptions.value.forEach((course) => {
    map[course.id] = course.courseName
  })
  return map
})

const selectedCourseName = computed(() => courseNameMap.value[selectedCourseId.value] || '')

const courseNameById = (courseId) => courseNameMap.value[courseId] || `课程 #${courseId}`

const loadCourses = async () => {
  const courseList = await getMyCourseList()
  courses.value = Array.isArray(courseList) ? courseList : []
  coursesLoaded = true
  if (selectedCourseId.value && !courses.value.some((course) => course.id === selectedCourseId.value)) {
    selectedCourseId.value = null
  }
}

const loadKnowledge = async () => {
  if (courses.value.length === 0) {
    knowledgeList.value = []
    selectedNodeKey.value = null
    return
  }

  loading.value = true
  try {
    const lists = await Promise.all(
      courses.value.map((course) =>
        getKnowledgeList(course.id)
          .then((data) => (Array.isArray(data) ? data : []))
          .catch(() => []),
      ),
    )

    knowledgeList.value = lists.flat()
    const keys = new Set([
      ...courseOptions.value.map((course) => `course-${course.id}`),
      ...knowledgeList.value.map((item) => `kp-${item.id}`),
    ])
    if (!keys.has(selectedNodeKey.value)) {
      selectedNodeKey.value = courseOptions.value[0] ? `course-${courseOptions.value[0].id}` : null
    }
  } finally {
    loading.value = false
  }
}

const syncCoursesAndKnowledge = async () => {
  await loadCourses()
  await loadKnowledge()
}

const handleCourseListUpdated = async () => {
  await syncCoursesAndKnowledge()
}

onMounted(async () => {
  loading.value = true
  try {
    await syncCoursesAndKnowledge()
    window.addEventListener('course-list-updated', handleCourseListUpdated)
  } finally {
    loading.value = false
  }
})

onActivated(async () => {
  if (!coursesLoaded) return
  await syncCoursesAndKnowledge()
})

onBeforeUnmount(() => {
  window.removeEventListener('course-list-updated', handleCourseListUpdated)
})

const visibleKnowledgeList = computed(() =>
  selectedCourseId.value
    ? knowledgeList.value.filter((item) => item.courseId === selectedCourseId.value)
    : knowledgeList.value,
)

const knowledgeMap = computed(() => {
  const map = {}
  visibleKnowledgeList.value.forEach((item) => {
    map[item.id] = {
      ...item,
      key: `kp-${item.id}`,
      nodeType: 'knowledge',
      children: [],
    }
  })
  return map
})

const buildKnowledgeTree = (items) => {
  const map = {}
  items.forEach((item) => {
    map[item.id] = {
      ...item,
      key: `kp-${item.id}`,
      nodeType: 'knowledge',
      children: [],
    }
  })

  const roots = []
  items.forEach((item) => {
    const current = map[item.id]
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(current)
    } else {
      roots.push(current)
    }
  })

  const sortNodes = (nodes) => {
    nodes.sort((a, b) => {
      const sortDiff = (a.sortNo || 0) - (b.sortNo || 0)
      return sortDiff !== 0 ? sortDiff : (a.id || 0) - (b.id || 0)
    })
    nodes.forEach((node) => sortNodes(node.children || []))
    return nodes
  }

  return sortNodes(roots)
}

const fullTree = computed(() => {
  if (selectedCourseId.value) {
    return buildKnowledgeTree(visibleKnowledgeList.value)
  }

  return courseOptions.value.map((course) => ({
    key: `course-${course.id}`,
    id: null,
    courseId: course.id,
    nodeType: 'course',
    name: course.courseName,
    children: buildKnowledgeTree(knowledgeList.value.filter((item) => item.courseId === course.id)),
  }))
})

const cloneNode = (node) => ({
  ...node,
  children: (node.children || []).map(cloneNode),
})

const filterTree = (nodes, keyword) => {
  if (!keyword) {
    return nodes.map(cloneNode)
  }

  const lowerKeyword = keyword.toLowerCase()
  return nodes
    .map((node) => {
      const children = filterTree(node.children || [], keyword)
      const matches = [
        node.name,
        node.description,
        node.path,
        node.nodeType === 'course' ? courseNameById(node.courseId) : '',
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(lowerKeyword)

      if (!matches && children.length === 0) {
        return null
      }

      return {
        ...node,
        children,
      }
    })
    .filter(Boolean)
}

const filteredTree = computed(() => filterTree(fullTree.value, searchKeyword.value.trim()))

const countNodes = (nodes) =>
  nodes.reduce((total, node) => total + 1 + countNodes(node.children || []), 0)

const filteredNodeCount = computed(() => countNodes(filteredTree.value))

const selectedTreeNode = computed(() => {
  if (!selectedNodeKey.value) return null
  if (String(selectedNodeKey.value).startsWith('course-')) {
    const courseId = Number(String(selectedNodeKey.value).replace('course-', ''))
    const course = courseOptions.value.find((item) => item.id === courseId)
    return course
      ? { key: selectedNodeKey.value, nodeType: 'course', courseId, name: course.courseName }
      : null
  }

  const id = Number(String(selectedNodeKey.value).replace('kp-', ''))
  const node = knowledgeList.value.find((item) => item.id === id)
  return node ? { ...node, key: selectedNodeKey.value, nodeType: 'knowledge' } : null
})

const createParentNode = computed(() => {
  if (!selectedTreeNode.value) return null
  return selectedTreeNode.value.nodeType === 'course' ? null : selectedTreeNode.value
})

const courseKnowledgeCount = (courseId) =>
  knowledgeList.value.filter((item) => item.courseId === courseId).length

const parentName = (parentId) => {
  if (!parentId) return '根节点'
  return knowledgeList.value.find((item) => item.id === parentId)?.name || `节点 #${parentId}`
}

const handleNodeClick = (data) => {
  selectedNodeKey.value = data.key
}

const resetFilters = () => {
  selectedCourseId.value = null
  searchKeyword.value = ''
}

const parentOptions = computed(() =>
  knowledgeList.value
    .filter((item) => item.courseId === createForm.value.courseId)
    .slice()
    .sort((a, b) => {
      const pathA = a.path || ''
      const pathB = b.path || ''
      return pathA.localeCompare(pathB, 'zh-CN')
    })
    .map((item) => ({
      id: item.id,
      label: `${'  '.repeat(Math.max((item.level || 1) - 1, 0))}${item.name}`,
    })),
)

const resetCreateForm = () => {
  createForm.value = {
    courseId: selectedCourseId.value || null,
    parentId: null,
    name: '',
    description: '',
  }
}

const openCreateDialog = (parent = null) => {
  resetCreateForm()
  if (parent?.id) {
    createForm.value.parentId = parent.id
    createForm.value.courseId = parent.courseId
  } else if (selectedTreeNode.value?.nodeType === 'course') {
    createForm.value.courseId = selectedTreeNode.value.courseId
  } else if (selectedTreeNode.value?.courseId) {
    createForm.value.courseId = selectedTreeNode.value.courseId
  } else if (courseOptions.value.length === 1) {
    createForm.value.courseId = courseOptions.value[0].id
  }
  createDialogVisible.value = true
}

const submitCreate = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    await createKnowledgePoint({
      courseId: createForm.value.courseId,
      parentId: createForm.value.parentId,
      name: createForm.value.name.trim(),
      description: createForm.value.description?.trim() || null,
    })
    ElMessage.success('知识点已添加')
    createDialogVisible.value = false
    await loadKnowledge()
    selectedNodeKey.value = createForm.value.courseId ? `course-${createForm.value.courseId}` : selectedNodeKey.value
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.module-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-panel {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.16), transparent 34%),
    linear-gradient(135deg, #5c1d32 0%, #8b3a3a 48%, #c56b3d 100%);
  color: #fff7f0;
  box-shadow: 0 18px 40px rgba(112, 44, 44, 0.18);
}

.module-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.module-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.module-desc {
  margin: 0;
  max-width: 720px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 247, 240, 0.84);
}

.add-button {
  border: none;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

.add-button:hover,
.add-button:focus {
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.stat-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(8px);
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 247, 240, 0.76);
}

.stat-value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-hint {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255, 247, 240, 0.76);
}

.filter-panel {
  padding: 22px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #fff9f4 100%);
  border: 1px solid rgba(164, 92, 61, 0.12);
  box-shadow: 0 10px 28px rgba(141, 82, 55, 0.08);
}

.filter-head,
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.filter-title,
.panel-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #5b2b2f;
}

.filter-subtitle,
.panel-head p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #8a6b63;
  line-height: 1.7;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.filter-item {
  width: 100%;
}

.filter-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.summary-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff1e5;
  color: #7b4f43;
  font-size: 12px;
}

.loading-wrap {
  padding: 20px 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 0 8px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.tree-panel,
.detail-panel {
  padding: 22px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, #fffaf7 100%);
  border: 1px solid rgba(164, 92, 61, 0.12);
  box-shadow: 0 14px 30px rgba(139, 80, 54, 0.08);
}

.panel-meta {
  color: #8a6b63;
  font-size: 12px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 6px 0;
}

.tree-node-main {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.tree-icon {
  color: #b65f38;
  flex-shrink: 0;
}

.tree-label {
  color: #4b2e2b;
  font-size: 14px;
  font-weight: 600;
}

.tree-tags {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.detail-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-title-row h4 {
  margin: 0;
  font-size: 22px;
  color: #4b2e2b;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-block {
  padding: 14px 16px;
  border-radius: 16px;
  background: #fff4eb;
}

.detail-label {
  font-size: 12px;
  color: #9a7060;
}

.detail-value {
  margin-top: 6px;
  color: #5b3832;
  font-size: 14px;
  line-height: 1.7;
}

.multiline {
  white-space: pre-wrap;
}

:deep(.el-tree-node__content) {
  min-height: 42px;
  border-radius: 12px;
}

:deep(.el-tree-node__content:hover) {
  background: #fff1e6;
}

:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background: #ffe8d7;
}

@media (max-width: 1100px) {
  .content-grid,
  .hero-stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-panel,
  .filter-panel,
  .tree-panel,
  .detail-panel {
    border-radius: 18px;
  }

  .module-header,
  .filter-head,
  .panel-head,
  .detail-title-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .tree-node {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
