import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import java.awt.datatransfer.*; 
import java.awt.Toolkit; 
import themidibus.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class solfa_glove_v2 extends PApplet {

/*
 * Processing Solfa Glove Sketch, using Arduino for input
 * By Mark O'Donnell - 2016 shotmodonnell@gmail.com
 * Some setup code for this is taken from the Adafruit bunny sketch, that demoes the BNO055
 * 
 * This code is designed to complement the solfa glove Arduino sketch I wrote, it interprets solfa names,
 * received over a Serial bus, and displays them in a draw window, as well as sending them over a midi bus
 *
 * In order to get the midi working an internal midi bus will need to be set up on your system. On OSX the
 * pre-installed IAC bus works very well, this is what this is configured to use. The sent midi can then
 * be interpreted by any DAW or similar program to play the notes
 *
 * v2 of the code adds:
 *    a handshake between the arduino and processing to increase robustness
 *    TO DO: more verbose messaging
 */






Serial       port;
final String serialConfigFile = "serialconfig.txt"; //config file where the port of the arduino will be read from, this may need to be filled in by you!
boolean connected = false; //boolean to maintain whether or not the connection between Arduino and Processing exists
boolean setupComplete = false;
long timeElapsed = 0; //time elapsed since last handshake was made, used to check if connection persists
boolean      printSerial = false; //debugging help
PFont f;
String currNote = "no input";
String nextNote = "no input";
int rootNote = 60; //the root of the key we are in, set to C here
MidiBus myBus;

public void setup()
{
  size(1280, 960, P3D);
  f = createFont("Easy 3D",128,true); //font setup for text display
  
  MidiBus.list(); //readout to make sure IAC bus is running (if you are using a different midi bus you can check its name in the debug)
  myBus = new MidiBus(this, -1, "Bus 1"); // Create a new MidiBus with no input device and IAC bus 1 as its output

  connectPort();
}

public void draw()
{
  //need a timer here to check connection is maintained if(number - millis() > 15000) etc
  if(millis() - timeElapsed > 5000){
    println("Device disconnected");
    connected = false;
    currNote = "No Input";
    connectPort();
  }
  
  background(0xff2CFFB3); //great colour!

  textFont(f,128);
  fill(0xffFF95E8); //pink and green!
  translate(width*.55f, height*.5f, 0); //place it in the centre
  
  if(nextNote != currNote){ //only play the note when a new note is received
    currNote = nextNote; 
    thread("sendMidi"); //this is the only part that requires P3, you should be able to change this to run as a function
  }                     //without too much trouble as the Arduino sketch contains a longer delay at this point
  
  writeText(currNote);
}

public void serialEvent(Serial p) 
{
  String incoming = p.readString();

  if (printSerial) {
    println("Printing incoming data: \n" + incoming); //debugging
  }
  
  if(incoming.equals("H")){ //handshake to maintain connection
    connected = true;
    timeElapsed = millis();
  } else if(!setupComplete){ //is the initial setup over?
      println(incoming);
      if(splitTokens(incoming)[splitTokens(incoming).length - 1].equals("Complete") || splitTokens(incoming)[splitTokens(incoming).length - 2].equals("Complete")){
        println("Processing Arduino Setup Complete");
        setupComplete = true;
      }
    } else nextNote = splitTokens(incoming)[0]; //take the sent note
}

public void connectPort(){

  // Serial port setup.
  // Grab list of serial ports and check which the Arduino is on
  setupComplete = false; //initial setup is not complete
  int selectedPort = 0;
  String[] availablePorts = Serial.list();
  printArray(availablePorts);

  if (availablePorts == null) {
    println("ERROR: No serial ports available!");
    //display this on the draw screen as well please
    exit();
  }
  
  String[] serialConfig = loadStrings(serialConfigFile);
  if (serialConfig != null && serialConfig.length > 0) {
    String savedPort = serialConfig[0];
  }
  
  int lastPort = availablePorts.length - 1;
  
  while (!connected)
  {
    String portName = availablePorts[lastPort];
    println("Connecting to -> " + portName);
    delay(200);
 
    try {
      port = new Serial(this, portName, 115200);
      port.clear();
      port.bufferUntil('H');
      port.write('H');   // Send  Hello
 
      int l = 5;
      while (!connected && l >0)
      {
        delay(200);
        println("Waiting for response from device on " + portName);
        l--;
      }
 
      if (!connected)
      {
        println("No response from device on " + portName);
        writeText(("No response from device on " + portName));
        port.clear();
        port.stop();
        delay(200);
      }
 
    }
    catch (Exception e) {
      println("Exception connecting to " + portName);
      println(e);
    }
 
    lastPort--;
    if (lastPort <0){
      availablePorts = Serial.list();
      lastPort = Serial.list().length -1;
    }
  }

  println("Connected to device, leaving connectPort function");
}

//send the midi over midibus
public void sendMidi()
{
  int note = 0;
  //conversion from names that arduino sends to  midi note values
  if(currNote.equals("do"))
      note = rootNote;
  else if(currNote.equals("re"))
      note = rootNote + 2;
  else if(currNote.equals("mi"))
      note = rootNote + 4;
  else if(currNote.equals("fa"))
      note = rootNote + 5;
  else if(currNote.equals("so"))
      note = rootNote + 7;
  else if(currNote.equals("la"))
      note = rootNote + 9;
  else if(currNote.equals("ti"))
      note = rootNote + 11;

  myBus.sendNoteOn(0, note, 127); //play a note
  delay(200);
  myBus.sendNoteOff(0, note, 127); //silence it
}

public void writeText(String input)
{
  text(input, -60, -20, 0);
  fill(0xffFF46E1);
  text(input, -60, 40, 120); //double text to make it look nice
  textAlign(CENTER);
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "solfa_glove_v2" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
