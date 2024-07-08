package com.musenconnect.linble.sample.wirelessuart.android.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommand
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandConnectionTest
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandDeviceNameRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandDeviceNameWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandRegisterRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandRegisterWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandSensorSampling
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandVersionRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.AsciiString
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.DurationIntervalSeconds
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.RegisterNumber
import com.musenconnect.linble.sample.wirelessuart.android.ui.MainView
import com.musenconnect.linble.sample.wirelessuart.android.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val tag = "MainActivity"

    private val viewModel by viewModels<MainActivityViewModel>()

    private lateinit var runtimePermissionHandler: RuntimePermissionHandler

    companion object {
        fun commands(): List<Pair<String, UartCommand>> {
            return listOf(
                ("導通テスト" to UartCommandConnectionTest()),
                ("レジスタ値アクセス.取得" to UartCommandRegisterRead(RegisterNumber(1))),
                ("レジスタ値アクセス.設定" to UartCommandRegisterWrite(
                    RegisterNumber(1), 0xAB.toByte()
                )),
                ("ホストマイコンバージョン確認" to UartCommandVersionRead()),
                ("センササンプリング実行要求" to UartCommandSensorSampling(DurationIntervalSeconds(5))),
                ("デバイス名.取得" to UartCommandDeviceNameRead()),
                ("デバイス名.設定" to UartCommandDeviceNameWrite(AsciiString("Sample-UartController-002")))
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(tag, "onCreate")
        super.onCreate(savedInstanceState)

        runtimePermissionHandler = RuntimePermissionHandler(activity = this, onGranted = {
            viewModel.start()
        })
        runtimePermissionHandler.check()

        val commandList = commands().map {
            it.first to { sendCommand(it.second) }
        }

        setContent {
            AppTheme {
                MainView(
                    deviceBluetoothState = viewModel.deviceBluetoothState,
                    operationStepState = viewModel.operationStepState,
                    commandList = commandList,
                    runtimePermissionGranted = runtimePermissionHandler.granted,
                    onStartRuntimePermission = {
                        runtimePermissionHandler.request()
                    },
                )
            }
        }
    }

    private fun sendCommand(uartCommand: UartCommand) {
        Toast.makeText(this@MainActivity, "送信: $uartCommand", Toast.LENGTH_LONG).show()

        viewModel.sendCommand(uartCommand)
    }
}
