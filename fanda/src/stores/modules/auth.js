import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { post } from '@/utils/http'
import { setToken, clearToken, getToken } from '@/utils/http'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref('')

  const isLoggedIn = computed(() => !!token.value)

  function loadFromStorage() {
    token.value = uni.getStorageSync('fd_access_token') || ''
    const savedUser = uni.getStorageSync('fd_user')
    if (savedUser) user.value = savedUser
  }

  async function login(username, password) {
    const data = await post('/api/auth/login', { username, password })
    token.value = data.token
    user.value = data.user
    setToken(data.token, data.refreshToken)
    uni.setStorageSync('fd_user', data.user)
    return data
  }

  async function register(username, password, nickname) {
    const data = await post('/api/auth/register', { username, password, nickname })
    token.value = data.token
    user.value = data.user
    setToken(data.token, data.refreshToken)
    uni.setStorageSync('fd_user', data.user)
    return data
  }

  function logout() {
    token.value = ''
    user.value = null
    clearToken()
    uni.removeStorageSync('fd_user')
    uni.reLaunch({ url: '/pages/login/login' })
  }

  return { user, token, isLoggedIn, loadFromStorage, login, register, logout }
})
