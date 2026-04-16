<template>
  <view class="fd-page">
    <fd-nav-bar title="我的好友" :show-back="true" />

    <!-- Tab 切换 -->
    <view class="tab-bar">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ 'tab-item--active': activeTab === tab.key }"
        @tap="activeTab = tab.key"
      >
        <text>{{ tab.label }}</text>
        <text v-if="tab.key === 'requests' && requests.length" class="tab-badge">{{ requests.length }}</text>
      </view>
    </view>

    <!-- 搜索好友 -->
    <view v-if="activeTab === 'search'" class="search-section">
      <view class="search-bar">
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索用户名或昵称..."
          confirm-type="search"
          @confirm="doSearch"
        />
        <view class="search-btn" @tap="doSearch">
          <text>搜索</text>
        </view>
      </view>

      <view v-if="searching" class="loading-wrap">
        <text class="fd-text-secondary">搜索中...</text>
      </view>

      <view v-else-if="searchResults.length === 0 && keyword && searched" class="empty-wrap">
        <text class="fd-text-secondary">没有找到用户</text>
      </view>

      <view v-for="user in searchResults" :key="user.id" class="user-card fd-card">
        <view class="user-avatar">
          <text class="avatar-text">{{ user.avatar || '😋' }}</text>
        </view>
        <view class="user-info">
          <text class="user-nickname">{{ user.nickname }}</text>
          <text class="user-username">@{{ user.username }}</text>
        </view>
        <view class="user-action">
          <view v-if="user.relation === 'accepted'" class="relation-badge relation-friend">
            <text>已是好友</text>
          </view>
          <view v-else-if="user.relation === 'pending'" class="relation-badge relation-pending">
            <text>待确认</text>
          </view>
          <view v-else class="add-btn" @tap="sendRequest(user)">
            <text>+ 加好友</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 好友动态 -->
    <view v-if="activeTab === 'feed'">
      <view v-if="feedLoading" class="loading-wrap">
        <text class="fd-text-secondary">加载动态中...</text>
      </view>
      <view v-else-if="feedItems.length === 0" class="empty-wrap">
        <text class="empty-icon">📰</text>
        <text class="empty-text">暂无好友动态，多加几个好友吧~</text>
      </view>

      <!-- 本周对比卡片 -->
      <view v-else class="week-compare fd-card">
        <text class="week-compare__title">📊 本周 vs 上周（我的消费）</text>
        <view class="week-compare__row">
          <view class="week-compare__item">
            <text class="week-compare__num">¥{{ myThisWeek }}</text>
            <text class="week-compare__label">本周花费</text>
          </view>
          <view class="week-compare__divider" />
          <view class="week-compare__item">
            <text class="week-compare__num">¥{{ myLastWeek }}</text>
            <text class="week-compare__label">上周花费</text>
          </view>
          <view class="week-compare__divider" />
          <view class="week-compare__item">
            <text class="week-compare__num" :class="weekDiffClass">{{ weekDiffLabel }}</text>
            <text class="week-compare__label">变化</text>
          </view>
        </view>
        <view class="week-compare__tip" v-if="weekCompareItems.length">
          <text class="week-compare__tip-text">好友本周最爱：{{ weekCompareItems.slice(0,3).map(i=>i.foodName).join('、') }}</text>
        </view>
      </view>

      <view v-for="item in feedItems" :key="item.id" class="feed-card fd-card">
        <view class="feed-header">
          <view class="user-avatar feed-avatar">
            <text class="avatar-text">{{ item.avatar || '😋' }}</text>
          </view>
          <view class="feed-meta">
            <text class="user-nickname">{{ item.nickname }}</text>
            <text class="feed-time">{{ item.date }} · {{ item.mealTypeLabel }}</text>
          </view>
        </view>
        <text class="feed-food">{{ item.foodName }}</text>
        <view v-if="item.cost" class="feed-cost">
          <text class="cost-label">花了 </text>
          <text class="cost-val">¥{{ item.cost }}</text>
        </view>
        <view v-if="item.rating" class="feed-rating">
          <text v-for="s in 5" :key="s" class="star" :class="{ 'star--active': s <= item.rating }">★</text>
        </view>
      </view>
    </view>

    <!-- 好友列表 -->
    <view v-if="activeTab === 'friends'">
      <view v-if="loading" class="loading-wrap">
        <text class="fd-text-secondary">加载中...</text>
      </view>
      <view v-else-if="friends.length === 0" class="empty-wrap">
        <text class="empty-icon">🤝</text>
        <text class="empty-text">还没有好友，快去搜索吧</text>
      </view>
      <view v-for="f in friends" :key="f.friendshipId" class="user-card fd-card">
        <view class="user-avatar">
          <text class="avatar-text">{{ f.avatar || '😋' }}</text>
        </view>
        <view class="user-info">
          <text class="user-nickname">{{ f.nickname }}</text>
          <text class="user-username">@{{ f.username }}</text>
        </view>
        <view class="user-action">
          <view class="invite-btn" @tap="inviteToGroup(f)">
            <text>邀请拼饭</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 好友请求 -->
    <view v-if="activeTab === 'requests'">
      <view v-if="loading" class="loading-wrap">
        <text class="fd-text-secondary">加载中...</text>
      </view>
      <view v-else-if="requests.length === 0" class="empty-wrap">
        <text class="empty-icon">📩</text>
        <text class="empty-text">暂无好友请求</text>
      </view>
      <view v-for="req in requests" :key="req.friendshipId" class="user-card fd-card">
        <view class="user-avatar">
          <text class="avatar-text">{{ req.avatar || '😋' }}</text>
        </view>
        <view class="user-info">
          <text class="user-nickname">{{ req.nickname }}</text>
          <text class="user-username">@{{ req.username }}</text>
        </view>
        <view class="request-actions">
          <view class="accept-btn" @tap="acceptRequest(req)">
            <text>接受</text>
          </view>
          <view class="reject-btn" @tap="rejectRequest(req)">
            <text>拒绝</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { get, post, del } from '@/utils/http'
