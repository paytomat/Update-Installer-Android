package com.deepdive.updateinstaller

import android.content.Context
import com.deepdive.updateinstaller.model.*

object UpdateInstaller {

    internal val storePackages: List<String> =
        listOf("com.android.vending", "com.google.android.feedback")

    private lateinit var config: Config

    /**
     * Set lib configuration
     * @param config lib configuration
     */
    fun setConfig(config: Config) {
        this.config = config
    }

    /**
     * Depending on configuration checks if PlayStore installed or if app was installed from store
     * @param context required to check what installed app or if store is on device
     * @throws UninitializedPropertyAccessException if config was not initialized
     * @return if app will be installed from store
     */
    @Throws(UninitializedPropertyAccessException::class)
    fun canUseStoreUpdate(context: Context): Boolean = when (config.usePlayStrategy) {
        InstalledByPlayStoreStrategy -> {
            val installer: String? =
                context.packageManager.getInstallerPackageName(context.packageName)
            installer != null && installer in storePackages
        }
        OnDevicePlayStoreStrategy -> {
            storePackages.any { Utils.isPackageEnabled(context.packageManager, it) }
        }
    }

    /**
     * Depending on config decides which strategy must be used and triggers update
     * @param context required to check what installed app or if store is on device, and open store or start download
     * @throws UninitializedPropertyAccessException if config was not initialized
     */
    @Throws(UninitializedPropertyAccessException::class)
    fun update(context: Context, downloadInfo: DownloadInfo) {
        if (canUseStoreUpdate(context)) {
            PlayStoreStrategy(config).update(context)
        } else {
            ApkLoaderStrategy(config, downloadInfo).update(context.applicationContext)
        }
    }

    /**
     * Remove application download files
     * @param context required to get directory where files are stored
     */
    fun cleanup(context: Context) {
        ApkLoaderStrategy.cleanUp(context)
    }
}