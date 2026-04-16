<template>
  <view class="fd-page">
    <fd-nav-bar title="AI 饮食顾问" :show-back="true" />

    <!-- 功能卡片区 2×2 -->
    <view class="feature-grid">
      <view class="feature-card" @tap="startRecommend">
        <text class="feature-icon">🍽️</text>
        <text class="feature-name">今日推荐</text>
        <text class="feature-desc">AI 智能选餐</text>
      </view>
      <view class="feature-card" @tap="loadNutrition">
        <text class="feature-icon">📊</text>
        <text class="feature-name">营养分析</text>
        <text class="feature-desc">30天健康报告</text>
      </view>
      <view class="feature-card" @tap="loadWeeklyReport">
        <text class="feature-icon">📅</text>
        <text class="feature-name">每周周报</text>
        <text class="feature-desc">本周饮食总结</text>
      </view>
      <view class="feature-card" @tap="loadBudgetAdvice">
        <text class="feature-icon">💰</text>
        <text class="feature-name">预算建议</text>
        <text class="feature-desc">AI 帮你定预算</text>
      </view>
    </view>

    <!-- 图片识别入口 -->
    <view class="photo-banner" @tap="pickImage">
      <text class="photo-banner-icon">📷</text>
      <view class="photo-banner-text">
        <text class="photo-banner-title">拍照识别菜品</text>
        <text class="photo-banner-desc">拍一张菜的照片，AI 自动帮你记录</text>
      </view>
      <text class="photo-banner-arrow">›</text>
    </view>

    <!-- 图片识别结果 -->
    <view v-if="photoResult" class="report-card fd-card">
      <view class="report-header">
        <text class="report-title">📷 识别结果</text>
        <text class="report-close" @tap="photoResult = ''">×</text>
      </view>
      <text class="report-content">{{ photoResult }}</text>
    </view>

    <!-- 每周周报展示 -->
    <view v-if="weeklyReport" class="report-card fd-card">
      <view class="report-header">
        <text class="report-title">📅 本周营养周报</text>
        <text class="report-close" @tap="weeklyReport = ''">×</text>
      </view>
      <text class="report-content">{{ weeklyReport }}</text>
    </view>

    <!-- 预算建议展示 -->
    <view v-if="budgetAdvice" class="report-card fd-card">
      <view class="report-header">
        <text class="report-title">💰 预算建议</text>
        <text class="report-close" @tap="budgetAdvice = ''">×</text>
      </view>
      <text class="report-content">{{ budgetAdvice }}</text>
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
      <view v-if="messages.length > 0" class="chat-clear" @tap="clearHistory">
        <text class="chat-clear-text">清除对话记录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { post, get, del } from '@/utils/http'
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
const weeklyReport = ref('')
const budgetAdvice = ref('')
const photoResult = ref('')
const scene = ref('lunch')

const scenes = [
  { key: 'breakfast', label: '早餐' },
  { key: 'lunch', label: '午餐' },
  { key: 'dinner', label: '晚餐' },
]

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
    await streamChat(text, token)
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '小饭暂时不在线，请稍后再试 😅' })
  } finally {
    streaming.value = false
    scrollToBottom()
  }
}

async function clearHistory() {
  uni.showModal({
    title: '清除对话',
    content: '确定清除所有对话记录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await del('/api/ai/chat/history')
        } catch {
          // 忽略网络错误，仅清除本地
        }
        messages.value = []
        streamingText.value = ''
      }
    },
  })
}

async function streamChat(message, token) {
  // #ifdef H5
  // H5 平台：原生 fetch + ReadableStream 实现 SSE 流式接收
  const baseUrl = (await import('@/config')).default.apiBaseUrl || ''
  const response = await fetch(baseUrl + '/api/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ message }),
  })

  if (!response.ok) throw new Error('请求失败')

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let fullText = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
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

  if (fullText) {
    messages.value.push({ role: 'assistant', content: fullText })
  }
  streamingText.value = ''
  // #endif

  // #ifndef H5
  // 小程序平台：SSE 不支持，降级为普通 POST 请求一次性获取回复
  const { post: httpPost } = await import('@/utils/http')
  const result = await httpPost('/api/ai/chat', { message })
  const reply = typeof result === 'string' ? result : (result?.content || '小饭暂时无法回复')
  streamingText.value = reply
  await new Promise(resolve => setTimeout(resolve, 30)) // 让 UI 刷新一帧
  messages.value.push({ role: 'assistant', content: reply })
  streamingText.value = ''
  // #endif
}

