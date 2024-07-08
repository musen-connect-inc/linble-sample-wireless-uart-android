package com.musenconnect.linble.sample.wirelessuart.android.ui

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
import com.musenconnect.linble.sample.wirelessuart.android.app.MainActivity
import com.musenconnect.linble.sample.wirelessuart.android.model.OperationStep
import com.musenconnect.linble.sample.wirelessuart.android.model.WirelessUartController
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.DeviceBluetoothState

@Composable
fun MainView(
    deviceBluetoothState: State<DeviceBluetoothState>,
    operationStepState: State<OperationStep>,
    commandList: List<Pair<String, () -> Unit>>,
    runtimePermissionGranted: State<Boolean>,
    onStartRuntimePermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val targetLinbleAddress = WirelessUartController.targetLinbleAddress

        if (runtimePermissionGranted.value.not()) {
            Text("RuntimePermissionの取得", fontSize = 24.sp)
            Text("このアプリはBluetooth機能を使用します。Androidの制約上、Bluetoothの使用のためにはユーザーからの位置情報の利用許可が必要です。\n\n次に表示される位置情報機能の利用許可に関するシステムダイアログで、「許可」を選択してください。\n\n※このアプリは端末の位置情報を記録したり、外部に送信したりすることはありません。")

            Button(onClick = { onStartRuntimePermission() }) {
                Text("RuntimePermissionダイアログを表示")
            }
        } else {
            Text(
                text = when (deviceBluetoothState.value) {
                    DeviceBluetoothState.Unknown -> "不明"
                    DeviceBluetoothState.PoweredOff -> "OFF"
                    DeviceBluetoothState.PoweredOnButDisabledLocationService -> "位置情報機能が無効"

                    DeviceBluetoothState.PoweredOn -> "ON"
                }.let { "端末のBluetooth状態: $it" },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = if (deviceBluetoothState.value == DeviceBluetoothState.PoweredOn) Color.Blue else Color.Red,
            )
            Text(
                text = when (val action = operationStepState.value) {
                    OperationStep.Scanning -> "${targetLinbleAddress}を$action"
                    OperationStep.Connecting, OperationStep.Connected -> "${targetLinbleAddress}と$action"

                    else -> action.toString()
                },
                textAlign = TextAlign.Center,
                color = if (operationStepState.value == OperationStep.Connected) {
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
private fun Preview_MainView_connected() {
    Preview_MainView(granted = true, operationStep = OperationStep.Connected)
}

@Preview(showBackground = true)
@Composable
private fun Preview_MainView_scanning() {
    Preview_MainView(granted = true, operationStep = OperationStep.Scanning)
}

@Preview(showBackground = true)
@Composable
private fun Preview_MainView_notGranted() {
    Preview_MainView(granted = false)
}

@Composable
private fun Preview_MainView(
    granted: Boolean, operationStep: OperationStep = OperationStep.Initializing
) {
    val commandList = MainActivity.commands().map { it.first to {} }

    val bluetoothState by remember { mutableStateOf(DeviceBluetoothState.PoweredOn) }
    val operationStepState by remember { mutableStateOf(operationStep) }
    val grantedState by remember { mutableStateOf(granted) }

    MainView(
        deviceBluetoothState = remember { mutableStateOf(bluetoothState) },
        operationStepState = remember { mutableStateOf(operationStepState) },
        commandList = commandList,
        runtimePermissionGranted = remember { mutableStateOf(grantedState) },
        onStartRuntimePermission = {},
    )
}