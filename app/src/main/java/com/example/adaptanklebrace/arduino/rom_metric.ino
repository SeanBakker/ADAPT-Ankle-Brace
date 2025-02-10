#include <Arduino_BMI270_BMM150.h>  // Library for the built-in IMU
#include <Wire.h>
#include <Adafruit_ISM330DHCX.h>    // External IMU library
#include <math.h>

// Create sensor object for the external IMU
Adafruit_ISM330DHCX externalIMU;  // External IMU object

#define SERIAL_BAUD 115200
const float alpha = 0.5;             // Smoothing factor for low-pass filter
const float jumpThreshold = 95.0;    // Maximum allowed jump between samples, in degrees
const float GIMBAL_LOCK_THRESHOLD = 2.0;  // Threshold in degrees around ±90 for gimbal lock

// Variables to hold filtered roll and pitch
float filteredRollBuiltIn = 0;
float filteredPitchBuiltIn = 0;
float filteredRollExternal = 0;
float filteredPitchExternal = 0;

// Variables to hold last good readings
float lastValidPlantarDorsi = 0;
float lastValidInversionEversion = 0;
bool initialReadingSet = false;  // Flag to allow first readings to pass without rejection

// Test-related variables
bool testInProgress = false;
bool continuousPrint = false;       // If true, sensor values are printed continuously during the test
unsigned long testStartTime = 0;    // Start time of the test
const unsigned long testDuration = 5000;  // 5 seconds
float maxPlantarDorsiAngle = -1e6;  // Start with very low number
float minPlantarDorsiAngle = 1e6;   // Start with very high number
float maxInversionEversionAngle = -1e6; // For tracking inversion/eversion
float minInversionEversionAngle = 1e6;  // For tracking inversion/eversion

// Variables to hold readings as uint8_t
uint8_t livePlantarDorsiAngle = 0;
uint8_t liveInversionEversionAngle = 0;
uint8_t finalplantarDorsiRange = 0;
uint8_t inversionEversionRange = 0;

void setup() {
    Serial.begin(SERIAL_BAUD);
    while (!Serial);  // Wait for the Serial Monitor to open

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

    Serial.println("Type 'go' to start a 5-second range of motion test without printing sensor values.");
    Serial.println("Type 'go+' to start a 5-second range of motion test with continuous sensor value printing.");
    Serial.println("----------------------------------------------------------------------------------------------------");
    Serial.println("Built-in IMU            | External IMU           | Calculated Angles                                ");
    Serial.println("Roll        Pitch       | Roll         Pitch     | Plantar/Dorsi            Inversion/Eversion      ");
    Serial.println("----------------------------------------------------------------------------------------------------");
}

void loop() {
    // Check for test start commands
    if (!testInProgress && Serial.available() > 0) {
        String input = Serial.readStringUntil('\n');
        input.trim();  // Remove any leading/trailing whitespace

        if (input.equalsIgnoreCase("go")) {
            // Start the 5-second test without continuous printing
            testInProgress = true;
            continuousPrint = false;
            testStartTime = millis();
            maxPlantarDorsiAngle = -1e6; // Reset for new test
            minPlantarDorsiAngle = 1e6;  // Reset for new test
            maxInversionEversionAngle = -1e6;
            minInversionEversionAngle = 1e6;
            Serial.println("Starting 5-second range of motion test without printing sensor values...");
        } else if (input.equalsIgnoreCase("go+")) {
            // Start the 5-second test with continuous printing
            testInProgress = true;
            continuousPrint = true;
            testStartTime = millis();
            maxPlantarDorsiAngle = -1e6; // Reset for new test
            minPlantarDorsiAngle = 1e6;  // Reset for new test
            maxInversionEversionAngle = -1e6;
            minInversionEversionAngle = 1e6;
            Serial.println("Starting 5-second range of motion test with continuous sensor values printing...");
        }
    }

    // If the test is in progress, check if 5 seconds have elapsed
    if (testInProgress && millis() - testStartTime >= testDuration) {
        testInProgress = false;

        // Calculate the maximum range of motion for plantar/dorsi and inversion/eversion
        float plantarDorsiRange = maxPlantarDorsiAngle - minPlantarDorsiAngle;
        float inversionEversionRange = maxInversionEversionAngle - minInversionEversionAngle;

        // Bluetooth final values
        uint8_t finalplantarDorsiRange = (uint8_t)round(finalplantarDorsiRange);
        uint8_t finalinversionEversionRange = (uint8_t)round(finalinversionEversionRange);

        // Output the test results
        Serial.println("\n5-second Range of Motion Test Results:");
        Serial.print("Maximum Plantar Flexion / Dorsiflexion Range (degrees): ");
        Serial.println(plantarDorsiRange, 2);
        Serial.print("Maximum Inversion / Eversion Range (degrees): ");
        Serial.println(inversionEversionRange, 2);
        Serial.println("-------------------------------------------------------------------------------------");
        Serial.println("Type 'go' to start a new test without printing sensor values.");
        Serial.println("Type 'go+' to start a new test with continuous sensor values printing.");
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

        // Store live values as uint8_t
        livePlantarDorsiAngle = (uint8_t)round(plantarDorsiAngle);
        liveInversionEversionAngle = (uint8_t)round(inversionEversionAngle);
    }

    delay(50);  // Sampling delay
}
