package com.heixss.guitartuner.domain.model

enum class GuitarString(
    val displayName: String,
    val frequency: Float,
    val stringNumber: Int,
) {
    E4("E4", 329.63f, 1),
    B3("B3", 246.94f, 2),
    G3("G3", 196.00f, 3),
    D3("D3", 146.83f, 4),
    A2("A2", 110.00f, 5),
    E2("E2",  82.41f, 6),
}
