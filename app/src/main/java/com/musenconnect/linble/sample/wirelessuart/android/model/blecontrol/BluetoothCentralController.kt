package com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.musenconnect.linble.sample.wirelessuart.android.description
import com.musenconnect.linble.sample.wirelessuart.android.logTag
import com.musenconnect.linble.sample.wirelessuart.android.model.DebugLogger
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule


typealias ProvideApplicationContext = () -> Context


@SuppressLint("MissingPermission")
class BluetoothCentralController(
    app: Application,
    private val debugLogger: DebugLogger,
) {
    var provideApplicationContext: ProvideApplicationContext? = null

    private val bluetoothManager =
        requireNotNull(app.getSystemService(BluetoothManager::class.java))
    private val bluetoothAdapter = requireNotNull(bluetoothManager.adapter)

    companion object {
        const val operationTimeoutMillis: Long = 5000
    }

    private var linbleSetupTimeoutDetector: TimerTask? = null

    // ..端末のBluetooth状態の監視..
    private var deviceBluetoothStateMonitoringCallback: DeviceBluetoothStateMonitoringCallback? =
        null
    var currentDeviceBluetoothState = DeviceBluetoothState.Unknown
        private set(value) {
            field = value
            debugLogger.logd(logTag, "currentDeviceBluetoothState.setter: value=$value")

            when (value) {
                DeviceBluetoothState.PoweredOff -> {
                    /*
                    AndroidのBLEでは、端末のBluetooth状態がオフになった場合、
                    処理中のオブジェクトからはエラー通知を起こしてくれません。
                    手動で全てのBLE処理を中断し、状態を巻き戻す必要があります。
                    */
                    writeOperationCallback?.onError(DeviceBluetoothStateErrorException())

                    cancelScan()
                    cancelConnection()
                }

                else -> {
                    // NOP
                }
            }

            deviceBluetoothStateMonitoringCallback?.onChange(value)
        }

    interface DeviceBluetoothStateMonitoringCallback {
        fun onChange(deviceBluetoothState: DeviceBluetoothState)
    }

    fun startDeviceBluetoothStateMonitoring(callback: DeviceBluetoothStateMonitoringCallback) {
        if (currentDeviceBluetoothState != DeviceBluetoothState.Unknown) {
            // 既に開始済みであれば処理を行わせないようにします。
            return
        }

        /*
        Androidでの端末のBluetooth状態監視には `BroadcastReceiver` を用います。

        `IntentFilter` には、
        端末のBluetooth状態監視用に `BluetoothAdapter.ACTION_STATE_CHANGED` 、
        位置情報機能の監視用に `LocationManager.PROVIDERS_CHANGED_ACTION` を取り扱わせます。
         */

        val provideApplicationContext = this.provideApplicationContext ?: return

        val applicationContext = provideApplicationContext()

        applicationContext.registerReceiver(bluetoothStateBroadcastReceiver, IntentFilter().also {
            it.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            it.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        })

        deviceBluetoothStateMonitoringCallback = callback

        updateCurrentDeviceBluetoothState(applicationContext)
    }

    private val bluetoothStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val localContext = context ?: return

            if (intent == null) {
                return
            }

            /*
            `BluetoothAdapter.ACTION_STATE_CHANGED` または `LocationManager.PROVIDERS_CHANGED_ACTION` が発生したら、
            現在の状態を確認し直して更新します。
            */
            updateCurrentDeviceBluetoothState(localContext)
        }
    }

    private fun updateCurrentDeviceBluetoothState(context: Context) {
        val bluetoothIsPoweredOn = bluetoothAdapter.isEnabled

        currentDeviceBluetoothState = if (bluetoothIsPoweredOn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DeviceBluetoothState.PoweredOn
            } else {
                /*
                Android12以前では、位置情報機能が有効になっているかもBLEスキャンの利用可否に繋がります。

                `LocationManager.NETWORK_PROVIDER` が有効になっていれば、BLEスキャンを利用可能と見なすことができます。
                */
                val locationManager = context.getSystemService(LocationManager::class.java)
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    DeviceBluetoothState.PoweredOn
                } else {
                    DeviceBluetoothState.PoweredOnButDisabledLocationService
                }
            }
        } else {
            DeviceBluetoothState.PoweredOff
        }
    }

    fun stopDeviceBluetoothStateMonitoring() {
        val provideApplicationContext = this.provideApplicationContext ?: return

        val applicationContext = provideApplicationContext()

        applicationContext.unregisterReceiver(bluetoothStateBroadcastReceiver)

        deviceBluetoothStateMonitoringCallback = null
    }

    // ..スキャン..
    private fun getBluetoothLeScanner(): BluetoothLeScanner? {
        return bluetoothAdapter.bluetoothLeScanner ?: run {
            // TODO: bluetoothLeScanner取得失敗時のエラー対応
            debugLogger.loge(this.logTag, "getBluetoothLeScanner: bluetoothLeScanner == null")

            return null
        }
    }

    interface ScanAdvertisementsCallback {
        fun onScanned(advertisement: Advertisement)
    }

    private var scanAdvertisementsCallback: ScanAdvertisementsCallback? = null
    private var bluetoothFrameworkScanCallback: ScanCallback? = null

    fun scanAdvertisements(callback: ScanAdvertisementsCallback) {
        when (currentDeviceBluetoothState) {
            DeviceBluetoothState.PoweredOn -> {
                debugLogger.logd(
                    logTag,
                    "scanAdvertisements: currentDeviceBluetoothState=$currentDeviceBluetoothState"
                )
            }

            else -> {
                // スキャンを開始できない状態であるため、処理を行わせないようにします。
                debugLogger.loge(
                    logTag,
                    "scanAdvertisements: ignored (currentDeviceBluetoothState=$currentDeviceBluetoothState)"
                )
                return
            }
        }

        scanAdvertisementsCallback = callback

        val bluetoothLeScanner = getBluetoothLeScanner() ?: return

        val bluetoothFrameworkScanCallback = object : ScanCallback() {
            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)

                debugLogger.loge(
                    logTag,
                    "onScanFailed: errorCode=$errorCode"
                )

                // TODO: スキャン開始失敗時のエラー対応
            }

            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                val deviceAddress = result?.device?.address ?: return

                /*
                コード簡略化のためここでは `BluetoothDevice.getName()` を利用してデバイス名を取得していますが、
                これはOSが認識しているデバイス名のキャッシュ情報であるため、
                実際にLINBLE側がリアルタイムで発信している名前と異なる可能性があります。

                LINBLEの名前を頻繁に変える使い方を想定している場合、
                `result.scanRecord.bytes` を自前で解析した方が安全です。
                */
                val deviceName = result.device?.name

                val advertisement = Advertisement(deviceName, deviceAddress)

                scanAdvertisementsCallback?.onScanned(advertisement)
            }
        }.also {
            bluetoothFrameworkScanCallback = it
        }

        /*
        Android8.1で `BluetoothLeScanner.startScan()` に `List<ScanFilter>` を渡さなかった場合、
        省電力設計のために、画面をオフにするとスキャンが止まるようになります。

        これを防ぐには「何もフィルタリングしないScanFilter」を設定します。
        */
        val scanFiltersToPreventScreenOffScanBlocking = listOf(ScanFilter.Builder().build())

        /*
        `SCAN_MODE_LOW_LATENCY` は端末の電池消費量が大きくなりますが、最速でアドバタイズを発見することができます。
        反対に、 `SCAN_MODE_LOW_POWER` は電池消費量が抑えられますが、アドバタイズ発見間隔が長くなります。
         */
        val scanSettings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()


        bluetoothLeScanner.startScan(
            scanFiltersToPreventScreenOffScanBlocking,
            scanSettings,
            bluetoothFrameworkScanCallback
        )

        /*
        TODO: 再スキャンの実装
        Androidでは30分間BLEスキャンを継続すると、自動的にそのスキャン処理は `SCAN_MODE_OPPORTUNISTIC` に格下げされます。
        この場合、他のアプリがスキャンを行ったときに連動してスキャン通知が上がるようになります。

        30分に到達する前に一旦スキャンを停止し、すぐに再度スキャンを開始することで、
        Android OS側の30分計測タイマーをリセットすることができます。
        */
    }

    fun cancelScan() {
        Log.w(logTag, "cancelScan")

        scanAdvertisementsCallback = null

        val bluetoothLeScanner = getBluetoothLeScanner() ?: return

        bluetoothFrameworkScanCallback?.let {
            bluetoothLeScanner.stopScan(it)

            bluetoothFrameworkScanCallback = null
        }
    }

    // ..GATT操作..
    interface LinbleSetupCallback {
        fun onError(reason: Throwable)

        fun onComplete()

        fun onNotify(notificationEvent: NotificationEvent)
    }

    // ..接続..
    private var linbleSetupCallback: LinbleSetupCallback? = null

    fun connect(
        target: Advertisement,
        linbleSetupCallback: LinbleSetupCallback
    ) {
        if (this.writeOperationCallback != null) {
            return
        }

        this.linbleSetupCallback = linbleSetupCallback

        debugLogger.logd(logTag, "connect: target=$target")

        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(target.deviceAddress)
        connect(bluetoothDevice)
    }

    private var _isConnected = isConnected

    val isConnected: Boolean
        get() = _isConnected

    var dataFromPeripheral: BluetoothGattCharacteristic? = null
    var dataFromPeripheralCccd: BluetoothGattDescriptor? = null

    var dataToPeripheral: BluetoothGattCharacteristic? = null

    private fun connect(target: BluetoothDevice) {
        if (linbleSetupCallback == null) {
            return
        }

        debugLogger.logd(logTag, "connect: target=$target")

        val provideApplicationContext = this.provideApplicationContext ?: run {
            linbleSetupCallback?.onError(IllegalStateException())
            return
        }

        val applicationContext = provideApplicationContext()

        /*
        第2引数 `autoConnect` をtrueにした場合、
        AndroidのBluetooth機能が対象との接続を何度も試みるようになります。
        ただし、接続性能が不安定になったり、アプリ上での制御が把握しづらくなるため、利用は非推奨です。
         */
        val autoConnect = false

        val bluetoothGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)

                connectionTimeoutDetector?.cancel()

                when (newState) {
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (!isConnected) {
                            retryConnection(target)
                        } else {
                            Log.e(logTag, "disconnected!")

                            // 接続完了後に切断が生じた場合、スキャンからやり直します。
                            linbleSetupCallback?.onError(DisconnectedAfterOnlineException())
                        }
                    }

                    BluetoothProfile.STATE_CONNECTED -> {
                        // AndroidはService検索とCharacteristics検索が一体になっています
                        val requested = gatt.discoverServices()
                        if (!requested) {
                            linbleSetupTimeoutDetector?.cancel()

                            linbleSetupCallback?.onError(
                                RequestFailureGattOperationException(
                                    "discoverServices"
                                )
                            )
                        }
                    }
                }
            }

            override fun onServicesDiscovered(serviceDiscoveredGatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(serviceDiscoveredGatt, status)

                linbleSetupTimeoutDetector?.cancel()

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    linbleSetupCallback?.onError(ResponseFailureGattOperationException("onServicesDiscovered"))
                    return
                }

                val succeeded = run {
                    val linbleUartService =
                        serviceDiscoveredGatt?.services?.firstOrNull { it.uuid == Linble.GattUuid.linbleUartService }
                            ?: return@run false
                    val characteristics = linbleUartService.characteristics ?: return@run false

                    dataFromPeripheral =
                        characteristics.firstOrNull { it.uuid == Linble.GattUuid.dataFromPeripheral }
                            ?: return@run false
                    dataFromPeripheralCccd =
                        dataFromPeripheral?.getDescriptor(BluetoothLowEnergySpec.GattUuid.cccd)
                            ?: return@run false
                    dataToPeripheral =
                        characteristics.firstOrNull { it.uuid == Linble.GattUuid.dataToPeripheral }
                            ?: return@run false

                    return@run true
                }

                if (!succeeded) {
                    linbleSetupCallback?.onError(UnsupportedDeviceConnectedException())
                    return
                }

                enableNotification()
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor?,
                status: Int
            ) {
                super.onDescriptorWrite(gatt, descriptor, status)

                linbleSetupTimeoutDetector?.cancel()

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    linbleSetupCallback?.onError(ResponseFailureGattOperationException("writeDescriptor"))
                    return
                }

                _isConnected = true

                linbleSetupCallback?.onComplete()
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {

                super.onCharacteristicChanged(gatt, characteristic)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return
                }

                val value = characteristic.value ?: return

                linbleSetupCallback?.onNotify(NotificationEvent(value))
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                super.onCharacteristicChanged(gatt, characteristic, value)

                linbleSetupCallback?.onNotify(NotificationEvent(value))
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)

                linbleSetupTimeoutDetector?.cancel()

                val writeOperationCallback = requireNotNull(writeOperationCallback)
                writeOperationCallback.onSuccess()
            }
        }

        connectionTimeoutDetector?.cancel()
        connectionTimeoutDetector = Timer().schedule(7000) { // 7秒以内に接続できなかったらタイムアウトと判定し、リトライを実施します。
            Log.e(logTag, "connection timeout!")

            retryConnection(target)
        }

        bluetoothGatt = target.connectGatt(applicationContext, autoConnect, bluetoothGattCallback)
    }

    private fun enableNotification() {
        debugLogger.logd(logTag, "enableNotification:")

        val callback = this.linbleSetupCallback
        requireNotNull(callback)

        val gatt = bluetoothGatt ?: run {
            return callback.onError(DisconnectedAfterOnlineException())
        }

        val dataFromPeripheral = this.dataFromPeripheral ?: run {
            return callback.onError(DisconnectedAfterOnlineException())
        }

        val dataFromPeripheralCccd = this.dataFromPeripheralCccd ?: run {
            return callback.onError(DisconnectedAfterOnlineException())
        }

        // AndroidでNotificationEnable操作をするためには、以下の2手順を行う必要があります。

        /*
        1. `BluetoothGatt.setCharacteristicNotification()` による、AndroidOSへのNotification受信イベント通知の許可

        対象キャラクタリスティックからNotificationが届いた場合、
        このアプリの `BluetoothGattCallback` にもNotification受信イベントを届けるよう、AndroidOSへ指示するための操作です。

        この操作はBLEフレームワーク内のフラグ制御のためにあるもので、これによるLINBLEとの通信はまだ発生しません。
         */
        var succeeded = gatt.setCharacteristicNotification(dataFromPeripheral, true)
        if (!succeeded) {
            callback.onError(RequestFailureGattOperationException("setCharacteristicNotification"))
            return
        }

        /*
        2. `BluetoothGatt.writeDescriptor()` による、LINBLEへのNotification発行の許可

        LINBLEと通信し、対象キャラクタリスティックからのNotification発行を実際に許可するための操作です。
         */
        dataFromPeripheralCccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

        linbleSetupTimeoutDetector = Timer().schedule(operationTimeoutMillis) {
            callback.onError(TimeoutGattOperationException("writeDescriptor"))
        }

        @Suppress("DEPRECATION")
        succeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = gatt.writeDescriptor(
                dataFromPeripheralCccd,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            )
            status == BluetoothGatt.GATT_SUCCESS
        } else {
            dataFromPeripheralCccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(dataFromPeripheralCccd)
        }
        if (!succeeded) {
            linbleSetupTimeoutDetector?.cancel()

            callback.onError(RequestFailureGattOperationException("writeDescriptor"))
        }
    }

    interface WriteOperationCallback {
        fun onError(reason: Throwable)

        fun onSuccess()
    }

    private var writeOperationCallback: WriteOperationCallback? = null

    @Suppress("DEPRECATION")
    fun write(data: ByteArray, writeOperationCallback: WriteOperationCallback) {
        debugLogger.logw(logTag, "write: data=${data.description()}")

        this.writeOperationCallback = writeOperationCallback

        val gatt = bluetoothGatt ?: run {
            return writeOperationCallback.onError(DisconnectedAfterOnlineException())
        }

        val dataToPeripheral = this.dataToPeripheral ?: run {
            return writeOperationCallback.onError(DisconnectedAfterOnlineException())
        }

        linbleSetupTimeoutDetector = Timer().schedule(operationTimeoutMillis) {
            writeOperationCallback.onError(TimeoutGattOperationException("writeCharacteristic"))
        }

        val succeeded =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val status = gatt.writeCharacteristic(
                    dataToPeripheral, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                debugLogger.logw(logTag, "writeCharacteristic: status=$status")

                status == BluetoothGatt.GATT_SUCCESS
            } else {
                dataToPeripheral.value = data

                val result = gatt.writeCharacteristic(dataToPeripheral)
                debugLogger.logw(logTag, "writeCharacteristic: result=$result")

                result
            }

        if (!succeeded) {
            linbleSetupTimeoutDetector?.cancel()

            writeOperationCallback.onError(RequestFailureGattOperationException("writeCharacteristic"))
        }
    }

    private fun retryConnection(target: BluetoothDevice) {
        /*
        AndroidのBLE接続APIはかなりの割合で失敗します。
        リトライ処理は必ず実装するようにしてください。

        ここではOS側で処理が進行している `BluetoothGatt` に `close()` を呼び出して放棄し、
        再度 `connectGatt()` を呼び出すことで新しく `BluetoothGatt` を作成し直します。
        */
        Log.w(logTag, "retryConnection: target=$target")

        bluetoothGatt?.close()
        bluetoothGatt = null

        connect(target)
    }

    private var connectionTimeoutDetector: TimerTask? = null

    private var bluetoothGatt: BluetoothGatt? = null

    fun cancelConnection() {
        debugLogger.logw(logTag, "cancelConnection")

        connectionTimeoutDetector?.cancel()

        dataFromPeripheral = null
        dataFromPeripheralCccd = null
        dataToPeripheral = null

        _isConnected = false
        writeOperationCallback = null

        bluetoothGatt?.close()  // `close()` を呼ぶことでBLE切断が実行されるとともに、全ての内部リソースが解放されます。
        bluetoothGatt = null
    }
}