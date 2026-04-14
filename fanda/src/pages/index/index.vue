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
      @confirm="onConfirm"
      @reroll="onReroll"
      @toggle-favorite="onToggleFav"
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
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRecommend } from '@/composables/useRecommend'
import { usePreferenceStore } from '@/stores/modules/preference'
import { useBudgetStore } from '@/stores/modules/budget'
import { mealTypeLabel } from '@/utils/date'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdWheel from '@/components/recommend/fd-wheel.vue'
import FdFoodResult from '@/components/recommend/fd-food-result.vue'
import FdExcludeTags from '@/components/recommend/fd-exclude-tags.vue'

const {
  excludedCategories,
  currentFood,
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
const targetIndex = ref(0)

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

onMounted(async () => {
  await init()
})
</script>

<style lang="scss" scoped>
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
</style>
