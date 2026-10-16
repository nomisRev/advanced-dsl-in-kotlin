import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { chromium } from 'playwright-chromium'

const port = process.env.TEST_PORT || '4175'
const displayURL = `http://localhost:${port}`
const remoteURL = process.env.REMOTE_URL || displayURL
let server
async function start() {
  server = spawn(process.execPath, ['scripts/serve.mjs'], {
    env: { ...process.env, PORT: port }, stdio: ['ignore', 'pipe', 'inherit'],
  })
  await Promise.race([
    once(server.stdout, 'data'),
    once(server, 'exit').then(() => { throw new Error('Test server failed to start') }),
  ])
}
async function stop() {
  const stopped = once(server, 'exit')
  server.kill('SIGTERM')
  await stopped
}
const browser = await chromium.launch()
try {
  await start()
  const displayContext = await browser.newContext()
  const remoteContext = await browser.newContext({viewport: {width: 390, height: 844}, hasTouch: true, isMobile: true})
  for (const context of [displayContext, remoteContext])
    await context.addInitScript(() => localStorage.setItem('slidev-wake-lock', 'false'))
  const display = await displayContext.newPage()
  const remote = await remoteContext.newPage()
  await display.goto(`${displayURL}/1`)
  await remote.goto(`${remoteURL}/presenter/1`)
  await remote.getByRole('status').filter({hasText: 'Connected · 1 display'}).waitFor()
  await remote.touchscreen.tap(350, 420)
  await display.waitForURL('**/2?clicks=0')
  const previousToken = await remote.evaluate(() => window.__productionRemote)
  await stop()
  await remote.getByRole('status').filter({hasText: 'Reconnecting'}).waitFor()
  await start()
  const nextToken = (await (await fetch(`${displayURL}/__remote/session`)).json()).token
  assert.notEqual(previousToken, nextToken)
  // Neither page is reloaded after the token changes.
  await remote.getByRole('status').filter({hasText: 'Connected · 1 display'}).waitFor()
  await remote.touchscreen.tap(350, 420)
  await display.waitForURL('**/3?clicks=0')
  await display.close()
  await remote.getByRole('status').filter({hasText: 'No display connected'}).waitFor()
  const newDisplay = await displayContext.newPage()
  await newDisplay.goto(`${displayURL}/1`)
  await newDisplay.waitForURL('**/3?clicks=0')
  await remote.getByRole('status').filter({hasText: 'Connected · 1 display'}).waitFor()
  console.log(`Passed: ${remoteURL} controls ${displayURL}; reconnects after token rotation without reloading; display presence is accurate.`)
} finally {
  await browser.close()
  if (server && server.exitCode === null) await stop()
}
