<script>
import config from '@/config'
import { flush, queueSize } from '@/utils/offlineQueue'
import { getService } from '@/services/factory'

async function flushOfflineQueue() {
  if (queueSize() === 0) return
  try {
    const recordService = getService('record')
    const budgetService = getService('budget')
    const count = await flush({
      record: {
        add:    (payload) => recordService.save(payload),
        remove: (payload) => recordService.delete(payload),
      },
      budget: {
        add:    (payload) => budgetService.addExpense(payload),
        remove: (payload) => budgetService.deleteExpense(payload),
      },
    })
    if (count > 0) {
      uni.showToast({ title: `已同步 ${count} 条离线记录`, icon: 'success' })
    }
  } catch (e) {
    console.warn('[App] 离线队列回放失败', e)
  }
}

function checkMonthlyReportReminder() {
  const now = new Date()
  const day = now.getDate()
  // 只在每月 1-3 日提示
  if (day > 3) return

  const key = `fd_monthly_reminder_${now.getFullYear()}_${now.getMonth() + 1}`
  const reminded = uni.getStorageSync(key)
  if (reminded) return

  // 计算上个月
  const prevMonth = now.getMonth() === 0 ? 12 : now.getMonth()
  const prevYear  = now.getMonth() === 0 ? now.getFullYear() - 1 : now.getFullYear()

  uni.showModal({
    title: '📊 查看上月饮食报告',
    content: `${prevYear}年${prevMonth}月的饮食数据已汇总，去看看你上个月吃了什么？`,
    confirmText: '去看看',
    cancelText: '下次再说',
    success: (res) => {
      if (res.confirm) {
        uni.navigateTo({ url: '/pages/stats/stats' })
      }
    },
  })

  uni.setStorageSync(key, '1')
}

export default {
  onLaunch() {
    console.log('饭搭 App Launch')
    // 远程模式下检查登录状态
    if (config.dataMode === 'remote') {
      const token = uni.getStorageSync('fd_access_token')
      if (!token) {
        uni.reLaunch({ url: '/pages/login/login' })
      }
      // 网络恢复时自动回放离线队列
      uni.onNetworkStatusChange((res) => {
        if (res.isConnected) {
          console.log('[App] 网络已恢复，尝试同步离线队列')
          flushOfflineQueue()
        }
      })
    }
  },
  onShow() {
    console.log('饭搭 App Show')
    // 每次前台时尝试回放离线队列（仅远程模式有意义）
    if (config.dataMode === 'remote') {
      flushOfflineQueue()
    }
    // 月初提醒查看上月报告（每月只提示一次）
    checkMonthlyReportReminder()
  },
  onHide() {
    console.log('饭搭 App Hide')
  },
}
</script>

<style lang="scss">
@use './styles/common.scss';
</style>
