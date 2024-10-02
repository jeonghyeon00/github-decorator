package com.jeonghyeon00.commit.graph.infrastructure.github

import com.jeonghyeon00.commit.graph.infrastructure.github.dto.response.SearchCommitResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GithubRestAPIClient {
    private val restClient = RestClient.create()

    fun searchCommit(githubId: String): SearchCommitResponse? {
        val searchCommitResponse = restClient.get()
            .uri("https://api.github.com/search/commits?q=author:${githubId}&order=desc&sort=author-date&page=0&per_page=100")
            .retrieve()
            .body(SearchCommitResponse::class.java)

        if (searchCommitResponse == null) {
            throw RuntimeException("Failed to search commit")
        }

        return searchCommitResponse
    }
}
