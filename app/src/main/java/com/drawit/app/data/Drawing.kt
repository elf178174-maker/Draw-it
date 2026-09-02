package com.drawit.app.data

/** A single finished drawing the user photographed. */
data class Drawing(
    val id: String,
    val title: String,
    val note: String,
    val createdAt: Long,
    val fileName: String
)
