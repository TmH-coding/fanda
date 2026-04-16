<template>
  <view v-if="visible" class="picker-mask" @tap.self="$emit('update:visible', false)">
    <view class="picker-panel">
      <view class="picker-header">
        <text class="picker-title">选择食物</text>
        <text class="picker-close" @tap="$emit('update:visible', false)">✕</text>
      </view>

      <!-- 搜索框 -->
      <view class="picker-search">
        <text class="picker-search__icon">🔍</text>
        <input
          class="picker-search__input"
          v-model="keyword"
          placeholder="搜索菜品名称…"
          confirm-type="search"
        />
        <text v-if="keyword" class="picker-search__clear" @tap="keyword = ''">×</text>
      </view>

      <!-- 分类 Tab -->
      <scroll-view scroll-x class="picker-cats">
        <view
          class="picker-cat"
          :class="{ 'picker-cat--active': activeCategory === '' }"
          @tap="activeCategory = ''"
        >
          <text>全部</text>
        </view>
        <view
          v-for="cat in FOOD_CATEGORIES"
          :key="cat.key"
          class="picker-cat"
          :class="{ 'picker-cat--active': activeCategory === cat.key }"
          @tap="activeCategory = cat.key"
        >
          <text>{{ cat.icon }} {{ cat.label }}</text>
        </view>
      </scroll-view>

      <!-- 食物列表 -->
      <scroll-view scroll-y class="picker-list">
        <view v-if="filtered.length === 0" class="picker-empty">
          <text class="fd-text-light">没有找到匹配的菜品</text>
        </view>
        <view
          v-for="food in filtered"
          :key="food.id"
          class="picker-item"
          :class="{ 'picker-item--selected': modelValue === food.id }"
          @tap="select(food)"
        >
          <view class="picker-item__left">
            <text class="picker-item__name">{{ food.name }}</text>
            <text class="picker-item__meta">{{ catLabel(food.category) }} · ¥{{ food.priceRange[0] }}-{{ food.priceRange[1] }}</text>
          </view>
          <text v-if="modelValue === food.id" class="picker-item__check">✓</text>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useFoodStore } from '@/stores/modules/food'
import { FOOD_CATEGORIES } from '@/config/constants'

const props = defineProps({
  visible: { type: Boolean, default: false },
  modelValue: { type: [String, Number], default: null },
  mealTime: { type: String, default: '' },
})

const emit = defineEmits(['update:visible', 'update:modelValue', 'select'])

const foodStore = useFoodStore()
const keyword = ref('')
const activeCategory = ref('')

const CAT_MAP = Object.fromEntries(FOOD_CATEGORIES.map((c) => [c.key, `${c.icon} ${c.label}`]))
function catLabel(key) { return CAT_MAP[key] || key }

const filtered = computed(() => {
  let list = foodStore.foods
  // 按餐次过滤（若传了 mealTime）
  if (props.mealTime) {
    list = list.filter((f) => !f.mealTime?.length || f.mealTime.includes(props.mealTime))
  }
  // 分类过滤
  if (activeCategory.value) {
    list = list.filter((f) => f.category === activeCategory.value)
  }
  // 关键词搜索
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter((f) => f.name.toLowerCase().includes(kw))
  }
  return list
})

function select(food) {
  emit('update:modelValue', food.id)
  emit('select', food)
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
.picker-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 998;
  display: flex;
  align-items: flex-end;
}

.picker-panel {
  background: #fff;
  border-radius: $fd-radius $fd-radius 0 0;
  width: 100%;
  height: 75vh;
  display: flex;
  flex-direction: column;
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $fd-space-base $fd-space-md;
  border-bottom: 2rpx solid $fd-border;
  flex-shrink: 0;
}
.picker-title {
  font-size: $fd-font-md;
  font-weight: 700;
  color: $fd-text;
}
.picker-close {
  font-size: $fd-font-lg;
  color: $fd-text-light;
  padding: 8rpx 16rpx;
  &:active { opacity: 0.6; }
}

.picker-search {
  display: flex;
  align-items: center;
  gap: $fd-space-xs;
  margin: $fd-space-sm $fd-space-md;
  background: $fd-bg;
  border-radius: $fd-radius-round;
  padding: 0 $fd-space-sm;
  flex-shrink: 0;
  &__icon { font-size: $fd-font-base; flex-shrink: 0; }
  &__input {
    flex: 1;
    height: 70rpx;
    font-size: $fd-font-sm;
    color: $fd-text;
  }
  &__clear {
    font-size: $fd-font-lg;
    color: $fd-text-light;
    padding: 0 8rpx;
    &:active { opacity: 0.6; }
  }
}

.picker-cats {
  white-space: nowrap;
  padding: 0 $fd-space-sm $fd-space-sm;
  flex-shrink: 0;
}
.picker-cat {
  display: inline-block;
  padding: 8rpx 24rpx;
  margin-right: $fd-space-xs;
  border-radius: $fd-radius-round;
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  background: $fd-bg;
  border: 2rpx solid $fd-border;
  &--active {
    background: $fd-primary;
    color: #fff;
    border-color: $fd-primary;
  }
  &:active { opacity: 0.8; }
}

.picker-list {
  flex: 1;
  overflow-y: auto;
}
.picker-empty {
  padding: $fd-space-xl;
  text-align: center;
}
.picker-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $fd-space-base $fd-space-md;
  border-bottom: 2rpx solid $fd-border;
  &:active { background: $fd-bg; }
  &--selected { background: rgba($fd-primary, 0.05); }
  &__left { flex: 1; }
  &__name {
    display: block;
    font-size: $fd-font-base;
    font-weight: 500;
    color: $fd-text;
    margin-bottom: 4rpx;
  }
  &__meta {
    font-size: $fd-font-xs;
    color: $fd-text-secondary;
  }
  &__check {
    font-size: $fd-font-md;
    color: $fd-primary;
    font-weight: 700;
    margin-left: $fd-space-sm;
  }
}
</style>
