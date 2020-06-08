/*SetupBox
	Marcel Bardehle
Schwerpunktwaage mit HX711

V0.1 Beginn

Ursprungsprogrammierung 
/*****************************RC INCIDENCE METER************************************


/************************************ SETTINGS ************************************/                                            
class CG{
private:
void setCalFactor(int weightNumber, int maxWeight, float calFactor);
  
public:
	void init();
	void readSensor();
  int get_CG();
  int get_DiffCG();
  float get_weight(int loadcell);
  //float get_weight_1();
  //float get_weight_2();
  //float get_weight_3();
  float get_weight_total();
  int get_ldcell_Maxweight(int loadcell);
  float getCalFactor(int weightNumber, int maxWeight);
  bool setZero(int loadcell);
 // int get_ldcell_1_weight();
 // int get_ldcell_2_weight();
 // int get_ldcell_3_weight();

  void startCalibration(int loadcell);
  bool calibrateWithKnownWheigt(int loadcell, int knownWheigt);
  bool fineTuneCal(int loadcell, int additionValue);
  
  
  int get_distanceB(); //Distanz zwischen beidne Waagen 
  int get_distanceA();//Distanz zwischen vorderer Waage und Nasenleiste
  int get_distanceCG();//Distanz zwischen Nasenleiste und Schwerpunkt
  
  void set_distanceB(int distance);//Distanz zwischen beidne Waagen setzen
  void set_distanceA(int distance);//Distanz zwischen vorderer Waage und Nasenleiste setzen
  void set_distanceCG(int distance);//Distanz zwischen Nasenleiste und Schwerpunkt setzen
  
  void set_ldcell_Maxweight(int loadcell, int weight);
  void set_Defaults();
};
