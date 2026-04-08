<template>
  <view v-if="food" class="result fd-anim-bounce-in">
    <view class="result__card">
      <text class="result__emoji">🎉</text>
      <text class="result__title">今天就吃</text>
      <text class="result__name">{{ food.name }}</text>

      <view class="result__info">
        <view class="result__tag">
          <text>{{ food.category === 'chinese' ? '🍚 中式' : food.category === 'western' ? '🍔 西式' : food.category === 'japanese' ? '🍣 日韩' : food.category === 'fastfood' ? '🍟 快餐' : food.category === 'light' ? '🥗 轻食' : food.category === 'snack' ? '🥟 小吃' : food.category === 'noodle' ? '🍜 面食' : '🍲 火锅' }}</text>
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

      <view class="result__favorite" @tap="$emit('toggleFavorite', food.id)">
        <text>{{ isFavorite ? '❤️ 已收藏' : '🤍 收藏' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  food: { type: Object, default: null },
  isFavorite: { type: Boolean, default: false },
})

const emit = defineEmits(['confirm', 'reroll', 'toggleFavorite'])

const costInput = ref('')

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

  &__favorite {
    margin-top: $fd-space-base;
    font-size: $fd-font-sm;
    &:active { opacity: 0.7; }
  }
}
</style>
