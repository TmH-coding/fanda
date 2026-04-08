import dayjs from 'dayjs'

export function today() {
  return dayjs().format('YYYY-MM-DD')
}

export function formatDate(date, fmt = 'YYYY-MM-DD') {
  return dayjs(date).format(fmt)
}

export function isSameDay(a, b) {
  return dayjs(a).isSame(dayjs(b), 'day')
}

export function getDaysInMonth(year, month) {
  return dayjs(`${year}-${String(month).padStart(2, '0')}`).daysInMonth()
}

export function getFirstDayOfWeek(year, month) {
  return dayjs(`${year}-${String(month).padStart(2, '0')}-01`).day()
}

export function getCurrentMealType() {
  const hour = dayjs().hour()
  if (hour < 10) return 'breakfast'
  if (hour < 15) return 'lunch'
  return 'dinner'
}

export function mealTypeLabel(type) {
  const map = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐' }
  return map[type] || type
}

export function getMonthRange(year, month) {
  const start = `${year}-${String(month).padStart(2, '0')}-01`
  const end = dayjs(start).endOf('month').format('YYYY-MM-DD')
  return { start, end }
}