import { mealTypeLabel } from '@/utils/date'
import { useRecordStore } from '@/stores/modules/record'
import { useBudgetStore } from '@/stores/modules/budget'
import FdNavBar from '@/components/common/fd-nav-bar.vue'

const activeTab = ref('feed')
const tabs = [
  { key: 'feed', label: '动态' },
  { key: 'friends', label: '好友' },
  { key: 'requests', label: '申请' },
  { key: 'search', label: '搜索' },
]

const friends = ref([])
const requests = ref([])
const feedItems = ref([])
const feedLoading = ref(false)
const keyword = ref('')
const searchResults = ref([])
const loading = ref(false)
const searching = ref(false)
const searched = ref(false)

const recordStore = useRecordStore()
const budgetStore = useBudgetStore()

// ── 本周 vs 上周对比 ──────────────────────────────────
function getWeekStart(offsetWeeks = 0) {
  const now = new Date()
  const day = now.getDay() || 7  // 1=Mon ... 7=Sun
  const mon = new Date(now)
  mon.setDate(now.getDate() - day + 1 - offsetWeeks * 7)
  mon.setHours(0, 0, 0, 0)
  return mon
}

function isoDate(d) {
  return d.toISOString().slice(0, 10)
}

const myThisWeek = computed(() => {
  const start = getWeekStart(0)
  const end = new Date()
  return Math.round(
    budgetStore.monthlyExpenses
      .filter(e => e.date >= isoDate(start) && e.date <= isoDate(end))
      .reduce((s, e) => s + e.amount, 0)
  )
})

const myLastWeek = computed(() => {
  const start = getWeekStart(1)
  const end = getWeekStart(0)
  end.setDate(end.getDate() - 1)
  return Math.round(
    budgetStore.monthlyExpenses
      .filter(e => e.date >= isoDate(start) && e.date <= isoDate(end))
      .reduce((s, e) => s + e.amount, 0)
  )
})

const weekDiff = computed(() => myThisWeek.value - myLastWeek.value)
const weekDiffLabel = computed(() => {
  const d = weekDiff.value
  if (d === 0) return '持平'
  return (d > 0 ? '+¥' : '-¥') + Math.abs(d)
})
const weekDiffClass = computed(() => weekDiff.value > 0 ? 'compare-up' : weekDiff.value < 0 ? 'compare-down' : '')

