package dev.robinsiegl.nia.platform

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal enum class FlashlightResult { CHANGED, NO_FLASH, UNAVAILABLE }

/** Public torch API only: no camera session, background service or camera permission. */
internal class FlashlightController(context: Context) : AutoCloseable {
    private val manager = context.applicationContext.getSystemService(CameraManager::class.java)
    private val executor = context.applicationContext.mainExecutor
    private val modes = MutableStateFlow<Map<String, Boolean?>>(emptyMap())
    private val mutex = Mutex()
    private var registered = false
    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { modes.update { it + (cameraId to enabled) } }
        override fun onTorchModeUnavailable(cameraId: String) { modes.update { it + (cameraId to null) } }
    }

    suspend fun toggle(): FlashlightResult = mutex.withLock {
        val service = manager ?: return@withLock FlashlightResult.NO_FLASH
        try {
            val camera = withContext(Dispatchers.IO) {
                service.cameraIdList.map { it to service.getCameraCharacteristics(it) }
                    .filter { it.second[CameraCharacteristics.FLASH_INFO_AVAILABLE] == true }
                    .sortedBy { if (it.second[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK) 0 else 1 }
                    .firstOrNull()?.first
            } ?: return@withLock FlashlightResult.NO_FLASH
            withContext(Dispatchers.Main.immediate) {
                if (!registered) { service.registerTorchCallback(executor, callback); registered = true }
            }
            // Use the real torch state, including changes from Android Quick Settings or another app.
            val known = withTimeoutOrNull(1_500) { modes.first { camera in it } }
            val enabled = known?.get(camera) ?: return@withLock FlashlightResult.UNAVAILABLE
            withContext(Dispatchers.IO) { service.setTorchMode(camera, !enabled) }
            val confirmed = withTimeoutOrNull(1_500) { modes.first { it[camera] == !enabled } }
            if (confirmed == null) FlashlightResult.UNAVAILABLE else FlashlightResult.CHANGED
        } catch (_: CameraAccessException) {
            FlashlightResult.UNAVAILABLE
        } catch (_: IllegalArgumentException) {
            FlashlightResult.NO_FLASH
        } catch (_: SecurityException) {
            FlashlightResult.UNAVAILABLE
        }
    }

    override fun close() {
        if (registered) manager?.unregisterTorchCallback(callback)
        registered = false
    }
}
