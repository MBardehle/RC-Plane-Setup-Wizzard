
#include "BluetoothSerial.h"
#include "EWD.h"
#include "CG.h"
#include "Arduino.h"
#include <inttypes.h>


EWD ewd;          //Sensor EWD einfügen
CG cg;            //Schwerpunktwaage einfügen
String btMessage;
String SerialMessage;
bool CG_initOK=false;
bool EWD_initOK=false;
bool RUD_initOK=false;
String function;
String sensor;
String instruction1;
String instruction2;
long timerSend=0.0;
int delaySend=200;

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("Air"); //Bluetooth device name
  Serial.println("The device started, now you can pair it with bluetooth!");
  
  ewd.init();
  cg.init();
}

void loop() {

//Serial.println(btMessage);
char outputText[100];
int i;
int startChar;
int endChar[3];
int endCharCounter=0;
int messageEnd;
long Startmillis;
 if (SerialBT.available()){      // Daten liegen an
   Startmillis=millis();
   
    btMessage = SerialBT.readString(); // Nachricht lesen
    Serial.println(btMessage);
    btMessage.toCharArray(outputText,btMessage.length()+1);     //Zum kopieren muss die Länge +1 genutzt werden, da ansonsten das letzte Zeichen abgeschnitten wird.
    endCharCounter=0;
    messageEnd=0;
    Serial.println(millis()-Startmillis);
    
    for(i=0;i<=btMessage.length();i++)                           //Nach Trennzeichen suchen und Position in endChar-Array speichern
    {
      if(outputText[i]=='#')
      {
        endChar[endCharCounter]=i;
        endCharCounter++;
      }
      if(outputText[i]==';')
      {
        messageEnd=i;
        i=btMessage.length();
      }
    }
    
//  Serial.print("EndChar: ");Serial.println(endCharCounter);  
  function = btMessage.substring(0, endChar[0]);              //Von 0 bis 1. Trennzeichen ist die Funktion
  sensor = btMessage.substring(endChar[0]+1, endChar[1]);     //Von 1.Trennzeichen +1 bis 2. Trennzeichen ist Sensor
  if(endCharCounter>2)                                        //Prüfen ob mehr als 2 Trennzeichen
  {
  instruction1 = btMessage.substring(endChar[1]+1, endChar[2]);         //3 Trennzeichen für Instruction 1 + 2
  instruction2 = btMessage.substring(endChar[2]+1, messageEnd);    
  }
  else{
  instruction1 = btMessage.substring(endChar[1]+1, messageEnd);  //Nur 2 Trennzeichen
  instruction2 = "";
  }
 Serial.print("Function: ");Serial.println(function); 
Serial.print("Sensor: ");Serial.println(sensor);
Serial.print("Instruction 1: ");Serial.println(instruction1);
Serial.print("Instruction 2: ");Serial.println(instruction2);
 endChar[0]=0;
 endChar[1]=0;
 endChar[2]=0;
 messageEnd=0;
 }

if (Serial.available()){      // Daten liegen an
    SerialMessage = Serial.readString(); // Nachricht lesen
    if(SerialMessage == "CG Set Default\n"){
      cg.set_Defaults();
      SerialMessage="";
      cg.init();
    }
    if(SerialMessage == "EWD Set Default\n"){
      ewd.set_Defaults();
      SerialMessage="";
      ewd.init();
    }
}


/***********STring splitten*****/
//  function = btMessage.substring(0, 2);
//  sensor = btMessage.substring(4, 5); 

  if(function=="CG"){
    //Serial.println("if CG Funktion");
    CG_Comm();
  }

  if(function=="EWD"){
    //Serial.println("if CG Funktion");
    EWD_Comm();
  }

  if(function=="RUD"){
    //Serial.println("if CG Funktion");
    RUD_Comm();
  }

}

