package com.jeonghyeon00.commit.graph.controller

import com.jeonghyeon00.commit.graph.domain.Theme
import com.jeonghyeon00.commit.graph.service.SvgService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/svg")
class SvgController(
    private val svgService: SvgService
) {
    @GetMapping(produces = ["image/svg+xml"])
    fun generateSvg(
        @RequestParam githubId: String,
        @RequestParam theme: Theme?
    ): String {
        return svgService.generateSvg(githubId, theme)
    }

    @GetMapping("/languages", produces = ["image/svg+xml"])
    fun generateMostUsedLanguagesSvg(
        @RequestParam nickname: String,
        @RequestParam(defaultValue = "LIGHT") theme: Theme
    ): ResponseEntity<String> {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "max-age=21600").body(svgService.generateMostUsedLanguagesSvg(nickname, theme))
    }
}
