package com.jeonghyeon00.commit.graph.infrastructure.github.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime

data class SearchCommitResponse(
    @JsonProperty("total_count")
    val totalCount: Long,
    val items: List<Item>,
) {
    data class Item(
        val commit: Commit
    ) {
        data class Commit(
            val author: Author
        ) {
            data class Author(
                val date: ZonedDateTime,
                val name: String,
                val email: String
            )
        }
    }
}
