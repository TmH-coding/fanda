<template>
  <view class="fd-page">
    <fd-nav-bar title="饭搭">
      <template #right>
        <text class="header-meal">{{ mealLabel }}</text>
      </template>
    </fd-nav-bar>

    <!-- 问候语 -->
    <view class="greeting">
      <text class="greeting__text">{{ greetingText }}</text>
      <text class="greeting__sub">{{ mealLabel }}吃什么？转一转就知道了！</text>
    </view>

    <!-- 今日总结卡片（晚8点后有记录时展示） -->
    <view v-if="showDaySummary" class="day-summary fd-card">
      <view class="day-summary__header">
        <text class="day-summary__title">今日饮食总结 🌙</text>
        <text class="day-summary__date">{{ today() }}</text>
      </view>
      <view class="day-summary__stats">
        <view class="day-stat">
          <text class="day-stat__num">{{ daySummary.records.length }}</text>
          <text class="day-stat__label">餐次</text>
        </view>
        <view class="day-stat__divider" />
        <view class="day-stat">
          <text class="day-stat__num">¥{{ daySummary.totalCost }}</text>
          <text class="day-stat__label">今日花费</text>
        </view>
        <view class="day-stat__divider" />
        <view class="day-stat">
          <text class="day-stat__num">{{ daySummary.nutritionCount }}</text>
          <text class="day-stat__label">营养类型</text>
        </view>
      </view>
      <view class="day-summary__foods">
        <text class="day-summary__foods-label">今天吃了</text>
        <text class="day-summary__foods-list">{{ daySummary.foods.join('、') }}</text>
      </view>
      <view class="day-summary__tip" :class="budgetStore.isOverBudget ? 'tip--warn' : 'tip--ok'">
        <text>{{ budgetStore.isOverBudget ? '⚠️ 今日消费超出预算，明天注意节制哦' : '✅ 预算控制良好，继续保持！' }}</text>
      </view>
    </view>

    <!-- 转盘无候选时提示 -->
    <view v-if="candidates.length === 0 && !isSpinning" class="empty-hint fd-card">
      <text class="empty-hint__icon">🤔</text>
      <text class="empty-hint__text">没有合适的菜了</text>
      <text class="empty-hint__sub">试试清除排除标签，或调整偏好设置</text>
      <view class="empty-hint__btn" @tap="excludedCategories = []">
        <text>清除排除 🗑️</text>
      </view>
    </view>

    <!-- 转盘 -->
    <view v-if="!hasResult && candidates.length > 0" class="wheel-section">
      <fd-wheel
        :items="wheelItems"
        :spinning="isSpinning"
        :target-index="targetIndex"
        @spin="onSpin"
        @spin-end="onSpinEnd"
      />
      <view class="wheel-hint">
        <text class="fd-text-light">👆 点击中间按钮开始</text>
      </view>
    </view>

    <!-- 推荐结果 -->
    <fd-food-result
      v-if="hasResult && currentFood"
      :food="currentFood"
      :is-favorite="isFav"
      :reason="recommendReason"
      @confirm="onConfirm"
      @reroll="onReroll"
      @toggle-favorite="onToggleFav"
      @blacklist="onBlacklist"
    />

    <!-- 排除标签 -->
    <fd-exclude-tags
      :excluded="excludedCategories"
      @toggle="toggleExclude"
      @clear-all="excludedCategories = []"
    />

    <!-- 候选数量 -->
    <view class="candidate-info">
      <text class="fd-text-light">当前可选菜品 {{ candidates.length }} 个</text>
    </view>

    <!-- 浮动手动记录按钮 -->
    <view class="float-record-btn" @tap="showManualRecord = true">
      <text class="float-record-icon">✏️</text>
      <text class="float-record-label">手动记录</text>
    </view>

    <!-- 连续打卡徽章 -->
    <view v-if="streakBadge" class="streak-badge" @tap="dismissStreakBadge">
      <text class="streak-badge__icon">🔥</text>
      <text class="streak-badge__text">{{ streakBadge }}</text>
      <text class="streak-badge__close">×</text>
    </view>

    <!-- 手动记录弹窗 -->
    <fd-modal v-model:visible="showManualRecord" title="手动记录用餐" @confirm="submitManualRecord">
      <view class="form-item">
        <text class="form-label">食物</text>
        <view class="food-select-btn" @tap="showManualPicker = true">
          <text :class="manualFood ? 'food-select-name' : 'food-select-placeholder'">
            {{ manualFood ? manualFood.name : '点击选择食物…' }}
          </text>
          <text class="food-select-arrow">›</text>
        </view>
      </view>
      <view class="form-item">
        <text class="form-label">餐次</text>
        <view class="meal-types">
          <view
            v-for="mt in MEAL_TYPES"
            :key="mt.key"
            class="meal-type-btn"
            :class="{ 'meal-type-btn--active': manualForm.mealType === mt.key }"
            @tap="manualForm.mealType = mt.key"
          >
            <text>{{ mt.label }}</text>
          </view>
        </view>
      </view>
      <view class="form-item">
        <text class="form-label">花费（元）</text>
        <input class="form-input" type="digit" v-model="manualForm.cost" placeholder="0" />
      </view>
    </fd-modal>

    <!-- 食物选择器（手动记录） -->
    <fd-food-picker
      v-model:visible="showManualPicker"
      v-model="manualForm.foodId"
      :meal-time="manualForm.mealType"
      @select="onManualFoodSelected"
    />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRecommend } from '@/composables/useRecommend'
