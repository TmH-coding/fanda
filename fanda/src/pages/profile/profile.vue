<template>
  <view class="fd-page">
    <fd-nav-bar title="我的">
      <template #right>
        <view v-if="pendingCount > 0" class="sync-badge" @tap="manualSync">
          <text class="sync-badge__num">{{ pendingCount }}</text>
          <text class="sync-badge__icon">↑</text>
        </view>
      </template>
    </fd-nav-bar>

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

    <!-- 打卡热力图 -->
    <view class="section fd-card">
      <fd-heatmap :count-map="heatmapData" @cell-tap="onHeatmapTap" />
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
      <view class="section-header">
        <text class="section-title">🏆 成就徽章</text>
        <text class="section-sub">{{ achievementStore.progress }}</text>
      </view>

      <!-- 分类 tab -->
      <scroll-view scroll-x class="ach-tabs">
        <view
          v-for="cat in achievementCategories"
          :key="cat.key"
          class="ach-tab"
          :class="{ 'ach-tab--active': achTab === cat.key }"
          @tap="achTab = cat.key"
        >
          <text>{{ cat.icon }} {{ cat.label }}</text>
        </view>
      </scroll-view>

      <!-- 徽章网格 -->
      <view class="badges">
        <view
          v-for="ach in filteredAchievements"
          :key="ach.id"
          class="badge"
          :class="{
            'badge--locked': !achievementStore.isUnlocked(ach.id),
            'badge--secret': ach.secret && !achievementStore.isUnlocked(ach.id),
          }"
          @tap="showAchDetail(ach)"
        >
          <text class="badge-icon">
            {{ (ach.secret && !achievementStore.isUnlocked(ach.id)) ? '❓' : (achievementStore.isUnlocked(ach.id) ? ach.icon : '🔒') }}
          </text>
          <text class="badge-name">
            {{ (ach.secret && !achievementStore.isUnlocked(ach.id)) ? '???' : ach.name }}
          </text>
          <!-- 进度条 -->
          <view v-if="!achievementStore.isUnlocked(ach.id) && !ach.secret" class="badge-progress-wrap">
            <view
              class="badge-progress-bar"
              :style="{ width: (ach.progress(achStats) / ach.maxProgress * 100) + '%' }"
            />
          </view>
          <text v-if="!achievementStore.isUnlocked(ach.id) && !ach.secret" class="badge-progress-text">
            {{ ach.progress(achStats) }}/{{ ach.maxProgress }}
          </text>
        </view>
      </view>
    </view>

    <!-- 收藏夹 -->
    <view class="section fd-card">
      <view class="section-header">
        <text class="section-title">❤️ 我的收藏</text>
        <view v-if="favoriteItems.length > 0" class="batch-toggle" @tap="toggleFavBatch">
          <text>{{ favBatchMode ? '完成' : '批量' }}</text>
        </view>
      </view>
      <view v-if="favoriteItems.length === 0">
        <fd-empty icon="❤️" text="还没有收藏哦" />
      </view>
      <view v-else>
        <view v-if="favBatchMode" class="batch-bar">
          <text class="batch-hint">已选 {{ favSelected.size }} / {{ favoriteItems.length }}</text>
          <view class="batch-btns">
            <view class="batch-btn batch-btn--select-all" @tap="selectAllFav">
              <text>全选</text>
            </view>
            <view class="batch-btn batch-btn--remove" @tap="batchRemoveFav" :class="{ 'batch-btn--disabled': favSelected.size === 0 }">
              <text>删除 {{ favSelected.size > 0 ? `(${favSelected.size})` : '' }}</text>
            </view>
          </view>
        </view>
        <view v-for="food in favoriteItems" :key="food.id" class="fav-item" @tap="favBatchMode && toggleFavSelect(food.id)">
          <view v-if="favBatchMode" class="batch-check" :class="{ 'batch-check--on': favSelected.has(food.id) }">
            <text>{{ favSelected.has(food.id) ? '✓' : '' }}</text>
          </view>
          <text class="fav-name">{{ food.name }}</text>
          <text class="fav-price">¥{{ food.priceRange[0] }}-{{ food.priceRange[1] }}</text>
          <view v-if="!favBatchMode" class="fav-remove" @tap="prefStore.toggleFavorite(food.id)">
            <text>取消</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 黑名单 -->
    <view class="section fd-card">
      <view class="section-header">
        <text class="section-title">🚫 不想吃列表</text>
        <view v-if="blacklistItems.length > 0" class="batch-toggle" @tap="toggleBlBatch">
          <text>{{ blBatchMode ? '完成' : '批量' }}</text>
        </view>
      </view>
      <text class="section-sub" style="display:block;margin-bottom:12rpx;">永久从推荐中排除</text>
      <view v-if="blacklistItems.length === 0">
        <fd-empty icon="🚫" text="黑名单为空，推荐不受限制" />
      </view>
      <view v-else>
        <view v-if="blBatchMode" class="batch-bar">
          <text class="batch-hint">已选 {{ blSelected.size }} / {{ blacklistItems.length }}</text>
          <view class="batch-btns">
            <view class="batch-btn batch-btn--select-all" @tap="selectAllBl">
              <text>全选</text>
            </view>
            <view class="batch-btn batch-btn--remove" @tap="batchRemoveBl" :class="{ 'batch-btn--disabled': blSelected.size === 0 }">
              <text>移出 {{ blSelected.size > 0 ? `(${blSelected.size})` : '' }}</text>
            </view>
          </view>
        </view>
        <view v-for="food in blacklistItems" :key="food.id" class="fav-item" @tap="blBatchMode && toggleBlSelect(food.id)">
          <view v-if="blBatchMode" class="batch-check" :class="{ 'batch-check--on': blSelected.has(food.id) }">
            <text>{{ blSelected.has(food.id) ? '✓' : '' }}</text>
          </view>
          <text class="fav-name blacklist-name">{{ food.name }}</text>
          <text class="fav-price">{{ food.category }}</text>
          <view v-if="!blBatchMode" class="fav-remove fav-restore" @tap="prefStore.toggleBlacklist(food.id)">
            <text>移除</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 我的自定义食物 -->
    <view v-if="isRemoteMode && myFoods.length > 0" class="section fd-card">
      <view class="section-header">
        <text class="section-title">🍽️ 我录入的食物</text>
        <text class="section-sub">{{ myFoods.length }} 个</text>
      </view>
      <view v-for="food in myFoods" :key="food.id" class="fav-item">
        <text class="fav-name">{{ food.name }}</text>
        <text class="fav-price">{{ food.category }}</text>
        <view class="fav-remove" @tap="deleteMyFood(food.id)">
          <text>删除</text>
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
      <view class="menu-item" @tap="goFriends">
        <text class="menu-item-label">🤝 我的好友</text>
        <text class="menu-item-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goStats">
        <text class="menu-item-label">📊 我的报告</text>
        <text class="menu-item-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goFoodCreate">
        <text class="menu-item-label">🍽️ 录入自定义食物</text>
        <text class="menu-item-arrow">›</text>
      </view>
      <view class="fd-btn--outline" style="margin-top: 16rpx" @tap="clearData">
        <text>清除所有数据</text>
      </view>
      <view class="fd-btn--outline export-data-btn" @tap="exportData">
        <text>{{ exporting ? '导出中...' : '📤 导出本地数据' }}</text>
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
import { computed, onMounted, ref } from 'vue'
import { useRecordStore } from '@/stores/modules/record'
import { usePreferenceStore } from '@/stores/modules/preference'
import { useAchievementStore } from '@/stores/modules/achievement'
import { useFoodStore } from '@/stores/modules/food'
import { spicyLevels } from '@/config/theme'
import { storage } from '@/utils/storage'
import config from '@/config'
import { ACHIEVEMENT_CATEGORIES } from '@/data/achievements'
import { useOfflineSync } from '@/composables/useOfflineSync'
import { flush, queueSize } from '@/utils/offlineQueue'
import { getService } from '@/services/factory'
import { get, del } from '@/utils/http'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdEmpty from '@/components/common/fd-empty.vue'
import FdHeatmap from '@/components/common/fd-heatmap.vue'

