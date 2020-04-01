package com.deepdive.updateinstaller

import android.content.pm.PackageManager
import java.io.File

/**
 * created by Alex Ivanov on 01.04.2020.
 */
object Utils {
    @JvmStatic
    fun isPackageEnabled(packageManager: PackageManager, packageName: String): Boolean = try {
        packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (ignore: PackageManager.NameNotFoundException) {
        false
    }
}