package com.example.waterpumpcontrol;

import android.content.Context;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * MqttHandler encapsulates MQTT connect, subscribe, publish, disconnect, and additional functions.
 */
public class MqttHandler {
    private MqttClient mqttClient;

    /**
     * Connect to MQTT broker with SSL and credentials.
     * @param context   Android context (not used by MqttClient but kept for signature consistency).
     * @param brokerUri URI of broker, e.g. "ssl://broker.address:8883".
     * @param username  MQTT username.
     * @param password  MQTT password.
     */
    public void connect(Context context, String brokerUri, String username, String password) {
        String clientId = MqttClient.generateClientId();
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            mqttClient = new MqttClient(brokerUri, clientId, persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            mqttClient.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reconnect to MQTT broker.
     */
    public void reconnect() {
        if (mqttClient == null || mqttClient.isConnected()) return;
        try {
            mqttClient.reconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Subscribe to a topic at QoS 0.
     * @param topic MQTT topic to subscribe.
     */
    public void subscribe(String topic) {
        subscribe(topic, 0);
    }

    /**
     * Subscribe to a topic with specified QoS.
     * @param topic MQTT topic.
     * @param qos   Quality of Service level (0, 1, or 2).
     */
    public void subscribe(String topic, int qos) {
        if (mqttClient == null || !mqttClient.isConnected()) return;
        try {
            mqttClient.subscribe(topic, qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unsubscribe from a topic.
     * @param topic MQTT topic to unsubscribe.
     */
    public void unsubscribe(String topic) {
        if (mqttClient == null || !mqttClient.isConnected()) return;
        try {
            mqttClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Publish a message to a topic with default QoS 0 and non-retained.
     * @param topic   MQTT topic.
     * @param message Payload string.
     */
    public void publish(String topic, String message) {
        publish(topic, message, 0, false);
    }

    /**
     * Publish a message with QoS and retained flag.
     * @param topic    MQTT topic.
     * @param message  Payload string.
     * @param qos      Quality of Service (0,1,2).
     * @param retained Whether the broker should retain this message.
     */
    public void publish(String topic, String message, int qos, boolean retained) {
        if (mqttClient == null || !mqttClient.isConnected()) return;
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            mqttMessage.setRetained(retained);
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Disconnect from the MQTT broker.
     */
    public void disconnect() {
        if (mqttClient == null || !mqttClient.isConnected()) return;
        try {
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if the client is currently connected.
     * @return True if connected, false otherwise.
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * Set callback to handle connection, incoming messages, delivery completion, etc.
     * @param callback Implementation of MqttCallback.
     */
    public void setCallback(MqttCallback callback) {
        if (mqttClient == null) return;
        mqttClient.setCallback(callback);
    }

}
