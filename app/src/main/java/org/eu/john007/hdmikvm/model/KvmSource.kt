package org.eu.john007.hdmikvm.model

import androidx.compose.ui.graphics.vector.ImageVector

data class KvmSource(
    val id: Int,
    val name: String,
    val description: String? = null,
    val icon: ImageVector,
    val optionName: String? = null,
    val isSystemItem: Boolean = false
)
