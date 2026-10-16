import { syncDirections } from '@slidev/client/state/storage.ts'

export default function setupRoot() {
  // The phone is the controller. Viewer route changes must not echo back and
  // overwrite newer taps while the projected slide is still transitioning.
  syncDirections.value.viewerSend = false
  syncDirections.value.viewerReceive = true
  syncDirections.value.presenterSend = true
  syncDirections.value.presenterReceive = false
}
