/*SetupBox
	Marcel Bardehle
EWD-Waage mit MMA8451

V0.1 Beginn

/************************************ SETTINGS ************************************/                                            

#include "BluetoothSerial.h"

class EWD{
  private:
  float round(float Value, int signs);
  float RUD_irr_low_pass_filter(float aSmoothedValue, float aCurrentValue, float aSmoothingFactor);
  
public:
	bool init();
	void readSensor();
	void zeroingFunction(char sensor);
	float get_SensorA();
	float get_SensorB();
	float get_SensorDiff();
  bool calibrateSensor(int sensor, BluetoothSerial &btserial);
  void set_Defaults();
  
  void RUD_readSensor();
  void RUD_zeroingFunction(char sensor);
  float RUD_get_SensorA();
  float RUD_get_SensorB();
  float RUD_get_SensorDiff();
  void RUD_set_DiffReverse(bool value);
    
};