void CG_Comm(){
  
  int i;
  char outputText[100];
  float calFactor;

  EWD_initOK=false;   //Init der anderen Funktionen zurücksetzen
  RUD_initOK=false;

/* Start Werte automatisch senden */
  if(sensor=="0"&& instruction1=="START"){
    if(CG_initOK==false){
    cg.init(); 
    CG_initOK=true;
    SerialBT.println("CG#0#Start#OK;");
    Serial.println("CG#0#Start#OK;");
    for(i=1;i<=3;i++){
      sprintf(outputText,"CG#%i#%i;", i,cg.get_ldcell_Maxweight(i));
      SerialBT.println(outputText);  
      }
    }
  cg.readSensor();
  sprintf(outputText,"CG#1#%.1f#2#%.1f#3#%.1f;", cg.get_weight(1),cg.get_weight(2),cg.get_weight(3));
  SerialBT.println(outputText);
  Serial.println(outputText);
  
  }

/**** Waage Max-Gewicht setzen ****/  
  if(instruction1=="SET"){          //CG#1#SET#1
    //Serial.print("Sensor: ");Serial.println(sensor.toInt());
    //Serial.print("Inst2: ");Serial.println(instruction2.toInt());
    cg.set_ldcell_Maxweight(sensor.toInt(),instruction2.toInt());
     cg.init(); 
    sprintf(outputText,"CG#%i#SET#%i#OK;", sensor.toInt(), instruction2.toInt());
      SerialBT.println(outputText);  
      Serial.println(outputText);
  function="";
  instruction1="";
  instruction2="";
  }

/*** Waagen nullen ***/
  if(instruction1=="ZERO"){          //CG#0#ZERO
 Serial.println(instruction1);
 
    if(sensor.toInt()==0){
    bool zero1=cg.setZero(1);
    bool zero2=cg.setZero(2);
    bool zero3=cg.setZero(3);
    Serial.print(zero1);Serial.print(zero2);Serial.println(zero3);
      if (zero1==true&&zero2==true&&zero3==true){
        sprintf(outputText,"CG#%i#ZERO#OK;", sensor.toInt());
        SerialBT.println(outputText);  
        Serial.println(outputText);
        }
        else{
        sprintf(outputText,"CG#%i#ZERO#ERROR;", sensor.toInt());
        SerialBT.println(outputText);  
        Serial.println(outputText);    
        }
    }
    else{
      bool zero=cg.setZero(sensor.toInt());
      if (zero==true){
        sprintf(outputText,"CG#%i#ZERO#OK;", sensor.toInt());
//        SerialBT.println(outputText);  
        Serial.println(outputText);
      }
      else{
        sprintf(outputText,"CG#%i#ZERO#ERROR;", sensor.toInt());
//        SerialBT.println(outputText);  
        Serial.println(outputText);    
        }
      
    }
    
  function="CG";
  sensor="0";
  instruction1="START";
  instruction2="";
  }

/*****Status Waage abfragen*/
if(instruction1=="STATUS"){          //CG#1#STATUS--> CG#1#Gewicht#Max-Gewicht#Kalibrierfaktor
  int maxWeight=cg.get_ldcell_Maxweight(sensor.toInt());
  calFactor=cg.getCalFactor(sensor.toInt(),maxWeight);    
  cg.readSensor();    
//  cg.get_weight(1)(sensor.toInt());
    sprintf(outputText,"CG#%i#%.1f#%i#%.2f;", sensor.toInt(),cg.get_weight(sensor.toInt()),maxWeight,calFactor);
      SerialBT.println(outputText);  
      Serial.println(outputText);
  function="";
  instruction1="";
  instruction2="";
  }

/****Kalibrierfaktor auslesen*/
if(instruction1=="GETFAKTOR"){          //CG#1#GETFAKTOR#1
    //Serial.print("Sensor: ");Serial.println(sensor.toInt());
    //Serial.print("Inst2: ");Serial.println(instruction2.toInt());
    calFactor=cg.getCalFactor(sensor.toInt(),instruction2.toInt()); 
    sprintf(outputText,"CG#%i#%i#%.2f;", sensor.toInt(),instruction2.toInt(),calFactor);
      SerialBT.println(outputText);  
      Serial.println(outputText);
  function="";
  instruction1="";
  instruction2="";
  }

/****Kalibrierung Waage frei setzen*/
if(instruction1=="READY"){          //CG#1#READY
cg.startCalibration(sensor.toInt());  
    sprintf(outputText,"CG#%i#READY#OK;", sensor.toInt());
      SerialBT.println(outputText);  
      Serial.println(outputText);
  function="";
  instruction1="";
  instruction2="";
  }


/****Kalibrierung mit Kalibriergewicht*/
if(instruction1=="CAL"){          //CG#1#CAL#Kalibriergewicht in Gramm
cg.calibrateWithKnownWheigt(sensor.toInt(),instruction2.toInt());  
    sprintf(outputText,"CG#%i#CAL#OK;", sensor.toInt());
      SerialBT.println(outputText);  
      Serial.println(outputText);
  function="";
  instruction1="";
  instruction2="";
  }

/****Kalibrierfaktor manuell Anpassen */  
if(instruction1=="ADDFAKTOR"){          //CG#1#ADDFAKTOR#Wert
cg.fineTuneCal(sensor.toInt(),instruction2.toInt());  
    sprintf(outputText,"CG#%i#ADDFAKTOR#OK;", sensor.toInt());
      SerialBT.println(outputText);  
      Serial.println(outputText);
  function="";
  instruction1="";
  instruction2="";
  }  
}


