<template>
  <view class="fd-page">
    <fd-nav-bar title="我的报告" />

    <!-- 月份选择 -->
    <view class="month-bar fd-card">
      <view class="month-btn" @tap="changeMonth(-1)">
        <text class="month-arrow">‹</text>
      </view>
      <text class="month-text">{{ year }}年{{ month }}月</text>
      <view class="month-btn" @tap="changeMonth(1)">
        <text class="month-arrow">›</text>
      </view>
    </view>

    <!-- Tab 切换 -->
    <view class="tab-bar fd-card">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ 'tab-item--active': activeTab === tab.key }"
        @tap="activeTab = tab.key"
      >
        <text>{{ tab.label }}</text>
      </view>
    </view>

    <!-- 消费趋势 -->
    <block v-if="activeTab === 'expense'">
      <view class="section fd-card" v-if="expenseStats">
        <!-- 汇总数字 -->
        <view class="summary-row">
          <view class="summary-item">
            <text class="summary-num fd-text-primary">¥{{ expenseStats.total }}</text>
            <text class="summary-label">月总消费</text>
          </view>
          <view class="summary-item">
            <text class="summary-num">¥{{ expenseStats.avgPerDay }}</text>
            <text class="summary-label">日均消费</text>
          </view>
          <view class="summary-item">
            <text class="summary-num">{{ expenseStats.activeDays }}</text>
            <text class="summary-label">消费天数</text>
          </view>
        </view>

        <!-- 按天柱状图 -->
        <text class="chart-title">每日消费 (元)</text>
        <view class="bar-chart">
          <view
            v-for="(val, day) in expenseStats.byDay"
            :key="day"
            class="bar-col"
          >
            <text class="bar-val" v-if="Number(val) > 0">{{ Number(val).toFixed(0) }}</text>
            <view
              class="bar"
              :style="{ height: barHeight(val, expenseStats.byDay) + 'rpx' }"
              :class="{ 'bar--active': Number(val) > 0 }"
            />
            <text class="bar-label">{{ day }}</text>
          </view>
        </view>

        <!-- 按餐次 -->
        <text class="chart-title">餐次分布</text>
        <view class="meal-dist">
          <view
            v-for="(val, key) in expenseStats.byMealType"
            :key="key"
            class="meal-dist-item"
          >
            <text class="meal-dist-name">{{ mealTypeLabel(key) }}</text>
            <view class="meal-dist-bar-bg">
              <view
                class="meal-dist-bar"
                :style="{ width: mealBarWidth(val, expenseStats.byMealType) + '%' }"
              />
            </view>
            <text class="meal-dist-val">¥{{ Number(val).toFixed(1) }}</text>
          </view>
        </view>
      </view>
      <view v-else class="loading-card fd-card">
        <text class="fd-text-secondary">{{ loading ? '加载中...' : '本月暂无消费记录' }}</text>
      </view>
    </block>

    <!-- 营养摄入 -->
    <block v-if="activeTab === 'nutrition'">
      <view class="section fd-card" v-if="nutritionStats && nutritionStats.totalRecords > 0">
        <view class="summary-row">
          <view class="summary-item">
            <text class="summary-num fd-text-primary">{{ nutritionStats.totalRecords }}</text>
            <text class="summary-label">本月记录餐数</text>
          </view>
        </view>
        <text class="chart-title">营养摄入分布</text>
        <view class="nutrition-list">
          <view
            v-for="(count, type) in nutritionStats.nutritionCount"
            :key="type"
            class="nutrition-item"
          >
            <view class="nutrition-info">
              <text class="nutrition-dot" :style="{ background: nutritionColor(type) }"></text>
              <text class="nutrition-name">{{ nutritionLabel(type) }}</text>
            </view>
            <view class="nutrition-bar-bg">
              <view
                class="nutrition-bar"
                :style="{
                  width: nutritionBarWidth(count, nutritionStats.nutritionCount) + '%',
                  background: nutritionColor(type)
                }"
              />
            </view>
            <text class="nutrition-count">{{ count }}次</text>
          </view>
        </view>
      </view>
      <view v-else class="loading-card fd-card">
        <text class="fd-text-secondary">{{ loading ? '加载中...' : '本月暂无营养记录' }}</text>
      </view>
    </block>

    <!-- 用餐习惯 -->
    <block v-if="activeTab === 'habit'">
      <view v-if="habitStats">
        <!-- 数据卡片 -->
        <view class="section fd-card">
          <view class="habit-stats">
            <view class="habit-stat-item">
              <text class="habit-num fd-text-primary">{{ habitStats.streak }}</text>
              <text class="habit-label">🔥 连续打卡天</text>
            </view>
            <view class="habit-stat-item">
              <text class="habit-num">{{ habitStats.totalRecords }}</text>
              <text class="habit-label">📋 累计记录餐</text>
            </view>
          </view>
        </view>

        <!-- 最爱食物 -->
        <view class="section fd-card" v-if="habitStats.topFoods && habitStats.topFoods.length">
          <text class="chart-title">🍜 最爱食物 Top 5</text>
          <view class="top-food-list">
            <view
              v-for="(food, index) in habitStats.topFoods"
              :key="food.name"
              class="top-food-item"
            >
              <text class="top-food-rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</text>
              <text class="top-food-name">{{ food.name }}</text>
              <text class="top-food-count">{{ food.count }}次</text>
            </view>
          </view>
        </view>

        <!-- 餐次习惯 -->
        <view class="section fd-card" v-if="habitStats.mealTypeCount">
          <text class="chart-title">🕐 餐次习惯</text>
          <view class="meal-dist">
            <view
              v-for="(count, type) in habitStats.mealTypeCount"
              :key="type"
              class="meal-dist-item"
            >
              <text class="meal-dist-name">{{ mealTypeLabel(type) }}</text>
              <view class="meal-dist-bar-bg">
                <view
                  class="meal-dist-bar meal-dist-bar--green"
                  :style="{ width: mealBarWidth(count, habitStats.mealTypeCount) + '%' }"
                />
              </view>
              <text class="meal-dist-val">{{ count }}次</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="loading-card fd-card">
        <text class="fd-text-secondary">{{ loading ? '加载中...' : '暂无用餐记录' }}</text>
      </view>
    </block>

  </view>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { get } from '@/utils/http'
