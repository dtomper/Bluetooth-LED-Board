# Bluetooth-LED-Board
Max7219 Arduino LED Board(s) controlled from Android via Bluetooth

## Required Hardware
Max7219 8x8 LED Matrix (You can connect as many as you want and configure this [line](https://github.com/dtomper/Bluetooth-LED-Board/blob/main/Arduino%20Script/Bluetooth_Board.ino#L34) in the Arduino script).<br/>
HC05 Bluetooth Module.<br/>
A Breadboard is recommended.

## Required Libraries
[Parola](https://www.arduino.cc/reference/en/libraries/md_parola/) library for Arduino.

## Arduino Wiring
### Connecting the HC05 Module
1. Connect the VCC pin with the 5V pin in Arduino (It is recommended to use a breadboard because you'll need the 5V pin later).
2. Connect the GND pin with the GND pin in Arduino.
3. Connect the TXD pin with D4 (TXD should be connected with RXD in Arduino, it could be any digital pin AFAIK, I chose D4 as shown in this [line](https://github.com/dtomper/Bluetooth-LED-Board/blob/main/Arduino%20Script/Bluetooth_Board.ino#L27)).
4. Connect the RXD pin with D5 (Same story here, I chose D5 as shown in this [line](https://github.com/dtomper/Bluetooth-LED-Board/blob/main/Arduino%20Script/Bluetooth_Board.ino#L28)).
5. Bluetooth module all set!

### Connecting the LED Board
1. Connect the VCC pin with the 5V pin in Arduino (In case you don't have a breadboard, you can use the VCC pin in Arduino, but it's not recommended).
2. Connect the GND pin with the GND pin in Arduino.
3. Connect the DIN pin with D11 (As shown in this [line](https://github.com/dtomper/Bluetooth-LED-Board/blob/main/Arduino%20Script/Bluetooth_Board.ino#L36)).
4. Connect the CS  pin with D10 (As shown in this [line](https://github.com/dtomper/Bluetooth-LED-Board/blob/main/Arduino%20Script/Bluetooth_Board.ino#L37)).
5. Connect the CLK pin with D13 (As shown in this [line](https://github.com/dtomper/Bluetooth-LED-Board/blob/main/Arduino%20Script/Bluetooth_Board.ino#L35)).

## Usage
1. Compile and Run the Arduino script.
2. Pair your Android device with the HC05 Bluetooth module (password is 1234).
3. Run the [Android app](https://github.com/dtomper/Bluetooth-LED-Board/releases/tag/v1.0.0), Select device from the first drop down and click Connect.
4. Once connected, You should be able to configure your LED Board from the App.

# Video Demonstration
https://www.youtube.com/watch?v=pGBO1Hv5_1M
[![IMAGE ALT TEXT HERE](http://i3.ytimg.com/vi/pGBO1Hv5_1M/maxresdefault.jpg)](https://www.youtube.com/watch?v=pGBO1Hv5_1M)
