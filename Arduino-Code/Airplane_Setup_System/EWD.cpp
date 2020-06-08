/*SetupBox
	Marcel Bardehle
EWD-Waage mit MMA8451

V0.1

Sensor A = Adresse 0x1D
Sensor B = Adresse 0x1C

/************************************ Includes************************************/                                            
#include "EWD.h"
#include <Arduino.h>
#include "Wire.h"
#include <Adafruit_MMA8451.h>       //https://github.com/adafruit/Adafruit_MMA8451_Library
#include <Adafruit_Sensor.h>        //https://github.com/adafruit/Adafruit_Sensor
#include <Filters.h>                //https://github.com/JonHub/Filters
#include "BluetoothSerial.h"
#include <Preferences.h>


/************************************ DEFINED VARIABLES ************************************/

bool debugging = true;     //Set to true for debugging via serial monitor

float raw_AX, raw_AY, raw_AZ = 0.0; //raw data from sensor A
float raw_BX, raw_BY, raw_BZ = 0.0; //raw data from sensor B

float g_value_AX, g_value_AY, g_value_AZ = 0.0; //g-values from angle sensor A
float g_value_BX, g_value_BY, g_value_BZ = 0.0; //g-values from angle sensor B

float deg_SensorA = 0.0; //Holds the sensor value in degrees
float deg_SensorB = 0.0; //Holds the sensor value in degrees

float deg_RelativeA = 0.0; //FOR ZEROING
float deg_RelativeB = 0.0; //FOR ZEROING

float RUD_deg_SensorA = 0.0; //sensor value for Rudder in degrees
float RUD_deg_SensorB = 0.0; //sensor value for Rudder in degrees

float RUD_deg_RelativeA = 0.0; //FOR ZEROING Rudder
float RUD_deg_RelativeB = 0.0; //FOR ZEROING Rudder

float RUD_deg_SensorA_Round = 0.0; //sensor value for Rudder in degrees
float RUD_deg_SensorB_Round = 0.0; //sensor value for Rudder in degrees

float RUD_smooth_deg_SensorA;
float RUD_smooth_deg_SensorB;

bool RUD_reverse=false;

float filterFrequency = 0.4;    //filters out changes faster than 0.4 Hz
//int average;    //counter for averaging


int offset_AX=0;       // OFFSET values 
int offset_AY=0;   
int offset_AZ=0;

int offset_BX=0;       // OFFSET values
int offset_BY=0;
int offset_BZ=0;

double gain_AX = 0.0;     // GAIN factors
double gain_AY = 0.0;
double gain_AZ = 0.0;  

double  gain_BX= 0.0;     // GAIN factors
double  gain_BY= 0.0;
double  gain_BZ= 0.0;


/*********************************************************************************************/
Preferences preferences;


Adafruit_MMA8451 sensor_A = Adafruit_MMA8451(); //create a sensor object
Adafruit_MMA8451 sensor_B = Adafruit_MMA8451(); //create a sensor object

FilterOnePole lowpassFilter_AX( LOWPASS, filterFrequency );    // create a one pole (RC) lowpass filter 
FilterOnePole lowpassFilter_AY( LOWPASS, filterFrequency );    // create a one pole (RC) lowpass filter
FilterOnePole lowpassFilter_AZ( LOWPASS, filterFrequency );    // create a one pole (RC) lowpass filter

FilterOnePole lowpassFilter_BX( LOWPASS, filterFrequency );    // create a one pole (RC) lowpass filter 
FilterOnePole lowpassFilter_BY( LOWPASS, filterFrequency );    // create a one pole (RC) lowpass filter
FilterOnePole lowpassFilter_BZ( LOWPASS, filterFrequency );    // create a one pole (RC) lowpass filter


//BluetoothSerial SerialBT1;


/************************************ DEFINED FUNCTIONS ************************************/

