import { ref, computed } from 'vue'
import { useFoodStore } from '@/stores/modules/food'
import { usePreferenceStore } from '@/stores/modules/preference'
import { useRecordStore } from '@/stores/modules/record'
import { useBudgetStore } from '@/stores/modules/budget'
import { getCurrentMealType, today } from '@/utils/date'
import { generateId } from '@/utils/format'

export function useRecommend() {
  const foodStore = useFoodStore()
  const prefStore = usePreferenceStore()
  const recordStore = useRecordStore()
  const budgetStore = useBudgetStore()

  const excludedCategories = ref([])
  const currentFood = ref(null)
  const isSpinning = ref(false)
  const hasResult = ref(false)

  const mealType = computed(() => getCurrentMealType())

  // 获取过滤后的候选菜品
  const candidates = computed(() => {
    const prefs = prefStore.preference
    const recentIds = recordStore.recentFoodIds(3)
    const dailyBudget = budgetStore.dailySuggestion

    return foodStore.foods.filter((food) => {
      // 排除用户手动排除的分类
      if (excludedCategories.value.includes(food.category)) return false
      // 排除忌口/过敏
      if (prefs.allergies.some((a) => food.allergens.includes(a))) return false
      // 排除不喜欢的标签
      if (prefs.dislike.some((d) => food.tags.includes(d))) return false
      // 按用餐时段过滤
      if (!food.mealTime.includes(mealType.value)) return false
      // 排除最近吃过的
      if (recentIds.includes(food.id)) return false
      // 预算过滤（取价格区间中位数）
      if (dailyBudget > 0) {
        const avgPrice = (food.priceRange[0] + food.priceRange[1]) / 2
        if (avgPrice > dailyBudget * 1.5) return false
      }
      return true
    })
  })

  // 计算每道菜的推荐权重
  function computeWeights(pool) {
    const recent7Ids = new Set(recordStore.recentFoodIds(7))
    const recent14Ids = new Set(recordStore.recentFoodIds(14))
    const favoriteIds = new Set(prefStore.preference.favorites || [])

    return pool.map((food) => {
      let w = 1.0
      if (recent7Ids.has(food.id)) {
        w = 0.3  // 近7天吃过：降权
      } else if (recent14Ids.has(food.id)) {
        w = 0.7  // 近14天吃过：轻微降权
      }
      if (favoriteIds.has(food.id)) {
        w *= 1.2  // 收藏菜品：小幅加权
      }
      return w
    })
  }

  // 转盘项目列表（最多显示12个，按权重优先展示）
  const wheelItems = computed(() => {
    const pool = candidates.value
    if (pool.length <= 12) return pool
    // 按权重加权洗牌，高权重菜品更容易出现在转盘上
    const weights = computeWeights(pool)
    const indexed = pool.map((f, i) => ({ food: f, w: weights[i] }))
    const result = []
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

  // 加权随机推荐一个
  function getRandomFood() {
    const pool = candidates.value
    if (pool.length === 0) return null

    const weights = computeWeights(pool)
    const total = weights.reduce((s, w) => s + w, 0)
    let rand = Math.random() * total
    for (let i = 0; i < pool.length; i++) {
      rand -= weights[i]
      if (rand <= 0) return pool[i]
    }
    return pool[pool.length - 1]
  }

  // 开始旋转
  function spin() {
    if (isSpinning.value) return
    isSpinning.value = true
    hasResult.value = false
    currentFood.value = getRandomFood()
  }

  // 旋转结束回调
  function onSpinEnd() {
    isSpinning.value = false
    hasResult.value = true
  }

  // 排除分类
  function toggleExclude(category) {
    const idx = excludedCategories.value.indexOf(category)
    if (idx >= 0) {
      excludedCategories.value.splice(idx, 1)
    } else {
      excludedCategories.value.push(category)
    }
  }

  // 确认选择，记录到今日用餐
  async function confirmChoice(cost = 0) {
    if (!currentFood.value) return
    const food = currentFood.value
    const record = {
      id: generateId('r_'),
      date: today(),
      mealType: mealType.value,
      foodName: food.name,
      foodId: food.id,
      cost: cost || Math.round((food.priceRange[0] + food.priceRange[1]) / 2),
      nutrition: food.nutrition,
    }
    await recordStore.add(record)
    if (record.cost > 0) {
      await budgetStore.addExpense({
        id: generateId('e_'),
        date: today(),
        amount: record.cost,
        mealType: record.mealType,
        description: food.name,
      })
    }
    hasResult.value = false
    currentFood.value = null
  }

  // 换一个
  function reroll() {
    currentFood.value = getRandomFood()
  }

  // 初始化数据
  async function init() {
    await foodStore.load()
    prefStore.load()
    await recordStore.load()
    await budgetStore.load()
  }

  return {
    excludedCategories,
    currentFood,
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
