<template>
  <view class="exclude">
    <view class="exclude__header">
      <text class="exclude__title">🚫 不想吃</text>
      <text v-if="excluded.length > 0" class="exclude__clear" @tap="$emit('clearAll')">清除</text>
    </view>
    <view class="exclude__tags">
      <view
        v-for="cat in categories"
        :key="cat.key"
        class="exclude__tag"
        :class="{ 'exclude__tag--active': excluded.includes(cat.key) }"
        @tap="$emit('toggle', cat.key)"
      >
        <text>{{ cat.icon }} {{ cat.label }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { FOOD_CATEGORIES } from '@/config/constants'

defineProps({
  excluded: { type: Array, default: () => [] },
})
defineEmits(['toggle', 'clearAll'])

const categories = FOOD_CATEGORIES
</script>

<style lang="scss" scoped>
.exclude {
  margin: $fd-space-base $fd-space-md;

  &__header {
    @include fd-flex-between;
    margin-bottom: $fd-space-sm;
  }
  &__title {
    font-size: $fd-font-base;
    font-weight: 600;
    color: $fd-text;
  }
  &__clear {
    font-size: $fd-font-sm;
    color: $fd-primary;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: $fd-space-sm;
  }

  &__tag {
    padding: 10rpx 24rpx;
    border-radius: $fd-radius-round;
    background: $fd-card-bg;
    border: 2rpx solid $fd-border;
    font-size: $fd-font-sm;
    transition: all 0.2s;

    &--active {
      background: rgba($fd-danger, 0.1);
      border-color: $fd-danger;
      color: $fd-danger;
      text-decoration: line-through;
    }
  }
}
</style>
