package com.musenconnect.linble.sample.wirelessuart.android.app

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.musenconnect.linble.sample.wirelessuart.android.R
import com.musenconnect.linble.sample.wirelessuart.android.common.OperationStep
import com.musenconnect.linble.sample.wirelessuart.android.common.UartDataParserCallback
import com.musenconnect.linble.sample.wirelessuart.android.common.WirelessUartController
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.DeviceBluetoothState
import com.musenconnect.linble.sample.wirelessuart.android.common.uart.command.*
import com.musenconnect.linble.sample.wirelessuart.android.common.uart.datatype.AsciiString
import com.musenconnect.linble.sample.wirelessuart.android.common.uart.datatype.DurationIntervalSeconds
import com.musenconnect.linble.sample.wirelessuart.android.common.uart.datatype.RegisterNumber
import com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController
import com.musenconnect.linble.sample.wirelessuart.android.logTag
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var buttonsSendCommand: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(logTag, "onCreate")

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        buttonsSendCommand = listOf(
            buttonSendCommandConnectionTest.also {
                it.setOnClickListener {
                    sendCommand(UartCommandConnectionTest())
                }
            },

            buttonSendCommandRegisterRead.also {
                it.setOnClickListener {
                    sendCommand(UartCommandRegisterRead(RegisterNumber(1)))
                }
            },

            buttonSendCommandRegisterWrite.also {
                it.setOnClickListener {
                    sendCommand(UartCommandRegisterWrite(RegisterNumber(1), 0xAB.toByte()))
                }
            },

            buttonSendCommandVersionRead.also {
                it.setOnClickListener {
                    sendCommand(UartCommandVersionRead())
                }
            },

            buttonSendCommandSensorSampling.also {
                it.setOnClickListener {
                    sendCommand(UartCommandSensorSampling(DurationIntervalSeconds(5)))
                }
            },

            buttonSendCommandDeviceNameRead.also {
                it.setOnClickListener {
                    sendCommand(UartCommandDeviceNameRead())
                }
            },

            buttonSendCommandDeviceNameWrite.also {
                it.setOnClickListener {
                    sendCommand(UartCommandDeviceNameWrite(AsciiString("Sample-UartController-002")))
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()

        viewModel.uartDataParserCallback = object : UartDataParserCallback {
            override fun onParse(rxPacket: UartRxPacket) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "受信: $rxPacket", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.liveDataDeviceBluetoothState.observe(this, observerDeviceBluetoothState)
        viewModel.liveDataOperationStep.observe(this, observerOperationStep)

        viewModel.onStart(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        viewModel.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onStop() {
        viewModel.onStop()

        viewModel.liveDataDeviceBluetoothState.removeObserver(observerDeviceBluetoothState)
        viewModel.liveDataOperationStep.removeObserver(observerOperationStep)

        super.onStop()
    }

    private val observerDeviceBluetoothState = Observer<DeviceBluetoothState> { valueNullable ->
        val value = valueNullable ?: return@Observer

        when (value) {
            DeviceBluetoothState.Unknown -> "不明"
            DeviceBluetoothState.PoweredOff -> "OFF"
            DeviceBluetoothState.PoweredOnButDisabledLocationService -> "位置情報機能が無効"
            DeviceBluetoothState.PoweredOn -> "ON"
        }.let { "端末のBluetooth状態: $it" }.let { textViewDeviceBluetoothState.text = it }

        when (value) {
            DeviceBluetoothState.PoweredOn -> Color.BLUE
            else -> Color.RED
        }.let {
            textViewDeviceBluetoothState.setTextColor(it)
        }
    }

    private val observerOperationStep = Observer<OperationStep> { valueNullable ->
        val value = valueNullable ?: return@Observer

        when (value) {
            OperationStep.Scanning -> "${viewModel.targetLinbleAddress}を$value"
            OperationStep.Connecting, OperationStep.Connected -> "${viewModel.targetLinbleAddress}と$value"

            else -> value.toString()
        }.let {
            textViewOperationStep.text = it
        }

        when (value) {
            OperationStep.Connected -> Color.BLUE
            else -> Color.RED
        }.let {
            textViewOperationStep.setTextColor(it)
        }

        when (value) {
            OperationStep.Connected -> true
            else -> false
        }.let {
            buttonsSendCommand.forEach { button -> button.isEnabled = it }
        }
    }

    private fun sendCommand(uartCommand: UartCommand) {
        Toast.makeText(this@MainActivity, "送信: $uartCommand", Toast.LENGTH_LONG).show()

        viewModel.sendCommand(uartCommand)
    }
}
