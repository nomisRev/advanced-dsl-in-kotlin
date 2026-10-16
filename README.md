# Advanced DSL in Kotlin

Slidev version of the *Advanced DSL in Kotlin* talk, built on
[`slidev-theme-kotlin`](https://www.npmjs.com/package/slidev-theme-kotlin).

## Run locally

Requires Node.js 20.12 or newer.

```bash
npm install
npm run dev
```

The optional **Lesson 0** recap is kept out of the main deck and its export.
Run `npm run lesson-0` to present it, or
`npx slidev export lesson-0.md` to export it separately.
It revisits the builder-to-DSL progression from the September 3 deck, adapted
to the invoice export from *Your first Kotlin DSL*, and ends where Part 1 begins.
The source is [`lessons/lesson-0.md`](lessons/lesson-0.md); its Kotlin snippets
are generated and checked alongside the main deck with `npm run snippets`
and `./gradlew build`.

## Rehearsal

The main talk targets **50 minutes**, with lesson 0 covered by the preceding
speaker. The presenter timer is a stopwatch with a 50-minute target. Stop at
the closing slide (83); the slides after it are backup material.

These are rehearsal checkpoints, not measured timings:

| Section | Minutes | Elapsed |
| --- | ---: | ---: |
| Introduction and handover | 1 | 1 |
| Scope control | 5 | 6 |
| Lambda return-type overloads, including extension resolution | 6 | 12 |
| Delegates, including discarded-return warnings | 8 | 20 |
| Extensions with dynamic dispatch | 6 | 26 |
| Context parameters, context lambdas, and scope control | 8 | 34 |
| Cost and evolution, expression trees, and reusable sheets | 15 | 49 |
| Recap and closing | 1 | 50 |

Original versions of every updated or promoted slide from this edit are kept
in [`old-slides.md`](old-slides.md), with their source locations in the cover's
presenter notes. Run `npm run old-slides` to view them. Their original annotations
and notes are preserved. The archive is separate from snippet generation.

## Build and export

```bash
npm run build
npm run export
```

## Production presentation with a phone remote

```bash
npm run build
npm run present
```

Open the printed display URL on the Mac and the phone remote URL on the iPhone,
on the same local network. No internet connection is required. This serves the
built `dist/` files and relays navigation locally; an ordinary static host only
synchronizes tabs in the same browser. Use `PORT=4175 npm run present` to change
the default port (4174). Rebuild and reload both devices after source changes.

The phone controls the display in one direction to prevent delayed display
transitions from sending old slide numbers back. Tap the right half to advance
or the left half to go back. The remote mounts only the current and upcoming
slides. This local server is intended for a trusted presentation network, not
public hosting.

With the server running, `npm run test:remote` checks the full deck in isolated
Chromium display and phone contexts, including reveals, backwards navigation,
speaker notes, and reconnecting. It does not replace testing on physical Safari.

The remote shows how many displays are connected and reconnects automatically
after a server restart. Reload both devices once when updating the application.
`npm run test:remote-restart` checks server restart recovery on a separate test
server (port 4175). Set `REMOTE_URL=http://<Mac-LAN-IP>:4175` to also test from
the LAN address to the localhost display.
