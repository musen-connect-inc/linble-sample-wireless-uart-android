package com.musenconnect.linble.sample.wirelessuart.android.concrete

class DeviceBluetoothStateErrorException: IllegalStateException()

class DisconnectedAfterOnlineException: IllegalStateException()

class UnsupportedDeviceConnectedException(): IllegalStateException()


abstract class FailureGattOperationException(type: String, methodName: String): IllegalStateException("failed $methodName $type")


class RequestFailureGattOperationException(methodName: String): FailureGattOperationException("request", methodName)

class ResponseFailureGattOperationException(methodName: String): FailureGattOperationException("response", methodName)

class TimeoutGattOperationException(methodName: String): FailureGattOperationException("timeout", methodName)
