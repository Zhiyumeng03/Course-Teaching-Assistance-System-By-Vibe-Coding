<template>
  <div class="module-view">
    <section class="hero-panel">
      <div class="module-header">
        <div>
          <h2 class="module-title">实验管理</h2>
          <p class="module-desc">
            <template v-if="isTeacher">集中管理已发布实验，快速定位需要调整的任务。</template>
            <template v-else>按课程、实验和截止时间筛选，快速找到当前需要完成的实验。</template>
          </p>
        </div>
        <el-button
          v-if="isTeacher"
          type="primary"
          :icon="Plus"
          round
          class="publish-button"
          @click="goPublish()"
        >
          发布实验
        </el-button>
      </div>

      <div class="hero-stats">
        <div class="stat-card">
          <div class="stat-label">当前展示</div>
          <div class="stat-value">{{ filteredExperiments.length }}</div>
          <div class="stat-hint">共 {{ experiments.length }} 个实验</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">课程数量</div>
          <div class="stat-value">{{ courseOptions.length }}</div>
          <div class="stat-hint">已加入或负责的课程</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">临近截止</div>
          <div class="stat-value">{{ dueSoonCount }}</div>
          <div class="stat-hint">7 天内到期</div>
        </div>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-head">
        <div>
          <div class="filter-title">搜索与筛选</div>
          <div class="filter-subtitle">支持关键词、课程、实验和截止时间多条件过滤。</div>
        </div>
        <el-button text @click="resetFilters">清空筛选</el-button>
      </div>

      <div class="filter-grid">
        <el-input
          v-model="searchKeyword"
          class="filter-item keyword-input"
          :prefix-icon="Search"
          clearable
          placeholder="搜索实验标题、目标或课程名称"
        />

        <el-select
          v-model="selectedCourseId"
          class="filter-item"
          clearable
          filterable
          placeholder="选择课程"
        >
          <el-option
            v-for="course in courseOptions"
            :key="course.id"
            :label="course.courseName"
            :value="course.id"
          />
        </el-select>

        <el-select
          v-model="selectedExperimentId"
          class="filter-item"
          clearable
          filterable
          placeholder="选择实验"
        >
          <el-option
            v-for="exp in experimentOptions"
            :key="exp.id"
            :label="exp.title"
            :value="exp.id"
          />
        </el-select>

        <el-select
          v-model="deadlineFilter"
          class="filter-item"
          placeholder="截止时间筛选"
        >
          <el-option
            v-for="option in deadlineOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>

      <div class="filter-summary">
        <span class="summary-chip">
          <el-icon><Search /></el-icon>
          关键词：{{ searchKeyword?.trim() || "全部" }}
        </span>
        <span class="summary-chip">
          <el-icon><Reading /></el-icon>
          课程：{{ selectedCourseName || "全部课程" }}
        </span>
        <span class="summary-chip">
          <el-icon><CollectionTag /></el-icon>
          实验：{{ selectedExperimentName || "全部实验" }}
        </span>
        <span class="summary-chip">
          <el-icon><Clock /></el-icon>
          截止：{{ selectedDeadlineLabel }}
        </span>
      </div>
    </section>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <el-empty
      v-else-if="experiments.length === 0"
      description="暂无实验任务"
      :image-size="120"
    />

    <section v-else-if="filteredExperiments.length === 0" class="empty-state">
      <el-empty description="没有符合当前筛选条件的实验" :image-size="110" />
      <el-button type="primary" plain @click="resetFilters">重置筛选</el-button>
    </section>

    <section v-else class="exp-grid">
      <article
        v-for="exp in filteredExperiments"
        :key="exp.id"
        class="exp-card"
        @click="handleCardClick(exp)"
      >
        <div class="exp-card-top">
          <div class="title-block">
            <div class="title-row">
              <span class="exp-status-dot" :class="statusClass(exp.status)" />
              <h3 class="exp-title">{{ exp.title }}</h3>
            </div>
            <p class="exp-objective">
              {{ exp.objective || "暂无实验目标说明" }}
            </p>
          </div>
          <el-tag :type="statusTagType(exp.status)" size="small" effect="light">
            {{ statusLabel(exp.status) }}
          </el-tag>
        </div>

        <div class="exp-meta">
          <span class="meta-chip">
            <el-icon><Reading /></el-icon>
            {{ courseNameMap[exp.courseId] || `课程 #${exp.courseId}` }}
          </span>
          <span class="meta-chip" v-if="exp.deadline">
            <el-icon><Clock /></el-icon>
            截止：{{ formatDateTime(exp.deadline) }}
          </span>
          <span class="meta-chip" v-else>
            <el-icon><Calendar /></el-icon>
            未设置截止时间
          </span>
          <span class="meta-chip" v-if="exp.maxScore">
            <el-icon><StarFilled /></el-icon>
            满分 {{ exp.maxScore }}
          </span>
        </div>

        <div class="exp-footer">
          <span class="deadline-badge" :class="deadlineBadgeClass(exp)">
            {{ deadlineText(exp) }}
          </span>

          <div class="exp-actions" v-if="isTeacher" @click.stop>
            <el-button link type="primary" size="small" @click.stop="goEdit(exp)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(exp)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </div>

          <el-button
            v-else
            type="primary"
            plain
            size="small"
            @click.stop="goSubmitReport(exp)"
          >
            提交报告
          </el-button>
        </div>
      </article>
    </section>

    <el-dialog
      v-model="detailVisible"
      :title="detailExp?.title"
      width="700px"
      align-center
      class="exp-detail-dialog"
    >
      <template v-if="detailExp">
        <div class="detail-meta-row">
          <el-tag :type="statusTagType(detailExp.status)" effect="light">
            {{ statusLabel(detailExp.status) }}
          </el-tag>
          <span class="detail-meta-item">
            <el-icon><Reading /></el-icon>
            {{ courseNameMap[detailExp.courseId] || `课程 #${detailExp.courseId}` }}
          </span>
          <span class="detail-meta-item" v-if="detailExp.deadline">
            <el-icon><Clock /></el-icon>截止：{{ formatDateTime(detailExp.deadline) }}
          </span>
          <span class="detail-meta-item" v-if="detailExp.maxScore">
            <el-icon><StarFilled /></el-icon>满分 {{ detailExp.maxScore }} 分
          </span>
        </div>

        <div class="detail-objective" v-if="detailExp.objective">
          <strong>实验目标：</strong>{{ detailExp.objective }}
        </div>

        <div class="detail-content ql-snow" v-if="detailExp.contentHtml">
          <div class="ql-editor" v-html="detailExp.contentHtml" />
        </div>
        <el-empty v-else description="暂无实验内容" :image-size="80" />
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="isTeacher"
          type="primary"
          @click="
            goEdit(detailExp);
            detailVisible = false;
          "
        >
          <el-icon><Edit /></el-icon>编辑实验
        </el-button>
        <el-button
          v-if="!isTeacher"
          type="primary"
          @click="
            goSubmitReport(detailExp);
            detailVisible = false;
          "
        >
          <el-icon><EditPen /></el-icon>提交报告
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Calendar,
  Clock,
  CollectionTag,
  Delete,
  Edit,
  EditPen,
  Plus,
  Reading,
  Search,
  StarFilled,
} from "@element-plus/icons-vue";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import { deleteExperiment, getMyExperimentList } from "@/api/experiment";
import { getMyCourseList } from "@/api/course";
import { getUserInfo } from "@/utils/auth";

