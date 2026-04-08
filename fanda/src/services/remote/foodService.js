import { get, post } from '@/utils/http'
export default {
  async getAll() { return get('/api/foods') },
  async getByCategory(category) { return get('/api/foods', { category }) },
  async search(keyword) { return get('/api/foods', { keyword }) },
  async addCustom(food) { return post('/api/foods', food) },
}
