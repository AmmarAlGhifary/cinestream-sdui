package com.ammar.cinestream.auth

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/** Opens [url] in a Custom Tab, or the default browser. False when there is no browser at all. */
internal fun Context.openInCustomTab(url: String): Boolean =
    try {
        CustomTabsIntent.Builder().build().launchUrl(this, url.toUri())
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
