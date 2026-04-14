<template>
  <view class="fd-page">
    <fd-nav-bar title="AI 饮食顾问" :show-back="true" />

    <!-- 功能卡片区 -->
    <view class="feature-grid">
      <view class="feature-card" @tap="startRecommend">
        <text class="feature-icon">🍽️</text>
        <text class="feature-name">今日推荐</text>
        <text class="feature-desc">AI 结合你的偏好智能选餐</text>
      </view>
      <view class="feature-card" @tap="loadNutrition">
        <text class="feature-icon">📊</text>
        <text class="feature-name">营养分析</text>
        <text class="feature-desc">30天饮食健康报告</text>
      </view>
    </view>

    <!-- 营养报告展示 -->
    <view v-if="nutritionReport" class="report-card fd-card">
      <view class="report-header">
        <text class="report-title">📊 营养分析报告</text>
        <text class="report-close" @tap="nutritionReport = ''">×</text>
      </view>
      <text class="report-content">{{ nutritionReport }}</text>
    </view>

    <!-- 推荐结果展示 -->
    <view v-if="recommendResult" class="report-card fd-card">
      <view class="report-header">
        <text class="report-title">🍽️ AI 推荐</text>
        <text class="report-close" @tap="recommendResult = ''">×</text>
      </view>
      <view class="scene-row">
        <text class="scene-label">推荐餐次</text>
        <view class="scene-tabs">
          <view
            v-for="s in scenes"
            :key="s.key"
            class="scene-tab"
            :class="{ 'scene-tab--active': scene === s.key }"
            @tap="changeScene(s.key)"
          >
            <text>{{ s.label }}</text>
          </view>
        </view>
      </view>
      <text class="report-content">{{ recommendResult }}</text>
    </view>

    <!-- 对话区 -->
    <view class="chat-section fd-card">
      <text class="chat-title">💬 问问小饭</text>
      <scroll-view class="chat-scroll" scroll-y :scroll-top="scrollTop">
        <view v-if="messages.length === 0" class="chat-placeholder">
          <text class="chat-placeholder-text">试试问："今天中午吃什么？" 或 "我最近营养均衡吗？"</text>
        </view>
        <view v-for="(msg, i) in messages" :key="i" class="chat-msg" :class="'chat-msg--' + msg.role">
          <view class="msg-bubble">
            <text class="msg-text">{{ msg.content }}</text>
          </view>
          <text v-if="msg.role === 'assistant'" class="msg-avatar">🤖</text>
        </view>
        <view v-if="streaming" class="chat-msg chat-msg--assistant">
          <view class="msg-bubble">
            <text class="msg-text">{{ streamingText }}<text class="cursor">▌</text></text>
          </view>
          <text class="msg-avatar">🤖</text>
        </view>
      </scroll-view>

      <view class="chat-input-row">
        <input
          class="chat-input"
          v-model="inputText"
          placeholder="问小饭..."
          maxlength="200"
          confirm-type="send"
          @confirm="sendMessage"
          :disabled="streaming || loading"
        />
        <view
          class="chat-send"
          :class="{ 'chat-send--disabled': streaming || loading || !inputText.trim() }"
          @tap="sendMessage"
        >
          <text>{{ streaming ? '...' : '发送' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { post, get } from '@/utils/http'
import { getToken } from '@/utils/http'
import FdNavBar from '@/components/common/fd-nav-bar.vue'

const messages = ref([])
const inputText = ref('')
const streaming = ref(false)
const streamingText = ref('')
const scrollTop = ref(0)
const loading = ref(false)
const nutritionReport = ref('')
const recommendResult = ref('')
const scene = ref('lunch')

const scenes = [
  { key: 'breakfast', label: '早餐' },
  { key: 'lunch', label: '午餐' },
  { key: 'dinner', label: '晚餐' },
]

// 获取 base URL（适配 H5 代理）
function getBaseUrl() {
  // H5 开发模式下走 Vite 代理，直接用空字符串
  return ''
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || streaming.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  streaming.value = true
  streamingText.value = ''

  try {
    const token = getToken()
    // 使用 uni.request 的流式响应（H5 模式下用 fetch SSE）
    await streamChat(text, token)
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '小饭暂时不在线，请稍后再试 😅' })
  } finally {
    streaming.value = false
    scrollToBottom()
  }
}

async function streamChat(message, token) {
  // H5 环境使用 fetch 支持 SSE 流式读取
  const response = await fetch('/api/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ message }),
  })

  if (!response.ok) {
    throw new Error('请求失败')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let fullText = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    // SSE 格式：data: xxx\n\n
    const lines = chunk.split('\n')
    for (const line of lines) {
      if (line.startsWith('data:')) {
        const data = line.slice(5).trim()
        if (data && data !== '[DONE]') {
          fullText += data
          streamingText.value = fullText
          scrollToBottom()
        }
      }
    }
  }

  // 流结束后转为正式消息
  if (fullText) {
    messages.value.push({ role: 'assistant', content: fullText })
  }
  streamingText.value = ''
}

