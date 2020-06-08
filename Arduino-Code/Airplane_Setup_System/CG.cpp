/*SetupBox
	Marcel Bardehle
Schwerpunktwaage-Waage 

V1.0 Erfolgreich getestet



/************************************ SETTINGS ************************************/                                            
#include "CG.h"
#include "HX711.h"    //https://github.com/bogde/HX711 can be installed from the library manager
#include <Preferences.h>

//HX711 constructor (dout pin, sck pint):

const int LOADCELL_1_DOUT_PIN = 27;
const int LOADCELL_1_SCK_PIN = 26;


const int LOADCELL_2_DOUT_PIN = 19;
const int LOADCELL_2_SCK_PIN = 18;

const int LOADCELL_3_DOUT_PIN = 33;
const int LOADCELL_3_SCK_PIN = 32;

HX711 loadCell[3];
#define LC1 0
#define LC2 1
#define LC3 2


/************************************ INCLUDES ********************************************/

/************************************ DEFINED VARIABLES ************************************/

const long stabilisingtime = 3000; // tare precision can be improved by adding a few seconds of stabilising time

int distanceB =0; //Distanz zwischen beiden Waagen
int distanceA =0; //Distanz zwischen vorderer Waage und Nasenleiste
int distanceCG=0; //Sollwert-Distanz zwischen Nasenleisten und Herstellerschwerpunkt
int diffCG=0;
int CG_Distance=0;
float weight[3]={0,0,0};
int loadcell_maxWeight[3]={5,10,10};
bool loadcell_rdy[3]={false,false,false};
Preferences prefs;

/*********************************************************************************************/

/************************************ DEFINED FUNCTIONS ************************************/

//This function will run once at startup
void CG::init() {
    Serial.println("CG Init Start");
  int i;
  
  prefs.begin("CG", false);
  unsigned int check=prefs.getUInt("check",0);  
//  Serial.println(check);
  prefs.end();
  
  if(check!=103){
    Serial.println("Init Parameter schreiben");
    
    setCalFactor(1, 1, 1997.501);
    setCalFactor(1, 5, 457.554);
    setCalFactor(1, 10, 215.00);
    setCalFactor(2, 1, 2003.34);
    setCalFactor(2, 5, 453.55);
    setCalFactor(2, 10, 213.84);
    setCalFactor(3, 1, 2041.06);
    setCalFactor(3, 5, 450.24);
    setCalFactor(3, 10, 215.544);
    set_ldcell_Maxweight(1,1);
    set_ldcell_Maxweight(2,1);
    set_ldcell_Maxweight(3,1);
    
    prefs.begin("CG", false);
    prefs.putUInt("check",103);
    prefs.end();
  }

  prefs.begin("CG", false);
  loadcell_maxWeight[0]=prefs.getInt("MaxWeight_1",0);
  loadcell_maxWeight[1]=prefs.getInt("MaxWeight_2",0);
  loadcell_maxWeight[2]=prefs.getInt("MaxWeight_3",0);
  prefs.end();

  loadCell[LC1].begin(LOADCELL_1_DOUT_PIN,LOADCELL_1_SCK_PIN);
  loadCell[LC2].begin(LOADCELL_2_DOUT_PIN,LOADCELL_2_SCK_PIN);
  loadCell[LC3].begin(LOADCELL_3_DOUT_PIN,LOADCELL_3_SCK_PIN);
  
 //   Serial.print("Zelle 1: ");Serial.println(loadCell[0].get_units());
//   Serial.print("Zelle 2: ");Serial.println(loadCell[1].get_units());
//    Serial.print("Zelle 3: ");Serial.println(loadCell[2].get_units());

    for(i=LC1;i<=LC3;i++){                                                //Prüfen ob alle Zellen aktiv sind und Kalibrierfaktor laden
    if(loadCell[i].get_units()!=0.00){
      loadCell[i].set_scale(getCalFactor(i+1,loadcell_maxWeight[i]));
      delay(100);
      Serial.print("Zelle ");Serial.print(i+1);Serial.println(" OK");
      loadCell[i].tare();
      loadcell_rdy[i]=true;
        }
        else{
          Serial.print("Zelle ");Serial.print(i+1);Serial.println(" nicht gefunden");
        }
    }
    Serial.print("Zelle 1: ");Serial.println(loadCell[0].get_units());
    Serial.print("Zelle 2: ");Serial.println(loadCell[1].get_units());
    Serial.print("Zelle 3: ");Serial.println(loadCell[2].get_units());
         
  
}//end setup


