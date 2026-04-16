<template>
  <view class="fd-page">
    <fd-nav-bar title="饮食日历" />

    <!-- 月份切换 -->
    <view class="month-bar">
      <view class="month-btn" @tap="prevMonth"><text>◀</text></view>
      <text class="month-title">{{ year }}年{{ month }}月</text>
      <view class="month-btn" @tap="nextMonth"><text>▶</text></view>
    </view>

    <!-- 星期头 -->
    <view class="week-header">
      <text v-for="d in weekDays" :key="d" class="week-day">{{ d }}</text>
    </view>

    <!-- 日历网格 -->
    <view class="calendar-grid">
      <view
        v-for="(day, i) in calendarDays"
        :key="i"
        class="day-cell"
        :class="{
          'day-cell--today': day.isToday,
          'day-cell--empty': !day.date,
          'day-cell--selected': day.date === selectedDate,
        }"
        @tap="day.date && selectDay(day.date)"
      >
        <text v-if="day.date" class="day-num">{{ day.day }}</text>
        <view v-if="day.recordCount > 0" class="day-dot" :class="'day-dot--' + Math.min(day.recordCount, 3)"></view>
      </view>
    </view>

    <!-- 选中日期的记录 -->
    <view class="records-section">
      <text class="records-title">{{ selectedDate }} 用餐记录</text>
      <!-- 骨架屏：数据加载中 -->
      <fd-skeleton v-if="recordLoading" :rows="4" />
      <view v-else-if="dayRecords.length === 0" class="records-empty">
        <fd-empty icon="📝" text="这天还没有记录哦" btn-text="去记录" @action="goRecord" />
      </view>
      <view v-for="record in dayRecords" :key="record.id" class="record-card fd-card">
        <view class="record-header">
          <text class="record-meal">{{ mealLabel(record.mealType) }}</text>
          <view class="record-header-right">
            <text class="record-cost">¥{{ record.cost }}</text>
            <text class="record-del" @tap="deleteRecord(record.id)">×</text>
          </view>
        </view>
        <text class="record-food">{{ record.foodName }}</text>
        <view v-if="record.nutrition" class="record-tags">
          <text v-for="n in record.nutrition" :key="n" class="fd-tag--accent">{{ nutritionLabel(n) }}</text>
        </view>
        <!-- 星级评价 -->
        <view class="record-rating">
          <text class="record-rating-label">评价</text>
          <view class="stars">
            <text
              v-for="s in 5"
              :key="s"
              class="star"
              :class="{ 'star--active': s <= (record.rating || 0) }"
              @tap="rateRecord(record.id, s)"
            >★</text>
          </view>
          <text v-if="record.rating" class="rating-text">{{ ratingText(record.rating) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRecordStore } from '@/stores/modules/record'
import { today, mealTypeLabel, getDaysInMonth, getFirstDayOfWeek } from '@/utils/date'
import { NUTRITION_TYPES } from '@/config/constants'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdEmpty from '@/components/common/fd-empty.vue'
import FdSkeleton from '@/components/common/fd-skeleton.vue'

const recordStore = useRecordStore()
const recordLoading = ref(true)

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const selectedDate = ref(today())
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const calendarDays = computed(() => {
  const days = getDaysInMonth(year.value, month.value)
  const firstDay = getFirstDayOfWeek(year.value, month.value)
  const todayStr = today()
  const result = []

  for (let i = 0; i < firstDay; i++) {
    result.push({ date: null, day: '', isToday: false, recordCount: 0 })
  }

  const monthStr = String(month.value).padStart(2, '0')
  for (let d = 1; d <= days; d++) {
    const dateStr = `${year.value}-${monthStr}-${String(d).padStart(2, '0')}`
    const recordCount = recordStore.getByDate(dateStr).length
    result.push({
      date: dateStr,
      day: d,
      isToday: dateStr === todayStr,
      recordCount,
    })
  }
  return result
})

const dayRecords = computed(() => {
  return recordStore.getByDate(selectedDate.value)
})

function mealLabel(type) { return mealTypeLabel(type) }

function nutritionLabel(key) {
  const found = NUTRITION_TYPES.find((n) => n.key === key)
  return found ? found.label : key
}

function selectDay(date) { selectedDate.value = date }

async function rateRecord(id, score) {
  await recordStore.rate(id, score)
  uni.showToast({ title: ratingText(score), icon: 'none', duration: 1000 })
}

const RATING_TEXTS = ['', '难吃', '一般', '还不错', '很好吃', '超级棒！']
function ratingText(score) { return RATING_TEXTS[score] || '' }

async function deleteRecord(id) {
  uni.showModal({
    title: '删除记录',
    content: '确认删除这条用餐记录？',
    success: async (res) => {
      if (res.confirm) {
        await recordStore.remove(id)
        uni.showToast({ title: '已删除', icon: 'success' })
      }
    }
  })
}

function prevMonth() {
  if (month.value === 1) { year.value--; month.value = 12 }
  else month.value--
}
function nextMonth() {
  if (month.value === 12) { year.value++; month.value = 1 }
  else month.value++
}
function goRecord() {
  uni.switchTab({ url: '/pages/index/index' })
}

onMounted(async () => {
  await recordStore.load()
  recordLoading.value = false
})
</script>

<style lang="scss" scoped>
.month-bar {
  @include fd-flex-center;
  padding: $fd-space-base;
  gap: $fd-space-lg;
}
.month-btn {
  @include fd-flex-center;
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: $fd-card-bg;
  box-shadow: $fd-shadow;
  &:active { opacity: 0.7; }
}
.month-title {
  font-size: $fd-font-lg;
  font-weight: 700;
  color: $fd-text;
}

.week-header {
  display: flex;
  padding: 0 $fd-space-sm;
}
.week-day {
  flex: 1;
  text-align: center;
  font-size: $fd-font-sm;
  color: $fd-text-light;
  padding: $fd-space-xs 0;
}

.calendar-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 0 $fd-space-sm;
}
.day-cell {
  width: calc(100% / 7);
  aspect-ratio: 1;
  @include fd-flex-column;
  align-items: center;
  justify-content: center;
  position: relative;

  &--today {
    .day-num {
      background: $fd-primary;
      color: #fff;
      border-radius: 50%;
      width: 56rpx;
      height: 56rpx;
      line-height: 56rpx;
      text-align: center;
    }
  }
  &--selected:not(&--today) {
    .day-num {
      background: rgba($fd-primary, 0.12);
      border-radius: 50%;
      width: 56rpx;
      height: 56rpx;
      line-height: 56rpx;
      text-align: center;
    }
  }
}
.day-num {
  font-size: $fd-font-base;
  color: $fd-text;
}
.day-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: rgba($fd-primary, 0.3);
  margin-top: 4rpx;
  &--1 { background: rgba($fd-primary, 0.4); }
  &--2 { background: rgba($fd-primary, 0.7); }
  &--3 { background: $fd-primary; width: 16rpx; height: 16rpx; }
}

