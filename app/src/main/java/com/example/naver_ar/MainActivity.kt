package com.example.naver_ar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.naver_ar.databinding.ActivityMainBinding
import com.example.naver_ar.util.CameraPermissionHelper
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    //requestInstall(Activity, true) will triggers installation of Google play services for AR if necessary.
    var mUserRequestedInstall = true

    private lateinit var mSession: Session

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        //Enable AR-related functionality on ARCore supported devices only.
        isSupportArDevice()
    }

    private fun isSupportArDevice() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            //Continue to query availability at 5Hz? while compatibility is checked in the background
            Handler().postDelayed({
                isSupportArDevice()
            }, 200)
        }

        if (availability.isSupported) {
            binding.btnAr.visibility = View.VISIBLE
            binding.btnAr.isEnabled = true
        } else {
            binding.btnAr.visibility = View.INVISIBLE
            binding.btnAr.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()

        //ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
        }

        // Ensure that Google Play Services for AR and ARCore device profile data are installed and up to date.
        try {
            if (mSession == null) {
                when (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        //Success: Safe to create the AR session.
                        mSession = Session(this)
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        /**
                         * When this method returns ; INSTALL_REQUESTED
                         * 1. ARCore pauses this activity.
                         * 2. ARCore prompts the user to install or update Google play
                         * Services for AR (market://details?id=com.google.ar.core).
                         * 3. ARCore downloads the latest device profile data.
                         * 4. ARCore resumes this activity. The next invocation of
                         * requestInstall() will either return 'INSTALLED' or throw an
                         * exception if the installation or update did not succeed.
                         */
                        mUserRequestedInstall = false
                        return
                    }
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            //Display an appropriate message to the user and return gracefully
            Toast.makeText(this, "TODO: handle exception$e", Toast.LENGTH_LONG).show()
            return
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG).show()

            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                //Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }
}