void EWD_Comm(){
  int i;
  char outputText[50];
  bool calOK=false;
  RUD_initOK=false;         //Init der anderen Funktionen zurücksetzen
  
  
/* Start Werte automatisch senden */
  if(sensor=="0"&& instruction1=="START"){
    if(EWD_initOK==false){
    EWD_initOK=ewd.init(); 
      if(EWD_initOK==true){
        SerialBT.println("EWD#0#START#OK;");
        Serial.println("EWD#0#START#OK;");    
        }
        else{
        SerialBT.println("EWD#0#START#ERROR;");
        Serial.println("EWD#0#START#ERROR;");
          function="";
          instruction1="";
          instruction2="";        
        }
    }
  if(EWD_initOK==true){
  ewd.readSensor();
  sprintf(outputText,"EWD#A#%.1f#B#%.1f#AB#%.1f;", ewd.get_SensorA(),ewd.get_SensorB(),ewd.get_SensorDiff());
  SerialBT.println(outputText);
  Serial.println(outputText);
    }
  }

/*** Neigunssensoren nullen ***/
  if(instruction1=="ZERO"){          //EWD#0#ZERO
    if(sensor=="0"){
    ewd.zeroingFunction('A');
    ewd.zeroingFunction('B');
    sprintf(outputText,"EWD#0#ZERO#OK;");
        SerialBT.println(outputText);  
        Serial.println(outputText);
    }   

    if(sensor=="A"){
    ewd.zeroingFunction('A');
    sprintf(outputText,"EWD#A#ZERO#OK;");
        SerialBT.println(outputText);  
        Serial.println(outputText);
    }
    
    if(sensor=="B"){
    ewd.zeroingFunction('B');
    sprintf(outputText,"EWD#B#ZERO#OK;");
        SerialBT.println(outputText);  
        Serial.println(outputText);
    }
  

  function="";
  sensor="";
  instruction1="";
  instruction2="";
  
  /*
  function="EWD";
  sensor="0";
  instruction1="START";
  instruction2="";
  */
 // EWD_initOK=false;
  }
  
/******Sensor A kalibrieren ***/
  if(sensor=="A"&&instruction1=="CAL"){
    calOK= ewd.calibrateSensor(1,SerialBT);
      if(calOK==true){
      SerialBT.println("EWD#A#CAL#OK;");
      Serial.println("EWD#A#CAL#OK;");
      }
      else{
      SerialBT.println("EWD#A#CAL#ERROR;");
      Serial.println("EWD#A#CAL#ERROR;");
      }
    function="";
    instruction1="";
    instruction2="";
  }

/******Sensor B kalibrieren ***/ 
  if(sensor=="B"&&instruction1=="CAL"){
    calOK= ewd.calibrateSensor(2,SerialBT);
      if(calOK==true){
      SerialBT.println("EWD#B#CAL#OK;");
      Serial.println("EWD#B#CAL#OK;");
      }
      else{
      SerialBT.println("EWD#B#CAL#ERROR;");
      Serial.println("EWD#B#CAL#ERROR;");
      }
    function="";
    instruction1="";
    instruction2="";
    }    
} /**Ende EWD-Comm



/***Ruder-Messung */
/* Die Ruder-Messung ist in die Klasse EWD eingebunden, da auf die gleichen Sensoren zugegriffen wird.
 *  Die Funktionen für die Rudermessung fangen mit dem Kürzel "RUD_" an.
 *  Die Initilisierung ist die gleiche wie bei der EWD-Messung
 */
