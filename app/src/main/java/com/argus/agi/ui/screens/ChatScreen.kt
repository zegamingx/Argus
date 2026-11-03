package com.argus.agi.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.argus.agi.R
import com.argus.agi.ui.ArgusViewModel
import com.argus.agi.ui.ChatMessage
import com.argus.agi.ui.theme.ArgusBlueAccent
import com.argus.agi.ui.theme.ArgusCardBorder
import com.argus.agi.ui.theme.ArgusCyanGlow
import com.argus.agi.ui.theme.ArgusCyanPrimary
import com.argus.agi.ui.theme.ArgusDarkBg
import com.argus.agi.ui.theme.ArgusSuccess
import com.argus.agi.ui.theme.ArgusSurface
import com.argus.agi.ui.theme.ArgusSurfaceVariant
import com.argus.agi.ui.theme.ArgusTextMuted
import com.argus.agi.ui.theme.ArgusTextPrimary
import com.argus.agi.ui.theme.ArgusTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ArgusViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val totalParams = viewModel.getTotalParameters()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var isHeaderExpanded by remember { mutableStateOf(true) }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = imeBottom > 0.dp

    // Auto-scroll on new message or when keyboard opens
    LaunchedEffect(messages.size, isKeyboardOpen) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // File and Image Pickers
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFileContent(it) }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.importImageContent(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArgusDarkBg)
            .imePadding()
    ) {
        // Collapsible Sci-Fi Status Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ArgusSurface,
            tonalElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isHeaderExpanded = !isHeaderExpanded }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_argus_logo),
                            contentDescription = "ARGUS Core",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, ArgusCyanPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "A.R.G.U.S AGI CORE",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                color = ArgusCyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100% On-Device | $totalParams Params",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusCyanGlow
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ArgusSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ZERO CLOUD",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusCyanPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isHeaderExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isHeaderExpanded) "Ciutkan Header" else "Perluas Header",
                            tint = ArgusCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Collapsible Header Details
                AnimatedVisibility(
                    visible = isHeaderExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArgusDarkBg.copy(alpha = 0.5f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Privasi: 100% On-Device | Zero Telemetry",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArgusTextMuted
                        )
                        Text(
                            text = "Bersihkan Chat",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = ArgusCyanPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.clearChat()
                                    Toast.makeText(context, "Percakapan dibersihkan", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Messages List with Selection & Copying
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(msg)
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ArgusSurfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ArgusCyanPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "A.R.G.U.S sedang menalar, memeriksa memori & bobot neural...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = ArgusCyanGlow,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Suggested Action Chips (hidden if keyboard is open to save vertical space)
        AnimatedVisibility(visible = !isKeyboardOpen) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "Hitung 125 * 34",
                    "Apa arsitektur A.R.G.U.S?",
                    "Pelajari perkembangan AGI terbaru",
                    "Bagaimana cara kerja LoRA?"
                )
                items(chips) { chip ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ArgusSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArgusCardBorder),
                        modifier = Modifier.clickable {
                            viewModel.sendMessage(chip)
                        }
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelSmall,
                            color = ArgusTextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Input Bar positioned safely above keyboard
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ArgusSurface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Document
                IconButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Import Dokumen",
                        tint = ArgusCyanPrimary
                    )
                }

                // Attach Image
                IconButton(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Import Gambar",
                        tint = ArgusBlueAccent
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Tanya, perintah aksi, atau latih...",
                            color = ArgusTextMuted,
                            fontSize = 12.sp
                        )
                    },
                    trailingIcon = {
                        // Quick Paste button if clipboard has text and field is empty
                        if (inputText.isEmpty()) {
                            IconButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        inputText = clipText
                                        Toast.makeText(context, "Teks ditempel dari clipboard", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Clipboard kosong", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Tempel Clipboard",
                                    tint = ArgusTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArgusCyanPrimary,
                        unfocusedBorderColor = ArgusCardBorder,
                        focusedTextColor = ArgusTextPrimary,
                        unfocusedTextColor = ArgusTextPrimary,
                        focusedContainerColor = ArgusSurfaceVariant,
                        unfocusedContainerColor = ArgusSurfaceVariant
                    ),
                    maxLines = 4
                )

                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotBlank()) {
                            viewModel.sendMessage(text)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ArgusCyanPrimary)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = ArgusDarkBg,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(msg: ChatMessage) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var isThinkingExpanded by remember { mutableStateOf(false) }
    var isLongTextExpanded by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }

    val isLongMessage = msg.text.length > 450
    val displayText = if (isLongMessage && !isLongTextExpanded) {
        msg.text.take(380) + "..."
    } else {
        msg.text
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (msg.isUser) "Pengguna" else "A.R.G.U.S",
                style = MaterialTheme.typography.labelSmall,
                color = if (msg.isUser) ArgusBlueAccent else ArgusCyanPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 16.dp
            ),
            color = if (msg.isUser) ArgusBlueAccent.copy(alpha = 0.25f) else ArgusSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (msg.isUser) ArgusBlueAccent else ArgusCardBorder
            ),
            modifier = Modifier.padding(
                start = if (msg.isUser) 44.dp else 0.dp,
                end = if (msg.isUser) 0.dp else 44.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Attached Image preview
                if (msg.attachedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = msg.attachedImageUri),
                        contentDescription = "Gambar Terlampir",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Attached File indicator
                if (msg.attachedFileName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ArgusDarkBg.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "File",
                            tint = ArgusCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = msg.attachedFileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ArgusTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Assistant Thinking Collapsible Block
                if (!msg.isUser && !msg.thinkReasoning.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ArgusDarkBg.copy(alpha = 0.6f))
                            .clickable { isThinkingExpanded = !isThinkingExpanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Reasoning",
                            tint = ArgusCyanGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Penalaran & Self-Reflection",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArgusCyanGlow,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = ArgusCyanGlow,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isThinkingExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = msg.thinkReasoning,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = ArgusTextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, start = 4.dp, end = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // SelectionContainer ensures users can select, highlight and copy-paste text directly!
                SelectionContainer {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ArgusTextPrimary
                    )
                }

                // Collapsible toggle for long messages
                if (isLongMessage) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLongTextExpanded) "Ciutkan Teks ▲" else "Baca Selengkapnya ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArgusCyanPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isLongTextExpanded = !isLongTextExpanded }
                            .padding(vertical = 2.dp)
                    )
                }

                // Copy Action Footer Button
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ArgusDarkBg.copy(alpha = 0.4f))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(msg.text))
                                isCopied = true
                                Toast.makeText(context, "Pesan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Salin pesan",
                            tint = if (isCopied) ArgusSuccess else ArgusTextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCopied) "Tersalin" else "Salin",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isCopied) ArgusSuccess else ArgusTextMuted
                        )
                    }
                }
            }
        }
    }
}
