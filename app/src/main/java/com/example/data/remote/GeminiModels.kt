package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: SystemInstruction? = null,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class SystemInstruction(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "role") val role: String, // "user" or "model"
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.7f,
    @Json(name = "topP") val topP: Float? = 0.95f,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 2048
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?,
    @Json(name = "error") val error: GeminiError?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?,
    @Json(name = "finishReason") val finishReason: String?
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    @Json(name = "code") val code: Int?,
    @Json(name = "message") val message: String?,
    @Json(name = "status") val status: String?
)
