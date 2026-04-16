<template>
  <view class="fd-page">
    <fd-nav-bar title="预算管家" />

    <!-- 预算总览卡片 -->
    <view class="budget-card fd-card">
      <view class="budget-ring">
        <view class="ring" :style="ringStyle">
          <view class="ring-inner">
            <text class="ring-percent">{{ budgetStore.monthlyPercent }}%</text>
            <text class="ring-label">已使用</text>
          </view>
        </view>
      </view>
      <view class="budget-info">
        <view class="budget-row">
          <text class="budget-label">月预算</text>
          <text class="budget-value">¥{{ budgetStore.budget.monthly }}</text>
        </view>
        <view class="budget-row">
          <text class="budget-label">已花费</text>
          <text class="budget-value fd-text-primary">¥{{ budgetStore.monthlyTotal }}</text>
        </view>
        <view class="budget-row">
          <text class="budget-label">剩余</text>
          <text class="budget-value" :class="budgetStore.isOverBudget ? 'over-budget' : ''">¥{{ budgetStore.monthlyRemaining }}</text>
        </view>
        <view class="budget-divider"></view>
        <view class="budget-row">
          <text class="budget-label">今日建议</text>
          <text class="budget-suggest">≤ ¥{{ budgetStore.dailySuggestion }}</text>
        </view>
      </view>
    </view>

    <!-- 超支警告 -->
    <view v-if="budgetStore.isOverBudget" class="warning fd-card fd-anim-shake">
      <text>⚠️ 本月预算已超支！请注意控制消费哦~</text>
    </view>

    <!-- 智能预警卡片（未超支时展示） -->
    <view v-else class="smart-card fd-card">
      <!-- 今日额度 -->
      <view class="smart-row">
        <view class="smart-item" :class="todayStatusClass">
          <text class="smart-num">¥{{ budgetStore.dailySuggestion }}</text>
          <text class="smart-label">今日建议上限</text>
        </view>
        <view class="smart-divider" />
        <view class="smart-item">
          <text class="smart-num">{{ daysLeft }}天</text>
          <text class="smart-label">本月剩余天数</text>
        </view>
        <view class="smart-divider" />
        <view class="smart-item" :class="trendClass">
          <text class="smart-num">{{ trendLabel }}</text>
          <text class="smart-label">消费趋势</text>
        </view>
      </view>

      <!-- 趋势提示语 -->
      <view class="smart-tip" :class="trendClass">
        <text class="smart-tip-text">{{ trendTip }}</text>
      </view>
    </view>

    <!-- 近7天消费折线图 -->
    <view class="week-chart fd-card">
      <text class="section-title">近7天消费</text>
      <view class="chart-area">
        <!-- Y轴标签 -->
        <view class="chart-y-labels">
          <text class="chart-y-label" v-for="(v, i) in yLabels" :key="i">{{ v }}</text>
        </view>
        <!-- 折线图 SVG -->
        <view class="chart-svg-wrap">
          <!-- 使用 canvas 绘制折线图 -->
          <canvas canvas-id="weekChartCanvas" class="chart-canvas" />
        </view>
      </view>
      <!-- X轴日期 -->
      <view class="chart-x-labels">
        <text v-for="d in weekDays" :key="d.date" class="chart-x-label">{{ d.label }}</text>
      </view>
      <!-- 金额点提示 -->
      <view class="chart-values">
        <view v-for="d in weekDays" :key="d.date" class="chart-value-item">
          <text class="chart-value-num" :class="d.amount > 0 ? 'chart-value--active' : ''">
            {{ d.amount > 0 ? '¥' + d.amount : '-' }}
          </text>
        </view>
      </view>
    </view>

    <!-- 设置预算 -->
    <view class="set-budget fd-card">
      <text class="section-title">设置预算</text>
      <view class="input-row">
        <text class="input-label">月预算</text>
        <input class="input-field" type="digit" :value="monthlyInput" placeholder="输入月预算" @input="monthlyInput = $event.detail.value" />
        <text class="input-unit">元</text>
      </view>
      <view class="fd-btn" @tap="saveBudget"><text>保存</text></view>
    </view>

    <!-- 消费明细 -->
    <view class="expense-section">
      <text class="section-title" style="padding: 0 32rpx;">本月消费明细</text>
      <view v-if="budgetStore.monthlyExpenses.length === 0">
        <fd-empty icon="💰" text="暂无消费记录" />
      </view>
      <view v-for="item in budgetStore.monthlyExpenses" :key="item.id" class="expense-item fd-card">
        <view class="expense-left">
          <text class="expense-desc">{{ item.description }}</text>
          <text class="expense-date">{{ item.date }} · {{ mealLabel(item.mealType) }}</text>
        </view>
        <view class="expense-right">
          <text class="expense-amount">-¥{{ item.amount }}</text>
          <text class="expense-delete" @tap="deleteExpense(item.id)">×</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useBudgetStore } from '@/stores/modules/budget'
