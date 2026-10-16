import type { RouteRecordRaw } from 'vue-router'
import RemoteControl from '../components/RemoteControl.vue'

/** Replace Slidev's desktop presenter console with a phone-first remote. */
export default function setupRoutes(routes: RouteRecordRaw[]) {
  const presenter = routes.find(route => route.name === 'presenter')
  if (presenter)
    presenter.component = RemoteControl
  return routes
}
