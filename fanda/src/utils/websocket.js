/**
 * 拼饭 WebSocket 客户端工具
 * 连接: ws://host/ws/social?token=xxx&groupId=xxx
 */
import { getToken } from '@/utils/http'
import config from '@/config'

export function createSocialSocket(groupId, handlers = {}) {
  const baseUrl = (config.apiBaseUrl || 'http://localhost:8080')
    .replace(/^http/, 'ws')
  const token = getToken()
  const url = `${baseUrl}/ws/social?token=${token}&groupId=${groupId}`

  let socketTask = null
  let reconnectTimer = null
  let closed = false

  function connect() {
    socketTask = uni.connectSocket({
      url,
      complete: () => {}
    })

    socketTask.onOpen(() => {
      console.log('[WS] 已连接拼饭房间', groupId)
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
      handlers.onError?.(err)
      scheduleReconnect()
    })

    socketTask.onClose(() => {
      console.log('[WS] 连接关闭')
      handlers.onClose?.()
      if (!closed) scheduleReconnect()
    })
  }

  function scheduleReconnect() {
    if (closed) return
    clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      console.log('[WS] 重连中...')
      connect()
    }, 3000)
  }

  function sendChat(content) {
    if (!socketTask) return
    socketTask.send({ data: JSON.stringify({ type: 'CHAT', content }) })
  }

  function close() {
    closed = true
    clearTimeout(reconnectTimer)
    socketTask?.close?.({})
  }

  connect()
  return { sendChat, close }
}
