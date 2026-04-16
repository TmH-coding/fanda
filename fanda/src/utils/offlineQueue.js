/**
 * 离线操作队列
 * 网络不可用时将写操作暂存到 localStorage，联网后自动回放
 *
 * 队列项结构：
 * { id, type: 'add'|'remove', resource: 'record'|'budget', payload, createdAt }
 */
import { storage } from '@/utils/storage'

const QUEUE_KEY = 'offline_queue'

function loadQueue() {
  return storage.get(QUEUE_KEY, [])
}

function saveQueue(queue) {
  storage.set(QUEUE_KEY, queue)
}

/** 入队一条操作 */
export function enqueue(type, resource, payload) {
  const queue = loadQueue()
  queue.push({
    id: Date.now() + '_' + Math.random().toString(36).slice(2, 7),
    type,
    resource,
    payload,
    createdAt: new Date().toISOString(),
  })
  saveQueue(queue)
  console.log(`[OfflineQueue] 入队 ${type}/${resource}`, payload)
}

/** 取出全部待处理项（不清空，由 flush 负责清除） */
export function peek() {
  return loadQueue()
}

/** 移除已成功回放的单条操作 */
export function dequeue(id) {
  const queue = loadQueue().filter((item) => item.id !== id)
  saveQueue(queue)
}

/** 清空队列 */
export function clearQueue() {
  storage.remove(QUEUE_KEY)
}

/** 队列长度 */
export function queueSize() {
  return loadQueue().length
}

/**
 * 回放队列
 * @param {Object} handlers - { record: { add, remove }, budget: { addExpense, removeExpense } }
 * @returns {Promise<number>} 成功回放的条数
 */
export async function flush(handlers) {
  const queue = loadQueue()
  if (queue.length === 0) return 0

  let successCount = 0
  for (const item of queue) {
    try {
      const handler = handlers[item.resource]
      if (!handler) {
        console.warn('[OfflineQueue] 未知 resource:', item.resource)
        dequeue(item.id)
        continue
      }

      if (item.type === 'add' && handler.add) {
        await handler.add(item.payload)
      } else if (item.type === 'remove' && handler.remove) {
        await handler.remove(item.payload)
      } else {
        console.warn('[OfflineQueue] 未知 type:', item.type)
      }

      dequeue(item.id)
      successCount++
    } catch (e) {
      // 回放失败保留在队列，等下次重试
      console.warn('[OfflineQueue] 回放失败，保留队列项', item.id, e)
    }
  }

  console.log(`[OfflineQueue] 回放完成，成功 ${successCount}/${queue.length} 条`)
  return successCount
}
