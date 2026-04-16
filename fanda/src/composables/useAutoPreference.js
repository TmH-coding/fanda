/**
 * 偏好自学习
 * 根据近30天用餐记录，自动推断用户偏好的口味标签和菜系
 * 在 preference store 中存储自动推断的标签，用于推荐加权
 */
import { useRecordStore } from '@/stores/modules/record'
import { useFoodStore } from '@/stores/modules/food'
import { usePreferenceStore } from '@/stores/modules/preference'

/**
 * 分析近 N 天记录，返回自动推断结果
 * @returns {{ autoTags: string[], autoCats: string[], insight: string }}
 */
export function analyzePreference(days = 30) {
  const recordStore = useRecordStore()
  const foodStore   = useFoodStore()
  const prefStore   = usePreferenceStore()

  const now    = Date.now()
  const cutoff = new Date(now - days * 86400000).toISOString().slice(0, 10)

  const recentRecords = recordStore.records.filter(r => r.date >= cutoff)
  if (recentRecords.length < 3) {
    return { autoTags: [], autoCats: [], insight: '' }
  }

  // 统计每个食物出现频次
  const foodCount  = {}
  const tagCount   = {}
  const catCount   = {}

  for (const rec of recentRecords) {
    if (!rec.foodId) continue
    foodCount[rec.foodId] = (foodCount[rec.foodId] || 0) + 1

    const food = foodStore.foods.find(f => f.id === rec.foodId)
    if (!food) continue

    // 统计口味标签
    for (const tag of (food.tags || [])) {
      tagCount[tag] = (tagCount[tag] || 0) + 1
    }
    // 统计菜系分类
    catCount[food.category] = (catCount[food.category] || 0) + 1
  }

  // 取出现超过3次的标签，最多3个
  const autoTags = Object.entries(tagCount)
    .filter(([, c]) => c >= 3)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([tag]) => tag)

  // 取出现次数最多的2个菜系
  const autoCats = Object.entries(catCount)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 2)
    .map(([cat]) => cat)

  // 黑名单候选：用户偏好中标记 dislike 却仍频繁出现的标签（可能是系统错误，忽略）
  // 检测是否长期避开某分类（从未出现但菜品库中有大量选择）
  const allCats = [...new Set(foodStore.foods.map(f => f.category))]
  const avoidedCats = allCats.filter(
    cat => !catCount[cat] && foodStore.foods.filter(f => f.category === cat).length >= 5
  )

  // 生成洞察文字
  let insight = ''
  if (autoTags.length > 0) {
    insight += `最近偏爱：${autoTags.join('、')}。`
  }
  if (autoCats.length > 0) {
    insight += `常吃菜系：${autoCats.join('、')}。`
  }
  if (avoidedCats.length > 0 && avoidedCats.length <= 3) {
    insight += `你好像不太吃${avoidedCats.join('、')}，是不喜欢吗？`
  }

  return { autoTags, autoCats, avoidedCats, insight }
}

/**
 * 将自学习结果合并写入 preferenceStore（不覆盖用户手动设置）
 */
export async function applyAutoPreference() {
  const prefStore = usePreferenceStore()
  const { autoTags, autoCats, insight } = analyzePreference(30)

  await prefStore.update({
    autoLearnedTags: autoTags,
    autoLearnedCats: autoCats,
  })

  return insight
}
