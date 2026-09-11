package com.example.util

/**
 * پیکربندی زون‌های تبلیغاتی تپسل مپ (Tapsell Mediation)
 * - زون ویدیوی جایزه‌ای (AI Rewarded)
 * - زون بنر استاندارد همسان (Standard Banner)
 */
object TapsellAdConfig {

    /**
     * شناسه زون ویدیوی جایزه‌ای رسمی تپسل (AI Rewarded Video)
     */
    const val PROD_REWARDED_ZONE_ID: String = "6aa3f115796a202335abbbd4"

    /**
     * شناسه زون بنر استاندارد رسمی تپسل (Standard Banner)
     */
    const val PROD_BANNER_ZONE_ID: String = "6aa3f1616f11d73a0bac75c2"

    /**
     * تعداد اعتبارات اهدایی به ازای مشاهده کامل هر ویدیوی تبلیغاتی
     */
    const val REWARD_CREDITS_PER_AD: Int = 3

    fun getRewardedZoneId(): String {
        return PROD_REWARDED_ZONE_ID
    }

    fun getBannerZoneId(): String {
        return PROD_BANNER_ZONE_ID
    }
}
