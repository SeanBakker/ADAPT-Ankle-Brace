#include <ArduinoBLE.h>
#include <Arduino_BMI270_BMM150.h>  // Library for the built-in IMU
//#include <Arduino_LSM6DS3.h>
//#include <Arduino_LSM9DS1.h>
#include <Adafruit_ISM330DHCX.h>    // External IMU library
#include <Wire.h>
#include <math.h>
#include <cstdlib>

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
float holdTime = 0;
unsigned long holdStartTime = 0;
float totalHoldTime = 0;
float averageHoldTime = 0;
bool isHolding = false;
bool repsCounted = false; // Variable to keep track of counting the current rep
float previousPlantarDorsiAngle = 0;
float previousInversionEversionAngle = 0;
const float REP_MIN_RANGE = 5.0; // degrees range at minimum to count as rep

struct ExerciseOptions {
    bool isPlantarExercise = false;
    bool isDorsiExercise = false;
    bool isInverExercise = false;
    bool isEverExercise = false;
    bool isTestRep = false;

    // Constructor with default values
    ExerciseOptions(bool plantar = false, bool dorsi = false, bool inver = false,
                    bool ever = false, bool testRep = false)
            : isPlantarExercise(plantar), isDorsiExercise(dorsi),
              isInverExercise(inver), isEverExercise(ever), isTestRep(testRep) {}
};

// Define global variables with preset configurations
ExerciseOptions isPlantarOptions(true, false, false, false, false);
ExerciseOptions isPlantarTestOptions(true, false, false, false, true);
ExerciseOptions isDorsiOptions(false, true, false, false, false);
ExerciseOptions isDorsiTestOptions(false, true, false, false, true);
ExerciseOptions isInverOptions(false, false, true, false, false);
ExerciseOptions isInverTestOptions(false, false, true, false, true);
ExerciseOptions isEverOptions(false, false, false, true, false);
ExerciseOptions isEverTestOptions(false, false, false, true, true);


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
unsigned long romTestDuration = 0; // Test length calculated by app
float maxPlantarDorsiAngle = -1e6; // Start with very low number
float minPlantarDorsiAngle = 1e6; // Start with very high number
float maxInversionEversionAngle = -1e6; // For tracking inversion/eversion
float minInversionEversionAngle = 1e6; // For tracking inversion/eversion


/***** GAIT VARIABLES *****/
const unsigned long SAMPLING_INTERVAL = 10; // Sampling interval ~100 Hz
const unsigned long MIN_STEP_INTERVAL_MS = 250; // Minimum time between valid steps (to avoid double-counting)
const unsigned long MINIMUM_STANCE_MS = 400; // Minimum stance duration (avoid stance phases that end too quickly)
const float GYRO_STABILITY_THRESHOLD = 30.0f; // Gyro threshold for detecting foot rotation or “unstable” foot
const float ALPHA_GAIT = 0.4; // Low-pass filter factor for accelerometer
const int MAX_STEPS = 50; // Data array sizes

unsigned long gaitTestDuration = 0; // Test length calculated by app
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
const float HEEL_STRIKE_THRESHOLD = -0.1f; // -0.6f
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


/***** POTENTIOMETER VARIABLES *****/
int tensionLevel1 = 0; // Tension level for Plantar/Dorsiflexion to send to app
int tensionLevel2 = 0; // Tension level for Inversion/Eversion to send to app

// Define the analog input pins
const int potPin1 = A1;  // First potentiometer on A0 (plantar/dorsiflexion)
const int potPin2 = A0;  // Second potentiometer on A1 (inversion/eversion)
const float referenceVoltage = 3.3;  // Nano 33 BLE operates at 3.3V
const int adcResolution = 1024;  // 10-bit ADC (0-1023)

// Rotation mapping
const float minADC = 1;     // Minimum valid ADC value
const float maxADC = 1023;  // Maximum valid ADC value
const float minDegrees = 0;   // Start of rotation range (degrees)
const float maxDegrees = 330; // End of rotation range (degrees)

