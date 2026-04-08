<script>
import config from '@/config'

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
  },
  onHide() {
    console.log('饭搭 App Hide')
  },
}
</script>

<style lang="scss">
@use './styles/common.scss';
</style>
