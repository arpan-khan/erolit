package dev.erolit.reader

import android.content.Context
import coil.ImageLoader
import dev.erolit.reader.core.auth.KeystoreSecretStore
import dev.erolit.reader.core.auth.SessionRepository
import dev.erolit.reader.core.db.AppDatabase
import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.ApiConfig
import dev.erolit.reader.core.net.ApiRoutes
import dev.erolit.reader.core.net.RateLimiter
import dev.erolit.reader.core.net.RequestLog
import dev.erolit.reader.core.net.UserAgent
import dev.erolit.reader.core.settings.SettingsRepository
import dev.erolit.reader.data.repo.AuthorRepository
import dev.erolit.reader.data.repo.CatalogRepository
import dev.erolit.reader.data.repo.CategoryCache
import dev.erolit.reader.data.repo.ListsRepository
import dev.erolit.reader.data.repo.SearchRepository
import dev.erolit.reader.data.repo.SeriesRepository
import dev.erolit.reader.data.repo.StoryRepository
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings = SettingsRepository(context)
    val db = AppDatabase.build(context)
    val requestLog = RequestLog()
    val rateLimiter = RateLimiter(minIntervalMs = 1_000L)

    val apiConfig = ApiConfig()
    val routes = ApiRoutes(apiConfig)
    val apiClient = ApiClient(rateLimiter = rateLimiter, requestLog = requestLog)

    val secretStore = KeystoreSecretStore(context)
    val session = SessionRepository(
        api = apiClient,
        routes = routes,
        secretStore = secretStore,
        db = db,
        appScope = appScope,
    )

    val categoryCache = CategoryCache()
    val catalog = CatalogRepository(apiClient, routes, db, categoryCache)
    val stories = StoryRepository(apiClient, routes, db)
    val series = SeriesRepository(apiClient, routes, categoryCache)
    val authors = AuthorRepository(apiClient, routes, categoryCache)
    val search = SearchRepository(apiClient, routes, categoryCache, db)
    val lists = ListsRepository(apiClient, routes, categoryCache)

    val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .okHttpClient {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .followRedirects(true)
                .dispatcher(
                    Dispatcher().apply {
                        maxRequests = 1
                        maxRequestsPerHost = 1
                    },
                )
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", UserAgent.VALUE)
                            .build(),
                    )
                }
                .addInterceptor { chain ->
                    rateLimiter.awaitSlot()
                    chain.proceed(chain.request())
                }
                .build()
        }
        .crossfade(true)
        .build()

    init {
        apiClient.tokenProvider = session
        appScope.launch {
            settings.settings.collect { current ->
                rateLimiter.minIntervalMs = if (current.slowMode) 2_000L else 1_000L
            }
        }
        appScope.launch { catalog.preload() }
    }

    suspend fun clearLocalData() {
        db.withTransaction {
            db.storyDao().clearAllPages()
            db.progressDao().clear()
            db.searchHistoryDao().clear()
            db.categoryDao().clear()
        }
        categoryCache.clear()
    }
}
