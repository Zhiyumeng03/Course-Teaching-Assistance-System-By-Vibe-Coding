<template>
  <div class="practice-view">
    <section class="hero-panel">
      <div>
        <h2 class="hero-title">{{ isTeacher ? '试卷练习管理' : '试卷练习中心' }}</h2>
        <p class="hero-desc">
          {{ isTeacher ? '按课程组卷、发布试卷、批改学生练习，并结合 AI 诊断学习掌握情况。' : '开始练习后将自动倒计时，可随时保存草稿，超时后系统会自动提交。' }}
        </p>
      </div>
      <div class="hero-actions">
        <el-button
          v-if="!isTeacher && activePracticePaper"
          class="hero-button secondary"
          :icon="Reading"
          @click="resumeActivePractice"
        >
          继续作答
        </el-button>
        <el-button
          class="hero-button secondary"
          :icon="RefreshRight"
          :loading="paperLoading || recordLoading"
          @click="refreshCourseData"
        >
          刷新数据
        </el-button>
        <el-button
          v-if="isTeacher"
          type="primary"
          class="hero-button"
          :icon="Plus"
          :disabled="!selectedCourseId"
          @click="openCreatePaperDialog"
        >
          新建试卷
        </el-button>
      </div>
    </section>

    <section class="toolbar-panel">
      <el-select
        v-model="selectedCourseId"
        class="toolbar-item"
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

      <el-input
        v-model="paperKeyword"
        class="toolbar-item grow"
        clearable
        placeholder="搜索试卷标题"
      />

      <el-button @click="resetFilters">重置试卷筛选</el-button>
    </section>

    <section class="stats-panel">
      <div class="stat-card">
        <span>当前课程</span>
        <strong>{{ selectedCourseName || '未选择' }}</strong>
      </div>
      <div class="stat-card">
        <span>试卷总数</span>
        <strong>{{ paperTotal }}</strong>
      </div>
      <div class="stat-card">
        <span>待批改</span>
        <strong>{{ pendingRecordCount }}</strong>
      </div>
      <div class="stat-card">
        <span>{{ isTeacher ? '已诊断' : '已批改' }}</span>
        <strong>{{ highlightedRecordCount }}</strong>
      </div>
    </section>

    <div v-if="initialLoading" class="loading-wrap">
      <el-skeleton :rows="8" animated />
    </div>

    <el-empty
      v-else-if="courseOptions.length === 0"
      description="当前没有可用课程"
      :image-size="100"
    />

    <section v-else class="content-grid">
      <article class="paper-panel">
        <div class="panel-head">
          <div>
            <h3>{{ isTeacher ? '试卷列表' : '可练习试卷' }}</h3>
            <p>{{ paperTotal }} 份试卷</p>
          </div>
          <el-tag effect="plain" type="info">{{ selectedCourseName || '未选择课程' }}</el-tag>
        </div>

        <div v-if="selectedCourseId && paperTotal > 0" class="panel-toolbar">
          <div class="panel-toolbar-text">分页查询已启用，支持按试卷标题检索</div>
          <el-pagination
            v-model:current-page="paperCurrentPage"
            v-model:page-size="paperPageSize"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :page-sizes="[6, 10, 20]"
            :total="paperTotal"
            @current-change="handlePaperCurrentChange"
            @size-change="handlePaperSizeChange"
          />
        </div>

        <div v-if="paperLoading" class="loading-wrap compact">
          <el-skeleton :rows="6" animated />
        </div>
        <el-empty
          v-else-if="!selectedCourseId"
          description="请先选择课程"
          :image-size="90"
        />
        <el-empty
          v-else-if="paperTotal === 0"
          description="当前课程下还没有试卷"
          :image-size="90"
        />

        <div v-else class="paper-list">
          <article
            v-for="paper in paperList"
            :key="paper.id"
            class="paper-card"
            :class="{ active: activePracticePaper?.paper?.id === paper.id }"
          >
            <div class="paper-card-top">
              <div class="paper-main">
                <div class="paper-title-row">
                  <h4>{{ paper.title }}</h4>
                  <el-tag :type="paperStatusTagType(paper.status)">
                    {{ paperStatusLabel(paper.status) }}
                  </el-tag>
                  <el-tag v-if="hasDraft(paper.id)" effect="plain" type="warning">
                    有草稿
                  </el-tag>
                </div>
                <div class="paper-meta">
                  <span>题数 {{ paper.questionCount || 0 }}</span>
                  <span>总分 {{ paper.totalScore || 0 }}</span>
                  <span>时长 {{ paper.durationMinutes || '--' }} 分钟</span>
                  <span>{{ formatDate(paper.createdAt) }}</span>
                </div>
              </div>

              <div class="card-actions">
                <template v-if="isTeacher">
                  <el-button link type="primary" @click="openPaperPreview(paper)">预览</el-button>
                  <el-button link type="primary" @click="openEditPaperDialog(paper)">编辑</el-button>
                  <el-button
                    v-if="paper.status === 'DRAFT'"
                    link
                    type="success"
                    @click="publishPaperItem(paper)"
                  >
                    发布
                  </el-button>
                  <el-button
                    v-if="paper.status === 'PUBLISHED'"
                    link
                    type="warning"
                    @click="closePaperItem(paper)"
                  >
                    关闭
                  </el-button>
                  <el-button link type="danger" @click="removePaper(paper)">删除</el-button>
                </template>

                <template v-else>
                  <el-button
                    type="primary"
                    :disabled="paper.status !== 'PUBLISHED'"
                    @click="openPracticeDialog(paper)"
                  >
                    {{ activePracticePaper?.paper?.id === paper.id || hasDraft(paper.id) ? '继续作答' : '开始练习' }}
                  </el-button>
                </template>
              </div>
            </div>
          </article>
        </div>
      </article>

      <article class="record-panel">
        <div class="panel-head">
          <div>
            <h3>{{ isTeacher ? '批改工作台' : '我的练习记录' }}</h3>
            <p>{{ recordTotal }} 条记录</p>
          </div>
          <el-tag :type="pendingRecordCount > 0 ? 'warning' : 'success'">
            {{ pendingRecordCount > 0 ? `${pendingRecordCount} 条待处理` : '已处理完成' }}
          </el-tag>
        </div>

        <div v-if="isTeacher && selectedCourseId" class="record-filter-bar">
          <el-select v-model="teacherRecordStatus" class="record-filter-item" placeholder="批改状态">
            <el-option label="待批改" value="PENDING_REVIEW" />
            <el-option label="已批改" value="GRADED" />
            <el-option label="全部记录" value="" />
          </el-select>
          <el-input
            v-model="teacherRecordKeyword"
            class="record-filter-item grow"
            clearable
            placeholder="搜索学生姓名或学号"
          />
          <el-button @click="resetTeacherRecordFilters">重置工作台筛选</el-button>
        </div>

        <div v-if="isTeacher && selectedCourseId && recordTotal > 0" class="panel-toolbar">
          <div class="panel-toolbar-text">教师工作台默认展示待批改，支持按姓名或学号筛选</div>
          <el-pagination
            v-model:current-page="teacherRecordCurrentPage"
            v-model:page-size="teacherRecordPageSize"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :page-sizes="[6, 10, 20]"
            :total="teacherRecordTotal"
            @current-change="handleTeacherRecordCurrentChange"
            @size-change="handleTeacherRecordSizeChange"
          />
        </div>

        <div v-if="recordLoading" class="loading-wrap compact">
          <el-skeleton :rows="6" animated />
        </div>
        <el-empty
          v-else-if="!selectedCourseId"
          description="请先选择课程"
          :image-size="90"
        />
        <el-empty
          v-else-if="recordTotal === 0"
          :description="isTeacher ? '当前筛选条件下暂无练习记录' : '当前课程下暂无练习记录'"
          :image-size="90"
        />

        <div v-else class="record-list">
          <article v-for="record in currentRecords" :key="record.id" class="record-card">
            <div class="record-top">
              <div>
                <h4>{{ record.paperTitle || `试卷 ${record.paperId}` }}</h4>
                <div class="record-meta">
                  <span v-if="isTeacher">{{ record.studentName || '未知学生' }}</span>
                  <span v-if="isTeacher && record.studentNo">学号 {{ record.studentNo }}</span>
                  <span>{{ formatDate(record.submittedAt) }}</span>
                </div>
              </div>

              <div class="record-tags">
                <el-tag :type="practiceStatusTagType(record.status)">
                  {{ practiceStatusLabel(record.status) }}
                </el-tag>
                <el-tag effect="plain" :type="diagnosisStatusTagType(record.diagnosisStatus)">
                  {{ diagnosisStatusLabel(record.diagnosisStatus) }}
                </el-tag>
              </div>
            </div>

            <div class="record-summary">
              <div class="summary-item">
                <span>客观题得分</span>
                <strong>{{ record.objectiveScore || 0 }}</strong>
              </div>
              <div class="summary-item">
                <span>总分</span>
                <strong>{{ record.totalScore ?? 0 }}</strong>
              </div>
              <div class="summary-item">
                <span>题量</span>
                <strong>{{ record.questionCount || 0 }}</strong>
              </div>
              <div class="summary-item">
                <span>时长</span>
                <strong>{{ record.durationMinutes || '--' }} 分钟</strong>
              </div>
            </div>

            <div class="card-actions">
              <el-button link type="primary" @click="openRecordDrawer(record)">
                {{ isTeacher && record.status === 'PENDING_REVIEW' ? '进入批改' : '查看详情' }}
              </el-button>
              <el-button
                v-if="isTeacher && record.status === 'GRADED'"
                link
                type="success"
                :loading="diagnosingRecordId === record.id"
                @click="generateDiagnosisForRecord(record)"
              >
                {{ record.diagnosisStatus === 'DONE' ? '重新诊断' : 'AI 诊断' }}
              </el-button>
            </div>
          </article>
        </div>
      </article>
    </section>

    <el-dialog
      v-model="paperDialogVisible"
      :title="paperForm.id ? '编辑试卷' : '新建试卷'"
      width="1080px"
      class="paper-editor-dialog"
      destroy-on-close
    >
      <div class="dialog-headline">
        <p>按课程从题库勾选题目组卷，保存时会自动生成题目快照。</p>
        <el-tag effect="plain" type="info">已选 {{ selectedPaperQuestionIds.length }} 题</el-tag>
      </div>

      <div class="form-grid">
        <div class="field-block">
          <span class="field-label">所属课程</span>
          <el-select v-model="paperForm.courseId" disabled>
            <el-option
              v-for="course in courseOptions"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
        </div>

        <div class="field-block">
          <span class="field-label">试卷标题</span>
          <el-input v-model="paperForm.title" maxlength="80" placeholder="输入试卷标题" />
        </div>

        <div class="field-block">
          <span class="field-label">答题时长</span>
          <el-input-number
            v-model="paperForm.durationMinutes"
            :min="1"
            :max="300"
            controls-position="right"
          />
          <span class="field-tip">分钟，学生开始作答后立即开始倒计时。</span>
        </div>
      </div>

      <div class="selector-summary">
        <span>可选题目 {{ paperQuestionPool.length }} 道</span>
        <span>已选总分 {{ selectedPaperTotalScore }}</span>
      </div>

      <div v-if="paperQuestionLoading" class="loading-wrap compact">
        <el-skeleton :rows="8" animated />
      </div>
      <el-empty
        v-else-if="paperQuestionPool.length === 0"
        description="当前课程下暂无可组卷题目"
        :image-size="90"
      />

      <div v-else class="selector-list">
        <article
          v-for="question in paperQuestionPool"
          :key="question.id"
          class="selector-card"
          :class="{ selected: isQuestionSelected(question.id) }"
        >
          <div class="selector-card-top">
            <div class="selector-left">
              <el-checkbox
                :model-value="isQuestionSelected(question.id)"
                @change="toggleQuestionSelection(question, $event)"
              />
              <div>
                <div class="selector-tags">
                  <el-tag size="small" :type="typeTagType(question.type)">
                    {{ typeLabel(question.type) }}
                  </el-tag>
                  <el-tag v-if="question.legacySnapshot" size="small" effect="plain" type="warning">
                    历史题目
                  </el-tag>
                </div>
                <div class="selector-stem">{{ question.stem }}</div>
              </div>
            </div>

            <div class="selector-score">
              <span>分值</span>
              <el-input-number
                :model-value="paperQuestionScoreMap[question.id] || 5"
                :min="1"
                :max="100"
                controls-position="right"
                @update:model-value="setQuestionScore(question.id, $event)"
              />
            </div>
          </div>

          <div v-if="renderQuestionContentLines(question).length" class="selector-detail">
            <div class="detail-title">题目内容</div>
            <ul class="line-list">
              <li v-for="line in renderQuestionContentLines(question)" :key="line">
                {{ line }}
              </li>
            </ul>
          </div>
        </article>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="paperDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="paperSubmitting" @click="submitPaperDialog">
            保存试卷
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="paperPreviewVisible"
      title="试卷预览"
      width="960px"
      class="paper-preview-dialog"
      destroy-on-close
    >
      <template v-if="paperPreviewData">
        <div class="paper-preview-body">
        <div class="preview-summary">
          <div class="summary-item">
            <span>标题</span>
            <strong>{{ paperPreviewData.paper?.title }}</strong>
          </div>
          <div class="summary-item">
            <span>状态</span>
            <strong>{{ paperStatusLabel(paperPreviewData.paper?.status) }}</strong>
          </div>
          <div class="summary-item">
            <span>题量</span>
            <strong>{{ paperPreviewData.questions?.length || 0 }}</strong>
          </div>
          <div class="summary-item">
            <span>时长</span>
            <strong>{{ paperPreviewData.paper?.durationMinutes || '--' }} 分钟</strong>
          </div>
        </div>

        <div class="question-preview-list">
          <article
            v-for="question in paperPreviewData.questions || []"
            :key="question.id"
            class="question-preview-card"
          >
            <div class="question-header">
              <div class="question-tags">
                <el-tag size="small" type="info">第 {{ question.sortNo }} 题</el-tag>
                <el-tag size="small" :type="typeTagType(getQuestionType(question))">
                  {{ typeLabel(getQuestionType(question)) }}
                </el-tag>
                <el-tag size="small" effect="plain">分值 {{ question.score }}</el-tag>
              </div>
            </div>

            <div class="question-stem">{{ getQuestionStem(question) }}</div>

            <div v-if="renderQuestionContentLines(question).length" class="detail-block">
              <div class="detail-title">题目内容</div>
              <ul class="line-list">
                <li v-for="line in renderQuestionContentLines(question)" :key="line">
                  {{ line }}
                </li>
              </ul>
            </div>

            <div class="detail-block">
              <div class="detail-title">参考答案</div>
              <div class="answer-box">{{ renderReferenceAnswer(question) }}</div>
            </div>
          </article>
        </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="practiceDialogVisible"
      width="1120px"
      top="4vh"
      :close-on-click-modal="false"
      :destroy-on-close="false"
      :before-close="handlePracticeDialogClose"
    >
      <template #header>
        <div class="practice-dialog-header">
          <div>
            <h3>{{ activePracticePaper?.paper?.title || '试卷作答' }}</h3>
            <p>
              共 {{ activePracticeQuestions.length }} 题，已作答 {{ answeredQuestionCount }} 题，
              总分 {{ activePracticePaper?.paper?.totalScore || 0 }}
            </p>
          </div>

          <div class="practice-header-actions">
            <div
              class="timer-chip"
              :class="{ warning: remainingSeconds <= 300 }"
            >
              <el-icon><Timer /></el-icon>
              <span>{{ formatCountdown(remainingSeconds) }}</span>
            </div>
            <el-button :loading="draftSaving" @click="saveDraftManually">
              保存草稿
            </el-button>
            <el-button
              type="primary"
              :loading="practiceSubmitting"
              @click="submitPracticeConfirm"
            >
              提交试卷
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="activePracticePaper" class="practice-layout">
        <aside class="practice-side">
          <div class="practice-side-card">
            <div class="practice-side-title">答题进度</div>
            <div class="progress-row">
              <span>完成度</span>
              <strong>{{ answeredQuestionCount }}/{{ activePracticeQuestions.length }}</strong>
            </div>
            <el-progress
              :percentage="practiceProgress"
              :stroke-width="10"
              :show-text="false"
            />
          </div>

          <div class="practice-side-card">
            <div class="practice-side-title">题号导航</div>
            <div class="question-nav">
              <button
                v-for="question in activePracticeQuestions"
                :key="question.id"
                type="button"
                class="question-nav-btn"
                :class="{ answered: isQuestionAnswered(question), active: currentPracticeFocusId === question.id }"
                @click="scrollToPracticeQuestion(question.id)"
              >
                {{ question.sortNo }}
              </button>
            </div>
          </div>
        </aside>

        <div class="practice-question-list">
          <article
            v-for="question in activePracticeQuestions"
            :id="`practice-question-${question.id}`"
            :key="question.id"
            class="practice-question-card"
          >
            <div class="question-header">
              <div class="question-tags">
                <el-tag size="small" type="info">第 {{ question.sortNo }} 题</el-tag>
                <el-tag size="small" :type="typeTagType(getQuestionType(question))">
                  {{ typeLabel(getQuestionType(question)) }}
                </el-tag>
                <el-tag size="small" effect="plain">分值 {{ question.score }}</el-tag>
              </div>
            </div>

            <div class="question-stem">{{ getQuestionStem(question) }}</div>

            <div
              v-if="renderQuestionContentLines(question).length"
              class="question-content-block"
            >
              <div class="detail-title">题目内容</div>
              <ul class="line-list">
                <li v-for="line in renderQuestionContentLines(question)" :key="line">
                  {{ line }}
                </li>
              </ul>
            </div>

            <div class="answer-editor">
              <template v-if="getQuestionType(question) === 'SINGLE' || getQuestionType(question) === 'JUDGE'">
                <el-radio-group
                  :model-value="getSingleSelectedKey(question)"
                  class="vertical-group"
                  @change="updateSingleAnswer(question, $event)"
                >
                  <el-radio
                    v-for="option in getQuestionOptions(question)"
                    :key="option.key"
                    :label="option.key"
                  >
                    {{ option.key }}. {{ option.text }}
                  </el-radio>
                </el-radio-group>
              </template>

              <template v-else-if="getQuestionType(question) === 'MULTI'">
                <el-checkbox-group
                  :model-value="getMultiSelectedKeys(question)"
                  class="vertical-group"
                  @change="updateMultiAnswer(question, $event)"
                >
                  <el-checkbox
                    v-for="option in getQuestionOptions(question)"
                    :key="option.key"
                    :label="option.key"
                  >
                    {{ option.key }}. {{ option.text }}
                  </el-checkbox>
                </el-checkbox-group>
              </template>

              <template v-else-if="getQuestionType(question) === 'BLANK'">
                <div class="blank-answer-list">
                  <div
                    v-for="blank in getQuestionOptions(question)"
                    :key="blank.key"
                    class="blank-answer-item"
                  >
                    <span class="blank-label">{{ blank.text }}</span>
                    <el-input
                      :model-value="getBlankAnswer(question, blank.key)"
                      placeholder="输入答案"
                      @focus="currentPracticeFocusId = question.id"
                      @update:model-value="updateBlankAnswer(question, blank.key, $event)"
                    />
                  </div>
                </div>
              </template>

              <template v-else>
                <el-input
                  type="textarea"
                  :rows="5"
                  :model-value="getShortAnswer(question)"
                  placeholder="请输入作答内容"
                  @focus="currentPracticeFocusId = question.id"
                  @update:model-value="updateShortAnswer(question, $event)"
                />
              </template>
            </div>
          </article>
        </div>
      </div>
    </el-dialog>

    <el-drawer
      v-model="recordDrawerVisible"
      :title="recordDrawerTitle"
      size="70%"
      destroy-on-close
    >
      <template v-if="recordDrawerData">
        <div class="record-drawer-body">
          <div class="preview-summary">
            <div class="summary-item">
              <span>试卷</span>
              <strong>{{ recordDrawerData.record?.paperTitle }}</strong>
            </div>
            <div class="summary-item">
              <span>状态</span>
              <strong>{{ practiceStatusLabel(recordDrawerData.record?.status) }}</strong>
            </div>
            <div class="summary-item">
              <span>总分</span>
              <strong>{{ recordDrawerData.record?.totalScore ?? 0 }}</strong>
            </div>
            <div class="summary-item">
              <span>提交时间</span>
              <strong>{{ formatDate(recordDrawerData.record?.submittedAt) }}</strong>
            </div>
          </div>

          <div
            v-if="recordDrawerData.record?.diagnosisText"
            class="diagnosis-panel"
          >
            <div class="detail-title">AI 学习建议</div>
            <p>{{ recordDrawerData.record.diagnosisText }}</p>
          </div>

