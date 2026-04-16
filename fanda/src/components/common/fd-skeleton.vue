<template>
  <view class="fd-skeleton">
    <view
      v-for="n in rows"
      :key="n"
      class="fd-skeleton__row"
      :style="{ width: rowWidth(n) }"
    />
  </view>
</template>

<script setup>
defineProps({
  rows:      { type: Number, default: 3 },
  /** 'fixed' 固定宽度列表, 'random' 随机宽度让骨架更自然 */
  widthMode: { type: String, default: 'random' },
})

function rowWidth(n) {
  // 最后一行短些，让骨架屏更像真实内容
  if (n % 3 === 0) return '60%'
  if (n % 2 === 0) return '85%'
  return '100%'
}
</script>

<style lang="scss" scoped>
.fd-skeleton {
  padding: $fd-space-md;
  display: flex;
  flex-direction: column;
  gap: $fd-space-sm;

  &__row {
    height: 32rpx;
    border-radius: 8rpx;
    background: linear-gradient(90deg, $fd-border 25%, rgba($fd-border, 0.5) 50%, $fd-border 75%);
    background-size: 400% 100%;
    animation: fd-shimmer 1.4s ease infinite;
  }
}

@keyframes fd-shimmer {
  0%   { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
</style>