async function startRecommend() {
  loading.value = true
  recommendResult.value = ''
  try {
    const res = await post('/api/ai/recommend', { scene: scenes.find(s => s.key === scene.value)?.label || '午餐' })
    recommendResult.value = res
  } catch {
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
  } catch {
    uni.showToast({ title: '营养分析失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function loadWeeklyReport() {
  loading.value = true
  weeklyReport.value = ''
  try {
    const res = await get('/api/ai/weekly-report')
    weeklyReport.value = res
  } catch {
    uni.showToast({ title: '周报获取失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function loadBudgetAdvice() {
  loading.value = true
  budgetAdvice.value = ''
  try {
    const res = await get('/api/ai/budget-advice')
    budgetAdvice.value = res
  } catch {
    uni.showToast({ title: '预算建议获取失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function changeScene(key) {
  scene.value = key
  recommendResult.value = ''
  startRecommend()
}

// ── 图片识别 ──────────────────────────────────────────
function pickImage() {
  // #ifndef H5
  // 小程序：先检查相册/相机权限，再选图
  uni.getSetting({
    success(settingRes) {
      const albumAuth = settingRes.authSetting['scope.album']
      if (albumAuth === false) {
        uni.showModal({
          title: '需要相册权限',
          content: '请在设置中开启相册权限以选择图片',
          confirmText: '去设置',
          success(modalRes) {
            if (modalRes.confirm) uni.openSetting({})
          },
        })
        return
      }
      doChooseImage()
    },
    fail: doChooseImage,
  })
  // #endif
  // #ifdef H5
  doChooseImage()
  // #endif
}

function doChooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res) => {
      const filePath = res.tempFilePaths[0]
      uploadAndRecognize(filePath)
    },
  })
}

async function uploadAndRecognize(filePath) {
  loading.value = true
  photoResult.value = ''
  uni.showLoading({ title: '识别中...' })
  try {
    const token = getToken()
    // 先将图片转为 base64
    const base64 = await fileToBase64(filePath)
    const res = await post('/api/ai/recognize-food', { imageBase64: base64 })
    photoResult.value = res
  } catch {
    uni.showToast({ title: '识别失败，请重试', icon: 'none' })
  } finally {
    loading.value = false
    uni.hideLoading()
  }
}

function fileToBase64(filePath) {
  return new Promise((resolve, reject) => {
    // #ifdef H5
    fetch(filePath)
      .then(r => r.blob())
      .then(blob => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result.split(',')[1])
        reader.onerror = reject
        reader.readAsDataURL(blob)
      })
    // #endif
    // #ifndef H5
    const fs = uni.getFileSystemManager()
    fs.readFile({
      filePath,
      encoding: 'base64',
      success: (r) => resolve(r.data),
      fail: reject,
    })
    // #endif
  })
}

function scrollToBottom() {
  setTimeout(() => { scrollTop.value = 999999 }, 50)
}
</script>

<style lang="scss" scoped>
.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $fd-space-sm;
  padding: $fd-space-md $fd-space-md $fd-space-sm;
}
.feature-card {
  background: $fd-card-bg;
  border-radius: $fd-radius;
  box-shadow: $fd-shadow;
  padding: $fd-space-md $fd-space-sm;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  &:active { opacity: 0.85; transform: scale(0.97); }
}
.feature-icon { font-size: 56rpx; }
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

.photo-banner {
  margin: 0 $fd-space-md $fd-space-base;
  background: linear-gradient(135deg, rgba($fd-primary, 0.08), rgba($fd-accent, 0.1));
  border-radius: $fd-radius;
  padding: $fd-space-base $fd-space-md;
  display: flex;
  align-items: center;
  gap: $fd-space-base;
  border: 2rpx solid rgba($fd-primary, 0.15);
  &:active { opacity: 0.85; }
}
.photo-banner-icon { font-size: 56rpx; flex-shrink: 0; }
.photo-banner-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.photo-banner-title {
  font-size: $fd-font-base;
  font-weight: 700;
  color: $fd-text;
}
.photo-banner-desc {
  font-size: $fd-font-xs;
  color: $fd-text-secondary;
}
.photo-banner-arrow {
  font-size: 40rpx;
  color: $fd-text-light;
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
.chat-clear {
  text-align: center;
  padding-top: $fd-space-sm;
  &:active { opacity: 0.7; }
}
.chat-clear-text {
  font-size: $fd-font-xs;
  color: $fd-text-light;
}
</style>
