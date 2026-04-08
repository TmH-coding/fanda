import { get, put, post, del } from '@/utils/http'
export default {
  async getBudget() { return get('/api/budget') },
  async setBudget(budget) { return put('/api/budget', budget) },
  async getExpenses() { return get('/api/expenses') },
  async getExpensesByMonth(year, month) { return get('/api/expenses', { year, month }) },
  async addExpense(expense) { return post('/api/expenses', expense) },
  async deleteExpense(id) { return del('/api/expenses/' + id) },
}
