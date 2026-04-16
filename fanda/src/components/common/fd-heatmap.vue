<template>
  <!-- 热力图：展示过去 N 周的打卡密度，仿 GitHub contribution graph -->
  <view class="fd-heatmap">
    <view class="heatmap-header">
      <text class="heatmap-title">{{ title }}</text>
      <view class="heatmap-legend">
        <text class="legend-label">少</text>
        <view v-for="l in 4" :key="l" class="legend-cell" :class="'heat-' + l" />
        <text class="legend-label">多</text>
      </view>
    </view>

    <!-- 星期标签 -->
    <view class="heatmap-week-labels">
      <text v-for="d in weekLabels" :key="d" class="week-label">{{ d }}</text>
    </view>

    <!-- 格子网格（按列排列，每列 7 天） -->
    <scroll-view scroll-x class="heatmap-scroll">
      <view class="heatmap-grid">
        <view v-for="(col, ci) in columns" :key="ci" class="heatmap-col">
          <view
            v-for="(cell, ri) in col"
            :key="ri"
            class="heatmap-cell"
            :class="cell.date ? 'heat-' + cell.level : 'heat-empty'"
            @tap="cell.date && onCellTap(cell)"
          />
        </view>
      </view>
    </scroll-view>

    <!-- 总计行 -->
    <view class="heatmap-summary">
      <text class="summary-text">过去 {{ weeks }} 周共打卡 <text class="summary-num">{{ totalDays }}</text> 天</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  /** 每个日期对应的计数 { '2026-04-01': 3 } */
  countMap:  { type: Object, default: () => ({}) },
  /** 展示多少周 */
  weeks:     { type: Number, default: 12 },
  title:     { type: String, default: '打卡热力图' },
})

const emit = defineEmits(['cell-tap'])

const weekLabels = ['日', '一', '二', '三', '四', '五', '六']

/** 将 count 映射到热度等级 0-4 */
function toLevel(count) {
  if (!count || count === 0) return 0
  if (count === 1) return 1
  if (count === 2) return 2
  if (count <= 4) return 3
  return 4
}

/**
 * 构建列数组：每列 7 个格子（日～六），从最旧的一周到最新的一周
 * 最后一列包含今天，不足的格子用空占位
 */
const columns = computed(() => {
  const today = dayjs()
  const todayDow = today.day() // 0=Sun … 6=Sat

  // 从今天往回推 (weeks*7 - 1) 天，得到起始点
  const totalCells = props.weeks * 7
  // 起始格：让今天落在最后一列的 todayDow 行
  const startDate = today.subtract(totalCells - 1, 'day')

  const cols = []
  for (let c = 0; c < props.weeks; c++) {
    const col = []
    for (let r = 0; r < 7; r++) {
      const idx = c * 7 + r
      const d = startDate.add(idx, 'day')
      const dateStr = d.format('YYYY-MM-DD')
      const count = props.countMap[dateStr] || 0
      col.push({
        date: dateStr,
        count,
        level: toLevel(count),
        isToday: dateStr === today.format('YYYY-MM-DD'),
      })
    }
    cols.push(col)
  }
  return cols
})

const totalDays = computed(() => {
  return Object.values(props.countMap).filter(v => v > 0).length
})

function onCellTap(cell) {
  if (cell.count > 0) emit('cell-tap', cell)
}
</script>

<style lang="scss" scoped>
.fd-heatmap {
  @include fd-flex-column;
  gap: $fd-space-xs;
}

.heatmap-header {
  @include fd-flex-between;
  align-items: center;
}
.heatmap-title {
  font-size: $fd-font-base;
  font-weight: 700;
  color: $fd-text;
}
.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 4rpx;
}
.legend-label {
  font-size: $fd-font-xs;
  color: $fd-text-light;
}

.heatmap-week-labels {
  display: flex;
  flex-direction: column;
  position: absolute;
  left: 0;
  /* 覆盖在格子左侧，仅展示效果 */
}
.week-label {
  font-size: 18rpx;
  color: $fd-text-light;
  height: 28rpx;
  line-height: 28rpx;
  text-align: center;
  width: 28rpx;
}

.heatmap-scroll {
  width: 100%;
}
.heatmap-grid {
  display: flex;
  gap: 4rpx;
  padding: 4rpx 0;
}
.heatmap-col {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

$cell: 26rpx;
.heatmap-cell {
  width: $cell;
  height: $cell;
  border-radius: 4rpx;
  &:active { opacity: 0.7; }
}

.legend-cell {
  width: 20rpx;
  height: 20rpx;
  border-radius: 3rpx;
}

/* 热度色阶 */
.heat-empty { background: $fd-border; opacity: 0.4; }
.heat-0     { background: $fd-border; opacity: 0.4; }
.heat-1     { background: rgba($fd-primary, 0.25); }
.heat-2     { background: rgba($fd-primary, 0.45); }
.heat-3     { background: rgba($fd-primary, 0.70); }
.heat-4     { background: $fd-primary; }

.heatmap-summary {
  text-align: center;
  padding-top: 4rpx;
}
.summary-text {
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
}
.summary-num {
  color: $fd-primary;
  font-weight: 700;
}
</style>
