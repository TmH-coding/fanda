<template>
  <view class="fd-page">
    <fd-nav-bar title="拼饭广场" />

    <!-- 发起拼饭按钮 -->
    <view class="create-btn" @tap="showCreate = true">
      <text class="create-btn__text">🍽️ 发起拼饭</text>
    </view>

    <!-- 拼饭列表 -->
    <fd-skeleton v-if="listLoading" :rows="5" />
    <view v-else-if="socialStore.openGroups.length === 0">
      <fd-empty icon="🤝" text="暂时没有拼饭局" btn-text="发起一个" @action="showCreate = true" />
    </view>

    <view v-if="!listLoading" v-for="group in socialStore.openGroups" :key="group.id" class="group-card fd-card" @tap="goDetail(group.id)">
      <view class="group-header">
        <text class="group-avatar">{{ group.avatar }}</text>
        <view class="group-creator">
          <text class="group-name">{{ group.creator }}</text>
          <text class="group-time">⏰ {{ group.time }}</text>
        </view>
        <view class="group-people">
          <text class="group-count">{{ group.currentPeople }}/{{ group.maxPeople }}</text>
          <text class="group-count-label">人</text>
        </view>
      </view>

      <text class="group-title">{{ group.title }}</text>
      <text class="group-location">📍 {{ group.location }}</text>

      <view class="group-tags">
        <text v-for="tag in group.tags" :key="tag" class="fd-tag--accent">{{ tag }}</text>
      </view>

      <!-- 投票 -->
      <view class="vote-section">
        <text class="vote-title">投票选餐厅</text>
        <view v-for="(candidate, idx) in group.candidates" :key="idx" class="vote-item" @tap="onVote(group.id, candidate)">
          <text class="vote-name">{{ candidate }}</text>
          <view class="vote-bar">
            <view class="vote-fill" :style="{ width: votePercent(group, candidate) + '%' }"></view>
          </view>
          <text class="vote-count">{{ group.votes[candidate] || 0 }}票</text>
        </view>
      </view>

      <view v-if="group.currentPeople < group.maxPeople" class="group-join fd-btn" @tap.stop="onJoin(group.id)">
        <text>加入 🙋</text>
      </view>
      <view v-else class="group-full">
        <text class="fd-text-light">已满员</text>
      </view>

      <!-- 留言板（远程模式） -->
      <view v-if="isRemote" class="message-board">
        <view class="message-board__header">
          <text class="message-board__title">留言板</text>
        </view>
        <view v-if="!messages[group.id]" class="message-loading">
          <text class="fd-text-light" @tap.stop="loadMessages(group.id)">查看留言 ›</text>
        </view>
        <view v-else>
          <view v-if="messages[group.id].length === 0" class="message-empty">
            <text class="fd-text-light">还没有留言，来说一句吧</text>
          </view>
          <view v-for="msg in messages[group.id]" :key="msg.id" class="message-item">
            <text class="message-user">{{ msg.username }}</text>
            <text class="message-content">{{ msg.content }}</text>
            <text class="message-time">{{ formatMsgTime(msg.createdAt) }}</text>
          </view>
          <view class="message-input-row" @tap.stop>
            <input
              class="message-input"
              v-model="inputMap[group.id]"
              placeholder="说点什么..."
              maxlength="100"
              confirm-type="send"
              @confirm="sendMessage(group.id)"
            />
            <view class="message-send" @tap.stop="sendMessage(group.id)">
              <text>发送</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 已结束的拼饭 -->
    <view v-if="!listLoading && socialStore.closedGroups.length > 0" class="closed-section">
      <text class="closed-section-title">已结束的拼饭</text>
      <view v-for="group in socialStore.closedGroups" :key="group.id" class="group-card group-card--closed fd-card" @tap="goDetail(group.id)">
        <view class="group-header">
          <text class="group-avatar group-avatar--muted">{{ group.avatar }}</text>
          <view class="group-creator">
            <text class="group-name">{{ group.creator }}</text>
            <text class="group-time">⏰ {{ group.time }}</text>
          </view>
          <view class="closed-badge">
            <text class="closed-badge-text">已结束</text>
          </view>
        </view>
        <text class="group-title group-title--muted">{{ group.title }}</text>
        <text class="group-location">📍 {{ group.location }}</text>
        <view class="closed-actions">
          <view class="review-btn" @tap.stop="goReviews(group.id)">
            <text>📝 看评价</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 发起拼饭弹窗 -->
    <fd-modal v-model:visible="showCreate" title="发起拼饭" @confirm="onCreate">
      <view class="form-item">
        <text class="form-label">标题</text>
        <input class="form-input" v-model="form.title" placeholder="想吃什么？一起来！" />
      </view>
      <view class="form-item">
        <text class="form-label">时间</text>
        <input class="form-input" v-model="form.time" placeholder="如 12:00" />
      </view>
      <view class="form-item">
        <text class="form-label">地点</text>
        <view class="location-picker" @tap="pickLocation">
          <text :class="form.location ? 'location-text' : 'location-placeholder'">
            {{ form.location || '点击选择地点…' }}
          </text>
          <text class="location-icon">📍</text>
        </view>
      </view>
      <view class="form-item">
        <text class="form-label">人数上限</text>
        <input class="form-input" type="number" v-model="form.maxPeople" placeholder="4" />
      </view>
    </fd-modal>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useSocialStore } from '@/stores/modules/social'
