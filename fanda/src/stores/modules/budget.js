import { defineStore } from 'pinia'
import { getService } from '@/services/factory'
import dayjs from 'dayjs'

export const useBudgetStore = defineStore('budget', {
  state: () => ({
    budget: { monthly: 2000, weekly: 500 },
    expenses: [],
    loaded: false,
  }),

  getters: {
    monthlyExpenses(state) {
      const prefix = dayjs().format('YYYY-MM')
      return state.expenses.filter((e) => e.date.startsWith(prefix))
    },
    monthlyTotal(state) {
      return this.monthlyExpenses.reduce((sum, e) => sum + e.amount, 0)
    },
    monthlyRemaining(state) {
      return Math.max(0, state.budget.monthly - this.monthlyTotal)
    },
    monthlyPercent(state) {
      if (state.budget.monthly <= 0) return 0
      return Math.min(100, Math.round((this.monthlyTotal / state.budget.monthly) * 100))
    },
    dailySuggestion(state) {
      const remaining = this.monthlyRemaining
      const daysLeft = dayjs().daysInMonth() - dayjs().date() + 1
      return Math.round(remaining / daysLeft)
    },
    isOverBudget() {
      return this.monthlyTotal > this.budget.monthly
    },
  },

  actions: {
    async load() {
      if (this.loaded) return
      const service = getService('budget')
      this.budget = await service.getBudget()
      this.expenses = await service.getExpenses()
      this.loaded = true
    },
    async setBudget(budget) {
      const service = getService('budget')
      await service.setBudget(budget)
      this.budget = budget
    },
    async addExpense(expense) {
      const service = getService('budget')
      await service.addExpense(expense)
      this.expenses.push(expense)
    },
  },
})
