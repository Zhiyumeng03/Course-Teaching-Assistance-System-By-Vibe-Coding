<template>
  <div class="module-view">
    <div class="module-header">
      <div>
        <h2 class="module-title">课程列表</h2>
        <p class="module-desc">
          <template v-if="isTeacher">管理你创建的课程</template>
          <template v-else>查看已加入课程，或通过加入码申请入课</template>
        </p>
      </div>
      <div class="header-actions">
        <el-button
          v-if="isStudent"
          type="danger"
          :icon="Lightning"
          round
          @click="openSeckillDialog"
        >
          抢课
        </el-button>
        <el-button
          v-if="isStudent"
          type="primary"
          :icon="Plus"
          round
          @click="joinDialogVisible = true"
        >
          加入课程
        </el-button>
        <el-button
          v-if="isTeacher"
          type="primary"
          :icon="Plus"
          round
          @click="createDialogVisible = true"
        >
          创建课程
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="3" animated />
    </div>

    <el-empty
      v-else-if="!loading && totalCourses === 0"
      description="暂无课程，快去加入或创建吧"
      :image-size="120"
    />

    <template v-else>
      <div class="course-grid">
        <div
          v-for="(course, index) in courses"
          :key="course.id"
          class="course-card"
          @click="goDetail(course.id)"
        >
          <div
            class="course-cover"
            :style="{ background: coverColors[index % coverColors.length] }"
          >
            <el-icon size="38" color="rgba(255,255,255,0.9)"><Reading /></el-icon>
            <div class="cover-overlay">
              <span class="cover-code">{{ course.courseCode }}</span>
            </div>
          </div>

          <div class="course-info">
            <div class="course-name-row">
              <h3 class="course-name">{{ course.courseName }}</h3>
              <el-tag size="small" :type="statusTagType(course.status)" effect="light">
                {{ statusLabel(course.status) }}
              </el-tag>
            </div>
            <p class="course-desc">{{ course.description || "暂无课程简介" }}</p>
            <div class="course-meta">
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>{{ course.term || "-" }}
              </span>
              <span v-if="course.enrollMode" class="meta-item">
                <el-tag size="small" effect="plain" :type="course.enrollMode === 'SECKILL' ? 'danger' : 'info'">
                  {{ course.enrollMode === "SECKILL" ? "抢课模式" : "审批模式" }}
                </el-tag>
              </span>
              <span
                v-if="isTeacher"
                class="meta-item join-code"
                @click.stop="copyJoinCode(course.joinCode)"
              >
                <el-icon><Key /></el-icon>加入码：{{ course.joinCode }}
                <el-icon class="copy-icon"><CopyDocument /></el-icon>
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[8, 12, 16, 20]"
          :total="totalCourses"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </template>

    <el-dialog
      v-model="joinDialogVisible"
      title="加入课程（审批入课）"
      width="420px"
      align-center
      :close-on-click-modal="false"
    >
      <div class="dialog-body">
        <p class="dialog-hint">此入口仅用于审批入课课程，抢课请使用顶部“抢课”按钮</p>
        <el-form ref="joinFormRef" :model="joinForm" :rules="joinRules" size="large">
          <el-form-item prop="joinCode">
            <el-input
              v-model="joinForm.joinCode"
              placeholder="请输入课程加入码"
              :prefix-icon="Key"
              clearable
              maxlength="20"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="joinLoading" @click="handleJoinCourse">
          提交申请
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="seckillDialogVisible"
      title="抢课中心"
      width="920px"
      align-center
      :close-on-click-modal="false"
    >
      <div v-if="seckillLoading" class="loading-wrap">
        <el-skeleton :rows="4" animated />
      </div>
      <el-empty
        v-else-if="!seckillLoading && seckillTotal === 0"
        description="暂无可抢课程"
        :image-size="90"
      />
      <template v-else>
        <el-table :data="seckillCourses" stripe style="width: 100%">
          <el-table-column label="课程" min-width="220">
            <template #default="{ row }">
              <div class="seckill-course-name">{{ row.courseName }}</div>
              <div class="seckill-course-sub">{{ row.courseCode }} | {{ row.term || "-" }}</div>
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="220">
            <template #default="{ row }">
              <div>{{ formatDisplayDateTime(row.enrollStartAt) }}</div>
              <div>{{ formatDisplayDateTime(row.enrollEndAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="名额" width="90" align="center">
            <template #default="{ row }">
              {{ row.enrollCapacity ?? "-" }}
            </template>
          </el-table-column>
          <el-table-column label="余量" width="90" align="center">
            <template #default="{ row }">
              {{ row.remainingStock ?? 0 }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="seckillStatusTagType(row.activityStatus)">
                {{ seckillStatusLabel(row.activityStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button
                :type="seckillActionType(row)"
                size="small"
                :disabled="isSeckillActionDisabled(row)"
                :loading="enrollingCourseId === row.courseId"
                @click="handleSeckillEnroll(row)"
              >
                {{ seckillActionLabel(row) }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="seckillCurrentPage"
            v-model:page-size="seckillPageSize"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :page-sizes="[6, 8, 10, 12]"
            :total="seckillTotal"
            @current-change="loadSeckillCourses"
            @size-change="handleSeckillPageSizeChange"
          />
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="createDialogVisible"
      title="创建新课程"
      width="560px"
      align-center
      :close-on-click-modal="false"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="95px"
        size="large"
      >
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="createForm.courseName" placeholder="请输入课程名称" clearable />
        </el-form-item>
        <el-form-item label="课程编号" prop="courseCode">
          <el-input v-model="createForm.courseCode" placeholder="如：CS101" clearable />
        </el-form-item>
        <el-form-item label="学期" prop="term">
          <el-input v-model="createForm.term" placeholder="如：2026春季" clearable />
        </el-form-item>
        <el-form-item label="加入码" prop="joinCode">
          <el-input v-model="createForm.joinCode" placeholder="学生通过加入码入课" clearable>
            <template #append>
              <el-button @click="genJoinCode">随机生成</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="课程状态" prop="status">
          <el-select v-model="createForm.status" style="width: 100%">
            <el-option label="未开始" value="PENDING" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已结束" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="入课模式" prop="enrollMode">
          <el-select v-model="createForm.enrollMode" style="width: 100%">
            <el-option label="审批入课" value="REVIEW" />
            <el-option label="抢课入课" value="SECKILL" />
          </el-select>
        </el-form-item>
        <template v-if="createForm.enrollMode === 'SECKILL'">
          <el-form-item label="抢课名额" prop="enrollCapacity">
            <el-input-number v-model="createForm.enrollCapacity" :min="1" :max="100000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="开始时间" prop="enrollStartAt">
            <el-date-picker
              v-model="createForm.enrollStartAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择开始时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="enrollEndAt">
            <el-date-picker
              v-model="createForm.enrollEndAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择结束时间"
              style="width: 100%"
            />
          </el-form-item>
        </template>
        <el-form-item label="课程简介">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateCourse">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Calendar, CopyDocument, Key, Lightning, Plus, Reading } from "@element-plus/icons-vue";
import {
  createCourse,
  getCourseByJoinCode,
  getMyCoursePage,
  getSeckillCoursePage,
  joinCourseByCode,
  seckillEnroll,
  seckillWithdraw,
} from "@/api/course";
import { getUserInfo } from "@/utils/auth";

const router = useRouter();
const userInfo = computed(() => getUserInfo());
const isTeacher = computed(() => ["TEACHER", "ADMIN"].includes(userInfo.value.role));
const isStudent = computed(() => userInfo.value.role === "STUDENT");

const coverColors = [
  "linear-gradient(135deg,#6c63ff,#3b82f6)",
  "linear-gradient(135deg,#f093fb,#f5576c)",
  "linear-gradient(135deg,#4facfe,#00f2fe)",
  "linear-gradient(135deg,#43e97b,#38f9d7)",
  "linear-gradient(135deg,#fa709a,#fee140)",
  "linear-gradient(135deg,#a18cd1,#fbc2eb)",
  "linear-gradient(135deg,#fda085,#f6d365)",
  "linear-gradient(135deg,#89f7fe,#66a6ff)",
];

const loading = ref(false);
const courses = ref([]);
const currentPage = ref(1);
const pageSize = ref(8);
const totalCourses = ref(0);

async function loadCourses() {
  loading.value = true;
  try {
    const data = await getMyCoursePage({
      current: currentPage.value,
      size: pageSize.value,
    });
    courses.value = Array.isArray(data?.records) ? data.records : [];
    totalCourses.value = Number(data?.total || 0);
    currentPage.value = Number(data?.current || currentPage.value);
  } finally {
    loading.value = false;
  }
}

onMounted(loadCourses);

const handleCurrentChange = async (page) => {
  currentPage.value = page;
  await loadCourses();
};

const handleSizeChange = async (size) => {
  pageSize.value = size;
  currentPage.value = 1;
  await loadCourses();
};

const statusLabel = (status) => {
  const map = { ACTIVE: "进行中", PENDING: "未开始", CLOSED: "已结束" };
  return map[status] || status || "未知";
};

const statusTagType = (status) => {
  const map = { ACTIVE: "success", PENDING: "warning", CLOSED: "info" };
  return map[status] || "";
};

const goDetail = (id) => {
  router.push(`/course/${id}`);
};

const copyJoinCode = (code) => {
  if (!code) return;
  navigator.clipboard.writeText(code).then(() => ElMessage.success(`加入码 ${code} 已复制`));
};

const joinDialogVisible = ref(false);
const joinLoading = ref(false);
const joinFormRef = ref(null);
const joinForm = ref({ joinCode: "" });

const joinRules = {
  joinCode: [{ required: true, message: "请输入课程加入码", trigger: "blur" }],
};

async function handleJoinCourse() {
  const valid = await joinFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  joinLoading.value = true;
  try {
    if (!userInfo.value.studentNo) {
      ElMessage.error("请先完善学号信息");
      return;
    }
    const code = joinForm.value.joinCode.trim();
    const course = await getCourseByJoinCode(code);
    if (!course) {
      ElMessage.error("加入码无效");
      return;
    }
    if (course.enrollMode === "SECKILL") {
      ElMessage.warning("该课程为抢课模式，请点击顶部“抢课”按钮");
      return;
    }
    await joinCourseByCode(code);
    ElMessage.success("申请已提交，等待教师审批");
    joinDialogVisible.value = false;
    joinForm.value.joinCode = "";
  } finally {
    joinLoading.value = false;
  }
}

const seckillDialogVisible = ref(false);
const seckillLoading = ref(false);
const seckillCourses = ref([]);
const seckillCurrentPage = ref(1);
const seckillPageSize = ref(8);
const seckillTotal = ref(0);
const enrollingCourseId = ref(null);

async function openSeckillDialog() {
  seckillDialogVisible.value = true;
  seckillCurrentPage.value = 1;
  await loadSeckillCourses();
}

async function loadSeckillCourses() {
  seckillLoading.value = true;
  try {
    const page = await getSeckillCoursePage({
      current: seckillCurrentPage.value,
      size: seckillPageSize.value,
    });
    seckillCourses.value = Array.isArray(page?.records) ? page.records : [];
    seckillTotal.value = Number(page?.total || 0);
    seckillCurrentPage.value = Number(page?.current || seckillCurrentPage.value);
  } finally {
    seckillLoading.value = false;
  }
}

async function handleSeckillPageSizeChange(size) {
  seckillPageSize.value = size;
  seckillCurrentPage.value = 1;
  await loadSeckillCourses();
}

function seckillStatusLabel(status) {
  return {
    NOT_STARTED: "未开始",
    ONGOING: "进行中",
    ENDED: "已结束",
  }[status] || "未知";
}

function seckillStatusTagType(status) {
  return {
    NOT_STARTED: "info",
    ONGOING: "success",
    ENDED: "warning",
  }[status] || "info";
}

function isSeckillActionDisabled(row) {
  if (row?.selected) {
    return row?.activityStatus !== "ONGOING";
  }
  return row?.activityStatus !== "ONGOING" || Number(row?.remainingStock || 0) <= 0;
}

function seckillActionLabel(row) {
  if (row?.selected) {
    return row?.activityStatus === "ONGOING" ? "退选" : "已选";
  }
  if (row?.activityStatus === "NOT_STARTED") return "未开始";
  if (row?.activityStatus === "ENDED") return "已结束";
  if (Number(row?.remainingStock || 0) <= 0) return "已满";
  return "抢课";
}

function seckillActionType(row) {
  if (row?.selected) {
    return row?.activityStatus === "ONGOING" ? "warning" : "success";
  }
  if (row?.activityStatus === "ONGOING" && Number(row?.remainingStock || 0) > 0) return "danger";
  return "info";
}

async function handleSeckillEnroll(row) {
  if (isSeckillActionDisabled(row)) return;
  enrollingCourseId.value = row.courseId;
  try {
    if (row?.selected) {
      const resp = await seckillWithdraw(row.courseId);
      if (resp?.code === "WITHDRAW_SUCCESS") {
        ElMessage.success("退选成功，名额已回退");
        await Promise.all([loadSeckillCourses(), loadCourses()]);
        return;
      }
      if (resp?.code === "NOT_IN_PERIOD") {
        ElMessage.warning("仅抢课期间可退选");
      } else if (resp?.code === "NOT_SELECTED") {
        ElMessage.warning("当前未选该课程");
      } else {
        ElMessage.error(resp?.message || "退选失败");
      }
      await loadSeckillCourses();
      return;
    }

    const resp = await seckillEnroll(row.courseId);
    if (resp?.code === "SUCCESS") {
      ElMessage.success("抢课成功");
      await Promise.all([loadSeckillCourses(), loadCourses()]);
      return;
    }
    if (resp?.code === "SOLD_OUT") {
      ElMessage.warning("名额已满");
    } else if (resp?.code === "NOT_STARTED") {
      ElMessage.warning("抢课未开始");
    } else if (resp?.code === "ENDED") {
      ElMessage.warning("抢课已结束");
    } else if (resp?.code === "ALREADY_SELECTED") {
      ElMessage.warning("你已选过该课程");
    } else {
      ElMessage.error(resp?.message || "抢课失败");
    }
    await loadSeckillCourses();
  } finally {
    enrollingCourseId.value = null;
  }
}

const createDialogVisible = ref(false);
const createLoading = ref(false);
const createFormRef = ref(null);
const createForm = ref({
  courseName: "",
  courseCode: "",
  term: "",
  joinCode: "",
  status: "PENDING",
  description: "",
  enrollMode: "REVIEW",
  enrollCapacity: 50,
  enrollStartAt: "",
  enrollEndAt: "",
});

const createRules = {
  courseName: [{ required: true, message: "课程名称不能为空", trigger: "blur" }],
  courseCode: [{ required: true, message: "课程编号不能为空", trigger: "blur" }],
  term: [{ required: true, message: "请填写学期", trigger: "blur" }],
  joinCode: [{ required: true, message: "请设置加入码", trigger: "blur" }],
  status: [{ required: true, message: "请选择课程状态", trigger: "change" }],
  enrollMode: [{ required: true, message: "请选择入课模式", trigger: "change" }],
};

function genJoinCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  createForm.value.joinCode = Array.from(
    { length: 6 },
    () => chars[Math.floor(Math.random() * chars.length)],
  ).join("");
}

async function handleCreateCourse() {
  const valid = await createFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (createForm.value.enrollMode === "SECKILL") {
    if (!createForm.value.enrollCapacity || createForm.value.enrollCapacity <= 0) {
      ElMessage.error("抢课名额必须大于 0");
      return;
    }
    if (!createForm.value.enrollStartAt || !createForm.value.enrollEndAt) {
      ElMessage.error("请设置抢课开始和结束时间");
      return;
    }
    if (toMillis(createForm.value.enrollStartAt) >= toMillis(createForm.value.enrollEndAt)) {
      ElMessage.error("开始时间必须早于结束时间");
      return;
    }
  }

  createLoading.value = true;
  try {
    const payload = {
      ...createForm.value,
      teacherNo: userInfo.value.teacherNo,
      deleted: 0,
    };
    if (payload.enrollMode !== "SECKILL") {
      payload.enrollCapacity = null;
      payload.enrollStartAt = null;
      payload.enrollEndAt = null;
    }
    await createCourse(payload);
    window.dispatchEvent(new CustomEvent("course-list-updated"));
    ElMessage.success(`课程《${createForm.value.courseName}》创建成功`);
    createDialogVisible.value = false;
    currentPage.value = 1;
    createFormRef.value?.resetFields();
    createForm.value = {
      courseName: "",
      courseCode: "",
      term: "",
      joinCode: "",
      status: "PENDING",
      description: "",
      enrollMode: "REVIEW",
      enrollCapacity: 50,
      enrollStartAt: "",
      enrollEndAt: "",
    };
    await loadCourses();
  } finally {
    createLoading.value = false;
  }
}

function toMillis(value) {
  if (!value) return 0;
  if (Array.isArray(value)) {
    return new Date(value.join("-")).getTime();
  }
  return new Date(value).getTime();
}

function formatDisplayDateTime(value) {
  if (!value) return "-";
  const date = new Date(Array.isArray(value) ? value.join("-") : value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16);
  return date.toISOString().replace("T", " ").slice(0, 16);
}
</script>

<style scoped>
.module-view {
  padding: 0;
}

.module-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
}

.module-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.module-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.loading-wrap {
  padding: 20px 0;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.course-card {
  border-radius: 18px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 14px rgba(0, 0, 0, 0.07);
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;
}

.course-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.13);
}

.course-cover {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.cover-overlay {
  position: absolute;
  bottom: 8px;
  left: 12px;
}

.cover-code {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  background: rgba(0, 0, 0, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  font-family: monospace;
  letter-spacing: 1px;
}

.course-info {
  padding: 16px;
}

.course-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.course-name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.course-desc {
  font-size: 13px;
  color: #999;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
  min-height: 36px;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #aaa;
}

.join-code {
  color: #6c63ff;
  font-weight: 600;
  cursor: pointer;
  background: rgba(108, 99, 255, 0.08);
  padding: 2px 8px;
  border-radius: 8px;
}

.copy-icon {
  margin-left: 2px;
}

.dialog-body {
  padding: 4px 0 12px;
}

.dialog-hint {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0 0 16px;
  text-align: center;
}

.seckill-course-name {
  font-weight: 700;
  color: #1a1a2e;
}

.seckill-course-sub {
  margin-top: 4px;
  color: #8792a2;
  font-size: 12px;
}

@media (max-width: 768px) {
  .pagination-wrap {
    justify-content: center;
    overflow-x: auto;
  }

  .header-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
}
</style>
