<template>
  <view class="fd-nav">
    <view class="fd-nav__status" :style="{ height: statusBarHeight + 'px' }"></view>
    <view class="fd-nav__bar">
      <view class="fd-nav__left" @tap="onBack">
        <text v-if="showBack" class="fd-nav__back">&#x276E;</text>
        <slot name="left" />
      </view>
      <view class="fd-nav__center">
        <text class="fd-nav__title">{{ title }}</text>
      </view>
      <view class="fd-nav__right">
        <slot name="right" />
      </view>
    </view>
  </view>
  <!-- 占位 -->
  <view :style="{ height: (statusBarHeight + 44) + 'px' }"></view>
</template>

<script setup>
import { getStatusBarHeight } from '@/utils/platform'

defineProps({
  title: { type: String, default: '' },
  showBack: { type: Boolean, default: false },
})

const statusBarHeight = getStatusBarHeight()

function onBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.fd-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: $fd-bg;

  &__bar {
    display: flex;
    align-items: center;
    height: 44px;
    padding: 0 $fd-space-md;
  }
  &__left, &__right {
    width: 80rpx;
    display: flex;
    align-items: center;
  }
  &__right {
    justify-content: flex-end;
  }
  &__center {
    flex: 1;
    text-align: center;
  }
  &__title {
    font-size: $fd-font-lg;
    font-weight: 700;
    color: $fd-text;
  }
  &__back {
    font-size: $fd-font-lg;
    color: $fd-text;
    font-weight: 700;
  }
}
</style>
