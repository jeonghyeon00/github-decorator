package com.jeonghyeon00.commit.graph.config

import java.time.Duration

object Caches {
    const val SVG_LANGUAGE = "SVG_LANGUAGE"
    val SVG_LANGUAGE_TTL: Duration = Duration.ofHours(6)
    const val SVG_TEXT = "SVG_TEXT"
    val SVG_TEXT_TTL: Duration = Duration.ofHours(6)
}