//This function will run once at startup
bool EWD::init() {
    bool initOK=true;

  Serial.println("EWD Init Start");
  preferences.begin("EWD", false);  //false for read/write
  unsigned int check=preferences.getUInt("check",0);  
  preferences.end();

  //Standard-Parameter schreiben
  if(check!=105){
    
    Serial.println("Init Parameter EWD schreiben");
    preferences.begin("EWD", false);
    preferences.putInt("offset_AX",-36);
    preferences.putInt("offset_AY",48);
    preferences.putInt("offset_AZ",19);

    preferences.putDouble("gain_AX",0.9870573871);
    preferences.putDouble("gain_AY",1.0070818071);
    preferences.putDouble("gain_AZ",0.9926739927);
    
    preferences.putInt("offset_BX",-54);
    preferences.putInt("offset_BY",48);
    preferences.putInt("offset_BZ",-18);

    preferences.putDouble("gain_BX",0.9997557998);
    preferences.putDouble("gain_BY",0.9987789988);
    preferences.putDouble("gain_BZ",0.9919413919);
    preferences.putUInt("check",105);
    //Serial.println(prefs.getUInt("check",0));
    preferences.end();
  }
   
//    if(debugging==true) Serial.begin(9600);

    //check an initialize sensors
    if (sensor_A.begin(0x1D))
    {
      sensor_A.setRange(MMA8451_RANGE_2_G);
      preferences.begin("EWD", true); 
      offset_AX=preferences.getInt("offset_AX",0);
      offset_AY=preferences.getInt("offset_AY",0);
      offset_AZ=preferences.getInt("offset_AZ",0);
      gain_AX=preferences.getDouble("gain_AX",0);     // OFFSET values 
      gain_AY=preferences.getDouble("gain_AY",0);
      gain_AZ=preferences.getDouble("gain_AZ",0);  
      preferences.end(); 


      Serial.println("Sensor A:");
      Serial.print("Offset X: ");Serial.print(offset_AX);Serial.print("  Y: ");Serial.print(offset_AY);Serial.print("  Z: ");Serial.println(offset_AZ);
      Serial.print("Gain X: ");Serial.print(gain_AX,10);Serial.print("  Y: ");Serial.print(gain_AY,10);Serial.print("  Z: ");Serial.println(gain_AZ,10);
      
      if (debugging==true){
        Serial.println("Sensor_A OK!");
        Serial.print("Range A = ");
        Serial.print(2 << sensor_A.getRange());
        Serial.println("G");
        }//end if  
         
    }//end if
     else{
      Serial.println("Sensor A nicht gefunden");
      initOK=false;
      }

    if (sensor_B.begin(0x1C))
    {
      sensor_B.setRange(MMA8451_RANGE_2_G);
      preferences.begin("EWD", true); 
      offset_BX=preferences.getInt("offset_BX",0);
      offset_BY=preferences.getInt("offset_BY",0);
      offset_BZ=preferences.getInt("offset_BZ",0);
      gain_BX=preferences.getDouble("gain_BX",0);     // OFFSET values 
      gain_BY=preferences.getDouble("gain_BY",0);
      gain_BZ=preferences.getDouble("gain_BZ",0); 
      preferences.end();  
      Serial.println("Sensor B:");
      Serial.print("Offset X: ");Serial.print(offset_BX);Serial.print("  Y: ");Serial.print(offset_BY);Serial.print("  Z: ");Serial.println(offset_BZ);
      Serial.print("Gain X: ");Serial.print(gain_BX,10);Serial.print("  Y: ");Serial.print(gain_BY,10);Serial.print("  Z: ");Serial.println(gain_BZ,10);
      
      if(debugging==true){
        Serial.println("Sensor_B OK!");
        Serial.print("Range B = ");
        Serial.print(2 << sensor_B.getRange());
        Serial.println("G");   
        }//end if  
        
    }//end if
     else{
      Serial.println("Sensor B nicht gefunden");
     initOK=false;
     }
    delay(100);
    preferences.end();
    return initOK;
}//end setup

