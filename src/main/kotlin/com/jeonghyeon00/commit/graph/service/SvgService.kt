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
        val topLanguagePercentage = if (totalSize > 0f) {
            (topLanguages.sumOf { it.second.size } / totalSize * 1000).roundToInt() / 10.0
        } else {
            0.0
        }

        val backgroundStart = if (theme == Theme.DARK) "#0b1020" else "#f8faff"
        val backgroundEnd = if (theme == Theme.DARK) "#11182b" else "#eef3ff"
        val cardColor = if (theme == Theme.DARK) "#111827e8" else "#ffffffeb"
        val textColor = if (theme == Theme.DARK) "#f8fafc" else "#172033"
        val subtextColor = if (theme == Theme.DARK) "#94a3b8" else "#64748b"
        val borderColor = if (theme == Theme.DARK) "#334155" else "#dce4f2"
        val trackColor = if (theme == Theme.DARK) "#253047" else "#e6ebf4"
        val rowColor = if (theme == Theme.DARK) "#ffffff06" else "#64748b08"
        val gridColor = if (theme == Theme.DARK) "#ffffff0a" else "#3341550a"
        val shadowColor = if (theme == Theme.DARK) "#02061780" else "#64748b30"

        val svgContent = """
    <svg width="420" height="260" viewBox="0 0 420 260" role="img" aria-label="Top languages for $safeNickname" xmlns="http://www.w3.org/2000/svg">
        <defs>
            <linearGradient id="languageBackground" x1="24" y1="10" x2="396" y2="250" gradientUnits="userSpaceOnUse">
                <stop stop-color="$backgroundStart"/>
                <stop offset="1" stop-color="$backgroundEnd"/>
            </linearGradient>
            <linearGradient id="languageAccent" x1="0" y1="0" x2="1" y2="1">
                <stop stop-color="#8b5cf6"/>
                <stop offset="1" stop-color="#22d3ee"/>
            </linearGradient>
            <radialGradient id="languageGlow" cx="0" cy="0" r="1" gradientTransform="translate(365 18) rotate(135) scale(180)">
                <stop stop-color="#8b5cf6" stop-opacity=".2"/>
                <stop offset="1" stop-color="#8b5cf6" stop-opacity="0"/>
            </radialGradient>
            <pattern id="languageGrid" width="20" height="20" patternUnits="userSpaceOnUse">
                <path d="M20 0H0V20" fill="none" stroke="$gridColor"/>
            </pattern>
            <clipPath id="distributionClip">
                <rect x="24" y="72" width="372" height="6" rx="3"/>
            </clipPath>
            <filter id="languageShadow" x="-20%" y="-20%" width="140%" height="150%">
                <feDropShadow dx="0" dy="8" stdDeviation="12" flood-color="$shadowColor"/>
            </filter>
        </defs>
        <style>
            text { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif; }
            .small { font-size: 11px; font-weight: 500; }
            .medium { font-size: 13px; font-weight: 650; }
            .large { font-size: 18px; font-weight: 700; letter-spacing: -.35px; }
            .label { font-size: 9px; font-weight: 700; letter-spacing: .8px; }
            .number { font-variant-numeric: tabular-nums; }
            .text { fill: $textColor; }
            .subtext { fill: $subtextColor; }
        </style>
        <a href="$LINK" target="_blank">
            <rect width="420" height="260" rx="28" fill="url(#languageBackground)"/>
            <rect width="420" height="260" rx="28" fill="url(#languageGrid)"/>
            <rect width="420" height="260" rx="28" fill="url(#languageGlow)"/>
            <g filter="url(#languageShadow)">
                <rect x="10" y="10" width="400" height="240" rx="22" fill="$cardColor" stroke="$borderColor"/>
            </g>
            <rect x="24" y="26" width="32" height="32" rx="10" fill="url(#languageAccent)"/>
            <path d="M34 48V42M40 48V36M46 48V32" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
            <text x="68" y="40" class="large text">Top Languages</text>
            <text x="68" y="55" class="small subtext">@$safeNickname · repository activity</text>
            <rect x="332" y="28" width="64" height="24" rx="12" fill="$rowColor" stroke="$borderColor"/>
            <circle cx="345" cy="40" r="3" fill="#22d3ee"/>
            <text x="353" y="43" class="label subtext">$topLanguagePercentage%</text>
            <rect x="24" y="72" width="372" height="6" rx="3" fill="$trackColor"/>
        ${
            topLanguages.mapIndexed { index, (lang, sizeAndColor) ->
                val percentage = if (totalSize > 0f) {
                    (sizeAndColor.size / totalSize * 10000).roundToInt() / 100.0
                } else {
                    0.0
                }
                val rowY = 91 + index * 50
                val segmentX = 24 + topLanguages.take(index).sumOf { (_, value) ->
                    if (totalSize > 0f) value.size / totalSize * 372.0 else 0.0
                }
                val segmentWidth = if (totalSize > 0f) sizeAndColor.size / totalSize * 372.0 else 0.0
                val barWidth = percentage / 100.0 * 278.0
                val rank = index + 1
                val languageName = lang.value.lowercase()
                val safeLanguage = escapeXml(lang.value)
                val base64Image = encodeImageToBase64("static/images/${languageName}.svg")
                """
            <rect x="${"%.2f".format(Locale.US, segmentX)}" y="72" width="${"%.2f".format(Locale.US, segmentWidth)}" height="6" fill="${sizeAndColor.color}" clip-path="url(#distributionClip)" opacity=".35">
                <animate attributeName="opacity" from=".35" to="1" begin="${index * 0.12}s" dur=".5s" fill="freeze"/>
            </rect>
            <g transform="translate(24, $rowY)">
                <rect width="372" height="42" rx="12" fill="$rowColor"/>
                <rect y="12" width="3" height="18" rx="1.5" fill="${sizeAndColor.color}"/>
                <circle cx="20" cy="21" r="11" fill="${sizeAndColor.color}22" stroke="${sizeAndColor.color}55"/>
                <text x="20" y="25" text-anchor="middle" class="small text">$rank</text>
                <rect x="40" y="7" width="28" height="28" rx="9" fill="${sizeAndColor.color}18"/>
                <image x="46" y="13" width="16" height="16" href="data:image/svg+xml;base64,$base64Image"/>
                <text x="78" y="18" class="medium text">$safeLanguage</text>
                <text x="356" y="18" text-anchor="end" class="small subtext number">$percentage%</text>
                <rect x="78" y="27" width="278" height="4" rx="2" fill="$trackColor"/>
                <rect x="78" y="27" width="${"%.2f".format(Locale.US, barWidth)}" height="4" rx="2" fill="${sizeAndColor.color}">
                    <animate attributeName="width" from="0" to="${"%.2f".format(Locale.US, barWidth)}" begin="${0.12 + index * 0.12}s" dur=".65s" fill="freeze"/>
                </rect>
            </g>
            """
            }.joinToString("")
        }
        ${
            if (topLanguages.isEmpty()) {
                """
            <g transform="translate(24, 96)">
                <rect width="372" height="126" rx="16" fill="$rowColor" stroke="$borderColor" stroke-dasharray="4 5"/>
                <circle cx="186" cy="47" r="20" fill="url(#languageAccent)" opacity=".14"/>
                <path d="M178 51l6-7 5 4 7-9" fill="none" stroke="$subtextColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <text x="186" y="82" text-anchor="middle" class="medium text">No language data yet</text>
                <text x="186" y="101" text-anchor="middle" class="small subtext">Public repository activity will appear here.</text>
            </g>
                """.trimIndent()
            } else {
                ""
            }
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