<div
            v-if="parsedDiagnosis"
            class="diagnosis-panel"
          >
            <div class="detail-title">AI 诊断详情</div>
            
            <div class="ai-analysis-content">
              <div v-if="parsedDiagnosis.strengths?.length" class="diagnosis-section">
                <h4><el-tag type="success" size="small" effect="light">掌握良好</el-tag> 学习优势</h4>
                <ul>
                  <li v-for="(item, index) in parsedDiagnosis.strengths" :key="'strength-' + index">
                    {{ item }}
                  </li>
                </ul>
              </div>

              <div v-if="parsedDiagnosis.weakKnowledgePoints?.length" class="diagnosis-section">
                <h4><el-tag type="danger" size="small" effect="light">待提升</el-tag> 薄弱知识点</h4>
                <ul>
                  <li v-for="(item, index) in parsedDiagnosis.weakKnowledgePoints" :key="'weak-' + index">
                    {{ item }}
                  </li>
                </ul>
              </div>

              <div v-if="parsedDiagnosis.nextActions?.length" class="diagnosis-section">
                <h4><el-tag type="primary" size="small" effect="light">学习计划</el-tag> 下一步建议</h4>
                <ul>
                  <li v-for="(item, index) in parsedDiagnosis.nextActions" :key="'action-' + index">
                    {{ item }}
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <div class="record-answer-list">
            <article
              v-for="answer in recordDrawerData.answers || []"
              :key="answer.id"
              class="record-answer-card"
            >
              <div class="question-header">
                <div class="question-tags">
                  <el-tag size="small" type="info">第 {{ answer.sortNo }} 题</el-tag>
                  <el-tag size="small" :type="typeTagType(answer.snapshot?.type)">
                    {{ typeLabel(answer.snapshot?.type) }}
                  </el-tag>
                  <el-tag size="small" effect="plain">满分 {{ answer.fullScore }}</el-tag>
                  <el-tag
                    v-if="answer.score !== null && answer.score !== undefined"
                    size="small"
                    :type="answer.score >= (answer.fullScore || 0) ? 'success' : 'warning'"
                  >
                    得分 {{ answer.score }}
                  </el-tag>
                </div>
              </div>

              <div class="question-stem">{{ answer.snapshot?.stem }}</div>

              <div v-if="renderQuestionContentLines(answer).length" class="detail-block">
                <div class="detail-title">题目内容</div>
                <ul class="line-list">
                  <li v-for="line in renderQuestionContentLines(answer)" :key="line">
                    {{ line }}
                  </li>
                </ul>
              </div>

              <div class="detail-block">
                <div class="detail-title">学生答案</div>
                <div class="answer-box">{{ renderStudentAnswer(answer) }}</div>
              </div>

              <div v-if="canShowReference(answer)" class="detail-block">
                <div class="detail-title">参考答案</div>
                <div class="answer-box">{{ renderReferenceAnswer(answer) }}</div>
              </div>

              <div v-if="getQuestionAnalysis(answer)" class="detail-block">
                <div class="detail-title">题目解析</div>
                <div class="analysis-box">{{ getQuestionAnalysis(answer) }}</div>
              </div>

