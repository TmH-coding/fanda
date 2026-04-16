import { defineStore } from 'pinia'
import { getService } from '@/services/factory'
import { today } from '@/utils/date'
import { enqueue } from '@/utils/offlineQueue'

export const useRecordStore = defineStore('record', {
  state: () => ({
    records: [],
    loaded: false,
  }),

  getters: {
    getByDate(state) {
      return (date) => state.records.filter((r) => r.date === date)
    },
    todayRecords(state) {
      const d = today()
      return state.records.filter((r) => r.date === d)
    },
    recentFoodIds(state) {
      return (days = 3) => {
        const now = new Date()
        const cutoff = new Date(now.getTime() - days * 86400000)
          .toISOString()
          .slice(0, 10)
        return state.records
          .filter((r) => r.date >= cutoff)
          .map((r) => r.foodId)
      }
    },
    totalRecords(state) {
      return state.records.length
    },
    uniqueFoodCount(state) {
      return new Set(state.records.map((r) => r.foodId).filter(Boolean)).size
    },
    streak(state) {
      if (state.records.length === 0) return 0
      const dates = [...new Set(state.records.map((r) => r.date))].sort().reverse()
      let count = 0
      const d = new Date(today())
      for (let i = 0; i < dates.length; i++) {
        const expected = new Date(d.getTime() - i * 86400000)
          .toISOString()
          .slice(0, 10)
        if (dates[i] === expected) {
          count++
        } else {
          break
        }
      }
      return count
    },
    breakfastCount(state) {
      return state.records.filter((r) => r.mealType === 'breakfast').length
    },
  },

  actions: {
    async load() {
      if (this.loaded) return
      const service = getService('record')
      this.records = await service.getAll()
      this.loaded = true
    },
    async add(record) {
      const service = getService('record')
      try {
        await service.save(record)
      } catch (e) {
        // 仅在网络不可用时入队，服务端错误不入队
        if (e?.isOffline) enqueue('add', 'record', record)
      }
      this.records.push(record)
    },
    async remove(id) {
      const service = getService('record')
      try {
        await service.delete(id)
      } catch (e) {
        if (e?.isOffline) enqueue('remove', 'record', id)
      }
      this.records = this.records.filter((r) => r.id !== id)
    },
  },
})
