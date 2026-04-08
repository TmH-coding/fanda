import { storage } from '@/utils/storage'
import { foods as defaultFoods } from '@/data/foods'

const FOODS_KEY = 'foods_custom'

export default {
  async getAll() {
    const custom = storage.get(FOODS_KEY, [])
    return [...defaultFoods, ...custom]
  },

  async getByCategory(category) {
    const all = await this.getAll()
    return all.filter((f) => f.category === category)
  },

  async search(keyword) {
    const all = await this.getAll()
    return all.filter((f) => f.name.includes(keyword))
  },

  async addCustom(food) {
    const custom = storage.get(FOODS_KEY, [])
    custom.push(food)
    storage.set(FOODS_KEY, custom)
  },
}
