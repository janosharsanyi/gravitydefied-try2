# Gravity Defied — Android (modernized)

A migration of the open-source Android port of *Gravity Defied* from its
original Eclipse / Ant / `targetSdk=19` form into a modern Android Studio
project (Gradle Kotlin DSL, AndroidX, current `compileSdk` / `targetSdk`,
`minSdk = 23`, scoped storage via SAF, runtime permissions, and various
deprecated-API replacements).

Game logic, physics, menus, levels and assets are ported essentially
unchanged from upstream. Only platform glue is rewritten.

## Upstream

Ported from the Android port of Gravity Defied:
<https://github.com/evgenyzinoviev/gravitydefied>

Baseline: upstream commit
[`ee26c95`](https://github.com/evgenyzinoviev/gravitydefied/commit/ee26c95fddff87826cfba7bfcdda6bac156c8741)
("Add GPLv2 license", 2015-08-14). Imported on 2026-05-09. This repository's
git history is intentionally orphan from upstream — every commit here is a
modification made as part of the migration. To diff against upstream, check
out the SHA above in the upstream repo and compare manually.

See [NOTICE](NOTICE) for a categorized summary of changes from upstream
(GPLv2 §2(a) modification notice).

## Authors

### Modernization
* This repository — Android Studio / Gradle migration, modern-API plumbing,
  controller input, on-screen keypad rework.
* Developed with AI coding assistance.

### Original Android port
* **[Gregory Klushnikov](https://vk.com/grishka)** — original J2ME-to-Android
  port, idea.
* **[Evgeny Zinoviev](https://vk.com/ez)** — porting, levels manager, levels
  API, graphics, everything else.

### Original Codebrew GDTR (J2ME, 2004)
* **Tors Björn Henrik Johansson** — system / game logic / interface, testing,
  level design.
* **Set Elis Norman** — graphics, physics, math, system, tools programming,
  level design.
* **Per David Jacobsson** — physics programming, game graphics, level design.

For more on the original game, see Codebrew Software:
<http://codebrew.se>

## Disclaimer

***This project is not associated with Codebrew Software in any fashion. All
rights to the original Gravity Defied — its name, logotype, brand and all
that stuff — belong to Codebrew Software.***

## License

GPL v2 — see [LICENSE.txt](LICENSE.txt).

This is a derivative work of the upstream Android port (also GPL v2). Per
GPLv2 §2(a), the work has been modified — see [NOTICE](NOTICE) for a
categorized summary, and the git history for the per-commit detail.
