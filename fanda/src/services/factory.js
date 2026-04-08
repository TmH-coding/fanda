import config from '@/config'

import localFoodService from './local/foodService.js'
import localRecordService from './local/recordService.js'
import localBudgetService from './local/budgetService.js'
import localSocialService from './local/socialService.js'

import remoteFoodService from './remote/foodService.js'
import remoteRecordService from './remote/recordService.js'
import remoteBudgetService from './remote/budgetService.js'
import remoteSocialService from './remote/socialService.js'

const localServices = {
  food: localFoodService,
  record: localRecordService,
  budget: localBudgetService,
  social: localSocialService
}

const remoteServices = {
  food: remoteFoodService,
  record: remoteRecordService,
  budget: remoteBudgetService,
  social: remoteSocialService
}

const serviceCache = {}

export function getService(name) {
  if (!serviceCache[name]) {
    serviceCache[name] = config.dataMode === 'remote'
      ? remoteServices[name]
      : localServices[name]
  }
  return serviceCache[name]
}
