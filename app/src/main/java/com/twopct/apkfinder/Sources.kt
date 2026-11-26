package com.twopct.apkfinder

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

data class ApkResult(val title: String, val summary: String, val source: String, val pageUrl: String, val directUrl: String? = null)

object Sources {
    private val client = OkHttpClient()
    private fun get(url: String): String =
        client.newCall(Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Android) 2pctApkFinder").build())
            .execute().use { it.body?.string().orEmpty() }

    /** F-Droid: open-source apps and community builds, with direct signed-APK links. */
    fun fdroid(q: String): List<ApkResult> {
        val doc = Jsoup.parse(get("https://search.f-droid.org/?lang=en&q=" + URLEncoder.encode(q, "UTF-8")))
        return doc.select("a.package-header").take(15).map { a ->
            val page = a.absUrl("href")
            val pkg = page.trimEnd('/').substringAfterLast('/')
            val direct = runCatching {
                val v = JSONObject(get("https://f-droid.org/api/v1/packages/$pkg")).getInt("suggestedVersionCode")
                "https://f-droid.org/repo/${pkg}_$v.apk"
            }.getOrNull()
            ApkResult(a.select(".package-name").text(), a.select(".package-summary").text(), "F-Droid", page, direct)
        }
    }

    /** APKMirror: original, signature-verified APKs (links to the download page). */
    fun apkMirror(q: String): List<ApkResult> {
        val doc = Jsoup.parse(get("https://www.apkmirror.com/?post_type=app_release&searchtype=apk&s=" + URLEncoder.encode(q, "UTF-8")), "https://www.apkmirror.com")
        return doc.select("div.appRow a.fontBlack").take(15).map {
            ApkResult(it.text(), "", "APKMirror", it.absUrl("href"))
        }
    }
}
