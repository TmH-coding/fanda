export function formatMoney(value) {
  return Number(value).toFixed(2)
}

export function formatMoneyShort(value) {
  if (value >= 10000) return (value / 10000).toFixed(1) + '万'
  return String(value)
}

export function generateId(prefix = '') {
  return prefix + Date.now().toString(36) + Math.random().toString(36).slice(2, 6)
}
