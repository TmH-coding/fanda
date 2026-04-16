import { defineStore } from 'pinia'
import { getService } from '@/services/factory'

export const useSocialStore = defineStore('social', {
  state: () => ({
    groups: [],
    loaded: false,
  }),

  getters: {
    openGroups(state) {
      return state.groups.filter((g) => g.status === 'open')
    },
    closedGroups(state) {
      return state.groups.filter((g) => g.status === 'closed')
    },
  },

  actions: {
    async load() {
      if (this.loaded) return
      const service = getService('social')
      this.groups = await service.getAll()
      this.loaded = true
    },
    async create(group) {
      const service = getService('social')
      await service.create(group)
      this.groups.unshift(group)
    },
    async vote(groupId, candidate) {
      const service = getService('social')
      const updated = await service.vote(groupId, candidate)
      const idx = this.groups.findIndex((g) => g.id === groupId)
      if (idx >= 0 && updated) this.groups[idx] = updated
    },
    async join(groupId) {
      const service = getService('social')
      const updated = await service.join(groupId)
      const idx = this.groups.findIndex((g) => g.id === groupId)
      if (idx >= 0 && updated) this.groups[idx] = updated
    },
  },
})
