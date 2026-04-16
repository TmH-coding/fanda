<template>
  <view class="wheel-wrap">
    <!-- 转盘主体 -->
    <view class="wheel" :style="wheelStyle">
      <view
        v-for="(item, i) in items"
        :key="item.id"
        class="wheel__sector"
        :style="sectorStyle(i)"
      >
        <view class="wheel__label" :style="labelStyle(i)">
          <text class="wheel__name">{{ item.name }}</text>
        </view>
      </view>
    </view>

    <!-- 中心指针 -->
    <view class="wheel__pointer">
      <view class="wheel__pointer-arrow"></view>
      <view class="wheel__pointer-btn" @tap="onTap">
        <text class="wheel__pointer-text">{{ spinning ? '...' : 'GO' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  spinning: { type: Boolean, default: false },
  targetIndex: { type: Number, default: 0 },
})

const emit = defineEmits(['spin', 'spinEnd'])

const rotation = ref(0)
const isAnimating = ref(false)

const sectorAngle = computed(() => {
  return props.items.length > 0 ? 360 / props.items.length : 0
})

const wheelStyle = computed(() => {
  return {
    transform: `rotate(${rotation.value}deg)`,
    transition: isAnimating.value ? 'transform 4s cubic-bezier(0.17, 0.67, 0.12, 0.99)' : 'none',
  }
})

function sectorStyle(index) {
  const angle = sectorAngle.value
  const rotate = angle * index
  const colors = [
    '#FF6B6B', '#FFE66D', '#4ECDC4', '#FF8A8A',
    '#A8E6CF', '#FFB6B9', '#FFDAC1', '#B5EAD7',
    '#C7CEEA', '#F8B500', '#FF6F61', '#88D8B0',
  ]
  return {
    transform: `rotate(${rotate}deg)`,
    background: colors[index % colors.length],
    clipPath: props.items.length <= 2
      ? 'none'
      : `polygon(0% 0%, 100% 0%, ${50 + 50 * Math.tan((Math.PI * (90 - angle / 2)) / 180)}% 100%, 50% 100%)`,
  }
}

function labelStyle(index) {
  const angle = sectorAngle.value
  return {
    transform: `rotate(${angle / 2}deg)`,
  }
}

function onTap() {
  if (isAnimating.value) return
  // 轻触觉反馈
  try { uni.vibrateShort({ type: 'medium' }) } catch (e) { /* 不支持则忽略 */ }
  emit('spin')
}

watch(() => props.spinning, (val) => {
  if (val && props.items.length > 0) {
    isAnimating.value = true
    // 转 5-8 圈 + 随机停留角度
    const idx = props.targetIndex >= 0 ? props.targetIndex : Math.floor(Math.random() * props.items.length)
    const targetAngle = 360 - (sectorAngle.value * idx + sectorAngle.value / 2)
    const extraRounds = (5 + Math.floor(Math.random() * 3)) * 360
    rotation.value = rotation.value + extraRounds + targetAngle + (Math.random() * sectorAngle.value * 0.4 - sectorAngle.value * 0.2)

    setTimeout(() => {
      isAnimating.value = false
      // 结果出现时短震动
      try { uni.vibrateShort({ type: 'light' }) } catch (e) { /* 不支持则忽略 */ }
      emit('spinEnd')
    }, 4200)
  }
})
</script>

<style lang="scss" scoped>
.wheel-wrap {
  position: relative;
  width: 580rpx;
  height: 580rpx;
  margin: 0 auto;
}

.wheel {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8rpx 40rpx rgba(255, 107, 107, 0.3);
  border: 8rpx solid #fff;

  &__sector {
    position: absolute;
    width: 50%;
    height: 50%;
    top: 0;
    left: 50%;
    transform-origin: 0% 100%;
  }

  &__label {
    position: absolute;
    top: 20rpx;
    left: -30rpx;
    width: 140rpx;
    text-align: center;
    transform-origin: bottom center;
  }

  &__name {
    font-size: 22rpx;
    color: #fff;
    font-weight: 600;
    text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.2);
  }
}

.wheel__pointer {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;

  &-arrow {
    width: 0;
    height: 0;
    border-left: 20rpx solid transparent;
    border-right: 20rpx solid transparent;
    border-bottom: 40rpx solid $fd-primary-dark;
    position: absolute;
    top: -45rpx;
    left: 50%;
    transform: translateX(-50%);
  }

  &-btn {
    width: 110rpx;
    height: 110rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, $fd-primary, $fd-primary-dark);
    @include fd-flex-center;
    box-shadow: 0 4rpx 20rpx rgba(255, 107, 107, 0.5);

    &:active {
      transform: scale(0.92);
    }
  }

  &-text {
    color: #fff;
    font-size: $fd-font-xl;
    font-weight: 800;
  }
}
</style>
