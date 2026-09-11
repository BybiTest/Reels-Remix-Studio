package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.TapsellAdConfig
import com.example.util.findActivity
import ir.tapsell.mediation.Tapsell
import ir.tapsell.mediation.ad.AdStateListener
import ir.tapsell.mediation.ad.request.BannerSize
import ir.tapsell.mediation.ad.request.RequestResultListener
import ir.tapsell.mediation.ad.views.banner.BannerContainer

/**
 * بنر استاندارد تپسل (Tapsell Mediation Banner) برای کاربران نسخه رایگان
 * - فقط در صورت فعال نبودن اشتراک VIP نمایش داده می‌شود.
 * - در ساختار Compose با استفاده از AndroidView و BannerContainer رسمی تپسل لود می‌شود.
 * - چرخه حیات و آزادسازی حافظه به درستی مدیریت می‌شود.
 */
@Composable
fun TapsellBannerAdView(
    isVipActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (isVipActive) {
        // کاربران VIP تبلیغات مشاهده نمی‌کنند
        return
    }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() } ?: return

    var activeAdId by remember { mutableStateOf<String?>(null) }
    var isAdVisible by remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        val bannerContainer = BannerContainer(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val bannerZoneId = TapsellAdConfig.getBannerZoneId()

        Tapsell.requestBannerAd(
            bannerZoneId,
            BannerSize.BANNER_320_50,
            activity,
            object : RequestResultListener {
                override fun onSuccess(adId: String) {
                    activeAdId = adId
                    isAdVisible = true
                    Tapsell.showBannerAd(
                        adId,
                        bannerContainer,
                        activity,
                        object : AdStateListener.Banner {
                            override fun onAdImpression() {}
                            override fun onAdClicked() {}
                            override fun onAdFailed(message: String) {
                                isAdVisible = false
                            }
                        }
                    )
                }

                override fun onFailure(message: String) {
                    isAdVisible = false
                }
            }
        )

        onDispose {
            activeAdId?.let { adId ->
                try {
                    Tapsell.destroyBannerAd(adId)
                } catch (_: Exception) {
                }
            }
        }
    }

    if (isAdVisible) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                factory = { ctx ->
                    BannerContainer(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        activeAdId?.let { adId ->
                            Tapsell.showBannerAd(adId, this, activity, null)
                        }
                    }
                }
            )
        }
    }
}
