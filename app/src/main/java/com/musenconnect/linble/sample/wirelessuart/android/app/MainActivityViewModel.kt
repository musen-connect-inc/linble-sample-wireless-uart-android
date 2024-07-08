package com.musenconnect.linble.sample.wirelessuart.android.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.musenconnect.linble.sample.wirelessuart.android.model.AndroidDebugLogger
import com.musenconnect.linble.sample.wirelessuart.android.model.DebugLogger
import com.musenconnect.linble.sample.wirelessuart.android.model.OperationStep
import com.musenconnect.linble.sample.wirelessuart.android.model.WirelessUartController
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.BluetoothCentralController
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.DeviceBluetoothState
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommand

class MainActivityViewModel(private val application: Application) : AndroidViewModel(application) {
    private val debugLogger: DebugLogger = AndroidDebugLogger

    // Androidの多くのBLE APIでは、 `Context` オブジェクトが必要になります。
    // これはOS側でのアプリケーションのプロセス識別のために用いられます。
    //
    // BLE API に渡す `Context` には Application Context 系のオブジェクトを使うようにしてください。
    // `Application` オブジェクト、または `Activity.applicationContext` が適切です。
    //
    // `Activity` オブジェクトも `Context` として使用することができますが、
    // 基本的に `Activity` は短命であるため、これを外部に差し出すようなコードはメモリリークを招く原因になります。
    private val bluetoothCentralController =
        BluetoothCentralController(application, debugLogger)

    private val wirelessUartController: WirelessUartController =
        WirelessUartController(
            bluetoothCentralController,
            debugLogger,
            onChangeOperationStep = { operationStep -> operationStepState.value = operationStep },
            onChangeDeviceBluetoothState = { deviceBluetoothState ->
                this.deviceBluetoothState.value = deviceBluetoothState
            },
            onParse = { rxPacket ->
                mainThreadHandler.post {
                    Toast.makeText(application, "受信: $rxPacket", Toast.LENGTH_LONG).show()
                }
            }
        )

    val deviceBluetoothState =
        mutableStateOf<DeviceBluetoothState>(
            bluetoothCentralController.currentDeviceBluetoothState
        )

    val operationStepState = mutableStateOf<OperationStep>(wirelessUartController.operationStep)

    fun start() {
        wirelessUartController.start()
    }

    fun sendCommand(uartCommand: UartCommand) {
        wirelessUartController.write(uartCommand)
    }

    override fun onCleared() {
        wirelessUartController.stop()
        super.onCleared()
    }

    private val mainThreadHandler = Handler(Looper.getMainLooper())
}
