# Publish advanced-dsl-in-kotlin on GitHub Pages with the static content and analytics

**Status:** open
**Repository:** advanced-dsl-in-kotlin
**Depends on:** slidev-theme-kotlin release (see ../../slidedev-theme-kotlin/issues/002)

## Problem

The deck is scaffolded locally on branch `main` with no commits and no remote.
The headmatter already has an `info:` paragraph but no `themeConfig.siteUrl`,
so a build would write no `sitemap.xml` and its handout links would be
base-relative. `package.json` depends on the theme through
`file:../slidedev-theme-kotlin`, which cannot resolve in the GitHub Action.
`deploy.yml` already builds with `--router-mode hash`.

## Work

- [ ] Make the first commit, create `nomisRev/advanced-dsl-in-kotlin`, push, and
      enable GitHub Pages with the Actions source.
- [ ] Depend on the released theme version instead of `file:`.
- [ ] Add to `themeConfig`:

  ```yaml
  siteUrl: https://nomisrev.github.io/advanced-dsl-in-kotlin/
  analytics:
    goatcounter: nomsrev
  ```

- [ ] Decide whether the Kotlin fences should compile (`themeConfig.snippets`
      plus a Gradle project, as in ktor-fundamentals). A DSL talk leans on
      context parameters and builders, so a compile check catches API drift.
- [ ] Register the deck on the blog: add `src/content/talks/advanced-dsl-in-kotlin.md`
      in new-blog with `slides: https://nomisrev.github.io/advanced-dsl-in-kotlin/`.
- [ ] After the first deploy, verify `sitemap.xml`, `handout/`, `llms.txt`, and
      `llms-full.txt` return 200 under `/advanced-dsl-in-kotlin/`.

## Acceptance

- [ ] The deck, its handout, and its sitemap are live and listed by the blog.
- [ ] Slide paths appear in the GoatCounter dashboard under `/advanced-dsl-in-kotlin/`.
