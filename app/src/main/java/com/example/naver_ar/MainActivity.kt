package com.example.naver_ar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.naver_ar.databinding.ActivityMainBinding
import com.example.naver_ar.util.CameraPermissionHelper
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException
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

    //Verify that ARCore is installed and using the current version.
    private fun isSupportArDevice(): Boolean {
        return when (ArCoreApk.getInstance().checkAvailability(this)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                try {
                    // Request ARCore installation or update if needed.
                    when (ArCoreApk.getInstance().requestInstall(this, true)) {
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            Log.d("HWO", "ARCore installation requested.")
                            false
                        }
                        ArCoreApk.InstallStatus.INSTALLED -> true
                    }
                } catch (e: UnavailableException) {
                    false
                }
            }
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE ->
                //This device is not supported for AR.
            false

            ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                //ARCore is checking the availability with a remote query.
                //This function should be called again after waiting 200ms to determine the query result.
                Handler().postDelayed({
                    isSupportArDevice()
                }, 200)
            }
            ArCoreApk.Availability.UNKNOWN_ERROR, ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                // There was an error checking for AR availability. This may be due to the device being offline.
                // Handle the error appropriately.
                false
            }
        }
    }

    private fun createSession() {
        //Create a new ARCore session.
        mSession = Session(this)

        //create a session config.
        val config = Config(mSession)

        //Do feature-specific operations here, such as enabling depth or turning on
        //support  for Augmented Faces.
        mSession.configure(config)
    }

    override fun onDestroy() {
        super.onDestroy()
        mSession.close()
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