#include <ArduinoBLE.h>
#include <Arduino_BMI270_BMM150.h>  // Library for the built-in IMU
#include <Wire.h>
#include <Adafruit_ISM330DHCX.h>    // External IMU library
#include <math.h>

/***** BLE VARIABLES *****/
// Define the BLE service and characteristic
BLEService customService("f3b4f9a8-25b8-4ee1-8b69-0a61a964de15"); // Custom service UUID

/*
 * Read/Write characteristics are opposite of the app such that we have one-way communication from
 * the app to the device and from the device to the app.
 */
BLECharacteristic writeCharacteristic("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c3", BLERead | BLEWrite | BLENotify, 20); // Custom characteristic UUID, read/write/notify, 20 byte size
BLECharacteristic readCharacteristic("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c4", BLERead | BLEWrite | BLENotify, 20); // Custom characteristic UUID, read/write/notify, 20 byte size


/***** EXERCISE VARIABLES *****/
float repsCount = 0;
bool repsCounted = false; // Variable to keep track of counting the current rep


/***** ROM VARIABLES *****/
// Create sensor object for the external IMU
Adafruit_ISM330DHCX externalIMU;  // External IMU object

#define SERIAL_BAUD 115200
const float alpha = 0.5;             // Smoothing factor for low-pass filter
const float GIMBAL_LOCK_THRESHOLD = 2.0;  // Threshold in degrees around ±90 for gimbal lock

// Variables to hold filtered roll and pitch
float filteredRollBuiltIn = 0;
float filteredPitchBuiltIn = 0;
float filteredRollExternal = 0;
float filteredPitchExternal = 0;

// Test-related variables
bool testInProgress = false;
bool timedTestComplete = false;
float triggerTimedTestComplete = -1;
unsigned long testStartTime = 0;    // Start time of the test
const unsigned long testDuration = 5000;  // 5 seconds
float maxPlantarDorsiAngle = -1e6;  // Start with very low number
float minPlantarDorsiAngle = 1e6;   // Start with very high number
float maxInversionEversionAngle = -1e6; // For tracking inversion/eversion
float minInversionEversionAngle = 1e6;  // For tracking inversion/eversion


/***** HELPER FUNCTIONS *****/
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

// Helper function to write data to the characteristic
void writeCharacteristicData(float data1, float data2 = NAN) {
    uint8_t floatBytes[8];  // Maximum needed for two floats (4 bytes each)
    memcpy(floatBytes, &data1, sizeof(float));  // Copy first float into byte array

    if (!isnan(data2)) {  // Check if second float is provided
        memcpy(floatBytes + 4, &data2, sizeof(float));  // Copy second float
        writeCharacteristic.writeValue(floatBytes, 8);  // Send 8 bytes
    } else {
        writeCharacteristic.writeValue(floatBytes, 4);  // Send only 4 bytes
    }
}


/***** REQUIRED SETUP *****/
void setup() {
    // Start serial communication for debugging
    Serial.begin(SERIAL_BAUD);
    while (!Serial); // todo: remove for final prototype

    // Initialize the built-in LED pin
    pinMode(LED_BUILTIN, OUTPUT);

    /***** BLE SETUP *****/
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
    writeCharacteristicData((float) 0);
    readCharacteristic.writeValue((uint8_t) 0);
    // Start advertising the BLE service
    BLE.advertise();
    Serial.println("BLE device is now advertising...");


    /***** ROM SETUP *****/
    // Initialize the built-in IMU
    Serial.print("Initializing built-in IMU... ");
    if (!IMU.begin()) {
        Serial.println("Failed to initialize built-in IMU!");
        while (1);
    }
    Serial.println("Success.");

    // Initialize the external IMU
    // todo: uncomment when external IMU is setup
//    Serial.print("Initializing external IMU at address 0x6A... ");
//    if (!externalIMU.begin_I2C(0x6A)) {  // Use I2C address 0x6A
//        Serial.println("Failed to initialize external IMU!");
//        while (1);
//    }
//    Serial.println("Success.");
}


