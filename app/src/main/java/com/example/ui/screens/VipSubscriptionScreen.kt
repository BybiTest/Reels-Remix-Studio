package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PaymentStatus
import com.example.data.local.SubscriptionStatus
import com.example.data.local.VipPaymentRequestEntity
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.PaymentConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipSubscriptionScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val subscriptionStatus by viewModel.subscriptionStatus.collectAsState()
    val activeVipRequest by viewModel.activeVipRequest.collectAsState()
    val allRequests by viewModel.vipRepository.getAllPaymentRequests().collectAsState(initial = emptyList())

    var selectedPlan by remember { mutableStateOf(PaymentConfig.getDefaultPlan()) }
    var refNumber by remember { mutableStateOf("") }
    var senderCardNumber by remember { mutableStateOf("") }
    var userNote by remember { mutableStateOf("") }
    var receiptImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        receiptImageUri = uri
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اشتراک VIP و نامحدود", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // وضعیت کنونی کاربر
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    VipStatusBanner(
                        status = subscriptionStatus,
                        activeRequest = activeVipRequest
                    )
                }

                // لیست مزایای VIP
                item {
                    VipBenefitsCard()
                }

                // اگر کاربر VIP فعال ندارد، فرم خرید اشتراک نمایش داده شود
                if (subscriptionStatus != SubscriptionStatus.VIP_ACTIVE) {
                    item {
                        Text(
                            text = "۱. انتخاب طرح اشتراک",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentConfig.AVAILABLE_PLANS.forEach { plan ->
                                PlanSelectionCard(
                                    plan = plan,
                                    isSelected = plan.id == selectedPlan.id,
                                    onSelect = { selectedPlan = plan }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "۲. واریز کارت به کارت",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BankingInfoCard(
                            context = context,
                            selectedPlan = selectedPlan
                        )
                    }

                    item {
                        Text(
                            text = "۳. ثبت مشخصات پرداخت و ارسال فیش",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = refNumber,
                                    onValueChange = { refNumber = it },
                                    label = { Text("شماره ارجاع / پیگیری فیش بانکی *") },
                                    placeholder = { Text("مثال: ۱۲۳۴۵۶۷۸") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = senderCardNumber,
                                    onValueChange = { senderCardNumber = it },
                                    label = { Text("۴ رقم آخر یا شماره کارت واریزکننده *") },
                                    placeholder = { Text("مثال: ۴۳۲۱ یا ۶۰۳۷۹۹...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = userNote,
                                    onValueChange = { userNote = it },
                                    label = { Text("توضیحات اختیاری (شماره موبایل یا آیدی تلگرام)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )

                                // انتخاب تصویر فیش
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (receiptImageUri != null) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = if (receiptImageUri != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (receiptImageUri != null) "تصویر فیش ضمیمه شد (تغییر)" else "پیوست تصویر فیش واریزی (اختیاری)"
                                    )
                                }

                                if (receiptImageUri != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = receiptImageUri,
                                            contentDescription = "تصویر فیش",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (refNumber.isBlank() || senderCardNumber.isBlank()) {
                                            Toast.makeText(context, "لطفاً شماره پیگیری و شماره کارت را تکمیل نمایید", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isSubmitting = true
                                        scope.launch {
                                            val result = viewModel.vipRepository.submitPaymentRequest(
                                                planId = selectedPlan.id,
                                                amount = selectedPlan.priceAmount,
                                                refNumber = refNumber,
                                                cardNumber = senderCardNumber,
                                                receiptUri = receiptImageUri?.toString(),
                                                userNote = userNote
                                            )
                                            isSubmitting = false
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "درخواست اشتراک VIP شما با موفقیت ثبت گردید و در حال بررسی است.", Toast.LENGTH_LONG).show()
                                                refNumber = ""
                                                senderCardNumber = ""
                                                userNote = ""
                                                receiptImageUri = null
                                            } else {
                                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "خطا در ثبت درخواست", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isSubmitting) "در حال ارسال..." else "ثبت نهایی درخواست اشتراک",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // تاریخچه درخواست‌های ثبت‌شده
                if (allRequests.isNotEmpty()) {
                    item {
                        Text(
                            text = "تاریخچه سفارش‌های VIP شما",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(allRequests) { req ->
                        VipRequestItemCard(request = req, viewModel = viewModel)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun VipStatusBanner(
    status: SubscriptionStatus,
    activeRequest: VipPaymentRequestEntity?
) {
    val isVip = (status == SubscriptionStatus.VIP_ACTIVE)
    val bgColor = if (isVip) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isVip) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isVip) Icons.Default.WorkspacePremium else Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = if (isVip) Color(0xFFFFD54F) else contentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isVip) "اشتراک VIP شما فعال است 🌟" else "حساب کاربری: نسخه عادی (رایگان)",
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 16.sp
                )
                if (isVip && activeRequest?.expiresAt != null) {
                    val dateFormatted = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(activeRequest.expiresAt))
                    Text(
                        text = "اعتبار تا: $dateFormatted (بدون محدودیت و بدون تبلیغات)",
                        color = contentColor.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "برای دسترسی نامحدود و حذف تبلیغات، اشتراک VIP تهیه فرمایید.",
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VipBenefitsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "✨ امکانات ویژه اشتراک VIP:",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            PaymentConfig.VIP_BENEFITS.forEach { benefit ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = benefit,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PlanSelectionCard(
    plan: PaymentConfig.VipPlan,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFFFB300) else Color.Transparent)
                        .border(2.dp, if (isSelected) Color(0xFFFFB300) else Color.Gray, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = plan.title, fontWeight = FontWeight.Bold)
                        if (plan.badge != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFB300).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = plan.badge,
                                    fontSize = 10.sp,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = plan.durationTitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "%,d %s".format(plan.priceAmount, plan.currencyUnit),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun BankingInfoCard(
    context: Context,
    selectedPlan: PaymentConfig.VipPlan
) {
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val cardNumber = PaymentConfig.getCardNumber(context)
    val iban = PaymentConfig.getIban(context)
    val accountHolder = PaymentConfig.getAccountHolderName(context)
    val bankName = PaymentConfig.getBankName(context)

    fun copyToClipboard(label: String, text: String) {
        val clip = ClipData.newPlainText(label, text.replace("-", "").trim())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label کپی شد", Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مبلغ قابل پرداخت:",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "%,d %s".format(selectedPlan.priceAmount, selectedPlan.currencyUnit),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    fontSize = 16.sp
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "شماره کارت ($bankName)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = cardNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "به نام: $accountHolder", fontSize = 12.sp)
                }
                IconButton(onClick = { copyToClipboard("شماره کارت", cardNumber) }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "کپی کارت")
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "شماره شبا (IBAN)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = iban, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                IconButton(onClick = { copyToClipboard("شماره شبا", iban) }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "کپی شبا")
                }
            }
        }
    }
}

@Composable
fun VipRequestItemCard(
    request: VipPaymentRequestEntity,
    viewModel: ChatViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dateFormatted = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(request.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = request.planTitle, fontWeight = FontWeight.Bold)
                StatusChip(status = request.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "مبلغ: %,d تومان | ارجاع: %s".format(request.amountPaid, request.transactionRef),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "تاریخ ثبت: $dateFormatted",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // در صورت تست یا تایید محلی برای بررسی فوری
            if (request.status == PaymentStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.vipRepository.approvePaymentRequest(request.id, 30)
                                Toast.makeText(context, "اشتراک شما بلافاصله تایید و فعال شد.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("فعال‌سازی آنی اشتراک (تست/دمو)", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: PaymentStatus) {
    val (label, bg, fg) = when (status) {
        PaymentStatus.PENDING -> Triple("در حال بررسی", Color(0xFFFFF8E1), Color(0xFFF57F17))
        PaymentStatus.APPROVED -> Triple("تایید شده و فعال", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        PaymentStatus.REJECTED -> Triple("رد شده", Color(0xFFFFEBEE), Color(0xFFC62828))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
