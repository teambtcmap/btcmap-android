package org.btcmap.map

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.view.IconButton

class UpdateNotificationController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val icon: IconButton,
) {
    init {
        icon.isVisible = false
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.withResumed {
                launch {
                    try {
                        val latestVerJson = OkHttpClient.Builder().build().newCall(
                            Request.Builder()
                                .url("https://static.btcmap.org/android/latest-app-ver.json".toHttpUrl())
                                .build()
                        ).executeAsync().body.string().trim()

                        val latestVer =
                            com.google.gson.JsonParser.parseString(latestVerJson).asJsonObject
                        val latestVerCode = latestVer.get("code").asInt
                        val latestVerName = latestVer.get("name").asString
                        val latestVerUrl = latestVer.get("url").asString

                        if (latestVerCode > BuildConfig.VERSION_CODE) {
                            lifecycleOwner.withResumed {
                                icon.isVisible = true
                                icon.iconColor(context.getErrorColor())

                                icon.setOnClickListener {
                                    MaterialAlertDialogBuilder(context)
                                        .setTitle(R.string.update_available)
                                        .setMessage(
                                            context.getString(
                                                R.string.update_available_description,
                                                BuildConfig.VERSION_NAME, latestVerName
                                            )
                                        )
                                        .setPositiveButton(R.string.get_apk) { _, _ ->
                                            val intent = Intent(Intent.ACTION_VIEW)
                                            intent.data = latestVerUrl.toUri()
                                            context.startActivity(intent)
                                        }
                                        .setNegativeButton(R.string.ignore, null)
                                        .show()
                                }
                            }
                        }
                    } catch (_: Throwable) {

                    }
                }
            }
        }
    }
}