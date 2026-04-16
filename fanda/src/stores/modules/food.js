import { defineStore } from 'pinia'
import { getService } from '@/services/factory'
import { storage } from '@/utils/storage'

const FOOD_CACHE_KEY = 'food_cache'
const FOOD_CACHE_TS_KEY = 'food_cache_ts'
const CACHE_TTL_MS = 24 * 60 * 60 * 1000  // 菜品数据本地缓存 24 小时

export const useFoodStore = defineStore('food', {
  state: () => ({
    foods: [],
    loaded: false,
  }),

  getters: {
    categories(state) {
      const cats = [...new Set(state.foods.map((f) => f.category))]
      return cats
    },
    getByCategory(state) {
      return (category) => state.foods.filter((f) => f.category === category)
    },
    getByMealTime(state) {
      return (mealTime) => state.foods.filter((f) => f.mealTime.includes(mealTime))
    },
  },

  actions: {
    async load() {
      if (this.loaded) return
      const service = getService('food')
      try {
        const foods = await service.getAll()
        this.foods = foods
        this.loaded = true
        // 拉取成功后写入本地缓存（供离线兜底）
        storage.set(FOOD_CACHE_KEY, foods)
        storage.set(FOOD_CACHE_TS_KEY, Date.now())
      } catch (e) {
        // 网络不可用时，尝试从本地缓存恢复
        const cached = storage.get(FOOD_CACHE_KEY, null)
        if (cached && cached.length > 0) {
          this.foods = cached
          this.loaded = true
          const ts = storage.get(FOOD_CACHE_TS_KEY, 0)
          const age = Math.round((Date.now() - ts) / 3600000)
          console.warn(`[FoodStore] 使用本地缓存菜品数据（${age}h 前）`)
          uni.showToast({ title: '已加载缓存菜品，部分数据可能不是最新', icon: 'none', duration: 2500 })
        } else {
          // 既无网络又无缓存，保持 loaded=false 让 UI 显示空态
          console.error('[FoodStore] 无缓存，菜品加载失败', e)
          uni.showToast({ title: '菜品加载失败，请检查网络', icon: 'none' })
        }
      }
    },

    /** 主动强制刷新（忽略 loaded 标志） */
    async refresh() {
      this.loaded = false
      await this.load()
    },
  },
})

