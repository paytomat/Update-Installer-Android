package com.deepdive.updateinstaller.model

import java.lang.IllegalArgumentException

class Config private constructor(
    val appId: String,
    val apkUrl: String,
    val usePlayStrategy: InstallerStrategy,
    val fileName: String,
    val showDownloadMessages: Boolean
) {

    /**
     * Builder for library configuration
     * @param appId application id for update
     */
    class Builder(private val appId: String) {
        private var apkUrl: String = ""
        private var usePlayStrategy: InstallerStrategy = InstalledByPlayStoreStrategy
        private var fileName = "app.apk"
        private var showDownloadMessages: Boolean = true

        /**
         * @param apkUrl web url where apk is stored. Make sure it can be downloaded by this link
         */
        fun setApkUrl(apkUrl: String): Builder = this.apply { this.apkUrl = apkUrl }

        /**
         * @param strategy decides how to act for updates, sometimes(when apk-signature matches store signature update can be installed from store)
         */
        fun setPlayStrategy(strategy: InstallerStrategy): Builder =
            this.apply { this.usePlayStrategy = strategy }

        /**
         * @param name file name for downloaded file
         */
        fun setFileName(name: String): Builder = this.apply {
            val fileExtension = ".apk"
            if (name.endsWith(fileExtension)) this.fileName = name
            else this.fileName = name + fileExtension
        }

        /**
         * @param showDownloadMessages decides if default toast prompts must be shown to user(only if apk is downloaded)
         */
        fun setShowDownloadMessages(showDownloadMessages: Boolean): Builder = this.apply {
            this.showDownloadMessages = showDownloadMessages
        }

        fun build(): Config = Config(
            appId = appId.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("AppId must be provided"),
            apkUrl = apkUrl.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("AppUlr must be provided"),
            usePlayStrategy = usePlayStrategy,
            fileName = fileName.takeIf { it.isNotBlank() } ?: "$appId.apk",
            showDownloadMessages = showDownloadMessages
        )
    }
}

/**
Used to decide source of update
 */
sealed class InstallerStrategy

/**
 * If developer uses .aab format or PlayConsole Signing - this must be your option
 * PlayStore will be used only if it's the source of app install.
 * This option - default one
 */
object InstalledByPlayStoreStrategy : InstallerStrategy()

/**
 * PlayStore will be used if it exists on device
 */
object OnDevicePlayStoreStrategy : InstallerStrategy()