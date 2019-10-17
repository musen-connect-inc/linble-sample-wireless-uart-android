package com.musenconnect.linble.sample.wirelessuart.android.concrete

import android.bluetooth.*
import android.os.Build
import android.util.Log
import com.musenconnect.linble.sample.wirelessuart.android.logTag
import java.util.concurrent.CopyOnWriteArraySet

/**
 * BluetoothGattCallbackの受け渡し役.
 *
 * Android BLE フレームワークでは、
 * BLE接続時に指定したGATTイベント受け取り先を
 * 後から自由に変更することができません。
 *
 * このクラスではその制約を解消します。
 */
class BluetoothGattCallbackBridger: BluetoothGattCallback() {
    private var bridges = CopyOnWriteArraySet<BluetoothGattCallback>()

    fun register(bluetoothGattCallback: BluetoothGattCallback) {
        bridges.add(bluetoothGattCallback)
        Log.w(logTag, "register: bridges.size=${bridges.size}")
    }

    fun unregister(bluetoothGattCallback: BluetoothGattCallback) {
        bridges.remove(bluetoothGattCallback)
        Log.w(logTag, "unregister: bridges.size=${bridges.size}")
    }

    fun unregisterAll() {
        bridges.clear()
        Log.w(logTag, "unregisterAll: bridges.size=${bridges.size}")
    }


    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        Log.d(logTag, "onConnectionStateChange: gatt=${gatt?.device}, status=$status, newState=$newState (${newStateString(newState)}) --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onConnectionStateChange(gatt, status, newState) }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.d(logTag, "onServicesDiscovered: gatt=${gatt?.device}, status=$status --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onServicesDiscovered(gatt, status) }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)
        Log.d(logTag, "onDescriptorWrite: gatt=${gatt?.device}, descriptor=${descriptor?.uuid}, status=$status --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onDescriptorWrite(gatt, descriptor, status) }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        Log.d(logTag, "onCharacteristicWrite: gatt=${gatt?.device}, characteristic=${characteristic?.uuid}, status=$status --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onCharacteristicWrite(gatt, characteristic, status) }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        super.onCharacteristicChanged(gatt, characteristic)
        Log.d(logTag, "onCharacteristicChanged: gatt=${gatt?.device}, characteristic=${characteristic?.uuid} --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onCharacteristicChanged(gatt, characteristic) }
    }


    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        Log.d(logTag, "onMtuChanged: gatt=${gatt?.device}, mtu=$mtu, status=$status --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onMtuChanged(gatt, mtu, status) }
    }

    override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        super.onPhyRead(gatt, txPhy, rxPhy, status)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(logTag, "onPhyRead: gatt=${gatt?.device}, txPhy=$txPhy, rxPhy=$rxPhy, status=$status --> bridge to ${bridges.size} callbacks")
            bridges.forEach { it.onPhyRead(gatt, txPhy, rxPhy, status) }
        }
    }

    override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        super.onPhyUpdate(gatt, txPhy, rxPhy, status)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(logTag, "onPhyUpdate: gatt=${gatt?.device}, txPhy=$txPhy, rxPhy=$rxPhy, status=$status --> bridge to ${bridges.size} callbacks")
            bridges.forEach { it.onPhyUpdate(gatt, txPhy, rxPhy, status) }
        }
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        Log.d(logTag, "onCharacteristicRead: gatt=${gatt?.device}, characteristic=${characteristic?.uuid}, status=$status --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onCharacteristicRead(gatt, characteristic, status) }
    }

    override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorRead(gatt, descriptor, status)
        Log.d(logTag, "onDescriptorRead: gatt=${gatt?.device}, descriptor=${descriptor?.uuid}, status=$status --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onDescriptorRead(gatt, descriptor, status) }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        Log.d(logTag, "onReadRemoteRssi: gatt=${gatt?.device}, rssi=$rssi, status=$status, status=$status --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onReadRemoteRssi(gatt, rssi, status) }
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
        super.onReliableWriteCompleted(gatt, status)
        Log.d(logTag, "onReliableWriteCompleted: gatt=${gatt?.device}, status=$status --> bridge to ${bridges.size} callbacks")
        bridges.forEach { it.onReliableWriteCompleted(gatt, status) }
    }


    companion object {
        fun newStateString(newState: Int): String {
            return when (newState) {
                BluetoothProfile.STATE_CONNECTED -> "STATE_CONNECTED"
                BluetoothProfile.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "STATE_CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
                else -> "STATE_UNKNOWN<$newState>"
            }
        }
    }
}