<template>
  <view class="fd-page">
    <fd-nav-bar title="我的" />

    <!-- 用户头像区 -->
    <view class="profile-header">
      <view class="avatar">
        <text class="avatar-text">{{ userAvatar }}</text>
      </view>
      <text class="nickname">{{ userNickname }}</text>
      <text class="achievement-progress">成就 {{ achievementStore.progress }}</text>
    </view>

    <!-- 数据概览 -->
    <view class="stats fd-card">
      <view class="stat-item">
        <text class="stat-num">{{ recordStore.totalRecords }}</text>
        <text class="stat-label">记录餐数</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ recordStore.streak }}</text>
        <text class="stat-label">连续天数</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ recordStore.uniqueFoodCount }}</text>
        <text class="stat-label">尝试菜品</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ prefStore.favoriteCount }}</text>
        <text class="stat-label">收藏</text>
      </view>
    </view>

    <!-- 口味偏好设置 -->
    <view class="section fd-card">
      <text class="section-title">🌶️ 口味偏好</text>
      <view class="pref-row">
        <text class="pref-label">辣度</text>
        <view class="spicy-levels">
          <view
            v-for="level in spicyLevels"
            :key="level.value"
            class="spicy-item"
            :class="{ 'spicy-item--active': prefStore.preference.spicyLevel === level.value }"
            @tap="setSpicy(level.value)"
          >
            <text>{{ level.label }}</text>
          </view>
        </view>
      </view>

      <view class="pref-row">
        <text class="pref-label">忌口</text>
        <view class="allergy-tags">
          <view
            v-for="item in allergyOptions"
            :key="item"
            class="allergy-tag"
            :class="{ 'allergy-tag--active': prefStore.preference.allergies.includes(item) }"
            @tap="toggleAllergy(item)"
          >
            <text>{{ item }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 成就徽章 -->
    <view class="section fd-card">
      <text class="section-title">🏆 成就徽章</text>
      <view class="badges">
        <view
          v-for="ach in achievementStore.allAchievements"
          :key="ach.id"
          class="badge"
          :class="{ 'badge--locked': !achievementStore.isUnlocked(ach.id) }"
        >
          <text class="badge-icon">{{ achievementStore.isUnlocked(ach.id) ? ach.icon : '🔒' }}</text>
          <text class="badge-name">{{ ach.name }}</text>
          <text class="badge-desc">{{ ach.description }}</text>
        </view>
      </view>
    </view>

    <!-- 收藏夹 -->
    <view class="section fd-card">
      <text class="section-title">❤️ 我的收藏</text>
      <view v-if="favoriteItems.length === 0">
        <fd-empty icon="❤️" text="还没有收藏哦" />
      </view>
      <view v-for="food in favoriteItems" :key="food.id" class="fav-item">
        <text class="fav-name">{{ food.name }}</text>
        <text class="fav-price">¥{{ food.priceRange[0] }}-{{ food.priceRange[1] }}</text>
        <view class="fav-remove" @tap="prefStore.toggleFavorite(food.id)">
          <text>取消</text>
        </view>
      </view>
    </view>

    <!-- 数据管理 -->
    <view class="section fd-card">
      <text class="section-title">⚙️ 数据管理</text>
      <view class="menu-item" @tap="goAi">
        <text class="menu-item-label">🤖 AI 饮食顾问</text>
        <text class="menu-item-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goStats">
        <text class="menu-item-label">📊 我的报告</text>
        <text class="menu-item-arrow">›</text>
      </view>
      <view class="fd-btn--outline" style="margin-top: 16rpx" @tap="clearData">
        <text>清除所有数据</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view v-if="isRemoteMode" class="section fd-card">
      <view class="logout-btn" @tap="onLogout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRecordStore } from '@/stores/modules/record'
import { usePreferenceStore } from '@/stores/modules/preference'
import { useAchievementStore } from '@/stores/modules/achievement'
import { useFoodStore } from '@/stores/modules/food'
import { spicyLevels } from '@/config/theme'
import { storage } from '@/utils/storage'
import config from '@/config'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdEmpty from '@/components/common/fd-empty.vue'

const recordStore = useRecordStore()
const prefStore = usePreferenceStore()
const achievementStore = useAchievementStore()
const foodStore = useFoodStore()

const isRemoteMode = computed(() => config.dataMode === 'remote')

const userAvatar = computed(() => {
  if (isRemoteMode.value) {
    try {
      const { useAuthStore } = require('@/stores/modules/auth')
      const authStore = useAuthStore()
      return authStore.user?.avatar || '😋'
    } catch { return '😋' }
  }
  return '😋'
})

const userNickname = computed(() => {
  if (isRemoteMode.value) {
    try {
      const { useAuthStore } = require('@/stores/modules/auth')
      const authStore = useAuthStore()
      return authStore.user?.nickname || '饭搭用户'
    } catch { return '饭搭用户' }
  }
  return '饭搭用户'
})

function goStats() {
  uni.navigateTo({ url: '/pages/stats/stats' })
}

function goAi() {
  uni.navigateTo({ url: '/pages/ai/ai' })
}

function onLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        try {
          const { useAuthStore } = require('@/stores/modules/auth')
          const authStore = useAuthStore()
          authStore.logout()
        } catch {
          uni.reLaunch({ url: '/pages/login/login' })
        }
      }
    },
  })
}