//this function runs repeatedly
void EWD::readSensor(){
    int average = 0;
    int i;
    
    for(i=0;i<500;i++){

    sensor_A.read();
    raw_AX = ((float) sensor_A.x - offset_AX) / gain_AX;    //compute offset from calibration
    raw_AY = ((float) sensor_A.y - offset_AY) / gain_AY;    //compute offset from calibration
    raw_AZ = ((float) sensor_A.z - offset_AZ) / gain_AZ;    //compute offset from calibration
    
    lowpassFilter_AX.input(raw_AX);    //filter sensor noise
    lowpassFilter_AY.input(raw_AY);    //filter sensor noise
    lowpassFilter_AZ.input(raw_AZ);    //filter sensor noise

    g_value_AX += raw_AX;       //add read value to variable 
    g_value_AY += raw_AY;       //add read value to variable
    g_value_AZ += raw_AZ;       //add read value to variable
        
    sensor_B.read();
    raw_BX = ((float) sensor_B.x - offset_BX) / gain_BX;    //compute offset from calibration
    raw_BY = ((float) sensor_B.y - offset_BY) / gain_BY;    //compute offset from calibration
    raw_BZ = ((float) sensor_B.z - offset_BZ) / gain_BZ;    //compute offset from calibration
    
    lowpassFilter_BX.input(raw_BX);    //filter sensor noise
    lowpassFilter_BY.input(raw_BY);    //filter sensor noise
    lowpassFilter_BZ.input(raw_BZ);    //filter sensor noise

    g_value_BX += raw_BX;       //add read value to variable 
    g_value_BY += raw_BY;       //add read value to variable
    g_value_BZ += raw_BZ;       //add read value to variable
    
    average++;      //add 1 to counter
    }
    //Winkel berechnen
        g_value_AX = (g_value_AX / average) * (1.00 / 4095.00);     //build average and convert to G ( 1G = gravity)
        g_value_AY = (g_value_AY / average) * (1.00 / 4095.00);
        g_value_AZ = (g_value_AZ / average) * (1.00 / 4095.00);

        g_value_BX = (g_value_BX / average) * (1.00 / 4095.00);     //build average and convert to G ( 1G = gravity)
        g_value_BY = (g_value_BY / average) * (1.00 / 4095.00);
        g_value_BZ = (g_value_BZ / average) * (1.00 / 4095.00); 
        

        deg_SensorA = (atan2(g_value_AY, (sqrt(g_value_AX * g_value_AX + g_value_AZ * g_value_AZ))) * 180.0) / PI;      //compute angle out of the 3 Axis und convert to degrees
        deg_SensorB = (atan2(g_value_BY, (sqrt(g_value_BX * g_value_BX + g_value_BZ * g_value_BZ))) * 180.0) / PI;      //compute angle out of the 3 Axis und convert to degrees
        
        deg_SensorA -= deg_RelativeA; //zeroing
        deg_SensorB -= deg_RelativeB; //zeroing

        Serial.print("Sensor A: ");Serial.println(deg_SensorA);
        Serial.print("Sensor B: ");Serial.println(deg_SensorB);
        
}//end loop

// This function will be called once, when the button is pressed for a long time. It will set the actual value
//of the sensors to a variable used for zeroing
void EWD::zeroingFunction(char sensor) {
     if(sensor=='A'){
     deg_RelativeA = (atan2(g_value_AY, (sqrt(g_value_AX * g_value_AX + g_value_AZ * g_value_AZ))) * 180.0) / PI;      //compute angle out of the 3 Axis and convert to degrees, set angle offset 
 //    Serial.println("Sensor A Null");
     }
     if(sensor=='B'){
     deg_RelativeB = (atan2(g_value_BY, (sqrt(g_value_BX * g_value_BX + g_value_BZ * g_value_BZ))) * 180.0) / PI;      //compute angle out of the 3 Axis and convert to degrees, set angle offset
     Serial.println("Sensor B Null");
     }
//      Serial.println("Reset OK!");
     delay(100);
} //end zeroingFunction



