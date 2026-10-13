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
