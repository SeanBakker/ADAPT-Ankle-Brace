#include <ArduinoBLE.h>
#include <Arduino_BMI270_BMM150.h>  // Library for the built-in IMU
//#include <Arduino_LSM6DS3.h>
//#include <Arduino_LSM9DS1.h>
#include <Adafruit_ISM330DHCX.h>    // External IMU library
#include <Wire.h>
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
Adafruit_ISM330DHCX externalIMU; // External IMU object

#define SERIAL_BAUD 115200
const float ALPHA_ROM = 0.5; // Smoothing factor for low-pass filter
const float GIMBAL_LOCK_THRESHOLD = 2.0; // Threshold in degrees around ±90 for gimbal lock

// Variables to hold filtered roll and pitch
float filteredRollBuiltIn = 0;
float filteredPitchBuiltIn = 0;
float filteredRollExternal = 0;
float filteredPitchExternal = 0;

// Test-related variables
bool testInProgress = false;
bool timedTestComplete = false;
float triggerTimedTestComplete = -1;
unsigned long testStartTime = 0; // Start time of the test
const unsigned long ROM_TEST_DURATION = 5000; // 5 seconds
float maxPlantarDorsiAngle = -1e6; // Start with very low number
float minPlantarDorsiAngle = 1e6; // Start with very high number
float maxInversionEversionAngle = -1e6; // For tracking inversion/eversion
float minInversionEversionAngle = 1e6; // For tracking inversion/eversion


/***** GAIT VARIABLES *****/
const unsigned long GAIT_TEST_DURATION = 10000; // Test length: 10 seconds
const unsigned long SAMPLING_INTERVAL = 10; // Sampling interval ~100 Hz
const unsigned long MIN_STEP_INTERVAL_MS = 250; // Minimum time between valid steps (to avoid double-counting)
const unsigned long MINIMUM_STANCE_MS = 400; // Minimum stance duration (avoid stance phases that end too quickly)
const float GYRO_STABILITY_THRESHOLD = 30.0f; // Gyro threshold for detecting foot rotation or “unstable” foot
const float ALPHA_GAIT = 0.7; // Low-pass filter factor for accelerometer
const int MAX_STEPS = 50; // Data array sizes

unsigned long lastSampleTime = 0;
unsigned long lastHeelStrikeTime = 0; // Last time we detected a heel strike
float azFiltered = 0.0f;
float tempPeakForce = 0.0f;
int stepIndex = 0;
int totalSteps = 0;
unsigned long firstStepTime = 0;
unsigned long finalStepTime = 0;

// Arrays to store intervals & forces
float stepIntervals[MAX_STEPS];
float peakForces[MAX_STEPS];
float prev_az = 0;

// Arrays to store stance / swing boundaries
unsigned long stepStartTime[MAX_STEPS]; // Stance start
unsigned long stepEndTime[MAX_STEPS]; // Stance end

// Thresholds for heel strike and toe-off
// todo: update these thresholds during device testing
const float HEEL_STRIKE_THRESHOLD = -0.3f; // -0.6f
const float TOE_OFF_THRESHOLD = 0.0f;
const float CHANGE_THRESHOLD = 0.2f;

// For the state machine
enum GaitState {
    GAIT_IDLE,
    WAIT_HEEL_STRIKE,
    STANCE,
    SWING
};
GaitState currentState = GAIT_IDLE;


/***** GENERAL HELPER FUNCTIONS *****/
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
    uint8_t floatBytes[8]; // Maximum needed for two floats (4 bytes each)
    memcpy(floatBytes, &data1, sizeof(float)); // Copy first float into byte array

    if (!isnan(data2)) {  // Check if second float is provided
        memcpy(floatBytes + 4, &data2, sizeof(float)); // Copy second float
        writeCharacteristic.writeValue(floatBytes, 8); // Send 8 bytes
    } else {
        writeCharacteristic.writeValue(floatBytes, 4); // Send only 4 bytes
    }
}

