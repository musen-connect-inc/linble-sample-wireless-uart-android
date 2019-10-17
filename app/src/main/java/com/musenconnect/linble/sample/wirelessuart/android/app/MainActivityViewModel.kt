package com.musenconnect.linble.sample.wirelessuart.android.app

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.musenconnect.linble.sample.wirelessuart.android.common.DebugLogger
import com.musenconnect.linble.sample.wirelessuart.android.common.OperationStep
import com.musenconnect.linble.sample.wirelessuart.android.common.UartDataParserCallback
import com.musenconnect.linble.sample.wirelessuart.android.common.WirelessUartController
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.DeviceBluetoothState
import com.musenconnect.linble.sample.wirelessuart.android.common.uart.command.UartCommand
import com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController
import com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteLinble

typealias ConsumedThisEvent = Boolean

class MainActivityViewModel : ViewModel() {
    private val debugLogger = object : DebugLogger {
        override fun logd(tag: String, message: String) {
            Log.d(tag, message)
        }

        override fun logw(tag: String, message: String) {
            Log.w(tag, message)
        }

        override fun loge(tag: String, message: String) {
            Log.e(tag, message)
        }
    }

    private val runtimePermissionHandler = RuntimePermissionHandler()

    private val bluetoothCentralController = ConcreteBluetoothCentralController(debugLogger) { gatt, bridger ->
        ConcreteLinble(gatt, bridger, debugLogger)
    }

    private val wirelessUartController: WirelessUartController = WirelessUartController(bluetoothCentralController, debugLogger).also {
        it.onChangeOperationStep = { operationStep ->
            liveDataOperationStep.postValue(operationStep)
        }

        it.onChangeDeviceBluetoothState = { deviceBluetoothState ->
            liveDataDeviceBluetoothState.postValue(deviceBluetoothState)
        }
    }

    val liveDataDeviceBluetoothState = MutableLiveData<DeviceBluetoothState>(bluetoothCentralController.currentDeviceBluetoothState)

    val liveDataOperationStep = MutableLiveData<OperationStep>(wirelessUartController.operationStep)

    fun onStart(activity: Activity) {
        val applicationContext = activity.applicationContext

        bluetoothCentralController.provideApplicationContext = {
            /*
            Androidの多くのBLE APIでは、 `Context` オブジェクトが必要になります。
            これはOS側でのアプリケーションのプロセス識別のために用いられます。

            ここではなるべく `Context.applicationContext` で取得できるオブジェクトを使うようにしてください。

            `Activity` も `Context` オブジェクトとして使用することができますが、
            基本的に `Activity` は短命であるため、これを外部に差し出すようなコードはメモリリークを招く原因になります。
             */

            applicationContext
        }

        runtimePermissionHandler.check(activity, ifGrantedRuntimePermission)
    }

    fun onRequestPermissionsResult(activity: Activity, requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        runtimePermissionHandler.onRequestPermissionsResult(activity, requestCode, permissions, grantResults, ifGrantedRuntimePermission)
    }

    private val ifGrantedRuntimePermission = {
        wirelessUartController.start()
    }

    val targetLinbleAddress: String
        get() = wirelessUartController.targetLinbleAddress

    var uartDataParserCallback: UartDataParserCallback?
        get() = wirelessUartController.uartDataParserCallback
        set(value) {
            wirelessUartController.uartDataParserCallback = value
        }

    fun sendCommand(uartCommand: UartCommand) {
        wirelessUartController.write(uartCommand)
    }

    fun onStop() {
        /*
        Activityへのコールバックにつながるイベント伝播だけブロックするようにします。
        */

        wirelessUartController.uartDataParserCallback = null
        bluetoothCentralController.provideApplicationContext = null
        runtimePermissionHandler.clearExplainDialog()

        /*
        Activityは画面回転時にも発生するため、
        ここで `wirelessUartController.stop()` を呼び出してしまうと、
        画面回転時にBLE通信が切断される挙動となってしまうため、注意が必要です。

        `wirelessUartController.stop()` の呼び出しは `onCleared()` 時に行います。
        */
    }

    override fun onCleared() {
        wirelessUartController.stop()

        super.onCleared()
    }
}
