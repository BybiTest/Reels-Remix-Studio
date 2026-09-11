package com.example.util

import android.app.Activity
import android.content.Context
import ir.tapsell.mediation.Tapsell
import ir.tapsell.mediation.ad.AdStateListener
import ir.tapsell.mediation.ad.request.RequestResultListener
import ir.tapsell.mediation.ad.show.AdShowCompletionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * وضعیت چرخه حیات تبلیغ ویدیویی جایزه‌ای تپسل
 */
sealed class RewardedAdUiState {
    object Idle : RewardedAdUiState()
    object Loading : RewardedAdUiState()
    object Ready : RewardedAdUiState()
    object Showing : RewardedAdUiState()
    data class RewardGranted(val credits: Int) : RewardedAdUiState()
    data class Failed(val message: String) : RewardedAdUiState()
    object Closed : RewardedAdUiState()
}

/**
 * مدیر جامع تبلیغات تپسل مپ (Tapsell Mediation)
 * - بارگذاری، مدیریت وضعیت و نمایش ویدیو جایزه‌ای
 * - پیشگیری قاطع از پاداش تکراری (Duplicate Reward Prevention) با استفاده از مکانیسم اتمیک
 * - حفاظت در برابر کلیک‌های پشت‌سر‌هم (Debouncing / Concurrency Guard)
 * - عدم نشت حافظه (بدون نگهداری دائمی رفرنس Activity)
 */
class TapsellManager(
    private val context: Context
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _adUiState = MutableStateFlow<RewardedAdUiState>(RewardedAdUiState.Idle)
    val adUiState: StateFlow<RewardedAdUiState> = _adUiState.asStateFlow()

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // شناسه تبلیغ لود شده از تپسل
    @Volatile
    private var currentLoadedAdId: String? = null

    // محافظت اتمیک در برابر کلیک مکرر یا درخواست‌های همزمان
    private val isOperationInProgress = AtomicBoolean(false)

    // پرچم اتمیک برای اطمینان از اعمال دقیقاً یک پاداش در هر نوبت پخش
    private val rewardConsumedForCurrentAd = AtomicBoolean(false)

    init {
        // پیش‌بارگذاری اولیه تبلیغ در پس‌زمینه
        loadRewardedAd()
    }

    /**
     * درخواست بارگذاری تبلیغ ویدیویی جایزه‌ای تپسل
     */
    fun loadRewardedAd(forceReload: Boolean = false) {
        if (!forceReload && currentLoadedAdId != null) {
            _isAdReady.value = true
            _isLoading.value = false
            return
        }

        if (_isLoading.value) {
            return
        }

        _isLoading.value = true
        _adUiState.value = RewardedAdUiState.Loading

        val zoneId = TapsellAdConfig.getRewardedZoneId()

        Tapsell.requestRewardedAd(
            zoneId,
            object : RequestResultListener {
                override fun onSuccess(adId: String) {
                    currentLoadedAdId = adId
                    _isLoading.value = false
                    _isAdReady.value = true
                    _adUiState.value = RewardedAdUiState.Ready
                }

                override fun onFailure(message: String) {
                    currentLoadedAdId = null
                    _isLoading.value = false
                    _isAdReady.value = false
                    val friendlyError = mapTapsellError(message)
                    _adUiState.value = RewardedAdUiState.Failed(friendlyError)
                }
            }
        )
    }

    /**
     * نمایش تبلیغ ویدیویی جایزه‌ای با کنترل کامل ایمنی و جلوگیری از پاداش تکراری
     *
     * @param activity اکتیویتی جاری برای نمایش ویدیو
     * @param onRewardEarned کالبک اهدای پاداش (فقط و فقط پس از تایید قطعی SDK تپسل اجرا می‌شود)
     * @param onAdClosed کالبک بسته شدن پنجره تبلیغ
     * @param onError کالبک بروز خطا در نمایش یا عدم آمادگی تبلیغ
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (credits: Int) -> Unit,
        onAdClosed: () -> Unit = {},
        onError: (message: String) -> Unit = {}
    ) {
        // جلوگیری از کلیک‌های سریع و پشت سر هم
        if (!isOperationInProgress.compareAndSet(false, true)) {
            return
        }

        val adId = currentLoadedAdId

        if (adId == null) {
            isOperationInProgress.set(false)
            val msg = "تبلیغ ویدیویی هنوز آماده نیست. در حال بارگذاری مجدد..."
            _adUiState.value = RewardedAdUiState.Failed(msg)
            onError(msg)
            loadRewardedAd(forceReload = true)
            return
        }

        // شناسه را مصرف می‌کنیم تا مجدداً استفاده نشود
        currentLoadedAdId = null
        _isAdReady.value = false
        _adUiState.value = RewardedAdUiState.Showing
        rewardConsumedForCurrentAd.set(false)

        Tapsell.showRewardedAd(
            adId,
            activity,
            object : AdStateListener.Rewarded {
                override fun onRewarded() {
                    // اطمینان اتمیک از اینکه پاداش فقط یک بار به ازای هر تبلیغ اعمال شود
                    if (rewardConsumedForCurrentAd.compareAndSet(false, true)) {
                        scope.launch {
                            val rewardAmount = TapsellAdConfig.REWARD_CREDITS_PER_AD
                            _adUiState.value = RewardedAdUiState.RewardGranted(rewardAmount)
                            onRewardEarned(rewardAmount)
                        }
                    }
                }

                override fun onAdClosed(completionState: AdShowCompletionState) {
                    isOperationInProgress.set(false)
                    scope.launch {
                        _adUiState.value = RewardedAdUiState.Closed
                        onAdClosed()
                        // آماده‌سازی خودکار تبلیغ بعدی برای استفاده مجدد کاربر
                        loadRewardedAd()
                    }
                }

                override fun onAdImpression() {
                    scope.launch {
                        _adUiState.value = RewardedAdUiState.Showing
                    }
                }

                override fun onAdClicked() {
                    // کاربر روی تبلیغ کلیک کرد
                }

                override fun onAdFailed(message: String) {
                    isOperationInProgress.set(false)
                    scope.launch {
                        val friendlyError = mapTapsellError(message)
                        _adUiState.value = RewardedAdUiState.Failed(friendlyError)
                        onError(friendlyError)
                        // بارگذاری مجدد پس از خطا
                        loadRewardedAd()
                    }
                }
            }
        )
    }

    private fun mapTapsellError(raw: String?): String {
        val err = raw.orEmpty().lowercase()
        return when {
            err.contains("no ad") || err.contains("fill") -> "در حال حاضر تبلیغ جدیدی موجود نیست. لطفاً چند دقیقه دیگر امتحان فرمایید."
            err.contains("network") || err.contains("timeout") || err.contains("connection") -> "عدم اتصال به اینترنت یا کندی ارتباط شبکه برای بارگذاری تبلیغ."
            err.contains("invalid") || err.contains("not found") -> "خطا در تنظیمات زون تبلیغاتی تپسل."
            else -> "بارگذاری تبلیغ انجام نشد ($raw). لطفاً مجدداً امتحان نمایید."
        }
    }
}
