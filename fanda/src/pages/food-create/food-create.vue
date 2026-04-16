<template>
  <view class="fd-page">
    <fd-nav-bar title="录入自定义食物" :show-back="true" />

    <view class="form fd-card">
      <!-- 食物名称 -->
      <view class="field">
        <text class="field-label">食物名称 <text class="required">*</text></text>
        <input class="field-input" v-model="form.name" placeholder="如：学校特色炒饭" maxlength="50" />
      </view>

      <!-- 分类 -->
      <view class="field">
        <text class="field-label">分类 <text class="required">*</text></text>
        <view class="tag-group">
          <view
            v-for="cat in FOOD_CATEGORIES"
            :key="cat.key"
            class="tag-option"
            :class="{ 'tag-option--active': form.category === cat.key }"
            @tap="form.category = cat.key"
          >
            <text>{{ cat.icon }} {{ cat.label }}</text>
          </view>
        </view>
      </view>

      <!-- 价格区间 -->
      <view class="field">
        <text class="field-label">价格区间（元）</text>
        <view class="price-row">
          <input class="field-input price-input" type="digit" v-model="form.priceMin" placeholder="最低" />
          <text class="price-sep">—</text>
          <input class="field-input price-input" type="digit" v-model="form.priceMax" placeholder="最高" />
        </view>
      </view>

      <!-- 营养标签 -->
      <view class="field">
        <text class="field-label">营养类型（可多选）</text>
        <view class="tag-group">
          <view
            v-for="n in NUTRITION_TYPES"
            :key="n.key"
            class="tag-option"
            :class="{ 'tag-option--active': form.nutrition.includes(n.key) }"
            @tap="toggleList(form.nutrition, n.key)"
          >
            <text>{{ n.label }}</text>
          </view>
        </view>
      </view>

      <!-- 适用餐次 -->
      <view class="field">
        <text class="field-label">适用餐次（可多选）</text>
        <view class="tag-group">
          <view
            v-for="m in MEAL_TYPES"
            :key="m.key"
            class="tag-option"
            :class="{ 'tag-option--active': form.mealTime.includes(m.key) }"
            @tap="toggleList(form.mealTime, m.key)"
          >
            <text>{{ m.label }}</text>
          </view>
        </view>
      </view>

      <!-- 提交 -->
      <view class="fd-btn" :class="{ 'fd-btn--disabled': submitting }" @tap="submit">
        <text>{{ submitting ? '保存中...' : '保存食物' }}</text>
      </view>
    </view>

    <!-- 批量导入 -->
    <view class="batch-section fd-card">
      <view class="batch-header" @tap="showBatch = !showBatch">
        <text class="batch-title">📋 批量导入</text>
        <text class="batch-toggle">{{ showBatch ? '▲' : '▼' }}</text>
      </view>
      <view v-if="showBatch">
        <text class="batch-hint">每行一个食物，格式：名称,分类,最低价,最高价（后两项可省略）</text>
        <text class="batch-example">示例：黄焖鸡米饭,chinese,15,25</text>
        <textarea
          class="batch-textarea"
          v-model="batchText"
          placeholder="粘贴多行数据..."
          :maxlength="2000"
          auto-height
        />
        <view v-if="batchPreview.length" class="batch-preview">
          <text class="batch-preview-label">预览 {{ batchPreview.length }} 条：</text>
          <view v-for="(item, i) in batchPreview.slice(0, 5)" :key="i" class="batch-preview-item">
            <text class="batch-preview-name">{{ item.name }}</text>
            <text class="batch-preview-cat">{{ item.category }}</text>
            <text v-if="item.error" class="batch-preview-error">{{ item.error }}</text>
          </view>
          <text v-if="batchPreview.length > 5" class="batch-preview-more">...还有 {{ batchPreview.length - 5 }} 条</text>
        </view>
        <view class="fd-btn" :class="{ 'fd-btn--disabled': batchSubmitting || batchValid.length === 0 }" @tap="submitBatch">
          <text>{{ batchSubmitting ? `导入中 ${batchProgress}/${batchValid.length}...` : `导入 ${batchValid.length} 个食物` }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { post } from '@/utils/http'
import { FOOD_CATEGORIES, NUTRITION_TYPES, MEAL_TYPES } from '@/config/constants'
import { useFoodStore } from '@/stores/modules/food'
import FdNavBar from '@/components/common/fd-nav-bar.vue'

const foodStore = useFoodStore()
const submitting = ref(false)

const form = reactive({
  name: '',
  category: '',
  priceMin: '',
  priceMax: '',
  nutrition: [],
  mealTime: [],
})

// ── 批量导入 ───────────────────────────────────────────
const showBatch = ref(false)
const batchText = ref('')
const batchSubmitting = ref(false)
const batchProgress = ref(0)

const CATEGORY_KEYS = new Set(FOOD_CATEGORIES.map(c => c.key))

const batchPreview = computed(() => {
  if (!batchText.value.trim()) return []
  return batchText.value
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean)
    .map(line => {
      const parts = line.split(',').map(s => s.trim())
      const name = parts[0] || ''
      const category = parts[1] || ''
      const priceMin = parts[2] ? Number(parts[2]) : null
      const priceMax = parts[3] ? Number(parts[3]) : null
      let error = ''
      if (!name) error = '缺少名称'
      else if (!category) error = '缺少分类'
      else if (!CATEGORY_KEYS.has(category)) error = `分类"${category}"不存在`
      return { name, category, priceMin, priceMax, error }
    })
})