void CG::readSensor(){
    long CG;
    
  for(int i=LC1;i<=LC3;i++){
    if(loadcell_rdy[i]){
    weight[i]=loadCell[i].get_units(2);
    }
    else{weight[i]=0.0;}
  }
  Serial.print("Waage 1: ");Serial.println(weight[LC1],1);
  Serial.print("Waage 2: ");Serial.println(weight[LC2],1);
  Serial.print("Waage 3: ");Serial.println(weight[LC3],1);

}//end readSensor


//Auto Kalibrierung mit Kalibriergewicht
bool CG::calibrateWithKnownWheigt(int loadcell, int knownWheigt){
  bool cal_ok=false;
  float weight = 0.0;
  float calFactor=0.0;
  int loadcellnumber=loadcell-1;      //Übergebener Parameter loadcell (1,2,3) wird in Array-Parameter 0-2 geändert

  if(loadCell[loadcellnumber].is_ready()){
    loadCell[loadcellnumber].set_scale();
    delay (50);
    weight=loadCell[loadcellnumber].get_units(10);
    calFactor=weight / knownWheigt;                 //gemessenes Gewicht durch bekanntes Gewicht teilen
    loadCell[loadcellnumber].set_scale(calFactor);
    setCalFactor(loadcell,loadcell_maxWeight[loadcellnumber],calFactor);
  }
   return cal_ok;
}

//Aktuelle Kalibrierfaktoren vor Kalibrierung löschen
void CG::startCalibration(int loadcell){
   //bool loadcell_ok=false;
   int loadcellnumber=loadcell-1;
   
   if(loadCell[loadcellnumber].is_ready()){
    loadCell[loadcellnumber].set_scale();
    loadCell[loadcellnumber].tare();
  //  loadcell_ok=true; 
   }
}

//Kalibrierfaktor um definierte Werte erhöhen/reduzieren
bool CG::fineTuneCal(int loadcell, int additionValue){
  bool cal_ok=false;
  float calFactor=0.0;
  int loadcellnumber = loadcell-1;
  
  if(loadCell[loadcellnumber].is_ready()){
    calFactor=loadCell[loadcellnumber].get_scale();
    calFactor=calFactor+additionValue;
    loadCell[loadcellnumber].set_scale(calFactor);
    setCalFactor(loadcell,loadcell_maxWeight[loadcellnumber],calFactor);
    cal_ok=true;
  }//end if
  return cal_ok;
}


/*****Waage Zero setzen*/
bool CG::setZero(int loadcell){
  if(loadCell[loadcell-1].get_units()!=0){
  loadCell[loadcell-1].get_units(5);
  //delay(100);
  loadCell[loadcell-1].tare();  
  return true;
  }
  else{
    return false;
  }
}


