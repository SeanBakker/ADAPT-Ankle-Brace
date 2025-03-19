// Define the analog input pins
const int potPin1 = A1;  // First potentiometer on A0
const int potPin2 = A0;  // Second potentiometer on A1
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

void setup() {
    Serial.begin(115200);  // Start serial communication
    while (!Serial);  // Wait for serial to connect (useful for native USB)
    pinMode(potPin1, INPUT);
    pinMode(potPin2, INPUT);

    // Initialize stability readings to zero
    for (int i = 0; i < stabilityCheckCount; i++) {
        lastReadings1[i] = 0;
        lastReadings2[i] = 0;
    }

    Serial.println("Ready. Type 'go' to start test.");
}

void loop() {
    if (Serial.available() > 0) {
        String command = Serial.readStringUntil('\n'); // Read command
        command.trim(); // Remove any extra spaces or newline characters

        if (command.equals("go")) {
            runPotentiometerTest(); // Run test once
            Serial.println("Test complete. Type 'go' to run again.");
        }
    }
}

// Function to execute the potentiometer test once
void runPotentiometerTest() {
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
                        : 0;
    float degrees2 = (adcValue2 >= minADC && adcValue2 <= maxADC && !isUnstable2)
                        ? map(adcValue2, minADC, maxADC, minDegrees, maxDegrees)
                        : 0;

    // Determine levels
    String level1 = getLevel(degrees1);
    String level2 = getLevel(degrees2);

    // Print output for both potentiometers
    Serial.print("POT 1 | ADC: ");
    Serial.print(adcValue1);
    Serial.print(" | Voltage: ");
    Serial.print(voltage1, 3);
    Serial.print(" V | Degrees: ");
    Serial.print(degrees1, 1);
    Serial.print("° | Position: ");
    Serial.println(level1);

    Serial.print("POT 2 | ADC: ");
    Serial.print(adcValue2);
    Serial.print(" | Voltage: ");
    Serial.print(voltage2, 3);
    Serial.print(" V | Degrees: ");
    Serial.print(degrees2, 1);
    Serial.print("° | Position: ");
    Serial.println(level2);

    Serial.println("---------------------------------------------------"); // Separator for readability
}

// Function to determine the level from the degree value
String getLevel(float degrees) {
    if (degrees >= 70 && degrees < 160) {
        return "Level 1 or Level 5";
    } else if (degrees >= 160 && degrees < 255) {
        return "Level 2 or Level 6";
    } else if (degrees >= 255 || degrees < 10) {
        return "Level 3";
    } else if (degrees >= 10 && degrees < 70) {
        return "Level 4";
    }
//    if (degrees >= 0 && degrees < 90) {
//        return "Level 1 or Level 5";
//    } else if (degrees >= 90 && degrees < 180) {
//        return "Level 2 or Level 6";
//    } else if (degrees >= 180 && degrees < 270) {
//        return "Level 3";
//    } else if (degrees >= 270 && degrees <= 330) {
//        return "Level 4";
//    }
    return "Unknown";
}