import FdNavBar from '@/components/common/fd-nav-bar.vue'

const tabs = [
  { key: 'expense', label: '消费趋势' },
  { key: 'nutrition', label: '营养摄入' },
  { key: 'habit', label: '用餐习惯' },
]
const activeTab = ref('expense')

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const loading = ref(false)
const expenseStats = ref(null)
const nutritionStats = ref(null)
const habitStats = ref(null)

function changeMonth(delta) {
  let m = month.value + delta
  let y = year.value
  if (m > 12) { m = 1; y++ }
  if (m < 1) { m = 12; y-- }
  month.value = m
  year.value = y
}

async function loadStats() {
  loading.value = true
  try {
    const [exp, nut, hab] = await Promise.all([
      get(`/api/stats/expense?year=${year.value}&month=${month.value}`),
      get(`/api/stats/nutrition?year=${year.value}&month=${month.value}`),
      get('/api/stats/habit'),
    ])
    expenseStats.value = exp
    nutritionStats.value = nut
    habitStats.value = hab
  } catch (e) {
    console.error('加载统计失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
watch([year, month], loadStats)

// ---- 图表辅助函数 ----
function barHeight(val, map) {
  const max = Math.max(...Object.values(map).map(Number))
  if (!max) return 4
  return Math.max(4, (Number(val) / max) * 160)
}

function mealBarWidth(val, map) {
  const max = Math.max(...Object.values(map).map(Number))
  if (!max) return 0
  return Math.round((Number(val) / max) * 100)
}

function nutritionBarWidth(count, map) {
  const max = Math.max(...Object.values(map).map(Number))
  if (!max) return 0
  return Math.round((Number(count) / max) * 100)
}

const MEAL_LABELS = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐', other: '其他' }
function mealTypeLabel(key) { return MEAL_LABELS[key] || key }

const NUTRITION_LABELS = {
  protein: '蛋白质', carb: '碳水', fat: '脂肪', fiber: '膳食纤维',
  vitamin: '维生素', calcium: '钙', iron: '铁', light: '轻食',
}
const NUTRITION_COLORS = {
  protein: '#FF6B6B', carb: '#FFE66D', fat: '#4ECDC4',
  fiber: '#2ED573', vitamin: '#FFA502', calcium: '#A29BFE',
  iron: '#FD79A8', light: '#55EFC4',
}
function nutritionLabel(type) { return NUTRITION_LABELS[type] || type }
function nutritionColor(type) { return NUTRITION_COLORS[type] || '#B2BEC3' }
</script>

<style lang="scss" scoped>
.month-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 24rpx;
  padding: 20rpx 32rpx;
}
.month-btn { padding: 8rpx 16rpx; }
.month-arrow { font-size: 48rpx; color: $fd-primary; line-height: 1; }
.month-text { font-size: $fd-font-md; font-weight: 600; color: $fd-text; }

.tab-bar {
  display: flex;
  margin: 0 24rpx 24rpx;
  padding: 8rpx;
  border-radius: $fd-radius;
  gap: 8rpx;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: $fd-radius-sm;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  transition: all 0.2s;
  &--active {
    background: $fd-primary;
    color: #fff;
    font-weight: 600;
  }
}

.section { margin: 0 24rpx 24rpx; }
.loading-card {
  margin: 0 24rpx;
  padding: 60rpx;
  text-align: center;
}

.summary-row {
  display: flex;
  justify-content: space-around;
  padding-bottom: 32rpx;
  border-bottom: 2rpx solid $fd-border;
  margin-bottom: 32rpx;
}
.summary-item { text-align: center; }
.summary-num { display: block; font-size: $fd-font-xl; font-weight: 700; }
.summary-label { display: block; font-size: $fd-font-xs; color: $fd-text-secondary; margin-top: 4rpx; }

.chart-title {
  display: block;
  font-size: $fd-font-sm;
  font-weight: 600;
  color: $fd-text-secondary;
  margin-bottom: 24rpx;
}

/* 柱状图 */
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 4rpx;
  height: 200rpx;
  overflow-x: auto;
  padding-bottom: 32rpx;
  margin-bottom: 32rpx;
}
.bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  min-width: 28rpx;
}
.bar-val {
  font-size: 18rpx;
  color: $fd-primary;
  margin-bottom: 4rpx;
  writing-mode: vertical-rl;
  transform: rotate(180deg);
  display: none;
}
.bar {
  width: 100%;
  background: $fd-border;
  border-radius: 4rpx 4rpx 0 0;
  min-height: 4rpx;
  &--active { background: $fd-primary; }
}
.bar-label {
  font-size: 18rpx;
  color: $fd-text-secondary;
  margin-top: 4rpx;
}

