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

    <!-- 加载失败 -->
    <fd-error-state v-if="loadError" text="报告加载失败，请检查网络" @retry="loadStats" />

    <!-- 消费趋势 -->
    <block v-else-if="activeTab === 'expense'">
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
    <block v-else-if="activeTab === 'nutrition'">
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

    <!-- 月度对比 -->
    <block v-else-if="activeTab === 'compare'">
      <view v-if="compareStats" class="section fd-card">
        <!-- 消费对比 -->
        <text class="chart-title">💰 消费对比</text>
        <view class="compare-row">
          <view class="compare-col">
            <text class="compare-month-label">上月</text>
            <text class="compare-amount compare-amount--secondary">¥{{ compareStats.prev.total }}</text>
          </view>
          <view class="compare-arrow-col">
            <text
              class="compare-delta"
              :class="compareDeltaClass(compareStats.curr.total, compareStats.prev.total)"
            >
              {{ compareDeltaText(compareStats.curr.total, compareStats.prev.total) }}
            </text>
          </view>
          <view class="compare-col compare-col--right">
            <text class="compare-month-label">本月</text>
            <text class="compare-amount fd-text-primary">¥{{ compareStats.curr.total }}</text>
          </view>
        </view>

        <!-- 日均对比 -->
        <view class="compare-detail-row">
          <view class="compare-detail-item">
            <text class="compare-detail-label">日均消费</text>
            <text class="compare-detail-prev">¥{{ compareStats.prev.avgPerDay }}</text>
            <text class="compare-detail-arrow">→</text>
            <text class="compare-detail-curr fd-text-primary">¥{{ compareStats.curr.avgPerDay }}</text>
          </view>
          <view class="compare-detail-item">
            <text class="compare-detail-label">消费天数</text>
            <text class="compare-detail-prev">{{ compareStats.prev.activeDays }}天</text>
            <text class="compare-detail-arrow">→</text>
            <text class="compare-detail-curr fd-text-primary">{{ compareStats.curr.activeDays }}天</text>
          </view>
        </view>
      </view>

      <view v-if="compareStats && compareNutrition" class="section fd-card">
        <!-- 营养对比 -->
        <text class="chart-title">🥦 营养摄入对比</text>
        <view class="nutrition-compare-list">
          <view
            v-for="(vals, type) in compareNutrition"
            :key="type"
            class="nutrition-compare-item"
          >
            <view class="nutrition-info">
              <text class="nutrition-dot" :style="{ background: nutritionColor(type) }"></text>
              <text class="nutrition-name">{{ nutritionLabel(type) }}</text>
            </view>
            <view class="nutrition-compare-bars">
              <view class="nutrition-compare-bar-wrap">
                <text class="nutrition-compare-sub">上月</text>
                <view class="nutrition-bar-bg">
                  <view class="nutrition-bar" :style="{ width: vals.prevPct + '%', background: '#ccc' }" />
                </view>
                <text class="nutrition-count">{{ vals.prev }}次</text>
              </view>
              <view class="nutrition-compare-bar-wrap">
                <text class="nutrition-compare-sub">本月</text>
                <view class="nutrition-bar-bg">
                  <view class="nutrition-bar" :style="{ width: vals.currPct + '%', background: nutritionColor(type) }" />
                </view>
                <text class="nutrition-count">{{ vals.curr }}次</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view v-if="!compareStats" class="loading-card fd-card">
        <text class="fd-text-secondary">{{ loading ? '加载中...' : '暂无对比数据' }}</text>
      </view>
    </block>

    <!-- 用餐习惯 -->
    <block v-else-if="activeTab === 'habit'">
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

    <!-- 月报导出 -->
    <view class="export-section">
      <!-- 隐藏的绘制画布 -->
      <canvas canvas-id="report-canvas" class="report-canvas" />
      <view class="fd-btn export-btn" :class="{ 'fd-btn--disabled': exporting }" @tap="exportReport">
        <text>{{ exporting ? '生成中...' : '📥 导出本月报告图片' }}</text>
      </view>
    </view>

  </view>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { get } from '@/utils/http'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdErrorState from '@/components/common/fd-error-state.vue'