const recordStore = useRecordStore()
const prefStore = usePreferenceStore()
const achievementStore = useAchievementStore()
const foodStore = useFoodStore()

const isRemoteMode = computed(() => config.dataMode === 'remote')

// 打卡热力图数据：日期 -> 当天记录条数
const heatmapData = computed(() => {
  const map = {}
  for (const r of recordStore.records) {
    map[r.date] = (map[r.date] || 0) + 1
  }
  return map
})

function onHeatmapTap(cell) {
  uni.showToast({ title: `${cell.date} 记录 ${cell.count} 餐`, icon: 'none' })
}

// 离线队列同步状态
const { pendingCount, refresh: refreshSync } = useOfflineSync()

async function manualSync() {
  if (queueSize() === 0) return
  uni.showToast({ title: '同步中...', icon: 'loading', duration: 2000 })
  try {
    const recordService = getService('record')
    const budgetService = getService('budget')
    const count = await flush({
      record: {
        add:    (p) => recordService.save(p),
        remove: (p) => recordService.delete(p),
      },
      budget: {
        add:    (p) => budgetService.addExpense(p),
        remove: (p) => budgetService.deleteExpense(p),
      },
    })
    refreshSync()
    uni.showToast({ title: count > 0 ? `已同步 ${count} 条` : '无需同步', icon: 'success' })
  } catch {
    uni.showToast({ title: '同步失败，请检查网络', icon: 'none' })
  }
}

