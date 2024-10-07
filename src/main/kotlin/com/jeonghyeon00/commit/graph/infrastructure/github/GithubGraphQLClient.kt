package com.jeonghyeon00.commit.graph.infrastructure.github

import com.jeonghyeon00.commit.graph.common.exception.NotFoundException
import com.jeonghyeon00.commit.graph.infrastructure.github.dto.response.FetchUsedLanguagesResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class GithubGraphQLClient(
    @Value("\${github.api.token}")
    private val githubApiToken: String
) {
    private val restClient = RestClient.create()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun fetchUsedLanguages(nickname: String): FetchUsedLanguagesResponse {
        val query = """
            query User {
                user(login: "$nickname") {
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
        return try {
            restClient.post()
                .uri("https://api.github.com/graphql")
                .header("Authorization", "Bearer $githubApiToken")
                .body(mapOf("query" to query))
                .retrieve()
                .body(FetchUsedLanguagesResponse::class.java)
                ?: throw RuntimeException("Failed to fetch used languages")
        } catch (e: RestClientException) {
            if (e.message == "Error while extracting response for type [com.jeonghyeon00.commit.graph.infrastructure.github.dto.response.FetchUsedLanguagesResponse] and content type [application/json;charset=utf-8]") {
                throw NotFoundException("Github User")
            } else {
                logger.error("Failed to fetch used languages", e)
                throw e
            }
        }
    }
}
