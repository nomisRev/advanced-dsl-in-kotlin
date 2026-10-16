import type { AppContext } from '@slidev/types'
import { addSyncMethod, disableBuiltinSync } from '@slidev/client/state/syncState.ts'
import { remoteConnection } from '../components/remoteConnection'

export default function setupMain({ router }: AppContext) {
  let token = (window as Window & { __productionRemote?: string }).__productionRemote
  if (!token)
    return

  remoteConnection.enabled = true
  disableBuiltinSync()
  addSyncMethod({
    init(channelKey) {
      if (!channelKey.endsWith(' - shared'))
        return

      const isPresenter = () => location.pathname.includes('/presenter/')
      let incoming: { page: number, clicks: number } | undefined
      let applying = false
      async function follow() {
        if (applying)
          return
        applying = true
        try {
          await router.isReady()
          while (incoming) {
            const target = incoming
            incoming = undefined
            await router.replace({
              path: `/${target.page}`,
              query: { ...router.currentRoute.value.query, clicks: target.clicks },
            })
          }
        }
        finally {
          applying = false
        }
      }

      let events: EventSource | undefined
      let reconnectTimer: ReturnType<typeof setTimeout> | undefined
      let connecting = false
      let outgoing: string | undefined
      let pending: string | undefined
      let sending = false

      function reconnect() {
        remoteConnection.connected = false
        remoteConnection.displays = 0
        events?.close()
        if (!reconnectTimer)
          reconnectTimer = setTimeout(() => {
            reconnectTimer = undefined
            void connect()
          }, 1000)
      }

      async function connect() {
        if (connecting)
          return
        connecting = true
        try {
          // Tokens change on a server restart. Never keep retrying a stale one.
          const response = await fetch('/__remote/session', { cache: 'no-store' })
          if (!response.ok)
            throw new Error('Remote server unavailable')
          token = (await response.json()).token
          events?.close()
          const role = isPresenter() ? 'presenter' : 'display'
          events = new EventSource(`/__remote/events?token=${encodeURIComponent(token!)}&role=${role}`)
          events.onopen = () => {
            remoteConnection.connected = true
            if (isPresenter() && outgoing) {
              pending = outgoing
              void send()
            }
          }
          events.addEventListener('peers', event => {
            remoteConnection.displays = JSON.parse((event as MessageEvent).data).displays
          })
          events.onmessage = event => {
            if (!isPresenter()) {
              incoming = JSON.parse(event.data)
              void follow()
            }
          }
          events.onerror = reconnect
        }
        catch {
          reconnect()
        }
        finally {
          connecting = false
        }
      }

      // Serialize writes so a slow request cannot overwrite a newer tap.
      async function send() {
        if (sending || !remoteConnection.connected)
          return
        sending = true
        try {
          while (pending && remoteConnection.connected) {
            const body = pending
            pending = undefined
            try {
              const response = await fetch('/__remote/state', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
                body,
              })
              if (!response.ok)
                throw new Error(`Remote sync: ${response.status}`)
            }
            catch {
              pending ??= body
              reconnect()
            }
          }
        }
        finally {
          sending = false
        }
      }
      void connect()
      return (state, updating) => {
        if (updating || !isPresenter())
          return
        outgoing = pending = JSON.stringify(state)
        void send()
      }
    },
  })
}
