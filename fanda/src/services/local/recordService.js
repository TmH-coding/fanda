import { storage } from '@/utils/storage'

const RECORD_KEY = 'records'

export default {
  async getAll() {
    return storage.get(RECORD_KEY, [])
  },

  async getByDate(date) {
    const all = await this.getAll()
    return all.filter((r) => r.date === date)
  },

  async getByMonth(year, month) {
    const prefix = `${year}-${String(month).padStart(2, '0')}`
    const all = await this.getAll()
    return all.filter((r) => r.date.startsWith(prefix))
  },

  async save(record) {
    const all = await this.getAll()
    const idx = all.findIndex((r) => r.id === record.id)
    if (idx >= 0) {
      all[idx] = record
    } else {
      all.push(record)
    }
    storage.set(RECORD_KEY, all)
  },

  async delete(id) {
    const all = await this.getAll()
    storage.set(RECORD_KEY, all.filter((r) => r.id !== id))
  },
}
