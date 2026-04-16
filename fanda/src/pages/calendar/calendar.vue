<template>
  <view class="fd-page">
    <fd-nav-bar title="饮食日历" />

    <!-- 营养均衡提醒 -->
    <view v-if="nutritionWarning" class="nutrition-tip">
      <text class="nutrition-tip__icon">🥗</text>
      <text class="nutrition-tip__text">{{ nutritionWarning }}</text>
    </view>

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
    <view
      class="calendar-grid"
      @touchstart="onTouchStart"
      @touchend="onTouchEnd"
    >
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
      <view class="records-title-row">
        <text class="records-title">{{ selectedDate }} 用餐记录</text>
        <view class="add-record-btn" @tap="openAddRecord">
          <text>+ 记录</text>
        </view>
      </view>
      <!-- 骨架屏：数据加载中 -->
      <fd-skeleton v-if="recordLoading" :rows="4" />
      <view v-else-if="dayRecords.length === 0" class="records-empty">
        <fd-empty icon="📝" text="这天还没有记录哦" btn-text="去记录" @action="openAddRecord" />
      </view>
      <view v-for="record in dayRecords" :key="record.id" class="record-card fd-card" @longpress="openRecordMenu(record)">
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

    <!-- 快速录入弹窗 -->
    <fd-modal v-model:visible="showAddRecord" title="记录用餐" @confirm="submitRecord">
      <!-- 选择食物（带搜索） -->
      <view class="form-item">
        <text class="form-label">食物</text>
        <view class="food-select-btn" @tap="showFoodPicker = true">
          <text :class="selectedFood ? 'food-select-name' : 'food-select-placeholder'">
            {{ selectedFood ? selectedFood.name : '点击选择食物…' }}
          </text>
          <text class="food-select-arrow">›</text>
        </view>
      </view>
      <!-- 餐次 -->
      <view class="form-item">
        <text class="form-label">餐次</text>
        <view class="meal-types">
          <view
            v-for="mt in MEAL_TYPES"
            :key="mt.key"
            class="meal-type-btn"
            :class="{ 'meal-type-btn--active': addForm.mealType === mt.key }"
            @tap="addForm.mealType = mt.key"
          >
            <text>{{ mt.label }}</text>
          </view>
        </view>
      </view>
      <!-- 花费 -->
      <view class="form-item">
        <text class="form-label">花费（元）</text>
        <input class="form-input" type="digit" v-model="addForm.cost" placeholder="0" />
      </view>
      <!-- 营养标签 -->
      <view class="form-item">
        <text class="form-label">营养标签（多选）</text>
        <view class="nutrition-tags">
          <view
            v-for="n in NUTRITION_TYPES"
            :key="n.key"
            class="nutrition-tag"
            :class="{ 'nutrition-tag--active': addForm.nutrition.includes(n.key) }"
            @tap="toggleNutrition(n.key)"
          >
            <text>{{ n.label }}</text>
          </view>
        </view>
      </view>
    </fd-modal>

    <!-- 食物选择器 -->
    <fd-food-picker
      v-model:visible="showFoodPicker"
      v-model="addForm.foodId"
      :meal-time="addForm.mealType"
      @select="onFoodSelected"
    />

    <!-- 长按快捷操作菜单 -->
    <view v-if="menuRecord" class="record-menu-mask" @tap="menuRecord = null">
      <view class="record-menu" @tap.stop>
        <text class="record-menu-title">{{ menuRecord.foodName }}</text>
        <view class="record-menu-item" @tap="menuDelete">
          <text class="record-menu-icon">🗑️</text>
          <text class="record-menu-label">删除记录</text>
        </view>
        <view class="record-menu-item" @tap="menuRatePanel">
          <text class="record-menu-icon">⭐</text>
          <text class="record-menu-label">快速评分</text>
        </view>
        <view class="record-menu-item" @tap="menuCopyToday">
          <text class="record-menu-icon">📋</text>
          <text class="record-menu-label">复制到今天</text>
        </view>
        <view class="record-menu-cancel" @tap="menuRecord = null">
          <text>取消</text>
        </view>
      </view>
    </view>

    <!-- 快速评分弹窗 -->
    <view v-if="showRatePanel" class="record-menu-mask" @tap="showRatePanel = false">
      <view class="record-menu" @tap.stop>
        <text class="record-menu-title">为「{{ menuRecord && menuRecord.foodName }}」评分</text>
        <view class="quick-stars">
          <text
            v-for="s in 5"
            :key="s"
            class="quick-star"
            :class="{ 'quick-star--active': s <= quickRating }"
            @tap="quickRating = s"
          >★</text>
        </view>
        <text class="quick-rating-text">{{ ratingText(quickRating) }}</text>
        <view class="record-menu-item record-menu-confirm" @tap="submitQuickRate">
          <text>确认评分</text>
        </view>
        <view class="record-menu-cancel" @tap="showRatePanel = false">
          <text>取消</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRecordStore } from '@/stores/modules/record'
