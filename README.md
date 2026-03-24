# EroLit

> [!WARNING]
> **Status:** Still not fully functional. Most of its functions don't work.

A modern Android story reader app for [Literotica.com](https://www.literotica.com) — built in **Kotlin** with **Jetpack Compose (Material 3)**.

---

## Architecture
Clean Architecture · MVVM · Hilt DI

```
app/
├── data/
│   ├── local/         Room DB (entities, DAOs, database)
│   ├── paging/        Paging 3 sources
│   ├── remote/        Jsoup HTML scraper + OkHttp data source
│   └── repository/    Repository implementations
├── domain/
│   ├── model/         Story, Author, Category, Tag, Series, ReadingList
│   └── repository/    Repository interfaces
├── di/                Hilt modules (Network, Database, Repository)
└── ui/
    ├── components/    StoryCard, AuthorCard, ErrorView, etc.
    ├── navigation/    NavGraph, Screen routes
    ├── screen/        Home, Reader, Browse, Search, Library, Author
    └── theme/         Material 3 colors, typography, theme
```

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Local Storage | Room |
| Networking | OkHttp |
| HTML Parsing | Jsoup |
| Images | Coil |
| Pagination | Paging 3 |
| Background | WorkManager |
| State | StateFlow + ViewModel |

## Basic Functions (WIP)

- **Browse Stories** — Page-by-page fetching of story lists.
- **Reader** — Basic text display with theme and font size control.
- **Library** — Local favorites and reading history.
- **Search** — Simple search by title/tag.
- **Offline Mode** — Native database storage for stories.

## Data Source

All content is fetched via **HTML scraping** of Literotica's classic server-rendered pages using **Jsoup**. No official API is used.

Key URL patterns:
- Stories: `/s/{slug}` → `?page=N`
- Categories: `/c/{slug}`
- New: `/new/stories`
- Top-rated: `/top/top-rated-erotic-stories/`
- Authors: `/authors/{username}`
- Tags: `tags.literotica.com/{tag}/`

## Notes

- The site has no public REST API — HTML scraping is the only approach
- Classic HTML pages (not the beta SPA) are targeted for reliable parsing
- ProGuard rules preserve Jsoup, OkHttp, Room, and Hilt classes in release builds
