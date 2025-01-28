#include <ArduinoBLE.h>

// Define the BLE service and characteristic
BLEService customService("f3b4f9a8-25b8-4ee1-8b69-0a61a964de15"); // Custom service UUID
BLECharacteristic customCharacteristic("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c3", BLERead | BLEWrite | BLENotify, 20); // Custom characteristic UUID, read/write/notify, 20 byte size

void setup() {
    // Start serial communication for debugging
    Serial.begin(9600);
    while (!Serial);

    // Initialize the built-in LED pin
    pinMode(LED_BUILTIN, OUTPUT);

    // Start Bluetooth Low Energy (BLE) radio
    if (!BLE.begin()) {
        Serial.println("Starting BLE failed!");
        while (1);
    }

    // Set BLE device name
    BLE.setLocalName("ADAPT");

    // Add the characteristic to the service
    customService.addCharacteristic(customCharacteristic);

    // Add the service
    BLE.addService(customService);

    // Set the initial value for the characteristic
    customCharacteristic.writeValue((uint8_t)0); // Cast 0 to uint8_t

    // Start advertising the BLE service
    BLE.advertise();
    Serial.println("BLE device is now advertising...");
}

void loop() {
    // Listen for BLE peripherals to connect
    BLEDevice central = BLE.central();

    // If a device is connected
    if (central) {
        Serial.print("Connected to central: ");
        Serial.println(central.address());
        digitalWrite(LED_BUILTIN, HIGH);

        // While the device is connected
        while (central.connected()) {
            // Device loop for "ready"
            if (customCharacteristic.written()) {
                String receivedData = readCharacteristicData();
                Serial.print("Data received from Bluetooth: ");
                Serial.println(receivedData);

                // Clear the value after reading
                customCharacteristic.writeValue((uint8_t)0);

                if (receivedData == "ready") {
                    Serial.println("Device is ready!");

                    // Wait on next received instruction from the app
                    while (central.connected()) {
                        // Device loop for "start"
                        if (customCharacteristic.written()) {
                            String receivedData = readCharacteristicData();
                            Serial.print("Data received from Bluetooth: ");
                            Serial.println(receivedData);

                            // Clear the value after reading
                            customCharacteristic.writeValue((uint8_t)0);

                            // Check the exercise type sent to the device
                            if (receivedData == "start") {
                                Serial.println("Device is starting basic exercises!");
                                delay(50);
                                performExerciseRoutine();
                            }
                        }
                    } // end while
                }
            }
        } // end while

        // Once the central device disconnects
        Serial.print("Disconnected from central: ");
        Serial.println(central.address());
        digitalWrite(LED_BUILTIN, LOW);
    }
}

// Helper function to read data from the characteristic
String readCharacteristicData() {
    uint8_t value[20] = {0}; // Create a buffer to store up to 20 bytes of received data
    int length = customCharacteristic.readValue(value, sizeof(value));

    String receivedData = "";
    for (int i = 0; i < length; i++) {
        if (value[i] == 0) break; // Stop at null terminator if present
        receivedData += (char)value[i];
    }
    return receivedData;
}

// Function to simulate an exercise routine
void performExerciseRoutine() {
    for (int angle = 0; angle <= 180; angle++) {
        uint8_t angleValue = (uint8_t)(angle % 256); // Limit to 0-255
        customCharacteristic.writeValue(angleValue);

        Serial.print("Sent angle: ");
        Serial.println(angle);

        delay(50); // Wait before sending the next value
    }
}