const router = useRouter();

const userInfo = computed(() => getUserInfo());

const isTeacher = computed(() => ["TEACHER", "ADMIN"].includes(userInfo.value.role));

const loading = ref(false);
const experiments = ref([]);
const courses = ref([]);
const courseNameMap = ref({});

const searchKeyword = ref("");
const selectedCourseId = ref(null);
const selectedExperimentId = ref(null);
const deadlineFilter = ref("ALL");

const deadlineOptions = [
  { label: "全部截止时间", value: "ALL" },
  { label: "7 天内截止", value: "DUE_SOON" },
  { label: "已截止", value: "OVERDUE" },
  { label: "未截止", value: "ACTIVE" },
  { label: "未设置截止时间", value: "NO_DEADLINE" },
];

const loadData = async () => {
  loading.value = true;
  try {
    const [expList, courseList] = await Promise.all([getMyExperimentList(), getMyCourseList()]);
    experiments.value = Array.isArray(expList) ? expList : [];
    courses.value = Array.isArray(courseList) ? courseList : [];

    const map = {};
    courses.value.forEach((course) => {
      map[course.id] = course.courseName;
    });
    courseNameMap.value = map;
  } catch {
    // 请求异常由统一拦截器处理
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);

const courseOptions = computed(() =>
  courses.value
    .map((course) => ({
      id: course.id,
      courseName: course.courseName || `课程 #${course.id}`,
    }))
    .sort((a, b) => String(a.courseName).localeCompare(String(b.courseName), "zh-CN")),
);

const experimentOptions = computed(() => {
  const selectedCourse = selectedCourseId.value;
  return experiments.value
    .filter((exp) => !selectedCourse || exp.courseId === selectedCourse)
    .map((exp) => ({ id: exp.id, title: exp.title || `实验 #${exp.id}` }))
    .sort((a, b) => String(a.title).localeCompare(String(b.title), "zh-CN"));
});

watch(selectedCourseId, () => {
  if (!experimentOptions.value.some((exp) => exp.id === selectedExperimentId.value)) {
    selectedExperimentId.value = null;
  }
});

const selectedCourseName = computed(() => {
  const match = courseOptions.value.find((item) => item.id === selectedCourseId.value);
  return match?.courseName || "";
});

const selectedExperimentName = computed(() => {
  const match = experimentOptions.value.find((item) => item.id === selectedExperimentId.value);
  return match?.title || "";
});

const selectedDeadlineLabel = computed(
  () => deadlineOptions.find((item) => item.value === deadlineFilter.value)?.label || "全部截止时间",
);

const now = () => new Date();

const parseDate = (value) => {
  if (!value) return null;
  const date = new Date(Array.isArray(value) ? value.join("-") : value);
  return Number.isNaN(date.getTime()) ? null : date;
};

const isOverdue = (deadline) => {
  const date = parseDate(deadline);
  return date ? date.getTime() < now().getTime() : false;
};

const isDueSoon = (deadline, days = 7) => {
  const date = parseDate(deadline);
  if (!date) return false;
  const diff = date.getTime() - now().getTime();
  return diff >= 0 && diff <= days * 24 * 60 * 60 * 1000;
};

const filteredExperiments = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();

  return experiments.value
    .filter((exp) => {
      if (selectedCourseId.value && exp.courseId !== selectedCourseId.value) return false;
      if (selectedExperimentId.value && exp.id !== selectedExperimentId.value) return false;

      if (keyword) {
        const content = [
          exp.title,
          exp.objective,
          courseNameMap.value[exp.courseId],
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();

        if (!content.includes(keyword)) return false;
      }

      switch (deadlineFilter.value) {
        case "DUE_SOON":
          return isDueSoon(exp.deadline);
        case "OVERDUE":
          return isOverdue(exp.deadline);
        case "ACTIVE":
          return !!exp.deadline && !isOverdue(exp.deadline);
        case "NO_DEADLINE":
          return !exp.deadline;
        default:
          return true;
      }
    })
    .slice()
    .sort((a, b) => {
      const dateA = parseDate(a.deadline);
      const dateB = parseDate(b.deadline);
      if (!dateA && !dateB) return b.id - a.id;
      if (!dateA) return 1;
      if (!dateB) return -1;
      return dateA.getTime() - dateB.getTime();
    });
});

