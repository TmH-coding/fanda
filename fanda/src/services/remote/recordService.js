import { get, post, del } from '@/utils/http'
export default {
  async getAll() { return get('/api/records') },
  async getByDate(date) { return get('/api/records', { date }) },
  async getByMonth(year, month) { return get('/api/records', { year, month }) },
  async save(record) { return post('/api/records', record) },
  async delete(id) { return del('/api/records/' + id) },
}
