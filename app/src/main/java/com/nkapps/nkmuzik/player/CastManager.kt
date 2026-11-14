package com.nkapps.nkmuzik.player

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.mediarouter.app.MediaRouteChooserDialogFragment
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter

object CastManager {
    fun showCastDialog(context: Context) {
        val mediaRouter = MediaRouter.getInstance(context)
        val selector = MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            .build()

        val activity = context.findActivity()
        if (activity is FragmentActivity) {
            try {
                val dialogFragment = MediaRouteChooserDialogFragment()
                dialogFragment.routeSelector = selector
                dialogFragment.show(activity.supportFragmentManager, "mediaRouteChooserDialogFragment")
            } catch (e: Exception) {
                try {
                    val dialog = androidx.mediarouter.app.MediaRouteChooserDialog(activity)
                    dialog.routeSelector = selector
                    dialog.show()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } else {
            try {
                val dialog = androidx.mediarouter.app.MediaRouteChooserDialog(context)
                dialog.routeSelector = selector
                dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private tailrec fun Context.findActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
