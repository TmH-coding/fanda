/**
 * 离线同步状态 composable
 * 提供响应式的队列长度，供 UI 展示同步角标
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { queueSize } from '@/utils/offlineQueue'

const pendingCount = ref(0)

function refresh() {
  pendingCount.value = queueSize()
}

export function useOfflineSync() {
  let timer = null

  onMounted(() => {
    refresh()
    // 每 5 秒轮询一次队列长度（轻量，仅读 localStorage）
    timer = setInterval(refresh, 5000)
  })

  onUnmounted(() => {
    if (timer) clearInterval(timer)
  })

  return { pendingCount, refresh }
}
