import { defineStore } from 'pinia'
import { storage } from '@/utils/storage'
import config from '@/config'
import { get, put, post } from '@/utils/http'

const PREF_KEY = 'preference'

const defaultPreference = {
  spicyLevel: 2,
  favCategories: [],
  allergies: [],
  dislike: [],
  favorites: [],
}

export const usePreferenceStore = defineStore('preference', {
  state: () => ({
    preference: { ...defaultPreference },
    loaded: false,
  }),

  getters: {
    favoriteIds(state) {
      return state.preference.favorites || []
    },
    favoriteCount(state) {
      return (state.preference.favorites || []).length
    },
  },

  actions: {
    async load() {
      if (this.loaded) return
      if (config.dataMode === 'remote') {
        try {
          const data = await get('/api/preferences')
          this.preference = { ...defaultPreference, ...data }
        } catch (e) {
          console.error('加载偏好失败', e)
        }
      } else {
        const saved = storage.get(PREF_KEY, null)
        if (saved) {
          this.preference = { ...defaultPreference, ...saved }
        }
      }
      this.loaded = true
    },
    async save() {
      if (config.dataMode === 'remote') {
        try {
          await put('/api/preferences', {
            spicyLevel: this.preference.spicyLevel,
            favCategories: this.preference.favCategories,
            allergies: this.preference.allergies,
            dislike: this.preference.dislike,
          })
        } catch (e) {
          console.error('保存偏好失败', e)
        }
      } else {
        storage.set(PREF_KEY, this.preference)
      }
    },
    async update(partial) {
      Object.assign(this.preference, partial)
      await this.save()
    },
    async toggleFavorite(foodId) {
      if (config.dataMode === 'remote') {
        try {
          const data = await post(`/api/preferences/favorite/${foodId}`)
          const favs = this.preference.favorites || []
          if (data.favorited) {
            favs.push(foodId)
          } else {
            const idx = favs.indexOf(foodId)
            if (idx >= 0) favs.splice(idx, 1)
          }
          this.preference.favorites = favs
        } catch (e) {
          console.error('切换收藏失败', e)
        }
      } else {
        const favs = this.preference.favorites || []
        const idx = favs.indexOf(foodId)
        if (idx >= 0) {
          favs.splice(idx, 1)
        } else {
          favs.push(foodId)
        }
        this.preference.favorites = favs
        this.save()
      }
    },
    isFavorite(foodId) {
      return (this.preference.favorites || []).includes(foodId)
    },
  },
})
