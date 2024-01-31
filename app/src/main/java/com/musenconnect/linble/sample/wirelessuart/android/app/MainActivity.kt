package com.musenconnect.linble.sample.wirelessuart.android.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musenconnect.linble.sample.wirelessuart.android.model.OperationStep
import com.musenconnect.linble.sample.wirelessuart.android.model.UartDataParserCallback
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.DeviceBluetoothState
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommand
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandConnectionTest
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandDeviceNameRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandDeviceNameWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandRegisterRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandRegisterWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandSensorSampling
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandVersionRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartRxPacket
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.AsciiString
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.DurationIntervalSeconds
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.RegisterNumber
import com.musenconnect.linble.sample.wirelessuart.android.ui.theme.LinbleSampleWirelessUartTheme

class MainActivity : ComponentActivity() {
    private val tag = "MainActivity"

    private val viewModel by viewModels<MainActivityViewModel>()

    private lateinit var runtimePermissionHandler: RuntimePermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(tag, "onCreate")

        super.onCreate(savedInstanceState)

        runtimePermissionHandler = RuntimePermissionHandler(
            this,
            viewModel.ifGrantedRuntimePermission
        )

        viewModel.uartDataParserCallback =
            object : UartDataParserCallback {
                override fun onParse(rxPacket: UartRxPacket) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "受信: $rxPacket", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }

        viewModel.onCreate(this)
        runtimePermissionHandler.check()

        val commandList =
            listOf(
                ("導通テスト" to UartCommandConnectionTest()),
                ("レジスタ値アクセス.取得" to UartCommandRegisterRead(RegisterNumber(1))),
                ("レジスタ値アクセス.設定" to UartCommandRegisterWrite(RegisterNumber(1), 0xAB.toByte())),
                ("ホストマイコンバージョン確認" to UartCommandVersionRead()),
                ("センササンプリング実行要求" to UartCommandSensorSampling(DurationIntervalSeconds(5))),
                ("デバイス名.取得" to UartCommandDeviceNameRead()),
                ("デバイス名.設定" to UartCommandDeviceNameWrite(AsciiString("Sample-UartController-002")))
            ).map {
                it.first to { sendCommand(it.second) }
            }

        setContent {
            LinbleSampleWirelessUartTheme {
                MainActivityView(
                    deviceBluetoothState = viewModel.deviceBluetoothState,
                    operationStepState = viewModel.operationStepState,
                    targetLinbleAddress = viewModel.targetLinbleAddress,
                    commandList = commandList,
                    grantedAlreadyRuntimePermission = runtimePermissionHandler.grantedAlreadyRuntimePermission,
                    onStartRuntimePermission = {
                        runtimePermissionHandler.requestRuntimePermissionToUseBluetooth()
                    },
                )
            }
        }
    }


    override fun onStop() {
        viewModel.onStop()

        super.onStop()
    }

    private fun sendCommand(uartCommand: UartCommand) {
        Toast.makeText(this@MainActivity, "送信: $uartCommand", Toast.LENGTH_LONG).show()

        viewModel.sendCommand(uartCommand)
    }
}

@Composable
fun MainActivityView(
    deviceBluetoothState: State<DeviceBluetoothState>,
    operationStepState: State<OperationStep>,
    targetLinbleAddress: String,
    commandList: List<Pair<String, () -> Unit>>,
    grantedAlreadyRuntimePermission: State<Boolean>,
    onStartRuntimePermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        if (grantedAlreadyRuntimePermission.value.not()) {
            Text("RuntimePermissionの取得", fontSize = 24.sp)
            Text("このアプリはBluetooth機能を使用します。Androidの制約上、Bluetoothの使用のためにはユーザーからの位置情報の利用許可が必要です。\n\n次に表示される位置情報機能の利用許可に関するシステムダイアログで、「許可」を選択してください。\n\n※このアプリは端末の位置情報を記録したり、外部に送信したりすることはありません。")

            Button(onClick = { onStartRuntimePermission() }) {
                Text("RuntimePermissionダイアログを表示")
            }
        } else {


            Text(
                text =
                when (deviceBluetoothState.value) {
                    DeviceBluetoothState.Unknown -> "不明"
                    DeviceBluetoothState.PoweredOff -> "OFF"
                    DeviceBluetoothState.PoweredOnButDisabledLocationService ->
                        "位置情報機能が無効"

                    DeviceBluetoothState.PoweredOn -> "ON"
                }.let { "端末のBluetooth状態: $it" },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color =
                if (deviceBluetoothState.value == DeviceBluetoothState.PoweredOn) {
                    Color.Blue
                } else {
                    Color.Red
                },
            )
            Text(
                text =
                when (val action = operationStepState.value) {
                    OperationStep.Scanning -> "${targetLinbleAddress}を$action"
                    OperationStep.Connecting, OperationStep.Connected ->
                        "${targetLinbleAddress}と$action"

                    else -> action.toString()
                },
                textAlign = TextAlign.Center,
                color =
                if (operationStepState.value == OperationStep.Connected) {
                    Color.Blue
                } else {
                    Color.Red
                },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                items(items = commandList) {
                    val (text, onClick) = it
                    ElevatedButton(
                        onClick = onClick,
                        enabled = operationStepState.value == OperationStep.Connected,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(text) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_ScanningView() {
    val commandList =
        listOf(
            ("導通テスト" to {}),
            ("レジスタ値アクセス.取得" to {}),
            ("レジスタ値アクセス.設定" to {}),
            ("ホストマイコンバージョン確認" to {}),
            ("センササンプリン実行要求" to {}),
            ("デバイス名.取得" to {}),
            ("デバイス名.設定" to {})
        )

    val bluetoothState by remember { mutableStateOf(DeviceBluetoothState.Unknown) }
    val operationStepState by remember { mutableStateOf(OperationStep.Scanning) }
    val grantedState by remember { mutableStateOf(false) }

    MainActivityView(
        deviceBluetoothState = remember { mutableStateOf(bluetoothState) },
        operationStepState = remember { mutableStateOf(operationStepState) },
        targetLinbleAddress = "FF:FF:FF:FF:FF:FF",
        commandList = commandList,
        grantedAlreadyRuntimePermission = remember { mutableStateOf(grantedState) },
        onStartRuntimePermission = {},
    )
}
