// 插件注册入口 - 后续扩展地图、支付、推送等
const plugins = []

export function registerPlugin(plugin) {
  if (plugin && typeof plugin.install === 'function') {
    plugin.install()
    plugins.push(plugin)
  }
}

export function getPlugins() {
  return plugins
}
