import { defineStore } from 'pinia'
import { getService } from '@/services/factory'

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
      this.foods = await service.getAll()
      this.loaded = true
    },
  },
})
