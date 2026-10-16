import { randomBytes } from 'node:crypto'
import { createReadStream } from 'node:fs'
import { readFile, realpath, stat } from 'node:fs/promises'
import { createServer } from 'node:http'
import { networkInterfaces } from 'node:os'
import { extname, resolve, sep } from 'node:path'

// A local, single-presenter server for the production build. No internet needed.
const root = await realpath(new URL('../dist', import.meta.url))
const token = randomBytes(24).toString('hex')
const html = (await readFile(resolve(root, 'index.html'), 'utf8'))
  .replace('<head>', `<head><script>window.__productionRemote=${JSON.stringify(token)}</script>`)
const clients = new Map()
let latest
function broadcastPeers() {
  const displays = [...clients.values()].filter(role => role === 'display').length
  for (const client of clients.keys())
    client.write(`event: peers\ndata: ${JSON.stringify({ displays })}\n\n`)
}
const types = {
  '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css',
  '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png',
  '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.webp': 'image/webp',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf',
  '.mp4': 'video/mp4', '.pdf': 'application/pdf', '.txt': 'text/plain',
}

const server = createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://localhost')
    res.setHeader('Cache-Control', 'no-store')
    if (url.pathname === '/__remote/session' && req.method === 'GET') {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({ token }))
      return
    }
    if (url.pathname === '/__remote/events') {
      if (req.method !== 'GET' || url.searchParams.get('token') !== token) {
        res.writeHead(403).end()
        return
      }
      res.writeHead(200, { 'Content-Type': 'text/event-stream', Connection: 'keep-alive' })
      res.write(': connected\n\n')
      clients.set(res, url.searchParams.get('role') === 'presenter' ? 'presenter' : 'display')
      broadcastPeers()
      if (latest) res.write(`data: ${latest}\n\n`)
      const heartbeat = setInterval(() => res.write(': heartbeat\n\n'), 15000)
      res.on('close', () => { clients.delete(res); clearInterval(heartbeat); broadcastPeers() })
      return
    }
    if (url.pathname === '/__remote/state') {
      if (req.method !== 'POST' || req.headers.authorization !== `Bearer ${token}`) {
        res.writeHead(403).end()
        return
      }
      let body = ''
      for await (const chunk of req) {
        body += chunk
        if (body.length > 65536) { res.writeHead(413).end(); return }
      }
      const state = JSON.parse(body)
      if (!Number.isInteger(state.page) || state.page < 1
        || !Number.isInteger(state.clicks) || state.clicks < 0
        || state.lastUpdate?.type !== 'presenter') {
        res.writeHead(400).end()
        return
      }
      latest = JSON.stringify(state)
      for (const client of clients.keys()) client.write(`data: ${latest}\n\n`)
      res.writeHead(204).end()
      return
    }
    if (!['GET', 'HEAD'].includes(req.method)) { res.writeHead(405).end(); return }
    const pathname = decodeURIComponent(url.pathname)
    if (pathname === '/index.html' || !extname(pathname)) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
      res.end(req.method === 'HEAD' ? undefined : html)
      return
    }
    const file = await realpath(resolve(root, `.${pathname}`))
    if (!file.startsWith(root + sep)) { res.writeHead(403).end(); return }
    const info = await stat(file)
    if (!info.isFile()) { res.writeHead(404).end(); return }
    res.writeHead(200, { 'Content-Type': types[extname(file)] ?? 'application/octet-stream', 'Content-Length': info.size })
    if (req.method === 'HEAD') res.end()
    else createReadStream(file).on('error', () => res.destroy()).pipe(res)
  }
  catch (error) {
    if (!res.headersSent) res.writeHead(error.code === 'ENOENT' ? 404 : 400)
    res.end()
  }
})

const port = Number(process.env.PORT || 4174)
server.listen(port, '0.0.0.0', () => {
  console.log(`Display: http://localhost:${port}/1`)
  for (const addresses of Object.values(networkInterfaces())) {
    for (const address of addresses ?? []) {
      if (address.family === 'IPv4' && !address.internal)
        console.log(`Phone remote: http://${address.address}:${port}/presenter/1`)
    }
  }
})
