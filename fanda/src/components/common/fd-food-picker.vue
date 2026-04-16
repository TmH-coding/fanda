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

      <!-- 无搜索词时：显示搜索历史 + 热门菜品 -->
      <view v-if="!keyword.trim()" class="picker-discovery">
        <!-- 搜索历史 -->
        <view v-if="searchHistory.length" class="discovery-section">
          <view class="discovery-header">
            <text class="discovery-title">最近选择</text>
            <text class="discovery-clear" @tap="clearHistory">清除</text>
          </view>
          <view class="discovery-tags">
            <view
              v-for="name in searchHistory"
              :key="name"
              class="discovery-tag"
              @tap="keyword = name"
            >
              <text>{{ name }}</text>
            </view>
          </view>
        </view>

        <!-- 热门菜品（按选择频次排序） -->
        <view v-if="popularFoods.length" class="discovery-section">
          <view class="discovery-header">
            <text class="discovery-title">🔥 热门推荐</text>
          </view>
          <view
            v-for="food in popularFoods"
            :key="food.id"
            class="picker-item picker-item--popular"
            :class="{ 'picker-item--selected': modelValue === food.id }"
            @tap="select(food)"
          >
            <view class="picker-item__left">
              <text class="picker-item__name">{{ food.name }}</text>
              <text class="picker-item__meta">{{ catLabel(food.category) }} · ¥{{ food.priceRange[0] }}-{{ food.priceRange[1] }}</text>
            </view>
            <view class="picker-item__right">
              <text class="popular-count">{{ food._count }}次</text>
              <text v-if="modelValue === food.id" class="picker-item__check">✓</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 有搜索词或分类时：分类 Tab + 筛选列表 -->
      <template v-else>
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
      </template>

      <!-- 无搜索词时也显示分类浏览入口 -->
      <view v-if="!keyword.trim()" class="picker-browse-hint">
        <scroll-view scroll-x class="picker-cats">
          <view
            class="picker-cat"
            :class="{ 'picker-cat--active': activeCategory === '' }"
            @tap="activeCategory = ''; keyword = ' '"
          >
            <text>全部</text>
          </view>
          <view
            v-for="cat in FOOD_CATEGORIES"
            :key="cat.key"
            class="picker-cat"
            :class="{ 'picker-cat--active': activeCategory === cat.key }"
            @tap="activeCategory = cat.key; keyword = ' '"
          >
            <text>{{ cat.icon }} {{ cat.label }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useFoodStore } from '@/stores/modules/food'
import { FOOD_CATEGORIES } from '@/config/constants'

const HISTORY_KEY = 'fd_food_pick_history'
const FREQ_KEY = 'fd_food_pick_freq'
const MAX_HISTORY = 8

const props = defineProps({
  visible: { type: Boolean, default: false },
  modelValue: { type: [String, Number], default: null },
  mealTime: { type: String, default: '' },
})

const emit = defineEmits(['update:visible', 'update:modelValue', 'select'])

const foodStore = useFoodStore()
const keyword = ref('')
const activeCategory = ref('')

// ── 搜索历史 ───────────────────────────────────────────
const searchHistory = ref(loadHistory())

function loadHistory() {
  try {
    const raw = uni.getStorageSync(HISTORY_KEY)
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}

function saveHistory(name) {
  const h = loadHistory().filter(n => n !== name)
  h.unshift(name)
  const trimmed = h.slice(0, MAX_HISTORY)
  uni.setStorageSync(HISTORY_KEY, JSON.stringify(trimmed))
  searchHistory.value = trimmed
}

function clearHistory() {
  uni.removeStorageSync(HISTORY_KEY)
  searchHistory.value = []
}

// ── 热门菜品（按频次排序）─────────────────────────────
function loadFreq() {
  try {
    const raw = uni.getStorageSync(FREQ_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch { return {} }
}

function incFreq(foodId) {
  const freq = loadFreq()
  freq[foodId] = (freq[foodId] || 0) + 1
  uni.setStorageSync(FREQ_KEY, JSON.stringify(freq))
}

const popularFoods = computed(() => {
  const freq = loadFreq()
  return foodStore.foods
    .filter(f => freq[f.id])
    .map(f => ({ ...f, _count: freq[f.id] }))
    .sort((a, b) => b._count - a._count)
    .slice(0, 5)
})

// ── 分类标签 ─────────────────────────────────────────
const CAT_MAP = Object.fromEntries(FOOD_CATEGORIES.map((c) => [c.key, `${c.icon} ${c.label}`]))
function catLabel(key) { return CAT_MAP[key] || key }

// ── 过滤列表 ─────────────────────────────────────────
const filtered = computed(() => {
  let list = foodStore.foods
  if (props.mealTime) {
    list = list.filter((f) => !f.mealTime?.length || f.mealTime.includes(props.mealTime))
  }
  if (activeCategory.value) {
    list = list.filter((f) => f.category === activeCategory.value)
  }
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter((f) => f.name.toLowerCase().includes(kw))
  }
  return list
})

function select(food) {
  saveHistory(food.name)
  incFreq(food.id)
  emit('update:modelValue', food.id)
  emit('select', food)
  emit('update:visible', false)
  keyword.value = ''
  activeCategory.value = ''
}

// 关闭时重置
watch(() => props.visible, (v) => {
  if (!v) { keyword.value = ''; activeCategory.value = '' }
})
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
  height: 78vh;
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

/* 发现区（无搜索词时） */
.picker-discovery {
  flex: 1;
  overflow-y: auto;
  padding: 0 $fd-space-md;
}
.discovery-section {
  margin-bottom: $fd-space-base;
}
.discovery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $fd-space-sm;
}
.discovery-title {
  font-size: $fd-font-sm;
  font-weight: 700;
  color: $fd-text-secondary;
}
.discovery-clear {
  font-size: $fd-font-xs;
  color: $fd-text-light;
  &:active { color: $fd-danger; }
}
.discovery-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $fd-space-xs;
}
.discovery-tag {
  padding: 8rpx 24rpx;
  border-radius: $fd-radius-round;
  background: $fd-bg;
  border: 2rpx solid $fd-border;
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  &:active { opacity: 0.7; border-color: $fd-primary; }
}

/* 热门菜品行 */
.picker-item--popular {
  background: rgba($fd-primary, 0.03);
}
.popular-count {
  font-size: 22rpx;
  color: $fd-primary;
  margin-right: $fd-space-xs;
}
.picker-item__right {
  display: flex;
  align-items: center;
}

/* 浏览提示区（无搜索词时底部） */
.picker-browse-hint {
  flex-shrink: 0;
  border-top: 2rpx solid $fd-border;
  padding-bottom: env(safe-area-inset-bottom);
}

.picker-cats {
  white-space: nowrap;
  padding: $fd-space-sm $fd-space-sm;
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
