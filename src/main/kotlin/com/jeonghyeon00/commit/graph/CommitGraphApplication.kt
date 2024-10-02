package com.jeonghyeon00.commit.graph

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CommitGraphApplication

fun main(args: Array<String>) {
    runApplication<CommitGraphApplication>(*args)
}
