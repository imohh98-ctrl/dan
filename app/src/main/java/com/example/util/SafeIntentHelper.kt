package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.R

object SafeIntentHelper {

    fun shareText(context: Context, text: String, title: String? = null) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                if (title != null) {
                    putExtra(Intent.EXTRA_TITLE, title)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooserIntent = Intent.createChooser(sendIntent, title ?: context.getString(R.string.btn_share_quote)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح نافذة المشاركة حالياً", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWebUrl(context: Context, url: String) {
        try {
            val webpage = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح الرابط الخارجي، يرجى التأكد من وجود متصفح", Toast.LENGTH_LONG).show()
        }
    }

    fun shareApp(context: Context) {
        val shareMessage = context.getString(R.string.share_app_text)
        shareText(context, shareMessage, context.getString(R.string.btn_share_app))
    }

    fun openMoreApps(context: Context) {
        val url = context.getString(R.string.more_apps_url)
        openWebUrl(context, url)
    }
}
