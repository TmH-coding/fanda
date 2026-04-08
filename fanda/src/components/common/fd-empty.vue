<template>
  <view class="fd-empty">
    <text class="fd-empty__icon">{{ icon }}</text>
    <text class="fd-empty__text">{{ text }}</text>
    <view v-if="btnText" class="fd-empty__btn" @tap="$emit('action')">
      <text>{{ btnText }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '🍽️' },
  text: { type: String, default: '暂无数据' },
  btnText: { type: String, default: '' },
})
defineEmits(['action'])
</script>

<style lang="scss" scoped>
.fd-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;

  &__icon {
    font-size: 120rpx;
    margin-bottom: $fd-space-md;
  }
  &__text {
    font-size: $fd-font-base;
    color: $fd-text-light;
    margin-bottom: $fd-space-md;
  }
  &__btn {
    @include fd-btn;
    font-size: $fd-font-sm;
    padding: 12rpx 40rpx;
  }
}
</style>
