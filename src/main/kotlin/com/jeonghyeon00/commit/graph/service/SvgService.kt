package com.jeonghyeon00.commit.graph.service

import com.jeonghyeon00.commit.graph.infrastructure.github.GithubRestAPIClient
import com.jeonghyeon00.commit.graph.infrastructure.github.dto.response.SearchCommitResponse
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class SvgService(
    private val githubRestAPIClient: GithubRestAPIClient
) {
    fun generateSvg(githubId: String): String {
        val weekData = getCommitCountGroupByLocalDate(githubId).sortedBy { it.first }

        val svgWidth = 600
        val svgHeight = 400
        val padding = 100
        val barWidth = 60
        val barGap = 15

        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

        val maxCommits = weekData.maxOfOrNull { it.second } ?: 1
        val yScale = (svgHeight - 2 * padding).toDouble() / maxCommits

        val svg = StringBuilder()
        svg.append("""
        <svg width="$svgWidth" height="$svgHeight" xmlns="http://www.w3.org/2000/svg">
        <style>
            .bar { fill: #40c463; }
            .bar:hover { fill: #2ea44f; }
            .axis { stroke: #333; stroke-width: 2; }
            .label { font-family: Arial, sans-serif; font-size: 12px; }
            .title { font-family: Arial, sans-serif; font-size: 18px; font-weight: bold; }
        </style>
        <text x="${svgWidth / 2}" y="30" text-anchor="middle" class="title">$githubId's Commits in the Last Week</text>
    """.trimIndent())

        // Draw bars and labels
        weekData.forEachIndexed { index, (date, count) ->
            val x = padding + index * (barWidth + barGap)
            val barHeight = count * yScale
            val y = svgHeight - padding - barHeight

            svg.append("""
            <rect class="bar" x="$x" y="$y" width="$barWidth" height="$barHeight">
                <title>${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}: $count commit(s)</title>
            </rect>
            <text x="${x + barWidth / 2}" y="${svgHeight - padding + 20}" text-anchor="middle" class="label">
                ${date.format(dateFormatter)}
            </text>
            <text x="${x + barWidth / 2}" y="${y - 5}" text-anchor="middle" class="label">$count</text>
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

        return items.groupBy { it.commit.author.localDate }.map { it.key to it.value.count() }
    }
}