/***** GAIT TEST HELPER FUNCTIONS *****/
// Helper function to compute stance & swing from stepStartTime[i], stepEndTime[i] for Gait test
float computeSwingStanceRatio() {
    float ratio = 0.0f;

    if (stepIndex == 0) {
        Serial.println("Not enough data to compute stance/swing times.");
    }

    float sumStance = 0.0f;
    float sumSwing  = 0.0f;
    int stanceCount = 0;
    int swingCount  = 0;

    for (int i = 0; i < stepIndex; i++) {
        unsigned long sStart = stepStartTime[i];
        unsigned long sEnd   = stepEndTime[i];
        if (sEnd > sStart) {
            float stanceMs = (float)(sEnd - sStart);
            sumStance += stanceMs;
            stanceCount++;
        }

        if (i < stepIndex - 1) {
            // swing = next stance start - current stance end
            unsigned long nextStart = stepStartTime[i+1];
            if (nextStart > sEnd) {
                float swingMs = (float)(nextStart - sEnd);
                sumSwing += swingMs;
                swingCount++;
            }
        }
    }

    float avgStanceMs = (stanceCount > 0) ? (sumStance / stanceCount) : 0.0f;
    float avgSwingMs  = (swingCount  > 0) ? (sumSwing  / swingCount ) : 0.0f;

    float avgStanceSec = avgStanceMs / 1000.0f;
    float avgSwingSec  = avgSwingMs  / 1000.0f;

    Serial.print("Average Stance Time: ");
    Serial.print(avgStanceSec, 3);
    Serial.println(" s");

    if (swingCount == 0) {
        Serial.println("Not enough data to compute average swing time.");
    }
    Serial.print("Average Swing Time: ");
    Serial.print(avgSwingSec, 3);
    Serial.println(" s");

    if (avgStanceSec > 0.0f) {
        ratio = (avgSwingSec / avgStanceSec) * 100; // Units of %
    } else {
        Serial.println("Invalid stance time for ratio calculation.");
    }

    return ratio;
}

// Helper function to compute the user's cadence for Gait test
float computeCadence() {
    float stepsPerMin = 0.0f;

    // Cadence
    if (firstStepTime == 0) {
        Serial.println("No valid steps for cadence calculation.");
    } else {
        unsigned long endTime = (finalStepTime == 0)
                                ? (testStartTime + GAIT_TEST_DURATION)
                                : finalStepTime;
        float elapsedSec = (float)(endTime - firstStepTime) / 1000.0f;
        if (elapsedSec > 0.0f) {
            stepsPerMin = (float)totalSteps / elapsedSec * 60.0f;
        } else {
            Serial.println("Cadence: insufficient time for calculation.");
        }
    }

    return stepsPerMin;
}

