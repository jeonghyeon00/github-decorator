package com.jeonghyeon00.commit.graph.controller

import com.jeonghyeon00.commit.graph.service.SvgService
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
    fun generateSvg(@RequestParam githubId: String): String {
        return svgService.generateSvg(githubId)
    }
}
