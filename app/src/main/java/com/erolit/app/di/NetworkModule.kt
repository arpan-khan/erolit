package com.erolit.app.di

import android.content.Context
import android.webkit.CookieManager
import com.erolit.app.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val webViewCookieJar = object : CookieJar {
            private val cookieManager = CookieManager.getInstance()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val urlString = url.toString()
                for (cookie in cookies) {
                    cookieManager.setCookie(urlString, cookie.toString())
                }
                cookieManager.flush()
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookieStr = cookieManager.getCookie(url.toString()) ?: return emptyList()
                return cookieStr.split(";").mapNotNull {
                    val trimmed = it.trim()
                    val split = trimmed.split("=", limit = 2)
                    if (split.size == 2) {
                        Cookie.Builder()
                            .name(split[0])
                            .value(split[1])
                            .domain(url.host)
                            .build()
                    } else null
                }
            }
        }

        // Add Cache to stay lightweight and fast
        val cacheSize = 50 * 1024 * 1024L // 50MB
        val cache = Cache(File(context.cacheDir, "http_cache"), cacheSize)

        return OkHttpClient.Builder()
            .cache(cache)
            .cookieJar(webViewCookieJar)
            .addNetworkInterceptor(logging)
            .connectTimeout(Constants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", Constants.USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Referer", "https://www.literotica.com/")
                    .build()
                chain.proceed(request)
            }
            .followRedirects(true)
            .build()
    }
}
