<template>
  <div class="detail-page">
    <div class="detail-topbar">
      <el-button :icon="ArrowLeft" text @click="router.back()">返回课程列表</el-button>
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="course">
      <section class="hero-banner" :style="{ background: heroBg }">
        <div class="hero-content">
          <div class="hero-badge">课程代码：{{ course.courseCode || "-" }}</div>
          <h1 class="hero-title">{{ course.courseName }}</h1>
          <p class="hero-desc">课程简介：{{ course.description || "暂无课程简介" }}</p>
          <div class="hero-meta">
            <span class="hero-meta-item">
              <el-icon><Calendar /></el-icon>
              {{ course.term || "-" }}
            </span>
            <span class="hero-meta-item">
              <el-icon><User /></el-icon>
              授课教师：{{ teacherName || "加载中" }}
            </span>
            <el-tag :type="statusTagType(course.status)" effect="dark" round>
              {{ statusLabel(course.status) }}
            </el-tag>
            <el-tag
              v-if="course.enrollMode"
              :type="course.enrollMode === 'SECKILL' ? 'danger' : 'info'"
              effect="light"
              round
            >
              {{ course.enrollMode === "SECKILL" ? "抢课模式" : "审批模式" }}
            </el-tag>
          </div>
        </div>

        <div v-if="isTeacher" class="hero-actions">
          <el-button :icon="Edit" round @click="editDialogVisible = true">编辑课程</el-button>
          <el-button
            v-if="course.enrollMode === 'SECKILL'"
            type="warning"
            round
            :loading="preheatLoading"
            @click="handlePreheat"
          >
            预热抢课缓存
          </el-button>
          <el-button
            :icon="Delete"
            type="danger"
            plain
            round
            :loading="deleteLoading"
            @click="handleDeleteCourse"
          >
            删除课程
          </el-button>
        </div>
      </section>

      <section v-if="course.enrollMode === 'SECKILL'" class="seckill-strip">
        <div class="seckill-item">
          <span>抢课名额</span>
          <strong>{{ seckillStats?.enrollCapacity ?? course.enrollCapacity ?? "-" }}</strong>
        </div>
        <div class="seckill-item">
          <span>已选人数</span>
          <strong>{{ seckillStats?.enrolledCount ?? "-" }}</strong>
        </div>
        <div class="seckill-item">
          <span>剩余名额</span>
          <strong>{{ seckillStats?.remainingStock ?? "-" }}</strong>
        </div>
        <div class="seckill-item">
          <span>活动状态</span>
          <strong>{{ seckillActivityLabel(seckillStats?.activityStatus) }}</strong>
        </div>
      </section>

      <section class="tab-nav">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="tab-item"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <el-icon><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
        </button>
      </section>

      <section class="panel-wrap">
        <Transition name="panel-fade" mode="out-in">
          <div v-if="activeTab === 'members'" key="members" class="panel-card">
            <div class="panel-header">
              <h2 class="panel-title">
                <el-icon><Avatar /></el-icon>
                课程成员
              </h2>
              <el-button
                v-if="isTeacher"
                :icon="Plus"
                size="small"
                round
                type="primary"
                @click="openApproveDialog"
              >
                审批申请
              </el-button>
            </div>

            <el-table
              :data="membersWithDetail"
              stripe
              style="width: 100%"
              empty-text="暂无成员数据"
              v-loading="membersLoading"
            >
              <el-table-column label="学号 / 工号" min-width="120">
                <template #default="{ row }">
                  <span class="mono-text">
                    {{
                      row.roleInCourse === "TEACHER"
                        ? row._user?.teacherNo || row.userNo || "-"
                        : row._user?.studentNo || row.userNo || "-"
                    }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="姓名" min-width="100">
                <template #default="{ row }">
                  <span>{{ row._user?.realName || "-" }}</span>
                </template>
              </el-table-column>
              <el-table-column label="角色" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.roleInCourse === 'TEACHER' ? 'primary' : 'success'" size="small">
                    {{ row.roleInCourse === "TEACHER" ? "教师" : "学生" }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="邮箱" min-width="200">
                <template #default="{ row }">
                  <span>{{ row._user?.email || "-" }}</span>
                </template>
              </el-table-column>
              <el-table-column label="加入日期" width="120">
                <template #default="{ row }">
                  <span>{{ formatDate(row.joinedAt) }}</span>
                </template>
              </el-table-column>
              <el-table-column v-if="isTeacher" label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button link type="danger" size="small" @click="removeMemberById(row.id)">
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-else-if="activeTab === 'experiments'" key="experiments" class="panel-card">
            <div class="panel-header">
              <h2 class="panel-title">
                <el-icon><Files /></el-icon>
                实验任务
              </h2>
              <el-button
                v-if="isTeacher"
                :icon="Plus"
                size="small"
                round
                type="primary"
                @click="goPublishExp"
              >
                发布实验
              </el-button>
            </div>

            <div v-if="expLoading" class="loading-inline">
              <el-skeleton :rows="3" animated />
            </div>

            <el-empty
              v-else-if="courseExperiments.length === 0"
              description="该课程暂时没有实验任务"
              :image-size="90"
            />

            <div v-else class="exp-list">
              <article
                v-for="exp in courseExperiments"
                :key="exp.id"
                class="exp-card"
                @click="openExpDetail(exp)"
              >
                <div class="exp-card-left">
                  <div class="exp-status-dot" :class="expStatusClass(exp.status)" />
                  <div class="exp-info">
                    <div class="exp-title">{{ exp.title }}</div>
                    <div class="exp-meta">
                      <span class="meta-chip" v-if="exp.deadline">
                        <el-icon><Clock /></el-icon>
                        截止：{{ formatDateTime(exp.deadline) }}
                      </span>
                      <span class="meta-chip" v-if="exp.maxScore">
                        <el-icon><StarFilled /></el-icon>
                        满分 {{ exp.maxScore }}
                      </span>
                    </div>
                    <div class="exp-objective" v-if="exp.objective">{{ exp.objective }}</div>
                  </div>
                </div>

                <div class="exp-card-right">
                  <el-tag :type="expStatusTagType(exp.status)" size="small" effect="light">
                    {{ expStatusLabel(exp.status) }}
                  </el-tag>
                  <div v-if="isTeacher" class="exp-actions" @click.stop>
                    <el-button link type="primary" size="small" @click.stop="goEditExp(exp)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-button>
                    <el-button link type="danger" size="small" @click.stop="handleExpDelete(exp)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </div>
                </div>
              </article>
            </div>
          </div>

          <div v-else key="quizzes" class="panel-card">
            <div class="panel-header">
              <h2 class="panel-title">
                <el-icon><Memo /></el-icon>
                测验 / 试卷
              </h2>
              <el-button
                v-if="isTeacher"
                :icon="Plus"
                size="small"
                round
                type="primary"
                @click="goPracticeCenter"
              >
                管理测验
              </el-button>
            </div>

            <div v-if="quizLoading" class="loading-inline">
              <el-skeleton :rows="4" animated />
            </div>

            <el-empty
              v-else-if="courseQuizzes.length === 0"
              description="该课程下暂无测验试卷"
              :image-size="100"
            />

            <div v-else class="quiz-list">
              <article v-for="quiz in courseQuizzes" :key="quiz.id" class="quiz-card">
                <div class="quiz-card-main">
                  <div class="quiz-title-row">
                    <div class="quiz-title">{{ quiz.title }}</div>
                    <el-tag size="small" :type="quizStatusTagType(quiz.status)" effect="light">
                      {{ quizStatusLabel(quiz.status) }}
                    </el-tag>
                  </div>

                  <div class="quiz-meta">
                    <span class="meta-chip">
                      <el-icon><Memo /></el-icon>
                      {{ quiz.questionCount || 0 }} 题
                    </span>
                    <span class="meta-chip">
                      <el-icon><StarFilled /></el-icon>
                      总分 {{ quiz.totalScore || 0 }}
                    </span>
                    <span class="meta-chip">
                      <el-icon><Clock /></el-icon>
                      {{ quiz.durationMinutes || "--" }} 分钟
                    </span>
                    <span class="meta-chip">
                      <el-icon><Calendar /></el-icon>
                      {{ formatDate(quiz.createdAt) }}
                    </span>
                  </div>

                  <div v-if="isTeacher" class="quiz-summary">
                    已提交 {{ getSubmittedCount(quiz.id) }}/{{ studentMembers.length }}
                    <span class="quiz-summary-divider">|</span>
                    未提交 {{ getUnsubmittedStudents(quiz.id).length }}
                  </div>

                  <div v-else-if="latestStudentRecord(quiz.id)" class="quiz-summary">
                    最近一次提交：{{ practiceStatusLabel(latestStudentRecord(quiz.id)?.status) }}
                    <span class="quiz-summary-divider">|</span>
                    得分 {{ latestStudentRecord(quiz.id)?.totalScore ?? 0 }}
                  </div>
                </div>

                <div class="quiz-card-actions">
                  <el-button link type="primary" @click="openQuizPreview(quiz)">预览</el-button>
                  <el-button
                    v-if="!isTeacher"
                    type="primary"
                    :disabled="quiz.status !== 'PUBLISHED'"
                    @click="startQuiz(quiz)"
                  >
                    答题
                  </el-button>
                  <el-button
                    v-if="isTeacher"
                    link
                    type="warning"
                    @click="openUnsubmittedDialog(quiz)"
                  >
                    未提交名单
                  </el-button>
                </div>
              </article>
            </div>
          </div>
        </Transition>
      </section>
    </template>

    <el-empty v-else description="课程不存在或已删除" />

    <el-dialog
      v-model="editDialogVisible"
      title="编辑课程信息"
      width="520px"
      align-center
      :close-on-click-modal="false"
    >
      <el-form
        v-if="editForm"
        ref="editFormRef"
        :model="editForm"
        label-width="90px"
        size="large"
      >
        <el-form-item
          label="课程名称"
          prop="courseName"
          :rules="[{ required: true, message: '不能为空' }]"
        >
          <el-input v-model="editForm.courseName" />
        </el-form-item>
        <el-form-item label="课程编号">
          <el-input v-model="editForm.courseCode" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="editForm.term" />
        </el-form-item>
        <el-form-item label="课程状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="未开始" value="PENDING" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已结束" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="入课模式">
          <el-select v-model="editForm.enrollMode" style="width: 100%">
            <el-option label="审批入课" value="REVIEW" />
            <el-option label="抢课入课" value="SECKILL" />
          </el-select>
        </el-form-item>
        <template v-if="editForm.enrollMode === 'SECKILL'">
          <el-form-item label="抢课名额">
            <el-input-number
              v-model="editForm.enrollCapacity"
              :min="1"
              :max="100000"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker
              v-model="editForm.enrollStartAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择开始时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="editForm.enrollEndAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择结束时间"
              style="width: 100%"
            />
          </el-form-item>
        </template>
        <el-form-item label="课程简介">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditCourse">
          保存修改
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="approveDialogVisible"
      title="审批入课申请"
      width="600px"
      align-center
      :close-on-click-modal="false"
    >
      <div v-if="pendingLoading" class="loading-inline">
        <el-text type="info">加载中...</el-text>
      </div>
      <el-empty
        v-else-if="pendingMembers.length === 0"
        description="暂无待审核的申请"
        :image-size="80"
      />
      <el-table
        v-else
        :data="pendingMembers"
        @selection-change="selectedPending = $event"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="学号" prop="userNo" min-width="120">
          <template #default="{ row }">
            <span class="mono-text">{{ row.userNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="姓名" min-width="100">
          <template #default="{ row }">
            <span>{{ row._name || "查询中" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="120">
          <template #default="{ row }">
            <span>{{ formatDate(row.joinedAt) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="approveDialogVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="approveLoading"
          :disabled="selectedPending.length === 0"
          @click="handleApprove"
        >
          通过选中 ({{ selectedPending.length }})
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="expDetailVisible"
      :title="detailExp?.title"
      width="700px"
      align-center
      class="exp-detail-dialog"
    >
      <template v-if="detailExp">
        <div class="detail-meta-row">
          <el-tag :type="expStatusTagType(detailExp.status)" effect="light">
            {{ expStatusLabel(detailExp.status) }}
          </el-tag>
          <span class="detail-meta-item" v-if="detailExp.deadline">
            <el-icon><Clock /></el-icon>
            截止：{{ formatDateTime(detailExp.deadline) }}
          </span>
          <span class="detail-meta-item" v-if="detailExp.maxScore">
            <el-icon><StarFilled /></el-icon>
            满分 {{ detailExp.maxScore }} 分
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
        <el-button @click="expDetailVisible = false">关闭</el-button>
        <el-button
          v-if="isTeacher"
          type="primary"
          @click="goEditExp(detailExp); expDetailVisible = false"
        >
          <el-icon><Edit /></el-icon>
          编辑此实验
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="quizPreviewVisible"
      :title="quizPreviewData?.paper?.title || '试卷预览'"
      width="920px"
      class="quiz-preview-dialog"
      destroy-on-close
    >
      <template v-if="quizPreviewData">
        <div class="quiz-preview-body">
          <div class="preview-summary">
            <div class="summary-item">
              <span>题量</span>
              <strong>{{ quizPreviewData.questions?.length || 0 }}</strong>
            </div>
            <div class="summary-item">
              <span>总分</span>
              <strong>{{ quizPreviewData.paper?.totalScore || 0 }}</strong>
            </div>
            <div class="summary-item">
              <span>时长</span>
              <strong>{{ quizPreviewData.paper?.durationMinutes || "--" }} 分钟</strong>
            </div>
            <div class="summary-item">
              <span>状态</span>
              <strong>{{ quizStatusLabel(quizPreviewData.paper?.status) }}</strong>
            </div>
          </div>

          <div class="quiz-preview-list">
            <article
              v-for="question in quizPreviewData.questions || []"
              :key="question.id"
              class="quiz-preview-card"
            >
              <div class="quiz-preview-head">
                <div class="question-tags">
                  <el-tag size="small" type="info">第 {{ question.sortNo }} 题</el-tag>
                  <el-tag size="small" :type="quizQuestionTagType(getQuizQuestionType(question))">
                    {{ quizQuestionTypeLabel(getQuizQuestionType(question)) }}
                  </el-tag>
                  <el-tag size="small" effect="plain">分值 {{ question.score }}</el-tag>
                </div>
              </div>

              <div class="quiz-question-stem">{{ getQuizQuestionStem(question) }}</div>

              <div v-if="renderQuizContentLines(question).length" class="detail-block">
                <div class="detail-title">题目内容</div>
                <ul class="line-list">
                  <li v-for="line in renderQuizContentLines(question)" :key="line">{{ line }}</li>
                </ul>
              </div>

              <div v-if="quizPreviewCanSeeReference(question)" class="detail-block">
                <div class="detail-title">参考答案</div>
                <div class="answer-box">{{ renderQuizReferenceAnswer(question) }}</div>
              </div>

              <div v-if="quizPreviewCanSeeAnalysis(question)" class="detail-block">
                <div class="detail-title">答案解析</div>
                <div class="answer-box">{{ getQuizQuestionAnalysis(question) }}</div>
              </div>
            </article>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="unsubmittedDialogVisible"
      :title="`未提交学生名单 - ${selectedQuizForUnsubmitted?.title || ''}`"
      width="720px"
      destroy-on-close
    >
      <el-empty
        v-if="unsubmittedStudents.length === 0"
        description="当前没有未提交学生"
        :image-size="90"
      />
      <el-table v-else :data="unsubmittedStudents" stripe style="width: 100%">
        <el-table-column label="学号" min-width="120">
          <template #default="{ row }">
            <span class="mono-text">{{ row._user?.studentNo || row.userNo || "-" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="姓名" min-width="100">
          <template #default="{ row }">
            <span>{{ row._user?.realName || "-" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180">
          <template #default="{ row }">
            <span>{{ row._user?.email || "-" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="加入日期" width="120">
          <template #default="{ row }">
            <span>{{ formatDate(row.joinedAt) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ArrowLeft,
  Calendar,
  User,
  Edit,
  Delete,
  Plus,
  Avatar,
  Files,
  Memo,
  Clock,
  StarFilled,
} from "@element-plus/icons-vue";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import { deleteExperiment, getExperimentsByCourse } from "@/api/experiment";
import {
  approveMembers,
  deleteCourse,
  getApprovedMembers,
  getCourseById,
  getCourseSeckillStats,
  getPendingMembers,
  preheatCourseSeckill,
  removeMember,
  updateCourseEnrollConfig,
  updateCourse,
} from "@/api/course";
import { getPaperDetail, getPaperList } from "@/api/paper";
import { getStudentPracticeList, getTeacherPracticeList } from "@/api/practice";
import { getUserByStudentNo, getUserByTeacherNo } from "@/api/user";
import { getUserInfo } from "@/utils/auth";

const router = useRouter();
const route = useRoute();
const courseId = computed(() => Number(route.params.id));

const userInfo = computed(() => getUserInfo());

const isTeacher = computed(() => ["TEACHER", "ADMIN"].includes(userInfo.value.role));

const coverColors = [
  "linear-gradient(135deg,#6c63ff,#3b82f6)",
  "linear-gradient(135deg,#f093fb,#f5576c)",
  "linear-gradient(135deg,#4facfe,#00f2fe)",
  "linear-gradient(135deg,#43e97b,#38f9d7)",
  "linear-gradient(135deg,#fa709a,#fee140)",
  "linear-gradient(135deg,#a18cd1,#fbc2eb)",
];

const heroBg = computed(() => coverColors[courseId.value % coverColors.length]);

const loading = ref(false);
const course = ref(null);
const teacherName = ref("");

const members = ref([]);
const membersWithDetail = ref([]);
const membersLoading = ref(false);

const tabs = [
  { key: "members", label: "成员", icon: Avatar },
  { key: "experiments", label: "实验", icon: Files },
  { key: "quizzes", label: "测验", icon: Memo },
];
const activeTab = ref("members");

const editDialogVisible = ref(false);
const editLoading = ref(false);
const deleteLoading = ref(false);
const editFormRef = ref(null);
const editForm = ref(null);
const preheatLoading = ref(false);
const seckillStats = ref(null);

const expLoading = ref(false);
const courseExperiments = ref([]);
const expDetailVisible = ref(false);
const detailExp = ref(null);

const approveDialogVisible = ref(false);
const pendingLoading = ref(false);
const approveLoading = ref(false);
const pendingMembers = ref([]);
const selectedPending = ref([]);

const quizLoading = ref(false);
const courseQuizzes = ref([]);
const quizRecords = ref([]);
const quizPreviewVisible = ref(false);
const quizPreviewData = ref(null);
const quizPreviewShowReference = ref(false);
const unsubmittedDialogVisible = ref(false);
const selectedQuizForUnsubmitted = ref(null);

const studentMembers = computed(() =>
  membersWithDetail.value.filter((item) => item.roleInCourse === "STUDENT")
);

const submittedStudentNoMap = computed(() => {
  const result = new Map();
  if (!isTeacher.value) return result;
  quizRecords.value.forEach((record) => {
    const paperId = Number(record.paperId);
    const studentNo = record.studentNo;
    if (!paperId || !studentNo) return;
    if (!result.has(paperId)) {
      result.set(paperId, new Set());
    }
    result.get(paperId).add(studentNo);
  });
  return result;
});

const unsubmittedStudents = computed(() => {
  if (!selectedQuizForUnsubmitted.value) return [];
  return getUnsubmittedStudents(selectedQuizForUnsubmitted.value.id);
});

async function loadCourse() {
  loading.value = true;
  try {
    course.value = await getCourseById(courseId.value);
    if (!course.value) return;
    editForm.value = toEditableCourse(course.value);
    seckillStats.value = null;
    await Promise.all([loadTeacherName(course.value.teacherNo), loadMembers()]);
    if (course.value.enrollMode === "SECKILL" && isTeacher.value) {
      await loadSeckillStats();
    }
    if (activeTab.value === "experiments") {
      await loadCourseExperiments();
    }
    if (activeTab.value === "quizzes") {
      await loadQuizPanelData();
    }
  } finally {
    loading.value = false;
  }
}

async function loadTeacherName(teacherNo) {
  if (!teacherNo) {
    teacherName.value = "未知教师";
    return;
  }
  try {
    const user = await getUserByTeacherNo(teacherNo);
    teacherName.value = user?.realName || user?.username || teacherNo;
  } catch {
    teacherName.value = teacherNo;
  }
}

async function loadMembers() {
  membersLoading.value = true;
  try {
    const approved = await getApprovedMembers(courseId.value);
    members.value = approved || [];
    const userRequests = (approved || []).map((member) =>
      member.roleInCourse === "TEACHER"
        ? getUserByTeacherNo(member.userNo).catch(() => null)
        : getUserByStudentNo(member.userNo).catch(() => null)
    );
    const userResults = await Promise.all(userRequests);
    membersWithDetail.value = (approved || []).map((member, index) => ({
      ...member,
      _user: userResults[index],
    }));
  } finally {
    membersLoading.value = false;
  }
}

async function removeMemberById(id) {
  try {
    await ElMessageBox.confirm("确定要移除该成员吗？", "提示", { type: "warning" });
    await removeMember(id);
    ElMessage.success("已移除成员");
    await loadMembers();
  } catch {}
}

function statusLabel(status) {
  return { ACTIVE: "进行中", PENDING: "未开始", CLOSED: "已结束" }[status] || status || "-";
}

function statusTagType(status) {
  return { ACTIVE: "success", PENDING: "warning", CLOSED: "info" }[status] || "";
}

async function loadSeckillStats() {
  try {
    seckillStats.value = await getCourseSeckillStats(courseId.value);
  } catch {
    seckillStats.value = null;
  }
}

function seckillActivityLabel(status) {
  return {
    NOT_STARTED: "未开始",
    ONGOING: "进行中",
    ENDED: "已结束",
  }[status] || "-";
}

async function handlePreheat() {
  preheatLoading.value = true;
  try {
    seckillStats.value = await preheatCourseSeckill(courseId.value);
    ElMessage.success("抢课缓存预热完成");
  } finally {
    preheatLoading.value = false;
  }
}

async function handleEditCourse() {
  const valid = await editFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (editForm.value?.enrollMode === "SECKILL") {
    if (!editForm.value.enrollCapacity || editForm.value.enrollCapacity <= 0) {
      ElMessage.error("抢课名额必须大于 0");
      return;
    }
    const startTs = new Date(editForm.value.enrollStartAt).getTime();
    const endTs = new Date(editForm.value.enrollEndAt).getTime();
    if (!startTs || !endTs || startTs >= endTs) {
      ElMessage.error("抢课开始时间必须早于结束时间");
      return;
    }
  }
  editLoading.value = true;
  try {
    const payload = toCourseUpdatePayload(editForm.value);
    await updateCourse(courseId.value, payload);
    await updateCourseEnrollConfig(courseId.value, {
      enrollMode: payload.enrollMode,
      enrollCapacity: payload.enrollMode === "SECKILL" ? payload.enrollCapacity : null,
      enrollStartAt: payload.enrollMode === "SECKILL" ? payload.enrollStartAt : null,
      enrollEndAt: payload.enrollMode === "SECKILL" ? payload.enrollEndAt : null,
    });
    if (payload.enrollMode === "SECKILL") {
      await loadSeckillStats();
    }
    ElMessage.success("课程信息更新成功");
    editDialogVisible.value = false;
    await loadCourse();
  } finally {
    editLoading.value = false;
  }
}

async function handleDeleteCourse() {
  if (!course.value?.id) return;
  try {
    await ElMessageBox.confirm(
      `确定要删除课程《${course.value.courseName}》吗？删除后将无法恢复。`,
      "提示",
      { type: "warning" }
    );
    deleteLoading.value = true;
    await deleteCourse(course.value.id);
    ElMessage.success("课程已删除");
    window.dispatchEvent(new CustomEvent("course-list-updated"));
    await router.replace({
      name: "Home",
      query: { menu: "course-list" },
    });
  } catch {
  } finally {
    deleteLoading.value = false;
  }
}

async function loadCourseExperiments() {
  expLoading.value = true;
  try {
    const list = await getExperimentsByCourse(courseId.value);
    courseExperiments.value = list || [];
  } finally {
    expLoading.value = false;
  }
}

function expStatusLabel(status) {
  return {
    DRAFT: "草稿",
    PUBLISHED: "已发布",
    ONGOING: "进行中",
    CLOSED: "已截止",
  }[status] || status || "-";
}

function expStatusTagType(status) {
  return {
    DRAFT: "info",
    PUBLISHED: "primary",
    ONGOING: "success",
    CLOSED: "warning",
  }[status] || "";
}

function expStatusClass(status) {
  return {
    DRAFT: "dot-info",
    PUBLISHED: "dot-primary",
    ONGOING: "dot-success",
    CLOSED: "dot-warning",
  }[status] || "dot-info";
}

function openExpDetail(exp) {
  detailExp.value = exp;
  expDetailVisible.value = true;
}

function goPublishExp() {
  router.push({
    name: "ExperimentPublish",
    query: { courseId: courseId.value },
  });
}

function goEditExp(exp) {
  if (!exp?.id) return;
  router.push({
    name: "ExperimentEdit",
    params: { id: exp.id },
    query: { courseId: courseId.value },
  });
}

async function handleExpDelete(exp) {
  try {
    await ElMessageBox.confirm(`确定要删除实验《${exp.title}》吗？`, "提示", {
      type: "warning",
    });
    await deleteExperiment(exp.id);
    ElMessage.success("已删除");
    await loadCourseExperiments();
  } catch {}
}

async function openApproveDialog() {
  approveDialogVisible.value = true;
  pendingLoading.value = true;
  try {
    const list = await getPendingMembers(courseId.value);
    pendingMembers.value = await Promise.all(
      (list || []).map(async (member) => {
        try {
          const user = await getUserByStudentNo(member.userNo);
          return { ...member, _name: user?.realName || member.userNo };
        } catch {
          return { ...member, _name: member.userNo };
        }
      })
    );
  } finally {
    pendingLoading.value = false;
  }
}

async function handleApprove() {
  if (!selectedPending.value.length) return;
  approveLoading.value = true;
  try {
    const ids = selectedPending.value.map((item) => item.id);
    await approveMembers(ids);
    ElMessage.success(`已通过 ${ids.length} 位学生的入课申请`);
    approveDialogVisible.value = false;
    await loadMembers();
  } finally {
    approveLoading.value = false;
  }
}

async function loadQuizPanelData() {
  quizLoading.value = true;
  try {
    const [papers, records] = await Promise.all([
      getPaperList(courseId.value),
      isTeacher.value ? getTeacherPracticeList(courseId.value) : getStudentPracticeList(courseId.value),
    ]);
    courseQuizzes.value = Array.isArray(papers) ? papers : [];
    quizRecords.value = Array.isArray(records) ? records : [];
  } finally {
    quizLoading.value = false;
  }
}

function quizStatusLabel(status) {
  return { DRAFT: "草稿", PUBLISHED: "已发布", CLOSED: "已关闭" }[status] || status || "-";
}

function quizStatusTagType(status) {
  return { DRAFT: "info", PUBLISHED: "success", CLOSED: "warning" }[status] || "info";
}

function practiceStatusLabel(status) {
  return { PENDING_REVIEW: "待批改", GRADED: "已批改" }[status] || status || "-";
}

function getSubmittedCount(paperId) {
  return submittedStudentNoMap.value.get(Number(paperId))?.size || 0;
}

function getUnsubmittedStudents(paperId) {
  const submitted = submittedStudentNoMap.value.get(Number(paperId)) || new Set();
  return studentMembers.value.filter((member) => !submitted.has(member.userNo));
}

function latestStudentRecord(paperId) {
  return quizRecords.value.find((record) => Number(record.paperId) === Number(paperId)) || null;
}

async function openQuizPreview(quiz) {
  quizPreviewData.value = await getPaperDetail(quiz.id);
  quizPreviewShowReference.value = isTeacher.value;
  quizPreviewVisible.value = true;
}

function startQuiz(quiz) {
  router.push({
    name: "Home",
    query: {
      menu: "practice",
      courseId: String(courseId.value),
      paperId: String(quiz.id),
      action: "answer",
    },
  });
}

function goPracticeCenter() {
  router.push({
    name: "Home",
    query: {
      menu: "practice",
      courseId: String(courseId.value),
    },
  });
}

function openUnsubmittedDialog(quiz) {
  selectedQuizForUnsubmitted.value = quiz;
  unsubmittedDialogVisible.value = true;
}

function quizQuestionTypeLabel(type) {
  return {
    SINGLE: "单选题",
    MULTI: "多选题",
    JUDGE: "判断题",
    BLANK: "填空题",
    SHORT: "简答题",
  }[type] || type || "未知题型";
}

function quizQuestionTagType(type) {
  return {
    SINGLE: "primary",
    MULTI: "success",
    JUDGE: "warning",
    BLANK: "info",
    SHORT: "danger",
  }[type] || "info";
}

function getQuizQuestionType(question) {
  return question?.snapshot?.type || question?.type || "";
}

function getQuizQuestionStem(question) {
  return question?.snapshot?.stem || question?.stem || "";
}

function getQuizQuestionContent(question) {
  return question?.snapshot?.content || question?.content || {};
}

function getQuizQuestionAnswer(question) {
  return question?.snapshot?.answer || question?.answer || {};
}

function getQuizQuestionAnalysis(question) {
  return question?.snapshot?.analysisText || question?.analysisText || "";
}

function renderQuizContentLines(question) {
  const type = getQuizQuestionType(question);
  const content = getQuizQuestionContent(question);
  if (["SINGLE", "MULTI", "JUDGE"].includes(type)) {
    return Object.entries(content).map(([key, value]) => `${key}. ${value}`);
  }
  if (type === "BLANK") {
    return Object.entries(content).map(([key, value]) => `空 ${key}: ${value}`);
  }
  if (type === "SHORT") {
    return content.guide ? [`作答说明: ${content.guide}`] : [];
  }
  return [];
}

function renderQuizReferenceAnswer(question) {
  const type = getQuizQuestionType(question);
  const answer = getQuizQuestionAnswer(question);
  if (type === "SHORT") {
    return answer.text || "未设置";
  }
  return Object.entries(answer)
    .map(([key, value]) => `${key}: ${value}`)
    .join("\n") || "未设置";
}

function quizPreviewCanSeeReference(question) {
  return quizPreviewShowReference.value && Object.keys(getQuizQuestionAnswer(question) || {}).length > 0;
}

function quizPreviewCanSeeAnalysis(question) {
  return quizPreviewShowReference.value && !!getQuizQuestionAnalysis(question);
}

function toEditableCourse(source) {
  return {
    ...source,
    enrollMode: source?.enrollMode || "REVIEW",
    enrollCapacity: source?.enrollCapacity ?? 50,
    enrollStartAt: toLocalDateTimeValue(source?.enrollStartAt),
    enrollEndAt: toLocalDateTimeValue(source?.enrollEndAt),
  };
}

function toCourseUpdatePayload(source) {
  const payload = {
    ...source,
    enrollMode: source?.enrollMode || "REVIEW",
    enrollCapacity: source?.enrollCapacity ?? null,
    enrollStartAt: toLocalDateTimeValue(source?.enrollStartAt),
    enrollEndAt: toLocalDateTimeValue(source?.enrollEndAt),
  };
  if (payload.enrollMode !== "SECKILL") {
    payload.enrollCapacity = null;
    payload.enrollStartAt = null;
    payload.enrollEndAt = null;
  }
  return payload;
}

function toLocalDateTimeValue(value) {
  if (!value) return "";
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}T${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}:${String(second).padStart(2, "0")}`;
  }
  if (typeof value === "string") {
    return value.includes("T") ? value.slice(0, 19) : value.replace(" ", "T").slice(0, 19);
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toISOString().slice(0, 19);
}

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(Array.isArray(value) ? value.join("-") : value);
  if (Number.isNaN(date.getTime())) {
    return String(value).slice(0, 10);
  }
  return date.toISOString().slice(0, 10);
}

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(Array.isArray(value) ? value.join("-") : value);
  if (Number.isNaN(date.getTime())) {
    return String(value).slice(0, 16);
  }
  return date.toISOString().replace("T", " ").slice(0, 16);
}

watch(activeTab, async (tab) => {
  if (tab === "experiments" && !courseExperiments.value.length) {
    await loadCourseExperiments();
  }
  if (tab === "quizzes" && !courseQuizzes.value.length) {
    await loadQuizPanelData();
  }
});

watch(courseId, async () => {
  await loadCourse();
});

onMounted(loadCourse);
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f0f2f8;
  padding-bottom: 60px;
}

.detail-topbar {
  padding: 16px 32px 0;
}

.loading-wrap {
  padding: 40px 32px;
}

.loading-inline {
  padding: 16px 0;
}

.hero-banner {
  margin: 14px 32px 0;
  border-radius: 22px;
  padding: 36px 40px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  position: relative;
  overflow: hidden;
}

.hero-banner::before {
  content: "";
  position: absolute;
  width: 280px;
  height: 280px;
  background: rgba(255, 255, 255, 0.07);
  border-radius: 50%;
  right: -70px;
  top: -70px;
  pointer-events: none;
}

.hero-content {
  flex: 1;
}

.hero-badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
  font-size: 11px;
  font-family: monospace;
  letter-spacing: 2px;
  padding: 3px 12px;
  border-radius: 20px;
  margin-bottom: 10px;
}

.hero-title {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 8px;
}

.hero-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 18px;
  max-width: 560px;
  line-height: 1.6;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
}

.hero-meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex-shrink: 0;
  padding-top: 4px;
}

.seckill-strip {
  margin: 14px 32px 0;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 14px 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.seckill-item {
  background: #f6f8fe;
  border-radius: 12px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.seckill-item span {
  font-size: 12px;
  color: #7f8ba1;
}

.seckill-item strong {
  font-size: 16px;
  color: #1a1a2e;
}

.tab-nav {
  display: flex;
  gap: 4px;
  margin: 16px 32px 0;
  background: #fff;
  border-radius: 16px;
  padding: 6px 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 22px;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 500;
  color: #8c8c8c;
  cursor: pointer;
  border: none;
  background: transparent;
  transition: all 0.22s;
}

.tab-item:hover {
  color: #6c63ff;
  background: rgba(108, 99, 255, 0.06);
}

.tab-item.active {
  color: #fff;
  background: linear-gradient(135deg, #6c63ff, #3b82f6);
  box-shadow: 0 4px 12px rgba(108, 99, 255, 0.35);
}

.panel-wrap {
  margin: 14px 32px 0;
}

.panel-card {
  background: #fff;
  border-radius: 18px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  min-height: 320px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.panel-title .el-icon {
  color: #6c63ff;
}

.mono-text {
  font-family: monospace;
  font-size: 13px;
  color: #555;
  letter-spacing: 0.5px;
}

.exp-list,
.quiz-list,
.quiz-preview-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exp-card,
.quiz-card,
.quiz-preview-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  background: #f9f9fc;
  border-radius: 14px;
  padding: 16px 18px;
  transition: all 0.2s;
}

.exp-card {
  cursor: pointer;
  border-left: 4px solid transparent;
}

.exp-card:hover {
  background: #f3f2ff;
  border-left-color: #6c63ff;
  box-shadow: 0 4px 14px rgba(108, 99, 255, 0.1);
}

.quiz-card,
.quiz-preview-card {
  border: 1px solid #edf0f5;
}

.exp-card-left {
  display: flex;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.exp-status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.dot-info {
  background: #909399;
}

.dot-primary {
  background: #6c63ff;
  box-shadow: 0 0 0 3px rgba(108, 99, 255, 0.18);
}

.dot-success {
  background: #67c23a;
  box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.18);
}

.dot-warning {
  background: #e6a23c;
  box-shadow: 0 0 0 3px rgba(230, 162, 60, 0.18);
}

.exp-info,
.quiz-card-main {
  flex: 1;
  min-width: 0;
}

.exp-title,
.quiz-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
}

.exp-meta,
.quiz-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #666;
  background: rgba(0, 0, 0, 0.04);
  padding: 3px 8px;
  border-radius: 20px;
}

.exp-objective,
.quiz-summary {
  margin-top: 10px;
  font-size: 13px;
  color: #7a8698;
}

.quiz-summary-divider {
  margin: 0 8px;
  color: #c0c6d0;
}

.exp-card-right,
.quiz-card-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.exp-actions {
  display: flex;
  gap: 2px;
}

.quiz-title-row,
.quiz-preview-head,
.question-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  background: #f4f8fc;
  border-radius: 14px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-item span {
  font-size: 12px;
  color: #6b7788;
}

.summary-item strong {
  color: #17324d;
  font-size: 18px;
}

.quiz-preview-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(100vh - 220px);
}

.quiz-preview-list {
  overflow-y: auto;
  min-height: 0;
}

.quiz-preview-card {
  flex-direction: column;
  background: #fbfdff;
}

.quiz-question-stem {
  color: #17324d;
  font-size: 16px;
  line-height: 1.7;
}

.detail-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}

.detail-meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #666;
}

.detail-objective {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin-bottom: 14px;
  padding: 10px 14px;
  background: #f8f8fc;
  border-radius: 8px;
}

.detail-content {
  border: 1px solid #ebebf0;
  border-radius: 8px;
  padding: 4px 16px;
  min-height: 60px;
}

.detail-block {
  margin-top: 14px;
}

.detail-title {
  font-size: 13px;
  font-weight: 700;
  color: #445162;
  margin-bottom: 8px;
}

.line-list {
  margin: 0;
  padding-left: 18px;
  color: #5c6878;
  line-height: 1.8;
}

.answer-box {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #415061;
  background: #f4f8fc;
  border-radius: 14px;
  padding: 14px;
}

.panel-fade-enter-active,
.panel-fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.panel-fade-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.panel-fade-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

@media (max-width: 900px) {
  .hero-banner {
    flex-direction: column;
  }

  .seckill-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .preview-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quiz-card,
  .exp-card {
    flex-direction: column;
  }

  .quiz-card-actions,
  .exp-card-right {
    width: 100%;
    flex-direction: row;
    justify-content: flex-end;
  }
}

@media (max-width: 768px) {
  .detail-topbar {
    padding: 10px 12px 0;
  }

  .loading-wrap {
    padding: 20px 12px;
  }

  .hero-banner {
    margin: 10px 12px 0;
    padding: 24px 20px;
  }

  .seckill-strip {
    margin: 10px 12px 0;
    grid-template-columns: 1fr;
  }

  .tab-nav {
    margin: 12px 12px 0;
    flex-wrap: wrap;
    padding: 6px;
  }

  .panel-wrap {
    margin: 12px 12px 0;
  }

  .panel-card {
    padding: 20px 16px;
  }

  .preview-summary {
    grid-template-columns: 1fr;
  }
}
</style>
