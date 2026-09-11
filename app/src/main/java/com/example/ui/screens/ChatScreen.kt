package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ReelsAiPersona
import com.example.data.local.ChatMessageEntity
import com.example.data.local.SubscriptionStatus
import com.example.ui.components.TapsellBannerAdView
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.UiEvent
import com.example.util.findActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToVip: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val conversations by viewModel.conversations.collectAsState()
    val currentConvId by viewModel.currentConversationId.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsState()
    val credits by viewModel.credits.collectAsState()
    val isOnline by viewModel.networkMonitor.isOnline.collectAsState(initial = true)
    val isAdLoading by viewModel.isAdLoading.collectAsState()

    val isVip = (subscriptionStatus == SubscriptionStatus.VIP_ACTIVE)

    var inputPrompt by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showOutOfCreditsDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // اسکرول به انتهای لیست پیام‌ها در صورت ارسال پیام جدید
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // دریافت رویدادهای اعلان
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is UiEvent.RewardGranted -> {
                    Toast.makeText(
                        context,
                        "🎉 تبریک! ${event.credits} اعتبار جدید با موفقیت به حساب شما اضافه شد (مجموع: ${event.newTotal})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سناریوهای ذخیره‌شده",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            viewModel.createNewConversation()
                            scope.launch { drawerState.close() }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "سناریوی جدید")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        items(conversations) { conv ->
                            val isSelected = (conv.id == currentConvId)
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = conv.title,
                                        maxLines = 1,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectConversation(conv.id)
                                    scope.launch { drawerState.close() }
                                },
                                badge = {
                                    IconButton(
                                        onClick = { viewModel.deleteConversation(conv.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    HorizontalDivider()

                    // دکمه ارتقا به VIP در منوی کناری
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                onNavigateToVip()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isVip) "اشتراک VIP فعال است 🌟" else "خرید اشتراک نامحدود VIP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isVip) "بدون هیچ‌گونه تبلیغات" else "تولید نامحدود + حذف تبلیغات",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SmartDisplay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reels Remix Studio",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "منو")
                            }
                        },
                        actions = {
                            // دکمه تماشای ویدیو تبلیغاتی تپسل جهت دریافت اعتبار (برای کاربران رایگان)
                            if (!isVip) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFE8F5E9),
                                    modifier = Modifier.clickable {
                                        if (activity != null) {
                                            viewModel.showTapsellRewardedAd(activity)
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isAdLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircleOutline,
                                                contentDescription = "ویدیو تبلیغاتی تپسل",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ویدیو تپسل (+۳)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // چیپ اشتراک VIP
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isVip) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { onNavigateToVip() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "VIP",
                                        tint = if (isVip) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isVip) "VIP" else "اعتبار: $credits",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isVip) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { showApiKeyDialog = true }) {
                                Icon(Icons.Default.Key, contentDescription = "تنظیمات کلید API")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                ) {
                    // نوار وضعیت آفلاین در صورت قطع اتصال
                    AnimatedVisibility(visible = !isOnline) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ ارتباط اینترنت شما قطع است. لطفاً اتصال شبکه را بررسی کنید.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // لیست پیام‌های سناریو
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (messages.isEmpty()) {
                            EmptyStateView(
                                onSuggestionClick = { suggestion ->
                                    inputPrompt = suggestion
                                }
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(messages) { message ->
                                    MessageBubble(message = message, context = context)
                                }

                                if (isGenerating) {
                                    item {
                                        GeneratingIndicatorBubble()
                                    }
                                }
                            }
                        }
                    }

                    // چیپ‌های پیشنهادات سریع سناریونویسی
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ReelsAiPersona.QUICK_PROMPT_SUGGESTIONS) { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { inputPrompt = suggestion }
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // بنر تبلیغاتی رسمی تپسل (Tapsell Mediation Standard Banner)
                    // فقط برای کاربران رایگان به نمایش گذاشته می‌شود و مانع تایپ یا دکمه ارسال نمی‌شود
                    TapsellBannerAdView(
                        isVipActive = isVip,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    // فیلد ارسال پیام و سناریو
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            placeholder = { Text("موضوع ویدیو، ایده یا قلاب ریلز را بنویسید...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = if (inputPrompt.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(
                                    enabled = inputPrompt.isNotBlank() && !isGenerating,
                                    onClick = {
                                        if (!isVip && credits <= 0) {
                                            showOutOfCreditsDialog = true
                                        } else {
                                            viewModel.sendPrompt(inputPrompt)
                                            inputPrompt = ""
                                        }
                                    }
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "ارسال",
                                        tint = if (inputPrompt.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // دیالوگ اتمام اعتبار برای کاربران رایگان
    if (showOutOfCreditsDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showOutOfCreditsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFFFB300)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اعتبار هوش مصنوعی شما تمام شده")
                    }
                },
                text = {
                    Text("برای تولید سناریوهای بیشتر می‌توانید یک ویدیوی تبلیغاتی کوتاه تپسل تماشا کنید (+۳ اعتبار رایگان) یا اشتراک نامحدود VIP تهیه فرمایید.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOutOfCreditsDialog = false
                            if (activity != null) {
                                viewModel.showTapsellRewardedAd(activity)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاهده تبلیغ تپسل (+۳ اعتبار)")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showOutOfCreditsDialog = false
                            onNavigateToVip()
                        }
                    ) {
                        Text("خرید اشتراک VIP")
                    }
                }
            )
        }
    }

    // دیالوگ تنظیم دستی کلید جمینای
    if (showApiKeyDialog) {
        ApiKeySettingsDialog(
            viewModel = viewModel,
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
fun EmptyStateView(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "استودیوی سناریونویسی ریلز و شورتز",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "موضوع، عنوان یا ایده ویدیوی خود را بنویسید تا سناریوی کامل با قلاب‌های وایرال ۳ ثانیه اول، متن بدنه و کال‌تو‌اکشن حرفه‌ای تحویل بگیرید.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "💡 پیشنهادهای پرطرفدار:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        ReelsAiPersona.QUICK_PROMPT_SUGGESTIONS.take(3).forEach { prompt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSuggestionClick(prompt) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Text(
                    text = prompt,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessageEntity,
    context: Context
) {
    val isUser = (message.role == "user")
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    fun copyText() {
        val clip = ClipData.newPlainText("سناریوی ریلز", message.content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "متن در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
    }

    fun shareText() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message.content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری سناریو")
        context.startActivity(shareIntent)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 4.dp else 16.dp,
                bottomEnd = if (isUser) 16.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "شما" else "دستیار ریلز استودیو 🎬",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isUser) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                    )

                    if (!isUser) {
                        Row {
                            IconButton(onClick = { copyText() }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کپی",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { shareText() }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "اشتراک‌گذاری",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "در حال نگارش سناریوی وایرال و قلاب‌های ریلز...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ApiKeySettingsDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    val maskedPreview = remember { viewModel.keyStorage.getMaskedKeyPreview() }
    val hasCustom = remember { viewModel.keyStorage.hasCustomKey() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنظیمات کلید Gemini API")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "وضعیت فعلی کلید: $maskedPreview",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "می‌توانید کلید اختصاصی جمینای خود را وارد کنید تا به صورت سخت‌افزاری رمزنگاری و ذخیره شود:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (hasCustom) {
                        TextButton(
                            onClick = {
                                viewModel.clearCustomApiKey()
                                onDismiss()
                            }
                        ) {
                            Text("حذف کلید اختصاصی و استفاده از پیش‌فرض", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyInput.isNotBlank()) {
                            viewModel.saveCustomApiKey(keyInput)
                        }
                        onDismiss()
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("انصراف")
                }
            }
        )
    }
}
