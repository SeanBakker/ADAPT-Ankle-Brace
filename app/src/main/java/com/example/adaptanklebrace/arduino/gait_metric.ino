/**
 *   1. Waits for user to type 's' to start an 8-second test.
 *   2. Uses a finite-state machine (FSM) to track:
 *         GAIT_IDLE -> WAIT_HEEL_STRIKE -> STANCE -> SWING -> (repeat)
 *   3. Captures metrics: step intervals, stance times, swing times, peak forces, etc.
 *   4. Computes a Gait Health Score [1..10] using sub-scores:
 *         - Average Step Interval
 *         - Step Interval Consistency
 *         - Average Peak Force
 *         - Swing/Stance Ratio
 *   5. Prints final results + score after 8 seconds.
 *
 * Dependencies:
 *   - Arduino_LSM6DS3 or equivalent library for Nano 33 IoT's IMU
 */

#include <Arduino_LSM6DS3.h>
#include <math.h> // for sqrt()

// ------------------------- USER SETTINGS ------------------------- //

// Test length: 10 seconds
const unsigned long TEST_DURATION_MS = 10000;

// Sampling interval ~100 Hz
const unsigned long SAMPLING_INTERVAL = 10;

// Thresholds for heel strike and toe-off
// Tweak these for your data.
const float HEEL_STRIKE_THRESHOLD = -0.6f; //1.0 0.8
const float TOE_OFF_THRESHOLD     = 0.0f; //0.1
const float CHANGE_THRESHOLD = 0.2f;

// Minimum time between valid steps (to avoid double-counting)
const unsigned long MIN_STEP_INTERVAL_MS = 250;

// Minimum stance duration (avoid stance phases that end too quickly)
const unsigned long MINIMUM_STANCE_MS = 400; //300

// Gyro threshold for detecting foot rotation or “unstable” foot
// if you want to incorporate orientation changes:
const float GYRO_STABILITY_THRESHOLD = 30.0f;

// Low-pass filter factor for accelerometer
const float alphaAcc = 0.7;

// Data array sizes
const int MAX_STEPS = 50;


// ------------------------- GLOBALS ------------------------- //

// For the state machine
enum GaitState {
    GAIT_IDLE,
    WAIT_HEEL_STRIKE,
    STANCE,
    SWING
};

GaitState currentState = GAIT_IDLE;

// Test control
bool testStarted      = false;
bool testCompleted    = false;
unsigned long testStartTime = 0;
unsigned long lastSampleTime = 0;

// Bookkeeping
unsigned long lastHeelStrikeTime = 0;  // last time we detected a heel strike
float azFiltered      = 0.0f;
float tempPeakForce   = 0.0f;

int   stepIndex       = 0;
int   totalSteps      = 0;
unsigned long firstStepTime = 0;
unsigned long finalStepTime = 0;

// Arrays to store intervals & forces
float stepIntervals[MAX_STEPS];
float peakForces[MAX_STEPS];

float prev_az = 0;

// Arrays to store stance / swing boundaries
unsigned long stepStartTime[MAX_STEPS];  // stance start
unsigned long stepEndTime[MAX_STEPS];    // stance end

// ----------------------------------------------------------- //
void setup() {
    Serial.begin(115200);
    while (!Serial);

    // Initialize IMU
    if (!IMU.begin()) {
        // Serial.println("Failed to initialize IMU! Check wiring / library.");
        while (1);
    }

    Serial.println("IMU initialized. Type 's' to start 10-second test.");
    // Serial.println("----------------------------------------");
}