import { mealTypeLabel } from '@/utils/date'
import dayjs from 'dayjs'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdEmpty from '@/components/common/fd-empty.vue'

const budgetStore = useBudgetStore()
const monthlyInput = ref('')

// ─── 趋势预测 ────────────────────────────────────────
// 按照当前已消费天数推算：如果保持这个日均速度到月末，总消费会是多少
const daysLeft = computed(() => {
  return dayjs().daysInMonth() - dayjs().date() + 1
})

const daysPassed = computed(() => {
  return dayjs().date()
})

// 当前日均消费
const actualDailyAvg = computed(() => {
  const passed = daysPassed.value
  if (passed === 0) return 0
  return budgetStore.monthlyTotal / passed
})

// 预测月末总消费
const projectedTotal = computed(() => {
  return Math.round(actualDailyAvg.value * dayjs().daysInMonth())
})

// 超出预算百分比
const projectedOverPct = computed(() => {
  const budget = budgetStore.budget.monthly
  if (budget <= 0) return 0
  return Math.round(((projectedTotal.value - budget) / budget) * 100)
})

const trendClass = computed(() => {
  const pct = projectedOverPct.value
  if (pct > 20) return 'trend--danger'
  if (pct > 0) return 'trend--warn'
  return 'trend--ok'
})

const trendLabel = computed(() => {
  const pct = projectedOverPct.value
  if (pct > 20) return '超支风险'
  if (pct > 0) return '略偏高'
  return '良好'
})

const trendTip = computed(() => {
  const pct = projectedOverPct.value
  const proj = projectedTotal.value
  const budget = budgetStore.budget.monthly
  if (pct > 20) return `⚠️ 按当前速度月末将超支 ¥${proj - budget}，建议今天只花 ¥${budgetStore.dailySuggestion} 以内`
  if (pct > 0) return `📈 按当前速度月末预计消费 ¥${proj}，略超预算，注意节制`
  return `✅ 消费节奏良好，预计月末总花费约 ¥${proj}`
})

const todayStatusClass = computed(() => {
  const today = budgetStore.monthlyExpenses
    .filter(e => e.date === dayjs().format('YYYY-MM-DD'))
    .reduce((s, e) => s + e.amount, 0)
  return today > budgetStore.dailySuggestion ? 'smart-item--warn' : ''
})

const ringStyle = computed(() => {
  const pct = budgetStore.monthlyPercent
  const deg = (pct / 100) * 360
  const color = pct > 100 ? '#FF4757' : pct > 80 ? '#FFA502' : '#4ECDC4'
  return {
    background: `conic-gradient(${color} ${deg}deg, #f0f0f0 ${deg}deg)`,
  }
})

function mealLabel(type) { return mealTypeLabel(type) }

async function deleteExpense(id) {
  uni.showModal({
    title: '删除消费',
    content: '确认删除这条消费记录？',
    success: async (res) => {
      if (res.confirm) {
        await budgetStore.removeExpense(id)
        uni.showToast({ title: '已删除', icon: 'success' })
      }
    }
  })
}

function saveBudget() {
  const val = Number(monthlyInput.value)
  if (val > 0) {
    budgetStore.setBudget({ ...budgetStore.budget, monthly: val })
    uni.showToast({ title: '预算已更新 ✅', icon: 'none' })
  }
}

// ── 近7天折线图 ───────────────────────────────────────
const weekDays = computed(() => {
  const result = []
  for (let i = 6; i >= 0; i--) {
    const d = dayjs().subtract(i, 'day')
    const dateStr = d.format('YYYY-MM-DD')
    const amount = budgetStore.monthlyExpenses
      .filter(e => e.date === dateStr)
      .reduce((s, e) => s + e.amount, 0)
    result.push({ date: dateStr, label: d.format('M/D'), amount: Math.round(amount) })
  }
  return result
})

