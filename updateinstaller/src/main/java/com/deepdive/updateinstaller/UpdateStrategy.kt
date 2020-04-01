package com.deepdive.updateinstaller

import android.app.DownloadManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.deepdive.updateinstaller.model.Config
import com.deepdive.updateinstaller.model.DownloadInfo
import java.io.File
import java.util.concurrent.atomic.AtomicLong

sealed class UpdateStrategy(internal val config: Config) {
    abstract fun update(context: Context)
}

/**
 * Uss this class to open play store from app or web
 * @param config configuration of update lib
 */
class PlayStoreStrategy(config: Config) : UpdateStrategy(config) {
    /**
     * Tries to open PlayStore app, if unavailable - fallback to web version
     * @param context current activity context
     */
    override fun update(context: Context) {
        val openAppInMarketIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${config.appId}"))

        val marketApps: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(openAppInMarketIntent, 0)
        for (app in marketApps) {
            if (app.activityInfo.applicationInfo.packageName in UpdateInstaller.storePackages) {
                val otherAppActivity: ActivityInfo = app.activityInfo
                val componentName = ComponentName(
                    otherAppActivity.applicationInfo.packageName,
                    otherAppActivity.name
                )
                openAppInMarketIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                openAppInMarketIntent.component = componentName
                context.startActivity(openAppInMarketIntent)
                return
            }
        }

        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=${config.appId}")
        )
        if (webIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(webIntent)
        }
    }
}

/**
 * Use this class only to download apk file from web and install it on device
 * @param config configuration of update lib
 * @param downloadInfo info which will be displayed apk download
 */
class ApkLoaderStrategy(config: Config, private val downloadInfo: DownloadInfo) :
    UpdateStrategy(config) {

    companion object {
        private val LOADING_ID = AtomicLong(-1)
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val UPDATES_DIR = "updates"

        /**
         * Generates path to updates directory
         * @param context required to get access to directory
         * @return updates dir path
         */
        private fun getUpdatesDirPath(context: Context): String =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() +
                    "/" + UPDATES_DIR


        /**
         * Deletes downloaded files
         * @param context required to get access to directory
         */
        internal fun cleanUp(context: Context) {
            File(getUpdatesDirPath(context)).deleteRecursively()
        }
    }

    /**
     * Tries to download and install apk from web.
     * @param context application context
     * @return true - if download is running or started
     */
    override fun update(context: Context) {
        val destination: String = getUpdatesDirPath(context) + "/" + config.fileName

        val uri: Uri = File(destination).let { file ->
            if (file.exists()) file.delete()
            Uri.fromFile(file)
        }
        if (LOADING_ID.get() != -1L) return
        val downloadManager: DownloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri: Uri = Uri.parse(config.apkUrl)
        val request = DownloadManager.Request(downloadUri)
        request.setMimeType(MIME_TYPE)
        if (downloadInfo.title.isNotBlank()) request.setTitle(downloadInfo.title)
        if (downloadInfo.description.isNotBlank()) request.setDescription(downloadInfo.description)
        request.setDestinationUri(uri)
        val downloadId: Long = downloadManager.enqueue(request)
        LOADING_ID.set(downloadId)

        if (config.showDownloadMessages) {
            Toast.makeText(context, R.string.file_load_started, Toast.LENGTH_SHORT).show()
        }
        subscribeOnComplete(context, destination)
    }

    private fun subscribeOnComplete(context: Context, destination: String) {
        // BroadcastReceiver will be triggered when download is complete. It used to install app
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(
                ctx: Context,
                intent: Intent
            ) {
                //Check if requested file downloaded
                if (intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID,
                        -1
                    ) != LOADING_ID.get()
                ) return
                val downloadedFile = File(destination)
                //Start install
                val install = Intent(Intent.ACTION_VIEW)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val contentUri: Uri = FileProvider.getUriForFile(
                        ctx,
                        config.appId + PROVIDER_PATH,
                        downloadedFile
                    )
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    install.data = contentUri
                } else {
                    val downloadManager: DownloadManager =
                        ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    install.setDataAndType(
                        Uri.fromFile(downloadedFile),
                        downloadManager.getMimeTypeForDownloadedFile(LOADING_ID.get())
                    )
                }
                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(install)
                context.unregisterReceiver(this)
                LOADING_ID.set(-1)
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

    }
}