/***** EXERCISE ROUTINES *****/
// Function to send live angle data for exercise routines
void performExerciseRoutine(bool isPlantarDorsiExercise, bool isTestRep = false) {
    // Initial setup before test starts
    if (!testInProgress) {
        repsCounted = false;
        testInProgress = true;

        // Reset angles for new test
        maxPlantarDorsiAngle = -1e6;
        minPlantarDorsiAngle = 1e6;
        maxInversionEversionAngle = -1e6;
        minInversionEversionAngle = 1e6;
    }

    // Variables for the built-in IMU
    float accX, accY, accZ;

    // Variables for the external IMU (foot-mounted)
    sensors_event_t accel, dummyGyro, dummyTemp;

    // Read data from the built-in IMU
    if (IMU.accelerationAvailable()) {
        IMU.readAcceleration(accX, accY, accZ);

        // Calculate the raw roll and pitch for the built-in IMU (calf-mounted)
        float roll = atan2(accY, accZ) * 180.0 / PI;
        float rawPitch = atan2(fabs(accX), sqrt(accY * accY + accZ * accZ)) * 180.0 / PI;
        float pitch = (accZ < 0) ? -rawPitch : rawPitch;

        // Apply gimbal lock protection near ±90°
        if (fabs(pitch - 90.0) <= GIMBAL_LOCK_THRESHOLD) {
            pitch = 90.0;
        } else if (fabs(pitch + 90.0) <= GIMBAL_LOCK_THRESHOLD) {
            pitch = -90.0;
        }

        // Convert negative pitch values to continuous range
        if (pitch < 0) {
            pitch = 180.0 + pitch;  // Convert negative pitch to range [90,180]
        }

        // Apply a low-pass filter to smooth values
        filteredRollBuiltIn = alpha * filteredRollBuiltIn + (1 - alpha) * roll;
        filteredPitchBuiltIn = alpha * filteredPitchBuiltIn + (1 - alpha) * pitch;
    }

    // Read data from the external IMU (foot-mounted)
    externalIMU.getEvent(&accel, &dummyGyro, &dummyTemp);

    // Calculate roll and pitch for the external IMU
    float extAccX = accel.acceleration.x;
    float extAccY = accel.acceleration.y;
    float extAccZ = accel.acceleration.z;

    float extRoll = atan2(extAccY, extAccZ) * 180.0 / PI;
    float extPitch = atan2(-extAccX, sqrt(extAccY * extAccY + extAccZ * extAccZ)) * 180.0 / PI;

    // Apply a low-pass filter to the external IMU data
    filteredRollExternal = alpha * filteredRollExternal + (1 - alpha) * extRoll;
    filteredPitchExternal = alpha * filteredPitchExternal + (1 - alpha) * extPitch;

    // Calculate angles
    float plantarDorsiAngle = fabs(filteredPitchBuiltIn - filteredPitchExternal);
    float inversionEversionAngle = fabs(filteredRollExternal);

    if (testInProgress) {
        if (isPlantarDorsiExercise) {
            if (isTestRep) {
                // Update range tracking
                maxPlantarDorsiAngle = max(maxPlantarDorsiAngle, plantarDorsiAngle);
                minPlantarDorsiAngle = min(minPlantarDorsiAngle, plantarDorsiAngle);
            } else {
                // Calculate reps count
                if (!repsCounted) {
                    // Check if a rep is completed (within 10% of the max angle)
                    if (plantarDorsiAngle >= (maxPlantarDorsiAngle - (maxPlantarDorsiAngle * 0.1))) {
                        repsCount++;
                        repsCounted = true;
                    }
                } else {
                    // Check if the next rep is started (within 10% of the min angle)
                    if (plantarDorsiAngle <= (minPlantarDorsiAngle + (minPlantarDorsiAngle * 0.1))) {
                        repsCounted = false;
                    }
                }
            }

            // Send live data
            writeCharacteristicData(plantarDorsiAngle);
            Serial.print("Sending live angle (plantar/dorsi): ");
            Serial.println(String(plantarDorsiAngle));
        } else {
            if (isTestRep) {
                // Update range tracking
                maxInversionEversionAngle = max(maxInversionEversionAngle, inversionEversionAngle);
                minInversionEversionAngle = min(minInversionEversionAngle, inversionEversionAngle);
            } else {
                // Calculate reps count
                // todo: test this with both IMUs
                if (!repsCounted) {
                    // Check if a rep is completed (within 10% of the max angle)
                    if (inversionEversionAngle >= (maxInversionEversionAngle - (maxInversionEversionAngle * 0.1))) {
                        repsCount++;
                        repsCounted = true;
                    }
                } else {
                    // Check if the next rep is started (within 10% of the min angle)
                    if (inversionEversionAngle <= (minInversionEversionAngle + (minInversionEversionAngle * 0.1))) {
                        repsCounted = false;
                    }
                }
            }

            // Send live data
            writeCharacteristicData(inversionEversionAngle);
            Serial.print("Sending live angle (inver/ever): ");
            Serial.println(String(inversionEversionAngle));
        }
    }

    delay(50);  // Sampling delay
}