<div v-if="answer.aiFeedback" class="detail-block">
                <div class="detail-title">单题 AI 反馈</div>
                <div class="analysis-box custom-ai-feedback">
                  <template v-for="feedbackObj in [parseJson(answer.aiFeedback, {})]" :key="feedbackObj.questionId || 'feedback'">
                    
                    <div v-if="feedbackObj.summary" class="feedback-section">
                      <span class="feedback-label">综合评价：</span>
                      <span>{{ feedbackObj.summary }}</span>
                    </div>
                    
                    <div v-if="feedbackObj.improvement" class="feedback-section">
                      <span class="feedback-label">提升建议：</span>
                      <span>{{ feedbackObj.improvement }}</span>
                    </div>

                    <div v-if="feedbackObj.knowledgePoints?.length" class="feedback-section kp-section">
                      <span class="feedback-label">知识点分析：</span>
                      <ul class="kp-list">
                        <li v-for="(kp, index) in feedbackObj.knowledgePoints" :key="index" class="kp-item">
                          <div class="kp-header">
                            <el-tag size="small" type="info">{{ kp.name }}</el-tag>
                            <el-tag 
                              size="small" 
                              :type="kp.mastery === '优秀' ? 'success' : (kp.mastery === '良好' ? 'primary' : 'warning')" 
                              effect="plain"
                            >
                              掌握度: {{ kp.mastery }}
                            </el-tag>
                          </div>
                          <div class="kp-detail">
                            <div><strong>表现：</strong>{{ kp.evidence }}</div>
                            <div><strong>建议：</strong>{{ kp.advice }}</div>
                          </div>
                        </li>
                      </ul>
                    </div>
                  </template>
                </div>
              </div>

              <div v-if="isReviewingSubjective(answer)" class="review-score-box">
                <span>教师评分</span>
                <el-input-number
                  v-model="reviewScoreMap[answer.id]"
                  :min="0"
                  :max="answer.fullScore || 0"
                  controls-position="right"
                />
              </div>
            </article>
          </div>
        </div>
      </template>

      <template #footer>
        <div class="drawer-footer">
          <el-button
            v-if="canGenerateDiagnosis"
            type="success"
            :loading="diagnosingRecordId === recordDrawerData?.record?.id"
            @click="generateDiagnosisForDrawer"
          >
            {{ recordDrawerData?.record?.diagnosisStatus === 'DONE' ? '重新生成 AI 诊断' : '生成 AI 诊断' }}
          </el-button>
          <el-button
            v-if="canSubmitReview"
            type="primary"
            :loading="reviewSubmitting"
            @click="submitReviewScores"
          >
            提交批改
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Reading,
  RefreshRight,
  Timer,
} from '@element-plus/icons-vue'
import { getMyCourseList } from '@/api/course'
import { getQuestionList } from '@/api/question'
import {
  closePaper,
  createPaper,
  deletePaper,
  getPaperDetail,
  getPaperPage,
  publishPaper,
  updatePaper,
} from '@/api/paper'
import {
  generatePracticeDiagnosis,
  getPracticeDetail,
  getStudentPracticeList,
  getTeacherPracticePage,
  reviewPractice,
  submitPractice,
} from '@/api/practice'
import { getUserInfo } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

const OBJECTIVE_TYPES = ['SINGLE', 'MULTI', 'JUDGE']
const SUBJECTIVE_TYPES = ['BLANK', 'SHORT']
const QUESTION_TYPE_OPTIONS = [
  { label: '单选题', value: 'SINGLE' },
  { label: '多选题', value: 'MULTI' },
  { label: '判断题', value: 'JUDGE' },
  { label: '填空题', value: 'BLANK' },
  { label: '简答题', value: 'SHORT' },
]

const initialLoading = ref(true)
const paperLoading = ref(false)
const recordLoading = ref(false)
const paperQuestionLoading = ref(false)
const paperSubmitting = ref(false)
const practiceSubmitting = ref(false)
const draftSaving = ref(false)
const reviewSubmitting = ref(false)
const diagnosingRecordId = ref(null)

