package dev.robinsiegl.nia

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import dev.robinsiegl.nia.platform.FlashlightController
import dev.robinsiegl.nia.platform.FlashlightResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import java.util.concurrent.Executor

/** Robolectric omits the API's initial torch-state callback and Executor overload. */
@Implements(CameraManager::class)
class CallbackCameraManagerShadow : ShadowCameraManager() {
    var unavailable = false
    val registered = mutableSetOf<CameraManager.TorchCallback>()
    @Implementation(minSdk = 28)
    protected fun registerTorchCallback(executor: Executor, callback: CameraManager.TorchCallback) {
        super.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        registered += callback
        super.getCameraIdList().forEach { id ->
            if (unavailable) callback.onTorchModeUnavailable(id) else callback.onTorchModeChanged(id, getTorchMode(id))
        }
    }
    @Implementation(minSdk = 23)
    override fun unregisterTorchCallback(callback: CameraManager.TorchCallback) {
        registered -= callback
        super.unregisterTorchCallback(callback)
    }
    @Implementation(minSdk = 23)
    override fun setTorchMode(cameraId: String, enabled: Boolean) {
        if (unavailable) throw CameraAccessException(CameraAccessException.CAMERA_IN_USE)
        super.setTorchMode(cameraId, enabled)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [CallbackCameraManagerShadow::class])
@OptIn(ExperimentalCoroutinesApi::class)
class FlashlightControllerTest {
    private fun add(manager: CameraManager, id: String, facing: Int) {
        val camera = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(camera).set(CameraCharacteristics.FLASH_INFO_AVAILABLE, true)
        shadowOf(camera).set(CameraCharacteristics.LENS_FACING, facing)
        shadowOf(manager).addCamera(id, camera)
        manager.setTorchMode(id, false)
    }
    @Test fun `toggles the rear flash and follows external torch changes while releasing its callback`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(CameraManager::class.java)
        val shadow = shadowOf(manager) as CallbackCameraManagerShadow
        add(manager, "front", CameraCharacteristics.LENS_FACING_FRONT)
        add(manager, "back", CameraCharacteristics.LENS_FACING_BACK)
        val controller = FlashlightController(context)
        try {
            assertEquals(FlashlightResult.CHANGED, controller.toggle())
            assertTrue(shadow.getTorchMode("back")); assertFalse(shadow.getTorchMode("front"))
            manager.setTorchMode("back", false)
            assertEquals(FlashlightResult.CHANGED, controller.toggle())
            assertTrue(shadow.getTorchMode("back"))
            assertEquals(FlashlightResult.CHANGED, controller.toggle())
            assertFalse(shadow.getTorchMode("back"))
            assertEquals(1, shadow.registered.size)
        } finally { controller.close(); Dispatchers.resetMain() }
        assertTrue(shadow.registered.isEmpty())
    }
    @Test fun `busy hardware is recoverable and missing flash does not register a callback`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(CameraManager::class.java)
        val shadow = shadowOf(manager) as CallbackCameraManagerShadow
        val controller = FlashlightController(context)
        try {
            assertEquals(FlashlightResult.NO_FLASH, controller.toggle())
            assertTrue(shadow.registered.isEmpty())
            add(manager, "back", CameraCharacteristics.LENS_FACING_BACK)
            shadow.unavailable = true
            assertEquals(FlashlightResult.UNAVAILABLE, controller.toggle())
            shadow.unavailable = false
            manager.setTorchMode("back", false)
            assertEquals(FlashlightResult.CHANGED, controller.toggle())
        } finally { controller.close(); Dispatchers.resetMain() }
        assertTrue(shadow.registered.isEmpty())
    }
}
