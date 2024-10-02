package com.jeonghyeon00.commit.graph.service

import com.jeonghyeon00.commit.graph.infrastructure.github.GithubRestAPIClient
import com.jeonghyeon00.commit.graph.infrastructure.github.dto.response.SearchCommitResponse
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.round

@Service
class SvgService(
    private val githubRestAPIClient: GithubRestAPIClient
) {
    fun generateSvg(githubId: String): String {
        val weekData = getCommitCountGroupByLocalDate(githubId)

        val svgWidth = 800
        val svgHeight = 300
        val padding = 60
        val barWidth = 80
        val barGap = 20

        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

        val maxCommits = weekData.maxOfOrNull { it.second } ?: 1
        val yScale = (svgHeight - 2 * padding).toDouble() / maxCommits

        val svg = StringBuilder()
        svg.append("""
    <svg width="$svgWidth" height="$svgHeight" xmlns="http://www.w3.org/2000/svg">
    <style>
        .bar { fill: #40c463; }
        .bar:hover { fill: #2ea44f; }
        .axis { stroke: #8b949e; stroke-width: 1; }
        .grid { stroke: #30363d; stroke-width: 1; }
        .label { font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif,"Apple Color Emoji","Segoe UI Emoji"; font-size: 12px; fill: #8b949e; }
        .title { font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif,"Apple Color Emoji","Segoe UI Emoji"; font-size: 18px; font-weight: bold; fill: #c9d1d9; }
        .tick { font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif,"Apple Color Emoji","Segoe UI Emoji"; font-size: 12px; fill: #8b949e; }
    </style>
    <rect width="$svgWidth" height="$svgHeight" fill="#0d1117" />
    <text x="${svgWidth / 2}" y="30" text-anchor="middle" class="title">$githubId's Commits in the Last Week</text>
    """.trimIndent())

        // Draw y-axis grid and labels
        val yTickCount = 5
        for (i in 0..yTickCount) {
            val y = svgHeight - padding - (i * (svgHeight - 2 * padding) / yTickCount)
            val tickValue = round(i * maxCommits.toDouble() / yTickCount).toInt()
            svg.append("""
        <line x1="$padding" y1="$y" x2="${svgWidth - padding}" y2="$y" class="grid" />
        <text x="${padding - 10}" y="${y + 5}" text-anchor="end" class="tick">$tickValue</text>
        """.trimIndent())
        }

        // Draw bars and labels
        weekData.forEachIndexed { index, (date, count) ->
            val x = padding + index * (barWidth + barGap)
            val barHeight = count * yScale
            val y = svgHeight - padding - barHeight

            svg.append("""
        <rect class="bar" x="$x" y="$y" width="$barWidth" height="$barHeight" rx="4" ry="4">
            <title>${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}: $count commit(s)</title>
        </rect>
        <text x="${x + barWidth / 2}" y="${svgHeight - padding + 20}" text-anchor="middle" class="label">
            ${date.format(dateFormatter)}
        </text>
        <text x="${x + barWidth / 2}" y="${y - 10}" text-anchor="middle" class="label" fill="#c9d1d9">$count</text>
        """.trimIndent())
        }

        // Draw axes
        svg.append("""
    <line x1="$padding" y1="${svgHeight - padding}" x2="${svgWidth - padding}" y2="${svgHeight - padding}" class="axis" />
    <line x1="$padding" y1="$padding" x2="$padding" y2="${svgHeight - padding}" class="axis" />
    """.trimIndent())

        svg.append("</svg>")
        return svg.toString()
    }

    fun getCommitCountGroupByLocalDate(githubId: String): List<Pair<LocalDate, Int>> {
        var page = 0
        val items = mutableListOf<SearchCommitResponse.Item>()
        do {
            page++
            items.addAll(githubRestAPIClient.fetchSearchCommit(githubId, page).items)
        } while (items.last().commit.author.localDate.isBefore(LocalDate.now().minusWeeks(1)))

        val commitMap = items.groupBy { it.commit.author.localDate }.mapValues { it.value.count() }

        val last7Days = (0..6).map { LocalDate.now().minusDays(it.toLong()) }.reversed()

        // add 0 for days without commits
        return last7Days.map { date ->
            date to (commitMap[date] ?: 0)
        }
    }
}