// Helper function to compute the user's average impact force for Gait test
float computeImpactForce() {
    float avgPeak = 0.0f;

    // Average Peak Force
    if (stepIndex == 0) {
        Serial.println("No peak force data (less than 1 step detected).");
    } else {
        float sumPeak = 0.0f;
        for (int i = 0; i < stepIndex; i++) {
            sumPeak += peakForces[i];
        }
        avgPeak = sumPeak / (float)stepIndex;
    }

    return avgPeak;
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


    /***** ROM & GAIT SETUP *****/
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
    // 1. Initial setup before test starts
    if (!testInProgress) {
        repsCounted = false;
        testInProgress = true;

        // Reset angles for new test
        maxPlantarDorsiAngle = -1e6;
        minPlantarDorsiAngle = 1e6;
        maxInversionEversionAngle = -1e6;
        minInversionEversionAngle = 1e6;
    }

    // 2. Collect data
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
        filteredRollBuiltIn = ALPHA_ROM * filteredRollBuiltIn + (1 - ALPHA_ROM) * roll;
        filteredPitchBuiltIn = ALPHA_ROM * filteredPitchBuiltIn + (1 - ALPHA_ROM) * pitch;
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
    filteredRollExternal = ALPHA_ROM * filteredRollExternal + (1 - ALPHA_ROM) * extRoll;
    filteredPitchExternal = ALPHA_ROM * filteredPitchExternal + (1 - ALPHA_ROM) * extPitch;

    // Calculate angles
    float plantarDorsiAngle = fabs(filteredPitchBuiltIn - filteredPitchExternal);
    float inversionEversionAngle = fabs(filteredRollExternal);

    // 3. Send live data & perform calculations
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
        // 1. Initial setup before test starts
        if (!testInProgress) {
            testInProgress = true;
            testStartTime = millis();

            // Reset angles for new test
            maxPlantarDorsiAngle = -1e6;
            minPlantarDorsiAngle = 1e6;
            maxInversionEversionAngle = -1e6;
            minInversionEversionAngle = 1e6;

            Serial.println("Starting ROM Test!");
            Serial.println(
                    "----------------------------------------------------------------------------------------------------");
            Serial.println(
                    "Built-in IMU            | External IMU           | Calculated Angles                                ");
            Serial.println(
                    "Roll        Pitch       | Roll         Pitch     | Plantar/Dorsi            Inversion/Eversion      ");
            Serial.println(
                    "----------------------------------------------------------------------------------------------------");
        }

        // 2. Collect data
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
            filteredRollBuiltIn = ALPHA_ROM * filteredRollBuiltIn + (1 - ALPHA_ROM) * roll;
            filteredPitchBuiltIn = ALPHA_ROM * filteredPitchBuiltIn + (1 - ALPHA_ROM) * pitch;
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
        filteredRollExternal = ALPHA_ROM * filteredRollExternal + (1 - ALPHA_ROM) * extRoll;
        filteredPitchExternal = ALPHA_ROM * filteredPitchExternal + (1 - ALPHA_ROM) * extPitch;

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

        // 3. Check for test completion
        if (testInProgress && millis() - testStartTime >= ROM_TEST_DURATION) {
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
    if (!timedTestComplete) {
        // 1. Initial setup before test starts
        if (!testInProgress && currentState == GAIT_IDLE) {
            testInProgress = true;
            testStartTime = millis();
            lastSampleTime = 0;
            currentState = WAIT_HEEL_STRIKE; // Move to waiting for first heel strike
            stepIndex = 0;
            totalSteps = 0;
            firstStepTime = 0;
            finalStepTime = 0;
            memset(stepIntervals, 0, sizeof(stepIntervals));
            memset(peakForces, 0, sizeof(peakForces));
            Serial.println("Starting Gait Test!");
        }

        // 2. Collect data
        unsigned long currentTime = millis();
        if (currentTime - lastSampleTime >= SAMPLING_INTERVAL) {
            lastSampleTime = currentTime;
            float ax, ay, az;
            float gx, gy, gz;

            // Read both acceleration & gyroscope if available
            if (IMU.accelerationAvailable() && IMU.gyroscopeAvailable()) {
                IMU.readAcceleration(ax, ay, az);
                IMU.readGyroscope(gx, gy, gz);

                // Invert z-axis if sensor is upside down
                float az_inverted = az; //removed negative sign

                // Optional low-pass filter
                azFiltered = ALPHA_GAIT * azFiltered + (1.0f - ALPHA_GAIT) * az_inverted;
                float azToUse = azFiltered;

                // Measure total gyro for foot "stability"
                float gyroMagnitude = sqrt(gx * gx + gy * gy + gz * gz);

                // 2.1. State Machine Data Collection
                switch (currentState) {

                    case WAIT_HEEL_STRIKE: {
                        // Looking for a new heel strike
                        bool heelStrikeDetected =
                                (azToUse < HEEL_STRIKE_THRESHOLD) &&
                                (prev_az - azToUse > CHANGE_THRESHOLD) &&
                                (currentTime - lastHeelStrikeTime > MIN_STEP_INTERVAL_MS);
                        if (heelStrikeDetected) {
                            // We have a heel strike
                            totalSteps++;
                            if (firstStepTime == 0) {
                                firstStepTime = currentTime;
                            }

                            // Step interval from previous heel strike
                            if (lastHeelStrikeTime != 0 && stepIndex < MAX_STEPS) {
                                float intervalMs = (float) (currentTime - lastHeelStrikeTime);
                                stepIntervals[stepIndex] = intervalMs;
                            }

                            lastHeelStrikeTime = currentTime;
                            tempPeakForce = azToUse;

                            // Record stance start
                            if (stepIndex < MAX_STEPS) {
                                stepStartTime[stepIndex] = currentTime;
                            }

                            // Move to STANCE
                            currentState = STANCE;
                        }
                        break;
                    }

                    case STANCE: {
                        // We remain in stance until we detect toe-off
                        // Keep tracking peak force
                        if (azToUse > tempPeakForce) {
                            tempPeakForce = azToUse;
                        }

                        unsigned long stanceElapsed = currentTime - lastHeelStrikeTime;
                        bool belowToeOff = (azToUse < TOE_OFF_THRESHOLD);
                        bool enoughStance = (stanceElapsed >= MINIMUM_STANCE_MS);
                        bool footUnstable = (gyroMagnitude > GYRO_STABILITY_THRESHOLD);

                        // If we see a valid toe-off condition, go to SWING
                        if (enoughStance && belowToeOff && footUnstable) {
                            finalStepTime = currentTime;
                            // Serial.println("Lift");
                            // Serial.println(azToUse);
                            // Store peak force
                            if (stepIndex < MAX_STEPS) {
                                peakForces[stepIndex] = tempPeakForce;
                            }
                            // Mark stance end
                            if (stepIndex < MAX_STEPS) {
                                stepEndTime[stepIndex] = currentTime;
                            }

                            float stepTimeSec = finalStepTime / 1000.0f;
                            Serial.print("STEP #");
                            Serial.print(totalSteps);
                            Serial.print(" ended stance at t=");
                            Serial.print(stepTimeSec, 3);
                            Serial.print(" s; Peak Force=");
                            Serial.print(tempPeakForce, 3);
                            Serial.println(" G");

                            // Increment stepIndex
                            stepIndex++;

                            // Next state: SWING
                            currentState = SWING;
                        }
                        break;
                    }

                    case SWING: {
                        // We remain in SWING until we detect the next heel strike
                        bool nextHeelStrike =
                                (azToUse < HEEL_STRIKE_THRESHOLD) &&
                                (prev_az - azToUse > CHANGE_THRESHOLD) &&
                                (currentTime - lastHeelStrikeTime > MIN_STEP_INTERVAL_MS);

                        if (nextHeelStrike) {
                            // New stance begins
                            // Serial.println("Strike");
                            // Serial.println(azToUse);
                            totalSteps++;
                            if (firstStepTime == 0) {
                                firstStepTime = currentTime;
                            }

                            // Step interval
                            if (lastHeelStrikeTime != 0 && stepIndex < MAX_STEPS) {
                                float intervalMs = (float) (currentTime - lastHeelStrikeTime);
                                stepIntervals[stepIndex] = intervalMs; // store next interval
                            }

                            lastHeelStrikeTime = currentTime;
                            tempPeakForce = azToUse;

                            // Mark stance start
                            if (stepIndex < MAX_STEPS) {
                                stepStartTime[stepIndex] = currentTime;
                            }

                            // Next state: STANCE
                            currentState = STANCE;
                        }
                        break;
                    }

                    default:
                        // GAIT_IDLE or any fallback
                        break;
                }
                prev_az = azToUse;
            } else {
                Serial.println("IMU Accelerometer and/or Gyroscope not available!");
            }
        }

        // 3. Check for test completion
        if (testInProgress && millis() - testStartTime >= GAIT_TEST_DURATION) {
            testInProgress = false;
            timedTestComplete = true;

            // Send flag to app that test is completed
            writeCharacteristicData(triggerTimedTestComplete);
            Serial.println("Gait Test Complete!");
            delay(1000);
        }

        delay(SAMPLING_INTERVAL); // Sampling delay
    }
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
                                testInProgress = false;

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
                                testInProgress = false;
                                currentState == GAIT_IDLE;

                                Serial.println("Device is starting Gait Test metric!");
                                delay(50);

                                /***** START GAIT METRIC *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performGaitMetricRoutine();

                                    /***** FINISH GAIT TEST *****/
                                    if (timedTestComplete) {
                                        if (readCharacteristic.written()) {
                                            String receivedData = readCharacteristicData();
                                            Serial.print("Data received from Bluetooth: ");
                                            Serial.println(receivedData);

                                            // Clear the value after reading
                                            readCharacteristic.writeValue((uint8_t) 0);

                                            if (receivedData == "gait_steps") {
                                                /***** GAIT - SEND STEPS DETECTED *****/
                                                Serial.println("\n***** GAIT TEST SUMMARY *****");
                                                Serial.print("Total Steps Detected: ");
                                                Serial.println(totalSteps);
                                                writeCharacteristicData((float) totalSteps);
                                            } else if (receivedData == "gait_cadence") {
                                                /***** GAIT - SEND CALCULATED CADENCE *****/
                                                float cadence = computeCadence();
                                                Serial.print("Calculated Cadence (steps/min): ");
                                                Serial.println(cadence);
                                                writeCharacteristicData(cadence);
                                            } else if (receivedData == "gait_force") {
                                                /***** GAIT - SEND CALCULATED IMPACT FORCE *****/
                                                float force = computeImpactForce();
                                                Serial.print("Calculated Impact Force (G): ");
                                                Serial.println(force);
                                                writeCharacteristicData(force);
                                            } else if (receivedData == "gait_ratio") {
                                                /***** GAIT - SEND CALCULATED SWING STANCE RATIO *****/
                                                float ratio = computeSwingStanceRatio();
                                                Serial.print("Calculated Swing Stance Ratio (Ideal ~ 67%): ");
                                                Serial.println(ratio);
                                                writeCharacteristicData(ratio);
                                                break;
                                            }
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "ready") {
                                // break out of while loop
                                break;
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