const dueSoonCount = computed(() => experiments.value.filter((exp) => isDueSoon(exp.deadline)).length);

const resetFilters = () => {
  searchKeyword.value = "";
  selectedCourseId.value = null;
  selectedExperimentId.value = null;
  deadlineFilter.value = "ALL";
};

const formatDateTime = (value) => {
  const date = parseDate(value);
  if (!date) return value ? String(value).slice(0, 16) : "-";

  const pad = (num) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`;
};

const deadlineText = (exp) => {
  if (!exp.deadline) return "未设置截止时间";
  if (isOverdue(exp.deadline)) return "已截止";
  if (isDueSoon(exp.deadline, 1)) return "24 小时内截止";
  if (isDueSoon(exp.deadline, 7)) return "7 天内截止";
  return "截止时间正常";
};

const deadlineBadgeClass = (exp) => {
  if (!exp.deadline) return "badge-muted";
  if (isOverdue(exp.deadline)) return "badge-danger";
  if (isDueSoon(exp.deadline, 7)) return "badge-warning";
  return "badge-safe";
};

const handleCardClick = (exp) => {
  if (isTeacher.value) {
    openDetailDialog(exp);
    return;
  }
  router.push({ name: "ExperimentDetail", params: { id: exp.id } });
};

const goSubmitReport = (exp) => {
  if (!exp?.id) return;
  router.push({ name: "SubmitReport", params: { experimentId: exp.id } });
};

const goPublish = (courseId = null) => {
  router.push({
    name: "ExperimentPublish",
    query: courseId ? { courseId } : {},
  });
};

const goEdit = (exp) => {
  if (!exp?.id) return;
  router.push({ name: "ExperimentEdit", params: { id: exp.id } });
};

const STATUS_MAP = {
  DRAFT: { label: "草稿", type: "info", cls: "dot-info" },
  PUBLISHED: { label: "已发布", type: "primary", cls: "dot-primary" },
  ONGOING: { label: "进行中", type: "success", cls: "dot-success" },
  CLOSED: { label: "已截止", type: "warning", cls: "dot-warning" },
  SUBMITTED: { label: "已提交", type: "success", cls: "dot-success" },
};

const statusLabel = (status) => STATUS_MAP[status]?.label || status || "-";
const statusTagType = (status) => STATUS_MAP[status]?.type || "info";
const statusClass = (status) => STATUS_MAP[status]?.cls || "dot-info";

const detailVisible = ref(false);
const detailExp = ref(null);

const openDetailDialog = (exp) => {
  detailExp.value = exp;
  detailVisible.value = true;
};

const handleDelete = async (exp) => {
  try {
    await ElMessageBox.confirm(`确定要删除实验《${exp.title}》吗？`, "提示", {
      type: "warning",
    });
    await deleteExperiment(exp.id);
    ElMessage.success("实验已删除");
    await loadData();
  } catch {
    // 用户取消或请求失败
  }
};

defineExpose({ goPublish });
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
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.22), transparent 32%),
    linear-gradient(135deg, #0f3d3e 0%, #15616d 45%, #1f8a70 100%);
  color: #fff;
  box-shadow: 0 18px 40px rgba(15, 61, 62, 0.16);
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
  letter-spacing: 0.02em;
}

.module-desc {
  margin: 0;
  max-width: 680px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.86);
}

.publish-button {
  border: none;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

.publish-button:hover,
.publish-button:focus {
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
  color: rgba(255, 255, 255, 0.72);
}

.stat-value {
  margin-top: 6px;
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
}

.stat-hint {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.filter-panel {
  padding: 22px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f6fbf8 100%);
  border: 1px solid rgba(19, 118, 92, 0.08);
  box-shadow: 0 10px 28px rgba(16, 76, 74, 0.08);
}

.filter-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.filter-title {
  font-size: 18px;
  font-weight: 700;
  color: #14323a;
}

.filter-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #6a7d84;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1.4fr repeat(3, minmax(0, 1fr));
  gap: 12px;
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
  background: #edf7f2;
  color: #23595b;
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

.exp-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.exp-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, #f8fbfc 100%);
  border: 1px solid rgba(19, 118, 92, 0.08);
  box-shadow: 0 14px 30px rgba(18, 70, 74, 0.08);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.exp-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 36px rgba(18, 70, 74, 0.12);
  border-color: rgba(31, 138, 112, 0.22);
}

.exp-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-block {
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.exp-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #17353d;
}

.exp-objective {
  margin: 10px 0 0;
  color: #6c7f84;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.exp-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #f2f7f8;
  color: #54707a;
  font-size: 12px;
}

.exp-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.deadline-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.badge-safe {
  background: #ebf8f2;
  color: #207e5f;
}

.badge-warning {
  background: #fff4de;
  color: #b7791f;
}

.badge-danger {
  background: #fdecec;
  color: #c24141;
}

.badge-muted {
  background: #eef2f4;
  color: #6a7d84;
}

.exp-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.exp-status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-info {
  background: #909399;
}

.dot-primary {
  background: #409eff;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.14);
}

.dot-success {
  background: #1f8a70;
  box-shadow: 0 0 0 4px rgba(31, 138, 112, 0.16);
}

.dot-warning {
  background: #e6a23c;
  box-shadow: 0 0 0 4px rgba(230, 162, 60, 0.16);
}

:deep(.exp-detail-dialog .el-dialog__body) {
  padding: 20px 24px;
}

.detail-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #5f7077;
}

.detail-objective {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f5f9fa;
  color: #455961;
  line-height: 1.7;
}

.detail-content {
  border: 1px solid #ebeff2;
  border-radius: 12px;
  padding: 4px 16px;
}

@media (max-width: 1100px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .exp-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-panel,
  .filter-panel,
  .exp-card {
    border-radius: 18px;
  }

  .module-header,
  .filter-head,
  .exp-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-stats,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .exp-card-top {
    flex-direction: column;
  }

  .exp-actions {
    justify-content: flex-end;
  }
}
</style>