void RUD_Comm(){
  int i;
  char outputText[50];

  EWD_initOK=false;         //Init der EWD-Funktion zurücksetzen
  
/* Start Werte automatisch senden */
  if(sensor=="0"&& instruction1=="START"){
    if(RUD_initOK==false){
    RUD_initOK=ewd.init(); 
    if(RUD_initOK==true){
      ewd.RUD_readSensor();
      //ewd.RUD_zeroingFunction('A');
      //ewd.RUD_zeroingFunction('B');
      SerialBT.println("RUD#0#START#OK;");
      Serial.println("RUD#0#START#OK;");    
      }
      else{
      SerialBT.println("RUD#0#START#ERROR;");
      Serial.println("RUD#0#START#ERROR;");
        function="";
        instruction1="";
        instruction2="";      
      }
    }
  if(RUD_initOK==true){
  ewd.RUD_readSensor();
  //Nur alle 50ms senden
  if(millis()>timerSend){
  sprintf(outputText,"RUD#A#%.1f#B#%.1f#AB#%.1f;", ewd.RUD_get_SensorA(),ewd.RUD_get_SensorB(),ewd.RUD_get_SensorDiff());
  SerialBT.println(outputText);
  Serial.println(outputText);  
  timerSend = millis()+delaySend;  
  }
  //delay(100);
    }
  }

/*** Neigunssensoren nullen ***/
  if(instruction1=="ZERO"){          //EWD#0#ZERO
    if(sensor=="0"){
    ewd.RUD_zeroingFunction('A');
    ewd.RUD_zeroingFunction('B');
    sprintf(outputText,"RUD#0#ZERO#OK;");
        SerialBT.println(outputText);  
        Serial.println(outputText);
    }   

    if(sensor=="A"){
    ewd.RUD_zeroingFunction('A');
    sprintf(outputText,"RUD#A#ZERO#OK;");
        SerialBT.println(outputText);  
        Serial.println(outputText);
        }
    
    if(sensor=="B"){
    ewd.RUD_zeroingFunction('B');
    sprintf(outputText,"RUD#B#ZERO#OK;");
        SerialBT.println(outputText);  
        Serial.println(outputText);
    }
  function="RUD";                     //Automatischer Start nach Tara
  sensor="0";
  instruction1="START";
  instruction2="";
  }

   if(instruction1=="REVERSE"){          //RUD#B#REVERSE;
    if(instruction2=="1"){
    ewd.RUD_set_DiffReverse(true);
    sprintf(outputText,"RUD#B#REVERSE#1#OK;");
    Serial.println("Sensor B Reverse = True");
    SerialBT.println(outputText);  

    }
    if(instruction2=="0"){
    ewd.RUD_set_DiffReverse(false);
    sprintf(outputText,"RUD#B#REVERSE#0#OK;");
    Serial.println("Sensor B Reverse = False");
    SerialBT.println(outputText);  
    }   
  function="RUD";
  sensor="0";
  instruction1="START";
  instruction2="";
  }
  
}
