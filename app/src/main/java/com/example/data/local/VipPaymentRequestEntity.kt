package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * وضعیت‌های مختلف درخواست پرداخت VIP
 */
enum class PaymentStatus {
    PENDING,    // در انتظار بررسی و تایید
    APPROVED,   // تایید شده - اشتراک فعال است
    REJECTED    // رد شده - اطلاعات نامعتبر
}

/**
 * وضعیت اشتراک کاربر
 */
enum class SubscriptionStatus {
    FREE,       // کاربر عادی (محدودیت سقف و تبلیغات فعال)
    VIP_ACTIVE  // کاربر ویژه فعال (نامحدود و بدون تبلیغات)
}

typealias VipStatus = SubscriptionStatus

@Entity(
    tableName = "vip_payment_requests",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class VipPaymentRequestEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val planId: String = "VIP_30_DAYS",
    val planTitle: String,
    val amountPaid: Long,
    val transactionRef: String,
    val senderCardNumber: String,
    val receiptImageUri: String? = null,
    val userNote: String? = null,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null,
    val expiresAt: Long? = null,
    val rejectionReason: String? = null
)
