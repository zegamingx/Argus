package com.argus.agi.data.acquisition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class SearchResultItem(
    val title: String,
    val snippet: String,
    val sourceUrl: String
)

class WebSearchEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun search(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResultItem>()
        if (query.isBlank()) return@withContext results

        // 1. First attempt: Direct Wikipedia API search for verified factual knowledge
        try {
            val encodedQuery = URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
            val enWikiUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encodedQuery&limit=4&namespace=0&format=json"

            val wikiResults = fetchWikiResults(enWikiUrl)
            results.addAll(wikiResults)
        } catch (_: Exception) {}

        // 2. DuckDuckGo HTML Search Query
        if (results.size < 3) {
            try {
                val encodedQuery = URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                val ddgUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"
                val request = Request.Builder()
                    .url(ddgUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string() ?: ""
                        val ddgItems = parseDdgHtml(html)
                        results.addAll(ddgItems)
                    }
                }
            } catch (_: Exception) {}
        }

        results.distinctBy { it.title }.take(5)
    }

    private fun fetchWikiResults(url: String): List<SearchResultItem> {
        val list = mutableListOf<SearchResultItem>()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ARGUS-Autonomous-System/1.0 (Android)")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: return emptyList()
                val json = JSONArray(bodyStr)
                if (json.length() >= 4) {
                    val titles = json.getJSONArray(1)
                    val snippets = json.getJSONArray(2)
                    val urls = json.getJSONArray(3)
                    for (i in 0 until titles.length()) {
                        val title = titles.optString(i)
                        val snippet = snippets.optString(i)
                        val sourceUrl = urls.optString(i)
                        if (snippet.isNotBlank()) {
                            list.add(SearchResultItem(title = title, snippet = snippet, sourceUrl = sourceUrl))
                        }
                    }
                }
            }
        }
        return list
    }

    private fun parseDdgHtml(html: String): List<SearchResultItem> {
        val list = mutableListOf<SearchResultItem>()
        // Extract result snippet blocks via regex
        val snippetPattern = Pattern.compile("<a class=\"result__snippet[^\"]*\"[^>]*>(.*?)</a>", Pattern.DOTALL)
        val titlePattern = Pattern.compile("<a class=\"result__url[^\"]*\"[^>]*>(.*?)</a>", Pattern.DOTALL)

        val snippetMatcher = snippetPattern.matcher(html)
        val titleMatcher = titlePattern.matcher(html)

        var count = 0
        while (snippetMatcher.find() && count < 4) {
            val rawSnippet = snippetMatcher.group(1) ?: ""
            val cleanSnippet = rawSnippet.replace(Regex("<[^>]*>"), "").replace("&quot;", "\"").replace("&amp;", "&").trim()

            var cleanUrl = "Web Resource"
            if (titleMatcher.find()) {
                val rawUrl = titleMatcher.group(1) ?: ""
                cleanUrl = rawUrl.replace(Regex("<[^>]*>"), "").trim()
            }

            if (cleanSnippet.length > 20) {
                list.add(
                    SearchResultItem(
                        title = cleanUrl.substringBefore("/").ifBlank { "Web result" },
                        snippet = cleanSnippet,
                        sourceUrl = cleanUrl
                    )
                )
                count++
            }
        }
        return list
    }
}
