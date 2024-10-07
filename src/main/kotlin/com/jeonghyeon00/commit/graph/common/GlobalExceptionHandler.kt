package com.jeonghyeon00.commit.graph.common

import com.jeonghyeon00.commit.graph.common.exception.NotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun handleRuntimeException(e: NotFoundException): ResponseEntity<Unit> {
        return ResponseEntity.notFound().build()
    }
}
