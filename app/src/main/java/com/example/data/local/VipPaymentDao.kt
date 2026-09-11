package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VipPaymentDao {

    @Query("SELECT * FROM vip_payment_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<VipPaymentRequestEntity>>

    @Query("SELECT * FROM vip_payment_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): VipPaymentRequestEntity?

    @Query("SELECT * FROM vip_payment_requests WHERE status = 'APPROVED' AND (expiresAt IS NULL OR expiresAt > :currentTime) ORDER BY expiresAt DESC LIMIT 1")
    fun getActiveApprovedRequest(currentTime: Long): Flow<VipPaymentRequestEntity?>

    @Query("SELECT * FROM vip_payment_requests WHERE status = 'APPROVED' AND (expiresAt IS NULL OR expiresAt > :currentTime) ORDER BY expiresAt DESC LIMIT 1")
    suspend fun getActiveApprovedRequestOnce(currentTime: Long): VipPaymentRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: VipPaymentRequestEntity)

    @Update
    suspend fun updateRequest(request: VipPaymentRequestEntity)

    @Query("DELETE FROM vip_payment_requests WHERE id = :id")
    suspend fun deleteRequestById(id: String)
}