const weekCompareItems = computed(() => {
  const start = isoDate(getWeekStart(0))
  return feedItems.value.filter(i => i.date >= start)
})

async function loadFeed() {
  feedLoading.value = true
  try {
    const data = await get('/api/friends/feed')
    feedItems.value = (data || []).map(item => ({
      ...item,
      mealTypeLabel: mealTypeLabel(item.mealType),
    }))
  } catch {
    feedItems.value = []
  } finally {
    feedLoading.value = false
  }
}

async function loadFriends() {
  loading.value = true
  try {
    const data = await get('/api/friends')
    friends.value = data
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function loadRequests() {
  try {
    const data = await get('/api/friends/requests')
    requests.value = data
  } catch {
    // 静默失败
  }
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  searching.value = true
  searched.value = false
  searchResults.value = []
  try {
    const data = await get(`/api/friends/search?keyword=${encodeURIComponent(kw)}`)
    searchResults.value = data
    searched.value = true
  } catch {
    uni.showToast({ title: '搜索失败', icon: 'none' })
  } finally {
    searching.value = false
  }
}

async function sendRequest(user) {
  try {
    await post('/api/friends/request', { userId: user.id })
    user.relation = 'pending'
    uni.showToast({ title: '好友请求已发送', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '发送失败', icon: 'none' })
  }
}

async function acceptRequest(req) {
  try {
    await post(`/api/friends/accept/${req.friendshipId}`)
    requests.value = requests.value.filter(r => r.friendshipId !== req.friendshipId)
    friends.value.push({ ...req })
    uni.showToast({ title: '已添加好友 🎉', icon: 'success' })
  } catch {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

async function rejectRequest(req) {
  try {
    await post(`/api/friends/reject/${req.friendshipId}`)
    requests.value = requests.value.filter(r => r.friendshipId !== req.friendshipId)
    uni.showToast({ title: '已拒绝', icon: 'none' })
  } catch {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

function inviteToGroup(friend) {
  uni.showToast({ title: `邀请 ${friend.nickname} 拼饭`, icon: 'none' })
  // 跳转到拼饭广场，可扩展为带邀请参数
  setTimeout(() => {
    uni.switchTab({ url: '/pages/social/social' })
  }, 1000)
}

watch(activeTab, (tab) => {
  if (tab === 'feed') loadFeed()
  if (tab === 'friends') loadFriends()
  if (tab === 'requests') loadRequests()
})

onMounted(() => {
  loadFeed()
  loadFriends()
  loadRequests()
  budgetStore.load()
  recordStore.load()
})
</script>

<style lang="scss" scoped>
.tab-bar {
  display: flex;
  background: $fd-card-bg;
  border-bottom: 2rpx solid $fd-border;
  margin-bottom: $fd-space-sm;
}
.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 24rpx 0;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  position: relative;
  &--active {
    color: $fd-primary;
    font-weight: 700;
    border-bottom: 4rpx solid $fd-primary;
  }
}
.tab-badge {
  background: $fd-primary;
  color: #fff;
  font-size: 20rpx;
  border-radius: 20rpx;
  padding: 2rpx 10rpx;
  min-width: 32rpx;
  text-align: center;
}

.search-section {
  padding: 0 $fd-space-md;
}
.search-bar {
  display: flex;
  gap: $fd-space-sm;
  margin-bottom: $fd-space-base;
}
.search-input {
  flex: 1;
  height: 72rpx;
  background: $fd-bg;
  border-radius: $fd-radius-round;
  padding: 0 $fd-space-md;
  font-size: $fd-font-sm;
  color: $fd-text;
}
.search-btn {
  background: $fd-primary;
  color: #fff;
  border-radius: $fd-radius-round;
  padding: 0 $fd-space-md;
  height: 72rpx;
  display: flex;
  align-items: center;
  font-size: $fd-font-sm;
  font-weight: 600;
}

.loading-wrap {
  padding: $fd-space-xl;
  text-align: center;
}
.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx $fd-space-md;
  gap: $fd-space-sm;
}
.empty-icon { font-size: 80rpx; }
.empty-text { font-size: $fd-font-sm; color: $fd-text-secondary; }

.user-card {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
  margin: 0 $fd-space-md $fd-space-sm;
}
.user-avatar {
  width: 80rpx;
  height: 80rpx;
  background: $fd-bg;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.avatar-text { font-size: 44rpx; }
.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.user-nickname {
  font-size: $fd-font-base;
  font-weight: 600;
  color: $fd-text;
}
.user-username {
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
}

.user-action { flex-shrink: 0; }
.add-btn {
  background: $fd-primary;
  color: #fff;
  border-radius: $fd-radius-round;
  padding: 10rpx 24rpx;
  font-size: $fd-font-sm;
  font-weight: 600;
  &:active { opacity: 0.85; }
}
.invite-btn {
  background: rgba($fd-accent, 0.15);
  color: $fd-accent;
  border-radius: $fd-radius-round;
  padding: 10rpx 24rpx;
  font-size: $fd-font-sm;
  font-weight: 600;
  &:active { opacity: 0.85; }
}
.relation-badge {
  border-radius: $fd-radius-round;
  padding: 10rpx 20rpx;
  font-size: $fd-font-xs;
  font-weight: 600;
  &.relation-friend { background: rgba($fd-success, 0.12); color: $fd-success; }
  &.relation-pending { background: $fd-border; color: $fd-text-secondary; }
}

.request-actions {
  display: flex;
  gap: $fd-space-xs;
  flex-shrink: 0;
}
.accept-btn {
  background: $fd-success;
  color: #fff;
  border-radius: $fd-radius-round;
  padding: 10rpx 20rpx;
  font-size: $fd-font-sm;
  font-weight: 600;
  &:active { opacity: 0.85; }
}
.reject-btn {
  background: $fd-border;
  color: $fd-text-secondary;
  border-radius: $fd-radius-round;
  padding: 10rpx 20rpx;
  font-size: $fd-font-sm;
  font-weight: 600;
  &:active { opacity: 0.85; }
}

/* 好友动态 */
.feed-card {
  margin: 0 $fd-space-md $fd-space-sm;
}

/* 本周对比卡片 */
.week-compare {
  margin: 0 $fd-space-md $fd-space-sm;
  &__title {
    font-size: $fd-font-sm;
    font-weight: 700;
    color: $fd-text-secondary;
    display: block;
    margin-bottom: $fd-space-sm;
  }
  &__row {
    display: flex;
    align-items: center;
    justify-content: space-around;
    padding: $fd-space-sm 0;
    border-top: 2rpx solid $fd-border;
    border-bottom: 2rpx solid $fd-border;
    margin-bottom: $fd-space-sm;
  }
  &__divider {
    width: 2rpx;
    height: 50rpx;
    background: $fd-border;
  }
  &__item {
    text-align: center;
    flex: 1;
  }
  &__num {
    display: block;
    font-size: $fd-font-lg;
    font-weight: 800;
    color: $fd-primary;
    margin-bottom: 4rpx;
  }
  &__label {
    font-size: $fd-font-xs;
    color: $fd-text-secondary;
  }
  &__tip {
    padding: 8rpx 0;
  }
  &__tip-text {
    font-size: $fd-font-xs;
    color: $fd-text-secondary;
  }
}
.compare-up { color: $fd-danger !important; }
.compare-down { color: $fd-success !important; }
.feed-header {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
  margin-bottom: $fd-space-xs;
}
.feed-avatar {
  width: 72rpx;
  height: 72rpx;
  flex-shrink: 0;
}
.feed-meta {
  @include fd-flex-column;
  gap: 2rpx;
}
.feed-time {
  font-size: $fd-font-xs;
  color: $fd-text-light;
}
.feed-food {
  font-size: $fd-font-md;
  font-weight: 600;
  color: $fd-text;
  display: block;
  margin-bottom: $fd-space-xs;
}
.feed-cost {
  display: flex;
  align-items: center;
}
.cost-label {
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
}
.cost-val {
  font-size: $fd-font-sm;
  font-weight: 700;
  color: $fd-primary;
}
.feed-rating {
  display: flex;
  gap: 4rpx;
  margin-top: 6rpx;
}
.star {
  font-size: 28rpx;
  color: $fd-border;
  &--active { color: #FFB800; }
}
</style>
