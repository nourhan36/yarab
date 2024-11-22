package com.example.yarab

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.Config
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableException

class Main2 : AppCompatActivity() {

    private var mSession: Session? = null
    private var mUserRequestedInstall = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // Check if ARCore is supported and up to date
        if (isARCoreSupportedAndUpToDate()) {
            // ARCore is supported, now set up the session
            createSession()
        } else {
            // ARCore is not supported
            Toast.makeText(this, "ARCore is not supported on this device.", Toast.LENGTH_LONG).show()
        }
    }

    // Verify if ARCore is supported and up to date
    private fun isARCoreSupportedAndUpToDate(): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(this)

        return when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                Log.i(TAG, "ARCore is installed and up-to-date.")
                true
            }
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                try {
                    when (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            Log.i(TAG, "ARCore installation requested.")
                            mUserRequestedInstall = false
                            false
                        }
                        ArCoreApk.InstallStatus.INSTALLED -> {
                            Log.i(TAG, "ARCore installed successfully.")
                            true
                        }
                    }
                } catch (e: UnavailableException) {
                    Log.e(TAG, "ARCore not installed: ${e.localizedMessage}")
                    Toast.makeText(this, "ARCore is not installed or not supported on this device.", Toast.LENGTH_LONG).show()
                    false
                }
            }
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                Log.e(TAG, "ARCore is not supported on this device.")
                Toast.makeText(this, "Your device does not support AR.", Toast.LENGTH_LONG).show()
                false
            }
            ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                Log.i(TAG, "ARCore availability is still being checked.")
                false
            }
            ArCoreApk.Availability.UNKNOWN_ERROR, ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                Log.e(TAG, "Error checking ARCore availability.")
                Toast.makeText(this, "An error occurred while checking ARCore availability.", Toast.LENGTH_LONG).show()
                false
            }
        }
    }

    // Create and configure an AR session
    private fun createSession() {
        try {
            // Create a new ARCore session
            mSession = Session(this)

            // Create a session config
            val config = Config(mSession)

            // Configure specific features, e.g., enabling depth or augmented faces
            // config.depthMode = Config.DepthMode.ANY
            // config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D

            // Apply the configuration to the session
            mSession?.configure(config)

            // Log success
            Log.i(TAG, "AR session created and configured successfully.")

        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Device is not compatible with AR.", e)
            Toast.makeText(this, "Device is not compatible with AR.", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableException) {
            Log.e(TAG, "Failed to create AR session.", e)
            Toast.makeText(this, "Failed to create AR session.", Toast.LENGTH_LONG).show()
        }
    }

    // Handle AR session lifecycle
    override fun onResume() {
        super.onResume()
        mSession?.resume()  // Resumes the AR session when the app comes into focus
    }

    override fun onPause() {
        super.onPause()
        mSession?.pause()  // Pauses the AR session when the app goes to the background
    }
}
