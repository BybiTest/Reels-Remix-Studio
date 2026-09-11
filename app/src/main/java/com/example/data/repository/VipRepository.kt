package com.example.data.repository

import com.example.data.local.PaymentStatus
import com.example.data.local.SubscriptionStatus
import com.example.data.local.VipPaymentDao
import com.example.data.local.VipPaymentRequestEntity
import com.example.util.PaymentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VipRepository(
    private val vipDao: VipPaymentDao
) {

    fun getAllPaymentRequests(): Flow<List<VipPaymentRequestEntity>> =
        vipDao.getAllRequests()

    fun observeSubscriptionStatus(): Flow<SubscriptionStatus> =
        vipDao.getActiveApprovedRequest(System.currentTimeMillis()).map { activeRequest ->
            if (activeRequest != null) {
                SubscriptionStatus.VIP_ACTIVE
            } else {
                SubscriptionStatus.FREE
            }
        }

    fun observeActiveVipRequest(): Flow<VipPaymentRequestEntity?> =
        vipDao.getActiveApprovedRequest(System.currentTimeMillis())

    suspend fun submitPaymentRequest(
        planId: String,
        amount: Long,
        refNumber: String,
        cardNumber: String,
        receiptUri: String?,
        userNote: String?
    ): Result<VipPaymentRequestEntity> = withContext(Dispatchers.IO) {
        val trimmedRef = refNumber.trim()
        val trimmedCard = cardNumber.trim()

        if (trimmedRef.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("شماره پیگیری یا ارجاع پرداخت الزامی است"))
        }

        if (trimmedCard.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("حداقل ۴ رقم آخر کارت واریزکننده الزامی است"))
        }

        val plan = PaymentConfig.getPlanById(planId)

        val entity = VipPaymentRequestEntity(
            planId = plan.id,
            planTitle = plan.title,
            amountPaid = if (amount > 0) amount else plan.priceAmount,
            transactionRef = trimmedRef,
            senderCardNumber = trimmedCard,
            receiptImageUri = receiptUri,
            userNote = userNote?.takeIf { it.isNotBlank() },
            status = PaymentStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        vipDao.insertRequest(entity)
        Result.success(entity)
    }

    suspend fun approvePaymentRequest(
        requestId: String,
        durationDays: Int = 30
    ) = withContext(Dispatchers.IO) {
        val request = vipDao.getRequestById(requestId) ?: return@withContext
        val now = System.currentTimeMillis()
        val durationMillis = durationDays.toLong() * 24 * 60 * 60 * 1000L
        val updated = request.copy(
            status = PaymentStatus.APPROVED,
            approvedAt = now,
            expiresAt = now + durationMillis,
            rejectionReason = null
        )
        vipDao.updateRequest(updated)
    }

    suspend fun rejectPaymentRequest(
        requestId: String,
        reason: String = "اطلاعات فیش با گردش حساب مطابقت ندارد"
    ) = withContext(Dispatchers.IO) {
        val request = vipDao.getRequestById(requestId) ?: return@withContext
        val updated = request.copy(
            status = PaymentStatus.REJECTED,
            rejectionReason = reason
        )
        vipDao.updateRequest(updated)
    }

    suspend fun deletePaymentRequest(requestId: String) = withContext(Dispatchers.IO) {
        vipDao.deleteRequestById(requestId)
    }
}
