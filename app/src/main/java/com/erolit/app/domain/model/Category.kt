package com.erolit.app.domain.model

data class Category(
    val slug: String,
    val name: String,
    val storyCount: Int = 0,
    val description: String = ""
) {
    val url: String get() = "https://www.literotica.com/c/$slug"
}

val AllCategories = listOf(
    Category("anal-sex-stories", "Anal"),
    Category("audio-sex-stories", "Audio"),
    Category("bdsm-stories", "BDSM"),
    Category("chain-stories", "Chain Stories"),
    Category("crossdressing", "Crossdressing"),
    Category("erotic-couplings", "Erotic Couplings"),
    Category("erotic-horror", "Erotic Horror"),
    Category("exhibitionist-voyeur", "Exhibitionist & Voyeur"),
    Category("celebrity-fan-fiction", "Fan Fiction & Celebrities"),
    Category("fetish-stories", "Fetish"),
    Category("first-time-sex-stories", "First Time"),
    Category("gay-sex-stories", "Gay Male"),
    Category("group-sex-stories", "Group Sex"),
    Category("how-to", "How To"),
    Category("adult-humor", "Humor & Satire"),
    Category("illustrated-stories", "Illustrated"),
    Category("interracial-love-stories", "Interracial Love"),
    Category("lesbian-sex-stories", "Lesbian Sex"),
    Category("letters-transcripts", "Letters & Transcripts"),
    Category("loving-wives", "Loving Wives"),
    Category("mature-stories", "Mature"),
    Category("mind-control", "Mind Control"),
    Category("non-english-stories", "Non-English"),
    Category("non-erotic-stories", "Non-Erotic"),
    Category("non-human-stories", "NonHuman"),
    Category("erotic-novels", "Novels & Novellas"),
    Category("non-consent-reluctance", "Reluctance/NonConsent"),
    Category("reviews-essays", "Reviews & Essays"),
    Category("adult-romance", "Romance"),
    Category("science-fiction-fantasy", "Sci-Fi & Fantasy"),
    Category("taboo-sex-stories", "Taboo/Incest"),
    Category("masturbation-stories", "Toys & Masturbation"),
    Category("transgender-crossdressers", "Transgender"),
    Category("erotic-poetry", "Poetry")
)
