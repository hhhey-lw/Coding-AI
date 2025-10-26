<template>
  <div class="plan-card">
    <!-- 卡片头部 -->
    <div class="plan-header">
      <div class="plan-icon">
        <el-icon :size="20"><List /></el-icon>
      </div>
      <div class="plan-title-wrapper">
        <div class="plan-label">执行计划</div>
        <div class="plan-title">{{ planData.title }}</div>
      </div>
      <div class="plan-status">
        <el-tag v-if="isFinished" type="success" size="small">已完成</el-tag>
        <el-tag v-else type="primary" size="small" effect="plain">
          {{ currentStep }}/{{ totalSteps }}
        </el-tag>
      </div>
    </div>

    <!-- 步骤列表 -->
    <div class="plan-steps">
      <div
        v-for="(step, index) in planData.steps"
        :key="index"
        class="step-item"
        :class="getStepClass(index)"
      >
        <div class="step-checkbox">
          <!-- 已完成：当前步骤之前的，或者计划完成时的所有步骤 -->
          <el-icon v-if="isFinished || index < currentStep - 1" :size="18" color="#67c23a">
            <CircleCheck />
          </el-icon>
          <!-- 进行中：当前步骤且计划未完成 -->
          <el-icon v-else-if="!isFinished && index === currentStep - 1" :size="18" color="#409eff">
            <Loading class="rotating" />
          </el-icon>
          <!-- 待执行：其他情况 -->
          <div v-else class="empty-circle"></div>
        </div>
        <div class="step-content">
          <div class="step-text">{{ step }}</div>
        </div>
      </div>
    </div>

    <!-- 计划ID（调试用） -->
    <div class="plan-footer" v-if="showPlanId">
      <el-text size="small" type="info">计划ID: {{ planData.planId }}</el-text>
    </div>
  </div>
</template>

<script setup lang="ts">
import { List, CircleCheck, Loading } from '@element-plus/icons-vue'

interface Props {
  planData: {
    planId: string
    title: string
    steps: string[]
  }
  currentStep?: number
  totalSteps?: number
  percentage?: number
  isFinished?: boolean
  stepDescription?: string
  showPlanId?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  currentStep: 0,
  totalSteps: 0,
  percentage: 0,
  isFinished: false,
  stepDescription: '',
  showPlanId: false
})

// 方法 - 获取步骤样式类
const getStepClass = (index: number) => {
  // 如果计划完成，所有步骤都标记为已完成
  if (props.isFinished) {
    return 'completed'
  }
  // 当前步骤之前的标记为已完成
  if (index < props.currentStep - 1) {
    return 'completed'
  }
  // 当前步骤标记为进行中
  if (index === props.currentStep - 1) {
    return 'active'
  }
  // 其他标记为待执行
  return 'pending'
}
</script>

<style scoped>
.plan-card {
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-left: 4px solid #409eff;
  border-radius: 8px;
  padding: 20px;
  margin: 16px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  color: #303133;
  transition: all 0.3s;
}

.plan-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.plan-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.plan-icon {
  width: 40px;
  height: 40px;
  background: #ecf5ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #409eff;
}

.plan-title-wrapper {
  flex: 1;
}

.plan-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.plan-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: #303133;
}

.plan-status {
  flex-shrink: 0;
}

.plan-steps {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #f0f0f0;
}

.step-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #e8e8e8;
  transition: all 0.3s;
}

.step-item:last-child {
  border-bottom: none;
}

.step-item.active {
  background: #f0f7ff;
  border-radius: 8px;
  padding: 12px;
  margin: 0 -12px;
  width: calc(100% + 24px);
  border-bottom: none;
}

.step-checkbox {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 50%;
}

.empty-circle {
  width: 16px;
  height: 16px;
  border: 2px solid #d0d0d0;
  border-radius: 50%;
  box-sizing: border-box;
}

.step-content {
  flex: 1;
}

.step-text {
  font-size: 14px;
  line-height: 1.6;
  font-weight: 500;
  color: #606266;
}

.step-item.completed .step-text {
  color: #909399;
  text-decoration: line-through;
}

.step-item.active .step-text {
  font-weight: 600;
  color: #303133;
}

.step-item.pending .step-text {
  color: #909399;
}

.plan-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
  color: #909399;
}

/* 旋转动画 */
@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.rotating {
  animation: rotating 1s linear infinite;
}
</style>

