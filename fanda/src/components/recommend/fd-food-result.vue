<template>
  <view v-if="food" class="result fd-anim-bounce-in">
    <view class="result__card">
      <text class="result__emoji">🎉</text>
      <text class="result__title">今天就吃</text>
      <text class="result__name" @tap="showDetail = true">{{ food.name }} ℹ️</text>

      <!-- 推荐理由 -->
      <view v-if="reason" class="result__reason">
        <text class="result__reason-text">{{ reason }}</text>
      </view>

      <view class="result__info">
        <view class="result__tag">
          <text>{{ categoryLabel }}</text>
        </view>
        <view class="result__price">
          <text>💰 {{ food.priceRange[0] }}-{{ food.priceRange[1] }}元</text>
        </view>
      </view>

      <view class="result__tags">
        <text v-for="tag in food.tags" :key="tag" class="result__food-tag">{{ tag }}</text>
      </view>

      <!-- 花费输入 -->
      <view class="result__cost">
        <text class="result__cost-label">实际花费</text>
        <input
          class="result__cost-input"
          type="digit"
          :value="costInput"
          :placeholder="String(defaultCost)"
          @input="onCostInput"
        />
        <text class="result__cost-unit">元</text>
      </view>

      <view class="result__actions">
        <view class="result__btn result__btn--secondary" @tap="$emit('reroll')">
          <text>换一个 🔄</text>
        </view>
        <view class="result__btn result__btn--primary" @tap="onConfirm">
          <text>就吃这个 ✅</text>
        </view>
      </view>

      <view class="result__quick-actions">
        <view class="result__favorite" @tap="$emit('toggleFavorite', food.id)">
          <text>{{ isFavorite ? '❤️ 已收藏' : '🤍 收藏' }}</text>
        </view>
        <view class="result__blacklist" @tap="onBlacklist">
          <text>🚫 不想吃</text>
        </view>
      </view>
    </view>

    <!-- 菜品详情弹窗 -->
    <view v-if="showDetail" class="detail-mask" @tap.self="showDetail = false">
      <view class="detail-panel fd-anim-bounce-in">
        <view class="detail-header">
          <text class="detail-title">{{ food.name }}</text>
          <text class="detail-close" @tap="showDetail = false">✕</text>
        </view>

        <view class="detail-section">
          <view class="detail-row">
            <text class="detail-label">分类</text>
            <text class="detail-value">{{ categoryLabel }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">价格区间</text>
            <text class="detail-value">¥{{ food.priceRange[0] }} – ¥{{ food.priceRange[1] }}</text>
          </view>
          <view v-if="food.mealTime && food.mealTime.length" class="detail-row">
            <text class="detail-label">适合餐次</text>
            <text class="detail-value">{{ mealTimeLabel }}</text>
          </view>
        </view>

        <view v-if="food.nutrition && food.nutrition.length" class="detail-section">
          <text class="detail-section-title">营养标签</text>
          <view class="detail-tags">
            <text v-for="n in food.nutrition" :key="n" class="detail-tag detail-tag--nutrition">{{ nutritionMap[n] || n }}</text>
          </view>
        </view>

        <view v-if="food.allergens && food.allergens.length" class="detail-section">
          <text class="detail-section-title">含过敏原</text>
          <view class="detail-tags">
            <text v-for="a in food.allergens" :key="a" class="detail-tag detail-tag--allergen">⚠️ {{ a }}</text>
          </view>
        </view>

        <view v-if="food.tags && food.tags.length" class="detail-section">
          <text class="detail-section-title">特色标签</text>
          <view class="detail-tags">
            <text v-for="t in food.tags" :key="t" class="detail-tag">{{ t }}</text>
          </view>
        </view>

        <view class="detail-actions">
          <view class="detail-fav-btn" @tap="$emit('toggleFavorite', food.id); showDetail = false">
            <text>{{ isFavorite ? '❤️ 取消收藏' : '🤍 加入收藏' }}</text>
          </view>
          <view class="detail-confirm-btn" @tap="showDetail = false; onConfirm()">
            <text>就吃这个 ✅</text>
          </view>
        </view>

        <!-- 分享图生成 -->
        <view class="detail-share-btn" @tap="generateShareImage">
          <text>📤 生成分享图</text>
        </view>
      </view>
    </view>

    <!-- Canvas（隐藏，用于生成分享图） -->
    <canvas canvas-id="shareCanvas" style="position:fixed;left:-9999px;width:600rpx;height:800rpx;" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  food: { type: Object, default: null },
  isFavorite: { type: Boolean, default: false },
  reason: { type: String, default: '' },
})

const emit = defineEmits(['confirm', 'reroll', 'toggleFavorite', 'blacklist'])

const costInput = ref('')
const showDetail = ref(false)

