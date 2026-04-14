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
          <view class="bill-row">
            <text class="bill-label">人均预估</text>
            <text class="bill-value bill-amount">¥{{ bill.perPerson }}</text>
          </view>
          <view class="bill-divider" />
          <view class="bill-row bill-total-row">
            <text class="bill-label">合计预估</text>
            <text class="bill-value bill-total">¥{{ bill.totalEstimate }}</text>
          </view>
          <text class="bill-tip">* 账单为预估金额，请实际结账后手动调整</text>
        </view>
        <view v-else class="bill-loading">
          <text class="fd-text-secondary">计算中...</text>
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
const joining = ref(false)
const hasVoted = ref(false)
const votedCandidateId = ref(null)
const messages = ref([])
const chatInput = ref('')
const chatScrollTop = ref(0)
const wsConnected = ref(false)

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
    }
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

async function loadBill() {
  try {
    bill.value = await get(`/api/social/groups/${groupId.value}/bill`)
  } catch (e) {
    console.warn('[Bill] 加载AA账单失败', e)
  }
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

function connectWs() {
  socket = createSocialSocket(groupId.value, {
    onOpen: () => { wsConnected.value = true },
    onClose: () => { wsConnected.value = false },
    onError: () => { wsConnected.value = false },
    onMessage: (msg) => {
      // 投票事件：同步最新候选数据
      if (msg.type === 'VOTE' && msg.data && group.value) {
        group.value.candidates = msg.data
      }
      // 加入事件：人数+1，满员时加载账单
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
.bill-total-row { }
.bill-total { font-size: $fd-font-md; font-weight: 800; color: $fd-primary; }
.bill-tip { display: block; font-size: $fd-font-xs; color: $fd-text-light; margin-top: 12rpx; }
.bill-loading { padding: 20rpx; text-align: center; }

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
