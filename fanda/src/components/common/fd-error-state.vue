<template>
  <view class="fd-error-state">
    <text class="fd-error-state__icon">{{ icon }}</text>
    <text class="fd-error-state__text">{{ text }}</text>
    <view v-if="retryText" class="fd-btn fd-error-state__btn" @tap="$emit('retry')">
      <text>{{ retryText }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  icon:      { type: String, default: '⚠️' },
  text:      { type: String, default: '加载失败，请重试' },
  retryText: { type: String, default: '重新加载' },
})
defineEmits(['retry'])
</script>

<style lang="scss" scoped>
.fd-error-state {
  @include fd-flex-column;
  align-items: center;
  justify-content: center;
  padding: $fd-space-xl 0;
  gap: $fd-space-sm;
}
.fd-error-state__icon {
  font-size: 80rpx;
}
.fd-error-state__text {
  font-size: $fd-font-base;
  color: $fd-text-secondary;
  text-align: center;
}
.fd-error-state__btn {
  margin-top: $fd-space-sm;
}
</style>
