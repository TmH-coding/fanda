export function getPlatform() {
  // #ifdef H5
  return 'h5'
  // #endif
  // #ifdef MP-WEIXIN
  return 'weixin'
  // #endif
  // #ifdef APP-PLUS
  return 'app'
  // #endif
}

export function getStatusBarHeight() {
  const sysInfo = uni.getSystemInfoSync()
  return sysInfo.statusBarHeight || 44
}

export function getWindowHeight() {
  const sysInfo = uni.getSystemInfoSync()
  return sysInfo.windowHeight || 750
}
