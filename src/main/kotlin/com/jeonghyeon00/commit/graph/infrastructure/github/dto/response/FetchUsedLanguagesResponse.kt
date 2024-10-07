package com.jeonghyeon00.commit.graph.infrastructure.github.dto.response

data class FetchUsedLanguagesResponse(
    val data: Data
)

data class Data(
    val user: User
)

data class User(
    val name: String,
    val repositories: Repositories
)

data class Repositories(
    val nodes: List<Node>
)

data class Node(
    val languages: Languages,
    val name: String
)

data class Languages(
    val edges: List<Edge>
)

data class Edge(
    val node: NodeX,
    val size: Int
)

data class NodeX(
    val color: String,
    val name: String
)
