package com.jeonghyeon00.commit.graph.domain

enum class Language(val value: String) {
    KOTLIN("Kotlin"),
    JAVA("Java"),
    JAVASCRIPT("JavaScript"),
    TYPESCRIPT("TypeScript"),
    C_PLUS_PLUS("C++"),
    C_SHARP("C#"),
    C("C"),
    SWIFT("Swift"),
    GO("Go"),
    RUST("Rust"),
    PYTHON("Python"),
    RUBY("Ruby"),
    SHELL("Shell"),
    OTHERS("Others")
    ;

    companion object {
        fun from(value: String): Language {
            return entries.firstOrNull() { it.value == value } ?: OTHERS
        }
    }
}
