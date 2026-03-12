<template>
  <div class="module-view">
    <!-- 头部 -->
    <div class="module-header">
      <div>
        <h2 class="module-title">课程列表</h2>
        <p class="module-desc">
          <template v-if="isTeacher">管理您创建的所有课程</template>
          <template v-else>查看并加入您的课程</template>
        </p>
      </div>
      <div class="header-actions">
        <!-- 学生：加入课程 -->
        <el-button
          v-if="isStudent"
          type="primary"
          :icon="Plus"
          round
          @click="joinDialogVisible = true"
        >
          加入课程
        </el-button>
        <!-- 教师：创建课程 -->
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

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 空状态 -->
    <el-empty
      v-else-if="!loading && totalCourses === 0"
      description="暂无课程，快去加入或创建吧！"
      :image-size="120"
    />

    <!-- 课程卡片网格 -->
    <template v-else>
      <div class="course-grid">
        <div
          v-for="(course, index) in courses"
          :key="course.id"
          class="course-card"
          @click="goDetail(course.id)"
        >
          <!-- 封面 -->
          <div
            class="course-cover"
            :style="{ background: coverColors[index % coverColors.length] }"
          >
            <el-icon size="38" color="rgba(255,255,255,0.9)"><Reading /></el-icon>
            <div class="cover-overlay">
              <span class="cover-code">{{ course.courseCode }}</span>
            </div>
          </div>
          <!-- 信息 -->
          <div class="course-info">
            <div class="course-name-row">
              <h3 class="course-name">{{ course.courseName }}</h3>
              <el-tag
                size="small"
                :type="statusTagType(course.status)"
                effect="light"
              >
                {{ statusLabel(course.status) }}
              </el-tag>
            </div>
            <p class="course-desc">{{ course.description || "暂无课程简介" }}</p>
            <div class="course-meta">
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>{{ course.term || "-" }}
              </span>
              <!-- 教师可见：加入码 -->
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

    <!-- ========== 学生：加入课程弹窗 ========== -->
    <el-dialog
      v-model="joinDialogVisible"
      title="加入课程"
      width="400px"
      align-center
      :close-on-click-modal="false"
    >
      <div class="dialog-body">
        <p class="dialog-hint">请输入教师提供的课程加入码</p>
        <el-form
          ref="joinFormRef"
          :model="joinForm"
          :rules="joinRules"
          size="large"
        >
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
        <el-button
          type="primary"
          :loading="joinLoading"
          @click="handleJoinCourse"
          >确认加入</el-button
        >
      </template>
    </el-dialog>

    <!-- ========== 教师：创建课程弹窗 ========== -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建新课程"
      width="520px"
      align-center
      :close-on-click-modal="false"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="90px"
        size="large"
      >
        <el-form-item label="课程名称" prop="courseName">
          <el-input
            v-model="createForm.courseName"
            placeholder="请输入课程名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="课程编号" prop="courseCode">
          <el-input
            v-model="createForm.courseCode"
            placeholder="如：CS101"
            clearable
          />
        </el-form-item>
        <el-form-item label="学期" prop="term">
          <el-input
            v-model="createForm.term"
            placeholder="如：2025春季"
            clearable
          />
        </el-form-item>
        <el-form-item label="加入码" prop="joinCode">
          <el-input
            v-model="createForm.joinCode"
            placeholder="学生通过此码申请加入"
            clearable
          >
            <template #append>
              <el-button @click="genJoinCode">随机生成</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="课程状态" prop="status">
          <el-select v-model="createForm.status" style="width: 100%">
            <el-option label="未开始" value="PENDING" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已结课" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入课程简介（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="createLoading"
          @click="handleCreateCourse"
          >确认创建</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  Plus,
  Reading,
  Calendar,
  Key,
  CopyDocument,
} from "@element-plus/icons-vue";
import {
  getMyCoursePage,
  createCourse,
  joinCourseByCode,
} from "@/api/course";
import { getUserInfo } from "@/utils/auth";

const router = useRouter();

// ==================== 用户身份 ====================
const userInfo = computed(() => getUserInfo());
const isTeacher = computed(() =>
  ["TEACHER", "ADMIN"].includes(userInfo.value.role),
);
const isStudent = computed(() => userInfo.value.role === "STUDENT");

// ==================== 封面颜色池 ====================
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

// ==================== 课程列表 ====================
const loading = ref(false);
const courses = ref([]);
const currentPage = ref(1);
const pageSize = ref(8);
const totalCourses = ref(0);