import { usePreferenceStore } from '@/stores/modules/preference'
import { useBudgetStore } from '@/stores/modules/budget'
import { useRecordStore } from '@/stores/modules/record'
import { useFoodStore } from '@/stores/modules/food'
import { mealTypeLabel, today } from '@/utils/date'
import { MEAL_TYPES } from '@/config/constants'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdWheel from '@/components/recommend/fd-wheel.vue'
import FdFoodResult from '@/components/recommend/fd-food-result.vue'
import FdExcludeTags from '@/components/recommend/fd-exclude-tags.vue'
import FdModal from '@/components/common/fd-modal.vue'
import FdFoodPicker from '@/components/common/fd-food-picker.vue'

const {
  excludedCategories,
  currentFood,
  recommendReason,
  isSpinning,
  hasResult,
  mealType,
  candidates,
  wheelItems,
  spin,
  onSpinEnd,
  toggleExclude,
  confirmChoice,
  reroll,
  init,
} = useRecommend()

const prefStore = usePreferenceStore()
const budgetStore = useBudgetStore()
const recordStore = useRecordStore()
const foodStore = useFoodStore()
const targetIndex = ref(0)

// 连续打卡徽章
const streakBadge = ref('')
function dismissStreakBadge() { streakBadge.value = '' }

// 手动记录
const showManualRecord = ref(false)
const showManualPicker = ref(false)
const manualFood = ref(null)
const manualForm = ref({ foodId: null, mealType: 'lunch', cost: '' })

function onManualFoodSelected(food) {
  manualFood.value = food
  manualForm.value.foodId = food.id
  if (!manualForm.value.cost && food.priceRange) {
    manualForm.value.cost = String(Math.round((food.priceRange[0] + food.priceRange[1]) / 2))
  }
}

async function submitManualRecord() {
  if (!manualFood.value) {
    uni.showToast({ title: '请选择食物', icon: 'none' })
    return
  }
  await recordStore.add({
    foodId: manualFood.value.id,
    foodName: manualFood.value.name,
    date: today(),
    mealType: manualForm.value.mealType,
    cost: Number(manualForm.value.cost) || 0,
    nutrition: manualFood.value.nutrition || [],
  })
  manualFood.value = null
  manualForm.value = { foodId: null, mealType: 'lunch', cost: '' }
  uni.showToast({ title: '已记录 ✅', icon: 'none' })
}

// 今日总结：晚上20点后且当天有记录时展示
const showDaySummary = computed(() => {
  const hour = new Date().getHours()
  if (hour < 20) return false
  return recordStore.todayRecords.length > 0 && !hasResult.value
})

const daySummary = computed(() => {
  const records = recordStore.todayRecords
  const totalCost = records.reduce((s, r) => s + (r.cost || 0), 0)
  const foods = records.map(r => r.foodName).filter(Boolean)
  const nutritionSet = new Set(records.flatMap(r => r.nutrition || []))
  return { records, totalCost, foods, nutritionCount: nutritionSet.size }
})

const mealLabel = computed(() => mealTypeLabel(mealType.value))

const greetingText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 10) return '早上好 ☀️'
  if (hour < 14) return '中午好 🌤️'
  if (hour < 18) return '下午好 🌅'
  return '晚上好 🌙'
})

const isFav = computed(() => {
  if (!currentFood.value) return false
  return prefStore.isFavorite(currentFood.value.id)
})

function onSpin() {
  spin()
  if (currentFood.value && wheelItems.value.length > 0) {
    targetIndex.value = wheelItems.value.findIndex((item) => item.id === currentFood.value.id)
    if (targetIndex.value < 0) targetIndex.value = 0
  }
}

async function onConfirm(cost) {
  await confirmChoice(cost)
  if (budgetStore.isOverBudget) {
    uni.showToast({ title: '⚠️ 本月预算已超支！', icon: 'none', duration: 2500 })
  } else {
    uni.showToast({ title: '已记录 ✅', icon: 'none' })
  }
}

function onReroll() {
  reroll()
}

function onToggleFav(foodId) {
  prefStore.toggleFavorite(foodId)
}

function onBlacklist(foodId) {
  prefStore.toggleBlacklist(foodId)
  reroll()
  uni.showToast({ title: '已加入黑名单，换一个！', icon: 'none' })
}

onMounted(async () => {
  await init()
  await foodStore.load()
  // 连续打卡提醒
  const streak = recordStore.streak
  if (streak >= 3) {
    streakBadge.value = `已连续打卡 ${streak} 天，太棒了！`
  } else if (streak === 0 && recordStore.records.length > 0) {
    // 曾经有记录但昨天没打卡（断签）
    streakBadge.value = '昨天没有记录，加油补上！'
  }
})
</script>