// ----------------------------------------------------------- //
void loop() {
    // 1. Handle test start
    if (!testStarted && currentState == GAIT_IDLE) {
        if (Serial.available() > 0) {
            char c = Serial.read();
            if (c == 's') {
                testStarted   = true;
                testStartTime = millis();
                currentState  = WAIT_HEEL_STRIKE;  // Move to waiting for first heel strike
                Serial.println("Test started! Move or walk now for 10 seconds...");
            }
        }
        return;
    }

    // 2. Check test completion
    if (!testCompleted && (millis() - testStartTime >= TEST_DURATION_MS)) {
        testCompleted = true;
        printFinalMetrics();
        Serial.println("Test finished. Restart or reset to run again.");
        while (1) {
            // idle
        }
    }

    // 3. If test is running, do sampling & state machine
    if (!testCompleted) {
        unsigned long currentTime = millis();
        if (currentTime - lastSampleTime >= SAMPLING_INTERVAL) {
            lastSampleTime = currentTime;
            float ax, ay, az;
            float gx, gy, gz;

            // read both acceleration & gyroscope if available
            if (IMU.accelerationAvailable() && IMU.gyroscopeAvailable()) {
                IMU.readAcceleration(ax, ay, az);
                IMU.readGyroscope(gx, gy, gz);

                // invert z-axis if sensor is upside down
                float az_inverted = az; //removed negative sign

                // optional low-pass filter
                azFiltered = alphaAcc * azFiltered + (1.0f - alphaAcc) * az_inverted;
                float azToUse = azFiltered;

                // measure total gyro for foot "stability"
                float gyroMagnitude = sqrt(gx*gx + gy*gy + gz*gz);

                // 3.1. State Machine
                switch (currentState) {

                    case WAIT_HEEL_STRIKE: {
                        // Looking for a new heel strike
                        bool heelStrikeDetected =
                                (azToUse < HEEL_STRIKE_THRESHOLD) && (prev_az-azToUse > CHANGE_THRESHOLD) &&
                                (currentTime - lastHeelStrikeTime > MIN_STEP_INTERVAL_MS);
                        if (heelStrikeDetected) {
                            // We have a heel strike
                            totalSteps++;
                            if (firstStepTime == 0) {
                                firstStepTime = currentTime;
                            }

                            // Step interval from previous heel strike
                            if (lastHeelStrikeTime != 0 && stepIndex < MAX_STEPS) {
                                float intervalMs = (float)(currentTime - lastHeelStrikeTime);
                                stepIntervals[stepIndex] = intervalMs;
                            }

                            lastHeelStrikeTime = currentTime;
                            tempPeakForce      = azToUse;

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
                        bool belowToeOff  = (azToUse < TOE_OFF_THRESHOLD);
                        bool enoughStance = (stanceElapsed >= MINIMUM_STANCE_MS);
                        bool footUnstable = (gyroMagnitude > GYRO_STABILITY_THRESHOLD);

                        // If we see a valid toe-off condition, go to SWING
                        if (enoughStance && belowToeOff && footUnstable) {
                            finalStepTime = currentTime;
                            // Serial.println("Lift");
                            // Serial.println(azToUse);
                            // store peak force
                            if (stepIndex < MAX_STEPS) {
                                peakForces[stepIndex] = tempPeakForce;
                            }
                            // mark stance end
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

                            // increment stepIndex
                            stepIndex++;

                            // Next state: SWING
                            currentState = SWING;
                        }
                        break;
                    }

                    case SWING: {
                        // We remain in SWING until we detect the next heel strike
                        bool nextHeelStrike =
                                (azToUse < HEEL_STRIKE_THRESHOLD) && (prev_az-azToUse > CHANGE_THRESHOLD) &&
                                (currentTime - lastHeelStrikeTime > MIN_STEP_INTERVAL_MS);

                        if (nextHeelStrike) {
                            // new stance begins
                            // Serial.println("Strike");
                            // Serial.println(azToUse);
                            totalSteps++;
                            if (firstStepTime == 0) {
                                firstStepTime = currentTime;
                            }

                            // step interval
                            if (lastHeelStrikeTime != 0 && stepIndex < MAX_STEPS) {
                                float intervalMs = (float)(currentTime - lastHeelStrikeTime);
                                stepIntervals[stepIndex] = intervalMs; // store next interval
                            }

                            lastHeelStrikeTime = currentTime;
                            tempPeakForce      = azToUse;

                            // Mark stance start
                            if (stepIndex < MAX_STEPS) {
                                stepStartTime[stepIndex] = currentTime;
                            }

                            currentState = STANCE;
                        }
                        break;
                    }

                    default:
                        // GAIT_IDLE or any fallback
                        break;
                }
                prev_az = azToUse;
            }
        }
    }
}

// ----------------------------------------------------------- //
// Print final metrics at the end
void printFinalMetrics() {
    Serial.println("");
    // Serial.println("***** 8-SECOND TEST SUMMARY *****");
    Serial.print("Total Steps Detected: ");
    Serial.println(totalSteps);

    // Average Peak Force
    if (stepIndex == 0) {
        Serial.println("No peak force data (less than 1 step).");
    } else {
        float sumPeak = 0.0f;
        for (int i = 0; i < stepIndex; i++) {
            sumPeak += peakForces[i];
        }
        float avgPeak = sumPeak / (float)stepIndex;
        Serial.print("Average Peak Force: ");
        Serial.print(avgPeak, 3);
        Serial.println(" G");
    }

    // Cadence
    if (firstStepTime == 0) {
        Serial.println("No valid steps for cadence calculation.");
    } else {
        unsigned long endTime = (finalStepTime == 0)
                                ? (testStartTime + TEST_DURATION_MS)
                                : finalStepTime;
        float elapsedSec = (float)(endTime - firstStepTime) / 1000.0f;
        if (elapsedSec > 0.0f) {
            float stepsPerMin = (float)totalSteps / elapsedSec * 60.0f;
            Serial.print("Cadence: ");
            Serial.print(stepsPerMin, 1);
            Serial.println(" steps/min");
        } else {
            Serial.println("Cadence: insufficient time for calculation.");
        }
    }

    // Compute stance/swing ratio
    float swingstanceratio = computeSwingStanceRatio();

    Serial.println("************************************");
}

// ----------------------------------------------------------- //
// Compute stance & swing from stepStartTime[i], stepEndTime[i]
float computeSwingStanceRatio() {
    if (stepIndex == 0) {
        Serial.println("Not enough data to compute stance/swing times.");
        //return;
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
        //return;
    }
    Serial.print("Average Swing Time: ");
    Serial.print(avgSwingSec, 3);
    Serial.println(" s");

    if (avgStanceSec > 0.0f) {
        float ratio = avgSwingSec / avgStanceSec;
        Serial.print("Swing/Stance Ratio: ");
        Serial.print(ratio, 3);
        Serial.println(" (Ideal ~ 0.67)");
        return ratio;
    } else {
        Serial.println("Invalid stance time for ratio calculation.");
    }
}
