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
speaker. The presenter timer is a stopwatch with a 50-minute target. The deck ends on
the closing slide (72); the backup material lives in `extras.md` (`npm run extras`).

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

The theme's `slidev-kotlin-present` serves the built `dist/` on port 4174
(`PORT=4175 npm run present` changes it) and prints the display link for the
Mac plus a QR code for the phone. Scan it on the same network; no internet
connection is required. Tap the right half to advance or the left half to go
back. On the Mac, `/presenter/1` is still Slidev's presenter console with the
stopwatch.

Only a page opened from the printed link can move the slides. Present over a
hotspot or a password-protected network: the link travels over plain HTTP.
After a rebuild, reload both devices. See "Presenting with a phone remote" in
the theme's README.