import { generateId } from '@/utils/format'
import { get, post } from '@/utils/http'
import config from '@/config'
import FdNavBar from '@/components/common/fd-nav-bar.vue'
import FdEmpty from '@/components/common/fd-empty.vue'
import FdModal from '@/components/common/fd-modal.vue'
import FdSkeleton from '@/components/common/fd-skeleton.vue'

const socialStore = useSocialStore()
const showCreate = ref(false)
const listLoading = ref(true)
const isRemote = config.dataMode === 'remote'

// 留言板：groupId -> 消息列表（undefined = 未加载）
const messages = ref({})
// 输入框：groupId -> 当前输入内容
const inputMap = ref({})

const form = ref({
  title: '',
  time: '',
  location: '',
  maxPeople: '4',
})

async function loadMessages(groupId) {
  try {
    const data = await get(`/api/social/groups/${groupId}/messages`)
    messages.value = { ...messages.value, [groupId]: data || [] }
    if (!inputMap.value[groupId]) inputMap.value[groupId] = ''
  } catch {
    uni.showToast({ title: '加载留言失败', icon: 'none' })
  }
}

async function sendMessage(groupId) {
  const content = (inputMap.value[groupId] || '').trim()
  if (!content) return
  try {
    const msg = await post(`/api/social/groups/${groupId}/messages`, { content })
    messages.value[groupId] = [...(messages.value[groupId] || []), msg]
    inputMap.value[groupId] = ''
  } catch {
    uni.showToast({ title: '发送失败', icon: 'none' })
  }
}

