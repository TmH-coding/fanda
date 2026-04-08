<template>
  <view class="login-page">
    <!-- Status bar -->
    <view class="fd-status-bar"></view>

    <!-- Header -->
    <view class="login-header">
      <text class="login-logo">🍜</text>
      <text class="login-title">饭搭</text>
      <text class="login-tagline">打工人的饮食决策助手</text>
    </view>

    <!-- Tab switcher -->
    <view class="login-tabs">
      <view
        class="login-tab"
        :class="{ active: activeTab === 'login' }"
        @tap="activeTab = 'login'"
      >
        <text>登录</text>
      </view>
      <view
        class="login-tab"
        :class="{ active: activeTab === 'register' }"
        @tap="activeTab = 'register'"
      >
        <text>注册</text>
      </view>
    </view>

    <!-- Login form -->
    <view class="login-card" v-if="activeTab === 'login'">
      <view class="form-item">
        <text class="form-label">用户名</text>
        <input
          class="form-input"
          v-model="loginForm.username"
          placeholder="请输入用户名"
          placeholder-class="form-placeholder"
        />
      </view>
      <view class="form-item">
        <text class="form-label">密码</text>
        <input
          class="form-input"
          v-model="loginForm.password"
          type="password"
          placeholder="请输入密码"
          placeholder-class="form-placeholder"
        />
      </view>
      <button class="login-btn" :loading="loading" :disabled="loading" @tap="handleLogin">
        登录
      </button>
    </view>

    <!-- Register form -->
    <view class="login-card" v-if="activeTab === 'register'">
      <view class="form-item">
        <text class="form-label">用户名</text>
        <input
          class="form-input"
          v-model="registerForm.username"
          placeholder="请输入用户名"
          placeholder-class="form-placeholder"
        />
      </view>
      <view class="form-item">
        <text class="form-label">昵称</text>
        <input
          class="form-input"
          v-model="registerForm.nickname"
          placeholder="请输入昵称"
          placeholder-class="form-placeholder"
        />
      </view>
      <view class="form-item">
        <text class="form-label">密码</text>
        <input
          class="form-input"
          v-model="registerForm.password"
          type="password"
          placeholder="请输入密码（至少6位）"
          placeholder-class="form-placeholder"
        />
      </view>
      <view class="form-item">
        <text class="form-label">确认密码</text>
        <input
          class="form-input"
          v-model="registerForm.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          placeholder-class="form-placeholder"
        />
      </view>
      <button class="login-btn" :loading="loading" :disabled="loading" @tap="handleRegister">
        注册
      </button>
    </view>

    <!-- Footer tip -->
    <view class="login-footer">
      <text class="login-footer-text" v-if="activeTab === 'login'">
        还没有账号？<text class="link-text" @tap="activeTab = 'register'">立即注册</text>
      </text>
      <text class="login-footer-text" v-else>
        已有账号？<text class="link-text" @tap="activeTab = 'login'">去登录</text>
      </text>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/modules/auth'

const authStore = useAuthStore()

const activeTab = ref('login')
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

function validateLogin() {
  if (!loginForm.username.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return false
  }
  if (!loginForm.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return false
  }
  return true
}

function validateRegister() {
  if (!registerForm.username.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return false
  }
  if (!registerForm.nickname.trim()) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return false
  }
  if (!registerForm.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return false
  }
  if (registerForm.password.length < 6) {
    uni.showToast({ title: '密码至少6位', icon: 'none' })
    return false
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return false
  }
  return true
}

async function handleLogin() {
  if (!validateLogin()) return
  loading.value = true
  try {
    await authStore.login(loginForm.username.trim(), loginForm.password)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 800)
  } catch (e) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!validateRegister()) return
  loading.value = true
  try {
    await authStore.register(
      registerForm.username.trim(),
      registerForm.password,
      registerForm.nickname.trim()
    )
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 800)
  } catch (e) {
    uni.showToast({ title: e.message || '注册失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, $fd-bg 0%, #FFE8E8 100%);
  padding: 0 $fd-space-md;
}

.login-header {
  @include fd-flex-column;
  align-items: center;
  padding-top: 100rpx;
  padding-bottom: 60rpx;
}

.login-logo {
  font-size: 120rpx;
  margin-bottom: $fd-space-sm;
}

.login-title {
  font-size: $fd-font-xxl;
  font-weight: 700;
  color: $fd-primary;
  margin-bottom: $fd-space-xs;
}

.login-tagline {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
}

.login-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: $fd-space-lg;
  gap: $fd-space-lg;
}

.login-tab {
  padding: $fd-space-sm $fd-space-md;
  font-size: $fd-font-md;
  color: $fd-text-light;
  position: relative;
  transition: color 0.3s;

  &.active {
    color: $fd-primary;
    font-weight: 700;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 48rpx;
      height: 6rpx;
      background: $fd-primary;
      border-radius: 3rpx;
    }
  }
}

.login-card {
  @include fd-card;
  margin-bottom: $fd-space-md;
}

.form-item {
  margin-bottom: $fd-space-md;
}

.form-label {
  display: block;
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
  margin-bottom: $fd-space-xs;
  font-weight: 600;
}

.form-input {
  width: 100%;
  height: 88rpx;
  background: $fd-bg;
  border-radius: $fd-radius-sm;
  padding: 0 $fd-space-base;
  font-size: $fd-font-base;
  color: $fd-text;
  border: 2rpx solid transparent;
  transition: border-color 0.3s;

  &:focus {
    border-color: $fd-primary-light;
  }
}

.form-placeholder {
  color: $fd-text-light;
  font-size: $fd-font-sm;
}

.login-btn {
  @include fd-btn;
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  margin-top: $fd-space-md;
  font-size: $fd-font-md;
  letter-spacing: 4rpx;
}

.login-footer {
  text-align: center;
  padding: $fd-space-md 0;
}

.login-footer-text {
  font-size: $fd-font-sm;
  color: $fd-text-secondary;
}

.link-text {
  color: $fd-primary;
  font-weight: 600;
}
</style>
