package com.musenconnect.linble.sample.wirelessuart.android.app

import android.app.Application
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.musenconnect.linble.sample.wirelessuart.android.model.DebugLogger
import com.musenconnect.linble.sample.wirelessuart.android.model.OperationStep
import com.musenconnect.linble.sample.wirelessuart.android.model.UartDataParserCallback
import com.musenconnect.linble.sample.wirelessuart.android.model.WirelessUartController
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.BluetoothCentralController
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.DeviceBluetoothState
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommand

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val debugLogger =
        object : DebugLogger {
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

    private val bluetoothCentralController =
        BluetoothCentralController(application, debugLogger)

    private val wirelessUartController: WirelessUartController =
        WirelessUartController(bluetoothCentralController, debugLogger).also {
            it.onChangeOperationStep = { operationStep ->
                operationStepState.value = operationStep
            }

            it.onChangeDeviceBluetoothState = { deviceBluetoothState ->
                this.deviceBluetoothState.value = deviceBluetoothState
            }
        }

    val deviceBluetoothState =
        mutableStateOf<DeviceBluetoothState>(
            bluetoothCentralController.currentDeviceBluetoothState
        )

    val operationStepState = mutableStateOf<OperationStep>(wirelessUartController.operationStep)

    fun onCreate(activity: ComponentActivity) {
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
    }

    val ifGrantedRuntimePermission = { wirelessUartController.start() }

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
