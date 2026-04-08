import config from '@/config'

const TOKEN_KEY = 'fd_access_token'
const REFRESH_KEY = 'fd_refresh_token'

function getToken() {
  return uni.getStorageSync(TOKEN_KEY)
}

function setToken(token, refreshToken) {
  uni.setStorageSync(TOKEN_KEY, token)
  if (refreshToken) uni.setStorageSync(REFRESH_KEY, refreshToken)
}

function clearToken() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(REFRESH_KEY)
}

function request(url, method, data) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: config.apiBaseUrl + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': getToken() ? `Bearer ${getToken()}` : ''
      },
      success(res) {
        const body = res.data
        if (res.statusCode === 401) {
          // Try refresh
          return tryRefresh().then(() => {
            // Retry original request
            request(url, method, data).then(resolve).catch(reject)
          }).catch(() => {
            clearToken()
            uni.reLaunch({ url: '/pages/login/login' })
            reject(new Error('登录已过期'))
          })
        }
        if (body.code !== 200) {
          uni.showToast({ title: body.message || '请求失败', icon: 'none' })
          reject(new Error(body.message))
          return
        }
        resolve(body.data)
      },
      fail(err) {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

function tryRefresh() {
  const refreshToken = uni.getStorageSync(REFRESH_KEY)
  if (!refreshToken) return Promise.reject()
  return new Promise((resolve, reject) => {
    uni.request({
      url: config.apiBaseUrl + '/api/auth/refresh',
      method: 'POST',
      data: { refreshToken },
      header: { 'Content-Type': 'application/json' },
      success(res) {
        if (res.data.code === 200) {
          setToken(res.data.data.token, res.data.data.refreshToken)
          resolve()
        } else {
          reject()
        }
      },
      fail: reject
    })
  })
}

export function get(url, params) {
  return request(url, 'GET', params)
}

export function post(url, data) {
  return request(url, 'POST', data)
}

export function put(url, data) {
  return request(url, 'PUT', data)
}

export function del(url) {
  return request(url, 'DELETE')
}

export { getToken, setToken, clearToken }