// Stability check parameters
const int stabilityCheckCount = 5;  // Number of past readings to monitor
int lastReadings1[stabilityCheckCount];  // Circular buffer for Potentiometer 1
int lastReadings2[stabilityCheckCount];  // Circular buffer for Potentiometer 2
int stabilityIndex = 0;  // Index for storing readings


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

    // todo: return ratio;
    return 50 + (rand() % 21); // Generates a random number between 50 and 70
}

// Helper function to compute the user's cadence for Gait test
float computeCadence() {
    float stepsPerMin = 0.0f;

    // Cadence
    if (firstStepTime == 0) {
        Serial.println("No valid steps for cadence calculation.");
    } else {
        unsigned long endTime = (finalStepTime == 0)
                                ? (testStartTime + gaitTestDuration)
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

// Helper function to determine the level from the degree value
int getTensionLevel1(float degrees) {
    if (degrees >= 70 && degrees < 160) {
        return 1; //"Level 1 or Level 5"
    } else if (degrees >= 160 && degrees < 255) {
        return 2; //"Level 2"
    } else if (degrees >= 255 || degrees < 10) {
        return 3; //"Level 3"
    } else if (degrees >= 10 && degrees < 70) {
        return 4; //"Level 4"
    }
    return 0;
}

// Helper function to determine the level from the degree value
int getTensionLevel2(float degrees) {
    if (degrees >= 250 && degrees < 330) {
        return 1; //"Level 1 or Level 5"
    } else if (degrees >= 330 || degrees < 40) {
        return 2; //"Level 2"
    } else if (degrees >= 40 && degrees < 110) {
        return 3; //"Level 3"
    } else if (degrees >= 110 && degrees <= 250) {
        return 4; //"Level 4"
    }
    return 0;
}


/***** REQUIRED SETUP *****/
void setup() {
    // Start serial communication for debugging
    Serial.begin(SERIAL_BAUD);

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
    Serial.print("Initializing external IMU at address 0x6A... ");
    if (!externalIMU.begin_I2C(0x6A)) {  // Use I2C address 0x6A
        Serial.println("Failed to initialize external IMU!");
        while (1);
    }
    Serial.println("Success.");


    /***** POTENTIOMETER SETUP *****/
    // Setup input pins
    pinMode(potPin1, INPUT);
    pinMode(potPin2, INPUT);

    // Initialize stability readings to zero
    for (int i = 0; i < stabilityCheckCount; i++) {
        lastReadings1[i] = 0;
        lastReadings2[i] = 0;
    }
}


/***** EXERCISE ROUTINES *****/
// Function to send live angle data for exercise routines
void performExerciseRoutine(const ExerciseOptions& options) {
    // 1. Initial setup before test starts
    if (!testInProgress) {
        repsCounted = false;
        testInProgress = true;
        holdTime = 0;
        totalHoldTime = 0;
        averageHoldTime = 0;
        isHolding = false;

        // Reset angles for new test
        if (options.isTestRep) {
            maxPlantarDorsiAngle = -1e6;
            minPlantarDorsiAngle = 1e6;
            maxInversionEversionAngle = -1e6;
            minInversionEversionAngle = 1e6;
        }
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
        if (options.isPlantarExercise || options.isDorsiExercise) {
            if (options.isTestRep) {
                // Update range tracking
                maxPlantarDorsiAngle = max(maxPlantarDorsiAngle, plantarDorsiAngle);
                minPlantarDorsiAngle = min(minPlantarDorsiAngle, plantarDorsiAngle);
            } else {
                // Calculate rep max range
                float repMaxRange = REP_MIN_RANGE;
                if (maxPlantarDorsiAngle * 0.1 > REP_MIN_RANGE) {
                    repMaxRange = (maxPlantarDorsiAngle * 0.1);
                }
                // Calculate rep min range
                float repMinRange = REP_MIN_RANGE;
                if (minPlantarDorsiAngle * 0.1 > REP_MIN_RANGE) {
                    repMinRange = (minPlantarDorsiAngle * 0.1);
                }

                if (options.isPlantarExercise) {
                    // Calculate reps count
                    if (!repsCounted) {
                        // Check if a rep is completed (user reached near max and was moving upward)
                        // Counted from movement of min -> max angle
                        if (plantarDorsiAngle >= (maxPlantarDorsiAngle - repMaxRange)) {
                            // Increase hold time
                            if (!isHolding) {
                                holdStartTime = millis(); // Start timing
                                isHolding = true;
                            }
                            holdTime = (millis() - holdStartTime) / 1000.0f; // Convert to seconds
                            Serial.print("Hold time: ");
                            Serial.println(String(holdTime));

                            if (plantarDorsiAngle > previousPlantarDorsiAngle) {  // Ensure movement is upward
                                // Calculate rep
                                repsCount++;
                                repsCounted = true;
                                Serial.print("Reps counted: ");
                                Serial.println(String(repsCount));
                            }
                        }
                    } else {
                        // Calculate hold time when near max angle
                        if (plantarDorsiAngle >= (maxPlantarDorsiAngle - repMaxRange)) {
                            // Increase hold time
                            if (!isHolding) {
                                isHolding = true;
                            }
                            holdTime = (millis() - holdStartTime) / 1000.0f; // Convert to seconds
                            Serial.print("Hold time: ");
                            Serial.println(String(holdTime));
                        } else {
                            isHolding = false;
                        }

                        // Check if the next rep is started (user moved downward past the min threshold)
                        if (plantarDorsiAngle <= (minPlantarDorsiAngle + repMinRange) &&
                            plantarDorsiAngle < previousPlantarDorsiAngle) {  // Ensure movement is downward
                            // Calculate total hold time
                            totalHoldTime += holdTime;
                            holdTime = 0;

                            repsCounted = false;
                            Serial.println("Starting next rep!");
                        }
                    }
                } else if (options.isDorsiExercise) {
                    // Calculate reps count
                    if (!repsCounted) {
                        // Check if a rep is completed (user reached near min and was moving downward)
                        // Counted from movement of max -> min angle
                        if (plantarDorsiAngle <= (minPlantarDorsiAngle + repMinRange)) {
                            // Increase hold time
                            if (!isHolding) {
                                holdStartTime = millis(); // Start timing
                                isHolding = true;
                            }
                            holdTime = (millis() - holdStartTime) / 1000.0f; // Convert to seconds
                            Serial.print("Hold time: ");
                            Serial.println(String(holdTime));

                            if (plantarDorsiAngle < previousPlantarDorsiAngle) {  // Ensure movement is downward
                                // Calculate rep
                                repsCount++;
                                repsCounted = true;
                                Serial.print("Reps counted: ");
                                Serial.println(String(repsCount));
                            }
                        }
                    } else {
                        // Calculate hold time when near min angle
                        if (plantarDorsiAngle <= (minPlantarDorsiAngle + repMinRange)) {
                            // Increase hold time
                            if (!isHolding) {
                                isHolding = true;
                            }
                            holdTime = (millis() - holdStartTime) / 1000.0f; // Convert to seconds
                            Serial.print("Hold time: ");
                            Serial.println(String(holdTime));
                        } else {
                            isHolding = false;
                        }

                        // Check if the next rep is started (user moved upward past the max threshold)
                        if (plantarDorsiAngle >= (maxPlantarDorsiAngle - repMaxRange) &&
                            plantarDorsiAngle > previousPlantarDorsiAngle) {  // Ensure movement is upward
                            // Calculate total hold time
                            totalHoldTime += holdTime;
                            holdTime = 0;

                            repsCounted = false;
                            Serial.println("Starting next rep!");
                        }
                    }
                }
                previousPlantarDorsiAngle = plantarDorsiAngle;
            }

            // Send live data
            writeCharacteristicData(plantarDorsiAngle);
            Serial.print("Sending live angle (plantar/dorsi): ");
            Serial.println(String(plantarDorsiAngle));
        } else if (options.isInverExercise || options.isEverExercise) {
            if (options.isTestRep) {
                // Update range tracking
                maxInversionEversionAngle = max(maxInversionEversionAngle, inversionEversionAngle);
                minInversionEversionAngle = min(minInversionEversionAngle, inversionEversionAngle);
            } else {
                // Calculate rep max range
                float repMaxRange = REP_MIN_RANGE;
                if (maxInversionEversionAngle * 0.1 > REP_MIN_RANGE) {
                    repMaxRange = (maxInversionEversionAngle * 0.1);
                }
                // Calculate rep min range
                float repMinRange = REP_MIN_RANGE;
                if (minInversionEversionAngle * 0.1 > REP_MIN_RANGE) {
                    repMinRange = (minInversionEversionAngle * 0.1);
                }

                if (options.isInverExercise) {
                    // Calculate reps count
                    if (!repsCounted) {
                        // Check if a rep is completed (user reached near max and was moving upward)
                        // Counted from movement of min -> max angle
                        if (inversionEversionAngle >= (maxInversionEversionAngle - repMaxRange)) {
                            // Increase hold time
                            if (!isHolding) {
                                holdStartTime = millis(); // Start timing
                                isHolding = true;
                            }
                            holdTime = (millis() - holdStartTime) / 1000.0f; // Convert to seconds
                            Serial.print("Hold time: ");
                            Serial.println(String(holdTime));

                            if (inversionEversionAngle > previousInversionEversionAngle) {  // Ensure movement is upward
                                // Calculate rep
                                repsCount++;
                                repsCounted = true;
                                Serial.print("Reps counted: ");
                                Serial.println(String(repsCount));
                            }
                        }
                    } else {
                        // Calculate hold time when near max angle
                        if (inversionEversionAngle >= (maxInversionEversionAngle - repMaxRange)) {
                            // Increase hold time
                            if (!isHolding) {
                                isHolding = true;
                            }
                            holdTime = (millis() - holdStartTime) / 1000.0f; // Convert to seconds
                            Serial.print("Hold time: ");
                            Serial.println(String(holdTime));
                        } else {
                            isHolding = false;
                        }

                        // Check if the next rep is started (user moved downward past the min threshold)
                        if (inversionEversionAngle <= (minInversionEversionAngle + repMinRange) &&
                            inversionEversionAngle < previousInversionEversionAngle) {
                            // Calculate total hold time
                            totalHoldTime += holdTime;
                            holdTime = 0;

                            repsCounted = false;
                            Serial.println("Starting next rep!");
                        }
                    }
                } else if (options.isEverExercise) {
                    // Calculate reps count
                    if (!repsCounted) {
                        // Check if a rep is completed (user reached near min and was moving downward)
                        // Counted from movement of max -> min angle
                        if (inversionEversionAngle <= (minInversionEversionAngle + repMinRange)) {
                            // Increase hold time
                            if (!isHolding) {
                                holdStartTime = millis(); // Start timing
                                isHolding = true;
                            }
                            holdTime = (millis() - holdStartTime) / 1000.0f; // Convert to seconds
                            Serial.print("Hold time: ");
                            Serial.println(String(holdTime));

                            if (inversionEversionAngle < previousInversionEversionAngle) {  // Ensure movement is downward
                                // Calculate rep
                                repsCount++;
                                repsCounted = true;
                                Serial.print("Reps counted: ");
                                Serial.println(String(repsCount));
                            }
                        }
                    } else {
                        // Calculate hold time when near min angle
                        if (inversionEversionAngle <= (minInversionEversionAngle + repMinRange)) {
                            // Increase hold time
                            if (!isHolding) {
                                isHolding = true;
                            }
                            holdTime = (millis() - holdStartTime) / 1000.0f; // Convert to seconds
                            Serial.print("Hold time: ");
                            Serial.println(String(holdTime));
                        } else {
                            isHolding = false;
                        }

                        // Check if the next rep is started (user moved upward past the max threshold)
                        if (inversionEversionAngle >= (maxInversionEversionAngle - repMaxRange) &&
                            inversionEversionAngle > previousInversionEversionAngle) {
                            // Calculate total hold time
                            totalHoldTime += holdTime;
                            holdTime = 0;

                            repsCounted = false;
                            Serial.println("Starting next rep!");
                        }
                    }
                }
                previousInversionEversionAngle = inversionEversionAngle;
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
            romTestDuration = 0;

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
        String receivedData = "";
        if (readCharacteristic.written()) {
            receivedData = readCharacteristicData();
            Serial.print("Data received from Bluetooth: ");
            Serial.println(receivedData);

            // Clear the value after reading
            readCharacteristic.writeValue((uint8_t) 0);
        }

        // 4. Trigger test completion
        if (testInProgress && receivedData == "end_ROM") {
            romTestDuration = millis() - testStartTime;
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
            Serial.println("\nRange of Motion Test Results:");
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
            gaitTestDuration = 0;
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
        String receivedData = "";
        if (readCharacteristic.written()) {
            receivedData = readCharacteristicData();
            Serial.print("Data received from Bluetooth: ");
            Serial.println(receivedData);

            // Clear the value after reading
            readCharacteristic.writeValue((uint8_t) 0);
        }

        // 4. Trigger test completion
        if (testInProgress && receivedData == "end_Gait") {
            gaitTestDuration = millis() - testStartTime;
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


// Function to execute the potentiometer test once
void readPotentiometerTension(int &tension1, int &tension2) {
    int adcValue1 = 0, adcValue2 = 0;

    // Take multiple readings over 100ms to process stability
    for (int i = 0; i < stabilityCheckCount; i++) {
        adcValue1 = analogRead(potPin1);
        adcValue2 = analogRead(potPin2);

        // Store readings in circular buffer for noise detection
        lastReadings1[i] = adcValue1;
        lastReadings2[i] = adcValue2;

        delay(20); // Short delay between samples (5 samples in 100ms)
    }

    // Compute voltage
    float voltage1 = (adcValue1 / float(adcResolution - 1)) * referenceVoltage;
    float voltage2 = (adcValue2 / float(adcResolution - 1)) * referenceVoltage;

    // Check for unstable readings (fluctuations)
    bool isUnstable1 = false;
    bool isUnstable2 = false;

    for (int i = 1; i < stabilityCheckCount; i++) {
        if (abs(lastReadings1[i] - lastReadings1[i - 1]) > 5) {
            isUnstable1 = true;
            break;
        }
    }
    for (int i = 1; i < stabilityCheckCount; i++) {
        if (abs(lastReadings2[i] - lastReadings2[i - 1]) > 5) {
            isUnstable2 = true;
            break;
        }
    }

    // Determine degree values (set to 0° if unstable or out of range)
    float degrees1 = (adcValue1 >= minADC && adcValue1 <= maxADC && !isUnstable1)
                     ? map(adcValue1, minADC, maxADC, minDegrees, maxDegrees)
                     : 2; // Unstable level
    float degrees2 = (adcValue2 >= minADC && adcValue2 <= maxADC && !isUnstable2)
                     ? map(adcValue2, minADC, maxADC, minDegrees, maxDegrees)
                     : 2; // Unstable level

    // Determine levels
    tension1 = getTensionLevel2(degrees2);  // todo: getTensionLevel1, degrees2
    tension2 = getTensionLevel2(degrees2);

    // Print output for both potentiometers
    Serial.print("POT 1 | ADC: ");
    Serial.print(adcValue1);
    Serial.print(" | Voltage: ");
    Serial.print(voltage1, 3);
    Serial.print(" V | Degrees: ");
    Serial.print(degrees1, 1);
    Serial.print("° | Position: ");
    Serial.println(tension1);

    Serial.print("POT 2 | ADC: ");
    Serial.print(adcValue2);
    Serial.print(" | Voltage: ");
    Serial.print(voltage2, 3);
    Serial.print(" V | Degrees: ");
    Serial.print(degrees2, 1);
    Serial.print("° | Position: ");
    Serial.println(tension2);

    Serial.println("---------------------------------------------------");
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

                    /***** SEND TENSION LEVEL OF DEVICE TO APP *****/
                    // Updates tension levels by pass by reference
                    readPotentiometerTension(tensionLevel1, tensionLevel2);

                    // Send tension to app
                    writeCharacteristicData(tensionLevel1, tensionLevel2);
                    Serial.print("Sending tension levels: ");
                    Serial.println("(" + String(tensionLevel1) + ", " + String(tensionLevel2) + ")");

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
                                    performExerciseRoutine(isPlantarTestOptions);

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
                                    performExerciseRoutine(isDorsiTestOptions);

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
                                    performExerciseRoutine(isInverTestOptions);

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
                                    performExerciseRoutine(isEverTestOptions);

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
                                averageHoldTime = 0;

                                Serial.println("Device is starting Plantar Flexion exercise!");
                                delay(50);

                                /***** START PLANTAR FLEXION EXERCISE *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performExerciseRoutine(isPlantarOptions);

                                    /***** FINISH SET *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "end_set") {
                                            /***** SET - SEND REPS COMPLETED *****/
                                            if (repsCounted) {
                                                // Add extra hold time
                                                totalHoldTime += holdTime;
                                            }
                                            averageHoldTime = totalHoldTime / repsCount;
                                            Serial.println("Set Complete!");
                                            writeCharacteristicData(repsCount, averageHoldTime);
                                            Serial.print("Sending reps count / hold time: ");
                                            Serial.println("(" + String(repsCount) + ", " + String(averageHoldTime) + ")");
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "start_dorsiflexion") {
                                // Setup variables for exercise
                                repsCount = 0;
                                averageHoldTime = 0;

                                Serial.println("Device is starting Dorsiflexion exercise!");
                                delay(50);

                                /***** START DORSIFLEXION EXERCISE *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performExerciseRoutine(isDorsiOptions);

                                    /***** FINISH SET *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "end_set") {
                                            /***** SET - SEND REPS COMPLETED *****/
                                            if (repsCounted) {
                                                // Add extra hold time
                                                totalHoldTime += holdTime;
                                            }
                                            averageHoldTime = totalHoldTime / repsCount;
                                            Serial.println("Set Complete!");
                                            writeCharacteristicData(repsCount, averageHoldTime);
                                            Serial.print("Sending reps count / hold time: ");
                                            Serial.println("(" + String(repsCount) + ", " + String(averageHoldTime) + ")");
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "start_inversion") {
                                // Setup variables for exercise
                                repsCount = 0;
                                averageHoldTime = 0;

                                Serial.println("Device is starting Inversion exercise!");
                                delay(50);

                                /***** START INVERSION EXERCISE *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performExerciseRoutine(isInverOptions);

                                    /***** FINISH SET *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "end_set") {
                                            /***** SET - SEND REPS COMPLETED *****/
                                            if (repsCounted) {
                                                // Add extra hold time
                                                totalHoldTime += holdTime;
                                            }
                                            averageHoldTime = totalHoldTime / repsCount;
                                            Serial.println("Set Complete!");
                                            writeCharacteristicData(repsCount, averageHoldTime);
                                            Serial.print("Sending reps count / hold time: ");
                                            Serial.println("(" + String(repsCount) + ", " + String(averageHoldTime) + ")");
                                            break;
                                        }
                                    }
                                } // end while
                            } else if (receivedData == "start_eversion") {
                                // Setup variables for exercise
                                repsCount = 0;
                                averageHoldTime = 0;

                                Serial.println("Device is starting Eversion exercise!");
                                delay(50);

                                /***** START EVERSION EXERCISE *****/
                                while (central.connected()) {
                                    delay(50);

                                    /***** SEND LIVE DATA *****/
                                    performExerciseRoutine(isEverOptions);

                                    /***** FINISH SET *****/
                                    if (readCharacteristic.written()) {
                                        String receivedData = readCharacteristicData();
                                        Serial.print("Data received from Bluetooth: ");
                                        Serial.println(receivedData);

                                        // Clear the value after reading
                                        readCharacteristic.writeValue((uint8_t) 0);

                                        if (receivedData == "end_set") {
                                            /***** SET - SEND REPS COMPLETED *****/
                                            if (repsCounted) {
                                                // Add extra hold time
                                                totalHoldTime += holdTime;
                                            }
                                            averageHoldTime = totalHoldTime / repsCount;
                                            Serial.println("Set Complete!");
                                            writeCharacteristicData(repsCount, averageHoldTime);
                                            Serial.print("Sending reps count / hold time: ");
                                            Serial.println("(" + String(repsCount) + ", " + String(averageHoldTime) + ")");
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
                                currentState = GAIT_IDLE;

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
