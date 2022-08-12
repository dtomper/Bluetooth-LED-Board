#include <SoftwareSerial.h>
#include <MD_Parola.h>
#include <MD_MAX72xx.h>
#include <SPI.h>

// set to 1 if we are implementing the user interface pot, switch, etc
#define USE_UI_CONTROL 0

#if USE_UI_CONTROL
#include <MD_UISwitch.h>
#endif

// Turn on debug statements to the serial output
#define DEBUG 0

#if DEBUG
#define PRINT(s, x) { Serial.print(F(s)); Serial.print(x); }
#define PRINTS(x) Serial.print(F(x))
#define PRINTX(x) Serial.println(x, HEX)
#else
#define PRINT(s, x)
#define PRINTS(x)
#define PRINTX(x)
#endif

// Bluetooth configuration
#define BLUETOOTH_RX 4 // D4
#define BLUETOOTH_TX 5 // D5

// Define the number of devices we have in the chain and the hardware interface
// NOTE: These pin numbers will probably not work with your hardware and may
// need to be adapted
#define HARDWARE_TYPE MD_MAX72XX::FC16_HW
#define MAX_DEVICES 4
#define CLK_PIN   13 // D13
#define DATA_PIN  11 // D11
#define CS_PIN    10 // D10

// Create Bluetooth object
SoftwareSerial MyBluetooth(BLUETOOTH_RX, BLUETOOTH_TX);

// HARDWARE SPI
MD_Parola P = MD_Parola(HARDWARE_TYPE, CS_PIN, MAX_DEVICES);
// SOFTWARE SPI
// MD_Parola P = MD_Parola(HARDWARE_TYPE, DATA_PIN, CLK_PIN, CS_PIN, MAX_DEVICES);

const uint8_t SPEED_DEADBAND = 5;

textEffect_t  effect[] =
{
  PA_NO_EFFECT,
  PA_PRINT,
  PA_SCROLL_LEFT,
  PA_SCROLL_UP_LEFT,
  PA_SCROLL_UP,
  PA_SCROLL_UP_RIGHT,
  PA_SCROLL_RIGHT,
  PA_SCROLL_DOWN_RIGHT,
  PA_SCROLL_DOWN,
  PA_SCROLL_DOWN_LEFT,
  PA_SCAN_HORIZ,
  PA_SCAN_VERT,
  PA_WIPE,
  PA_WIPE_CURSOR,
  PA_OPENING_CURSOR,
  PA_CLOSING_CURSOR,
  PA_GROW_UP,
  PA_GROW_DOWN,
  PA_MESH,
  PA_BLINDS,
  PA_OPENING,
  PA_CLOSING,
  PA_RANDOM,
  PA_DISSOLVE,
  PA_SLICE,
};
int maxEffectIndex = sizeof(effect) / sizeof(effect[0]) - 1;
int effectInIndex = 2;
int effectOutIndex = 2;
uint8_t effectDelay = 25;    // default frame delay value
uint16_t effectPause = 0; // in milliseconds
int intensity = 10;
int maxIntensity = 15;

textPosition_t textAlign[] = 
{
  PA_LEFT,
  PA_CENTER,
  PA_RIGHT,
};
int maxAlignIndex = sizeof(textAlign) / sizeof(textAlign[0]) - 1;
int alignIndex = 0;

// Global message buffers shared by Serial and Scrolling functions
#define	BUF_SIZE	75
char curMessage[BUF_SIZE] = { "" };
char newMessage[BUF_SIZE];
bool newMessageAvailable = true;

#if USE_UI_CONTROL

MD_UISwitch_Digital uiDirection(DIRECTION_SET);
MD_UISwitch_Digital uiInvert(INVERT_SET);

void doUI(void)
{
  // set the speed if it has changed
  {
    int16_t  speed = map(analogRead(SPEED_IN), 0, 1023, 0, 250);

    if (speed != (int16_t)P.getSpeed())
    {
      P.setSpeed(speed);
      P.setPause(speed);
      effectDelay = speed;
      PRINT("\nChanged speed to ", P.getSpeed());
    }
  }
}
#endif // USE_UI_CONTROL

void setup()
{
  Serial.begin(57600);

  // Begin Bluetooth serial
  MyBluetooth.begin(9600);

#if USE_UI_CONTROL
  uiDirection.begin();
  uiInvert.begin();
  pinMode(SPEED_IN, INPUT);

  doUI();
#endif // USE_UI_CONTROL

  P.begin();
  P.displayText(curMessage, textAlign[alignIndex], effectDelay, effectPause, effect[effectInIndex], effect[effectOutIndex]);
  P.setIntensity(intensity);

  String temp = "Waiting for connection...";
  strcpy(newMessage, temp.c_str());
}

bool isPositiveInteger(String str) {
  for (char x: str) {
    if (not isDigit(x)) return false;
  }
  return true;
}

