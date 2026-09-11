package com.example.util

import android.content.Context
import android.content.SharedPreferences

/**
 * PaymentConfig: تنظیمات شماره حساب، کارت بانکی و تعرفه‌های اشتراک VIP.
 */
object PaymentConfig {

    // =========================================================================
    // اطلاعات کارت و حساب بانکی پیش‌فرض
    // =========================================================================
    /** نام صاحب حساب بانکی پیش‌فرض */
    const val DEFAULT_ACCOUNT_HOLDER_NAME = "داوود عزیزی"

    /** شماره شبا بانکی پیش‌فرض (با پیشوند IR) */
    const val DEFAULT_IBAN = "IR120560084580001234567890"

    /** شماره ۱۶ رقمی کارت بانکی (با فرمت خوانا) */
    const val DEFAULT_CARD_NUMBER = "6219-8610-1234-5678"

    /** نام بانک پذیرنده پیش‌فرض */
    const val DEFAULT_BANK_NAME = "بانک سامان"

    // =========================================================================
    // تعرفه‌ها و بسته‌های اشتراک VIP
    // =========================================================================
    data class VipPlan(
        val id: String,
        val title: String,
        val durationTitle: String,
        val durationDays: Int,
        val priceAmount: Long,
        val currencyUnit: String = "تومان",
        val badge: String? = null
    )

    val AVAILABLE_PLANS = listOf(
        VipPlan(
            id = "VIP_30_DAYS",
            title = "اشتراک ماهانه VIP",
            durationTitle = "۳۰ روز دسترسی کامل",
            durationDays = 30,
            priceAmount = 199_000L,
            badge = "محبوب‌ترین"
        ),
        VipPlan(
            id = "VIP_90_DAYS",
            title = "اشتراک فصلی VIP",
            durationTitle = "۹۰ روز دسترسی کامل",
            durationDays = 90,
            priceAmount = 499_000L,
            badge = "تخفیف ویژه"
        ),
        VipPlan(
            id = "VIP_365_DAYS",
            title = "اشتراک سالانه VIP",
            durationTitle = "۳۶۵ روز دسترسی نامحدود",
            durationDays = 365,
            priceAmount = 1_499_000L,
            badge = "بیشترین صرفه"
        )
    )

    fun getPlanById(planId: String?): VipPlan {
        return AVAILABLE_PLANS.find { it.id == planId } ?: getDefaultPlan()
    }

    fun getDefaultPlan(): VipPlan = AVAILABLE_PLANS.first()

    /** عنوان طرح پیش‌فرض VIP */
    const val DEFAULT_PLAN_TITLE = "اشتراک ماهانه VIP"

    /** مدت اعتبار طرح پیش‌فرض */
    const val DEFAULT_PLAN_DURATION = "۳۰ روز"

    /** قیمت به تومان (مبلغ ریالی معادل با ۱۰ ضرب می‌شود) */
    const val DEFAULT_VIP_PRICE_AMOUNT = 199_000L

    /** واحد پول برای نمایش در UI */
    const val DEFAULT_CURRENCY_UNIT = "تومان"

    /** آیدی پشتیبانی تلگرام جهت تسریع در بررسی */
    const val SUPPORT_CONTACT = "پشتیبانی تلگرام: @ReelsStudio_Support"

    // =========================================================================
    // مزایای اشتراک VIP
    // =========================================================================
    val VIP_BENEFITS = listOf(
        "تولید نامحدود سناریوهای وایرال ریلز و شورتز",
        "تولید قلاب‌های میخکوب‌کننده اختصاصی (۳ ثانیه اول)",
        "بدون هیچ‌گونه تبلیغات (حذف کامل تبلیغات ویدیویی و بنری)",
        "دسترسی به تکنیک‌های پیشرفته قلاب-بدنه-CTA",
        "اولویت پردازش بالا در پردازش هوش مصنوعی جمینای",
        "پشتیبانی مستقیم و اولویت‌دار در تلگرام VIP"
    )

    private const val PREFS_NAME = "vip_payment_config_prefs"
    private const val KEY_ACCOUNT_HOLDER = "account_holder"
    private const val KEY_IBAN = "iban"
    private const val KEY_CARD_NUMBER = "card_number"
    private const val KEY_BANK_NAME = "bank_name"
    private const val KEY_PLAN_TITLE = "plan_title"
    private const val KEY_PLAN_DURATION = "plan_duration"
    private const val KEY_PRICE_AMOUNT = "price_amount"
    private const val KEY_CURRENCY_UNIT = "currency_unit"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAccountHolderName(context: Context): String =
        getPrefs(context).getString(KEY_ACCOUNT_HOLDER, DEFAULT_ACCOUNT_HOLDER_NAME) ?: DEFAULT_ACCOUNT_HOLDER_NAME

    fun getIban(context: Context): String =
        getPrefs(context).getString(KEY_IBAN, DEFAULT_IBAN) ?: DEFAULT_IBAN

    fun getCardNumber(context: Context): String =
        getPrefs(context).getString(KEY_CARD_NUMBER, DEFAULT_CARD_NUMBER) ?: DEFAULT_CARD_NUMBER

    fun getBankName(context: Context): String =
        getPrefs(context).getString(KEY_BANK_NAME, DEFAULT_BANK_NAME) ?: DEFAULT_BANK_NAME

    fun getPlanTitle(context: Context): String =
        getPrefs(context).getString(KEY_PLAN_TITLE, DEFAULT_PLAN_TITLE) ?: DEFAULT_PLAN_TITLE

    fun getPlanDuration(context: Context): String =
        getPrefs(context).getString(KEY_PLAN_DURATION, DEFAULT_PLAN_DURATION) ?: DEFAULT_PLAN_DURATION

    fun getPriceAmount(context: Context): Long =
        getPrefs(context).getLong(KEY_PRICE_AMOUNT, DEFAULT_VIP_PRICE_AMOUNT)

    fun getCurrencyUnit(context: Context): String =
        getPrefs(context).getString(KEY_CURRENCY_UNIT, DEFAULT_CURRENCY_UNIT) ?: DEFAULT_CURRENCY_UNIT

    fun getFormattedPrice(context: Context): String {
        val amount = getPriceAmount(context)
        val unit = getCurrencyUnit(context)
        return "%,d %s".format(amount, unit)
    }

    /**
     * بروزرسانی مشخصات کارت و درگاه بانکی
     */
    fun updateConfig(
        context: Context,
        accountHolder: String = DEFAULT_ACCOUNT_HOLDER_NAME,
        iban: String = DEFAULT_IBAN,
        cardNumber: String = DEFAULT_CARD_NUMBER,
        bankName: String = DEFAULT_BANK_NAME,
        priceAmount: Long = DEFAULT_VIP_PRICE_AMOUNT,
        duration: String = DEFAULT_PLAN_DURATION
    ) {
        getPrefs(context).edit()
            .putString(KEY_ACCOUNT_HOLDER, accountHolder.trim())
            .putString(KEY_IBAN, iban.trim())
            .putString(KEY_CARD_NUMBER, cardNumber.trim())
            .putString(KEY_BANK_NAME, bankName.trim())
            .putLong(KEY_PRICE_AMOUNT, priceAmount)
            .putString(KEY_PLAN_DURATION, duration.trim())
            .apply()
    }
}
