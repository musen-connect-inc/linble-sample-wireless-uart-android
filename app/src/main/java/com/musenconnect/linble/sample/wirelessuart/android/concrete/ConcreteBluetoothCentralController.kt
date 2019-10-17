package com.musenconnect.linble.sample.wirelessuart.android.concrete

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.musenconnect.linble.sample.wirelessuart.android.common.DebugLogger
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.Advertisement
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.BluetoothCentralController
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.DeviceBluetoothState
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.Linble
import com.musenconnect.linble.sample.wirelessuart.android.logTag
import java.util.*
import kotlin.concurrent.schedule

typealias ProvideApplicationContext = () -> Context

typealias CreateLinble = (gatt: BluetoothGatt, bridger: BluetoothGattCallbackBridger) -> Linble

class ConcreteBluetoothCentralController(
    private val debugLogger: DebugLogger,
    private val createLinble: CreateLinble
): BluetoothCentralController {
    var provideApplicationContext: ProvideApplicationContext? = null


    // ..端末のBluetooth状態の監視..
    private var deviceBluetoothStateMonitoringCallback: BluetoothCentralController.DeviceBluetoothStateMonitoringCallback? = null

    override var currentDeviceBluetoothState = DeviceBluetoothState.Unknown
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
                    connectCallback?.onError(DeviceBluetoothStateErrorException())

                    cancelScan()
                    cancelConnection()
                }

                else -> {
                    // NOP
                }
            }

            deviceBluetoothStateMonitoringCallback?.onChange(value)
        }

    override fun startDeviceBluetoothStateMonitoring(callback: BluetoothCentralController.DeviceBluetoothStateMonitoringCallback) {
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
        val bluetoothIsPoweredOn = BluetoothAdapter.getDefaultAdapter().isEnabled

        currentDeviceBluetoothState = if (bluetoothIsPoweredOn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                /*
                Android6以降では、位置情報機能が有効になっているかもBLEスキャンの利用可否に繋がります。

                `LocationManager.NETWORK_PROVIDER` が有効になっていれば、BLEスキャンを利用可能と見なすことができます。
                */
                val locationManager = context.getSystemService(LocationManager::class.java)
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    DeviceBluetoothState.PoweredOn
                }
                else {
                    DeviceBluetoothState.PoweredOnButDisabledLocationService
                }
            }
            else {
                DeviceBluetoothState.PoweredOn
            }
        }
        else {
            DeviceBluetoothState.PoweredOff
        }
    }

    override fun stopDeviceBluetoothStateMonitoring() {
        val provideApplicationContext = this.provideApplicationContext ?: return

        val applicationContext = provideApplicationContext()

        applicationContext.unregisterReceiver(bluetoothStateBroadcastReceiver)

        deviceBluetoothStateMonitoringCallback = null
    }


    // ..スキャン..

    private var scanAdvertisementsCallback: BluetoothCentralController.ScanAdvertisementsCallback? = null
    private var bluetoothFrameworkScanCallback: ScanCallback? = null

    private fun getBluetoothLeScanner(): BluetoothLeScanner? {
        return BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner ?: run {
            // TODO: bluetoothLeScanner取得失敗時のエラー対応
            debugLogger.loge(this@ConcreteBluetoothCentralController.logTag, "getBluetoothLeScanner: bluetoothLeScanner == null")

            return null
        }
    }

    override fun scanAdvertisements(callback: BluetoothCentralController.ScanAdvertisementsCallback) {
        when (currentDeviceBluetoothState) {
            DeviceBluetoothState.PoweredOn -> {
                debugLogger.logd(logTag, "scanAdvertisements: currentDeviceBluetoothState=$currentDeviceBluetoothState")
            }
            else -> {
                // スキャンを開始できない状態であるため、処理を行わせないようにします。
                debugLogger.loge(logTag, "scanAdvertisements: ignored (currentDeviceBluetoothState=$currentDeviceBluetoothState)")
                return
            }
        }

        scanAdvertisementsCallback = callback

        val bluetoothLeScanner = getBluetoothLeScanner() ?: return

        val bluetoothFrameworkScanCallback = object : ScanCallback() {
            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)

                debugLogger.loge(this@ConcreteBluetoothCentralController.logTag, "onScanFailed: errorCode=$errorCode")

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
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()


        bluetoothLeScanner.startScan(scanFiltersToPreventScreenOffScanBlocking, scanSettings, bluetoothFrameworkScanCallback)


        /*
        TODO: 再スキャンの実装
        Androidでは30分間BLEスキャンを継続すると、自動的にそのスキャン処理は `SCAN_MODE_OPPORTUNISTIC` に格下げされます。
        この場合、他のアプリがスキャンを行ったときに連動してスキャン通知が上がるようになります。

        30分に到達する前に一旦スキャンを停止し、すぐに再度スキャンを開始することで、
        Android OS側の30分計測タイマーをリセットすることができます。
        */
    }

    override fun cancelScan() {
        Log.w(this@ConcreteBluetoothCentralController.logTag, "cancelScan")

        scanAdvertisementsCallback = null

        val bluetoothLeScanner = getBluetoothLeScanner() ?: return

        bluetoothFrameworkScanCallback?.let {
            bluetoothLeScanner.stopScan(it)

            bluetoothFrameworkScanCallback = null
        }
    }


    // ..接続..

    private var connectCallback: Linble.GattOperationCallback? = null

    override fun connect(target: Advertisement, callback: Linble.GattOperationCallback) {
        if (connectCallback != null) {
            return
        }

        connectCallback = callback

        debugLogger.logd(this@ConcreteBluetoothCentralController.logTag, "connect: target=$target")

        val bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(target.deviceAddress)
        connect(bluetoothDevice)
    }

    private var connected: Linble? = null
        private set(value) {
            field = value

            if (value != null) {
                Log.w(this@ConcreteBluetoothCentralController.logTag, "connected.setter: connected")
            }
            else {
                Log.e(this@ConcreteBluetoothCentralController.logTag, "connected.setter: disconnected")
            }
        }

    override val isConnected: Boolean
        get() = connected != null

    private fun connect(target: BluetoothDevice) {
        if (connectCallback == null) {
            return
        }

        debugLogger.logd(this@ConcreteBluetoothCentralController.logTag, "connect: target=$target")

        val provideApplicationContext = this.provideApplicationContext ?: run {
            connectCallback?.onError(IllegalStateException())
            return
        }

        val applicationContext = provideApplicationContext()

        /*
        第2引数 `autoConnect` をtrueにした場合、
        AndroidのBluetooth機能が対象との接続を何度も試みるようになります。
        ただし、接続性能が不安定になったり、アプリ上での制御が把握しづらくなるため、利用は非推奨です。
         */
        val autoConnect = false

        val currentBridger = BluetoothGattCallbackBridger()
        val bluetoothGattCallback = currentBridger.also { bridger ->
            bridger.register(object: BluetoothGattCallback() {
                private val self = this

                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    super.onConnectionStateChange(gatt, status, newState)

                    connectionTimeoutDetector?.cancel()

                    when (newState) {
                        BluetoothProfile.STATE_DISCONNECTED -> retryConnection(target)

                        BluetoothProfile.STATE_CONNECTED -> {
                            bridger.register(object : BluetoothGattCallback() {
                                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                                    super.onConnectionStateChange(gatt, status, newState)

                                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                        Log.e(this@ConcreteBluetoothCentralController.logTag, "disconnected!")

                                        // 接続完了後に切断が生じた場合、スキャンからやり直します。
                                        connectCallback?.onError(DisconnectedAfterOnlineException())
                                    }
                                }
                            })

                            bridger.unregister(self)

                            val connectedLinble = createLinble(gatt!!, bridger).also { connected = it }
                            /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteLinble */

                            connectCallback?.onSuccess(connectedLinble)
                        }
                    }
                }
            })
        }

        connectionTimeoutDetector?.cancel()
        connectionTimeoutDetector = Timer().schedule(7000) { // 7秒以内に接続できなかったらタイムアウトと判定し、リトライを実施します。
            Log.e(this@ConcreteBluetoothCentralController.logTag, "connection timeout!")

            retryConnection(target)
        }

        bluetoothGatt = target.connectGatt(applicationContext, autoConnect, bluetoothGattCallback)
    }

    private fun retryConnection(target: BluetoothDevice) {
        /*
        AndroidのBLE接続APIはかなりの割合で失敗します。
        リトライ処理は必ず実装するようにしてください。

        ここではOS側で処理が進行している `BluetoothGatt` に `close()` を呼び出して放棄し、
        再度 `connectGatt()` を呼び出すことで新しく `BluetoothGatt` を作成し直します。
        */
        Log.w(this@ConcreteBluetoothCentralController.logTag, "retryConnection: target=$target")

        bluetoothGatt?.close()
        bluetoothGatt = null

        connect(target)
    }

    private var connectionTimeoutDetector: TimerTask? = null

    private var bluetoothGatt: BluetoothGatt? = null

    override fun cancelConnection() {
        debugLogger.logw(logTag, "cancelConnection")

        connectionTimeoutDetector?.cancel()

        connected = null
        connectCallback = null

        bluetoothGatt?.close()  // `close()` を呼ぶことでBLE切断が実行されるとともに、全ての内部リソースが解放されます。
        bluetoothGatt = null
    }
}
