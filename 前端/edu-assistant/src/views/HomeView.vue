<template>
  <div class="home" :style="themeVars">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <div class="brand-icon">
          <el-icon>
            <Reading />
          </el-icon>
        </div>
        <div v-show="!collapsed" class="brand-text">
          <strong>{{userInfo.realName}}</strong>
          <span>{{ roleLabel }}工作台</span>
        </div>
      </div>
      <button class="collapse" @click="collapsed = !collapsed">
        <el-icon>
          <DArrowLeft v-if="!collapsed" />
          <DArrowRight v-else />
        </el-icon>
      </button>
      <div class="menu-wrap">
        <template v-if="menuGroups.length">
          <div v-for="group in menuGroups" :key="group.key" class="menu-group">
            <div v-show="!collapsed" class="menu-title">{{ group.label }}</div>
            <button
              v-for="item in group.children"
              :key="item.key"
              class="menu-item"
              :class="{ active: activeMenu === item.key }"
              @click="navigate(item.key)"
            >
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
              <span v-show="!collapsed" class="menu-label">{{
                item.label
              }}</span>
              <el-badge
                v-if="!collapsed && item.badge"
                :value="formatBadge(item.badge)"
              />
            </button>
          </div>
        </template>
        <div v-else class="menu-empty">没有匹配的菜单</div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="crumb">
          <span>{{ currentGroup }}</span
          ><el-icon>
            <ArrowRight /> </el-icon
          ><strong>{{ currentTitle }}</strong>
        </div>
        <div class="tools">
          <label class="search"
            ><el-icon>
              <Search /> </el-icon
            ><input v-model="searchText" placeholder="搜索菜单或通知"
          /></label>
          <el-badge
            :value="formatBadge(dashboard.notificationCount)"
            :hidden="!dashboard.notificationCount"
          >
            <button class="icon-btn" @click="notificationDrawer = true">
              <el-icon>
                <Bell />
              </el-icon>
            </button>
          </el-badge>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <button class="user-btn">
              <div v-if="userInfo.avatarUrl" class="avatar image">
                <img :src="userInfo.avatarUrl" alt="avatar" />
              </div>
              <div v-else class="avatar">{{ avatarText }}</div>
              <div class="user-meta">
                <strong>{{
                  userInfo.realName || userInfo.username || "用户"
                }}</strong
                ><span>{{ roleLabel }}</span>
              </div>
              <el-icon>
                <ArrowDown />
              </el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile"
                  ><el-icon>
                    <UserFilled /> </el-icon
                  >个人信息</el-dropdown-item
                >
                <el-dropdown-item command="settings"
                  ><el-icon>
                    <Setting /> </el-icon
                  >系统设置</el-dropdown-item
                >
                <el-dropdown-item command="logout" divided
                  ><el-icon>
                    <SwitchButton /> </el-icon
                  >退出登录</el-dropdown-item
                >
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <template v-if="activeMenu === 'course-list'">
          <section class="hero">
            <div>
              <div class="hero-tag">{{ roleLabel }}首页</div>
              <h2>
                {{ userInfo.realName || userInfo.username || "同学" }}，欢迎回来
              </h2>
              <p>
                课程、实验、批改与题库统计已接入真实数据，通知铃铛会按角色推送关键动态。
              </p>
            </div>
            <div class="hero-side">
              <div>
                <span>主题色</span><strong>{{ currentTheme.name }}</strong>
              </div>
              <div>
                <span>待处理</span><strong>{{ todoCount }}</strong>
              </div>
              <div class="hero-actions">
                <el-button type="primary" @click="notificationDrawer = true"
                  >系统通知</el-button
                ><el-button plain @click="settingsDrawer = true"
                  >主题设置</el-button
                >
              </div>
            </div>
          </section>
          <section class="stats">
            <article v-for="stat in statCards" :key="stat.key" class="stat">
              <div class="stat-icon" :style="{ background: stat.bg }">
                <el-icon>
                  <component :is="stat.icon" />
                </el-icon>
              </div>
              <div>
                <span>{{ stat.label }}</span
                ><strong>{{ stat.value }}</strong
                ><small>{{ stat.hint }}</small>
              </div>
            </article>
          </section>
        </template>
        <section class="panel">
          <KeepAlive>
            <component :is="currentComponent" />
          </KeepAlive>
        </section>
      </main>
    </div>

    <el-drawer v-model="notificationDrawer" title="通知中心" size="420px">
      <div class="drawer-tip">
        共 {{ dashboard.notificationCount }} 条未读通知{{
          normalizedSearch ? "，已按搜索词过滤" : ""
        }}
      </div>
      <div v-if="filteredNotifications.length" class="notice-list">
        <button
          v-for="item in filteredNotifications"
          :key="item.id"
          class="notice"
          :class="{ unread: item.unread }"
          @click="handleNotificationClick(item)"
        >
          <div class="notice-head">
            <strong>{{ item.title }}</strong>
            <div class="notice-time">
              <i v-if="item.unread" class="notice-dot" />
              <span>{{ formatNotificationTime(item.createdAt) }}</span>
            </div>
          </div>
          <p>{{ item.content }}</p>
          <div class="notice-foot">
            <el-tag size="small" :type="notificationTagType(item.level)">{{
              notificationCategoryLabel(item.category)
            }}</el-tag
            ><span v-if="item.path">点击查看</span>
          </div>
        </button>
      </div>
      <el-empty v-else description="暂无通知" />
    </el-drawer>

    <el-drawer v-model="profileDrawer" title="个人中心" size="420px">
      <div class="profile">
        <div v-if="userInfo.avatarUrl" class="profile-avatar image">
          <img :src="userInfo.avatarUrl" alt="avatar" />
        </div>
        <div v-else class="profile-avatar">{{ avatarText }}</div>
        <h3>{{ userInfo.realName || userInfo.username || "用户" }}</h3>
        <el-tag round>{{ roleLabel }}</el-tag>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{
            userInfo.username || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{
            userInfo.realName || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{
            userInfo.email || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{
            userInfo.phone || "-"
          }}</el-descriptions-item>
          <el-descriptions-item v-if="showStudentNo" label="学号">{{
            userInfo.studentNo || "-"
          }}</el-descriptions-item>
          <el-descriptions-item v-if="showTeacherNo" label="工号">{{
            userInfo.teacherNo || "-"
          }}</el-descriptions-item>
        </el-descriptions>
        <el-button
          type="primary"
          class="full"
          :loading="profileRefreshing"
          @click="openProfileEditor"
          >编辑个人信息</el-button
        >
      </div>
    </el-drawer>

    <el-drawer v-model="settingsDrawer" title="系统设置" size="440px">
      <div class="drawer-tip">主题配色会按当前账号单独保存。</div>
      <div class="themes">
        <button
          v-for="theme in themeOptions"
          :key="theme.key"
          class="theme"
          :class="{ active: selectedThemeKey === theme.key }"
          @click="selectedThemeKey = theme.key"
        >
          <div class="theme-bars">
            <span :style="{ background: theme.colors.accent }"></span
            ><span :style="{ background: theme.colors.accentAlt }"></span
            ><span :style="{ background: theme.colors.sidebarEnd }"></span>
          </div>
          <strong>{{ theme.name }}</strong>
          <small>{{ theme.description }}</small>
        </button>
      </div>
    </el-drawer>

    <el-dialog v-model="editDialogVisible" title="编辑个人信息" width="520px">
      <el-form
        ref="profileFormRef"
        :model="editForm"
        :rules="profileRules"
        label-position="top"
      >
        <div class="form-grid">
          <el-form-item label="真实姓名" prop="realName"
            ><el-input v-model="editForm.realName"
          /></el-form-item>
          <el-form-item label="邮箱" prop="email"
            ><el-input v-model="editForm.email"
          /></el-form-item>
          <el-form-item label="手机号" prop="phone"
            ><el-input v-model="editForm.phone"
          /></el-form-item>
          <el-form-item v-if="showStudentNo" label="学号" prop="studentNo"
            ><el-input v-model="editForm.studentNo"
          /></el-form-item>
          <el-form-item v-if="showTeacherNo" label="工号" prop="teacherNo"
            ><el-input v-model="editForm.teacherNo"
          /></el-form-item>
          <el-form-item label="头像" prop="avatarUrl" class="span-2">
            <el-upload
              class="avatar-uploader"
              accept="image/*"
              :show-file-list="false"
              :http-request="uploadAvatarHandler"
              :before-upload="beforeAvatarUpload"
            >
              <div
                class="avatar-upload-card"
                :class="{ uploading: avatarUploading }"
              >
                <el-avatar :src="editForm.avatarUrl || undefined" :size="88">
                  {{ avatarText }}
                </el-avatar>
                <div class="avatar-upload-copy">
                  <strong>{{
                    avatarUploading ? "头像上传中..." : "点击更换头像"
                  }}</strong>
                  <span>支持 JPG、PNG、WEBP、GIF 格式，大小不超过 5MB</span>
                </div>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="新密码" prop="password"
            ><el-input
              v-model="editForm.password"
              type="password"
              show-password
              placeholder="留空表示不修改密码"
          /></el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword"
            ><el-input
              v-model="editForm.confirmPassword"
              type="password"
              show-password
          /></el-form-item>
        </div>
      </el-form>
      <template #footer
        ><el-button @click="editDialogVisible = false">取消</el-button
        ><el-button
          type="primary"
          :loading="profileSaving"
          @click="submitProfileEdit"
          >保存修改</el-button
        ></template
      >
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, markRaw, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ArrowDown,
  ArrowRight,
  Bell,
  Collection,
  DArrowLeft,
  DArrowRight,
  Document,
  Files,
  Memo,
  Notebook,
  Reading,
  Search,
  Setting,
  SwitchButton,
  UserFilled,
} from "@element-plus/icons-vue";
import {
  getHomeDashboard,
  markDashboardNotificationRead,
  getProfile,
  logout as logoutApi,
  updateUser,
  uploadAvatar,
} from "@/api/user";
import {
  clearAuthSession,
  getToken,
  getUserInfo,
  setAuthSession,
} from "@/utils/auth";
import CourseListView from "./home/CourseListView.vue";
import ExperimentView from "./home/ExperimentView.vue";
import KnowledgeView from "./home/KnowledgeView.vue";
import PracticeView from "./home/PracticeView.vue";
import QuestionBankView from "./home/QuestionBankView.vue";
import ReportView from "./home/ReportView.vue";

const router = useRouter();
const route = useRoute();
const VIEW_MAP = {
  "course-list": markRaw(CourseListView),
  experiment: markRaw(ExperimentView),
  report: markRaw(ReportView),
  knowledge: markRaw(KnowledgeView),
  "question-bank": markRaw(QuestionBankView),
  practice: markRaw(PracticeView),
};
const TITLE_MAP = {
  "course-list": ["课程管理", "课程列表"],
  experiment: ["实验报告", "实验"],
  report: ["实验报告", "报告"],
  knowledge: ["题库测验", "知识点"],
  "question-bank": ["题库测验", "题库"],
  practice: ["题库测验", "试卷练习"],
};
const themeOptions = [
  {
    key: "ocean",
    name: "海湾蓝",
    description: "清爽蓝绿",
    colors: {
      pageBg: "#eef5ff",
      surface: "#fff",
      surfaceAlt: "#f7fbff",
      text: "#102a43",
      muted: "#5f7185",
      border: "rgba(16,42,67,.08)",
      accent: "#2563eb",
      accentAlt: "#14b8a6",
      sidebarStart: "#13284d",
      sidebarEnd: "#0f766e",
      hero: "linear-gradient(135deg,#1d4ed8,#14b8a6)",
    },
  },
  {
    key: "sunset",
    name: "日落橙",
    description: "暖色强调",
    colors: {
      pageBg: "#fff4ec",
      surface: "#fff",
      surfaceAlt: "#fff8f1",
      text: "#4c1d14",
      muted: "#8a5a47",
      border: "rgba(76,29,20,.08)",
      accent: "#ea580c",
      accentAlt: "#f59e0b",
      sidebarStart: "#7c2d12",
      sidebarEnd: "#b45309",
      hero: "linear-gradient(135deg,#ea580c,#f59e0b)",
    },
  },
  {
    key: "forest",
    name: "森林绿",
    description: "沉稳专注",
    colors: {
      pageBg: "#eef8f1",
      surface: "#fff",
      surfaceAlt: "#f6fcf7",
      text: "#133128",
      muted: "#5a736b",
      border: "rgba(19,49,40,.08)",
      accent: "#15803d",
      accentAlt: "#0f766e",
      sidebarStart: "#163b30",
      sidebarEnd: "#166534",
      hero: "linear-gradient(135deg,#166534,#0f766e)",
    },
  },
  {
    key: "slate",
    name: "雾灰",
    description: "低饱和专业风",
    colors: {
      pageBg: "#f2f6fa",
      surface: "#fff",
      surfaceAlt: "#f8fafc",
      text: "#0f172a",
      muted: "#64748b",
      border: "rgba(15,23,42,.08)",
      accent: "#334155",
      accentAlt: "#0f766e",
      sidebarStart: "#0f172a",
      sidebarEnd: "#334155",
      hero: "linear-gradient(135deg,#0f172a,#334155)",
    },
  },
  {
    key: "dawn",
    name: "晨曦粉",
    description: "柔和亮色",
    colors: {
      pageBg: "#fff2f6",
      surface: "#fff",
      surfaceAlt: "#fff8fa",
      text: "#4a1f2b",
      muted: "#8e6570",
      border: "rgba(74,31,43,.08)",
      accent: "#db2777",
      accentAlt: "#fb7185",
      sidebarStart: "#831843",
      sidebarEnd: "#be185d",
      hero: "linear-gradient(135deg,#be185d,#fb7185)",
    },
  },
];
const menuDefs = [
  {
    key: "course",
    label: "课程管理",
    children: [
      { key: "course-list", label: "课程列表", icon: markRaw(Notebook) },
    ],
  },
  {
    key: "report-group",
    label: "实验报告",
    children: [
      { key: "experiment", label: "实验", icon: markRaw(Files) },
      { key: "report", label: "报告", icon: markRaw(Document) },
    ],
  },
  {
    key: "question-group",
    label: "题库测验",
    children: [
      { key: "knowledge", label: "知识点", icon: markRaw(Collection) },
      { key: "question-bank", label: "题库", icon: markRaw(Collection) },
      { key: "practice", label: "试卷练习", icon: markRaw(Memo) },
    ],
  },
];
const userInfo = computed(() => getUserInfo() || {});
const dashboard = ref(createDashboard());
const collapsed = ref(false);
const searchText = ref("");
const activeMenu = ref("course-list");
const notificationDrawer = ref(false);
const profileDrawer = ref(false);
const settingsDrawer = ref(false);
const editDialogVisible = ref(false);
const selectedThemeKey = ref("ocean");
const profileRefreshing = ref(false);
const profileSaving = ref(false);
const avatarUploading = ref(false);
const profileFormRef = ref();
const readingNotificationIds = new Set();
const editForm = reactive({
  realName: "",
  email: "",
  phone: "",
  studentNo: "",
  teacherNo: "",
  avatarUrl: "",
  password: "",
  confirmPassword: "",
});
const normalizedRole = computed(() =>
  normalizeRole(userInfo.value.role || dashboard.value.role),
);
const roleLabel = computed(
  () =>
    ({ STUDENT: "学生", TEACHER: "教师", ADMIN: "管理员" })[
      normalizedRole.value
    ] || "用户",
);
const avatarText = computed(() =>
  (userInfo.value.realName || userInfo.value.username || "U")
    .slice(0, 1)
    .toUpperCase(),
);
const showStudentNo = computed(() => normalizedRole.value === "STUDENT");
const showTeacherNo = computed(() => normalizedRole.value === "TEACHER");
const currentComponent = computed(
  () => VIEW_MAP[activeMenu.value] || CourseListView,
);
const currentGroup = computed(
  () => TITLE_MAP[activeMenu.value]?.[0] || "课程管理",
);
const currentTitle = computed(
  () => TITLE_MAP[activeMenu.value]?.[1] || "课程列表",
);
const normalizedSearch = computed(() => searchText.value.trim().toLowerCase());
const currentTheme = computed(
  () =>
    themeOptions.find((item) => item.key === selectedThemeKey.value) ||
    themeOptions[0],
);
const themeVars = computed(() => ({
  "--page-bg": currentTheme.value.colors.pageBg,
  "--surface": currentTheme.value.colors.surface,
  "--surface-alt": currentTheme.value.colors.surfaceAlt,
  "--text-primary": currentTheme.value.colors.text,
  "--text-secondary": currentTheme.value.colors.muted,
  "--border-color": currentTheme.value.colors.border,
  "--accent": currentTheme.value.colors.accent,
  "--accent-alt": currentTheme.value.colors.accentAlt,
  "--sidebar-start": currentTheme.value.colors.sidebarStart,
  "--sidebar-end": currentTheme.value.colors.sidebarEnd,
  "--hero-bg": currentTheme.value.colors.hero,
}));
const menuGroups = computed(() =>
  menuDefs
    .map((group) => ({
      ...group,
      children: group.children
        .map((item) => ({
          ...item,
          badge: Number(
            {
              experiment: dashboard.value.experimentBadgeCount,
              report: dashboard.value.reportBadgeCount,
              "question-bank": dashboard.value.questionBadgeCount,
              practice: dashboard.value.practiceBadgeCount,
            }[item.key] || 0,
          ),
        }))
        .filter(
          (item) =>
            !normalizedSearch.value ||
            item.label.toLowerCase().includes(normalizedSearch.value),
        ),
    }))
    .filter((group) => group.children.length),
);
const filteredNotifications = computed(() => {
  const list = Array.isArray(dashboard.value.notifications)
    ? dashboard.value.notifications
    : [];
  return !normalizedSearch.value
    ? list
    : list.filter((item) =>
        [item.title, item.content, item.category]
          .join(" ")
          .toLowerCase()
          .includes(normalizedSearch.value),
      );
});
const todoCount = computed(() =>
  normalizedRole.value === "STUDENT"
    ? Number(dashboard.value.pendingExperimentCount || 0)
    : Number(dashboard.value.pendingReportReviewCount || 0) +
      Number(dashboard.value.questionBadgeCount || 0) +
      Number(dashboard.value.practiceBadgeCount || 0),
);
const statCards = computed(() => [
  {
    key: "courseCount",
    label: "课程总数",
    value: dashboard.value.courseCount,
    hint:
      normalizedRole.value === "STUDENT" ? "当前已加入课程" : "当前负责课程",
    icon: markRaw(Notebook),
    bg: "linear-gradient(135deg,var(--accent),var(--accent-alt))",
  },
  ...(normalizedRole.value === "STUDENT"
    ? [
        {
          key: "pendingExperimentCount",
          label: "待提交实验",
          value: dashboard.value.pendingExperimentCount ?? 0,
          hint: "需要尽快完成提交",
          icon: markRaw(Files),
          bg: "linear-gradient(135deg,#f97316,#f59e0b)",
        },
      ]
    : [
        {
          key: "pendingReportReviewCount",
          label: "待批阅报告",
          value: dashboard.value.pendingReportReviewCount ?? 0,
          hint: "等待你完成批改",
          icon: markRaw(Document),
          bg: "linear-gradient(135deg,#16a34a,#14b8a6)",
        },
      ]),
  {
    key: "questionCount",
    label: "题库题目数",
    value: dashboard.value.questionCount,
    hint:
      normalizedRole.value === "STUDENT"
        ? "当前课程可见题目"
        : "当前课程全部题目",
    icon: markRaw(Collection),
    bg: "linear-gradient(135deg,#3b82f6,#06b6d4)",
  },
]);
const profileRules = {
  realName: [{ required: true, message: "请输入真实姓名", trigger: "blur" }],
  email: [
    { type: "email", message: "请输入正确的邮箱", trigger: ["blur", "change"] },
  ],
  phone: [
    {
      validator: (_, value, callback) =>
        !value || /^1[3-9]\d{9}$/.test(value)
          ? callback()
          : callback(new Error("请输入正确的手机号")),
      trigger: "blur",
    },
  ],
  studentNo: [
    {
      validator: (_, value, callback) =>
        showStudentNo.value && !value?.trim()
          ? callback(new Error("请输入学号"))
          : callback(),
      trigger: "blur",
    },
  ],
  teacherNo: [
    {
      validator: (_, value, callback) =>
        showTeacherNo.value && !value?.trim()
          ? callback(new Error("请输入工号"))
          : callback(),
      trigger: "blur",
    },
  ],
  password: [
    {
      validator: (_, value, callback) =>
        !value || value.length >= 6
          ? callback()
          : callback(new Error("密码至少 6 位")),
      trigger: "blur",
    },
  ],
  confirmPassword: [
    {
      validator: (_, value, callback) =>
        !editForm.password || value === editForm.password
          ? callback()
          : callback(new Error("两次输入的密码不一致")),
      trigger: "blur",
    },
  ],
};
watch(
  () => route.query.menu,
  (menu) => {
    activeMenu.value = VIEW_MAP[menu] ? menu : "course-list";
  },
  { immediate: true },
);
watch(
  () => userInfo.value.id,
  (userId) => {
    if (userId) {
      loadTheme(userId);
      loadDashboard();
    }
  },
  { immediate: true },
);
watch(selectedThemeKey, (value) => {
  const userId = userInfo.value.id;
  if (userId && typeof window !== "undefined")
    window.localStorage.setItem(`home-theme:${userId}`, value);
});
async function loadDashboard() {
  try {
    const result = await getHomeDashboard();
    dashboard.value = {
      ...createDashboard(),
      ...result,
      notifications: Array.isArray(result?.notifications)
        ? result.notifications.map((item) => ({
            ...item,
            unread: Boolean(item?.unread),
          }))
        : [],
      notificationCount: Number(result?.notificationCount || 0),
    };
  } catch {}
}
function navigate(menu) {
  router.replace({ path: "/home", query: { menu } });
}
function handleUserCommand(command) {
  if (command === "profile") profileDrawer.value = true;
  if (command === "settings") settingsDrawer.value = true;
  if (command === "logout") logout();
}
async function logout() {
  try {
    await ElMessageBox.confirm("确定要退出当前账号吗？", "退出登录", {
      type: "warning",
    });
    await logoutApi().catch(() => {});
    clearAuthSession();
    router.push("/login");
  } catch {}
}
async function handleNotificationClick(item) {
  await markNotificationAsRead(item);
  notificationDrawer.value = false;
  if (item?.path) await router.push(item.path);
}
async function markNotificationAsRead(item) {
  const notificationId = item?.id;
  if (!notificationId || !item?.unread || readingNotificationIds.has(notificationId))
    return;
  readingNotificationIds.add(notificationId);
  item.unread = false;
  dashboard.value.notificationCount = Math.max(
    0,
    Number(dashboard.value.notificationCount || 0) - 1,
  );
  try {
    await markDashboardNotificationRead(notificationId);
  } catch {
    item.unread = true;
    dashboard.value.notificationCount = Number(dashboard.value.notificationCount || 0) + 1;
    ElMessage.error("通知状态更新失败，请稍后重试");
  } finally {
    readingNotificationIds.delete(notificationId);
  }
}
async function openProfileEditor() {
  profileRefreshing.value = true;
  try {
    const profile = await getProfile();
    Object.assign(editForm, {
      realName: profile?.realName || "",
      email: profile?.email || "",
      phone: profile?.phone || "",
      studentNo: profile?.studentNo || "",
      teacherNo: profile?.teacherNo || "",
      avatarUrl: profile?.avatarUrl || "",
      password: "",
      confirmPassword: "",
    });
    profileDrawer.value = false;
    editDialogVisible.value = true;
  } finally {
    profileRefreshing.value = false;
  }
}
function beforeAvatarUpload(file) {
  const isImage = file.type?.startsWith("image/");
  if (!isImage) {
    ElMessage.error("请上传图片格式的头像文件");
    return false;
  }
  const maxSize = 5 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.error("头像大小不能超过 5MB");
    return false;
  }
  return true;
}
async function uploadAvatarHandler({ file }) {
  avatarUploading.value = true;
  try {
    const response = await uploadAvatar(file);
    editForm.avatarUrl = response?.url || "";
    ElMessage.success("头像上传成功");
  } catch {
    ElMessage.error("头像上传失败");
  } finally {
    avatarUploading.value = false;
  }
}
async function submitProfileEdit() {
  const valid = await profileFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  profileSaving.value = true;
  try {
    const updated = await updateUser({
      realName: editForm.realName.trim(),
      email: editForm.email.trim() || null,
      phone: editForm.phone.trim() || null,
      avatarUrl: editForm.avatarUrl.trim() || null,
      studentNo: showStudentNo.value ? editForm.studentNo.trim() : null,
      teacherNo: showTeacherNo.value ? editForm.teacherNo.trim() : null,
      password: editForm.password || null,
    });
    setAuthSession(getToken(), updated);
    ElMessage.success("个人信息已更新");
    editDialogVisible.value = false;
    await loadDashboard();
  } finally {
    profileSaving.value = false;
  }
}
function loadTheme(userId) {
  if (typeof window !== "undefined") {
    const stored = window.localStorage.getItem(`home-theme:${userId}`);
    selectedThemeKey.value = themeOptions.some((item) => item.key === stored)
      ? stored
      : "ocean";
  }
}
function createDashboard() {
  return {
    role: "",
    courseCount: 0,
    pendingExperimentCount: null,
    pendingReportReviewCount: null,
    questionCount: 0,
    notificationCount: 0,
    experimentBadgeCount: 0,
    reportBadgeCount: 0,
    questionBadgeCount: 0,
    practiceBadgeCount: 0,
    notifications: [],
  };
}
function normalizeRole(role) {
  const value = String(role || "")
    .trim()
    .toUpperCase();
  return value === "TEACHER" || value === "ADMIN" ? value : "STUDENT";
}
function formatBadge(value) {
  const count = Number(value || 0);
  return count > 99 ? "99+" : count || "";
}
function formatNotificationTime(value) {
  const time = value ? new Date(value) : null;
  if (!time || Number.isNaN(time.getTime())) return "刚刚";
  const diff = Date.now() - time.getTime();
  if (diff < 60000) return "刚刚";
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`;
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`;
  return time.toLocaleDateString();
}
function notificationTagType(level) {
  return (
    { success: "success", warning: "warning", info: "primary" }[level] || "info"
  );
}
function notificationCategoryLabel(category) {
  return (
    {
      COURSE: "课程",
      EXPERIMENT: "实验",
      QUESTION: "题库",
      REPORT: "报告",
      PRACTICE: "测验",
    }[category] || "通知"
  );
}
</script>