const yLabels = computed(() => {
  const amounts = weekDays.value.map(d => d.amount)
  const maxVal = Math.max(...amounts, budgetStore.dailySuggestion, 1)
  const step = Math.ceil(maxVal / 3 / 10) * 10
  return [step * 3, step * 2, step, 0]
})

function drawWeekChart() {
  const days = weekDays.value
  const amounts = days.map(d => d.amount)
  const maxVal = Math.max(...amounts, budgetStore.dailySuggestion || 1, 1)
  const yMax = Math.ceil(maxVal / 10) * 10 * 1.1

  const W = 280, H = 100  // canvas logical px (rpx / 2 approximately)
  const padL = 10, padR = 10, padT = 10, padB = 10
  const chartW = W - padL - padR
  const chartH = H - padT - padB
  const n = days.length

  const ctx = uni.createCanvasContext('weekChartCanvas')
  ctx.clearRect(0, 0, W, H)

  // 网格线
  ctx.setStrokeStyle('#f0f0f0')
  ctx.setLineWidth(1)
  ;[0.25, 0.5, 0.75, 1].forEach(pct => {
    const y = padT + chartH * pct
    ctx.beginPath()
    ctx.moveTo(padL, y)
    ctx.lineTo(W - padR, y)
    ctx.stroke()
  })

  // 日均建议线（虚线）
  if (budgetStore.dailySuggestion > 0) {
    const sy = padT + chartH * (1 - budgetStore.dailySuggestion / yMax)
    ctx.setStrokeStyle('rgba(255,107,107,0.4)')
    ctx.setLineDash([4, 4])
    ctx.beginPath()
    ctx.moveTo(padL, sy)
    ctx.lineTo(W - padR, sy)
    ctx.stroke()
    ctx.setLineDash([])
  }

  // 计算点坐标
  const pts = days.map((d, i) => ({
    x: padL + (i / (n - 1)) * chartW,
    y: padT + chartH * (1 - d.amount / yMax),
  }))

  // 渐变填充区域
  const grad = ctx.createLinearGradient(0, padT, 0, padT + chartH)
  grad.addColorStop(0, 'rgba(255,107,107,0.18)')
  grad.addColorStop(1, 'rgba(255,107,107,0)')
  ctx.setFillStyle(grad)
  ctx.beginPath()
  ctx.moveTo(pts[0].x, pts[0].y)
  for (let i = 1; i < pts.length; i++) {
    const cp1x = (pts[i - 1].x + pts[i].x) / 2
    ctx.bezierCurveTo(cp1x, pts[i - 1].y, cp1x, pts[i].y, pts[i].x, pts[i].y)
  }
  ctx.lineTo(pts[pts.length - 1].x, padT + chartH)
  ctx.lineTo(pts[0].x, padT + chartH)
  ctx.closePath()
  ctx.fill()

  // 折线
  ctx.setStrokeStyle('#FF6B6B')
  ctx.setLineWidth(2)
  ctx.beginPath()
  ctx.moveTo(pts[0].x, pts[0].y)
  for (let i = 1; i < pts.length; i++) {
    const cp1x = (pts[i - 1].x + pts[i].x) / 2
    ctx.bezierCurveTo(cp1x, pts[i - 1].y, cp1x, pts[i].y, pts[i].x, pts[i].y)
  }
  ctx.stroke()

  // 数据点
  pts.forEach((pt, i) => {
    if (days[i].amount > 0) {
      ctx.setFillStyle('#FF6B6B')
      ctx.beginPath()
      ctx.arc(pt.x, pt.y, 3, 0, Math.PI * 2)
      ctx.fill()
    }
  })

  ctx.draw()
}

onMounted(async () => {
  await budgetStore.load()
  monthlyInput.value = String(budgetStore.budget.monthly)
  nextTick(() => drawWeekChart())
})
</script>

<style lang="scss" scoped>
.budget-card {
  display: flex;
  gap: $fd-space-md;
  align-items: center;
}

.budget-ring {
  flex-shrink: 0;
}
.ring {
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  @include fd-flex-center;
}
.ring-inner {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: $fd-card-bg;
  @include fd-flex-column;
  align-items: center;
  justify-content: center;
}
.ring-percent {
  font-size: $fd-font-lg;
  font-weight: 800;
  color: $fd-text;
}
.ring-label {
  font-size: $fd-font-xs;
  color: $fd-text-light;
}