async function startRecommend() {
  loading.value = true
  recommendResult.value = ''
  try {
    const res = await post('/api/ai/recommend', { scene: scenes.find(s => s.key === scene.value)?.label || '午餐' })
    recommendResult.value = res
  } catch (e) {
    uni.showToast({ title: '推荐获取失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function loadNutrition() {
  loading.value = true
  nutritionReport.value = ''
  try {
    const res = await get('/api/ai/nutrition')
    nutritionReport.value = res
  } catch (e) {
    uni.showToast({ title: '营养分析失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function changeScene(key) {
  scene.value = key
  recommendResult.value = ''
  startRecommend()
}

function scrollToBottom() {
  setTimeout(() => { scrollTop.value = 999999 }, 50)
}
</script>

<style lang="scss" scoped>
.feature-grid {
  display: flex;
  gap: $fd-space-base;
  padding: $fd-space-md;
}
.feature-card {
  flex: 1;
  background: $fd-card-bg;
  border-radius: $fd-radius;
  box-shadow: $fd-shadow;
  padding: $fd-space-md;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  &:active { opacity: 0.85; transform: scale(0.97); }
}
.feature-icon { font-size: 64rpx; }
.feature-name {
  font-size: $fd-font-base;
  font-weight: 700;
  color: $fd-text;
}
.feature-desc {
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  text-align: center;
}

.report-card {
  margin: 0 $fd-space-md $fd-space-base;
}
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $fd-space-sm;
}
.report-title {
  font-size: $fd-font-sm;
  font-weight: 700;
  color: $fd-text;
}
.report-close {
  font-size: 40rpx;
  color: $fd-text-light;
  padding: 0 8rpx;
}
.report-content {
  font-size: $fd-font-sm;
  color: $fd-text;
  line-height: 1.8;
  display: block;
  white-space: pre-wrap;
}
.scene-row {
  display: flex;
  align-items: center;
  gap: $fd-space-sm;
  margin-bottom: $fd-space-sm;
}
.scene-label {
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
  flex-shrink: 0;
}
.scene-tabs {
  display: flex;
  gap: 8rpx;
}
.scene-tab {
  font-size: $fd-font-xs;
  padding: 6rpx 20rpx;
  border-radius: $fd-radius-round;
  background: $fd-border;
  color: $fd-text-secondary;
  &--active {
    background: $fd-primary;
    color: #fff;
  }
}

.chat-section {
  margin: 0 $fd-space-md $fd-space-md;
  display: flex;
  flex-direction: column;
}
.chat-title {
  font-size: $fd-font-sm;
  font-weight: 700;
  color: $fd-text;
  margin-bottom: $fd-space-sm;
  display: block;
}
.chat-scroll {
  height: 500rpx;
  margin-bottom: $fd-space-sm;
}
.chat-placeholder {
  padding: $fd-space-lg;
  text-align: center;
}
.chat-placeholder-text {
  font-size: $fd-font-sm;
  color: $fd-text-light;
}

.chat-msg {
  display: flex;
  align-items: flex-end;
  gap: $fd-space-xs;
  margin-bottom: $fd-space-sm;
  &--user {
    flex-direction: row-reverse;
    .msg-bubble {
      background: $fd-primary;
      .msg-text { color: #fff; }
    }
  }
  &--assistant {
    flex-direction: row;
  }
}
.msg-bubble {
  max-width: 75%;
  background: $fd-bg;
  border-radius: $fd-radius;
  padding: 16rpx 20rpx;
}
.msg-text {
  font-size: $fd-font-sm;
  color: $fd-text;
  line-height: 1.7;
  white-space: pre-wrap;
}
.msg-avatar {
  font-size: 36rpx;
  flex-shrink: 0;
}
.cursor {
  color: $fd-primary;
  animation: blink 0.8s infinite;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.chat-input-row {
  display: flex;
  gap: $fd-space-sm;
  border-top: 2rpx solid $fd-border;
  padding-top: $fd-space-sm;
}
.chat-input {
  flex: 1;
  background: $fd-bg;
  border-radius: $fd-radius-round;
  padding: 16rpx 24rpx;
  font-size: $fd-font-sm;
  color: $fd-text;
}
.chat-send {
  background: $fd-primary;
  color: #fff;
  border-radius: $fd-radius-round;
  padding: 16rpx 32rpx;
  font-size: $fd-font-sm;
  font-weight: 600;
  &--disabled {
    background: $fd-border;
    color: $fd-text-light;
  }
}
</style>
