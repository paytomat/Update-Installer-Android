package com.deepdive.updateinstallerlib

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.deepdive.updateinstaller.UpdateInstaller
import com.deepdive.updateinstaller.model.DownloadInfo
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUpdate.setOnClickListener {
            UpdateInstaller.update(
                context = this,
                downloadInfo = DownloadInfo(title = "Updating my app", description = "")
            )
        }
    }
}
