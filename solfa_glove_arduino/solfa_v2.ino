/*
 * Arduino Solfa Glove Sketch, using BNO055 9 DOF Sensor
 * By Mark O'Donnell - 2016 shotmodonnell@gmail.com
 * Some setup code taken from the Adafruit bunny sketch, that demoes the BNO055
 * 
 * Note that this code is designed to be used with the ATMEL XPLAINED PRO BNO055, not the Adafruit
 * one, some details of data are different, pitch and roll are swapped for example
 * 
 * This code is designed to go on a glove, with the BNO055 placed on the back of the hand and the
 * magnetic switch's contact at the base of the index finger. A small magnet should also be placed
 * on the thumb, to close the switch when the hand is in a 'karate chop' shape.
 * 
 * This code sends the name of the solfa sign that is made with the hand over Serial. These can be interpreted
 * by the program of your choice, originally included with this was a Processing sketch that reads the input to
 * display a nice text display of the note, as well as play a corresponding note in midi.
 * 
 * v2 of the code adds:
 *    TO DO: a handshake between the processing and arduino code to increase robustness
 *    TO DO: ranges of values for the solfa signs to fall within
 */
 
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BNO055.h>
#include <utility/imumaths.h>

/* Set the delay between fresh samples */
#define BNO055_SAMPLERATE_DELAY_MS (100)

Adafruit_BNO055 bno = Adafruit_BNO055(55);

void displaySensorDetails(void)
{
  sensor_t sensor;
  bno.getSensor(&sensor);
  Serial.println("------------------------------------");
  Serial.print  ("Sensor:       "); Serial.println(sensor.name);
  Serial.print  ("Driver Ver:   "); Serial.println(sensor.version);
  Serial.print  ("Unique ID:    "); Serial.println(sensor.sensor_id);
  Serial.print  ("Max Value:    "); Serial.print(sensor.max_value); Serial.println(" xxx");
  Serial.print  ("Min Value:    "); Serial.print(sensor.min_value); Serial.println(" xxx");
  Serial.print  ("Resolution:   "); Serial.print(sensor.resolution); Serial.println(" xxx");
  Serial.println("------------------------------------");
  Serial.println("");
  Serial.println("Setup Complete");
  delay(500);
}

//end of Adafruit code

int const switchPin = 8; // a digital pin connected to a magnetic switch, determining which solfa group

boolean whichGroup = 0; //group 0 or group 1 of the decision tree, to determine what group of signs is possible
String currPitch = "do"; //pitch of the note we are currntly on
String nextPitch = "do"; //pitch of the next note (used so each note is only played once)
float pitch = 0;
float roll = 0;

void determineNote()
{
  if(whichGroup)
  {
    if(roll < -35)
      //return so
      nextPitch = "so";
    else if(pitch < -30)
        // return re
        nextPitch = "re";
        //return mi
        else nextPitch = "mi";
  }
  else if(roll > 50)
    // return fa
    nextPitch = "fa";
    else if(pitch > 30)
      //return la
      nextPitch = "la";
      else if(pitch < -30)
        //return ti
        nextPitch = "ti";
        else nextPitch = "do";
}

void playNote()
{
  Serial.println(nextPitch);
  delay(1000); //this delay was added to prevent extra "do"s from playing when transitioning between signs
}


void setup() {
  Serial.begin(115200);

  Serial.print("H"); //this is the handshake to send to Processing

  if(!bno.begin())
  {
    //if things are not working...
    Serial.print("Sensor doesn't seem to be connected, check yr stuff");
    while(1);
  }

  delay(1000);
  displaySensorDetails(); //basic startup
}

void loop() {
  Serial.print("H");
  sensors_event_t event;
  bno.getEvent(&event);

  roll = event.orientation.y; //get the value for roll from the BNO055
  pitch = event.orientation.z; //get the value for pitch from the BNO055
  whichGroup = digitalRead(switchPin); //check the magnetic switch
  
  determineNote();

  if(nextPitch != currPitch)
    playNote();

  currPitch = nextPitch;

  delay(BNO055_SAMPLERATE_DELAY_MS);

}


