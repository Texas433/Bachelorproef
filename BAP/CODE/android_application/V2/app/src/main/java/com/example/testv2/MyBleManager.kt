import android.content.Context
import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data

class MyBleManager(context: Context) : BleManager(context) {

    var onDataReceived: ((String) -> Unit)? = null

    override fun getGattCallback(): BleManagerGattCallback {
        return object : BleManagerGattCallback() {
            override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                // Controleer hier op de services en characteristics die de ESP32 ondersteunt
                // Bijvoorbeeld het verkrijgen van een specifieke characteristic die je nodig hebt
                return true
            }

            override fun initialize() {
                // Stel notificaties in om data van de ESP32 te ontvangen
                setNotificationCallback(characteristic).with { _, data ->
                    val receivedString = data.getStringValue(0)
                    if (receivedString != null) {
                        onDataReceived?.invoke(receivedString)
                    }
                }
                enableNotifications(characteristic).enqueue()
            }

            override fun onDeviceDisconnected() {
                // Logica bij verbreking van verbinding
            }
        }
    }
}