import { useFoodStore } from '@/stores/modules/food'
import { today, mealTypeLabel, getDaysInMonth, getFirstDayOfWeek } from '@/utils/date'
import { NUTRITION_TYPES, MEAL_TYPES } from '@/config/constants'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdEmpty from '@/components/common/fd-empty.vue'
import FdSkeleton from '@/components/common/fd-skeleton.vue'
import FdModal from '@/components/common/fd-modal.vue'
import FdFoodPicker from '@/components/common/fd-food-picker.vue'

const recordStore = useRecordStore()
const foodStore = useFoodStore()
const recordLoading = ref(true)

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const selectedDate = ref(today())
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

// 滑动手势切换月份
let touchStartX = 0
function onTouchStart(e) {
  touchStartX = e.touches[0].clientX
}
function onTouchEnd(e) {
  const delta = e.changedTouches[0].clientX - touchStartX
  if (delta < -50) nextMonth()
  else if (delta > 50) prevMonth()
}

// 快速录入表单
const showAddRecord = ref(false)
const showFoodPicker = ref(false)
const selectedFood = ref(null)
const addForm = ref({ foodId: null, mealType: 'lunch', cost: '', nutrition: [] })

function openAddRecord() {
  // 根据时间自动选择餐次
  const h = new Date().getHours()
  addForm.value.mealType = h < 10 ? 'breakfast' : h < 15 ? 'lunch' : 'dinner'
  showAddRecord.value = true
}

function onFoodSelected(food) {
  selectedFood.value = food
  addForm.value.foodId = food.id
  // 自动填充该食物的营养标签
  if (food.nutrition?.length) {
    addForm.value.nutrition = [...food.nutrition]
  }
  // 自动填充价格中值
  if (!addForm.value.cost && food.priceRange) {
    addForm.value.cost = String(Math.round((food.priceRange[0] + food.priceRange[1]) / 2))
  }
}

function toggleNutrition(key) {
  const idx = addForm.value.nutrition.indexOf(key)
  if (idx >= 0) addForm.value.nutrition.splice(idx, 1)
  else addForm.value.nutrition.push(key)
}

async function submitRecord() {
  if (!selectedFood.value) {
    uni.showToast({ title: '请选择食物', icon: 'none' })
    return
  }
  await recordStore.add({
    foodId: selectedFood.value.id,
    foodName: selectedFood.value.name,
    date: selectedDate.value,
    mealType: addForm.value.mealType,
    cost: Number(addForm.value.cost) || 0,
    nutrition: addForm.value.nutrition,
  })
  // 重置表单
  selectedFood.value = null
  addForm.value = { foodId: null, mealType: 'lunch', cost: '', nutrition: [] }
  uni.showToast({ title: '已记录 ✅', icon: 'none' })
}

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

