package com.example.yarab

import android.content.Intent
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnsupportedConfigurationException

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private var session: Session? = null
    private var currentGlassesIndex = 0
    private var installRequested = false
    private val glassesModels = listOf(
        "glasses_model1.glb", // Replace with your actual 3D model files
        "glasses_model2.glb"
    )
    private var renderer: SimpleRenderer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.surfaceview)
        val switchGlassesButton = findViewById<Button>(R.id.switch_glasses_button)


        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                renderer?.start(session, surfaceView.holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                renderer?.onSurfaceChanged(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                renderer?.stop()
            }
        })

        // Switch glasses models on button click
        switchGlassesButton.setOnClickListener {
            currentGlassesIndex = (currentGlassesIndex + 1) % glassesModels.size
            renderer?.loadModel(this, glassesModels[currentGlassesIndex])
            Toast.makeText(this, "Switched Glasses Model", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isARCoreSupportedAndUpToDate(): Boolean {
        return when (ArCoreApk.getInstance().checkAvailability(this)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                try {
                    // Request ARCore installation or update if needed.
                    when (ArCoreApk.getInstance().requestInstall(this, true)) {
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            Log.i(TAG, "ARCore installation requested.")
                            false
                        }
                        ArCoreApk.InstallStatus.INSTALLED -> true
                    }
                } catch (e: UnavailableException) {
                    Log.e(TAG, "ARCore not installed", e)
                    false
                }
            }
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> false
            ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                // ARCore is checking the availability with a remote query.
                // This function should be called again after waiting 200 ms to determine the query result.
                false
            }
            ArCoreApk.Availability.UNKNOWN_ERROR, ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                // There was an error checking for AR availability. This may be due to the device being offline.
                // Handle the error appropriately.
                false
            }
            else -> false
        }
    }

    private fun setupSession() {
        if (!isARCoreSupportedAndUpToDate()) {
            Toast.makeText(this, "ARCore is not supported or needs to be updated", Toast.LENGTH_LONG).show()
            return
        }

        try {
            session = Session(this)
            // Set a camera configuration that uses the front-facing camera.
            val filter = CameraConfigFilter(session).setFacingDirection(CameraConfig.FacingDirection.FRONT)
            val cameraConfig = session!!.getSupportedCameraConfigs(filter)[0]
            session!!.cameraConfig = cameraConfig

            val config = Config(session).apply {
                augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
            }
            session!!.configure(config)

            renderer = SimpleRenderer(this)

        } catch (e: UnavailableArcoreNotInstalledException) {
            e.printStackTrace()
            Toast.makeText(this, "ARCore is not installed. Redirecting to Play Store...", Toast.LENGTH_LONG).show()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.ar.core"))
            startActivity(intent)
        } catch (e: UnavailableDeviceNotCompatibleException) {
            e.printStackTrace()
            Toast.makeText(this, "This device is not compatible with ARCore", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableSdkTooOldException) {
            e.printStackTrace()
            Toast.makeText(this, "Please update ARCore", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableException) {
            e.printStackTrace()
            Toast.makeText(this, "ARCore session could not be started", Toast.LENGTH_LONG).show()
        } catch (e: UnsupportedConfigurationException) {
            e.printStackTrace()
            Toast.makeText(this, "Configuration not supported on this device", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "An unexpected error occurred", Toast.LENGTH_LONG).show()
        }
    }

    private fun isDeviceSupported(): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        return availability.isSupported
    }

    override fun onResume() {
        super.onResume()
        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
            Toast.makeText(this, "Camera not available", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        session?.pause()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}