<style lang="scss" scoped>
.day-summary {
  margin: $fd-space-md;
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $fd-space-base;
  }
  &__title {
    font-size: $fd-font-md;
    font-weight: 700;
    color: $fd-text;
  }
  &__date {
    font-size: $fd-font-xs;
    color: $fd-text-light;
  }
  &__stats {
    display: flex;
    align-items: center;
    justify-content: space-around;
    padding: $fd-space-base 0;
    border-top: 2rpx solid $fd-border;
    border-bottom: 2rpx solid $fd-border;
    margin-bottom: $fd-space-base;
  }
  &__foods {
    display: flex;
    gap: $fd-space-sm;
    align-items: baseline;
    margin-bottom: $fd-space-base;
    flex-wrap: wrap;
  }
  &__foods-label {
    font-size: $fd-font-sm;
    color: $fd-text-secondary;
    flex-shrink: 0;
  }
  &__foods-list {
    font-size: $fd-font-sm;
    color: $fd-text;
    font-weight: 600;
  }
  &__tip {
    border-radius: $fd-radius-sm;
    padding: 16rpx 20rpx;
    font-size: $fd-font-sm;
    font-weight: 500;
    &.tip--ok {
      background: rgba($fd-success, 0.1);
      color: $fd-success;
    }
    &.tip--warn {
      background: rgba($fd-danger, 0.08);
      color: $fd-danger;
    }
  }
}

.day-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  &__num {
    font-size: $fd-font-lg;
    font-weight: 800;
    color: $fd-primary;
  }
  &__label {
    font-size: $fd-font-xs;
    color: $fd-text-secondary;
  }
  &__divider {
    width: 2rpx;
    height: 60rpx;
    background: $fd-border;
  }
}

.empty-hint {
  margin: $fd-space-md;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $fd-space-xl $fd-space-md;
  gap: $fd-space-sm;
  &__icon { font-size: 80rpx; }
  &__text { font-size: $fd-font-lg; font-weight: 700; color: $fd-text; }
  &__sub { font-size: $fd-font-sm; color: $fd-text-secondary; text-align: center; }
  &__btn {
    margin-top: $fd-space-sm;
    @include fd-btn;
    padding: 16rpx 40rpx;
    font-size: $fd-font-sm;
  }
}

.header-meal {
  font-size: $fd-font-sm;
  color: $fd-primary;
  background: rgba($fd-primary, 0.1);
  padding: 4rpx 16rpx;
  border-radius: $fd-radius-round;
}

.greeting {
  padding: $fd-space-md $fd-space-md 0;
  &__text {
    font-size: $fd-font-xl;
    font-weight: 800;
    color: $fd-text;
    display: block;
  }
  &__sub {
    font-size: $fd-font-base;
    color: $fd-text-secondary;
    margin-top: $fd-space-xs;
    display: block;
  }
}

.wheel-section {
  padding: $fd-space-lg 0;
}

.wheel-hint {
  text-align: center;
  margin-top: $fd-space-md;
  font-size: $fd-font-sm;
}

.candidate-info {
  text-align: center;
  padding: $fd-space-base;
  font-size: $fd-font-sm;
}

/* 浮动手动记录按钮 */
.float-record-btn {
  position: fixed;
  right: 40rpx;
  bottom: 160rpx;
  background: $fd-primary;
  color: #fff;
  border-radius: 56rpx;
  padding: 20rpx 32rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  box-shadow: 0 8rpx 24rpx rgba($fd-primary, 0.4);
  z-index: 100;
  &:active { opacity: 0.85; transform: scale(0.96); }
}
.float-record-icon { font-size: 36rpx; }
.float-record-label {
  font-size: $fd-font-sm;
  font-weight: 600;
}

/* 连续打卡徽章 */
.streak-badge {
  position: fixed;
  top: 100rpx;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: $fd-space-xs;
  background: rgba(255, 107, 107, 0.95);
  color: #fff;
  border-radius: $fd-radius-round;
  padding: 12rpx 28rpx;
  z-index: 200;
  box-shadow: 0 8rpx 32rpx rgba(255, 107, 107, 0.35);
  &__icon { font-size: 32rpx; }
  &__text { font-size: $fd-font-sm; font-weight: 600; }
  &__close { font-size: 36rpx; opacity: 0.7; margin-left: 4rpx; }
  &:active { opacity: 0.85; }
}

/* 手动记录弹窗表单 */
.form-item { margin-bottom: $fd-space-base; }
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
.meal-types { display: flex; gap: $fd-space-xs; }
.meal-type-btn {
  flex: 1;
  text-align: center;
  padding: 12rpx 0;
  border-radius: $fd-radius-round;
  border: 2rpx solid $fd-border;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  &--active { background: $fd-primary; color: #fff; border-color: $fd-primary; }
  &:active { opacity: 0.8; }
}
</style>
