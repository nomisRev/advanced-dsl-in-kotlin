import assert from 'node:assert/strict'
import { chromium } from 'playwright-chromium'

const base = process.env.SLIDES_URL || 'http://localhost:4174'
const browser = await chromium.launch()
const runtimeErrors = []
const consoleErrors = new Set()
try {
  // Separate contexts ensure the test cannot pass via BroadcastChannel alone.
  const displayContext = await browser.newContext()
  const phoneContext = await browser.newContext({
    viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true,
  })
  // Headless/background tabs cannot hold a screen wake lock. This test covers
  // navigation and networking; leave the real presentation's setting intact.
  for (const context of [displayContext, phoneContext])
    await context.addInitScript(() => localStorage.setItem('slidev-wake-lock', 'false'))
  const display = await displayContext.newPage()
  const phone = await phoneContext.newPage()
  for (const page of [display, phone]) {
    page.on('pageerror', error => runtimeErrors.push(String(error)))
    page.on('console', message => {
      if (message.type() === 'error') consoleErrors.add(message.text().split('\n')[0])
    })
  }
  const state = page => {
    const url = new URL(page.url())
    return { page: Number(url.pathname.split('/').at(-1)), clicks: Number(url.searchParams.get('clicks') || 0) }
  }
  async function synchronized() {
    const target = state(phone)
    await display.waitForURL(url => Number(url.pathname.slice(1)) === target.page
      && Number(url.searchParams.get('clicks') || 0) === target.clicks)
    await display.waitForFunction(no => {
      const slide = document.querySelector(`[data-slidev-no="${no}"]`)
      return slide && slide.getBoundingClientRect().width > 0 && slide.textContent.trim().length > 0
    }, target.page)
  }
  async function tap(x = 350) {
    await phone.touchscreen.tap(x, 420)
    await phone.waitForTimeout(160)
  }
  await display.goto(`${base}/1`)
  await phone.goto(`${base}/presenter/1`)
  await phone.locator('.slide-count').waitFor()
  const total = Number((await phone.locator('.slide-count').innerText()).split('/')[1])
  const notesAtStart = await phone.locator('.speaker-notes').innerText()
  const seen = new Set([1])
  let previous = state(phone)
  for (let i = 0; previous.page < total && i < total * 10; i++) {
    await tap()
    const current = state(phone)
    assert(current.page === previous.page + 1
      || (current.page === previous.page && current.clicks === previous.clicks + 1),
    `Navigation stalled or reversed: ${JSON.stringify({ previous, current })}`)
    seen.add(current.page)
    previous = current
    await synchronized()
  }
  assert.equal(seen.size, total)
  await synchronized()
  assert.notEqual(await phone.locator('.speaker-notes').innerText(), notesAtStart)
  assert(await phone.locator('[data-slidev-no]').count() <= 2)
  await tap()
  assert.equal(state(phone).page, total)

  // Slide 16 has a reveal: going back from 17 must restore its final step.
  await phone.goto(`${base}/presenter/16`)
  await phone.locator('.slide-count').waitFor()
  await phone.waitForTimeout(300)
  await tap()
  assert.deepEqual(state(phone), { page: 16, clicks: 1 })
  await tap()
  assert.equal(state(phone).page, 17)
  await tap(30)
  assert.deepEqual(state(phone), { page: 16, clicks: 1 })
  await synchronized()
  await display.reload()
  await synchronized()
  await tap(30)
  assert.deepEqual(state(phone), { page: 16, clicks: 0 })
  await synchronized()

  // The display catches up after its connection is interrupted.
  await displayContext.setOffline(true)
  await tap()
  await tap()
  await displayContext.setOffline(false)
  await synchronized()
  assert.deepEqual(runtimeErrors, [])
  console.log(`Passed: ${total} slides, reveals, back navigation, notes, reload and reconnect; no uncaught browser errors.`)
  for (const message of consoleErrors) console.log(`Console diagnostic: ${message}`)
}
finally {
  await browser.close()
}
