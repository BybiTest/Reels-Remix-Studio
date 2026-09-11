package com.example.data.repository

import com.example.data.ai.ReelsAiPersona
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.remote.Content
import com.example.data.remote.GenerationConfig
import com.example.data.remote.GeminiApiClient
import com.example.data.remote.GeminiGenerateRequest
import com.example.data.remote.Part
import com.example.data.remote.SystemInstruction
import com.example.util.SecureKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException

class ChatRepository(
    private val chatDao: ChatDao,
    private val keyStorage: SecureKeyStorage
) {

    fun getAllConversations(): Flow<List<ConversationEntity>> =
        chatDao.getAllConversations()

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForConversation(conversationId)

    suspend fun createNewConversation(initialTitle: String = "سناریوی جدید ریلز"): ConversationEntity =
        withContext(Dispatchers.IO) {
            val conversation = ConversationEntity(title = initialTitle)
            chatDao.insertConversation(conversation)
            conversation
        }

    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        chatDao.deleteConversationById(id)
    }

    suspend fun updateConversationTitle(id: String, title: String) = withContext(Dispatchers.IO) {
        val existing = chatDao.getConversationById(id)
        if (existing != null) {
            chatDao.updateConversation(existing.copy(title = title, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("متن پیام نمی‌تواند خالی باشد"))
        }

        val apiKey = keyStorage.getActiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("کلید دسترسی هوش مصنوعی (API Key) تنظیم نشده است. لطفاً از طریق منوی تنظیمات کلید را وارد کنید.")
            )
        }

        // ۱. ذخیره پیام کاربر در دیتابیس
        val userMessage = ChatMessageEntity(
            conversationId = conversationId,
            role = "user",
            content = trimmed
        )
        chatDao.insertMessage(userMessage)

        // ۲. به‌روزرسانی تاریخچه مکالمه
        val existingConv = chatDao.getConversationById(conversationId)
        if (existingConv != null) {
            val updatedTitle = if (existingConv.title == "سناریوی جدید ریلز") {
                trimmed.take(40).let { if (trimmed.length > 40) "$it..." else it }
            } else {
                existingConv.title
            }
            chatDao.updateConversation(
                existingConv.copy(
                    title = updatedTitle,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // ۳. بازیابی تاریخچه کامل برای Multi-turn
        val history = chatDao.getMessagesForConversationOnce(conversationId)

        // ۴. ساخت ساختار درخواست Gemini
        val contentsList = history
            .filter { !it.isError }
            .map { msg ->
                Content(
                    role = if (msg.role == "user") "user" else "model",
                    parts = listOf(Part(text = msg.content))
                )
            }

        val request = GeminiGenerateRequest(
            contents = contentsList,
            systemInstruction = SystemInstruction(
                parts = listOf(Part(text = ReelsAiPersona.SYSTEM_INSTRUCTION))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                topP = 0.95f,
                maxOutputTokens = 2500
            )
        )

        try {
            val response = GeminiApiClient.service.generateContent(
                apiKey = apiKey,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val candidateText = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!candidateText.isNullOrBlank()) {
                    val modelMessage = ChatMessageEntity(
                        conversationId = conversationId,
                        role = "model",
                        content = candidateText
                    )
                    chatDao.insertMessage(modelMessage)
                    return@withContext Result.success(candidateText)
                } else {
                    val err = body?.error?.message ?: "پاسخی از مدل دریافت نشد"
                    recordError(conversationId, err)
                    return@withContext Result.failure(Exception(err))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val parsedError = parseHttpError(response.code(), errorBody)
                recordError(conversationId, parsedError)
                return@withContext Result.failure(Exception(parsedError))
            }
        } catch (e: IOException) {
            val netErr = "خطا در برقراری ارتباط با اینترنت. لطفاً اتصال شبکه خود را بررسی نمایید."
            recordError(conversationId, netErr)
            return@withContext Result.failure(Exception(netErr, e))
        } catch (e: Exception) {
            val genErr = e.localizedMessage ?: "خطای ناشناخته در ارتباط با هوش مصنوعی"
            recordError(conversationId, genErr)
            return@withContext Result.failure(Exception(genErr, e))
        }
    }

    private suspend fun recordError(conversationId: String, errorMessage: String) {
        val errorMsg = ChatMessageEntity(
            conversationId = conversationId,
            role = "model",
            content = "❌ متأسفانه در تولید سناریو مشکلی پیش آمد:\n$errorMessage",
            isError = true,
            errorMessage = errorMessage
        )
        chatDao.insertMessage(errorMsg)
    }

    private fun parseHttpError(code: Int, errorBody: String?): String {
        return when (code) {
            400 -> "درخواست نامعتبر است (کد ۴۰۰). لطفاً پیام کوتاه‌تر یا متفاوتی را امتحان کنید."
            401, 403 -> "کلید Gemini API نامعتبر است یا دسترسی به آن مسدود شده است (کد $code)."
            429 -> "محدودیت تعداد درخواست (Rate Limit). چند ثانیه بعد دوباره تلاش کنید."
            500, 503 -> "سرور هوش مصنوعی جمینای موقتاً با اختلال مواجه است (کد $code). لطفاً دقایقی دیگر امتحان کنید."
            else -> "خطای سرور: $code ${errorBody?.take(100) ?: ""}"
        }
    }
}
