package com.musenconnect.linble.sample.wirelessuart.android.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat

class RuntimePermissionHandler(
    private val activity: ComponentActivity,
    private val onGranted: () -> Unit
) {
    companion object {
        // Bluetoothを使うために必要なPermissionはAndroid12以降かそれ未満かで変わります。
        private val permissionsToUseBluetooth =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android12以降では機能ごとに3つにPermissionが分かれており、必要な機能の権限のみ必要となります。
                // 今回はスキャンと接続を行うので、 Manifest.permission.BLUETOOTH_SCANとManifest.permission.BLUETOOTH_CONNECTが必要になります。
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                // Android12未満ではManifest.permission.ACCESS_FINE_LOCATIONが必要になります。
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }

    val granted = mutableStateOf<Boolean>(false)

    fun check() {
        // AndroidでBluetoothを利用したアプリを動作させる場合、
        // 事前に位置情報に関するRuntime Permissionを獲得する必要があります。
        val grantedAlready = permissionsToUseBluetooth.all { permission ->
            ActivityCompat.checkSelfPermission(
                activity, permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        granted.value = grantedAlready

        if (grantedAlready) {
            onGranted()
        }
    }

    private val launcher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionMap ->
            val granted = permissionMap.entries.all {
                it.value
            }

            if (granted) {
                this.granted.value = true
                onGranted()
            } else {
                this.granted.value = false

                // 「今後表示しない」にチェックされて許可されなかった場合、
                // アプリはRuntime Permissionを再度ユーザーに要求することができなくなります。
                //
                // この状態になっているかは
                // `ActivityCompat.shouldShowRequestPermissionRationale()`
                // の返り値で判断できます。
                val canRetry = permissionsToUseBluetooth.all { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, permission
                    )
                }
                if (canRetry) {
                    request()
                } else {
                    // この場合、Runtime Permissionに関するメッセージが表示できないので、
                    // Androidの設定アプリから直接位置情報の利用権限を修正してもらう必要があります。
                    Toast.makeText(
                        activity,
                        "Androidの設定アプリを操作して、このアプリに位置情報の利用権限を与えてください。",
                        Toast.LENGTH_LONG
                    ).show()

                    // アプリとしてはエラー表示をしたり、 `Activity.finish()` を呼び出して強制終了するようにします。
                    activity.finish()
                }

                return@registerForActivityResult
            }
        }

    fun request() {
        launcher.launch(permissionsToUseBluetooth)
    }
}
