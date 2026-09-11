package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.SubscriptionStatus
import com.example.data.local.VipPaymentRequestEntity
import com.example.data.repository.ChatRepository
import com.example.data.repository.VipRepository
import com.example.util.NetworkMonitor
import com.example.util.SecureKeyStorage
import com.example.util.TapsellAdConfig
import com.example.util.TapsellManager
import com.example.util.RewardedAdUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    data class RewardGranted(val credits: Int, val newTotal: Int) : UiEvent()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getInstance(context)
    val keyStorage = SecureKeyStorage(context)
    val networkMonitor = NetworkMonitor(context)

    private val chatRepository = ChatRepository(database.chatDao(), keyStorage)
    val vipRepository = VipRepository(database.vipPaymentDao())

    // مدیر تبلیغات رسمی تپسل
    val tapsellManager = TapsellManager(context)

    // جریان رویدادهای یک‌باره UI
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    // وضعیت اشتراک VIP کاربر
    val subscriptionStatus: StateFlow<SubscriptionStatus> = vipRepository
        .observeSubscriptionStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubscriptionStatus.FREE
        )

    val activeVipRequest: StateFlow<VipPaymentRequestEntity?> = vipRepository
        .observeActiveVipRequest()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isVipActive: StateFlow<Boolean> = vipRepository
        .observeSubscriptionStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        ).let { flow ->
            MutableStateFlow(false).apply {
                viewModelScope.launch {
                    flow.collect { status ->
                        value = (status == SubscriptionStatus.VIP_ACTIVE)
                    }
                }
            }
        }

    // فهرست مکالمات ذخیره شده
    val conversations: StateFlow<List<ConversationEntity>> = chatRepository
        .getAllConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // شناسه مکالمه جاری انتخاب شده
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    // پیام‌های مکالمه جاری
    private val _currentMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentMessages.asStateFlow()

    // وضعیت بارگذاری هوش مصنوعی
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // سیستم اعتبارات هوش مصنوعی (برای کاربران عادی)
    private val prefs = context.getSharedPreferences("ai_credits_prefs", Application.MODE_PRIVATE)
    private val _credits = MutableStateFlow(prefs.getInt("available_credits", 10))
    val credits: StateFlow<Int> = _credits.asStateFlow()

    // وضعیت تبلیغات ویدیویی جایزه‌ای تپسل
    val isAdReady: StateFlow<Boolean> = tapsellManager.isAdReady
    val isAdLoading: StateFlow<Boolean> = tapsellManager.isLoading
    val adUiState: StateFlow<RewardedAdUiState> = tapsellManager.adUiState

    init {
        viewModelScope.launch {
            conversations.collect { convList ->
                if (_currentConversationId.value == null && convList.isNotEmpty()) {
                    selectConversation(convList.first().id)
                } else if (convList.isEmpty()) {
                    createNewConversation("سناریوی اول ریلز")
                }
            }
        }
    }

    fun selectConversation(conversationId: String) {
        _currentConversationId.value = conversationId
        viewModelScope.launch {
            chatRepository.getMessagesForConversation(conversationId).collect { msgs ->
                _currentMessages.value = msgs
            }
        }
    }

    fun createNewConversation(title: String = "سناریوی جدید ریلز") {
        viewModelScope.launch {
            val newConv = chatRepository.createNewConversation(title)
            selectConversation(newConv.id)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (_currentConversationId.value == id) {
                _currentConversationId.value = null
                _currentMessages.value = emptyList()
            }
        }
    }

    fun sendPrompt(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isEmpty()) return

        // بررسی اعتبار کاربر در صورت فعال نبودن VIP
        val isVip = (subscriptionStatus.value == SubscriptionStatus.VIP_ACTIVE)
        if (!isVip) {
            if (_credits.value <= 0) {
                viewModelScope.launch {
                    _uiEvents.emit(
                        UiEvent.ShowError(
                            "اعتبار هوش مصنوعی شما به پایان رسیده است. لطفاً یک ویدیوی تبلیغاتی تپسل تماشا کنید یا اشتراک VIP تهیه نمایید."
                        )
                    )
                }
                return
            }
        }

        viewModelScope.launch {
            var activeConvId = _currentConversationId.value
            if (activeConvId == null) {
                val newConv = chatRepository.createNewConversation("سناریوی ریلز")
                activeConvId = newConv.id
                _currentConversationId.value = activeConvId
            }

            _isGenerating.value = true

            val result = chatRepository.sendMessage(activeConvId, trimmed)

            _isGenerating.value = false

            if (result.isSuccess) {
                // کسر یک اعتبار در صورت کاربر عادی بودن
                if (!isVip) {
                    consumeCredit(1)
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "خطا در برقراری ارتباط با جمینای"
                _uiEvents.emit(UiEvent.ShowError(err))
            }
        }
    }

    /**
     * نمایش ویدیوی جایزه‌ای تپسل جهت دریافت ۳ اعتبار رایگان
     * تنها پس از اتمام موفق و تایید قطعی SDK تپسل اعتبار افزوده می‌شود.
     */
    fun showTapsellRewardedAd(activity: Activity) {
        tapsellManager.showRewardedAd(
            activity = activity,
            onRewardEarned = { rewardedCredits ->
                addCredits(rewardedCredits)
                viewModelScope.launch {
                    _uiEvents.emit(
                        UiEvent.RewardGranted(
                            credits = rewardedCredits,
                            newTotal = _credits.value
                        )
                    )
                }
            },
            onAdClosed = {
                // پنجره تبلیغ تپسل بسته شد
            },
            onError = { errorMsg ->
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowError(errorMsg))
                }
            }
        )
    }

    fun loadRewardedAd() {
        tapsellManager.loadRewardedAd()
    }

    private fun addCredits(amount: Int) {
        val updated = _credits.value + amount
        _credits.value = updated
        prefs.edit().putInt("available_credits", updated).apply()
    }

    private fun consumeCredit(amount: Int) {
        val updated = (_credits.value - amount).coerceAtLeast(0)
        _credits.value = updated
        prefs.edit().putInt("available_credits", updated).apply()
    }

    fun saveCustomApiKey(key: String) {
        keyStorage.saveCustomApiKey(key)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("کلید اختصاصی جمینای با موفقیت رمزنگاری و ذخیره شد."))
        }
    }

    fun clearCustomApiKey() {
        keyStorage.clearCustomApiKey()
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("کلید اختصاصی پاک شد."))
        }
    }
}
