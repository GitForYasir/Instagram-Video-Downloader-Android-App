package com.example.instavideodownloader.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.instavideodownloader.presentation.viewModel.MainViewModel
import com.example.instavideodownloader.ui.theme.InstaVideoDownloaderTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10–12
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                // Old devices < Android 10
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }*/

        // request storage permission android 12 above
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ){
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            InstaVideoDownloaderTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()
                ) {
                    InstagramVideoDownloader()
                }
            }
        }
    }
}

@Composable
fun InstagramVideoDownloader(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiStates by viewModel.uiStates.collectAsState()

    var url by rememberSaveable { mutableStateOf("") }
    val clipboardManger = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var previewUrl by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // header
        Card(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFBE3DFD)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(18.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(70.dp)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Instagram Video",
                    fontSize = 22.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Downloader",
                    fontSize = 22.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )


                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Paste instagram video url to preview and",
                    fontSize = 16.sp,
                    color = Color.White
                )

                Text(
                    text = "Download", fontSize = 16.sp, color = Color.White
                )

            }

        }


        Spacer(Modifier.height(12.dp))

        // url input section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp), colors = CardDefaults.cardColors(
                containerColor = Color.DarkGray
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Instagram URL",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = {
                        Text(
                            "https://www.instagram.com/...", color = Color.LightGray
                        )
                    },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedLabelColor = Color.LightGray,
                        unfocusedTextColor = Color.LightGray
                    ),

                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val getClipboardText = clipboardManger.getText()
                                getClipboardText?.let {
                                    url = it.text
                                    Toast.makeText(
                                        context, "URL pasted from clipboard", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    })

                Spacer(Modifier.height(14.dp))


                // action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // play preview button
                    OutlinedButton(
                        modifier = Modifier
                            .padding(10.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(corner = CornerSize(50.dp)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent, contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White),
                        onClick = {
                            if (url.isNotBlank()) {
                                coroutineScope.launch {
                                    viewModel.getPreviewUrl(url) { videoUrl ->
                                        if (videoUrl != null) {
                                            previewUrl = videoUrl
                                            showPreview = true
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to load preview",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }

                            }
                        },
                        enabled = !uiStates.isLoading && url.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "play",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Preview", color = Color.White)
                    }

                    Spacer(Modifier.width(6.dp))

                    // download button
                    Button(
                        onClick = {
                            if (url.isNotBlank()) {
                                coroutineScope.launch {
                                    viewModel.downloadVideo(url, context)
                                }
                            } else {
                                Toast.makeText(context, "Please enter a URL", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray, contentColor = Color.DarkGray
                        )
                    ) {
                        if (uiStates.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download")
                        }
                    }


                }


            }
        }

        // Preview video section
        if (showPreview && previewUrl != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.DarkGray
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {

                    Text("Video Preview", fontSize = 18.sp, color = Color.White)

                    Spacer(Modifier.height(8.dp))

                    VideoPlayer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp)), videoUrl = previewUrl!!
                    )


                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                showPreview = false
                                previewUrl = null
                            }) {
                            Text("Close preview", color = Color.White, fontSize = 18.sp)
                        }
                    }

                }
            }
        }

        // set uiStates status messages
        uiStates.message?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .padding(bottom = 6.dp, top = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiStates.error) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiStates.error) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer

                )
            }
        }

        // Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp), colors = CardDefaults.cardColors(
                containerColor = Color.DarkGray
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "How to use:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp),
                    color = Color.White
                )

                val instructions = listOf(
                    "1. Go to Instagram and find the video you want to download",
                    "2. Tap the share button and select 'Copy Link'",
                    "3. Come back to this app and paste the URL",
                    "4. Tap 'Preview' to watch the video before downloading",
                    "5. Tap 'Download' and wait for the download to complete",
                    "6. Check your Downloads folder for the video file"
                )

                instructions.forEach { instruction ->
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.White
                    )
                }
            }
        }

    }

}


@Composable
@OptIn(UnstableApi::class)
fun VideoPlayer(modifier: Modifier = Modifier, videoUrl: String) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                val exoPlayer = ExoPlayer.Builder(ctx).build().also { player ->
                    val mediaItem = MediaItem.fromUri(videoUrl.toUri())
                    player.setMediaItem(mediaItem)
                    player.prepare()
                }
                player = exoPlayer
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }

        }, update = { playerView ->
            playerView.player?.let { player ->
                val mediaItem = MediaItem.fromUri(videoUrl.toUri())
                player.setMediaItem(mediaItem)
                player.prepare()
            }
        }, modifier = modifier
    )
}