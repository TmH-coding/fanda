<template>
  <view class="fd-page">
    <fd-nav-bar :title="group ? group.title : '拼饭详情'" :show-back="true" />

    <view v-if="!group" class="loading-wrap">
      <text class="fd-text-secondary">加载中...</text>
    </view>

    <block v-else>
      <!-- 群信息卡片 -->
      <view class="info-card fd-card">
        <view class="info-header">
          <view class="avatar-wrap">
            <text class="avatar-emoji">{{ group.avatar || '🍜' }}</text>
          </view>
          <view class="info-main">
            <text class="group-title">{{ group.title }}</text>
            <view class="info-tags">
              <text v-for="tag in group.tags" :key="tag.id" class="tag">{{ tag.tag }}</text>
            </view>
          </view>
          <view class="status-badge" :class="'status-' + group.status">
            <text>{{ statusLabel }}</text>
          </view>
        </view>
        <view class="info-meta">
          <text class="meta-item">🕐 {{ group.mealTime }}</text>
          <text class="meta-item">📍 {{ group.location }}</text>
          <text class="meta-item">👥 {{ group.currentPeople }}/{{ group.maxPeople }}人</text>
        </view>

        <!-- 加入按钮 -->
        <view
          v-if="group.status === 'open' && !isMember"
          class="fd-btn"
          :class="{ 'fd-btn--loading': joining }"
          @tap="joinGroup"
        >
          <text>{{ joining ? '加入中...' : '加入拼饭' }}</text>
        </view>
        <view v-else-if="isMember" class="joined-badge">
          <text>✅ 已加入</text>
        </view>
        <view v-else-if="group.status === 'full'" class="full-badge">
          <text>🔥 已满员</text>
        </view>
      </view>

      <!-- 投票区 -->
      <view class="section fd-card" v-if="group.candidates && group.candidates.length">
        <text class="section-title">🗳️ 选餐投票</text>
        <view class="candidates">
          <view
            v-for="c in group.candidates"
            :key="c.id"
            class="candidate-item"
            :class="{ 'candidate-item--voted': hasVoted && votedCandidateId === c.id }"
            @tap="vote(c)"
          >
            <view class="candidate-info">
              <text class="candidate-name">{{ c.name }}</text>
              <text class="candidate-votes">{{ c.votes }}票</text>
            </view>
            <view class="candidate-bar-bg">
              <view
                class="candidate-bar"
                :style="{ width: votePercent(c) + '%' }"
              />
            </view>
          </view>
        </view>
        <text v-if="hasVoted" class="voted-tip">已投票 ✓</text>
        <text v-else-if="!isMember" class="voted-tip fd-text-secondary">加入后可投票</text>
      </view>

      <!-- 成员列表 -->
      <view class="section fd-card">
        <text class="section-title">👥 成员 ({{ group.members.length }})</text>
        <view class="members">
          <view v-for="m in group.members" :key="m.id" class="member-item">
            <text class="member-avatar">😋</text>
            <text class="member-id">用户 #{{ m.userId }}</text>
          </view>
        </view>
      </view>

      <!-- AA账单 -->
      <view class="section fd-card" v-if="group.status === 'full' || group.status === 'closed'">
        <text class="section-title">🧾 AA账单</text>
        <view v-if="bill" class="bill-wrap">
          <view class="bill-winner">
            <text class="bill-label">胜出餐厅</text>
            <text class="bill-value bill-winner-name">🏆 {{ bill.winner }}</text>
          </view>
          <view class="bill-row">
            <text class="bill-label">参与人数</text>
            <text class="bill-value">{{ bill.memberCount }}人</text>
          </view>

          <!-- 实际金额输入 -->
          <view class="bill-actual-row">
            <text class="bill-label">实际总价</text>
            <view class="bill-actual-input-wrap">
              <text class="bill-currency">¥</text>
              <input
                class="bill-actual-input"
                type="digit"
                :value="actualTotal"
                :placeholder="String(bill.totalEstimate)"
                @input="onActualInput"
              />
            </view>
            <view class="bill-calc-btn" @tap="calcAA">
              <text>计算</text>
            </view>
          </view>

          <view class="bill-divider" />
          <view class="bill-row bill-total-row">
            <text class="bill-label">人均 AA</text>
            <text class="bill-value bill-total">¥{{ aaPerPerson }}</text>
          </view>
          <text v-if="actualTotal" class="bill-tip">基于实际总价 ¥{{ actualTotal }} 计算</text>
          <text v-else class="bill-tip">* 输入实际总价后点击「计算」得出人均</text>
        </view>
        <view v-else class="bill-loading">
          <text class="fd-text-secondary">计算中...</text>
        </view>
      </view>

      <!-- 活动评价 -->
      <view class="section fd-card" v-if="group.status === 'full' || group.status === 'closed'">
        <text class="section-title">⭐ 活动评价</text>

        <!-- 已有评价列表 -->
        <view v-if="reviews.length" class="review-list">
          <view v-for="r in reviews" :key="r.id" class="review-item">
            <view class="review-header">
              <text class="review-author">{{ r.username }}</text>
              <view class="review-stars">
                <text v-for="s in 5" :key="s" class="star" :class="{ 'star--on': s <= r.rating }">★</text>
              </view>
            </view>
            <text v-if="r.content" class="review-content">{{ r.content }}</text>
          </view>
        </view>
        <text v-else class="fd-text-secondary" style="display:block;margin-bottom:24rpx;">暂无评价</text>

        <!-- 提交评价（仅成员且未评价） -->
        <view v-if="isMember && !myReview" class="review-form">
          <view class="review-divider" />
          <text class="review-form-label">发表你的评价</text>
          <view class="star-picker">
            <text
              v-for="s in 5"
              :key="s"
              class="star star-pick"
              :class="{ 'star--on': s <= reviewRating }"
              @tap="reviewRating = s"
            >★</text>
          </view>
          <input
            class="review-input"
            v-model="reviewContent"
            placeholder="说说你的感受... (选填)"
            maxlength="200"
          />
          <view class="fd-btn" :class="{ 'fd-btn--loading': submittingReview }" @tap="submitReview">
            <text>{{ submittingReview ? '提交中...' : '提交评价' }}</text>
          </view>
        </view>
        <view v-else-if="myReview" class="my-review-tip">
          <text>✅ 已评价</text>
        </view>
      </view>

      <!-- 实时消息 -->
      <view class="section fd-card">
        <view class="chat-header">
          <text class="section-title">💬 实时动态</text>
          <view class="ws-status" :class="wsConnected ? 'ws-on' : 'ws-off'">
            <text>{{ wsConnected ? '● 实时' : '○ 离线' }}</text>
          </view>
        </view>
        <scroll-view class="chat-list" scroll-y :scroll-top="chatScrollTop">
          <view v-if="messages.length === 0" class="chat-empty">
            <text class="fd-text-secondary">暂无动态，快来发消息吧~</text>
          </view>
          <view v-for="(msg, i) in messages" :key="i" class="chat-msg" :class="'msg-' + msg.type.toLowerCase()">
            <view class="msg-bubble">
              <text class="msg-sender" v-if="msg.sender">{{ msg.sender }}</text>
              <text class="msg-content">{{ msg.content }}</text>
              <text class="msg-time">{{ formatTime(msg.timestamp) }}</text>
            </view>
          </view>
        </scroll-view>

        <!-- 发送聊天 -->
        <view v-if="isMember && wsConnected" class="chat-input-row">
          <input
            class="chat-input"
            v-model="chatInput"
            placeholder="说点什么..."
            maxlength="200"
            confirm-type="send"
            @confirm="sendChat"
          />
          <view class="chat-send-btn" @tap="sendChat">
            <text>发送</text>
          </view>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { get, post } from '@/utils/http'
