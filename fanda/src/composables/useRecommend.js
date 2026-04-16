import { ref, computed } from 'vue'
import { useFoodStore } from '@/stores/modules/food'
import { usePreferenceStore } from '@/stores/modules/preference'
import { useRecordStore } from '@/stores/modules/record'
import { useBudgetStore } from '@/stores/modules/budget'
import { getCurrentMealType, today } from '@/utils/date'
import { generateId } from '@/utils/format'

export function useRecommend() {
  const foodStore   = useFoodStore()
  const prefStore   = usePreferenceStore()
  const recordStore = useRecordStore()
  const budgetStore = useBudgetStore()

  const excludedCategories = ref([])
  const currentFood        = ref(null)
  const recommendReason    = ref('')
  const isSpinning         = ref(false)
  const hasResult          = ref(false)

  const mealType = computed(() => getCurrentMealType())

  // 过滤后的候选菜品
  const candidates = computed(() => {
    const prefs       = prefStore.preference
    const recentIds   = recordStore.recentFoodIds(3)
    const dailyBudget = budgetStore.dailySuggestion
    const blacklist   = new Set(prefs.blacklist || [])

    return foodStore.foods.filter((food) => {
      if (excludedCategories.value.includes(food.category)) return false
      if (prefs.allergies.some((a) => food.allergens.includes(a))) return false
      if (prefs.dislike.some((d) => food.tags.includes(d))) return false
      if (!food.mealTime.includes(mealType.value)) return false
      if (recentIds.includes(food.id)) return false
      if (blacklist.has(food.id)) return false
      if (dailyBudget > 0) {
        const avgPrice = (food.priceRange[0] + food.priceRange[1]) / 2
        if (avgPrice > dailyBudget * 1.5) return false
      }
      return true
    })
  })

  // 计算推荐权重（含自学习加权）
  function computeWeights(pool) {
    const recent7Ids  = new Set(recordStore.recentFoodIds(7))
    const recent14Ids = new Set(recordStore.recentFoodIds(14))
    const favoriteIds = new Set(prefStore.preference.favorites || [])
    const autoTags    = new Set(prefStore.preference.autoLearnedTags || [])
    const autoCats    = new Set(prefStore.preference.autoLearnedCats || [])

    return pool.map((food) => {
      let w = 1.0
      if (recent7Ids.has(food.id))       w = 0.3
      else if (recent14Ids.has(food.id)) w = 0.7
      if (favoriteIds.has(food.id))      w *= 1.2
      // 自学习口味标签加权
      if ((food.tags || []).some(t => autoTags.has(t))) w *= 1.15
      // 自学习菜系加权
      if (autoCats.has(food.category)) w *= 1.10
      return w
    })
  }

  // 转盘项目（最多12个，按权重优先展示）
  const wheelItems = computed(() => {
    const pool = candidates.value
    if (pool.length <= 12) return pool
    const weights  = computeWeights(pool)
    const indexed  = pool.map((f, i) => ({ food: f, w: weights[i] }))
    const result   = []
    const remaining = [...indexed]
    while (result.length < 12 && remaining.length > 0) {
      const total = remaining.reduce((s, x) => s + x.w, 0)
      let rand = Math.random() * total
      for (let i = 0; i < remaining.length; i++) {
        rand -= remaining[i].w
        if (rand <= 0) {
          result.push(remaining[i].food)
          remaining.splice(i, 1)
          break
        }
      }
    }
    return result
  })

  // 生成推荐理由
  function buildReason(food) {
    const favoriteIds = new Set(prefStore.preference.favorites || [])
    const autoTags    = new Set(prefStore.preference.autoLearnedTags || [])
    const foodRecords = recordStore.records.filter(r => r.foodId === food.id)

    if (favoriteIds.has(food.id)) return `这是你的收藏菜品，今天再来一次 ❤️`

    const matchedTag = (food.tags || []).find(t => autoTags.has(t))
    if (matchedTag) return `你最近偏爱"${matchedTag}"口味，${food.name}正合适 ✨`

    if (foodRecords.length === 0) return `你还没吃过${food.name}，今天尝个鲜 🆕`

    const lastDate = foodRecords.map(r => r.date).sort().reverse()[0]
    const days = Math.floor((Date.now() - new Date(lastDate).getTime()) / 86400000)

    if (days >= 14) return `上次吃${food.name}是 ${days} 天前，久违了 😋`
    if (days >= 7)  return `距上次吃${food.name}已过去 ${days} 天，正好换换口味 🎯`
    return `${food.name}符合你的口味偏好，今天就它了 👍`
  }

  // 加权随机选一道菜
  function getRandomFood() {
    const pool = candidates.value
    if (pool.length === 0) return null
    const weights = computeWeights(pool)
    const total   = weights.reduce((s, w) => s + w, 0)
    let rand = Math.random() * total
    for (let i = 0; i < pool.length; i++) {
      rand -= weights[i]
      if (rand <= 0) {
        recommendReason.value = buildReason(pool[i])
        return pool[i]
      }
    }
    const food = pool[pool.length - 1]
    recommendReason.value = buildReason(food)
    return food
  }

  function spin() {
    if (isSpinning.value) return
    isSpinning.value = true
    hasResult.value  = false
    currentFood.value = getRandomFood()
  }

  function onSpinEnd() {
    isSpinning.value = false
    hasResult.value  = true
  }

  function toggleExclude(category) {
    const idx = excludedCategories.value.indexOf(category)
    if (idx >= 0) excludedCategories.value.splice(idx, 1)
    else          excludedCategories.value.push(category)
  }

  async function confirmChoice(cost = 0) {
    if (!currentFood.value) return
    const food   = currentFood.value
    const record = {
      id:        generateId('r_'),
      date:      today(),
      mealType:  mealType.value,
      foodName:  food.name,
      foodId:    food.id,
      cost:      cost || Math.round((food.priceRange[0] + food.priceRange[1]) / 2),
      nutrition: food.nutrition,
    }
    await recordStore.add(record)
    if (record.cost > 0) {
      await budgetStore.addExpense({
        id:          generateId('e_'),
        date:        today(),
        amount:      record.cost,
        mealType:    record.mealType,
        description: food.name,
      })
    }
    hasResult.value   = false
    currentFood.value = null
  }

  function reroll() {
    currentFood.value = getRandomFood()
  }

  async function init() {
    await foodStore.load()
    prefStore.load()
    await recordStore.load()
    await budgetStore.load()
  }

  return {
    excludedCategories,
    currentFood,
    recommendReason,
    isSpinning,
    hasResult,
    mealType,
    candidates,
    wheelItems,
    spin,
    onSpinEnd,
    toggleExclude,
    confirmChoice,
    reroll,
    init,
  }
}
