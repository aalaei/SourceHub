package org.eu.john007.hdmikvm.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.TvContractCompat
import org.eu.john007.hdmikvm.MainActivity
import org.eu.john007.hdmikvm.model.KvmSource

object HomeChannelManager {
    private const val CHANNEL_NAME = "HDMI KVM Sources"
    private const val CHANNEL_DESCRIPTION = "Quick switch for HDMI KVM inputs"

    fun updateHomeChannel(context: Context, sources: List<KvmSource>) {
        val helper = PreviewChannelHelper(context)
        
        var channelId = getExistingChannelId(helper)
        if (channelId == -1L) {
            val channel = PreviewChannel.Builder()
                .setDisplayName(CHANNEL_NAME)
                .setDescription(CHANNEL_DESCRIPTION)
                .setAppLinkIntent(Intent(context, MainActivity::class.java))
                .build()
            try {
                channelId = helper.publishChannel(channel)
                TvContractCompat.requestChannelBrowsable(context, channelId)
            } catch (e: Exception) {
                android.util.Log.e("KVM_DEBUG", "Failed to publish channel", e)
            }
        }

        if (channelId != -1L) {
            try {
                // Clear existing programs
                context.contentResolver.delete(
                    TvContract.PreviewPrograms.CONTENT_URI,
                    "${TvContract.PreviewPrograms.COLUMN_CHANNEL_ID} = ?",
                    arrayOf(channelId.toString())
                )

                // Add new programs
                sources.forEach { source ->
                    val intent = Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("source_option", source.optionName)
                        putExtra("source_name", source.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }

                    val values = ContentValues().apply {
                        put(TvContract.PreviewPrograms.COLUMN_CHANNEL_ID, channelId)
                        put(TvContract.PreviewPrograms.COLUMN_TITLE, source.name)
                        put(TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION, "Switch to ${source.name}")
                        put(TvContract.PreviewPrograms.COLUMN_INTENT_URI, intent.toUri(Intent.URI_INTENT_SCHEME))
                        put(TvContract.PreviewPrograms.COLUMN_TYPE, TvContract.PreviewPrograms.TYPE_CHANNEL)
                        put(TvContract.PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO, TvContract.PreviewPrograms.ASPECT_RATIO_16_9)
                    }
                    context.contentResolver.insert(TvContract.PreviewPrograms.CONTENT_URI, values)
                }
            } catch (e: Exception) {
                android.util.Log.e("KVM_DEBUG", "Failed to update programs", e)
            }
        }
    }

    private fun getExistingChannelId(helper: PreviewChannelHelper): Long {
        return try {
            helper.allChannels.find { it.displayName == CHANNEL_NAME }?.id ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }
}
