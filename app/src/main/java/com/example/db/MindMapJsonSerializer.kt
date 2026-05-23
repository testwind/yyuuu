package com.example.db

import com.example.model.MindMapNode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object MindMapJsonSerializer {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(MindMapNode::class.java)

    /**
     * Convert MindMapNode to JSON String
     */
    fun toJson(node: MindMapNode): String {
        return adapter.toJson(node) ?: ""
    }

    /**
     * Parse MindMapNode from JSON String
     */
    fun fromJson(json: String): MindMapNode? {
        if (json.isBlank()) return null
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
