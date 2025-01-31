#include <ArduinoBLE.h>

// Define the BLE service and characteristic
BLEService customService("f3b4f9a8-25b8-4ee1-8b69-0a61a964de15"); // Custom service UUID

/*
 * Read/Write characteristics are opposite of the app such that we have one-way communication from
 * the app to the device and from the device to the app.
 */
BLECharacteristic writeCharacteristic("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c3", BLERead | BLEWrite | BLENotify, 20); // Custom characteristic UUID, read/write/notify, 20 byte size
BLECharacteristic readCharacteristic("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c4", BLERead | BLEWrite | BLENotify, 20); // Custom characteristic UUID, read/write/notify, 20 byte size

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
    customService.addCharacteristic(writeCharacteristic);
    customService.addCharacteristic(readCharacteristic);

    // Add the service
    BLE.addService(customService);

    // Set the initial value for the characteristic
    writeCharacteristic.writeValue((uint8_t) 0); // Cast 0 to uint8_t
    readCharacteristic.writeValue((uint8_t) 0); // Cast 0 to uint8_t

    // Start advertising the BLE service
    BLE.advertise();
    Serial.println("BLE device is now advertising...");
}

/***** MAIN LOOP *****/
void loop() {
    // Listen for BLE peripherals to connect
    BLEDevice central = BLE.central();
    bool finishTestRep = false;

    // If a device is connected
    if (central) {
        Serial.print("Connected to central: ");
        Serial.println(central.address());
        digitalWrite(LED_BUILTIN, HIGH);

        // While the device is connected
        while (central.connected()) {
            /***** READY - DEVICE LOOP *****/
            if (readCharacteristic.written()) {
                String receivedData = readCharacteristicData();
                Serial.print("Data received from Bluetooth: ");
                Serial.println(receivedData);

                // Clear the value after reading
                readCharacteristic.writeValue((uint8_t)0);

                /***** CHECK APP IS READY *****/
                if (receivedData == "ready") {
                    Serial.println("Device is ready!");

                    /***** SEND TENSION LEVEL *****/
                    // Send the configured tension level from the device
                    uint8_t tensionLevel = (uint8_t) 4;
                    writeCharacteristic.writeValue(tensionLevel);
                    Serial.print("Sending tension level: ");
                    Serial.println(tensionLevel);

                    // Wait on next received instruction from the app
                    while (central.connected()) {
                        /***** TEST REP - DEVICE LOOP *****/
                        if (readCharacteristic.written()) {
                            String receivedData = readCharacteristicData();
                            Serial.print("Data received from Bluetooth: ");
                            Serial.println(receivedData);

                            // Clear the value after reading
                            readCharacteristic.writeValue((uint8_t)0);

                            /***** CHECK EXERCISE TYPE TO START TEST REP *****/
                            if (receivedData == "test_plantar") {
                                Serial.println("Device is starting test rep for Plantar Flexion!");

                                // Wait on next received instruction from the app
                                while (central.connected()) {
                                    delay(50);

                                    /***** TEST REP - START PLANTAR FLEXION *****/
                                    writeCharacteristic.writeValue((uint8_t) 35);
                                    /***** TEST REP - END PLANTAR FLEXION *****/

                                    /***** FINISH TEST REP - DEVICE LOOP *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "finish") {
                                            /***** TEST REP - CALCULATE MIN/MAX ANGLES *****/
                                            uint8_t minAngle = 10;
                                            uint8_t maxAngle = 70;
                                            uint8_t data[2] = {minAngle, maxAngle};  // Combined two 8-bit values
                                            writeCharacteristic.writeValue(data, sizeof(data));
                                            Serial.print("Sending min/max Angles: ");
                                            Serial.println("(" + String(minAngle) + ", " + String(maxAngle) + ")");
                                            /***** TEST REP - CALCULATE MIN/MAX ANGLES *****/

                                            finishTestRep = true;
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "test_dorsiflexion") {
                                Serial.println("Device is starting test rep for Dorsiflexion!");
                                delay(50);
                                performExerciseRoutine();
                            } else if (receivedData == "test_inversion") {
                                Serial.println("Device is starting test rep for Inversion!");
                                delay(50);
                                performExerciseRoutine();
                            } else if (receivedData == "test_eversion") {
                                Serial.println("Device is starting test rep for Eversion!");
                                delay(50);
                                performExerciseRoutine();
                            } else if (finishTestRep || receivedData == "no_test_rep" || receivedData == "error") {
                                break;
                            }
                        }
                    } // end while

                    // Wait on next received instruction from the app
                    while (central.connected()) {
                        /***** START - DEVICE LOOP *****/
                        if (readCharacteristic.written()) {
                            String receivedData = readCharacteristicData();
                            Serial.print("Data received from Bluetooth: ");
                            Serial.println(receivedData);

                            // Clear the value after reading
                            readCharacteristic.writeValue((uint8_t)0);

                            /***** CHECK EXERCISE TYPE TO START *****/
                            if (receivedData == "start") {
                                Serial.println("Device is starting basic exercises!");
                                delay(50);
                                performExerciseRoutine();
                            } else if (receivedData == "start_ROM") {
                                Serial.println("Device is starting ROM Test exercise!");
                                delay(50);
                                uint8_t testValue = (uint8_t)(50);
                                writeCharacteristic.writeValue(testValue);
                                Serial.print("Sent value: ");
                                Serial.println(testValue);
                            } else if (receivedData == "start_Gait") {
                                Serial.println("Device is starting Gait Test exercise!");
                                delay(50);
                                uint8_t testValue = (uint8_t)(10);
                                writeCharacteristic.writeValue(testValue);
                                Serial.print("Sent value: ");
                                Serial.println(testValue);
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
    int length = readCharacteristic.readValue(value, sizeof(value));

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
        writeCharacteristic.writeValue(angleValue);

        Serial.print("Sent angle: ");
        Serial.println(angle);

        delay(50); // Wait before sending the next value
    }
}