const loadCourses = async () => {
  loading.value = true;
  try {
    const data = await getMyCoursePage({
      current: currentPage.value,
      size: pageSize.value,
    });
    courses.value = Array.isArray(data?.records) ? data.records : [];
    totalCourses.value = Number(data?.total || 0);
    currentPage.value = Number(data?.current || currentPage.value);
  } catch {
    // 拦截器已处理错误提示
  } finally {
    loading.value = false;
  }
};

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

// ==================== 状态标签映射 ====================
const statusLabel = (status) => {
  const map = { ACTIVE: "进行中", PENDING: "未开始", CLOSED: "已结课" };
  return map[status] || status || "未知";
};
const statusTagType = (status) => {
  const map = { ACTIVE: "success", PENDING: "warning", CLOSED: "info" };
  return map[status] || "";
};

// ==================== 跳转详情 ====================
const goDetail = (id) => {
  router.push(`/course/${id}`);
};

// ==================== 复制加入码 ====================
const copyJoinCode = (code) => {
  if (!code) return;
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success(`加入码 ${code} 已复制到剪贴板`);
  });
};

// ==================== 学生：加入课程 ====================
const joinDialogVisible = ref(false);
const joinLoading = ref(false);
const joinFormRef = ref(null);
const joinForm = ref({ joinCode: "" });
const joinRules = {
  joinCode: [{ required: true, message: "请输入课程加入码", trigger: "blur" }],
};

const handleJoinCourse = async () => {
  const valid = await joinFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  joinLoading.value = true;
  try {
    const studentNo = userInfo.value.studentNo;
    if (!studentNo) {
      ElMessage.error("请先在个人中心完善学号信息");
      return;
    }
    await joinCourseByCode(joinForm.value.joinCode.trim(), studentNo);
    ElMessage.success("申请已提交，等待教师审批");
    joinDialogVisible.value = false;
    joinForm.value.joinCode = "";
  } catch {
    // 拦截器已处理
  } finally {
    joinLoading.value = false;
  }
};

// ==================== 教师：创建课程 ====================
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
});
const createRules = {
  courseName: [
    { required: true, message: "课程名称不能为空", trigger: "blur" },
  ],
  courseCode: [
    { required: true, message: "课程编号不能为空", trigger: "blur" },
  ],
  term: [{ required: true, message: "请填写学期", trigger: "blur" }],
  joinCode: [{ required: true, message: "请设置加入码", trigger: "blur" }],
  status: [{ required: true, message: "请选择课程状态", trigger: "change" }],
};

/** 随机生成6位加入码 */
const genJoinCode = () => {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  createForm.value.joinCode = Array.from(
    { length: 6 },
    () => chars[Math.floor(Math.random() * chars.length)],
  ).join("");
};

const handleCreateCourse = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  createLoading.value = true;
  try {
    const payload = {
      ...createForm.value,
      teacherNo: userInfo.value.teacherNo,
      deleted: 0,
    };
    await createCourse(payload);
    window.dispatchEvent(new CustomEvent("course-list-updated"));
    ElMessage.success(`课程《${createForm.value.courseName}》创建成功！`);
    createDialogVisible.value = false;
    currentPage.value = 1;
    // 重置表单并刷新列表
    createFormRef.value?.resetFields();
    createForm.value = {
      courseName: "",
      courseCode: "",
      term: "",
      joinCode: "",
      status: "PENDING",
      description: "",
    };
    await loadCourses();
  } catch {
    // 拦截器已处理
  } finally {
    createLoading.value = false;
  }
};
</script>

<style scoped>
.module-view {
  padding: 0;
}

/* ========== 头部 ========== */
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

/* ========== 加载占位 ========== */
.loading-wrap {
  padding: 20px 0;
}

/* ========== 课程网格 ========== */
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

/* ========== 课程卡片 ========== */
.course-card {
  border-radius: 18px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 14px rgba(0, 0, 0, 0.07);
  cursor: pointer;
  transition:
    transform 0.25s,
    box-shadow 0.25s;
}
.course-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.13);
}

/* 封面 */
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

/* 信息区 */
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
  transition: background 0.2s;
}
.join-code:hover {
  background: rgba(108, 99, 255, 0.18);
}
.copy-icon {
  margin-left: 2px;
}

/* ========== 弹窗 ========== */
.dialog-body {
  padding: 4px 0 12px;
}
.dialog-hint {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0 0 16px;
  text-align: center;
}

@media (max-width: 768px) {
  .pagination-wrap {
    justify-content: center;
    overflow-x: auto;
  }
}
</style>