// 营养均衡提醒：检查近 3 天是否缺少蔬菜/水果
const nutritionWarning = computed(() => {
  const now = new Date()
  const days = [0, 1, 2].map((i) => {
    const d = new Date(now.getTime() - i * 86400000)
    return d.toISOString().slice(0, 10)
  })
  const VEG_KEYS = ['vegetable', 'veg', '蔬菜', 'fruit', '水果']
  const hasVegOrFruit = days.some((date) => {
    const recs = recordStore.getByDate(date)
    return recs.some((r) =>
      (r.nutrition || []).some((n) => VEG_KEYS.some((k) => n.toLowerCase().includes(k)))
    )
  })
  if (!hasVegOrFruit && recordStore.records.length > 0) {
    return '近 3 天没有蔬菜或水果记录，注意营养均衡哦'
  }
  return null
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
  openAddRecord()
}

// 长按快捷菜单
const menuRecord = ref(null)
const showRatePanel = ref(false)
const quickRating = ref(0)

function openRecordMenu(record) {
  menuRecord.value = record
  quickRating.value = record.rating || 0
  showRatePanel.value = false
}

function menuDelete() {
  const id = menuRecord.value?.id
  menuRecord.value = null
  if (id) deleteRecord(id)
}

function menuRatePanel() {
  showRatePanel.value = true
}

async function submitQuickRate() {
  if (!menuRecord.value || !quickRating.value) return
  await rateRecord(menuRecord.value.id, quickRating.value)
  showRatePanel.value = false
  menuRecord.value = null
}

async function menuCopyToday() {
  const rec = menuRecord.value
  menuRecord.value = null
  if (!rec) return
  const todayStr = today()
  if (rec.date === todayStr) {
    uni.showToast({ title: '已经是今天的记录', icon: 'none' })
    return
  }
  await recordStore.add({
    foodId: rec.foodId,
    foodName: rec.foodName,
    date: todayStr,
    mealType: rec.mealType,
    cost: rec.cost || 0,
    nutrition: rec.nutrition || [],
  })
  uni.showToast({ title: '已复制到今天 ✅', icon: 'none' })
}

onMounted(async () => {
  await Promise.all([recordStore.load(), foodStore.load()])
  recordLoading.value = false
})
</script>

<style lang="scss" scoped>
.nutrition-tip {
  display: flex;
  align-items: center;
  gap: $fd-space-xs;
  margin: $fd-space-sm $fd-space-md 0;
  padding: 16rpx 20rpx;
  background: rgba($fd-success, 0.08);
  border-radius: $fd-radius;
  border-left: 6rpx solid $fd-success;
  &__icon { font-size: 32rpx; }
  &__text { font-size: $fd-font-sm; color: darken(#4caf50, 10%); flex: 1; }
}

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

.records-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $fd-space-sm;
}
.records-title {
  font-size: $fd-font-md;
  font-weight: 700;
  color: $fd-text;
}
.add-record-btn {
  background: $fd-primary;
  color: #fff;
  font-size: $fd-font-xs;
  font-weight: 600;
  padding: 8rpx 24rpx;
  border-radius: $fd-radius-round;
  &:active { opacity: 0.85; }
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

/* 快速录入表单 */
.food-select-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: 0 $fd-space-sm;
  height: 72rpx;
  background: $fd-bg;
  &:active { border-color: $fd-primary; }
}
.food-select-name {
  font-size: $fd-font-base;
  color: $fd-text;
  font-weight: 500;
}
.food-select-placeholder {
  font-size: $fd-font-base;
  color: $fd-text-light;
}
.food-select-arrow {
  font-size: $fd-font-lg;
  color: $fd-text-light;
}
.form-item {
  margin-bottom: $fd-space-base;
}
.form-label {
  display: block;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  margin-bottom: 8rpx;
}
.form-input {
  width: 100%;
  height: 72rpx;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: 0 $fd-space-sm;
  font-size: $fd-font-base;
}
.meal-types {
  display: flex;
  gap: $fd-space-xs;
}
.meal-type-btn {
  flex: 1;
  text-align: center;
  padding: 12rpx 0;
  border-radius: $fd-radius-round;
  border: 2rpx solid $fd-border;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  &--active {
    background: $fd-primary;
    color: #fff;
    border-color: $fd-primary;
  }
  &:active { opacity: 0.8; }
}
.nutrition-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $fd-space-xs;
}
.nutrition-tag {
  padding: 8rpx 24rpx;
  border-radius: $fd-radius-round;
  border: 2rpx solid $fd-border;
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  &--active {
    background: rgba($fd-accent, 0.15);
    border-color: $fd-accent;
    color: $fd-accent;
    font-weight: 600;
  }
  &:active { opacity: 0.8; }
}

/* 长按菜单 */
.record-menu-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.record-menu {
  width: 100%;
  background: $fd-card-bg;
  border-radius: $fd-radius $fd-radius 0 0;
  padding: $fd-space-base $fd-space-md;
  padding-bottom: env(safe-area-inset-bottom, 0);
}
.record-menu-title {
  display: block;
  text-align: center;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  padding: $fd-space-xs 0 $fd-space-base;
  border-bottom: 2rpx solid $fd-border;
  margin-bottom: $fd-space-xs;
}
.record-menu-item {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
  padding: $fd-space-base $fd-space-sm;
  border-radius: $fd-radius-sm;
  &:active { background: $fd-bg; }
}
.record-menu-icon { font-size: 40rpx; }
.record-menu-label {
  font-size: $fd-font-base;
  color: $fd-text;
}
.record-menu-confirm {
  justify-content: center;
  background: $fd-primary;
  color: #fff;
  border-radius: $fd-radius-round;
  margin-top: $fd-space-sm;
  .record-menu-label { color: #fff; font-weight: 700; }
}
.record-menu-cancel {
  text-align: center;
  padding: $fd-space-base;
  color: $fd-text-secondary;
  font-size: $fd-font-base;
  &:active { opacity: 0.7; }
}
.quick-stars {
  display: flex;
  justify-content: center;
  gap: $fd-space-sm;
  padding: $fd-space-base 0 $fd-space-xs;
}
.quick-star {
  font-size: 64rpx;
  color: $fd-border;
  &--active { color: #FFB800; }
  &:active { opacity: 0.7; }
}
.quick-rating-text {
  display: block;
  text-align: center;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  margin-bottom: $fd-space-sm;
  min-height: 36rpx;
}
</style>