import { createSocialSocket } from '@/utils/websocket'
import FdNavBar from '@/components/common/fd-nav-bar.vue'

const props = defineProps({ groupId: { type: [String, Number], default: null } })

const group = ref(null)
const bill = ref(null)
const actualTotal = ref('')       // 用户输入的实际总价
const aaPerPerson = ref('--')     // 计算出的人均
const joining = ref(false)
const hasVoted = ref(false)
const votedCandidateId = ref(null)
const messages = ref([])
const chatInput = ref('')
const chatScrollTop = ref(0)
const wsConnected = ref(false)
const reviews = ref([])
const reviewRating = ref(5)
const reviewContent = ref('')
const submittingReview = ref(false)
const myReview = ref(false)

let socket = null
let currentUserId = null

// 获取当前用户 ID
try {
  const savedUser = uni.getStorageSync('fd_user')
  if (savedUser) currentUserId = savedUser.id
} catch {}

const groupId = computed(() => {
  if (props.groupId) return props.groupId
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  return page?.options?.id || page?.$page?.fullPath?.split('id=')[1]
})

const statusLabel = computed(() => {
  const map = { open: '招募中', full: '已满员', closed: '已结束' }
  return map[group.value?.status] || group.value?.status
})

const isMember = computed(() => {
  if (!group.value || !currentUserId) return false
  return group.value.members.some(m => m.userId === currentUserId)
})

