#include <Wire.h>
#include <SPI.h>
#include <Adafruit_LSM9DS1.h>
#include <Adafruit_Sensor.h>  // not used in this demo but required!
#include <pt.h>

static struct pt timerPT, pressurePT, imuPT;

// i2c
Adafruit_LSM9DS1 lsm = Adafruit_LSM9DS1();

#define LSM9DS1_SCK A5
#define LSM9DS1_MISO 12
#define LSM9DS1_MOSI A4
#define LSM9DS1_XGCS 6
#define LSM9DS1_MCS 5

int milliDelay = 100;

//The pin that the controller is connected to
const int FSR_PIN = A0;

// Set values of voltage and resistance used for calculations later
const float VCC = 4.98; // Measured voltage of Ardunio 5V line
const float R_DIV = 10000.0; // Measured resistance of 3.3k resistor

void setupSensor()
{

  // Set the sensors and their range
  lsm.setupAccel(lsm.LSM9DS1_ACCELRANGE_2G);
  lsm.setupMag(lsm.LSM9DS1_MAGGAIN_4GAUSS);
  lsm.setupGyro(lsm.LSM9DS1_GYROSCALE_245DPS);
}

void setup()
{
  Serial.begin(115200);
  Serial.flush();

  while (!Serial) {
    delay(1); // will pause Zero, Leonardo, etc until serial console opens
  }
  
  // Try to initialise and warn if we couldn't detect the chip
  if (!lsm.begin())
  {
    while (1);
  }

  // helper to just set the default scaling we want, see above!
  setupSensor();
  pinMode(FSR_PIN, INPUT);


  //Pressure sensor code
  Wire.begin(); // join i2c bus (address optional for master)
  //TWBR = 12; //Increase i2c speed if you have Arduino MEGA2560, not suitable for Arduino UNO

  PT_INIT(&timerPT);
  PT_INIT(&pressurePT);
  PT_INIT(&imuPT);
}

static int timerPTFunc (struct pt *tim, int interval)
{
  static unsigned long timestamp = 0;
  PT_BEGIN(tim);
  while (1)
  {
    PT_WAIT_UNTIL(tim, millis() - timestamp > interval);
    timestamp = millis();
    Serial.println("t " + String(interval / 1000.0));
  }
  PT_END(tim)
}

static int pressurePTFunc (struct pt *pressure, int interval)
{
  static unsigned long timestamp = 0;
  PT_BEGIN(pressure);
  while (1)
  {
    PT_WAIT_UNTIL(pressure, millis() - timestamp > interval);
    timestamp = millis();

    int fsrADC = analogRead(FSR_PIN);
    // If the FSR has no pressure, the resistance will be near infinite. So the voltage should be near 0.
    if (fsrADC != 0) // If the analog reading is non-zero
    {
      // Use ADC reading to calculate voltage:
      float fsrV = fsrADC * VCC / 1023.0;
      // Use voltage and static resistor value to calculate FSR resistance:
      float fsrR = R_DIV * (VCC / fsrV - 1.0);
      float force;
      float fsrG = 1.0 / fsrR; 
      if (fsrR <= 600)
        force = (fsrG - 0.00075) / 0.00000032639;
      else
        force =  fsrG / 0.000000642857;
      Serial.print("p " + String(fsrADC / 1023.0 * 22.0462) + " ");
    }
    else
    {
    // No input or pressure detected
    Serial.print("p 0 ");
    }
  }
  
  PT_END(pressure)
}

static int imuPTFunc (struct pt *imu, int interval)
{
  static unsigned long timestamp = 0;
  PT_BEGIN(imu);
  while (1)
  {
    PT_WAIT_UNTIL(imu, millis() - timestamp > interval);
    timestamp = millis();
    
    lsm.read();
    sensors_event_t a, m, g, temp;
    lsm.getEvent(&a, &m, &g, &temp);
    
    Serial.print("a ");
    Serial.print(a.acceleration.x); Serial.print(" ");
    Serial.print(a.acceleration.y); Serial.print(" ");
    Serial.println(a.acceleration.z);

  /*magnetism is not needed. Leaving code base for reference

    Serial.print("m ");
    Serial.print(m.magnetic.x); Serial.print(" ");
    Serial.print(m.magnetic.y); Serial.print(" ");
    Serial.println(m.magnetic.z);*/

    Serial.print("g ");
    Serial.print(g.gyro.x);   Serial.print(" ");
    Serial.print(g.gyro.y); Serial.print(" ");
    Serial.println(g.gyro.z);
  }
  PT_END(imu)
}

