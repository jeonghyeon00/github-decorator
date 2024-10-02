package com.jeonghyeon00.commit.graph.domain

data class ThemeColors(
    val background: String,
    val barFill: String,
    val barHover: String,
    val axisColor: String,
    val gridColor: String,
    val labelColor: String,
    val titleColor: String,
    val tickColor: String,
    val countColor: String
) {
    companion object {
        fun getThemeColorByTheme(theme: Theme): ThemeColors {
            return when (theme) {
                Theme.LIGHT -> ThemeColors(
                    background = "#ffffff",
                    barFill = "#40c463",
                    barHover = "#2ea44f",
                    axisColor = "#24292e",
                    gridColor = "#e1e4e8",
                    labelColor = "#24292e",
                    titleColor = "#24292e",
                    tickColor = "#24292e",
                    countColor = "#24292e"
                )

                Theme.DARK -> ThemeColors(
                    background = "#0d1117",
                    barFill = "#40c463",
                    barHover = "#2ea44f",
                    axisColor = "#8b949e",
                    gridColor = "#30363d",
                    labelColor = "#8b949e",
                    titleColor = "#c9d1d9",
                    tickColor = "#8b949e",
                    countColor = "#c9d1d9"
                )
            }
        }

    }
}
