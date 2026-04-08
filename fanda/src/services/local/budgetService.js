import { storage } from '@/utils/storage'

const BUDGET_KEY = 'budget'
const EXPENSE_KEY = 'expenses'

export default {
  async getBudget() {
    return storage.get(BUDGET_KEY, { monthly: 0, weekly: 0 })
  },

  async setBudget(budget) {
    storage.set(BUDGET_KEY, budget)
  },

  async getExpenses() {
    return storage.get(EXPENSE_KEY, [])
  },

  async getExpensesByMonth(year, month) {
    const all = await this.getExpenses()
    const prefix = `${year}-${String(month).padStart(2, '0')}`
    return all.filter((e) => e.date.startsWith(prefix))
  },

  async addExpense(expense) {
    const all = await this.getExpenses()
    all.push(expense)
    storage.set(EXPENSE_KEY, all)
  },

  async deleteExpense(id) {
    const all = await this.getExpenses()
    storage.set(EXPENSE_KEY, all.filter((e) => e.id !== id))
  },
}