void loop()
{
  pressurePTFunc(&pressurePT, milliDelay);
  
  Serial.println("t " + String(milliDelay/1000.0));
  lsm.read();
  sensors_event_t a, m, g, temp;
  lsm.getEvent(&a, &m, &g, &temp);
    
  Serial.print("a ");
  Serial.print(a.acceleration.x); Serial.print(" ");
  Serial.print(a.acceleration.y); Serial.print(" ");
  Serial.println(a.acceleration.z);
    
  Serial.print("g ");
  Serial.print(g.gyro.x);   Serial.print(" ");
  Serial.print(g.gyro.y); Serial.print(" ");
  Serial.println(g.gyro.z);

  delay(milliDelay);
  
  /*
  pressurePTFunc(&pressurePT, milliDelay);
  imuPTFunc(&imuPT, 1000);
  timerPTFunc(&timerPT, milliDelay);
  
/*
    Serial.println("t " + String(milliDelay / 1000.0));
    
    lsm.read();
    sensors_event_t a, m, g, temp;
    lsm.getEvent(&a, &m, &g, &temp);
    
    Serial.print("a ");
    Serial.print(a.acceleration.x); Serial.print(" ");
    Serial.print(a.acceleration.y); Serial.print(" ");
    Serial.println(a.acceleration.z);
    
    Serial.print("g ");
    Serial.print(g.gyro.x);   Serial.print(" ");
    Serial.print(g.gyro.y); Serial.print(" ");
    Serial.println(g.gyro.z);
      
  /*

    long last_time = millis();






    Serial.print("t: ");
    Serial.print(milliDelay/1000.0);
    Serial.print(" ");andr



    long runDelay = (millis() - last_time);
    delay(milliDelay - runDelay);
    }


    //Pressure sensor code
    short readDataFromSensor(short address)
    {
    byte i2cPacketLength = 6;//i2c packet length. Just need 6 bytes from each slave
    byte outgoingI2CBuffer[3];//outgoing array buffer
    byte incomingI2CBuffer[6];//incoming array buffer

    outgoingI2CBuffer[0] = 0x01;//I2c read command
    outgoingI2CBuffer[1] = 128;//Slave data offset
    outgoingI2CBuffer[2] = i2cPacketLength;//require 6 bytes

    Wire.beginTransmission(address); // transmit to device
    Wire.write(outgoingI2CBuffer, 3);// send out command
    byte error = Wire.endTransmission(); // stop transmitting and check slave status
    if (error != 0) return -1; //if slave not exists or has error, return -1
    Wire.requestFrom(address, i2cPacketLength);//require 6 bytes from slave

    byte incomeCount = 0;
    while (incomeCount < i2cPacketLength)    // slave may send less than requested
    {
    if (Wire.available())
    {
      incomingI2CBuffer[incomeCount] = Wire.read(); // receive a byte as character
      incomeCount++;
    }
    else
    {
      delayMicroseconds(10); //Wait 10us
    }
    }

    short rawData = (incomingI2CBuffer[4] << 8) + incomingI2CBuffer[5]; //get the raw data

    return rawData;


    //Pressure sensor code
    /*
    byte i2cAddress = 0x04; // Slave address (SingleTact), default 0x04
    short pressureData = readDataFromSensor(i2cAddress);
    prevPressure[2] = prevPressure[1];
    prevPressure[1] = prevPressure[0];
    prevPressure[0] = pressureData;
    short averagePressure = (prevPressure[0] + prevPressure[1] + prevPressure[2])/3.0;
    Serial.print("p: "); Serial.print(averagePressure);
    Serial.print("\n");*/
}