function formatMsgTime(createdAt) {
  if (!createdAt) return ''
  const d = new Date(createdAt)
  const now = new Date()
  const diffMin = Math.floor((now - d) / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  if (diffMin < 1440) return `${Math.floor(diffMin / 60)}小时前`
  return d.toLocaleDateString()
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/social-detail/social-detail?id=${id}` })
}

function goReviews(id) {
  uni.navigateTo({ url: `/pages/social-detail/social-detail?id=${id}&tab=reviews` })
}

function votePercent(group, candidate) {
  const total = Object.values(group.votes).reduce((s, v) => s + v, 0)
  if (total === 0) return 0
  return Math.round(((group.votes[candidate] || 0) / total) * 100)
}

function onVote(groupId, candidate) {
  socialStore.vote(groupId, candidate)
}

function onJoin(groupId) {
  socialStore.join(groupId)
  uni.showToast({ title: '已加入 🎉', icon: 'none' })
}

function onCreate() {
  if (!form.value.title) {
    uni.showToast({ title: '请填写标题', icon: 'none' })
    return
  }
  const group = {
    id: generateId('g_'),
    creator: '我',
    avatar: '😎',
    title: form.value.title,
    time: form.value.time || '12:00',
    location: form.value.location || '待定',
    maxPeople: Number(form.value.maxPeople) || 4,
    currentPeople: 1,
    candidates: [],
    votes: {},
    status: 'open',
    tags: [],
  }
  socialStore.create(group)
  showCreate.value = false
  form.value = { title: '', time: '', location: '', maxPeople: '4' }
  uni.showToast({ title: '发起成功 🎉', icon: 'none' })
}

function pickLocation() {
  // #ifndef H5
  uni.chooseLocation({
    success: (res) => {
      // 组合成"名称（地址）"形式，名称为空时只用地址
      const name = res.name || ''
      const addr = res.address || ''
      form.value.location = name ? (addr ? `${name}（${addr}）` : name) : addr
    },
    fail: () => {
      // 用户取消或不支持时，保持原值，不弹 toast 打断体验
    },
  })
  // #endif
  // #ifdef H5
  // H5 不支持 chooseLocation，弹出文字输入框作为降级方案
  uni.showModal({
    title: '输入地点',
    editable: true,
    placeholderText: '请输入地点名称',
    content: form.value.location,
    success: (res) => {
      if (res.confirm && res.content) {
        form.value.location = res.content
      }
    },
  })
  // #endif
}

onMounted(async () => {
  await socialStore.load()
  listLoading.value = false
})
</script>

<style lang="scss" scoped>
.create-btn {
  margin: $fd-space-base $fd-space-md;
  @include fd-btn;
  @include fd-flex-center;
  padding: 24rpx;
  &__text { font-size: $fd-font-md; }
}

.group-card {
  margin-bottom: $fd-space-sm;
}
.group-header {
  display: flex;
  align-items: center;
  margin-bottom: $fd-space-sm;
}
.group-avatar {
  font-size: 60rpx;
  margin-right: $fd-space-sm;
}
.group-creator { flex: 1; }
.group-name {
  font-size: $fd-font-base;
  font-weight: 600;
  display: block;
}
.group-time {
  font-size: $fd-font-xs;
  color: $fd-text-light;
}
.group-people {
  @include fd-flex-column;
  align-items: center;
}
.group-count {
  font-size: $fd-font-lg;
  font-weight: 800;
  color: $fd-primary;
}
.group-count-label {
  font-size: $fd-font-xs;
  color: $fd-text-light;
}

.group-title {
  font-size: $fd-font-md;
  font-weight: 700;
  display: block;
  margin-bottom: 6rpx;
}
.group-location {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  display: block;
  margin-bottom: $fd-space-sm;
}
.group-tags {
  display: flex;
  gap: $fd-space-xs;
  margin-bottom: $fd-space-sm;
}

.vote-section {
  margin: $fd-space-sm 0;
}
.vote-title {
  font-size: $fd-font-sm;
  font-weight: 600;
  color: $fd-text-secondary;
  margin-bottom: $fd-space-xs;
  display: block;
}
.vote-item {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
  margin-bottom: 8rpx;
  &:active { opacity: 0.7; }
}
.vote-name {
  width: 140rpx;
  font-size: $fd-font-sm;
}
.vote-bar {
  flex: 1;
  height: 24rpx;
  background: #f0f0f0;
  border-radius: 12rpx;
  overflow: hidden;
}
.vote-fill {
  height: 100%;
  background: linear-gradient(90deg, $fd-primary, $fd-primary-light);
  border-radius: 12rpx;
  transition: width 0.3s ease;
}
.vote-count {
  width: 80rpx;
  font-size: $fd-font-xs;
  color: $fd-text-light;
  text-align: right;
}

.group-join {
  margin-top: $fd-space-sm;
  text-align: center;
}
.group-full {
  text-align: center;
  margin-top: $fd-space-sm;
}

/* 留言板 */
.message-board {
  margin-top: $fd-space-base;
  border-top: 2rpx solid $fd-border;
  padding-top: $fd-space-sm;
  &__header { margin-bottom: $fd-space-xs; }
  &__title {
    font-size: $fd-font-sm;
    font-weight: 600;
    color: $fd-text-secondary;
  }
}
.message-loading, .message-empty {
  padding: 12rpx 0;
}
.message-item {
  display: flex;
  align-items: baseline;
  gap: $fd-space-xs;
  padding: 8rpx 0;
  border-bottom: 1rpx solid $fd-border;
  &:last-of-type { border-bottom: none; }
}
.message-user {
  font-size: $fd-font-xs;
  font-weight: 700;
  color: $fd-primary;
  flex-shrink: 0;
}
.message-content {
  font-size: $fd-font-sm;
  color: $fd-text;
  flex: 1;
}
.message-time {
  font-size: 20rpx;
  color: $fd-text-light;
  flex-shrink: 0;
}
.message-input-row {
  display: flex;
  gap: $fd-space-xs;
  margin-top: $fd-space-sm;
}
.message-input {
  flex: 1;
  height: 64rpx;
  background: $fd-bg;
  border-radius: $fd-radius-round;
  padding: 0 $fd-space-sm;
  font-size: $fd-font-sm;
  border: 2rpx solid $fd-border;
}
.message-send {
  background: $fd-primary;
  color: #fff;
  border-radius: $fd-radius-round;
  padding: 0 $fd-space-base;
  height: 64rpx;
  display: flex;
  align-items: center;
  font-size: $fd-font-sm;
  font-weight: 600;
  flex-shrink: 0;
  &:active { opacity: 0.85; }
}

/* 已结束拼饭 */
.closed-section { margin-top: $fd-space-base; }
.closed-section-title {
  display: block;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  font-weight: 600;
  padding: 0 $fd-space-md $fd-space-xs;
}
.group-card--closed {
  opacity: 0.75;
}
.group-avatar--muted { filter: grayscale(60%); }
.group-title--muted { color: $fd-text-secondary !important; }
.closed-badge {
  background: $fd-border;
  border-radius: $fd-radius-round;
  padding: 6rpx 18rpx;
}
.closed-badge-text {
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
}
.closed-actions {
  margin-top: $fd-space-sm;
  display: flex;
  justify-content: flex-end;
}
.review-btn {
  background: rgba($fd-accent, 0.12);
  color: $fd-accent;
  border-radius: $fd-radius-round;
  padding: 10rpx 28rpx;
  font-size: $fd-font-sm;
  font-weight: 600;
  &:active { opacity: 0.8; }
}

/* 表单 */
.form-item {
  margin-bottom: $fd-space-base;
}
.form-label {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  margin-bottom: 8rpx;
  display: block;
}
.form-input {
  width: 100%;
  height: 72rpx;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: 0 $fd-space-sm;
  font-size: $fd-font-base;
}
.location-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72rpx;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: 0 $fd-space-sm;
  background: $fd-bg;
  &:active { border-color: $fd-primary; }
}
.location-text {
  font-size: $fd-font-base;
  color: $fd-text;
  flex: 1;
}
.location-placeholder {
  font-size: $fd-font-base;
  color: $fd-text-light;
  flex: 1;
}
.location-icon {
  font-size: $fd-font-md;
  flex-shrink: 0;
}
</style>
