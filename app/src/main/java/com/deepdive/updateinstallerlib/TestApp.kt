package com.deepdive.updateinstallerlib

import android.app.Application
import com.deepdive.updateinstaller.UpdateInstaller
import com.deepdive.updateinstaller.model.Config
import com.deepdive.updateinstaller.model.InstalledByPlayStoreStrategy

/**
 * created by Alex Ivanov on 01.04.2020.
 */
class TestApp : Application() {

    override fun onCreate() {
        super.onCreate()
        UpdateInstaller.cleanup(this)
        UpdateInstaller.setConfig(
            Config.Builder(BuildConfig.APPLICATION_ID)
                .setApkUrl("https://s3.amazonaws.com/paytomat-builds/paytomat.apk")
                .setPlayStrategy(InstalledByPlayStoreStrategy)
                .setFileName("myappname")
                .setShowDownloadMessages(true)
                .build()
        )
    }
}