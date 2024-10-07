package com.jeonghyeon00.commit.graph.infrastructure.github

import com.jeonghyeon00.commit.graph.infrastructure.github.dto.response.FetchUsedLanguagesResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GithubGraphQLClient(
    @Value("\${github.api.token}")
    private val githubApiToken: String
) {
    private val restClient = RestClient.create()

    fun fetchUsedLanguages(githubId: String): FetchUsedLanguagesResponse {
        val query = """
            query User {
                user(login: "$githubId") {
                    name
                    repositories(isFork: false, first: 100, ownerAffiliations: OWNER, orderBy: { field: UPDATED_AT, direction: DESC }) {
                        nodes {
                            name
                            languages(first: 10, orderBy: { field: SIZE, direction: DESC }) {
                                edges {
                                    size
                                    node {
                                        color
                                        name
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """

        return restClient.post()
            .uri("https://api.github.com/graphql")
            .header("Authorization", "Bearer $githubApiToken")
            .body(mapOf("query" to query))
            .retrieve()
            .body(FetchUsedLanguagesResponse::class.java) ?: throw RuntimeException("Failed to fetch used languages")
    }
}