<style scoped>
.home {
  display: flex;
  min-height: 100vh;
  background: var(--page-bg);
  color: var(--text-primary);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.sidebar {
  width: 228px;
  min-width: 228px;
  position: relative;
  background: linear-gradient(180deg, var(--sidebar-start), var(--sidebar-end));
  color: #fff;
  transition: 0.25s;
}

.sidebar.collapsed {
  width: 72px;
  min-width: 72px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 18px;
}

.brand-icon {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--accent), var(--accent-alt));
  color: #fff;
}

.avatar,
.profile-avatar
{
  width: 40px;          /* 根据你的设计调整尺寸 */
  height: 40px;         /* 宽高必须一致 */
  border-radius: 50%;   /* 核心代码：设置为 50% 即为圆形 */
  overflow: hidden;     /* 必须添加：隐藏超出圆圈部分的图片内容 */
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5; /* 无头像时的背景色 */
  flex-shrink: 0;       /* 防止在 flex 布局中被挤压 */
}

.brand-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-text span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.collapse {
  position: absolute;
  top: 24px;
  right: -12px;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: #fff;
  color: var(--accent);
  cursor: pointer;
}

.menu-wrap {
  height: calc(100vh - 86px);
  overflow: auto;
  padding: 8px 10px 20px;
}