.budget-info { flex: 1; }
.budget-row {
  @include fd-flex-between;
  padding: 6rpx 0;
}
.budget-label {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
}
.budget-value {
  font-size: $fd-font-base;
  font-weight: 600;
  color: $fd-text;
}
.over-budget { color: $fd-danger; }
.budget-suggest {
  font-size: $fd-font-base;
  font-weight: 700;
  color: $fd-accent;
}
.budget-divider {
  height: 2rpx;
  background: $fd-border;
  margin: 8rpx 0;
}

.warning {
  background: rgba($fd-danger, 0.08);
  color: $fd-danger;
  font-size: $fd-font-sm;
  font-weight: 600;
  text-align: center;
}

/* 智能预警卡片 */
.smart-card { padding: $fd-space-base $fd-space-md; }
.smart-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-bottom: $fd-space-sm;
}
.smart-divider { width: 2rpx; height: 60rpx; background: $fd-border; }
.smart-item {
  text-align: center;
  flex: 1;
}
.smart-num {
  display: block;
  font-size: $fd-font-lg;
  font-weight: 700;
  color: $fd-text;
}
.smart-label {
  display: block;
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  margin-top: 4rpx;
}
.smart-item--warn .smart-num { color: $fd-danger; }
.smart-tip {
  border-radius: $fd-radius-sm;
  padding: 16rpx 20rpx;
  margin-top: 4rpx;
}
.smart-tip-text { font-size: $fd-font-sm; line-height: 1.6; }
.trend--ok { background: rgba($fd-accent, 0.1); .smart-tip-text { color: darken(#4ECDC4, 15%); } .smart-num { color: $fd-accent; } }
.trend--warn { background: rgba(#FFA502, 0.1); .smart-tip-text { color: darken(#FFA502, 10%); } .smart-num { color: #FFA502; } }
.trend--danger { background: rgba($fd-danger, 0.1); .smart-tip-text { color: $fd-danger; } .smart-num { color: $fd-danger; } }

.section-title {
  @include fd-title;
  margin-bottom: $fd-space-sm;
  display: block;
}

.set-budget {
  @include fd-flex-column;
  gap: $fd-space-sm;
}
.input-row {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
}
.input-label {
  font-size: $fd-font-base;
  color: $fd-text-secondary;
  width: 120rpx;
}
.input-field {
  flex: 1;
  height: 72rpx;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: 0 $fd-space-sm;
  font-size: $fd-font-base;
}
.input-unit {
  font-size: $fd-font-sm;
  color: $fd-text-light;
}

.expense-item {
  @include fd-flex-between;
  padding: $fd-space-base $fd-space-md;
}
.expense-left {
  @include fd-flex-column;
}
.expense-desc {
  font-size: $fd-font-base;
  font-weight: 600;
  color: $fd-text;
}
.expense-date {
  font-size: $fd-font-xs;
  color: $fd-text-light;
  margin-top: 4rpx;
}
.expense-right {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
}
.expense-amount {
  font-size: $fd-font-base;
  font-weight: 700;
  color: $fd-danger;
}
.expense-delete {
  font-size: 40rpx;
  color: $fd-text-light;
  padding: 0 8rpx;
  &:active { color: $fd-danger; }
}

/* 近7天折线图 */
.week-chart { margin: 0 $fd-space-md $fd-space-base; }
.chart-area {
  display: flex;
  align-items: stretch;
  height: 200rpx;
  margin-bottom: 4rpx;
}
.chart-y-labels {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 8rpx 0;
  width: 64rpx;
  flex-shrink: 0;
}
.chart-y-label {
  font-size: 18rpx;
  color: $fd-text-light;
  text-align: right;
  padding-right: 8rpx;
}
.chart-svg-wrap {
  flex: 1;
}
.chart-canvas {
  width: 100%;
  height: 200rpx;
}
.chart-x-labels {
  display: flex;
  margin-left: 64rpx;
}
.chart-x-label {
  flex: 1;
  text-align: center;
  font-size: 18rpx;
  color: $fd-text-light;
}
.chart-values {
  display: flex;
  margin-left: 64rpx;
  margin-top: 4rpx;
}
.chart-value-item {
  flex: 1;
  text-align: center;
}
.chart-value-num {
  font-size: 18rpx;
  color: $fd-text-light;
  &.chart-value--active { color: $fd-primary; font-weight: 600; }
}
</style>