const allergyOptions = ['海鲜', '花生', '鸡蛋', '乳制品', '麸质', '豆制品', '香菜', '内脏']

const favoriteItems = computed(() => {
  const favIds = prefStore.favoriteIds
  return foodStore.foods.filter((f) => favIds.includes(f.id))
})

function setSpicy(level) {
  prefStore.update({ spicyLevel: level })
}

function toggleAllergy(item) {
  const list = [...prefStore.preference.allergies]
  const idx = list.indexOf(item)
  if (idx >= 0) list.splice(idx, 1)
  else list.push(item)
  prefStore.update({ allergies: list })
}

function clearData() {
  uni.showModal({
    title: '确认清除',
    content: '将清除所有本地数据，不可恢复！',
    success: (res) => {
      if (res.confirm) {
        storage.clear()
        uni.showToast({ title: '已清除', icon: 'none' })
        setTimeout(() => {
          // #ifdef H5
          location.reload()
          // #endif
        }, 1000)
      }
    },
  })
}

onMounted(async () => {
  await foodStore.load()
  prefStore.load()
  await recordStore.load()
  achievementStore.load()

  // 检查成就
  achievementStore.check({
    totalRecords: recordStore.totalRecords,
    streak: recordStore.streak,
    uniqueFoods: recordStore.uniqueFoodCount,
    favorites: prefStore.favoriteCount,
    breakfastCount: recordStore.breakfastCount,
    budgetWeekStreak: 0,
    socialJoined: 0,
  })
})
</script>

<style lang="scss" scoped>
.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 2rpx solid $fd-border;
  &:last-of-type { border-bottom: none; }
}
.menu-item-label { font-size: $fd-font-base; color: $fd-text; }
.menu-item-arrow { font-size: 40rpx; color: $fd-text-light; }

.profile-header {
  @include fd-flex-column;
  align-items: center;
  padding: $fd-space-lg 0 $fd-space-base;
}
.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, $fd-primary, $fd-secondary);
  @include fd-flex-center;
  margin-bottom: $fd-space-sm;
}
.avatar-text { font-size: 80rpx; }
.nickname {
  font-size: $fd-font-lg;
  font-weight: 700;
  color: $fd-text;
}
.achievement-progress {
  font-size: $fd-font-sm;
  color: $fd-text-light;
  margin-top: 4rpx;
}

.stats {
  display: flex;
  justify-content: space-around;
}
.stat-item {
  @include fd-flex-column;
  align-items: center;
}
.stat-num {
  font-size: $fd-font-xl;
  font-weight: 800;
  color: $fd-primary;
}
.stat-label {
  font-size: $fd-font-xs;
  color: $fd-text-light;
  margin-top: 4rpx;
}

.section { margin-bottom: 0; }
.section-title {
  @include fd-title;
  margin-bottom: $fd-space-base;
  display: block;
}

.pref-row {
  margin-bottom: $fd-space-base;
}
.pref-label {
  font-size: $fd-font-base;
  font-weight: 600;
  color: $fd-text-secondary;
  margin-bottom: $fd-space-xs;
  display: block;
}
.spicy-levels {
  display: flex;
  flex-wrap: wrap;
  gap: $fd-space-xs;
}
.spicy-item {
  padding: 8rpx 24rpx;
  border-radius: $fd-radius-round;
  border: 2rpx solid $fd-border;
  font-size: $fd-font-sm;
  &--active {
    background: $fd-primary;
    color: #fff;
    border-color: $fd-primary;
  }
}
.allergy-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $fd-space-xs;
}
.allergy-tag {
  padding: 8rpx 24rpx;
  border-radius: $fd-radius-round;
  border: 2rpx solid $fd-border;
  font-size: $fd-font-sm;
  &--active {
    background: rgba($fd-danger, 0.1);
    color: $fd-danger;
    border-color: $fd-danger;
  }
}

.badges {
  display: flex;
  flex-wrap: wrap;
  gap: $fd-space-sm;
}
.badge {
  width: calc(33.33% - 12rpx);
  @include fd-flex-column;
  align-items: center;
  padding: $fd-space-sm;
  border-radius: $fd-radius;
  background: rgba($fd-secondary, 0.15);

  &--locked {
    opacity: 0.45;
    background: #f5f5f5;
  }
}
.badge-icon { font-size: 48rpx; }
.badge-name {
  font-size: $fd-font-xs;
  font-weight: 600;
  margin-top: 4rpx;
}
.badge-desc {
  font-size: 20rpx;
  color: $fd-text-light;
  text-align: center;
}

.fav-item {
  @include fd-flex-between;
  padding: $fd-space-sm 0;
  border-bottom: 2rpx solid $fd-border;
  &:last-child { border-bottom: none; }
}
.fav-name {
  flex: 1;
  font-size: $fd-font-base;
  font-weight: 600;
}
.fav-price {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  margin-right: $fd-space-sm;
}
.fav-remove {
  font-size: $fd-font-sm;
  color: $fd-danger;
  &:active { opacity: 0.7; }
}

.logout-btn {
  text-align: center;
  padding: 20rpx;
  &:active { opacity: 0.7; }
}
.logout-text {
  font-size: $fd-font-base;
  color: $fd-danger;
  font-weight: 600;
}
</style>
