package com.example.instavideodownloader.abstraction

data class DownloadUiStates(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: Boolean = false
)
