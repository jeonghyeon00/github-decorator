package com.jeonghyeon00.commit.graph.infrastructure.github

import com.jeonghyeon00.commit.graph.infrastructure.github.dto.response.SearchCommitResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GithubRestAPIClient(
    @Value("\${github.api.token}")
    private val githubApiToken: String
) {
    private val restClient = RestClient.create()

    fun fetchSearchCommit(githubId: String, page: Int): SearchCommitResponse {
        return restClient.get()
            .uri("https://api.github.com/search/commits?q=author:${githubId}&order=desc&sort=author-date&page=$page&per_page=100")
            .header("X-Github-Api-Version", "2022-11-28")
            .header("Authorization", "Bearer $githubApiToken")
            .retrieve()
            .body(SearchCommitResponse::class.java) ?: throw RuntimeException("Failed to search commit")
    }
}