const courseOptions = ref([])
const selectedCourseId = ref(null)
const paperKeyword = ref('')
const paperCurrentPage = ref(1)
const paperPageSize = ref(6)
const paperTotal = ref(0)
const teacherRecordKeyword = ref('')
const teacherRecordStatus = ref('PENDING_REVIEW')
const teacherRecordCurrentPage = ref(1)
const teacherRecordPageSize = ref(6)
const teacherRecordTotal = ref(0)
const teacherPendingCount = ref(0)
const teacherDiagnosisDoneCount = ref(0)

const paperList = ref([])
const studentRecordList = ref([])
const teacherRecordList = ref([])

const paperDialogVisible = ref(false)
const paperPreviewVisible = ref(false)
const practiceDialogVisible = ref(false)
const recordDrawerVisible = ref(false)

const paperQuestionPool = ref([])
const selectedPaperQuestionIds = ref([])
const paperPreviewData = ref(null)
const recordDrawerData = ref(null)
const currentPracticeFocusId = ref(null)

const activePracticePaper = ref(null)
const activePracticeAnswers = ref({})
const activePracticeStartedAt = ref('')
const remainingSeconds = ref(0)

const practiceTimer = ref(null)
const draftPersistTimer = ref(null)
const handledRouteIntentKey = ref('')
let paperFilterTimer = null
let teacherRecordFilterTimer = null

const paperForm = reactive({
  id: null,
  courseId: null,
  title: '',
  durationMinutes: 30,
})

const paperQuestionScoreMap = reactive({})
const reviewScoreMap = reactive({})

const userInfo = computed(() => getUserInfo())

const roleCode = computed(() => String(userInfo.value.role || '').toUpperCase())
const isTeacher = computed(() => roleCode.value === 'TEACHER' || roleCode.value === 'ADMIN')
const currentRecords = computed(() => (isTeacher.value ? teacherRecordList.value : studentRecordList.value))
const recordTotal = computed(() => (isTeacher.value ? teacherRecordTotal.value : studentRecordList.value.length))
const selectedCourseName = computed(
  () => courseOptions.value.find((course) => course.id === selectedCourseId.value)?.courseName || ''
)

const pendingRecordCount = computed(() => {
  if (isTeacher.value) {
    return teacherPendingCount.value
  }
  return studentRecordList.value.filter((item) => item.status === 'PENDING_REVIEW').length
})

const highlightedRecordCount = computed(() => {
  if (isTeacher.value) {
    return teacherDiagnosisDoneCount.value
  }
  return studentRecordList.value.filter((item) => item.status === 'GRADED').length
})

const selectedPaperTotalScore = computed(() =>
  selectedPaperQuestionIds.value.reduce(
    (sum, questionId) => sum + (Number(paperQuestionScoreMap[questionId]) || 0),
    0
  )
)

const activePracticeQuestions = computed(() => activePracticePaper.value?.questions || [])

const answeredQuestionCount = computed(() =>
  activePracticeQuestions.value.filter((question) => isQuestionAnswered(question)).length
)

const practiceProgress = computed(() => {
  if (!activePracticeQuestions.value.length) return 0
  return Math.round((answeredQuestionCount.value / activePracticeQuestions.value.length) * 100)
})

const recordDrawerTitle = computed(() => {
  if (!recordDrawerData.value?.record) return '练习详情'
  if (isTeacher.value && recordDrawerData.value.record.status === 'PENDING_REVIEW') {
    return '练习批改'
  }
  return '练习详情'
})

const canSubmitReview = computed(
  () => !!recordDrawerData.value && isTeacher.value && recordDrawerData.value.record?.status === 'PENDING_REVIEW'
)

const canGenerateDiagnosis = computed(
  () => !!recordDrawerData.value && isTeacher.value && recordDrawerData.value.record?.status === 'GRADED'
)

// 新增或替换：将原本的格式化 JSON 字符串改为返回解析后的对象
const parsedDiagnosis = computed(() => {
  const jsonText = recordDrawerData.value?.record?.diagnosisJson
  return parseJson(jsonText, null)
})

function resetFilters() {
  const shouldReload = !paperKeyword.value && paperCurrentPage.value === 1
  paperKeyword.value = ''
  paperCurrentPage.value = 1
  if (shouldReload && selectedCourseId.value) {
    void loadPapers()
  }
}

function resetTeacherRecordFilters() {
  const shouldReload =
    !teacherRecordKeyword.value &&
    teacherRecordStatus.value === 'PENDING_REVIEW' &&
    teacherRecordCurrentPage.value === 1
  teacherRecordKeyword.value = ''
  teacherRecordStatus.value = 'PENDING_REVIEW'
  teacherRecordCurrentPage.value = 1
  if (selectedCourseId.value && isTeacher.value && shouldReload) {
    void loadRecords()
  }
}

function parseJson(text, fallback = {}) {
  if (!text) return fallback
  if (typeof text === 'object') return text
  try {
    return JSON.parse(text)
  } catch {
    return fallback
  }
}

function typeLabel(type) {
  return QUESTION_TYPE_OPTIONS.find((item) => item.value === type)?.label || type || '未知题型'
}

function typeTagType(type) {
  return {
    SINGLE: 'primary',
    MULTI: 'success',
    JUDGE: 'warning',
    BLANK: 'info',
    SHORT: 'danger',
  }[type] || 'info'
}

function paperStatusLabel(status) {
  return {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    CLOSED: '已关闭',
  }[status] || status || '未知状态'
}

function paperStatusTagType(status) {
  return {
    DRAFT: 'info',
    PUBLISHED: 'success',
    CLOSED: 'warning',
  }[status] || 'info'
}

function practiceStatusLabel(status) {
  return {
    PENDING_REVIEW: '待批改',
    GRADED: '已批改',
  }[status] || status || '未知状态'
}

function practiceStatusTagType(status) {
  return {
    PENDING_REVIEW: 'warning',
    GRADED: 'success',
  }[status] || 'info'
}

function diagnosisStatusLabel(status) {
  return {
    PENDING: '待 AI 诊断',
    DONE: '已 AI 诊断',
  }[status] || (status ? status : '未诊断')
}

function diagnosisStatusTagType(status) {
  return {
    PENDING: 'warning',
    DONE: 'success',
  }[status] || 'info'
}