/***** METRIC ROUTINES *****/
// Function to run ROM metric routine
void performROMMetricRoutine() {
    if (!timedTestComplete) {
        // Initial setup before test starts
        if (!testInProgress) {
            // Start the 5-second test with continuous printing
            testInProgress = true;
            testStartTime = millis();

            // Reset angles for new test
            maxPlantarDorsiAngle = -1e6;
            minPlantarDorsiAngle = 1e6;
            maxInversionEversionAngle = -1e6;
            minInversionEversionAngle = 1e6;

            Serial.println(
                    "Starting 5-second range of motion test with continuous sensor values printing...");
            Serial.println(
                    "----------------------------------------------------------------------------------------------------");
            Serial.println(
                    "Built-in IMU            | External IMU           | Calculated Angles                                ");
            Serial.println(
                    "Roll        Pitch       | Roll         Pitch     | Plantar/Dorsi            Inversion/Eversion      ");
            Serial.println(
                    "----------------------------------------------------------------------------------------------------");
        }

        // Variables for the built-in IMU
        float accX, accY, accZ;

        // Variables for the external IMU (foot-mounted)
        sensors_event_t accel, dummyGyro, dummyTemp;

        // Read data from the built-in IMU
        if (IMU.accelerationAvailable()) {
            IMU.readAcceleration(accX, accY, accZ);

            // Calculate the raw roll and pitch for the built-in IMU (calf-mounted)
            float roll = atan2(accY, accZ) * 180.0 / PI;
            float rawPitch = atan2(fabs(accX), sqrt(accY * accY + accZ * accZ)) * 180.0 / PI;
            float pitch = (accZ < 0) ? -rawPitch : rawPitch;

            // Apply gimbal lock protection near ±90°
            if (fabs(pitch - 90.0) <= GIMBAL_LOCK_THRESHOLD) {
                pitch = 90.0;
            } else if (fabs(pitch + 90.0) <= GIMBAL_LOCK_THRESHOLD) {
                pitch = -90.0;
            }

            // Convert negative pitch values to continuous range
            if (pitch < 0) {
                pitch = 180.0 + pitch;  // Convert negative pitch to range [90,180]
            }

            // Apply a low-pass filter to smooth values
            filteredRollBuiltIn = alpha * filteredRollBuiltIn + (1 - alpha) * roll;
            filteredPitchBuiltIn = alpha * filteredPitchBuiltIn + (1 - alpha) * pitch;
        }

        // Read data from the external IMU (foot-mounted)
        externalIMU.getEvent(&accel, &dummyGyro, &dummyTemp);

        // Calculate roll and pitch for the external IMU
        float extAccX = accel.acceleration.x;
        float extAccY = accel.acceleration.y;
        float extAccZ = accel.acceleration.z;

        float extRoll = atan2(extAccY, extAccZ) * 180.0 / PI;
        float extPitch = atan2(-extAccX, sqrt(extAccY * extAccY + extAccZ * extAccZ)) * 180.0 / PI;

        // Apply a low-pass filter to the external IMU data
        filteredRollExternal = alpha * filteredRollExternal + (1 - alpha) * extRoll;
        filteredPitchExternal = alpha * filteredPitchExternal + (1 - alpha) * extPitch;

        // Calculate angles
        float plantarDorsiAngle = fabs(filteredPitchBuiltIn - filteredPitchExternal);
        float inversionEversionAngle = fabs(filteredRollExternal);

        // Update range tracking
        if (testInProgress) {
            maxPlantarDorsiAngle = max(maxPlantarDorsiAngle, plantarDorsiAngle);
            minPlantarDorsiAngle = min(minPlantarDorsiAngle, plantarDorsiAngle);
            maxInversionEversionAngle = max(maxInversionEversionAngle, inversionEversionAngle);
            minInversionEversionAngle = min(minInversionEversionAngle, inversionEversionAngle);

            writeCharacteristicData(plantarDorsiAngle, inversionEversionAngle);
            Serial.print("Sending live angles (plantar/dorsi, inver/ever): ");
            Serial.println("(" + String(plantarDorsiAngle) + ", " +
                           String(inversionEversionAngle) + ")");
        }

        // If the test is in progress, check if 5 seconds have elapsed
        if (testInProgress && millis() - testStartTime >= testDuration) {
            testInProgress = false;
            timedTestComplete = true;

            // Send flag to app that test is completed
            writeCharacteristicData(triggerTimedTestComplete);
            Serial.println("ROM Test Complete!");
            delay(1000);

            // Calculate the maximum range of motion for plantar/dorsi and inversion/eversion
            float plantarDorsiRange = maxPlantarDorsiAngle - minPlantarDorsiAngle;
            float inversionEversionRange = maxInversionEversionAngle - minInversionEversionAngle;

            // Send final values to app
            writeCharacteristicData(plantarDorsiRange, inversionEversionRange);

            // Output the test results
            Serial.println("\n5-second Range of Motion Test Results:");
            Serial.print("Maximum Plantar Flexion / Dorsiflexion Range (degrees): ");
            Serial.println(plantarDorsiRange, 2);
            Serial.print("Maximum Inversion / Eversion Range (degrees): ");
            Serial.println(inversionEversionRange, 2);
            Serial.println("-------------------------------------------------------------------------------------");
        }

        delay(50);  // Sampling delay
    }
}

