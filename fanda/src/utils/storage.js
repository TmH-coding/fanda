/**
 * 本地存储封装 — 统一 uni.storage API
 */
const PREFIX = 'fd_'

export const storage = {
  get(key, defaultValue = null) {
    try {
      const raw = uni.getStorageSync(PREFIX + key)
      return raw !== '' && raw !== undefined ? raw : defaultValue
    } catch (e) {
      console.warn(`[storage] get ${key} failed:`, e)
      return defaultValue
    }
  },

  set(key, value) {
    try {
      uni.setStorageSync(PREFIX + key, value)
    } catch (e) {
      console.warn(`[storage] set ${key} failed:`, e)
    }
  },

  remove(key) {
    try {
      uni.removeStorageSync(PREFIX + key)
    } catch (e) {
      console.warn(`[storage] remove ${key} failed:`, e)
    }
  },

  clear() {
    try {
      uni.clearStorageSync()
    } catch (e) {
      console.warn(`[storage] clear failed:`, e)
    }
  },
}
