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
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
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
</style>