// Function to run Gait metric routine
void performGaitMetricRoutine() {

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

                    /***** SEND TENSION LEVEL OF DEVICE *****/
                    float tensionLevel = 4; // todo: replace with actual tension on device
                    writeCharacteristicData(tensionLevel);
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
                            readCharacteristic.writeValue((uint8_t) 0);

                            /***** CHECK EXERCISE TYPE TO START TEST REP *****/
                            if (receivedData == "test_plantar") {
                                Serial.println("Device is starting test rep for Plantar Flexion!");

                                // Wait on next received instruction from the app
                                while (central.connected()) {
                                    delay(50);

                                    /***** TEST REP - START PLANTAR FLEXION *****/
                                    performExerciseRoutine(true, true);

                                    /***** FINISH TEST REP *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "test_complete") {
                                            /***** TEST REP - CALCULATE MIN/MAX ANGLES *****/
                                            writeCharacteristicData(minPlantarDorsiAngle, maxPlantarDorsiAngle);
                                            Serial.print("Sending min/max Angles: ");
                                            Serial.println("(" + String(minPlantarDorsiAngle) + ", " + String(maxPlantarDorsiAngle) + ")");
                                            finishTestRep = true;
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "test_dorsiflexion") {
                                Serial.println("Device is starting test rep for Dorsiflexion!");

                                // Wait on next received instruction from the app
                                while (central.connected()) {
                                    delay(50);

                                    /***** TEST REP - START DORSIFLEXION *****/
                                    performExerciseRoutine(true, true);

                                    /***** FINISH TEST REP *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "test_complete") {
                                            /***** TEST REP - CALCULATE MIN/MAX ANGLES *****/
                                            writeCharacteristicData(minPlantarDorsiAngle, maxPlantarDorsiAngle);
                                            Serial.print("Sending min/max Angles: ");
                                            Serial.println("(" + String(minPlantarDorsiAngle) + ", " + String(maxPlantarDorsiAngle) + ")");
                                            finishTestRep = true;
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "test_inversion") {
                                Serial.println("Device is starting test rep for Inversion!");

                                // Wait on next received instruction from the app
                                while (central.connected()) {
                                    delay(50);

                                    /***** TEST REP - START INVERSION *****/
                                    performExerciseRoutine(false, true);

                                    /***** FINISH TEST REP *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "test_complete") {
                                            /***** TEST REP - CALCULATE MIN/MAX ANGLES *****/
                                            writeCharacteristicData(minInversionEversionAngle, maxInversionEversionAngle);
                                            Serial.print("Sending min/max Angles: ");
                                            Serial.println("(" + String(minInversionEversionAngle) + ", " + String(maxInversionEversionAngle) + ")");
                                            finishTestRep = true;
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "test_eversion") {
                                Serial.println("Device is starting test rep for Eversion!");

                                // Wait on next received instruction from the app
                                while (central.connected()) {
                                    delay(50);

                                    /***** TEST REP - START EVERSION *****/
                                    performExerciseRoutine(false, true);

                                    /***** FINISH TEST REP *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "test_complete") {
                                            /***** TEST REP - CALCULATE MIN/MAX ANGLES *****/
                                            writeCharacteristicData(minInversionEversionAngle, maxInversionEversionAngle);
                                            Serial.print("Sending min/max Angles: ");
                                            Serial.println("(" + String(minInversionEversionAngle) + ", " + String(maxInversionEversionAngle) + ")");
                                            finishTestRep = true;
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "no_test_rep" || receivedData == "error") {
                                Serial.println("Device is skipping test rep!");
                                // Clear the tension value
                                writeCharacteristicData((float) 0);
                                break;
                            }
                            // Finish test rep
                            if (finishTestRep) {
                                Serial.println("Device is finishing test rep!");
                                break;
                            }
                        }
                    } // end while

                    // Wait on next received instruction from the app
                    while (central.connected()) {
                        // Setup variables
                        testInProgress = false;

                        /***** START - DEVICE LOOP *****/
                        if (readCharacteristic.written()) {
                            String receivedData = readCharacteristicData();
                            Serial.print("Data received from Bluetooth: ");
                            Serial.println(receivedData);

                            // Clear the value after reading
                            readCharacteristic.writeValue((uint8_t) 0);

                            /***** CHECK EXERCISE TYPE TO START *****/
                            if (receivedData == "start_plantar") {
                                // Setup variables for exercise
                                repsCount = 0;

                                Serial.println("Device is starting Plantar Flexion exercise!");
                                delay(50);

                                /***** START PLANTAR FLEXION EXERCISE *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performExerciseRoutine(true);

                                    /***** FINISH SET *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "end_set") {
                                            /***** SET - SEND REPS COMPLETED *****/
                                            Serial.println("Set Complete!");
                                            writeCharacteristicData(repsCount);
                                            Serial.print("Sending reps count: ");
                                            Serial.println(String(repsCount));
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "start_dorsiflexion") {
                                // Setup variables for exercise
                                repsCount = 0;

                                Serial.println("Device is starting Dorsiflexion exercise!");
                                delay(50);

                                /***** START DORSIFLEXION EXERCISE *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performExerciseRoutine(true);

                                    /***** FINISH SET *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "end_set") {
                                            /***** SET - SEND REPS COMPLETED *****/
                                            Serial.println("Set Complete!");
                                            writeCharacteristicData(repsCount);
                                            Serial.print("Sending reps count: ");
                                            Serial.println(String(repsCount));
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "start_inversion") {
                                // Setup variables for exercise
                                repsCount = 0;

                                Serial.println("Device is starting Inversion exercise!");
                                delay(50);

                                /***** START INVERSION EXERCISE *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performExerciseRoutine(false);

                                    /***** FINISH SET *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "end_set") {
                                            /***** SET - SEND REPS COMPLETED *****/
                                            Serial.println("Set Complete!");
                                            writeCharacteristicData(repsCount);
                                            Serial.print("Sending reps count: ");
                                            Serial.println(String(repsCount));
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "start_eversion") {
                                // Setup variables for exercise
                                repsCount = 0;

                                Serial.println("Device is starting Eversion exercise!");
                                delay(50);

                                /***** START EVERSION EXERCISE *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performExerciseRoutine(false);

                                    /***** FINISH SET *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "end_set") {
                                            /***** SET - SEND REPS COMPLETED *****/
                                            Serial.println("Set Complete!");
                                            writeCharacteristicData(repsCount);
                                            Serial.print("Sending reps count: ");
                                            Serial.println(String(repsCount));
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "start_ROM") {
                                // Setup variables for ROM test
                                timedTestComplete = false;

                                Serial.println("Device is starting ROM Test metric!");
                                delay(50);

                                /***** START ROM METRIC *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performROMMetricRoutine();
                                } // end while
                            } else if (receivedData == "start_Gait") {
                                // Setup variables for Gait test
                                timedTestComplete = false;

                                Serial.println("Device is starting Gait Test metric!");
                                delay(50);

                                /***** START GAIT METRIC *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performGaitMetricRoutine();
                                } // end while
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
