package com.arnauquest.parnaugo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

class MqttService : Service() {
    private var mqttClient: MqttClient? = null
    private val CHANNEL_ID = "MqttServiceChannel"
    private val MSG_CHANNEL_ID = "MqttMessageChannel"
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val brokerUrl = intent?.getStringExtra("brokerUrl") ?: return START_NOT_STICKY
        val topics = intent.getStringArrayExtra("topics") ?: emptyArray()

        val notification = createForegroundNotification()
        startForeground(1, notification)

        connectToMqtt(brokerUrl, topics)

        return START_STICKY
    }

    private fun connectToMqtt(brokerUrl: String, topics: Array<String>) {
        try {
            if (mqttClient != null && mqttClient!!.isConnected) {
                mqttClient!!.disconnect()
            }
            
            val serverUri = if (brokerUrl.startsWith("tcp://") || brokerUrl.startsWith("ssl://") || brokerUrl.startsWith("ws://")) brokerUrl else "tcp://$brokerUrl"
            val clientId = UUID.randomUUID().toString()
            mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                connectionTimeout = 10
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MqttService", "Connection lost", cause)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("MqttService", "Message arrived: $topic -> ${message?.toString()}")
                    showNotification("Nuevo mensaje en $topic", message?.toString() ?: "")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            mqttClient?.connect(options)
            
            if (topics.isNotEmpty()) {
                val qos = IntArray(topics.size) { 1 }
                mqttClient?.subscribe(topics, qos)
            }
        } catch (e: Exception) {
            Log.e("MqttService", "Error connecting to MQTT", e)
        }
    }

    private fun createNotificationChannels() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Servicio MQTT",
            NotificationManager.IMPORTANCE_LOW
        )
        val msgChannel = NotificationChannel(
            MSG_CHANNEL_ID,
            "Mensajes MQTT",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
        manager?.createNotificationChannel(msgChannel)
    }

    private fun createForegroundNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Suscrito a MQTT")
            .setContentText("Escuchando actualizaciones en segundo plano")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showNotification(title: String, message: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, MSG_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        try {
            mqttClient?.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