//Kalibrierfaktor auslesen
float CG::getCalFactor(int weightNumber, int maxWeight){          //Cal-Factor für eine bestimmte Waage abrufen
  float calFactor=0.0;
  prefs.begin("CG", false);
  
  if(weightNumber==1){
    switch(maxWeight){
    case 1:
          calFactor=prefs.getFloat("Cal_1_1",0);
    break;
    case 5:
          calFactor=prefs.getFloat("Cal_1_5",0);
     break;
    case 10:
          calFactor=prefs.getFloat("Cal_1_10",0);
     break;    
    default:
    break;
    }
  }
   
    if(weightNumber==2){
    switch(maxWeight){
    case 1:
          calFactor=prefs.getFloat("Cal_2_1",0);
    break;
    case 5:
          calFactor=prefs.getFloat("Cal_2_5",0);
     break;
    case 10:
          calFactor=prefs.getFloat("Cal_2_10",0);
     break;    
    default:
    break;
      }
    }
    
    
    if(weightNumber==3){
    switch(maxWeight){
    case 1:
          calFactor=prefs.getFloat("Cal_3_1",0);
    break;
    case 5:
          calFactor=prefs.getFloat("Cal_3_5",0);
     break;
    case 10:
          calFactor=prefs.getFloat("Cal_3_10",0);
     break;    
    default:
    break;
      }
   }
  prefs.end();
  return calFactor;
  }


//Kalibrierfaktor setzen
void CG::setCalFactor(int weightNumber, int maxWeight, float calFactor){      //Cal-Factor für eine bestimmte Waage setzen
    prefs.begin("CG", false);
  if(weightNumber==1){
    switch(maxWeight){
    case 1:
          prefs.putFloat("Cal_1_1",calFactor);
    break;
    case 5:
          prefs.putFloat("Cal_1_5",calFactor);
     break;
    case 10:
          prefs.putFloat("Cal_1_10",calFactor);
     break;    
    default:
    break;
      }
    }

    if(weightNumber==2){
    switch(maxWeight){
    case 1:
          prefs.putFloat("Cal_2_1",calFactor);
    break;
    case 5:
          prefs.putFloat("Cal_2_5",calFactor);
     break;
    case 10:
          prefs.putFloat("Cal_2_10",calFactor);
     break;    
    default:
    break;
      }
    }
  
    if(weightNumber==3){
    switch(maxWeight){
    case 1:
          prefs.putFloat("Cal_3_1",calFactor);
    break;
    case 5:
          prefs.putFloat("Cal_3_5",calFactor);
     break;
    case 10:
          prefs.putFloat("Cal_3_10",calFactor);
     break;    
    default:
    break;
    }
  }
  prefs.end();
}

//Max-Gewicht für Wiegezelle setzen (1/5/10kg)
void CG::set_ldcell_Maxweight(int loadcell, int weight)
{
  prefs.begin("CG", false);
  
  loadcell_maxWeight[loadcell-1]=weight;
  
    if(loadcell==1){
        prefs.putInt("MaxWeight_1",weight);
        }
    if(loadcell==2){
        prefs.putInt("MaxWeight_2",weight);
        }
    if(loadcell==3){
        prefs.putInt("MaxWeight_3",weight);
        }
  prefs.end();
}

void CG::set_Defaults(){
    prefs.begin("CG", false);
    prefs.putUInt("check",0);
    prefs.end();
}

//Rückgabemethoden
int CG::get_CG(){return CG_Distance;}
int CG::get_DiffCG(){return diffCG;}
float CG::get_weight(int loadcell){return weight[loadcell-1];}
float CG::get_weight_total(){return (weight[0]+weight[1]+weight[2]);}
int CG::get_distanceB(){return distanceB;} //Distanz zwischen beidne Waagen 
int CG::get_distanceA(){return distanceA;}//Distanz zwischen vorderer Waage und Nasenleiste
int CG::get_distanceCG(){return distanceCG;} //Distanz zwischen Nasenleiste und Schwerpunkt
int CG::get_ldcell_Maxweight(int loadcell){return loadcell_maxWeight[loadcell-1];}

void CG::set_distanceB(int distance){distanceB=distance;} //Distanz zwischen beidne Waagen setzen
void CG::set_distanceA(int distance){distanceA=distance;}//Distanz zwischen vorderer Waage und Nasenleiste setzen
void CG::set_distanceCG(int distance){distanceCG=distance;} //Distanz zwischen Nasenleiste und Schwerpunkt setzenzero