.menu-title {
  padding: 10px 12px 6px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin: 4px 0;
  border: none;
  border-radius: 14px;
  background: transparent;
  color: rgba(255, 255, 255, 0.76);
  cursor: pointer;
  text-align: left;
}

.menu-item.active,
.menu-item:hover {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.menu-label {
  flex: 1;
}

.menu-empty {
  padding: 18px 12px;
  color: rgba(255, 255, 255, 0.7);
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 28px;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--border-color);
}

.crumb,
.tools,
.notice-head,
.notice-foot,
.notice-time {
  display: flex;
  align-items: center;
  gap: 10px;
}

.crumb span,
.drawer-tip,
.notice-head span,
.notice-foot span,
.user-meta span,
.stat small,
.stat span {
  color: var(--text-secondary);
}

.tools {
  gap: 14px;
}

.search {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 240px;
  height: 42px;
  padding: 0 14px;
  border-radius: 999px;
  background: var(--surface);
  border: 1px solid var(--border-color);
}

.search input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
}

.icon-btn,
.user-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
}

.icon-btn {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  cursor: pointer;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 8px;
  border-radius: 999px;
  cursor: pointer;
}

.avatar,
.avatar.image,
.profile-avatar,
.profile-avatar.image {
  width: 42px;
  height: 42px;
  border-radius: 50%;
}