//Funktion zum Kalibrieren eines Sensors
bool EWD::calibrateSensor(int sensor, BluetoothSerial &btserial){
int16_t AccelMinX = 0;
int16_t AccelMaxX = 0;
int16_t AccelMinY = 0;
int16_t AccelMaxY = 0;
int16_t AccelMinZ = 0;
int16_t AccelMaxZ = 0;
uint8_t calSteps=0;
int16_t offset=0;
double gain=0.0;
int CalTime=120000;                    //10Sekunden Kalibrierzeit
long timeOutMillis = millis()+CalTime;
bool timeOut=false;
bool sensorA_OK=false;
bool sensorB_OK=false;
String btMessage;
char outputText[100];

  if(sensor==1&&sensor_A.begin(0x1D)){      //Sensor 1= Sensor A
    sensor_A.setRange(MMA8451_RANGE_2_G);
    Serial.println("Kalibrierung Sensor A gestartet");
    btserial.println("EWD#A#CAL#READY;");
    sensorA_OK=true;
      do{
     if (btserial.available()){           // Daten liegen an
     btMessage = btserial.readString();   // Nachricht lesen
     Serial.print(btMessage);
          if(btMessage=="EWD#A#CAL#STEP;"){        //Wenn EWD#A#CAL#STEP; gelesen wird, akutelle Sensor-Werte lesen
             sensor_A.read();
        //  Serial.println(sensor_A.x);
          if (sensor_A.x < AccelMinX) AccelMinX = sensor_A.x;
          if (sensor_A.x > AccelMaxX) AccelMaxX = sensor_A.x;
          
          if (sensor_A.y < AccelMinY) AccelMinY = sensor_A.y;
          if (sensor_A.y > AccelMaxY) AccelMaxY = sensor_A.y;
        
          if (sensor_A.z < AccelMinZ) AccelMinZ = sensor_A.z;
          if (sensor_A.z > AccelMaxZ) AccelMaxZ = sensor_A.z;
    
        Serial.print("Minima Sensor A: "); Serial.print(AccelMinX); Serial.print("  ");Serial.print(AccelMinY); Serial.print("  "); Serial.print(AccelMinZ); Serial.println();
        Serial.print("Maxima Sensor A: "); Serial.print(AccelMaxX); Serial.print("  ");Serial.print(AccelMaxY); Serial.print("  "); Serial.print(AccelMaxZ); Serial.println();
      //  Serial.print("Offset Sensor A: "); calc_Off (AccelMaxX, AccelMinX); calc_Off (AccelMaxY, AccelMinY); calc_Off (AccelMaxZ, AccelMinZ); Serial.println();
      //  Serial.print("Gain Sensor A:   "); calc_Gain (AccelMaxX, AccelMinX); calc_Gain (AccelMaxY, AccelMinY); calc_Gain (AccelMaxZ, AccelMinZ); Serial.println();
      //  Serial.print("Sensor A Range:  "); Serial.print(sensor_A.getRange()); Serial.println(); Serial.println();
          calSteps++;
          Serial.print("Step: ");Serial.println(calSteps);
          sprintf(outputText,"EWD#A#CAL#STEP#%i;", calSteps);
          btserial.println(outputText);
          Serial.println(outputText);  
          btMessage="";
          }
          if(millis()>timeOutMillis){
            btserial.println("EWD#A#CAL#TIMEOUT;");
            Serial.println("EWD#A#CAL#TIMEOUT;");
            timeOut=true;   
            sensorA_OK=false;
            }  
        } 
      }while (calSteps<=6&&timeOut==false); 
    }

  
  if(sensor==2&&sensor_B.begin(0x1C)){      //Sensor 2= Sensor B
    sensor_B.setRange(MMA8451_RANGE_2_G);
    Serial.println("Kalibrierung Sensor B gestartet");
    btserial.println("EWD#B#CAL#READY;");
    sensorB_OK=true;
      do{
        if (btserial.available()){
        // Daten liegen an
         btMessage = btserial.readString();   // Nachricht lesen
         Serial.print(btMessage);
             if(btMessage=="EWD#B#CAL#STEP;"){        //Wenn EWD#B#CAL#STEP; gelesen wird, akutelle Sensor-Werte lesen
              sensor_B.read();
             // Serial.println(sensor_B.x);
              if (sensor_B.x < AccelMinX) AccelMinX = sensor_B.x;
              if (sensor_B.x > AccelMaxX) AccelMaxX = sensor_B.x;
              
              if (sensor_B.y < AccelMinY) AccelMinY = sensor_B.y;
              if (sensor_B.y > AccelMaxY) AccelMaxY = sensor_B.y;
            
              if (sensor_B.z < AccelMinZ) AccelMinZ = sensor_B.z;
              if (sensor_B.z > AccelMaxZ) AccelMaxZ = sensor_B.z;

              Serial.print("Minima Sensor B: "); Serial.print(AccelMinX); Serial.print("  ");Serial.print(AccelMinY); Serial.print("  "); Serial.print(AccelMinZ); Serial.println();
              Serial.print("Maxima Sensor B: "); Serial.print(AccelMaxX); Serial.print("  ");Serial.print(AccelMaxY); Serial.print("  "); Serial.print(AccelMaxZ); Serial.println();
              
              calSteps++;
              Serial.print("Step: ");Serial.println(calSteps);
              sprintf(outputText,"EWD#B#CAL#STEP#%i;", calSteps);
              btserial.println(outputText);
              Serial.println(outputText);  
              btMessage="";
              }
          }
       if(millis()>timeOutMillis){
              btserial.println("EWD#B#CAL#TIMEOUT;");
              Serial.println("EWD#B#CAL#TIMEOUT;");
              timeOut=true;   
              sensorB_OK=false;
       }
      }while(calSteps<=6&&timeOut==false);  
  }

//    calc_Off (AccelMaxX, AccelMinX); calc_Off (AccelMaxY_A, AccelMinY_A); calc_Off (AccelMaxZ_A, AccelMinZ_A);
  if(sensorA_OK&&calSteps==7&&timeOut==false){
    //Offset-Werte Sensor A
    preferences.begin("EWD", false);
    offset = 0.5*(AccelMaxX+AccelMinX);
    Serial.print("Offset X: ");Serial.println(offset);
    preferences.putInt("offset_AX",offset);
     offset = 0.5*(AccelMaxY+AccelMinY);
    Serial.print("Offset Y: ");Serial.println(offset);
    preferences.putInt("offset_AY",offset);
    offset = 0.5*(AccelMaxZ+AccelMinZ);
    Serial.print("Offset Z: ");Serial.println(offset);
    preferences.putInt("offset_AZ",offset);

    gain = 0.5*((float(AccelMaxX-AccelMinX))/4095.0);
    Serial.print("Gain X :");Serial.println(gain,10);
    preferences.putDouble("gain_AX",gain);
    gain = 0.5*((float(AccelMaxY-AccelMinY))/4095.0);
    Serial.print("Gain Y: ");Serial.println(gain,10);
    preferences.putDouble("gain_AY",gain);
    gain = 0.5*((float(AccelMaxZ-AccelMinZ))/4095.0);
    Serial.print("Gain Z: ");Serial.println(gain,10);
    preferences.putDouble("gain_AZ",gain);
    preferences.end();
  }
  
  if(sensorB_OK&&calSteps==7&&timeOut==false){
    //Offset-Werte Sensor B
    preferences.begin("EWD", false);
    offset = 0.5*(AccelMaxX+AccelMinX);
    Serial.print("Offset X: ");Serial.println(offset);
    preferences.putInt("offset_BX",offset);
     offset = 0.5*(AccelMaxY+AccelMinY);
    Serial.print("Offset Y: ");Serial.println(offset);
    preferences.putInt("offset_BY",offset);
    offset = 0.5*(AccelMaxZ+AccelMinZ);
    Serial.print("Offset Z: ");Serial.println(offset);
    preferences.putInt("offset_BZ",offset);

    gain = 0.5*((float(AccelMaxX-AccelMinX))/4095.0);
    Serial.print("Gain X :");Serial.println(gain,10);
    preferences.putDouble("gain_BX",gain);
    gain = 0.5*((float(AccelMaxY-AccelMinY))/4095.0);
    Serial.print("Gain Y: ");Serial.println(gain,10);
    preferences.putDouble("gain_BY",gain);
    gain = 0.5*((float(AccelMaxZ-AccelMinZ))/4095.0);
    Serial.print("Gain Z: ");Serial.println(gain,10);
    preferences.putDouble("gain_BZ",gain);
    preferences.end();
  }
  if((sensor==1&&sensorA_OK)||(sensor==2&&sensorB_OK)){return true;}
  else{return false;}
}