const CATEGORY_MAP = {
  chinese: '🍚 中式', western: '🍔 西式', japanese: '🍣 日韩',
  fastfood: '🍟 快餐', light: '🥗 轻食', snack: '🥟 小吃',
  noodle: '🍜 面食', hotpot: '🍲 火锅',
}
const NUTRITION_MAP = {
  carb: '碳水', protein: '蛋白质', veggie: '蔬菜',
  fruit: '水果', fat: '脂肪', fiber: '膳食纤维',
}
const MEAL_TIME_MAP = {
  breakfast: '早餐', lunch: '午餐', dinner: '晚餐', night: '宵夜',
}

const categoryLabel = computed(() => CATEGORY_MAP[props.food?.category] || props.food?.category || '')
const nutritionMap = NUTRITION_MAP
const mealTimeLabel = computed(() =>
  (props.food?.mealTime || []).map((m) => MEAL_TIME_MAP[m] || m).join('、')
)

const defaultCost = computed(() => {
  if (!props.food) return 0
  return Math.round((props.food.priceRange[0] + props.food.priceRange[1]) / 2)
})

function onCostInput(e) {
  costInput.value = e.detail.value
}

function onConfirm() {
  const cost = costInput.value ? Number(costInput.value) : defaultCost.value
  emit('confirm', cost)
  costInput.value = ''
}

function onBlacklist() {
  uni.showModal({
    title: '不想吃这个？',
    content: `将"${props.food.name}"加入黑名单后，不会再被推荐`,
    confirmText: '确认',
    cancelText: '算了',
    success(res) {
      if (res.confirm) emit('blacklist', props.food.id)
    },
  })
}

function generateShareImage() {
  const food = props.food
  if (!food) return

  const ctx = uni.createCanvasContext('shareCanvas')

  // 背景
  ctx.setFillStyle('#FFF8F0')
  ctx.fillRect(0, 0, 300, 400)

  // 顶部色块
  ctx.setFillStyle('#FF6B6B')
  ctx.fillRect(0, 0, 300, 120)

  // 顶部文字
  ctx.setFillStyle('#fff')
  ctx.setFontSize(22)
  ctx.setTextAlign('center')
  ctx.fillText('饭搭推荐', 150, 48)
  ctx.setFontSize(14)
  ctx.fillText('今天就吃这个！', 150, 78)

  // 食物名称
  ctx.setFillStyle('#FF6B6B')
  ctx.setFontSize(30)
  ctx.setTextAlign('center')
  ctx.fillText(food.name, 150, 168)

  // 分类 + 价格
  ctx.setFillStyle('#888')
  ctx.setFontSize(13)
  const catText = CATEGORY_MAP[food.category] || food.category || ''
  ctx.fillText(`${catText}  ¥${food.priceRange[0]}-${food.priceRange[1]}`, 150, 200)

  // 分隔线
  ctx.beginPath()
  ctx.moveTo(40, 220)
  ctx.lineTo(260, 220)
  ctx.setStrokeStyle('#eee')
  ctx.stroke()

  // 营养标签
  if (food.nutrition?.length) {
    ctx.setFillStyle('#4ECDC4')
    ctx.setFontSize(12)
    ctx.setTextAlign('left')
    const tags = food.nutrition.slice(0, 4).map(n => NUTRITION_MAP[n] || n)
    tags.forEach((tag, i) => {
      ctx.fillText(`• ${tag}`, 40 + (i % 2) * 130, 248 + Math.floor(i / 2) * 24)
    })
  }

  // 底部 slogan
  ctx.setTextAlign('center')
  ctx.setFillStyle('#bbb')
  ctx.setFontSize(11)
  ctx.fillText('用饭搭管理你的每一餐', 150, 375)

  ctx.draw(false, () => {
    uni.canvasToTempFilePath({
      canvasId: 'shareCanvas',
      fileType: 'png',
      success: (res) => {
        uni.saveImageToPhotosAlbum({
          filePath: res.tempFilePath,
          success: () => {
            uni.showToast({ title: '分享图已保存到相册 📸', icon: 'none', duration: 2000 })
            showDetail.value = false
          },
          fail: () => {
            uni.previewImage({ urls: [res.tempFilePath] })
          },
        })
      },
      fail: () => uni.showToast({ title: '生成失败，请重试', icon: 'none' }),
    })
  })
}
</script>

