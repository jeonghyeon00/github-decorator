package com.jeonghyeon00.commit.graph.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jeonghyeon00.commit.graph.domain.Theme
import com.jeonghyeon00.commit.graph.infrastructure.github.GithubGraphQLClient
import com.jeonghyeon00.commit.graph.infrastructure.github.dto.response.FetchUsedLanguagesResponse
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SvgServiceSmokeTest {

    private val outputDirectory = Path.of("build", "smoke-test-svg")

    @Test
    fun `generateMostUsedLanguagesSvg creates dark and light previews from real fixture`() {
        val githubClient = mock(GithubGraphQLClient::class.java)
        `when`(githubClient.fetchUsedLanguages("jeonghyeon00")).thenReturn(loadLanguageFixture())
        val svgService = SvgService(githubClient)

        Files.createDirectories(outputDirectory)
        Theme.entries.forEach { theme ->
            val suffix = theme.name.lowercase()
            val svg = svgService.generateMostUsedLanguagesSvg("jeonghyeon00", theme)
            val preview = outputDirectory.resolve("languages-$suffix.svg")

            Files.writeString(preview, svg)

            assertTrue(Files.size(preview) > 0)
            assertContains(svg, "<svg")
            assertContains(svg, "@jeonghyeon00")
            assertContains(svg, "Kotlin")
            assertContains(svg, "TypeScript")
            assertContains(svg, "Java")
            assertContains(svg, "38.01%")
        }
    }

    @Test
    fun `generateAnimatedSvg creates dark and light previews`() {
        val svgService = SvgService(mock(GithubGraphQLClient::class.java))

        Files.createDirectories(outputDirectory)
        Theme.entries.forEach { theme ->
            val suffix = theme.name.lowercase()
            val svg = svgService.generateAnimatedSvg("Build something beautiful", theme)
            val preview = outputDirectory.resolve("text-$suffix.svg")

            Files.writeString(preview, svg)

            assertTrue(Files.size(preview) > 0)
            assertContains(svg, "<svg")
            assertContains(svg, "Build something beautiful")
            assertEquals(3, "Build something beautiful".toRegex().findAll(svg).count())
        }
    }

    private fun loadLanguageFixture(): FetchUsedLanguagesResponse =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/jeonghyeon00-languages.json")) {
            "Missing GitHub language fixture"
        }.use(jacksonObjectMapper()::readValue)
}
