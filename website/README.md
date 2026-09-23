# BTC Map Android documentation site

A small [Hugo](https://gohugo.io/) site that renders the user guides with a
sidebar, typography and screenshots. It is published to
[android.btcmap.org](https://android.btcmap.org/).

The site uses Hugo's conventional layout, with the pages in
[`content/`](content) and the screenshots in [`static/images/`](static/images):

- `content/_index.md` is the overview page.
- `content/getting-started/` holds the step-by-step install and first-run guides.
- `content/features/` holds the standalone guide for each feature.
- `static/images/` holds the screenshots, served from `/images/`.

The sidebar order for the two sections lives in `params.gettingStarted` and
`params.features` in `hugo.toml`; add a page to the matching list when you
create one.

## Requirements

Hugo extended, without Go (the site uses no remote modules):

```sh
hugo version   # any recent extended release
```

## Build and preview

Run everything from this directory, not the repository root:

```sh
cd website
hugo server        # live preview at http://localhost:1313/
hugo               # one-off build into website/public/
```

## GitHub links

Pages link to files outside `content/` (`../../README.md` from the home page,
`../../../README.md` from a guide). The link render hook in
`layouts/_default/_markup/render-link.html` rewrites those to
`params.githubRepo`, so they resolve to the repository instead of 404ing.
Internal relative links (`features/merchants.md`, `../index.md`) become the built page
URLs.

## Styling

The site follows Material 3 (Material You), the same design system as the app's
`Theme.Material3Expressive.DynamicColors` theme. Everything lives in one
hand-written stylesheet, `static/css/style.css`.

- **Color.** The **light** `--md-sys-color-*` roles are an M3 *tonal spot*
  scheme generated from the Bitcoin orange brand seed `#f7931a` (the app's
  boosted-marker default) — the same variant Android's dynamic color uses. The
  **dark** roles are a bespoke palette aligned with the
  `dashboard.btcmap.org` web app (its `css/styles.css`): neutral zinc
  surfaces, the `#f7931a` accent and the teal brand. Dark follows
  `prefers-color-scheme`. To re-seed the light scheme, regenerate the roles
  with
  [`material-color-utilities`](https://github.com/material-foundation/material-color-utilities)
  (`SchemeTonalSpot`) and paste them back.
- **Type.** Roboto is self-hosted as one variable Latin woff2
  (`static/fonts/roboto-latin.woff2`, SIL OFL — see `static/fonts/OFL.txt`) and
  applied with the M3 typescale (headline, title, body and label roles).
- **Brand.** The sidebar uses the official multicolor pin logo, vendored as
  `static/logo.svg` from
  [`dashboard.btcmap.org/icons/btcmap.svg`](https://dashboard.btcmap.org/icons/btcmap.svg).
- **Shape, elevation, motion and state layers** use the `--md-sys-*` tokens at
  the top of the file. The sidebar is an M3 navigation drawer with a rounded
  active indicator; on mobile it becomes an app bar with a CSS-only menu.

`--image-max-height` (default `min(70vh, 40rem)`) caps how tall the portrait
phone screenshots get; lower it if they still feel too dominant.

## Publishing

The site is published to `https://android.btcmap.org/` with, from the
repository root:

```sh
./devtools website deploy
```

That builds the site (`hugo` in this directory) and rsyncs `website/public/` to
`btcmap-api:/srv/http/android.btcmap.org/`, removing files that are no longer
part of the build. `baseURL` in `hugo.toml` is set to that host; `hugo server`
overrides it while previewing.