<style lang="scss" scoped>
.result {
  &__card {
    @include fd-card;
    @include fd-flex-column;
    align-items: center;
    margin: $fd-space-md;
    padding: $fd-space-lg $fd-space-md;
  }

  &__emoji {
    font-size: 80rpx;
    margin-bottom: $fd-space-sm;
  }
  &__title {
    font-size: $fd-font-base;
    color: $fd-text-secondary;
  }
  &__name {
    font-size: $fd-font-xxl;
    font-weight: 800;
    color: $fd-primary;
    margin: $fd-space-sm 0;
    &:active { opacity: 0.75; }
  }

  &__reason {
    background: rgba($fd-accent, 0.12);
    border-radius: $fd-radius;
    padding: 16rpx 24rpx;
    margin: $fd-space-xs 0 $fd-space-sm;
    width: 100%;
  }
  &__reason-text {
    font-size: $fd-font-sm;
    color: $fd-accent;
    text-align: center;
    display: block;
  }

  &__info {
    display: flex;
    gap: $fd-space-base;
    margin: $fd-space-sm 0;
  }
  &__tag, &__price {
    @include fd-tag(rgba($fd-primary, 0.1), $fd-primary);
    font-size: $fd-font-sm;
    padding: 8rpx 20rpx;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: $fd-space-xs;
    margin: $fd-space-sm 0;
  }
  &__food-tag {
    @include fd-tag(rgba($fd-accent, 0.12), $fd-accent);
    font-size: $fd-font-xs;
  }

  &__cost {
    display: flex;
    align-items: center;
    margin: $fd-space-md 0;
    gap: $fd-space-sm;
  }
  &__cost-label {
    font-size: $fd-font-sm;
    color: $fd-text-secondary;
  }
  &__cost-input {
    width: 160rpx;
    height: 64rpx;
    border: 2rpx solid $fd-border;
    border-radius: $fd-radius-sm;
    text-align: center;
    font-size: $fd-font-base;
    font-weight: 600;
  }
  &__cost-unit {
    font-size: $fd-font-sm;
    color: $fd-text-secondary;
  }

  &__actions {
    display: flex;
    gap: $fd-space-base;
    margin-top: $fd-space-base;
    width: 100%;
  }
  &__btn {
    flex: 1;
    @include fd-flex-center;
    padding: 20rpx 0;
    border-radius: $fd-radius-round;
    font-size: $fd-font-base;
    font-weight: 600;

    &--primary {
      background: $fd-primary;
      color: #fff;
      &:active { opacity: 0.85; }
    }
    &--secondary {
      background: rgba($fd-primary, 0.1);
      color: $fd-primary;
      &:active { opacity: 0.85; }
    }
  }

  &__quick-actions {
    display: flex;
    gap: $fd-space-lg;
    margin-top: $fd-space-base;
    align-items: center;
  }
  &__favorite {
    font-size: $fd-font-sm;
    &:active { opacity: 0.7; }
  }
  &__blacklist {
    font-size: $fd-font-sm;
    color: $fd-text-secondary;
    &:active { opacity: 0.7; }
  }
}

/* 详情弹窗 */
.detail-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.detail-panel {
  background: #fff;
  border-radius: $fd-radius $fd-radius 0 0;
  padding: $fd-space-lg $fd-space-md;
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $fd-space-base;
}
.detail-title {
  font-size: $fd-font-xl;
  font-weight: 800;
  color: $fd-text;
}
.detail-close {
  font-size: $fd-font-lg;
  color: $fd-text-light;
  padding: 8rpx 16rpx;
  &:active { opacity: 0.6; }
}

.detail-section {
  margin-bottom: $fd-space-base;
  padding-bottom: $fd-space-base;
  border-bottom: 2rpx solid $fd-border;
  &:last-of-type { border-bottom: none; }
}
.detail-section-title {
  display: block;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  font-weight: 600;
  margin-bottom: $fd-space-sm;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.detail-label {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
}
.detail-value {
  font-size: $fd-font-sm;
  font-weight: 600;
  color: $fd-text;
}
.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $fd-space-xs;
}
.detail-tag {
  @include fd-tag(rgba($fd-primary, 0.1), $fd-primary);
  font-size: $fd-font-xs;
  &--nutrition { background: rgba($fd-accent, 0.12); color: $fd-accent; }
  &--allergen { background: rgba($fd-danger, 0.08); color: $fd-danger; }
}

.detail-actions {
  display: flex;
  gap: $fd-space-sm;
  margin-top: $fd-space-base;
}
.detail-fav-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx;
  border-radius: $fd-radius-round;
  border: 2rpx solid $fd-border;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  &:active { opacity: 0.8; }
}
.detail-confirm-btn {
  flex: 2;
  text-align: center;
  padding: 20rpx;
  border-radius: $fd-radius-round;
  background: $fd-primary;
  color: #fff;
  font-size: $fd-font-sm;
  font-weight: 600;
  &:active { opacity: 0.85; }
}
.detail-share-btn {
  text-align: center;
  margin-top: $fd-space-sm;
  padding: 16rpx;
  border-radius: $fd-radius-round;
  border: 2rpx solid $fd-border;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  &:active { opacity: 0.8; }
}
</style>
