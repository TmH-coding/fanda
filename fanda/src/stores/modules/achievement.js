import { defineStore } from 'pinia'
import { storage } from '@/utils/storage'
import { achievements } from '@/data/achievements'
import config from '@/config'
import { get, post } from '@/utils/http'

const ACHIEVEMENT_KEY = 'achievements_unlocked'

export const useAchievementStore = defineStore('achievement', {
  state: () => ({
    unlocked: [],
    loaded: false,
  }),

  getters: {
    allAchievements() {
      return achievements
    },
    unlockedSet(state) {
      return new Set(state.unlocked)
    },
    isUnlocked(state) {
      return (id) => state.unlocked.includes(id)
    },
    progress(state) {
      return `${state.unlocked.length}/${achievements.length}`
    },
  },

  actions: {
    async load() {
      if (this.loaded) return
      if (config.dataMode === 'remote') {
        try {
          const data = await get('/api/achievements')
          this.unlocked = data.unlocked || []
        } catch (e) {
          console.error('加载成就失败', e)
        }
      } else {
        this.unlocked = storage.get(ACHIEVEMENT_KEY, [])
      }
      this.loaded = true
    },
    async check(stats) {
      if (config.dataMode === 'remote') {
        try {
          const data = await post('/api/achievements/check', stats)
          const newlyUnlocked = data || []
          for (const ach of newlyUnlocked) {
            if (!this.unlocked.includes(ach.id)) {
              this.unlocked.push(ach.id)
            }
          }
          return newlyUnlocked
        } catch (e) {
          console.error('检查成就失败', e)
          return []
        }
      } else {
        const newlyUnlocked = []
        for (const ach of achievements) {
          if (!this.unlocked.includes(ach.id) && ach.condition(stats)) {
            this.unlocked.push(ach.id)
            newlyUnlocked.push(ach)
          }
        }
        if (newlyUnlocked.length > 0) {
          storage.set(ACHIEVEMENT_KEY, this.unlocked)
        }
        return newlyUnlocked
      }
    },
  },
})