const totalVotes = computed(() => {
  if (!group.value?.candidates) return 0
  return group.value.candidates.reduce((s, c) => s + (c.votes || 0), 0)
})

function votePercent(candidate) {
  if (!totalVotes.value) return 0
  return Math.round((candidate.votes / totalVotes.value) * 100)
}

async function loadGroup() {
  try {
    const data = await get(`/api/social/groups/${groupId.value}`)
    group.value = data
    if (data.status === 'full' || data.status === 'closed') {
      loadBill()
      loadReviews()
    }
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

async function loadBill() {
  try {
    bill.value = await get(`/api/social/groups/${groupId.value}/bill`)
    // 初始化人均为预估值
    aaPerPerson.value = bill.value.perPerson
  } catch (e) {
    console.warn('[Bill] 加载AA账单失败', e)
  }
}

function onActualInput(e) {
  actualTotal.value = e.detail.value
}

function calcAA() {
  const total = Number(actualTotal.value)
  if (!total || !bill.value?.memberCount) {
    uni.showToast({ title: '请输入有效金额', icon: 'none' })
    return
  }
  const per = Math.ceil(total / bill.value.memberCount)
  aaPerPerson.value = per
  uni.showToast({ title: `人均 ¥${per}`, icon: 'none' })
}

async function joinGroup() {
  if (joining.value) return
  joining.value = true
  try {
    const data = await post(`/api/social/groups/${groupId.value}/join`)
    group.value = data
    uni.showToast({ title: '加入成功！', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '加入失败', icon: 'none' })
  } finally {
    joining.value = false
  }
}

async function vote(candidate) {
  if (hasVoted.value || !isMember.value) return
  try {
    const data = await post(`/api/social/groups/${groupId.value}/vote`, { candidateId: candidate.id })
    group.value = data
    hasVoted.value = true
    votedCandidateId.value = candidate.id
    uni.showToast({ title: `投票给 ${candidate.name}`, icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '投票失败', icon: 'none' })
  }
}

function sendChat() {
  const text = chatInput.value.trim()
  if (!text || !socket) return
  socket.sendChat(text)
  chatInput.value = ''
}

function addMessage(msg) {
  messages.value.push(msg)
  if (messages.value.length > 100) messages.value.shift()
  // 滚动到底部
  setTimeout(() => { chatScrollTop.value = 999999 }, 50)
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

async function loadReviews() {
  try {
    const data = await get(`/api/social/groups/${groupId.value}/reviews`)
    reviews.value = data
    myReview.value = data.some(r => r.userId === currentUserId)
  } catch (e) {
    console.warn('[Review] 加载评价失败', e)
  }
}

async function submitReview() {
  if (submittingReview.value) return
  submittingReview.value = true
  try {
    const r = await post(`/api/social/groups/${groupId.value}/reviews`, {
      rating: reviewRating.value,
      content: reviewContent.value.trim(),
    })
    reviews.value.unshift(r)
    myReview.value = true
    reviewContent.value = ''
    uni.showToast({ title: '评价成功 ⭐', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '提交失败', icon: 'none' })
  } finally {
    submittingReview.value = false
  }
}

function connectWs() {
  socket = createSocialSocket(groupId.value, {
    onOpen: () => { wsConnected.value = true },
    onClose: () => { wsConnected.value = false },
    onError: () => { wsConnected.value = false },
    onMessage: async (msg) => {
      if (msg.type === 'VOTE' && msg.data && group.value) {
        group.value.candidates = msg.data
      }
      if (msg.type === 'JOIN' && group.value) {
        await loadGroup()
      }
      addMessage(msg)
    }
  })
}

onMounted(async () => {
  await loadGroup()
  connectWs()
})

onUnmounted(() => {
  socket?.close()
})
</script>

<style lang="scss" scoped>
.loading-wrap {
  display: flex;
  justify-content: center;
  padding: $fd-space-xl;
}

.info-card {
  margin: 24rpx;
}
.info-header {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  margin-bottom: 24rpx;
}
.avatar-wrap {
  width: 80rpx;
  height: 80rpx;
  background: $fd-bg;
  border-radius: $fd-radius;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.avatar-emoji { font-size: 44rpx; }
.info-main { flex: 1; }
.group-title { display: block; font-size: $fd-font-md; font-weight: 700; color: $fd-text; margin-bottom: 8rpx; }
.info-tags { display: flex; flex-wrap: wrap; gap: 8rpx; }
.tag {
  font-size: $fd-font-xs;
  color: $fd-primary;
  background: rgba(255,107,107,0.1);
  padding: 4rpx 12rpx;
  border-radius: $fd-radius-round;
}
.status-badge {
  padding: 6rpx 16rpx;
  border-radius: $fd-radius-round;
  font-size: $fd-font-xs;
  font-weight: 600;
  flex-shrink: 0;
  &.status-open { background: rgba(46,213,115,0.15); color: $fd-success; }
  &.status-full { background: rgba(255,107,107,0.15); color: $fd-primary; }
  &.status-closed { background: $fd-border; color: $fd-text-secondary; }
}
.info-meta { display: flex; flex-wrap: wrap; gap: 16rpx; margin-bottom: 24rpx; }
.meta-item { font-size: $fd-font-sm; color: $fd-text-secondary; }
.joined-badge, .full-badge {
  text-align: center;
  padding: 20rpx;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
}

.section { margin: 0 24rpx 24rpx; }
.section-title { display: block; font-size: $fd-font-sm; font-weight: 600; color: $fd-text-secondary; margin-bottom: 20rpx; }

/* 投票 */
.candidates { display: flex; flex-direction: column; gap: 20rpx; }
.candidate-item {
  cursor: pointer;
  &--voted .candidate-bar { background: $fd-accent; }
}
.candidate-info { display: flex; justify-content: space-between; margin-bottom: 8rpx; }
.candidate-name { font-size: $fd-font-base; color: $fd-text; font-weight: 500; }
.candidate-votes { font-size: $fd-font-sm; color: $fd-text-secondary; }
.candidate-bar-bg {
  height: 16rpx;
  background: $fd-border;
  border-radius: $fd-radius-round;
  overflow: hidden;
}
.candidate-bar {
  height: 100%;
  background: $fd-primary;
  border-radius: $fd-radius-round;
  transition: width 0.4s ease;
}
.voted-tip { display: block; text-align: center; font-size: $fd-font-xs; color: $fd-success; margin-top: 16rpx; }

/* 成员 */
.members { display: flex; flex-wrap: wrap; gap: 16rpx; }
.member-item { display: flex; flex-direction: column; align-items: center; gap: 8rpx; }
.member-avatar { font-size: 48rpx; }
.member-id { font-size: $fd-font-xs; color: $fd-text-secondary; }

/* AA账单 */
.bill-wrap { display: flex; flex-direction: column; gap: 16rpx; }
.bill-row { display: flex; justify-content: space-between; align-items: center; }
.bill-winner { display: flex; justify-content: space-between; align-items: center; padding-bottom: 16rpx; border-bottom: 2rpx solid $fd-border; margin-bottom: 8rpx; }
.bill-label { font-size: $fd-font-sm; color: $fd-text-secondary; }
.bill-value { font-size: $fd-font-sm; color: $fd-text; font-weight: 500; }
.bill-winner-name { font-size: $fd-font-base; font-weight: 700; color: $fd-primary; }
.bill-amount { color: $fd-primary; }
.bill-divider { height: 2rpx; background: $fd-border; margin: 8rpx 0; }
.bill-total { font-size: $fd-font-md; font-weight: 800; color: $fd-primary; }
.bill-tip { display: block; font-size: $fd-font-xs; color: $fd-text-light; margin-top: 12rpx; }
.bill-loading { padding: 20rpx; text-align: center; }
.bill-actual-row {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
}
.bill-actual-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: 0 16rpx;
  height: 64rpx;
}
.bill-currency {
  font-size: $fd-font-base;
  color: $fd-text-secondary;
  margin-right: 8rpx;
}
.bill-actual-input {
  flex: 1;
  font-size: $fd-font-base;
  font-weight: 600;
  color: $fd-text;
}
.bill-calc-btn {
  background: $fd-primary;
  color: #fff;
  border-radius: $fd-radius-sm;
  padding: 12rpx 28rpx;
  font-size: $fd-font-sm;
  font-weight: 600;
  &:active { opacity: 0.85; }
}

/* 评价 */
.review-list { display: flex; flex-direction: column; gap: 20rpx; margin-bottom: 24rpx; }
.review-item {
  background: $fd-bg;
  border-radius: $fd-radius-sm;
  padding: 20rpx;
}
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx; }
.review-author { font-size: $fd-font-sm; font-weight: 600; color: $fd-text; }
.review-stars { display: flex; gap: 4rpx; }
.star { font-size: $fd-font-base; color: $fd-border; &--on { color: #FFD700; } }
.review-content { display: block; font-size: $fd-font-sm; color: $fd-text-secondary; line-height: 1.6; }
.review-divider { height: 2rpx; background: $fd-border; margin: 16rpx 0; }
.review-form-label { display: block; font-size: $fd-font-sm; color: $fd-text-secondary; margin-bottom: 16rpx; }
.star-picker { display: flex; gap: 12rpx; margin-bottom: 20rpx; }
.star-pick { font-size: 48rpx; cursor: pointer; transition: transform 0.1s; &:active { transform: scale(1.2); } }
.review-input {
  width: 100%;
  min-height: 80rpx;
  background: $fd-bg;
  border: 2rpx solid $fd-border;
  border-radius: $fd-radius-sm;
  padding: 16rpx;
  font-size: $fd-font-sm;
  color: $fd-text;
  margin-bottom: 20rpx;
}
.my-review-tip { text-align: center; padding: 16rpx; font-size: $fd-font-sm; color: $fd-success; }

/* 聊天 */
.chat-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.ws-status { font-size: $fd-font-xs; font-weight: 600; &.ws-on { color: $fd-success; } &.ws-off { color: $fd-text-light; } }
.chat-list { height: 400rpx; }
.chat-empty { padding: 40rpx; text-align: center; }
.chat-msg { margin-bottom: 16rpx; }
.msg-bubble {
  display: inline-block;
  max-width: 80%;
  background: $fd-bg;
  border-radius: $fd-radius;
  padding: 16rpx 20rpx;
}
.msg-join .msg-bubble, .msg-full .msg-bubble, .msg-info .msg-bubble {
  background: rgba(78,205,196,0.1);
  text-align: center;
  max-width: 100%;
}
.msg-vote .msg-bubble { background: rgba(255,230,109,0.2); }
.msg-sender { display: block; font-size: $fd-font-xs; color: $fd-primary; font-weight: 600; margin-bottom: 4rpx; }
.msg-content { display: block; font-size: $fd-font-sm; color: $fd-text; }
.msg-time { display: block; font-size: 20rpx; color: $fd-text-light; margin-top: 4rpx; text-align: right; }

.chat-input-row {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 2rpx solid $fd-border;
}
.chat-input {
  flex: 1;
  background: $fd-bg;
  border-radius: $fd-radius-round;
  padding: 16rpx 24rpx;
  font-size: $fd-font-sm;
  color: $fd-text;
}
.chat-send-btn {
  background: $fd-primary;
  color: #fff;
  border-radius: $fd-radius-round;
  padding: 16rpx 32rpx;
  font-size: $fd-font-sm;
  font-weight: 600;
}
</style>
