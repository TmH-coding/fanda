<script>
import config from '@/config'
import { flush, queueSize } from '@/utils/offlineQueue'
import { getService } from '@/services/factory'

async function flushOfflineQueue() {
  if (queueSize() === 0) return
  try {
    const recordService = getService('record')
    const count = await flush({
      record: {
        add:    (payload) => recordService.save(payload),
        remove: (payload) => recordService.delete(payload),
      },
    })
    if (count > 0) {
      uni.showToast({ title: `已同步 ${count} 条离线记录`, icon: 'success' })
    }
  } catch (e) {
    console.warn('[App] 离线队列回放失败', e)
  }
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
    }
  },
  onShow() {
    console.log('饭搭 App Show')
    // 每次前台时尝试回放离线队列（仅远程模式有意义）
    if (config.dataMode === 'remote') {
      flushOfflineQueue()
    }
  },
  onHide() {
    console.log('饭搭 App Hide')
  },
}
</script>

<style lang="scss">
@use './styles/common.scss';
</style>