const batchValid = computed(() => batchPreview.value.filter(i => !i.error))

async function submitBatch() {
  const items = batchValid.value
  if (!items.length) return
  batchSubmitting.value = true
  batchProgress.value = 0
  let successCount = 0
  for (const item of items) {
    try {
      await post('/api/foods', {
        name: item.name,
        category: item.category,
        priceMin: item.priceMin,
        priceMax: item.priceMax,
        nutrition: [],
        mealTime: [],
      })
      successCount++
    } catch {
      // 单条失败继续
    }
    batchProgress.value++
  }
  foodStore.loaded = false
  await foodStore.load()
  batchSubmitting.value = false
  batchText.value = ''
  uni.showToast({ title: `导入完成 ${successCount}/${items.length} ✅`, icon: 'none', duration: 2000 })
}

function toggleList(arr, key) {
  const idx = arr.indexOf(key)
  if (idx >= 0) arr.splice(idx, 1)
  else arr.push(key)
}

async function submit() {
  if (!form.name.trim()) {
    uni.showToast({ title: '请输入食物名称', icon: 'none' })
    return
  }
  if (!form.category) {
    uni.showToast({ title: '请选择分类', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      category: form.category,
      priceMin: form.priceMin ? Number(form.priceMin) : null,
      priceMax: form.priceMax ? Number(form.priceMax) : null,
      nutrition: form.nutrition,
      mealTime: form.mealTime,
    }
    const newFood = await post('/api/foods', payload)
    // 重新加载食物列表使新增的出现在推荐候选中
    foodStore.loaded = false
    await foodStore.load()
    uni.showToast({ title: '保存成功 ✅', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1200)
  } catch (e) {
    uni.showToast({ title: '保存失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.form {
  @include fd-flex-column;
  gap: $fd-space-base;
}

.field {
  @include fd-flex-column;
  gap: $fd-space-xs;
}
.field-label {
  font-size: $fd-font-sm;
  font-weight: 600;
  color: $fd-text-secondary;
}
.required { color: $fd-danger; }
.field-input {
  height: 72rpx;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: 0 $fd-space-sm;
  font-size: $fd-font-base;
  background: $fd-bg;
}

.price-row {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
}
.price-input { flex: 1; }
.price-sep {
  font-size: $fd-font-base;
  color: $fd-text-light;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: $fd-space-xs;
}
.tag-option {
  padding: 8rpx 20rpx;
  border-radius: $fd-radius-sm;
  border: 2rpx solid $fd-border;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  background: $fd-bg;
  &:active { opacity: 0.7; }
  &--active {
    border-color: $fd-primary;
    color: $fd-primary;
    background: rgba($fd-primary, 0.08);
  }
}

.fd-btn--disabled { opacity: 0.5; pointer-events: none; }

/* 批量导入 */
.batch-section {
  margin-top: $fd-space-sm;
  @include fd-flex-column;
  gap: $fd-space-sm;
}
.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  &:active { opacity: 0.7; }
}
.batch-title {
  font-size: $fd-font-base;
  font-weight: 700;
  color: $fd-text;
}
.batch-toggle { font-size: $fd-font-sm; color: $fd-text-secondary; }
.batch-hint {
  display: block;
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  line-height: 1.6;
}
.batch-example {
  display: block;
  font-size: $fd-font-xs;
  color: $fd-text-light;
  font-family: monospace;
  margin-bottom: $fd-space-xs;
}
.batch-textarea {
  width: 100%;
  min-height: 200rpx;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: $fd-space-sm;
  font-size: $fd-font-sm;
  background: $fd-bg;
  font-family: monospace;
}
.batch-preview {
  background: $fd-bg;
  border-radius: $fd-radius-sm;
  padding: $fd-space-sm;
}
.batch-preview-label {
  display: block;
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  margin-bottom: $fd-space-xs;
}
.batch-preview-item {
  display: flex;
  align-items: center;
  gap: $fd-space-xs;
  padding: 6rpx 0;
}
.batch-preview-name { font-size: $fd-font-sm; color: $fd-text; font-weight: 600; }
.batch-preview-cat { font-size: $fd-font-xs; color: $fd-text-secondary; }
.batch-preview-error { font-size: $fd-font-xs; color: $fd-danger; }
.batch-preview-more { font-size: $fd-font-xs; color: $fd-text-light; display: block; }
</style>