float EWD::get_SensorA(){return deg_SensorA;}
float EWD::get_SensorB(){return deg_SensorB;}
float EWD::get_SensorDiff(){return deg_SensorA-deg_SensorB;}


void EWD::set_Defaults(){
    preferences.begin("EWD", false);
    preferences.putUInt("check",0);
    preferences.end();
}


/****** Ab hier Funktionen für Ruder-Sensoren****/

void EWD::RUD_readSensor(){
    int average = 0;
    int i;
    const static float smooth = 0.98;
    float RUD_actual_deg_SensorA=0.0;
    float RUD_actual_deg_SensorB=0.0;
    
    for(i=0;i<2;i++){
    sensor_A.read();
    raw_AY = ((float) sensor_A.y - offset_AY) / gain_AY;    //compute offset from calibration
    raw_AZ = ((float) sensor_A.z - offset_AZ) / gain_AZ;    //compute offset from calibration
    RUD_actual_deg_SensorA += atan2(raw_AY , raw_AZ) * 57.296;;       //add read value to variable and convert to Angle
        
    sensor_B.read();
    raw_BY = ((float) sensor_B.y - offset_BY) / gain_BY;    //compute offset from calibration
    raw_BZ = ((float) sensor_B.z - offset_BZ) / gain_BZ;    //compute offset from calibration
    RUD_actual_deg_SensorB += atan2(raw_BY , raw_BZ) * 57.296;        //add read value to variable and convert to Angle
    
    average++;      //add 1 to counter
    }
    //Winkel berechnen
        RUD_actual_deg_SensorA = (RUD_actual_deg_SensorA / average);     //build average and convert to G ( 1G = gravity)
        RUD_actual_deg_SensorA=RUD_actual_deg_SensorA*(-1);             //Wirkrichtung ändern

        RUD_actual_deg_SensorB = (RUD_actual_deg_SensorB / average);     //build average and convert to G ( 1G = gravity)
        RUD_actual_deg_SensorB=RUD_actual_deg_SensorB*(-1);

        RUD_smooth_deg_SensorA = RUD_irr_low_pass_filter(RUD_smooth_deg_SensorA, RUD_actual_deg_SensorA,smooth) ; 
        RUD_smooth_deg_SensorB = RUD_irr_low_pass_filter(RUD_smooth_deg_SensorB, RUD_actual_deg_SensorB,smooth) ;         
        
        RUD_deg_SensorA = RUD_smooth_deg_SensorA-RUD_deg_RelativeA; //zeroing
        RUD_deg_SensorB = RUD_smooth_deg_SensorB-RUD_deg_RelativeB; //zeroing

        RUD_deg_SensorA_Round = round(RUD_deg_SensorA ,1); //sensor value for Rudder in degrees
        RUD_deg_SensorB_Round = round(RUD_deg_SensorB ,1); //sensor value for Rudder in degrees
        
//        Serial.print(RUD_deg_SensorA);Serial.print("\t");Serial.print(RUD_deg_SensorB);Serial.print("\t");Serial.println(RUD_get_SensorDiff());
}//end loop

