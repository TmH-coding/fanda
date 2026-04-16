/**
 * 拼饭 WebSocket 客户端工具
 * 连接: ws://host/ws/social?token=xxx&groupId=xxx
 *
 * 断线重连策略：指数退避，最大 5 次，间隔 2s/4s/8s/16s/30s
 */
import { getToken } from '@/utils/http'
import config from '@/config'

const MAX_RETRIES   = 5
const BASE_DELAY_MS = 2000

export function createSocialSocket(groupId, handlers = {}) {
  const baseUrl = (config.apiBaseUrl || 'http://localhost:8080')
    .replace(/^http/, 'ws')

  let socketTask     = null
  let reconnectTimer = null
  let closed         = false
  let retryCount     = 0
  let connected      = false

  function getUrl() {
    // token 可能在重连时已刷新，每次取最新
    const token = getToken()
    return `${baseUrl}/ws/social?token=${token}&groupId=${groupId}`
  }

  function connect() {
    if (closed) return

    socketTask = uni.connectSocket({
      url: getUrl(),
      complete: () => {}
    })

    socketTask.onOpen(() => {
      console.log('[WS] 已连接拼饭房间', groupId)
      connected  = true
      retryCount = 0          // 成功后重置计数
      handlers.onOpen?.()
    })

    socketTask.onMessage(({ data }) => {
      try {
        const msg = JSON.parse(data)
        handlers.onMessage?.(msg)
      } catch (e) {
        console.warn('[WS] 消息解析失败', e)
      }
    })

    socketTask.onError((err) => {
      console.warn('[WS] 连接错误', err)
      connected = false
      handlers.onError?.(err)
      scheduleReconnect()
    })

    socketTask.onClose(() => {
      console.log('[WS] 连接关闭')
      connected = false
      handlers.onClose?.()
      scheduleReconnect()
    })
  }

  function scheduleReconnect() {
    if (closed) return
    if (retryCount >= MAX_RETRIES) {
      console.warn('[WS] 已达最大重连次数，停止重连')
      handlers.onError?.({ message: '连接失败，请刷新页面重试' })
      return
    }
    clearTimeout(reconnectTimer)
    // 指数退避：2s, 4s, 8s, 16s, 30s(上限)
    const delay = Math.min(BASE_DELAY_MS * Math.pow(2, retryCount), 30000)
    retryCount++
    console.log(`[WS] 第 ${retryCount} 次重连，${delay / 1000}s 后发起`)
    reconnectTimer = setTimeout(() => {
      connect()
    }, delay)
  }

  function sendChat(content) {
    if (!socketTask || !connected) return
    socketTask.send({ data: JSON.stringify({ type: 'CHAT', content }) })
  }

  function close() {
    closed = true
    connected = false
    clearTimeout(reconnectTimer)
    socketTask?.close?.({})
  }

  connect()
  return { sendChat, close }
}