/* 餐次横向进度条 */
.meal-dist { display: flex; flex-direction: column; gap: 20rpx; }
.meal-dist-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.meal-dist-name { font-size: $fd-font-sm; color: $fd-text; width: 80rpx; flex-shrink: 0; }
.meal-dist-bar-bg {
  flex: 1;
  height: 16rpx;
  background: $fd-border;
  border-radius: $fd-radius-round;
  overflow: hidden;
}
.meal-dist-bar {
  height: 100%;
  background: $fd-primary;
  border-radius: $fd-radius-round;
  transition: width 0.4s ease;
  &--green { background: $fd-accent; }
}
.meal-dist-val { font-size: $fd-font-sm; color: $fd-text-secondary; width: 80rpx; text-align: right; }

/* 营养列表 */
.nutrition-list { display: flex; flex-direction: column; gap: 20rpx; }
.nutrition-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.nutrition-info { display: flex; align-items: center; gap: 10rpx; width: 120rpx; flex-shrink: 0; }
.nutrition-dot { width: 16rpx; height: 16rpx; border-radius: 50%; flex-shrink: 0; }
.nutrition-name { font-size: $fd-font-sm; color: $fd-text; }
.nutrition-bar-bg {
  flex: 1;
  height: 16rpx;
  background: $fd-border;
  border-radius: $fd-radius-round;
  overflow: hidden;
}
.nutrition-bar { height: 100%; border-radius: $fd-radius-round; transition: width 0.4s ease; }
.nutrition-count { font-size: $fd-font-sm; color: $fd-text-secondary; width: 60rpx; text-align: right; }

/* 习惯统计 */
.habit-stats { display: flex; justify-content: space-around; }
.habit-stat-item { text-align: center; }
.habit-num { display: block; font-size: 72rpx; font-weight: 700; line-height: 1; }
.habit-label { display: block; font-size: $fd-font-sm; color: $fd-text-secondary; margin-top: 8rpx; }

.top-food-list { display: flex; flex-direction: column; gap: 16rpx; }
.top-food-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 0;
  border-bottom: 2rpx solid $fd-border;
  &:last-child { border-bottom: none; }
}
.top-food-rank {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fd-font-sm;
  font-weight: 700;
  color: $fd-text-secondary;
  background: $fd-border;
  flex-shrink: 0;
  &.rank-1 { background: #FFD700; color: #fff; }
  &.rank-2 { background: #C0C0C0; color: #fff; }
  &.rank-3 { background: #CD7F32; color: #fff; }
}
.top-food-name { flex: 1; font-size: $fd-font-base; color: $fd-text; }
.top-food-count { font-size: $fd-font-sm; color: $fd-text-secondary; }
</style>