.records-section {
  padding: $fd-space-base $fd-space-md;
}
.records-title {
  font-size: $fd-font-md;
  font-weight: 700;
  color: $fd-text;
  margin-bottom: $fd-space-sm;
  display: block;
}

.record-card {
  margin-bottom: $fd-space-sm;
}
.record-header {
  @include fd-flex-between;
  margin-bottom: $fd-space-xs;
}
.record-meal {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
}
.record-header-right {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
}
.record-cost {
  font-size: $fd-font-base;
  font-weight: 700;
  color: $fd-primary;
}
.record-del {
  font-size: 40rpx;
  color: $fd-text-light;
  padding: 0 8rpx;
  &:active { color: $fd-danger; }
}
.record-food {
  font-size: $fd-font-md;
  font-weight: 600;
  color: $fd-text;
  display: block;
  margin-bottom: $fd-space-xs;
}
.record-tags {
  display: flex;
  gap: $fd-space-xs;
}

/* 星级评价 */
.record-rating {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
  margin-top: $fd-space-xs;
  padding-top: $fd-space-xs;
  border-top: 2rpx solid $fd-border;
}
.record-rating-label {
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  flex-shrink: 0;
}
.stars { display: flex; gap: 4rpx; }
.star {
  font-size: 36rpx;
  color: $fd-border;
  &--active { color: #FFB800; }
  &:active { opacity: 0.7; }
}
.rating-text {
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
}
</style>