void EWD::RUD_zeroingFunction(char sensor) {
     if(sensor=='A'){
     RUD_deg_RelativeA = RUD_smooth_deg_SensorA;      //compute angle out of the 3 Axis and convert to degrees, set angle offset 
     Serial.println("Sensor RUD A Null");
     }
     if(sensor=='B'){
     RUD_deg_RelativeB = RUD_smooth_deg_SensorB;      //compute angle out of the 3 Axis and convert to degrees, set angle offset
     Serial.println("Sensor RUD B Null");
     }
      Serial.println("Reset OK!");
//     delay(20);
} //end zeroingFunction


float EWD::RUD_get_SensorA(){return RUD_deg_SensorA_Round;}
float EWD::RUD_get_SensorB(){return RUD_deg_SensorB_Round;}
float EWD::RUD_get_SensorDiff(){
  float RUD_Diff;
  if(RUD_reverse==true){
    RUD_Diff=(RUD_deg_SensorA_Round+RUD_deg_SensorB_Round);
  }
  else{
    RUD_Diff=(RUD_deg_SensorA_Round-RUD_deg_SensorB_Round);
  }
  return RUD_Diff;
  }

void EWD::RUD_set_DiffReverse(bool value){
  RUD_reverse=value;
}


float EWD::round(float Value, int signs){

  int faktor=1;
  float additionValue=0.0;
  int i;
  for(i=0;i<signs;i++){
    faktor = faktor *10;
  }
  additionValue=1.0/(2.0*(float)faktor);
  Value = Value + additionValue;
  Value =(int) (Value*faktor);
  Value = Value / faktor;
  return Value;
}

float EWD::RUD_irr_low_pass_filter(float aSmoothedValue, float aCurrentValue, float aSmoothingFactor) {
  //Übernommen von https://github.com/Pulsar07/RuderwegMessSensor
  // see: https://en.wikipedia.org/wiki/Low-pass_filter#Simple_infinite_impulse_response_filter
  return aCurrentValue + aSmoothingFactor * (aSmoothedValue - aCurrentValue);
}
