<template>
  <view v-if="visible" class="fd-modal__mask" @tap.self="onClose">
    <view class="fd-modal fd-anim-bounce-in">
      <view v-if="title" class="fd-modal__header">
        <text class="fd-modal__title">{{ title }}</text>
      </view>
      <view class="fd-modal__body">
        <slot />
      </view>
      <view v-if="showFooter" class="fd-modal__footer">
        <view v-if="showCancel" class="fd-modal__btn fd-modal__btn--cancel" @tap="onClose">
          <text>{{ cancelText }}</text>
        </view>
        <view class="fd-modal__btn fd-modal__btn--confirm" @tap="onConfirm">
          <text>{{ confirmText }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  showFooter: { type: Boolean, default: true },
  showCancel: { type: Boolean, default: true },
  cancelText: { type: String, default: '取消' },
  confirmText: { type: String, default: '确定' },
})

const emit = defineEmits(['update:visible', 'confirm', 'close'])

function onClose() {
  emit('update:visible', false)
  emit('close')
}
function onConfirm() {
  emit('confirm')
}
</script>

<style lang="scss" scoped>
.fd-modal {
  &__mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.45);
    z-index: 999;
    @include fd-flex-center;
  }

  width: 600rpx;
  background: $fd-card-bg;
  border-radius: $fd-radius-lg;
  overflow: hidden;

  &__header {
    padding: $fd-space-md $fd-space-md 0;
    text-align: center;
  }
  &__title {
    @include fd-title;
  }
  &__body {
    padding: $fd-space-md;
  }
  &__footer {
    display: flex;
    border-top: 2rpx solid $fd-border;
  }
  &__btn {
    flex: 1;
    @include fd-flex-center;
    padding: $fd-space-base 0;
    font-size: $fd-font-base;
    font-weight: 600;
    &:active {
      background: rgba(0, 0, 0, 0.03);
    }
    &--cancel {
      color: $fd-text-secondary;
      border-right: 2rpx solid $fd-border;
    }
    &--confirm {
      color: $fd-primary;
    }
  }
}
</style>
