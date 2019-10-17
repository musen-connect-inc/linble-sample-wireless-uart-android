package com.musenconnect.linble.sample.wirelessuart.android.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat

class RuntimePermissionHandler {
    companion object {
        // Bluetoothを使うために必要なPermissionは `Manifest.permission.ACCESS_FINE_LOCATION` です。
        // なお、Android 9までは `Manifest.permission.ACCESS_COARSE_LOCATION` でしたが、 Android 10からはFINEの方が必要になっています。
        private const val permissionToUseBluetooth = Manifest.permission.ACCESS_FINE_LOCATION

        const val requestCodeToGetRuntimePermissionToUseBluetooth = 3000
    }

    fun check(activity: Activity, ifGranted: () -> Unit) {
        /*
        AndroidでBluetoothを利用したアプリを動作させる場合、
        事前に位置情報に関するRuntime Permissionを獲得する必要があります。
         */

        val grantedAlready = ActivityCompat.checkSelfPermission(activity, permissionToUseBluetooth) == PackageManager.PERMISSION_GRANTED
        if (grantedAlready) {
            ifGranted.invoke()
        }
        else {
            requestRuntimePermissionToUseBluetooth(activity)
        }
    }

    private var runtimePermissionExplainDialog: AlertDialog? = null

    private var requestingNow = false

    private fun requestRuntimePermissionToUseBluetooth(activity: Activity) {
        if (requestingNow) {
            // 既に現在Runtime Permissionに関するダイアログが表示されている場合、
            // 二重に表示されてしまうのを防ぐためにここで処理を止めます。
            return
        }

        runtimePermissionExplainDialog = AlertDialog.Builder(activity)
            .setCancelable(false)
            .setTitle("RuntimePermissionの取得")
            .setMessage("このアプリはBluetooth機能を使用します。Androidの制約上、Bluetoothの使用のためにはユーザーからの位置情報の利用許可が必要です。\n\n次に表示される位置情報機能の利用許可に関するシステムダイアログで、「許可」を選択してください。\n\n※このアプリは端末の位置情報を記録したり、外部に送信したりすることはありません。")
            .setPositiveButton("OK") { _, _ ->
                requestingNow = true

                // Runtime Permission獲得に関するシステムダイアログを表示します。
                ActivityCompat.requestPermissions(activity, arrayOf(permissionToUseBluetooth), requestCodeToGetRuntimePermissionToUseBluetooth)

                // このダイアログの操作結果は
                // `Activity.onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)`
                // で返されます。
            }
            .show()
    }

    fun onRequestPermissionsResult(activity: Activity, requestCode: Int, permissions: Array<out String>, grantResults: IntArray, ifGranted: () -> Unit): ConsumedThisEvent {
        if (requestCode != requestCodeToGetRuntimePermissionToUseBluetooth) {
            return false
        }

        requestingNow = false

        val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            ifGranted.invoke()
        }
        else {
            /*
            「今後表示しない」にチェックされて許可されなかった場合、
            アプリはRuntime Permissionを再度ユーザーに要求することができなくなります。

            この状態になっているかは
            `ActivityCompat.shouldShowRequestPermissionRationale()`
            の返り値で判断できます。
            */
            val canRetry = ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionToUseBluetooth)
            if (canRetry) {
                requestRuntimePermissionToUseBluetooth(activity)
            }
            else {
                // この場合、Runtime Permissionに関するシステムダイアログが表示できないので、
                // Androidの設定アプリから直接位置情報の利用権限を修正してもらう必要があります。
                Toast.makeText(activity, "Androidの設定アプリを操作して、このアプリに位置情報の利用権限を与えてください。", Toast.LENGTH_LONG).show()

                // アプリとしてはエラー表示をしたり、 `Activity.finish()` を呼び出して強制終了するようにします。
                activity.finish()
            }
        }

        return true
    }

    fun clearExplainDialog() {
        runtimePermissionExplainDialog?.let {
            it.dismiss()
            runtimePermissionExplainDialog = null
        }
    }
}