// 成就分类 tab
const achTab = ref('streak')
const achievementCategories = ACHIEVEMENT_CATEGORIES

const filteredAchievements = computed(() => {
  return achievementStore.allAchievements.filter(a => a.category === achTab.value)
})

// 传给成就 progress() 的统计数据
const achStats = computed(() => ({
  totalRecords:   recordStore.totalRecords,
  streak:         recordStore.streak,
  uniqueFoods:    recordStore.uniqueFoodCount,
  favorites:      prefStore.favoriteCount,
  breakfastCount: recordStore.breakfastCount,
  blacklistCount: prefStore.blacklistCount,
  socialJoined:   0,
  socialCreated:  0,
  friendCount:    0,
  budgetWeekStreak: 0,
  budgetMonthOk:  0,
  lateNightCount: 0,
  vegStreak:      0,
}))

function showAchDetail(ach) {
  const unlocked = achievementStore.isUnlocked(ach.id)
  const isSecret = ach.secret && !unlocked
  uni.showModal({
    title: isSecret ? '神秘成就' : ach.name,
    content: isSecret ? '完成某个特殊条件后解锁' : `${ach.description}${unlocked ? '\n\n✅ 已解锁！' : `\n\n进度：${ach.progress(achStats.value)}/${ach.maxProgress}`}`,
    showCancel: false,
    confirmText: '知道了',
  })
}

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

function goFoodCreate() {
  uni.navigateTo({ url: '/pages/food-create/food-create' })
}

function goAi() {
  uni.navigateTo({ url: '/pages/ai/ai' })
}

