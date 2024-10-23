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

        val backgroundColor = if (theme == Theme.DARK) "#000000" else "#ffffff"
        val textColor = if (theme == Theme.DARK) "#f5f5f7" else "#1d1d1f"
        val subtextColor = if (theme == Theme.DARK) "#a1a1a6" else "#86868b"
        val strokeColor = if (theme == Theme.DARK) "#1d1d1f" else "#d2d2d7"

        val svgContent = """
    <svg width="330" height="211" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
        <style>
            @import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css');
            .small { font: 400 11px 'Pretendard', sans-serif; }
            .medium { font: 600 14px 'Pretendard', sans-serif; }
            .large { font: 600 18px 'Pretendard', sans-serif; }
            .background { fill: $backgroundColor; }
            .text { fill: $textColor; }
            .subtext { fill: $subtextColor; }
        </style>
        <a href="$LINK" target="_blank">
            <!-- Background -->
            <rect width="100%" height="100%" class="background"/>
            
            <!-- Title -->
            <text x="165" y="29" class="large text" text-anchor="middle">Top Languages for $nickname</text>
        
        ${
            topLanguages.mapIndexed { index, (lang, sizeAndColor) ->
                val percentage = ((sizeAndColor.size / totalSize * 100 * 100).roundToInt() / 100.0)
                val yPos = 112
                val xPos = 59 + index * 106
                val rank = index + 1
                val languageName = lang.value.lowercase()
                val base64Image = encodeImageToBase64("static/images/${languageName}.svg")
                val medalType = when (rank) {
                    1 -> "gold"
                    2 -> "silver"
                    3 -> "bronze"
                    else -> ""
                }
                val medalBase64 = encodeImageToBase64("static/images/${medalType}-medal.svg")
                """
            <!-- ${lang.value} -->
            <g transform="translate($xPos, $yPos)">
                <circle r="33" fill="${sizeAndColor.color}" stroke="$strokeColor" stroke-width="1"/>
                <image x="-21" y="-21" width="42" height="42" xlink:href="data:image/svg+xml;base64,$base64Image"/>
                <image x="-12" y="-61" width="24" height="24" xlink:href="data:image/svg+xml;base64,$medalBase64"/>
                <text y="61" class="small text" text-anchor="middle">${lang.value}</text>
                <text y="77" class="small subtext" text-anchor="middle">$percentage%</text>
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

    @Cacheable(Caches.SVG_TEXT, key = "#text + #theme")
    fun generateAnimatedSvg(text: String, theme: Theme): String {
        val backgroundColor = if (theme == Theme.DARK) "#000000" else "#ffffff"
        val accentColor = if (theme == Theme.DARK) "#5ac8fa" else "#007aff"
        val patternColor = if (theme == Theme.DARK) "#ffffff10" else "#00000008"
        val lineColor = if (theme == Theme.DARK) "#ffffff30" else "#00000015"

        return """
    <svg width="500" height="150" xmlns="http://www.w3.org/2000/svg">
        <defs>
            <pattern id="dotPattern" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
                <circle cx="2" cy="2" r="1" fill="$patternColor"/>
            </pattern>
            <linearGradient id="textGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:$accentColor;stop-opacity:1" />
                <stop offset="100%" style="stop-color:${if (theme == Theme.DARK) "#af52de" else "#5856d6"};stop-opacity:1" />
            </linearGradient>
        </defs>
        
        <style>
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@700&amp;display=swap');
            .background { fill: $backgroundColor; }
            .pattern { fill: url(#dotPattern); opacity: ${if (theme == Theme.DARK) "0.8" else "0.5"}; }
            .text {
                font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
                font-weight: 700;
                font-size: 40px;
                fill: url(#textGradient);
            }
            .decoration {
                fill: none;
                stroke: $lineColor;
                stroke-width: 1.5;
            }
            .accent {
                fill: $accentColor;
            }
        </style>
        <a href="$LINK" target="_blank">
            <rect width="100%" height="100%" class="background"/>
            <rect width="100%" height="100%" class="pattern"/>
            <!-- Decorative elements -->
            <path d="M0 30 Q250 0 500 30" class="decoration">
                <animate attributeName="d" from="M0 30 Q250 30 500 30" to="M0 30 Q250 0 500 30" dur="3s" repeatCount="indefinite" />
            </path>
            <path d="M0 120 Q250 150 500 120" class="decoration">
                <animate attributeName="d" from="M0 120 Q250 120 500 120" to="M0 120 Q250 150 500 120" dur="3s" repeatCount="indefinite" />
            </path>
            <text x="50%" y="50%" text-anchor="middle" dominant-baseline="middle" class="text">
                $text
                <animate attributeName="opacity" from="0" to="1" dur="1.5s" fill="freeze" />
        </text>
        </a>
    </svg>
    """.trimIndent()
    }
}
