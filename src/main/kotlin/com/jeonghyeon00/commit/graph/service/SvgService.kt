package com.jeonghyeon00.commit.graph.service

import com.jeonghyeon00.commit.graph.domain.Language
import com.jeonghyeon00.commit.graph.domain.SizeAndColor
import com.jeonghyeon00.commit.graph.domain.Theme
import com.jeonghyeon00.commit.graph.domain.ThemeColors
import com.jeonghyeon00.commit.graph.infrastructure.github.GithubGraphQLClient
import com.jeonghyeon00.commit.graph.infrastructure.github.GithubRestAPIClient
import com.jeonghyeon00.commit.graph.infrastructure.github.dto.response.SearchCommitResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.round

@Service
class SvgService(
    private val githubRestAPIClient: GithubRestAPIClient,
    private val githubGraphQLClient: GithubGraphQLClient
) {
    @Cacheable("svg", key = "#githubId + #theme")
    fun generateSvg(githubId: String, theme: Theme?): String {
        val weekData = getCommitCountGroupByLocalDate(githubId)
        val selectedTheme = theme ?: Theme.DARK

        val svgWidth = 800
        val svgHeight = 300
        val padding = 60
        val barWidth = 80
        val barGap = 20

        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

        val maxCommits = weekData.maxOfOrNull { it.second } ?: 1
        val yScale = (svgHeight - 2 * padding).toDouble() / maxCommits

        val colors = ThemeColors.getThemeColorByTheme(selectedTheme)

        val svg = StringBuilder()
        svg.append(
            """
    <svg width="$svgWidth" height="$svgHeight" xmlns="http://www.w3.org/2000/svg">
    <style>
        .bar { fill: ${colors.barFill}; }
        .bar:hover { fill: ${colors.barHover}; }
        .axis { stroke: ${colors.axisColor}; stroke-width: 1; }
        .grid { stroke: ${colors.gridColor}; stroke-width: 1; }
        .label { font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif,"Apple Color Emoji","Segoe UI Emoji"; font-size: 12px; fill: ${colors.labelColor}; }
        .title { font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif,"Apple Color Emoji","Segoe UI Emoji"; font-size: 18px; font-weight: bold; fill: ${colors.titleColor}; }
        .tick { font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif,"Apple Color Emoji","Segoe UI Emoji"; font-size: 12px; fill: ${colors.tickColor}; }
    </style>
    <rect width="$svgWidth" height="$svgHeight" fill="${colors.background}" />
    <text x="${svgWidth / 2}" y="30" text-anchor="middle" class="title">$githubId's Commits in the Last Week</text>
    """.trimIndent()
        )

        // Draw y-axis grid and labels
        val yTickCount = 5
        for (i in 0..yTickCount) {
            val y = svgHeight - padding - (i * (svgHeight - 2 * padding) / yTickCount)
            val tickValue = round(i * maxCommits.toDouble() / yTickCount).toInt()
            svg.append(
                """
        <line x1="$padding" y1="$y" x2="${svgWidth - padding}" y2="$y" class="grid" />
        <text x="${padding - 10}" y="${y + 5}" text-anchor="end" class="tick">$tickValue</text>
        """.trimIndent()
            )
        }

        // Draw bars and labels
        weekData.forEachIndexed { index, (date, count) ->
            val x = padding + index * (barWidth + barGap)
            val barHeight = count * yScale
            val y = svgHeight - padding - barHeight

            svg.append(
                """
        <rect class="bar" x="$x" y="$y" width="$barWidth" height="$barHeight" rx="4" ry="4">
            <title>${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}: $count commit(s)</title>
        </rect>
        <text x="${x + barWidth / 2}" y="${svgHeight - padding + 20}" text-anchor="middle" class="label">
            ${date.format(dateFormatter)}
        </text>
        <text x="${x + barWidth / 2}" y="${y - 10}" text-anchor="middle" class="label" fill="${colors.countColor}">$count</text>
        """.trimIndent()
            )
        }

        // Draw axes
        svg.append(
            """
    <line x1="$padding" y1="${svgHeight - padding}" x2="${svgWidth - padding}" y2="${svgHeight - padding}" class="axis" />
    <line x1="$padding" y1="$padding" x2="$padding" y2="${svgHeight - padding}" class="axis" />
    """.trimIndent()
        )

        svg.append("</svg>")
        return svg.toString()
    }

    private fun getCommitCountGroupByLocalDate(githubId: String): List<Pair<LocalDate, Int>> {
        var page = 0
        val items = mutableListOf<SearchCommitResponse.Item>()
        val oneWeekAgo = LocalDate.now().minusWeeks(1)

        while (page <= 9) {
            val response = githubRestAPIClient.fetchSearchCommit(githubId, page)
            if (response.items.isEmpty()) break

            items.addAll(response.items)

            if (response.items.last().commit.author.localDate.isBefore(oneWeekAgo)) break

            page++
        }

        val commitMap = items
            .filter { it.commit.author.localDate.isAfter(oneWeekAgo) || it.commit.author.localDate.isEqual(oneWeekAgo) }
            .groupBy { it.commit.author.localDate }
            .mapValues { it.value.count() }

        val last7Days = (0..6).map { LocalDate.now().minusDays(it.toLong()) }.reversed()

        return last7Days.map { date ->
            date to (commitMap[date] ?: 0)
        }
    }

    @Cacheable("svgLanguage", key = "#githubId + #theme")
    fun generateMostUsedLanguagesSvg(githubId: String, theme: Theme): String {
        val allLanguages = getMostUsedLanguages(githubId)
        val totalSize = allLanguages.sumOf { it.second.size }.toFloat()
        val topLanguages = allLanguages.take(3)

        val backgroundColor = if (theme == Theme.DARK) "#000000" else "#ffffff"
        val textColor = if (theme == Theme.DARK) "#f5f5f7" else "#1d1d1f"
        val subtextColor = if (theme == Theme.DARK) "#a1a1a6" else "#86868b"
        val strokeColor = if (theme == Theme.DARK) "#1d1d1f" else "#d2d2d7"

        val svgContent = """
    <svg width="300" height="200" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
        <style>
            @import url('https://fonts.googleapis.com/css2?family=SF+Pro+Display:wght@400;600&amp;display=swap');
            .small { font: 400 10px 'SF Pro Display', sans-serif; }
            .medium { font: 600 14px 'SF Pro Display', sans-serif; }
            .large { font: 600 16px 'SF Pro Display', sans-serif; }
            .background { fill: $backgroundColor; }
            .text { fill: $textColor; }
            .subtext { fill: $subtextColor; }
        </style>
        
        <!-- Background -->
        <rect width="100%" height="100%" class="background"/>
        
        <!-- Title -->
        <text x="150" y="30" class="large text" text-anchor="middle">Top Languages for $githubId</text>
        
        ${
            topLanguages.mapIndexed { index, (lang, sizeAndColor) ->
                val percentage = (sizeAndColor.size / totalSize * 100).toInt()
                val yPos = 105
                val xPos = 55 + index * 95
                val rank = index + 1
                val languageName = lang.value.lowercase()
                val base64Image = encodeImageToBase64("static/images/${languageName}.svg")
                """
            <!-- ${lang.value} -->
            <g transform="translate($xPos, $yPos)">
                <circle r="32" fill="${sizeAndColor.color}" stroke="$strokeColor" stroke-width="1"/>
                <image x="-20" y="-20" width="40" height="40" xlink:href="data:image/svg+xml;base64,$base64Image"/>
                <text y="-45" class="medium text" text-anchor="middle">#$rank</text>
                <text y="60" class="medium text" text-anchor="middle">${lang.value}</text>
                <text y="75" class="medium subtext" text-anchor="middle">$percentage%</text>
            </g>
            """
            }.joinToString("")
        }
    </svg>
    """.trimIndent()

        return svgContent
    }

    private fun getMostUsedLanguages(githubId: String): List<Pair<Language, SizeAndColor>> {
        val response = githubGraphQLClient.fetchUsedLanguages(githubId)
        return response.data.user.repositories.nodes
            .flatMap { it.languages.edges }
            .groupBy { it.node.name }
            .mapKeys { (name, _) ->
                Language.from(name)
            }
            .mapValues { (_, edges) -> SizeAndColor(edges.sumOf { it.size }, color = edges.first().node.color) }
            .filter { it.key != Language.OTHERS }
            .toList()
            .sortedByDescending { it.second.size }
    }

    private fun encodeImageToBase64(imagePath: String): String {
        val bytes = ClassPathResource(imagePath).inputStream.readBytes()
        return Base64.getEncoder().encodeToString(bytes)
    }
}
