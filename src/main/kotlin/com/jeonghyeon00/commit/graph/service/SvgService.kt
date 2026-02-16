package com.jeonghyeon00.commit.graph.service

import com.jeonghyeon00.commit.graph.config.Caches
import com.jeonghyeon00.commit.graph.domain.Language
import com.jeonghyeon00.commit.graph.domain.SizeAndColor
import com.jeonghyeon00.commit.graph.domain.Theme
import com.jeonghyeon00.commit.graph.infrastructure.github.GithubGraphQLClient
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.roundToInt

@Service
class SvgService(
    private val githubGraphQLClient: GithubGraphQLClient
) {
    companion object {
        const val LINK = "https://github.com/jeonghyeon00/github-decorator"
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Cacheable(Caches.SVG_LANGUAGE, key = "#nickname + #theme")
    fun generateMostUsedLanguagesSvg(nickname: String, theme: Theme): String {
        val allLanguages = getMostUsedLanguages(nickname)
        logger.info("nickname: $nickname allLanguages: $allLanguages")
        val totalSize = allLanguages.sumOf { it.second.size }.toFloat()
        val topLanguages = allLanguages.take(3)
        val safeNickname = escapeXml(nickname)

        val backgroundColor = if (theme == Theme.DARK) "#090b10" else "#f5f7fb"
        val cardColor = if (theme == Theme.DARK) "#11151e" else "#ffffff"
        val textColor = if (theme == Theme.DARK) "#f8fafc" else "#111827"
        val subtextColor = if (theme == Theme.DARK) "#9aa4b2" else "#6b7280"
        val borderColor = if (theme == Theme.DARK) "#272f3c" else "#e6eaf2"
        val trackColor = if (theme == Theme.DARK) "#1b2230" else "#eef2f8"

        val svgContent = """
    <svg width="360" height="220" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
        <style>
            @import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css');
            .small { font: 500 11px 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif; }
            .medium { font: 600 13px 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif; }
            .large { font: 700 17px 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif; }
            .background { fill: $backgroundColor; }
            .card { fill: $cardColor; stroke: $borderColor; stroke-width: 1; }
            .text { fill: $textColor; }
            .subtext { fill: $subtextColor; }
        </style>
        <a href="$LINK" target="_blank">
            <rect width="100%" height="100%" class="background"/>
            <rect x="12" y="12" width="336" height="196" rx="22" class="card"/>
            <text x="26" y="39" class="large text">Top Languages</text>
            <text x="26" y="58" class="small subtext">$safeNickname</text>
            <rect x="24" y="69" width="312" height="10" rx="5" fill="$trackColor"/>
        ${
            topLanguages.mapIndexed { index, (lang, sizeAndColor) ->
                val percentage = if (totalSize > 0f) {
                    (sizeAndColor.size / totalSize * 10000).roundToInt() / 100.0
                } else {
                    0.0
                }
                val xPos = 16 + index * 112
                val segmentX = 24 + topLanguages.take(index).sumOf { (_, value) ->
                    if (totalSize > 0f) value.size / totalSize * 312.0 else 0.0
                }
                val segmentWidth = if (totalSize > 0f) sizeAndColor.size / totalSize * 312.0 else 0.0
                val rank = index + 1
                val languageName = lang.value.lowercase()
                val safeLanguage = escapeXml(lang.value)
                val base64Image = encodeImageToBase64("static/images/${languageName}.svg")
                """
            <rect x="${"%.2f".format(Locale.US, segmentX)}" y="69" width="${"%.2f".format(Locale.US, segmentWidth)}" height="10" fill="${sizeAndColor.color}"/>
            <g transform="translate($xPos, 92)">
                <rect width="104" height="104" rx="14" class="card"/>
                <circle cx="52" cy="30" r="18" fill="${sizeAndColor.color}20"/>
                <image x="38" y="16" width="28" height="28" xlink:href="data:image/svg+xml;base64,$base64Image"/>
                <circle cx="86" cy="20" r="11" fill="$cardColor" stroke="$borderColor" stroke-width="1"/>
                <text x="86" y="24" text-anchor="middle" class="small text">$rank</text>
                <text x="52" y="64" class="medium text" text-anchor="middle">$safeLanguage</text>
                <text x="52" y="82" class="small subtext" text-anchor="middle">$percentage%</text>
            </g>
            """
            }.joinToString("")
        }
        </a>
    </svg>
    """.trimIndent()

        return svgContent
    }

    private fun getMostUsedLanguages(nickname: String): List<Pair<Language, SizeAndColor>> {
        val response = githubGraphQLClient.fetchUsedLanguages(nickname)
        return response.data.user.repositories.nodes
            .flatMap { repo -> repo.languages.edges }
            .groupBy(
                keySelector = { Language.from(it.node.name) },
                valueTransform = { SizeAndColor(it.size, it.node.color) }
            )
            .mapValues { (_, sizeAndColors) ->
                SizeAndColor(
                    size = sizeAndColors.sumOf { it.size },
                    color = sizeAndColors.first().color
                )
            }
            .toList()
            .sortedByDescending { (_, sizeAndColor) -> sizeAndColor.size }
    }

    private fun encodeImageToBase64(imagePath: String): String {
        val bytes = ClassPathResource(imagePath).inputStream.readBytes()
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    @Cacheable(Caches.SVG_TEXT, key = "#text + #theme")
    fun generateAnimatedSvg(text: String, theme: Theme): String {
        val safeText = escapeXml(text)
        val backgroundColor = if (theme == Theme.DARK) "#0b0e14" else "#f4f7fb"
        val cardColor = if (theme == Theme.DARK) "#111826" else "#ffffff"
        val borderColor = if (theme == Theme.DARK) "#273246" else "#e6edf8"
        val textBaseColor = if (theme == Theme.DARK) "#dbe5f5" else "#1f2937"
        val textGradientStart = if (theme == Theme.DARK) "#f8fbff" else "#0f172a"
        val textGradientMid = if (theme == Theme.DARK) "#9cc2ff" else "#2f80f7"
        val textGradientEnd = if (theme == Theme.DARK) "#b39bff" else "#6aa6ff"
        val subtitleColor = if (theme == Theme.DARK) "#93a1b6" else "#6b7280"
        val shadowColor = if (theme == Theme.DARK) "#00000066" else "#c4d0e455"

        return """
    <svg width="520" height="170" xmlns="http://www.w3.org/2000/svg">
        <defs>
            <linearGradient id="textGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" style="stop-color:$textGradientStart;stop-opacity:1"/>
                <stop offset="52%" style="stop-color:$textGradientMid;stop-opacity:1"/>
                <stop offset="100%" style="stop-color:$textGradientEnd;stop-opacity:1"/>
                <animateTransform attributeName="gradientTransform" type="translate" values="-0.18 0;0.18 0;-0.18 0" dur="5.5s" repeatCount="indefinite"/>
            </linearGradient>
            <linearGradient id="shineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" style="stop-color:#ffffff00;stop-opacity:0"/>
                <stop offset="50%" style="stop-color:#ffffff88;stop-opacity:1"/>
                <stop offset="100%" style="stop-color:#ffffff00;stop-opacity:0"/>
            </linearGradient>
            <filter id="cardShadow" x="-20%" y="-20%" width="140%" height="140%">
                <feDropShadow dx="0" dy="6" stdDeviation="10" flood-color="$shadowColor"/>
            </filter>
            <clipPath id="textClip">
                <text x="260" y="95" text-anchor="middle"
                      font-family="Pretendard, -apple-system, BlinkMacSystemFont, sans-serif"
                      font-weight="800" font-size="44" letter-spacing="-0.8">$safeText</text>
            </clipPath>
        </defs>
        <style>
            @import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css');
            .background { fill: $backgroundColor; }
            .card { fill: $cardColor; stroke: $borderColor; stroke-width: 1; }
            .text {
                font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif;
                font-weight: 800;
                font-size: 44px;
                letter-spacing: -0.8px;
            }
            .text-base { fill: $textBaseColor; }
            .text-gradient { fill: url(#textGradient); }
            .subtext {
                font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif;
                font-weight: 500;
                font-size: 12px;
                fill: $subtitleColor;
            }
        </style>
        <a href="$LINK" target="_blank">
            <rect width="100%" height="100%" class="background"/>
            <g filter="url(#cardShadow)">
                <rect x="16" y="16" width="488" height="138" rx="26" class="card"/>
            </g>
            <text x="260" y="95" text-anchor="middle" class="text text-base">
                $safeText
                <animate attributeName="opacity" from="0" to="1" dur="0.6s" fill="freeze"/>
                <animate attributeName="letter-spacing" values="-1.6px;-0.8px" dur="0.6s" fill="freeze"/>
            </text>
            <rect x="-180" y="42" width="180" height="68" fill="url(#shineGradient)" clip-path="url(#textClip)" opacity="${if (theme == Theme.DARK) "0.45" else "0.55"}">
                <animate attributeName="x" values="-220;560" dur="2.6s" begin="0.8s" repeatCount="indefinite"/>
            </rect>
            <text x="260" y="95" text-anchor="middle" class="text text-gradient" opacity="0.92">
                $safeText
                <animate attributeName="opacity" values="0.84;1;0.84" dur="3.4s" repeatCount="indefinite"/>
            </text>
        </a>
    </svg>
    """.trimIndent()
    }
}
