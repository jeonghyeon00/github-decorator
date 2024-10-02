package com.jeonghyeon00.commit.graph

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CommitGraphApplication

fun main(args: Array<String>) {
    runApplication<CommitGraphApplication>(*args)
}