function formatDate(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatLocalDateTime(date = new Date()) {
  const pad = (value) => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function formatCountdown(seconds) {
  const safeSeconds = Math.max(0, Number(seconds) || 0)
  const hour = Math.floor(safeSeconds / 3600)
  const minute = Math.floor((safeSeconds % 3600) / 60)
  const second = safeSeconds % 60
  const pad = (value) => String(value).padStart(2, '0')
  return hour > 0
    ? `${pad(hour)}:${pad(minute)}:${pad(second)}`
    : `${pad(minute)}:${pad(second)}`
}

function numberFromRouteValue(value) {
  if (value === null || value === undefined || value === '') return null
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

function buildRouteIntentKey() {
  return [
    String(route.query.menu || ''),
    String(route.query.action || ''),
    String(route.query.courseId || ''),
    String(route.query.paperId || ''),
  ].join('|')
}

function getQuestionType(question) {
  return question?.snapshot?.type || question?.type || ''
}

function getQuestionStem(question) {
  return question?.snapshot?.stem || question?.stem || ''
}

function getQuestionContent(question) {
  return question?.snapshot?.content || question?.contentParsed || question?.content || {}
}

function getQuestionReference(question) {
  console.log(question)
  return question?.snapshot?.answer || question?.answerParsed || question?.answer || {}
}

function getQuestionAnalysis(question) {
  return question?.snapshot?.analysisText || question?.analysisText || ''
}

function getQuestionOptions(question) {
  return Object.entries(getQuestionContent(question) || {}).map(([key, value]) => ({
    key,
    text: value,
  }))
}

function buildRecordQuestionLookup(questions = []) {
  const byPaperQuestionId = new Map()
  const byQuestionId = new Map()
  questions.forEach((question) => {
    if (question?.id !== null && question?.id !== undefined) {
      byPaperQuestionId.set(question.id, question)
    }
    if (
      question?.questionId !== null &&
      question?.questionId !== undefined &&
      !byQuestionId.has(question.questionId)
    ) {
      byQuestionId.set(question.questionId, question)
    }
  })
  return { byPaperQuestionId, byQuestionId }
}

function mergeAnswerSnapshot(answer, question) {
  const snapshot = answer?.snapshot || {}
  if (!question) return snapshot
  return {
    ...snapshot,
    type: snapshot.type ?? question.snapshot?.type ?? question.type ?? '',
    stem: snapshot.stem ?? question.snapshot?.stem ?? question.stem ?? '',
    content: snapshot.content ?? question.snapshot?.content ?? question.contentParsed ?? question.content ?? {},
    answer: snapshot.answer ?? question.snapshot?.answer ?? question.answerParsed ?? question.answer ?? {},
    analysisText:
      snapshot.analysisText ??
      question.snapshot?.analysisText ??
      question.analysisText ??
      '',
  }
}

async function enrichRecordDrawerDetail(detail) {
  if (!detail?.record?.paperId) return detail
  const paperDetail = await getPaperDetail(detail.record.paperId)
  const { byPaperQuestionId, byQuestionId } = buildRecordQuestionLookup(paperDetail?.questions || [])
  return {
    ...detail,
    answers: (detail.answers || []).map((answer) => {
      const question =
        byPaperQuestionId.get(answer.paperQuestionId) || byQuestionId.get(answer.questionId)
      return {
        ...answer,
        sortNo: answer.sortNo ?? question?.sortNo ?? null,
        fullScore: answer.fullScore ?? question?.score ?? null,
        snapshot: mergeAnswerSnapshot(answer, question),
      }
    }),
  }
}

function renderQuestionContentLines(question) {
  const type = getQuestionType(question)
  const content = getQuestionContent(question)
  if (OBJECTIVE_TYPES.includes(type)) {
    return Object.entries(content).map(([key, value]) => `${key}. ${value}`)
  }
  if (type === 'BLANK') {
    return Object.entries(content).map(([key, value]) => `空 ${key}: ${value}`)
  }
  if (type === 'SHORT') {
    return content.guide ? [`作答说明: ${content.guide}`] : []
  }
  return []
}

function renderReferenceAnswer(question) {
  const type = getQuestionType(question)
  const answer = getQuestionReference(question)
  if (type === 'SHORT') return answer.text || '未设置'
  return Object.entries(answer).map(([key, value]) => `${key}: ${value}`).join('\n') || '未设置'
}

function renderStudentAnswer(answer) {
  const type = answer?.snapshot?.type || answer?.type; // 兼容快照和原始题型 
  const studentAnswer = answer?.studentAnswer || {};
  
  if (!studentAnswer || Object.keys(studentAnswer).length === 0) return '未作答';

  if (type === 'SHORT') {
    return studentAnswer.text || '未作答'; 
  }
  
  if (type === 'BLANK') {
    const content = answer?.snapshot?.content || {}; 
    return Object.entries(content)
      .map(([key, prompt]) => `${prompt}: ${studentAnswer[key] || '未填写'}`)
      .join('\n'); 
  }

  // 针对单选、多选、判断题：将 key (如 A, B) 映射为具体文本内容
  const content = answer?.snapshot?.content || {};
  return Object.entries(studentAnswer)
    .map(([key, value]) => `${key}: ${content[key] || value}`)
    .join('\n');
}



function normalizeQuestionItem(item) {
  return {
    ...item,
    contentParsed: parseJson(item.contentJson, {}),
    answerParsed: parseJson(item.answerJson, {}),
    knowledgePointNames: Array.isArray(item.knowledgePointNames)
      ? item.knowledgePointNames
      : parseJson(item.knowledgePointNames, []),
  }
}

function ensureLegacyQuestions(detailQuestions = []) {
  const existingIds = new Set(paperQuestionPool.value.map((item) => item.id))
  const extras = detailQuestions
    .filter((item) => !existingIds.has(item.questionId))
    .map((item) => ({
      id: item.questionId,
      stem: item.snapshot?.stem || `历史题目 ${item.questionId}`,
      type: item.snapshot?.type || '',
      contentParsed: item.snapshot?.content || {},
      answerParsed: item.snapshot?.answer || {},
      analysisText: item.snapshot?.analysisText || '',
      legacySnapshot: true,
    }))
  if (extras.length) {
    paperQuestionPool.value = [...extras, ...paperQuestionPool.value]
  }
}

function clearObject(target) {
  Object.keys(target).forEach((key) => {
    delete target[key]
  })
}

function getDraftStorageKey(paperId) {
  const uid = userInfo.value.id || userInfo.value.userId || userInfo.value.username || 'anonymous'
  return `practice-draft:${uid}:${paperId}`
}

function loadDraft(paperId) {
  if (!paperId) return null
  return parseJson(localStorage.getItem(getDraftStorageKey(paperId)), null)
}

function hasDraft(paperId) {
  return !!loadDraft(paperId)
}

function clearDraft(paperId) {
  if (!paperId) return
  localStorage.removeItem(getDraftStorageKey(paperId))
}

function buildEmptyAnswer(type) {
  return type === 'SHORT' ? { text: '' } : {}
}

function normalizeStoredAnswer(question, rawAnswer) {
  const type = getQuestionType(question)
  if (!rawAnswer || typeof rawAnswer !== 'object') {
    return buildEmptyAnswer(type)
  }
  if (type === 'SHORT') {
    return { text: rawAnswer.text || '' }
  }
  return { ...rawAnswer }
}

function buildInitialAnswerMap(questions = [], rawAnswers = {}) {
  return questions.reduce((result, question) => {
    result[question.id] = normalizeStoredAnswer(question, rawAnswers[question.id])
    return result
  }, {})
}

function parseDateValue(value) {
  if (!value) return Date.now()
  const parsed = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(parsed.getTime()) ? Date.now() : parsed.getTime()
}

function computeRemainingSeconds(durationMinutes, startedAt) {
  const totalSeconds = Math.max(0, Number(durationMinutes || 0) * 60)
  const elapsedSeconds = Math.max(0, Math.floor((Date.now() - parseDateValue(startedAt)) / 1000))
  return Math.max(0, totalSeconds - elapsedSeconds)
}

function clearPracticeTimer() {
  if (practiceTimer.value) {
    window.clearInterval(practiceTimer.value)
    practiceTimer.value = null
  }
}

async function loadCourses() {
  initialLoading.value = true
  try {
    const response = await getMyCourseList()
    courseOptions.value = Array.isArray(response) ? response : []
    if (!courseOptions.value.length) {
      selectedCourseId.value = null
      paperList.value = []
      paperTotal.value = 0
      studentRecordList.value = []
      teacherRecordList.value = []
      teacherRecordTotal.value = 0
      teacherPendingCount.value = 0
      teacherDiagnosisDoneCount.value = 0
      return
    }
    const routeCourseId = numberFromRouteValue(route.query.courseId)
    const targetCourseId = routeCourseId && courseOptions.value.some((course) => course.id === routeCourseId)
      ? routeCourseId
      : selectedCourseId.value
    const exists = courseOptions.value.some((course) => course.id === targetCourseId)
    if (!exists) {
      selectedCourseId.value = courseOptions.value[0].id
      return
    }
    const previousCourseId = selectedCourseId.value
    selectedCourseId.value = targetCourseId
    if (previousCourseId === targetCourseId) {
      await refreshCourseData()
    }
  } finally {
    initialLoading.value = false
  }
}

async function loadPapers() {
  if (!selectedCourseId.value) {
    paperList.value = []
    paperTotal.value = 0
    return
  }
  paperLoading.value = true
  try {
    const data = await getPaperPage({
      courseId: selectedCourseId.value,
      current: paperCurrentPage.value,
      size: paperPageSize.value,
      keyword: paperKeyword.value.trim() || undefined,
    })
    paperList.value = Array.isArray(data?.records) ? data.records : []
    paperTotal.value = Number(data?.total || 0)
    paperCurrentPage.value = Number(data?.current || paperCurrentPage.value)
  } finally {
    paperLoading.value = false
  }
}

async function maybeHandleRouteIntent() {
  const routeCourseId = numberFromRouteValue(route.query.courseId)
  const routePaperId = numberFromRouteValue(route.query.paperId)
  const action = String(route.query.action || '').toLowerCase()
  const menu = String(route.query.menu || '').toLowerCase()
  const intentKey = buildRouteIntentKey()

  if (handledRouteIntentKey.value === intentKey) return
  if (menu !== 'practice' || action !== 'answer' || !routeCourseId || !routePaperId) return
  if (!selectedCourseId.value || Number(selectedCourseId.value) !== routeCourseId) return
  if (isTeacher.value) return

  let paper = paperList.value.find((item) => Number(item.id) === routePaperId)
  if (!paper) {
    try {
      paper = (await getPaperDetail(routePaperId))?.paper || null
    } catch {
      paper = null
    }
  }
  if (!paper) return

  handledRouteIntentKey.value = intentKey

  if (paper.status !== 'PUBLISHED') {
    await router.replace({
      name: 'Home',
      query: {
        menu: 'practice',
        courseId: String(routeCourseId),
      },
    })
    ElMessage.warning('当前试卷未处于可作答状态')
    return
  }

  await openPracticeDialog(paper)
  await router.replace({
    name: 'Home',
    query: {
      menu: 'practice',
      courseId: String(routeCourseId),
    },
  })
}

async function loadRecords() {
  if (!selectedCourseId.value) {
    studentRecordList.value = []
    teacherRecordList.value = []
    teacherRecordTotal.value = 0
    teacherPendingCount.value = 0
    teacherDiagnosisDoneCount.value = 0
    return
  }
  recordLoading.value = true
  try {
    if (isTeacher.value) {
      const data = await getTeacherPracticePage({
        courseId: selectedCourseId.value,
        current: teacherRecordCurrentPage.value,
        size: teacherRecordPageSize.value,
        status: teacherRecordStatus.value || undefined,
        keyword: teacherRecordKeyword.value.trim() || undefined,
      })
      teacherRecordList.value = Array.isArray(data?.records) ? data.records : []
      teacherRecordTotal.value = Number(data?.total || 0)
      teacherRecordCurrentPage.value = Number(data?.current || teacherRecordCurrentPage.value)
      teacherPendingCount.value = Number(data?.pendingCount || 0)
      teacherDiagnosisDoneCount.value = Number(data?.diagnosisDoneCount || 0)
    } else {
      const list = await getStudentPracticeList(selectedCourseId.value)
      studentRecordList.value = Array.isArray(list) ? list : []
      teacherRecordList.value = []
      teacherRecordTotal.value = 0
      teacherPendingCount.value = 0
      teacherDiagnosisDoneCount.value = 0
    }
  } finally {
    recordLoading.value = false
  }
}

async function refreshCourseData() {
  if (!selectedCourseId.value) return
  await Promise.all([loadPapers(), loadRecords()])
  await maybeHandleRouteIntent()
}

function schedulePaperReload() {
  if (!selectedCourseId.value) return
  if (paperFilterTimer) {
    window.clearTimeout(paperFilterTimer)
  }
  paperFilterTimer = window.setTimeout(async () => {
    await loadPapers()
    paperFilterTimer = null
  }, 250)
}

function scheduleTeacherRecordReload() {
  if (!selectedCourseId.value || !isTeacher.value) return
  if (teacherRecordFilterTimer) {
    window.clearTimeout(teacherRecordFilterTimer)
  }
  teacherRecordFilterTimer = window.setTimeout(async () => {
    await loadRecords()
    teacherRecordFilterTimer = null
  }, 250)
}

async function handlePaperCurrentChange(page) {
  paperCurrentPage.value = page
  await loadPapers()
}

async function handlePaperSizeChange(size) {
  paperPageSize.value = size
  paperCurrentPage.value = 1
  await loadPapers()
}

async function handleTeacherRecordCurrentChange(page) {
  teacherRecordCurrentPage.value = page
  await loadRecords()
}

async function handleTeacherRecordSizeChange(size) {
  teacherRecordPageSize.value = size
  teacherRecordCurrentPage.value = 1
  await loadRecords()
}

async function loadPaperQuestionPool(courseId) {
  if (!courseId) {
    paperQuestionPool.value = []
    return
  }
  paperQuestionLoading.value = true
  try {
    const list = await getQuestionList(courseId)
    paperQuestionPool.value = (Array.isArray(list) ? list : [])
      .map(normalizeQuestionItem)
      .filter((item) => item.reviewStatus === 'APPROVED')
  } finally {
    paperQuestionLoading.value = false
  }
}

function resetPaperForm() {
  paperForm.id = null
  paperForm.courseId = selectedCourseId.value
  paperForm.title = ''
  paperForm.durationMinutes = 30
  selectedPaperQuestionIds.value = []
  clearObject(paperQuestionScoreMap)
}

async function openCreatePaperDialog() {
  resetPaperForm()
  await loadPaperQuestionPool(selectedCourseId.value)
  paperDialogVisible.value = true
}

async function openEditPaperDialog(paper) {
  resetPaperForm()
  paperForm.id = paper.id
  paperForm.courseId = paper.courseId
  paperForm.title = paper.title
  paperForm.durationMinutes = paper.durationMinutes || 30
  await loadPaperQuestionPool(paper.courseId)
  const detail = await getPaperDetail(paper.id)
  ensureLegacyQuestions(detail.questions || [])
  selectedPaperQuestionIds.value = (detail.questions || []).map((item) => item.questionId)
  clearObject(paperQuestionScoreMap)
  ;(detail.questions || []).forEach((item) => {
    paperQuestionScoreMap[item.questionId] = item.score || 5
  })
  paperDialogVisible.value = true
}

function isQuestionSelected(questionId) {
  return selectedPaperQuestionIds.value.includes(questionId)
}

function setQuestionScore(questionId, value) {
  paperQuestionScoreMap[questionId] = Math.max(1, Number(value) || 1)
}

function toggleQuestionSelection(question, checked) {
  const nextChecked = !!checked
  if (nextChecked && !isQuestionSelected(question.id)) {
    selectedPaperQuestionIds.value = [...selectedPaperQuestionIds.value, question.id]
    if (!paperQuestionScoreMap[question.id]) {
      paperQuestionScoreMap[question.id] = 5
    }
    return
  }
  if (!nextChecked) {
    selectedPaperQuestionIds.value = selectedPaperQuestionIds.value.filter((id) => id !== question.id)
    delete paperQuestionScoreMap[question.id]
  }
}

async function submitPaperDialog() {
  if (!paperForm.courseId) {
    ElMessage.warning('请先选择课程')
    return
  }
  if (!paperForm.title.trim()) {
    ElMessage.warning('请输入试卷标题')
    return
  }
  if (!paperForm.durationMinutes || paperForm.durationMinutes <= 0) {
    ElMessage.warning('请设置有效的答题时长')
    return
  }
  if (!selectedPaperQuestionIds.value.length) {
    ElMessage.warning('请至少勾选一道题目')
    return
  }

  const questionMap = new Map(paperQuestionPool.value.map((question) => [question.id, question]))
  const questionItems = selectedPaperQuestionIds.value
    .map((questionId, index) => {
      const question = questionMap.get(questionId)
      if (!question) return null
      return {
        questionId: question.id,
        score: Number(paperQuestionScoreMap[question.id]) || 5,
        sortNo: index + 1,
      }
    })
    .filter(Boolean)

  const payload = {
    courseId: paperForm.courseId,
    title: paperForm.title.trim(),
    durationMinutes: Number(paperForm.durationMinutes) || 30,
    paperType: 'PRACTICE',
    generationMode: 'MANUAL',
    questionItems,
  }

  paperSubmitting.value = true
  try {
    if (paperForm.id) {
      await updatePaper({ ...payload, id: paperForm.id })
      ElMessage.success('试卷已更新')
    } else {
      await createPaper(payload)
      ElMessage.success('试卷已创建')
    }
    paperDialogVisible.value = false
    await refreshCourseData()
  } finally {
    paperSubmitting.value = false
  }
}

async function openPaperPreview(paper) {
  paperPreviewData.value = await getPaperDetail(paper.id)
  paperPreviewVisible.value = true
}

async function publishPaperItem(paper) {
  try {
    await ElMessageBox.confirm(`确认发布试卷《${paper.title}》吗？`, '发布试卷', {
      type: 'warning',
      confirmButtonText: '发布',
      cancelButtonText: '取消',
    })
    await publishPaper(paper.id)
    ElMessage.success('试卷已发布')
    await refreshCourseData()
  } catch {}
}

async function closePaperItem(paper) {
  try {
    await ElMessageBox.confirm(`确认关闭试卷《${paper.title}》吗？`, '关闭试卷', {
      type: 'warning',
      confirmButtonText: '关闭',
      cancelButtonText: '取消',
    })
    await closePaper(paper.id)
    ElMessage.success('试卷已关闭')
    await refreshCourseData()
  } catch {}
}

async function removePaper(paper) {
  try {
    await ElMessageBox.confirm(`确认删除试卷《${paper.title}》吗？`, '删除试卷', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deletePaper(paper.id)
    ElMessage.success('试卷已删除')
    await refreshCourseData()
  } catch {}
}

function persistDraft(showMessage = false) {
  if (!activePracticePaper.value?.paper?.id) return
  const payload = {
    paperId: activePracticePaper.value.paper.id,
    courseId: activePracticePaper.value.paper.courseId,
    paperTitle: activePracticePaper.value.paper.title,
    startedAt: activePracticeStartedAt.value,
    answers: activePracticeAnswers.value,
    savedAt: formatLocalDateTime(),
  }
  localStorage.setItem(getDraftStorageKey(activePracticePaper.value.paper.id), JSON.stringify(payload))
  if (showMessage) {
    ElMessage.success('草稿已保存')
  }
}

function scheduleDraftPersist() {
  if (!activePracticePaper.value?.paper?.id) return
  if (draftPersistTimer.value) {
    window.clearTimeout(draftPersistTimer.value)
  }
  draftPersistTimer.value = window.setTimeout(() => {
    persistDraft(false)
    draftPersistTimer.value = null
  }, 400)
}

function resetPracticeSession(options = {}) {
  const paperId = activePracticePaper.value?.paper?.id
  clearPracticeTimer()
  if (draftPersistTimer.value) {
    window.clearTimeout(draftPersistTimer.value)
    draftPersistTimer.value = null
  }
  if (options.clearDraft !== false && paperId) {
    clearDraft(paperId)
  }
  activePracticePaper.value = null
  activePracticeAnswers.value = {}
  activePracticeStartedAt.value = ''
  remainingSeconds.value = 0
  currentPracticeFocusId.value = null
}

function startPracticeTimer() {
  clearPracticeTimer()
  if (!activePracticePaper.value?.paper?.durationMinutes || !activePracticeStartedAt.value) return
  const updateRemaining = () => {
    remainingSeconds.value = computeRemainingSeconds(
      activePracticePaper.value.paper.durationMinutes,
      activePracticeStartedAt.value
    )
    if (remainingSeconds.value <= 0 && activePracticePaper.value && !practiceSubmitting.value) {
      clearPracticeTimer()
      ElMessage.warning('答题时间已到，系统正在自动提交')
      void submitPracticeNow(true)
    }
  }
  updateRemaining()
  practiceTimer.value = window.setInterval(updateRemaining, 1000)
}

function normalizeAnswerForSubmit(question, rawAnswer) {
  const type = getQuestionType(question)
  if (type === 'SHORT') {
    return { text: String(rawAnswer?.text || '').trim() }
  }
  return Object.entries(rawAnswer || {}).reduce((result, [key, value]) => {
    if (value === null || value === undefined) return result
    const text = String(value).trim()
    if (!text) return result
    result[key] = value
    return result
  }, {})
}

function buildSubmitAnswers() {
  return activePracticeQuestions.value.map((question) => ({
    paperQuestionId: question.id,
    questionId: question.questionId,
    answer: normalizeAnswerForSubmit(question, activePracticeAnswers.value[question.id]),
  }))
}

async function openPracticeDialog(paper) {
  const currentPaperId = activePracticePaper.value?.paper?.id
  if (currentPaperId && currentPaperId !== paper.id) {
    ElMessage.warning('当前已有进行中的试卷，请先完成或继续当前试卷')
    return
  }

  const detail = currentPaperId === paper.id && activePracticePaper.value
    ? activePracticePaper.value
    : await getPaperDetail(paper.id)

  const draft = loadDraft(paper.id)
  const startedAt = currentPaperId === paper.id && activePracticeStartedAt.value
    ? activePracticeStartedAt.value
    : draft?.startedAt || formatLocalDateTime()

  activePracticePaper.value = detail
  activePracticeAnswers.value = currentPaperId === paper.id && Object.keys(activePracticeAnswers.value).length
    ? activePracticeAnswers.value
    : buildInitialAnswerMap(detail.questions || [], draft?.answers || {})
  activePracticeStartedAt.value = startedAt
  currentPracticeFocusId.value = detail.questions?.[0]?.id || null

  startPracticeTimer()
  practiceDialogVisible.value = true
}

function resumeActivePractice() {
  if (!activePracticePaper.value?.paper?.id) return
  practiceDialogVisible.value = true
  startPracticeTimer()
}

function handlePracticeDialogClose(done) {
  persistDraft(false)
  practiceDialogVisible.value = false
  done()
}

function isQuestionAnswered(question) {
  const answer = normalizeAnswerForSubmit(question, activePracticeAnswers.value[question.id])
  if (getQuestionType(question) === 'SHORT') {
    return !!answer.text
  }
  return Object.keys(answer).length > 0
}

function getSingleSelectedKey(question) {
  return Object.keys(activePracticeAnswers.value[question.id] || {})[0] || ''
}

function updateSingleAnswer(question, selectedKey) {
  currentPracticeFocusId.value = question.id
  const content = getQuestionContent(question)
  activePracticeAnswers.value = {
    ...activePracticeAnswers.value,
    [question.id]: selectedKey ? { [selectedKey]: content[selectedKey] } : {},
  }
}

function getMultiSelectedKeys(question) {
  return Object.keys(activePracticeAnswers.value[question.id] || {})
}

function updateMultiAnswer(question, selectedKeys) {
  currentPracticeFocusId.value = question.id
  const content = getQuestionContent(question)
  const nextValue = Array.isArray(selectedKeys)
    ? selectedKeys.reduce((result, key) => {
        if (content[key] !== undefined) {
          result[key] = content[key]
        }
        return result
      }, {})
    : {}
  activePracticeAnswers.value = {
    ...activePracticeAnswers.value,
    [question.id]: nextValue,
  }
}

function getBlankAnswer(question, blankKey) {
  return activePracticeAnswers.value[question.id]?.[blankKey] || ''
}

function updateBlankAnswer(question, blankKey, value) {
  currentPracticeFocusId.value = question.id
  activePracticeAnswers.value = {
    ...activePracticeAnswers.value,
    [question.id]: {
      ...(activePracticeAnswers.value[question.id] || {}),
      [blankKey]: value,
    },
  }
}

function getShortAnswer(question) {
  return activePracticeAnswers.value[question.id]?.text || ''
}

function updateShortAnswer(question, value) {
  currentPracticeFocusId.value = question.id
  activePracticeAnswers.value = {
    ...activePracticeAnswers.value,
    [question.id]: { text: value },
  }
}

function scrollToPracticeQuestion(questionId) {
  currentPracticeFocusId.value = questionId
  const element = document.getElementById(`practice-question-${questionId}`)
  element?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function saveDraftManually() {
  draftSaving.value = true
  try {
    persistDraft(true)
  } finally {
    draftSaving.value = false
  }
}

async function submitPracticeConfirm() {
  if (!activePracticePaper.value?.paper?.id) return
  const unansweredCount = activePracticeQuestions.value.length - answeredQuestionCount.value
  try {
    await ElMessageBox.confirm(
      unansweredCount > 0
        ? `还有 ${unansweredCount} 道题未作答，确认现在提交吗？`
        : '确认提交当前试卷吗？',
      '提交试卷',
      {
        type: 'warning',
        confirmButtonText: '提交',
        cancelButtonText: '继续作答',
      }
    )
    await submitPracticeNow(false)
  } catch {}
}

async function submitPracticeNow(autoSubmitted) {
  if (!activePracticePaper.value?.paper?.id || practiceSubmitting.value) return
  practiceSubmitting.value = true
  try {
    const detail = await submitPractice({
      paperId: activePracticePaper.value.paper.id,
      startedAt: activePracticeStartedAt.value || formatLocalDateTime(),
      answers: buildSubmitAnswers(),
    })
    const paperId = activePracticePaper.value.paper.id
    resetPracticeSession({ clearDraft: false })
    clearDraft(paperId)
    practiceDialogVisible.value = false
    recordDrawerData.value = await enrichRecordDrawerDetail(detail)
    recordDrawerVisible.value = true
    ElMessage.success(autoSubmitted ? '试卷已自动提交' : '试卷提交成功')
    await refreshCourseData()
  } finally {
    practiceSubmitting.value = false
  }
}

async function openRecordDrawer(record) {
  const detail = await getPracticeDetail(record.id);
  // 关键：无论老师还是学生，都调用此函数补全题目快照和分值 
  recordDrawerData.value = await enrichRecordDrawerDetail(detail); 
  
  if (isTeacher.value) {
    clearObject(reviewScoreMap);
    (recordDrawerData.value.answers || []).forEach((answer) => {
      reviewScoreMap[answer.id] = answer.score ?? 0; 
    });
  }
  recordDrawerVisible.value = true;
}

function isReviewingSubjective(answer) {
  return canSubmitReview.value && SUBJECTIVE_TYPES.includes(answer?.snapshot?.type)
}

function canShowReference(answer) {
  return !!answer?.snapshot?.answer
}

async function submitReviewScores() {
  if (!recordDrawerData.value?.record?.id) return
  const subjectiveAnswers = (recordDrawerData.value.answers || []).filter((answer) =>
    SUBJECTIVE_TYPES.includes(answer?.snapshot?.type)
  )
  const invalidAnswer = subjectiveAnswers.find((answer) => {
    const score = reviewScoreMap[answer.id]
    return score === null || score === undefined || score < 0 || score > (answer.fullScore || 0)
  })
  if (invalidAnswer) {
    ElMessage.warning('请完整填写主观题评分，且分值不能超过题目满分')
    return
  }

  reviewSubmitting.value = true
  try {
    const detail = await reviewPractice({
      recordId: recordDrawerData.value.record.id,
      answers: subjectiveAnswers.map((answer) => ({
        answerId: answer.id,
        paperQuestionId: answer.paperQuestionId,
        score: Number(reviewScoreMap[answer.id]) || 0,
      })),
    })
    recordDrawerData.value = await enrichRecordDrawerDetail(detail)
    ElMessage.success('批改已提交')
    await refreshCourseData()
  } finally {
    reviewSubmitting.value = false
  }
}

async function generateDiagnosisForRecord(record) {
  diagnosingRecordId.value = record.id
  try {
    await generatePracticeDiagnosis(record.id)
    ElMessage.success('AI 诊断已生成')
    await refreshCourseData()
    if (recordDrawerData.value?.record?.id === record.id) {
      const detail = await getPracticeDetail(record.id)
      recordDrawerData.value = await enrichRecordDrawerDetail(detail)
    }
  } finally {
    diagnosingRecordId.value = null
  }
}

async function generateDiagnosisForDrawer() {
  if (!recordDrawerData.value?.record?.id) return
  await generateDiagnosisForRecord({ id: recordDrawerData.value.record.id })
}

watch(
  () => [route.query.menu, route.query.action, route.query.courseId, route.query.paperId].join('|'),
  async () => {
    handledRouteIntentKey.value = ''
    const routeCourseId = numberFromRouteValue(route.query.courseId)
    if (routeCourseId && courseOptions.value.some((course) => course.id === routeCourseId)) {
      if (selectedCourseId.value !== routeCourseId) {
        selectedCourseId.value = routeCourseId
        return
      }
    }
    await maybeHandleRouteIntent()
  }
)

watch(
  selectedCourseId,
  async (courseId) => {
    paperCurrentPage.value = 1
    teacherRecordCurrentPage.value = 1
    if (!courseId) {
      paperList.value = []
      paperTotal.value = 0
      studentRecordList.value = []
      teacherRecordList.value = []
      teacherRecordTotal.value = 0
      teacherPendingCount.value = 0
      teacherDiagnosisDoneCount.value = 0
      return
    }
    await refreshCourseData()
  },
  { immediate: false }
)

watch(paperKeyword, () => {
  paperCurrentPage.value = 1
  schedulePaperReload()
})

watch([teacherRecordKeyword, teacherRecordStatus], () => {
  teacherRecordCurrentPage.value = 1
  scheduleTeacherRecordReload()
})

watch(
  activePracticeAnswers,
  () => {
    if (activePracticePaper.value?.paper?.id) {
      scheduleDraftPersist()
    }
  },
  { deep: true }
)

onMounted(async () => {
  await loadCourses()
})

onBeforeUnmount(() => {
  clearPracticeTimer()
  if (draftPersistTimer.value) {
    window.clearTimeout(draftPersistTimer.value)
    draftPersistTimer.value = null
  }
  if (paperFilterTimer) {
    window.clearTimeout(paperFilterTimer)
    paperFilterTimer = null
  }
  if (teacherRecordFilterTimer) {
    window.clearTimeout(teacherRecordFilterTimer)
    teacherRecordFilterTimer = null
  }
})
</script>

<style scoped>
.practice-view {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-panel,
.toolbar-panel,
.stats-panel,
.paper-panel,
.record-panel {
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
  background: linear-gradient(135deg, #17324d 0%, #285b7a 55%, #f4a261 100%);
  color: #fff;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  max-width: 660px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.88);
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-button {
  min-width: 120px;
}

.hero-button.secondary {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.22);
  color: #fff;
}

.toolbar-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 20px;
}

.toolbar-item {
  width: 220px;
}

.toolbar-item.grow {
  flex: 1;
  width: auto;
}

.stats-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  padding: 18px;
}

.stat-card {
  background: linear-gradient(180deg, #f8fbff 0%, #eef4fb 100%);
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stat-card span {
  color: #5f6c7b;
  font-size: 13px;
}

.stat-card strong {
  color: #17324d;
  font-size: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 18px;
  min-height: 560px;
}

.paper-panel,
.record-panel {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.panel-head h3 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #17324d;
}

.panel-head p {
  margin: 0;
  color: #7a8698;
  font-size: 13px;
}

.panel-toolbar,
.record-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-toolbar-text {
  color: #6f7c8f;
  font-size: 13px;
}

.record-filter-item {
  width: 180px;
}

.record-filter-item.grow {
  flex: 1;
  width: auto;
  min-width: 220px;
}

.paper-list,
.record-list,
.selector-list,
.question-preview-list,
.record-answer-list,
.practice-question-list {
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.paper-card,
.record-card,
.selector-card,
.question-preview-card,
.record-answer-card,
.practice-question-card {
  overflow-y: auto;
  border: 1px solid #e6edf5;
  border-radius: 18px;
  padding: 18px;
  background: #fbfdff;
}

.paper-card.active {
  border-color: #4f7cff;
  box-shadow: 0 10px 24px rgba(79, 124, 255, 0.12);
}

.paper-card-top,
.record-top,
.selector-card-top,
.question-header,
.practice-dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.paper-main,
.selector-left {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.paper-title-row,
.selector-tags,
.question-tags,
.record-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.paper-title-row h4,
.record-top h4 {
  margin: 0;
  font-size: 18px;
  color: #17324d;
}

.paper-meta,
.record-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #6f7c8f;
  font-size: 13px;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.record-summary,
.preview-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.paper-preview-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: calc(100vh - 220px);
  min-height: 0;
}

:deep(.paper-editor-dialog .el-dialog) {
  max-height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

:deep(.paper-editor-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  padding-right: 20px;
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

.dialog-headline {
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.dialog-headline p {
  margin: 0;
  color: #6f7c8f;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 700;
  color: #445162;
}

.field-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #7a8698;
}

.selector-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #516070;
  font-size: 13px;
}

.selector-card.selected {
  border-color: #4f7cff;
  background: #f6f9ff;
}

.selector-left {
  flex-direction: row;
  align-items: flex-start;
}

.selector-stem,
.question-stem {
  color: #17324d;
  font-size: 16px;
  line-height: 1.7;
}

.selector-score {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6f7c8f;
  white-space: nowrap;
}

.selector-detail,
.detail-block,
.question-content-block {
  margin-top: 14px;
}

.detail-title,
.practice-side-title {
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

.answer-box,
.analysis-box {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #415061;
  background: #f4f8fc;
  border-radius: 14px;
  padding: 14px;
}

.practice-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 18px;
  height: calc(100vh - 220px);
  min-height: 0;
  max-height: 76vh;
  overflow: hidden;
}

.practice-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.practice-side-card {
  border: 1px solid #e6edf5;
  border-radius: 18px;
  padding: 16px;
  background: #fbfdff;
}

.progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #516070;
}

.question-nav {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.question-nav-btn {
  height: 38px;
  border: 1px solid #dbe6f0;
  border-radius: 12px;
  background: #fff;
  color: #516070;
  cursor: pointer;
  transition: all 0.2s ease;
}

.question-nav-btn:hover,
.question-nav-btn.active {
  border-color: #4f7cff;
  color: #4f7cff;
}

.question-nav-btn.answered {
  background: #eef4ff;
}

.practice-question-list {
  overflow-y: auto;
  min-height: 0;
  padding-right: 4px;
}

.question-preview-list {
  flex: 1;
  min-height: 0;
}

.question-preview-card {
  overflow: visible;
}

.practice-question-card {
  overflow: visible;
}

:deep(.el-dialog__body) {
  max-height: calc(100vh - 140px);
  overflow: hidden;
}

:deep(.el-drawer__body) {
  padding-right: 20px;
}

:deep(.paper-preview-dialog .el-dialog__body) {
  max-height: calc(100vh - 140px);
  overflow: hidden;
}

.practice-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.practice-dialog-header h3 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #17324d;
}

.practice-dialog-header p {
  margin: 0;
  color: #738095;
}

.timer-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 10px 14px;
  background: #eef4ff;
  color: #2853d8;
  font-weight: 700;
}

.timer-chip.warning {
  background: #fff3e8;
  color: #d67218;
}

.answer-editor {
  margin-top: 18px;
}

.vertical-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
  align-items: stretch;
}

/* 修改：单选和多选的外层容器 */
:deep(.vertical-group .el-radio),
:deep(.vertical-group .el-checkbox) {
  margin-right: 0;
  width: 100%;
  min-width: 0;
  display: flex;
  align-items: flex-start; /* 顶部对齐，适合多行文本 */
  border: 1px solid #dbe6f0;
  border-radius: 14px;
  padding: 12px 14px;
  background: #ffffff;
  box-sizing: border-box; /* 修复点 1：确保 padding 被包含在 100% 宽度内，防止水平撑破 */
  height: auto; /* 修复点 2：覆盖组件库默认的固定高度，允许随着多行文本自动变高 */
}

/* 修改：选项文字标签部分 */
:deep(.vertical-group .el-radio__label),
:deep(.vertical-group .el-checkbox__label) {
  white-space: normal;
  line-height: 1.6;
  word-break: break-word; /* 修复点 3：强制超长连续字符（如链接）换行 */
  overflow-wrap: break-word;
  flex: 1; /* 修复点 4：占据剩余的所有空间，避免超出弹性盒子 */
  padding-left: 8px; /* 根据需要微调文本与单选/复选框的间距 */
}

/* 新增：防止前面的圆圈/方框在文本换行变长时被挤压变形 */
:deep(.vertical-group .el-radio__input),
:deep(.vertical-group .el-checkbox__input) {
  flex-shrink: 0; 
  margin-top: 2px; /* 让圆圈/方框和第一行文字的视觉中心对齐 */
}
.blank-answer-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.blank-answer-item {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 12px;
  align-items: center;
}

.blank-label {
  color: #516070;
  font-size: 14px;
}

.diagnosis-panel {
  margin-top: 18px;
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(135deg, #f6fbff 0%, #eef7f3 100%);
}

.diagnosis-panel p,
.diagnosis-panel pre {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.8;
  color: #425160;
}

.review-score-box {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  color: #516070;
}

.record-drawer-body {
  display: flex;
  flex-direction: column;
  gap: 18px
}

.record-answer-list {
  overflow-y: visible !important;
  padding-right: 0px;
}

.record-answer-card {
  overflow: visible;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.loading-wrap {
  padding: 16px 0;
}

.loading-wrap.compact {
  padding: 6px 0;
}

@media (max-width: 1200px) {
  .content-grid,
  .practice-layout {
    grid-template-columns: 1fr;
  }

  .stats-panel,
  .record-summary,
  .preview-summary,
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .practice-question-list {
    max-height: none;
  }
}

@media (max-width: 768px) {
  .hero-panel,
  .toolbar-panel,
  .panel-toolbar,
  .record-filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-item,
  .toolbar-item.grow,
  .record-filter-item,
  .record-filter-item.grow {
    width: 100%;
  }

  .stats-panel,
  .record-summary,
  .preview-summary,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .vertical-group {
    grid-template-columns: 1fr;
  }

  .blank-answer-item {
    grid-template-columns: 1fr;
  }
}

/* AI 诊断详情样式 */
.ai-analysis-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 12px;
}

.diagnosis-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #17324d;
  display: flex;
  align-items: center;
  gap: 8px;
}

.diagnosis-section ul {
  margin: 0;
  padding-left: 20px;
  color: #415061;
  line-height: 1.8;
  font-size: 14px;
}

.diagnosis-section li {
  margin-bottom: 6px;
}

/* 单题 AI 反馈专有样式 */
.custom-ai-feedback {
  white-space: normal; /* 覆盖原始的 pre-wrap */
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-section {
  line-height: 1.6;
  color: #415061;
  font-size: 14px;
}

.feedback-label {
  font-weight: 700;
  color: #17324d;
  margin-right: 4px;
}

.kp-section {
  margin-top: 4px;
}

.kp-list {
  margin: 8px 0 0;
  padding-left: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kp-item {
  background: #ffffff;
  border: 1px solid #e6edf5;
  border-radius: 10px;
  padding: 12px;
}

.kp-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.kp-detail {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #5c6878;
}

.kp-detail strong {
  color: #445162;
}

</style>
