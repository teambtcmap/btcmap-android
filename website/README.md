# BTC Map Android documentation site

A small [Hugo](https://gohugo.io/) site that renders the user guides in
[`../docs`](../docs) with a sidebar, typography and screenshots.

The markdown is **not** copied or moved. `hugo.toml` mounts the pages from
`../docs` into the build, so the files under `docs/` remain the single source of
truth and keep rendering on GitHub exactly as before. Nothing under `content/`
is committed — it only exists for the duration of a build.

A directory whose top level contains `index.md` is treated by Hugo as a *leaf
bundle* that swallows its siblings, so `docs/` is mounted one entry at a time
rather than wholesale: `index.md` as the home page, `getting-started.md`, the
`features/` directory (which picks up new guides automatically), and `images/`.
Add a mount when a new top-level page appears outside `features/`.

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

`docs/` pages link to files outside `docs/` (`../README.md`, `../AGENTS.md`).
The link render hook in `layouts/_default/_markup/render-link.html` rewrites
those to `params.githubRepo`, so they resolve to the repository instead of
404ing. Internal relative links (`features/map.md`, `../index.md`) become the
built page URLs, and the `docs/images/*` screenshots are served from the site
root.

## Styling

The whole site is one hand-written stylesheet,
`static/css/style.css`, with the palette and layout defined as custom
properties at the top. In particular `--image-max-height`
(default `min(70vh, 40rem)`) caps how tall the portrait phone screenshots get;
lower it if they still feel too dominant.

## Publishing

`baseURL` in `hugo.toml` is a placeholder. Set it to the URL the site is served
from before deploying; `hugo server` overrides it while previewing. The build
output is `website/public/`.
