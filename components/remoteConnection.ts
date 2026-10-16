import { reactive } from 'vue'

export const remoteConnection = reactive({
  enabled: false,
  connected: false,
  displays: 0,
})
