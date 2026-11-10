# Changelog

All notable changes to EroLit are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.1] - 2026-11-10

### Added

- Long-press action sheet for story entries with quick navigation to the
  author, series, and category, plus tag search and Hot/New/writer's-pick
  badges.
- Offline series saving. A saved series stores its ordered chapter list on the
  device, appears under **My Lists → Offline → Saved series**, and opens its
  chapter table of contents even without a network connection.
- Story tags are now read from the upstream API, shown in the action sheet and
  metadata, and stored locally with offline stories.
- Six reader fonts: Serif, Sans, Mono, Cursive, Condensed, and Light, with a
  live preview inside reading settings.
- Dedicated series cards for series feeds and the offline list, including a
  long-press author action.
- Tag actions can open Search with the tag already filled in.

### Changed

- Reading settings now open in a bottom sheet instead of a dialog.
- Reader text is centered with a comfortable maximum line width, and page
  navigation uses icon buttons with a slimmer progress bar.
- Search has a clearer input field with a search icon, clear button, and
  support for a prefilled query.
- Screen headers, filter chips, section headers, and loading/error/empty states
  share one consistent Material 3 style.
- "More by author" actions moved into long-press and overflow menus to reduce
  clutter in lists.
- The theme now defines the full Material 3 surface color roles, with serif
  headings for a more book-like feel.
- Room database upgraded to version 3 with a migration that preserves existing
  cached stories, reading progress, and offline saves.

### Fixed

- Font selection now visibly changes the rendered reading text, and the
  selected font can be previewed before leaving settings.
- Offline story tags survive saving, reopening, and database upgrades.

## [0.1.0] - 2026-11-09

First public release.

### Added

- New, Top, and Series feeds, including top-rated, most-read, most-favorited,
  and time-range filters plus updated, discussed, favorited, recently started,
  and longest-running series sorting.
- Category browsing with story counts.
- Search for stories, poetry, and members, with sorting and local search
  history.
- Reader with server-side page navigation, page and progress indicators,
  resume-where-you-left-off, adjustable font, size, spacing, justification,
  and theme.
- Local saved story list that works without an account, plus caching of pages
  that have already been opened.
- Ordered series tables of contents and author profiles with their works.
- My Lists: sign in to view, create, and delete account lists, and add or
  remove stories.
- First-run 18+ age gate.
- Polite networking: one request in flight, rate limiting, and backoff for
  transient errors. No WebView, CAPTCHA bypass, or bulk downloading.
- Privacy-first defaults: no ads, analytics, telemetry, or crash reporting;
  session secrets encrypted with the Android Keystore; Android backups
  disabled.