const tabs = [
  { key: 'expense',  label: '消费趋势' },
  { key: 'nutrition', label: '营养摄入' },
  { key: 'habit',    label: '用餐习惯' },
  { key: 'compare',  label: '月度对比' },
]
const activeTab = ref('expense')

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const loading = ref(false)
const loadError = ref(false)
const expenseStats = ref(null)
const nutritionStats = ref(null)
const habitStats = ref(null)
const compareStats = ref(null)
const compareNutritionRaw = ref(null)

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
  loadError.value = false
  try {
    const prevMonth = month.value === 1 ? 12 : month.value - 1
    const prevYear  = month.value === 1 ? year.value - 1 : year.value

    const [exp, nut, hab, prevExp, prevNut] = await Promise.all([
      get(`/api/stats/expense?year=${year.value}&month=${month.value}`),
      get(`/api/stats/nutrition?year=${year.value}&month=${month.value}`),
      get('/api/stats/habit'),
      get(`/api/stats/expense?year=${prevYear}&month=${prevMonth}`),
      get(`/api/stats/nutrition?year=${prevYear}&month=${prevMonth}`),
    ])
    expenseStats.value = exp
    nutritionStats.value = nut
    habitStats.value = hab

    if (exp && prevExp) {
      compareStats.value = { curr: exp, prev: prevExp }
    } else {
      compareStats.value = null
    }
    compareNutritionRaw.value = (nut && prevNut) ? { curr: nut, prev: prevNut } : null
  } catch (e) {
    console.error('加载统计失败', e)
    loadError.value = true
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

// ---- 月度对比辅助 ----
const compareNutrition = computed(() => {
  const raw = compareNutritionRaw.value
  if (!raw) return null
  const allTypes = new Set([
    ...Object.keys(raw.curr.nutritionCount || {}),
    ...Object.keys(raw.prev.nutritionCount || {}),
  ])
  const maxVal = Math.max(
    ...Object.values(raw.curr.nutritionCount || {}).map(Number),
    ...Object.values(raw.prev.nutritionCount || {}).map(Number),
    1,
  )
  const result = {}
  for (const t of allTypes) {
    const curr = Number((raw.curr.nutritionCount || {})[t] || 0)
    const prev = Number((raw.prev.nutritionCount || {})[t] || 0)
    result[t] = {
      curr,
      prev,
      currPct: Math.round((curr / maxVal) * 100),
      prevPct: Math.round((prev / maxVal) * 100),
    }
  }
  return result
})

function compareDeltaText(curr, prev) {
  const c = Number(curr) || 0
  const p = Number(prev) || 0
  if (p === 0) return c > 0 ? '↑ 新增' : '-'
  const pct = Math.round(((c - p) / p) * 100)
  if (pct > 0) return `↑ ${pct}%`
  if (pct < 0) return `↓ ${Math.abs(pct)}%`
  return '持平'
}

function compareDeltaClass(curr, prev) {
  const c = Number(curr) || 0
  const p = Number(prev) || 0
  if (c > p) return 'compare-delta--up'
  if (c < p) return 'compare-delta--down'
  return 'compare-delta--flat'
}

// ─── 月报图片导出 ────────────────────────────────────────────────
const exporting = ref(false)

function exportReport() {
  if (exporting.value) return
  const exp = expenseStats.value
  if (!exp) {
    uni.showToast({ title: '数据还未加载，请稍候', icon: 'none' })
    return
  }
  exporting.value = true

  const W = 600
  const H = 800
  const ctx = uni.createCanvasContext('report-canvas')

  // 背景
  ctx.setFillStyle('#FFF5F5')
  ctx.fillRect(0, 0, W, H)

  // 标题
  ctx.setFillStyle('#FF6B6B')
  ctx.setFontSize(28)
  ctx.setTextAlign('center')
  ctx.fillText(`${year.value}年${month.value}月 饮食报告`, W / 2, 60)

  // 分割线
  ctx.setStrokeStyle('#FFE0E0')
  ctx.setLineWidth(1)
  ctx.beginPath()
  ctx.moveTo(40, 80)
  ctx.lineTo(W - 40, 80)
  ctx.stroke()

  // 消费摘要
  ctx.setFillStyle('#333')
  ctx.setFontSize(20)
  ctx.setTextAlign('left')
  ctx.fillText('💰 本月消费', 40, 120)

  const totalSpent = Number(exp.total || 0).toFixed(2)
  const dayAvg = Number(exp.avgPerDay || 0).toFixed(2)
  const activeDays = exp.activeDays || 0

  ctx.setFillStyle('#FF6B6B')
  ctx.setFontSize(36)
  ctx.setTextAlign('center')
  ctx.fillText(`¥${totalSpent}`, W / 2, 175)

  ctx.setFillStyle('#888')
  ctx.setFontSize(18)
  ctx.fillText(`日均 ¥${dayAvg} · 记录 ${activeDays} 天`, W / 2, 205)

  // 营养分布
  ctx.setFillStyle('#333')
  ctx.setFontSize(20)
  ctx.setTextAlign('left')
  ctx.fillText('🥗 营养摄入', 40, 255)

  const nutMap = nutritionStats.value?.nutritionCount || {}
  const nutEntries = Object.entries(nutMap)
  const nutColors = { carb: '#FFE66D', protein: '#FF6B6B', veggie: '#4ECDC4', fruit: '#A8E6CF' }
  const nutLabels = { carb: '碳水', protein: '蛋白质', veggie: '蔬菜', fruit: '水果' }
  const maxNut = Math.max(...nutEntries.map(([, v]) => Number(v)), 1)
  const barMaxW = W - 200

  nutEntries.forEach(([k, v], i) => {
    const y = 285 + i * 50
    const barW = Math.max(4, (Number(v) / maxNut) * barMaxW)
    ctx.setFillStyle('#f0f0f0')
    ctx.fillRoundRect(140, y, barMaxW, 28, 6)
    ctx.setFillStyle(nutColors[k] || '#4ECDC4')
    ctx.fillRoundRect(140, y, barW, 28, 6)
    ctx.setFillStyle('#555')
    ctx.setFontSize(18)
    ctx.setTextAlign('left')
    ctx.fillText(nutLabels[k] || k, 40, y + 20)
    ctx.setTextAlign('right')
    ctx.fillText(`${v}次`, W - 40, y + 20)
  })

  // 用餐习惯
  const habitY = 290 + nutEntries.length * 50 + 20
  ctx.setFillStyle('#333')
  ctx.setFontSize(20)
  ctx.setTextAlign('left')
  ctx.fillText('🍽️ 用餐习惯', 40, habitY)

  const mealMap = habitStats.value?.mealTypeCount || {}
  const mealLabels = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐' }
  const mealEntries = Object.entries(mealMap)
  mealEntries.forEach(([k, v], i) => {
    ctx.setFillStyle('#888')
    ctx.setFontSize(18)
    ctx.setTextAlign('left')
    ctx.fillText(`${mealLabels[k] || k}: ${v}次`, 40 + i * 190, habitY + 36)
  })

  // 底部水印
  ctx.setFillStyle('#ccc')
  ctx.setFontSize(16)
  ctx.setTextAlign('center')
  ctx.fillText('饭搭 · 让每顿饭都有意义', W / 2, H - 30)

  ctx.draw(false, () => {
    uni.canvasToTempFilePath({
      canvasId: 'report-canvas',
      fileType: 'png',
      quality: 1,
      success(res) {
        exporting.value = false
        uni.saveImageToPhotosAlbum({
          filePath: res.tempFilePath,
          success() {
            uni.showToast({ title: '已保存到相册 ✅', icon: 'success' })
          },
          fail() {
            // 保存失败时改为预览
            uni.previewImage({ urls: [res.tempFilePath] })
          },
        })
      },
      fail() {
        exporting.value = false
        uni.showToast({ title: '生成失败，请重试', icon: 'none' })
      },
    })
  })
}
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

/* 月度对比 */
.compare-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32rpx;
}
.compare-col {
  text-align: center;
  flex: 1;
  &--right { text-align: right; }
}
.compare-month-label { display: block; font-size: $fd-font-xs; color: $fd-text-secondary; margin-bottom: 8rpx; }
.compare-amount {
  display: block;
  font-size: $fd-font-xl;
  font-weight: 700;
  &--secondary { color: $fd-text-secondary; }
}
.compare-arrow-col { text-align: center; padding: 0 16rpx; }
.compare-delta {
  display: block;
  font-size: $fd-font-sm;
  font-weight: 600;
  padding: 8rpx 16rpx;
  border-radius: $fd-radius-round;
  &--up { color: $fd-danger; background: rgba($fd-danger, 0.1); }
  &--down { color: $fd-accent; background: rgba($fd-accent, 0.1); }
  &--flat { color: $fd-text-secondary; background: $fd-border; }
}
.compare-detail-row {
  display: flex;
  gap: 24rpx;
  border-top: 2rpx solid $fd-border;
  padding-top: 24rpx;
}
.compare-detail-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}
.compare-detail-label { font-size: $fd-font-xs; color: $fd-text-secondary; }
.compare-detail-prev { font-size: $fd-font-sm; color: $fd-text-secondary; }
.compare-detail-arrow { font-size: $fd-font-xs; color: $fd-text-light; }
.compare-detail-curr { font-size: $fd-font-sm; font-weight: 700; }

.nutrition-compare-list { display: flex; flex-direction: column; gap: 24rpx; }
.nutrition-compare-item { display: flex; align-items: flex-start; gap: 16rpx; }
.nutrition-compare-bars { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.nutrition-compare-bar-wrap { display: flex; align-items: center; gap: 12rpx; }
.nutrition-compare-sub { font-size: 20rpx; color: $fd-text-secondary; width: 50rpx; flex-shrink: 0; }

.export-section {
  padding: $fd-space-base $fd-space-md $fd-space-xl;
}
.report-canvas {
  width: 600px;
  height: 800px;
  position: fixed;
  left: -9999px;
  top: -9999px;
  opacity: 0;
}
.export-btn { width: 100%; }
.fd-btn--disabled { opacity: 0.5; pointer-events: none; }

</style>