function goFriends() {
  uni.navigateTo({ url: '/pages/friends/friends' })
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

const blacklistItems = computed(() => {
  const ids = prefStore.blacklistIds
  return foodStore.foods.filter((f) => ids.includes(f.id))
})

// 收藏批量操作
const favBatchMode = ref(false)
const favSelected = ref(new Set())

function toggleFavBatch() {
  favBatchMode.value = !favBatchMode.value
  favSelected.value = new Set()
}
function toggleFavSelect(id) {
  const s = new Set(favSelected.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  favSelected.value = s
}
function selectAllFav() {
  if (favSelected.value.size === favoriteItems.value.length) {
    favSelected.value = new Set()
  } else {
    favSelected.value = new Set(favoriteItems.value.map((f) => f.id))
  }
}
function batchRemoveFav() {
  if (favSelected.value.size === 0) return
  const count = favSelected.value.size
  uni.showModal({
    title: '批量取消收藏',
    content: `确认取消 ${count} 个收藏？`,
    success: (res) => {
      if (res.confirm) {
        for (const id of favSelected.value) {
          prefStore.toggleFavorite(id)
        }
        favSelected.value = new Set()
        favBatchMode.value = false
        uni.showToast({ title: `已取消 ${count} 个收藏`, icon: 'success' })
      }
    },
  })
}

// 黑名单批量操作
const blBatchMode = ref(false)
const blSelected = ref(new Set())

function toggleBlBatch() {
  blBatchMode.value = !blBatchMode.value
  blSelected.value = new Set()
}
function toggleBlSelect(id) {
  const s = new Set(blSelected.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  blSelected.value = s
}
function selectAllBl() {
  if (blSelected.value.size === blacklistItems.value.length) {
    blSelected.value = new Set()
  } else {
    blSelected.value = new Set(blacklistItems.value.map((f) => f.id))
  }
}
function batchRemoveBl() {
  if (blSelected.value.size === 0) return
  const count = blSelected.value.size
  uni.showModal({
    title: '批量移出黑名单',
    content: `确认将 ${count} 个菜品移出黑名单？`,
    success: (res) => {
      if (res.confirm) {
        for (const id of blSelected.value) {
          prefStore.toggleBlacklist(id)
        }
        blSelected.value = new Set()
        blBatchMode.value = false
        uni.showToast({ title: `已移出 ${count} 个`, icon: 'success' })
      }
    },
  })
}

// 我的自定义食物
const myFoods = ref([])

async function loadMyFoods() {
  if (!isRemoteMode.value) return
  try {
    const res = await get('/api/foods/mine')
    myFoods.value = res?.data ?? []
  } catch {
    // 忽略加载失败
  }
}

async function deleteMyFood(foodCode) {
  uni.showModal({
    title: '确认删除',
    content: '删除后该食物将从推荐中移除，不可恢复。',
    success: async (res) => {
      if (res.confirm) {
        try {
          await del('/api/foods/' + foodCode)
          myFoods.value = myFoods.value.filter((f) => f.id !== foodCode)
          foodStore.loaded = false
          await foodStore.load()
          uni.showToast({ title: '已删除', icon: 'success' })
        } catch {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    },
  })
}

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

// ── 数据导出 ───────────────────────────────────────────
const exporting = ref(false)

async function exportData() {
  exporting.value = true
  try {
    const exportObj = {
      exportedAt: new Date().toISOString(),
      version: '1.0',
      records: recordStore.records,
      preferences: prefStore.$state,
    }
    const json = JSON.stringify(exportObj, null, 2)
    // #ifdef H5
    const blob = new Blob([json], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `fanda-export-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
    uni.showToast({ title: '已下载 JSON 文件', icon: 'success' })
    // #endif
    // #ifndef H5
    const fs = uni.getFileSystemManager()
    const filePath = `${uni.env.USER_DATA_PATH}/fanda-export-${Date.now()}.json`
    fs.writeFileSync(filePath, json, 'utf8')
    uni.shareFileMessage({
      filePath,
      fileName: 'fanda-export.json',
      success: () => uni.showToast({ title: '导出成功', icon: 'success' }),
      fail: () => uni.showToast({ title: `已保存到：${filePath}`, icon: 'none', duration: 3000 }),
    })
    // #endif
  } catch (e) {
    uni.showToast({ title: '导出失败', icon: 'none' })
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  await foodStore.load()
  prefStore.load()
  await recordStore.load()
  achievementStore.load()

  // 检查成就
  achievementStore.check(achStats.value)

  // 加载自定义食物
  loadMyFoods()
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

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $fd-space-base;
}
.section-sub {
  font-size: $fd-font-xs;
  color: $fd-text-light;
}

.ach-tabs {
  display: flex;
  white-space: nowrap;
  margin-bottom: $fd-space-base;
}
.ach-tab {
  display: inline-flex;
  padding: 8rpx 24rpx;
  border-radius: $fd-radius-round;
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  background: $fd-bg;
  margin-right: $fd-space-xs;
  flex-shrink: 0;
  &--active {
    background: $fd-primary;
    color: #fff;
  }
}

.badge-progress-wrap {
  width: 100%;
  height: 8rpx;
  background: $fd-border;
  border-radius: $fd-radius-round;
  overflow: hidden;
  margin-top: 6rpx;
}
.badge-progress-bar {
  height: 100%;
  background: $fd-primary;
  border-radius: $fd-radius-round;
  transition: width 0.4s ease;
}
.badge-progress-text {
  font-size: 18rpx;
  color: $fd-text-light;
  margin-top: 2rpx;
}
.badge-desc { display: none; }
.badge--secret { background: rgba(#888, 0.08); }

.section-title {
  @include fd-title;
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
.fav-restore { color: $fd-accent; }
.blacklist-name { text-decoration: line-through; color: $fd-text-secondary; }

/* 批量操作 */
.batch-toggle {
  font-size: $fd-font-sm;
  color: $fd-primary;
  font-weight: 600;
  padding: 4rpx 16rpx;
  &:active { opacity: 0.7; }
}
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $fd-space-sm 0;
  margin-bottom: $fd-space-xs;
  border-bottom: 2rpx solid $fd-border;
}
.batch-hint {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
}
.batch-btns {
  display: flex;
  gap: $fd-space-xs;
}
.batch-btn {
  font-size: $fd-font-xs;
  padding: 6rpx 20rpx;
  border-radius: $fd-radius-round;
  &:active { opacity: 0.8; }
  &--select-all {
    border: 2rpx solid $fd-border;
    color: $fd-text-secondary;
  }
  &--remove {
    background: $fd-danger;
    color: #fff;
    font-weight: 600;
  }
  &--disabled {
    background: $fd-border;
    color: $fd-text-light;
  }
}
.batch-check {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid $fd-border;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: $fd-space-sm;
  flex-shrink: 0;
  font-size: $fd-font-sm;
  color: #fff;
  &--on {
    background: $fd-primary;
    border-color: $fd-primary;
  }
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

.export-data-btn { margin-top: 12rpx; }

.sync-badge {
  display: flex;
  align-items: center;
  gap: 4rpx;
  background: rgba($fd-danger, 0.1);
  border-radius: 24rpx;
  padding: 4rpx 12rpx;
  &:active { opacity: 0.7; }
}
.sync-badge__num {
  font-size: $fd-font-xs;
  color: $fd-danger;
  font-weight: 700;
}
.sync-badge__icon {
  font-size: $fd-font-xs;
  color: $fd-danger;
}
</style>
