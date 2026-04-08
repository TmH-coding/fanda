import { storage } from '@/utils/storage'
import { mockGroups } from '@/data/mockSocial'

const GROUPS_KEY = 'social_groups'

export default {
  async getAll() {
    const saved = storage.get(GROUPS_KEY, null)
    if (saved === null) {
      storage.set(GROUPS_KEY, mockGroups)
      return [...mockGroups]
    }
    return saved
  },

  async getById(id) {
    const all = await this.getAll()
    return all.find((g) => g.id === id) || null
  },

  async create(group) {
    const all = await this.getAll()
    all.unshift(group)
    storage.set(GROUPS_KEY, all)
  },

  async update(group) {
    const all = await this.getAll()
    const idx = all.findIndex((g) => g.id === group.id)
    if (idx >= 0) {
      all[idx] = group
      storage.set(GROUPS_KEY, all)
    }
  },

  async vote(groupId, candidate) {
    const all = await this.getAll()
    const group = all.find((g) => g.id === groupId)
    if (group && group.votes[candidate] !== undefined) {
      group.votes[candidate]++
      storage.set(GROUPS_KEY, all)
    }
    return group
  },

  async join(groupId) {
    const all = await this.getAll()
    const group = all.find((g) => g.id === groupId)
    if (group && group.currentPeople < group.maxPeople) {
      group.currentPeople++
      storage.set(GROUPS_KEY, all)
    }
    return group
  },
}