void parseSetAllCommand(String command) {
  // Command Syntax:
  // Some Text To Display|alignIndex|effectInIndex|effectOutIndex|effectDelay|effectPause|intensity
  // Example:
  // Hello World|1|2|3|25|2000|10

  // Check if there are 6 delimiters
  int count = 0;
  for (char x: command) {
    if (x == '|') count += 1;
    if (count >= 7) {
      Serial.println("Command contains more than 6 delimiters");
      return;
    }
  }
  if (count != 6) {
    Serial.println("Command contains less than 6 delimiters");
    return;
  }

  // Get parameters from command
  String temp = "";
  String parameters[7];
  int i = 0;
  for (char x: command)
  {
    if (x != '|') {
      temp.concat(x);
      continue;
    }
    
    parameters[i] = temp;
    i += 1;
    temp = "";
  }
  parameters[i] = temp;

  // Crop text if it's too big
  String text = parameters[0];
  if (text.length() > BUF_SIZE) {
    Serial.println("Text too big, It'll be cropped to " + String(BUF_SIZE) + " Characters.");
    text = text.substring(0, BUF_SIZE);
  }

  // Check if the remaining parameters are integers
  String _alignIndex = parameters[1];
  String _effectInIndex = parameters[2];
  String _effectOutIndex = parameters[3];
  String _effectDelay = parameters[4];
  String _effectPause = parameters[5];
  String _intensity = parameters[6];

  if (not isPositiveInteger(_alignIndex + _effectInIndex + _effectOutIndex + _effectDelay + _effectPause + _intensity))
  {
    Serial.println("Parameters that are supposed to be positive integers, are not.");
    return;
  }

  // Check if indices are valid
  if (_alignIndex.toInt() > maxAlignIndex) {
    Serial.println("alignIndex is too big.");
    return;
  }
  if (_effectInIndex.toInt() > maxEffectIndex) {
    Serial.println("effectInIndex is too big.");
    return;
  }
  if (_effectOutIndex.toInt() > maxEffectIndex) {
    Serial.println("effectOutIndex is too big.");
    return;
  }
  if (_intensity.toInt() > maxIntensity) {
    _intensity = String(maxIntensity);
  }
  
  // Set parameters
  strcpy(curMessage, text.c_str());
  P.setTextBuffer(curMessage);
  P.displayReset();
  P.displayClear();

  alignIndex = _alignIndex.toInt();
  P.setTextAlignment(textAlign[alignIndex]);

  effectInIndex = _effectInIndex.toInt();
  effectOutIndex = _effectOutIndex.toInt();
  P.setTextEffect(effect[effectInIndex], effect[effectOutIndex]);

  effectDelay = _effectDelay.toInt();
  P.setSpeed(effectDelay);

  effectPause = _effectPause.toInt();
  P.setPause(effectPause);

  intensity = _intensity.toInt();
  P.setIntensity(intensity);

  MyBluetooth.print("`OK`");
}

void handleGetAllCommand() {
  String command = "`" + String(curMessage) + "|" + String(alignIndex) + "|" + String(effectInIndex) + "|" + String(effectOutIndex) + "|" + String(effectDelay) + "|" + String(effectPause) + "|" + String(intensity) + "`";
  MyBluetooth.print(command);
}

void parseSetDelayCommand(String command) {
  if (command == "") return;

  if (not isPositiveInteger(command)) {
    Serial.println("Delay must be a positive integer.");
    return;
  }

  P.setSpeed(command.toInt());
}

void parseSetPauseCommand(String command) {
  if (command == "") return;

  if (not isPositiveInteger(command)) {
    Serial.println("Pause must be a positive integer.");
    return;
  }

  P.setPause(command.toInt());
}

void parseSetIntensityCommand(String command) {
  if (command == "") return;

  if (not isPositiveInteger(command)) {
    Serial.println("Intensity must be a positive integer.");
    return;
  }

  P.setIntensity(command.toInt());
}

String getBluetoothDataString() {
  String data = "";
  while (MyBluetooth.available()) {
    char character = MyBluetooth.read();
    Serial.println(character);
    data.concat(character);
  }
  return data;
}

void loop()
{
#if USE_UI_CONTROL
  doUI();
#endif // USE_UI_CONTROL

  if (MyBluetooth.available()) {
    String data = getBluetoothDataString();
    if (data.startsWith("setAll ")) {
      parseSetAllCommand(data.substring(7, data.length()));
    }
    else if (data.startsWith("setDelay ")) {
      parseSetDelayCommand(data.substring(9, data.length()));
    }
    else if (data.startsWith("setPause ")) {
      parseSetPauseCommand(data.substring(9, data.length()));
    }
    else if (data.startsWith("setIntensity ")) {
      parseSetIntensityCommand(data.substring(13, data.length()));
    }
    else if (data.equals("getAll")) {
      handleGetAllCommand();
    }
  }

  if (P.displayAnimate() || newMessageAvailable)
  {
    if (newMessageAvailable)
    {
      strcpy(curMessage, newMessage);
      newMessageAvailable = false;
    }
    P.displayReset();
  }
}