.avatar.image img,
.profile-avatar.image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.content {
  padding: 24px 28px 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 28px;
  border-radius: 28px;
  background: var(--hero-bg);
  color: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.16);
}

.hero-tag {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
}

.hero h2 {
  margin: 10px 0;
}

.hero p {
  margin: 0;
  max-width: 620px;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.7;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 220px;
}

.hero-side > div {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
}

.hero-side span {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.hero-side strong {
  font-size: 22px;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stat,
.panel,
.theme,
.notice {
  background: var(--surface);
  border: 1px solid var(--border-color);
}

.stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border-radius: 22px;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.05);
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  color: #fff;
  font-size: 24px;
}

.stat strong {
  display: block;
  font-size: 28px;
  line-height: 1.1;
  color: var(--text-primary);
}

.panel {
  min-height: 520px;
  padding: 28px;
  border-radius: 28px;
  overflow: auto;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.06);
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice {
  width: 100%;
  padding: 16px;
  border-radius: 18px;
  text-align: left;
  cursor: pointer;
}

.notice.unread {
  border-color: rgba(239, 68, 68, 0.45);
}

.notice-head {
  justify-content: space-between;
}

.notice-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  flex-shrink: 0;
}

.notice p {
  margin: 10px 0 14px;
  line-height: 1.6;
  color: var(--text-primary);
}

.profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  width: 88px;
  height: 88px;
  font-size: 30px;
}

.full {
  width: 100%;
}

.themes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.theme {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border-radius: 20px;
  text-align: left;
  cursor: pointer;
}

.theme.active {
  border-color: var(--accent);
}

.theme-bars {
  display: flex;
  gap: 8px;
}

.theme-bars span {
  width: 32px;
  height: 12px;
  border-radius: 999px;
}

.avatar-uploader {
  width: 100%;
}

:deep(.avatar-uploader .el-upload) {
  width: 100%;
}

.avatar-upload-card {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 18px 20px;
  border: 1px dashed var(--border-color);
  border-radius: 20px;
  background: var(--surface-alt);
  cursor: pointer;
  transition: 0.2s ease;
}

.avatar-upload-card:hover,
.avatar-upload-card.uploading {
  border-color: var(--accent);
  background: rgba(37, 99, 235, 0.08);
}

.avatar-upload-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.avatar-upload-copy strong {
  color: var(--text-primary);
}

.avatar-upload-copy span {
  font-size: 12px;
  color: var(--text-secondary);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.span-2 {
  grid-column: 1/-1;
}

@media (max-width: 1100px) {
  .hero,
  .topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .sidebar {
    width: 72px;
    min-width: 72px;
  }

  .brand-text,
  .menu-title,
  .menu-label,
  .user-meta {
    display: none;
  }

  .search {
    width: 100%;
  }

  .tools {
    width: 100%;
    flex-wrap: wrap;
  }

  .content {
    padding: 18px;
  }

  .panel {
    padding: 18px;
  }

  .themes,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
