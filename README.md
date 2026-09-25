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

## Build and export

```bash
npm run build
npm run export
```
