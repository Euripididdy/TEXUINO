#define FOUND 0
#define INCREMENT 1
#define RESET 0
#define LIMIT 1700 // 1 CYCLE = 100ms (105-107) => 10 CYCLLES = 1 SECOND => 600 CYCLES = 1 MIN => 3 MINS = 3*600=1800 CYCLES (1698 cycles to be exact)
#define ERROR 69
#define SUCCESS 83
#define PULSE 250
#define UP 1
#define DOWN -1
#define UNLOCK 1
#define LOCK -1
#define IDLE 0
#define DISABLED 0
#define ENABLED 1
#define OPEN 0
#define CLOSED 1
#define UNLOCKED 0
#define LOCKED 1
#define START 2
#define COUNTER_LIMIT 40 // MAYBE DECREASE TO 1S
//Output pins:
#define OUT_LOCK 5
#define OUT_UNLOCK 6
#define OUT_LIGHTS 7
#define OUT_ACC 8
#define OUT_ON 9
#define OUT_START 10
#define OUT_AUX1 11
#define OUT_AUX2 12
//Input pins:
#define IN_ON A0
#define IN_ACC A1
#define IN_AUX A2
#define IN_DOORS A3
#define IN_LOCKS A4
#define IN_ENGINE A5
//SPARE PINS
#define CONT_INTER 2
#define CURR_INTER 3
#define IN_CONT4 4
#define IN_CURR4 13

// Indicators:
int carStatusByte = 0;
int inactivityCounter = 0;
int cycleCounter = 0;
// Flags:
boolean messages = false;
boolean debug = false;
boolean lostConnection = false;
boolean newConnection = false;
boolean centralLocking = true;
boolean autolock = false;
boolean autounlock = false;
boolean cranking = false;
boolean interpretCommand = true;
// Inputs:
int linkedPast = 0;
int linkedPresent = 0;
int doors = 0;
int engine = 0;
int keyacc = 0;
int locks = 0;
int keyon = 0;
int contaux = 0;

void setup()
{
  Serial.begin(115200);
  //Relays:
  pinMode(OUT_LIGHTS, OUTPUT);
  pinMode(OUT_ACC, OUTPUT);
  pinMode(OUT_ON, OUTPUT);
  pinMode(OUT_START, OUTPUT);
  pinMode(OUT_LOCK, OUTPUT);
  pinMode(OUT_UNLOCK, OUTPUT);
  pinMode(OUT_AUX1, OUTPUT);
  pinMode(OUT_AUX2, OUTPUT);
  //Contuinity inputs:
  pinMode(IN_AUX, INPUT_PULLUP);
  pinMode(IN_DOORS, INPUT_PULLUP);
  pinMode(IN_LOCKS, INPUT_PULLUP);
  //Current inputs:
  pinMode(IN_ON, INPUT);
  pinMode(IN_ACC, INPUT);
  pinMode(IN_ENGINE, INPUT);
  //Initialise Wi-Fi module:
  setupModule();
}

//MAYBE THE BUFFER OVERFLOWS AND THE COMMAND IS LOST??? INDEED THIS IS WHAT WAS HAPPENING -_-
void loop()
{
  //long tickCountStart = millis(); //<- for counting cycle length
  counter();
  queryConnection();
  processCommand(streamRead());
  getFeedback();
  debugger();
  determineConnection();
  checkLocks(locks);
  measureInactivity(INCREMENT);
  //long tickCountEnd = millis(); //<- for counting cycle length
  //long loopDuration = tickCountEnd - tickCountStart; //<- for counting cycle length
  //sendInfoMessage("LOOP CYCLE: " + String(loopDuration), true);
  //delay(50);
}

void counter()
{
  //1 CYCLE = ~ 50MS
  if (cycleCounter == COUNTER_LIMIT)
  {
    cycleCounter = 0;
  }
  cycleCounter++;
}

void debugger()
{
  if (debug)
  {
    sendInfoMessage("=======FEEDBACK=======", false);
    sendInfoMessage("[CON - " + String(linkedPresent) + " | EL - " + String(keyacc) + " | EN - "  + String(engine) + " | LO - " + String(locks) + " | DO - " + String(doors) + " | COUNTER - " + String(inactivityCounter) + "]", true);
    sendInfoMessage("======================", false);
    delay(500);
  }
}

void sendInfoMessage(String msg, boolean timestamp)
{
  if (messages)
  {
    interpretCommand = false;

    if (timestamp)
    {
      long tickCount = millis();
      msg = String(tickCount) + " : " + msg;
    }
    streamWrite(msg, true);
    delay(msg.length() * 5);

    interpretCommand = true;
  }
}

void streamWrite(String value, boolean newline)
{
  int msgSize = value.length();
  if (newline)
  {
    msgSize = msgSize + 2;
  }
  Serial.print("AT+CIPSEND=0,");
  Serial.println(msgSize);
  delay(50);
  Serial.print(value);
  if (newline)
  {
    Serial.print("\n\r");
  }
}

String streamRead()
{
  String content = "";
  char character;
  delay(50);
  while (Serial.available())
  {
    character = Serial.read();
    content.concat(character);
  }
  return content;
}

void processCommand(String cmd)
{
  if (cmd.length() > 0)
  {
    if (cmd.indexOf("STATUS:2") >= 0)
    {
      linkedPresent = 2;
      //streamWrite("ip obt", true);
    }
    if (cmd.indexOf("STATUS:3") >= 0)
    {
      linkedPresent = 1;
      //blinkTwice();
      //streamWrite("connected", true);
    }
    if (cmd.indexOf("STATUS:4") >= 0)
    {
      linkedPresent = 0;
      //blinkOnce();
      //streamWrite("discon", true);
    }

    if (cmd.indexOf("/") >= 0)
    {
      measureInactivity(RESET);
    }

    if (cmd.indexOf("MSGS") >= 0)
    {
      messages = !messages;
      if (messages)
      {
        streamWrite("MESSAGES SHOWING", true);
      }
      else
      {
        streamWrite("MESSAGES HIDDEN", true);
      }
    }
    if (cmd.indexOf("DBG") >= 0)
    {
      debug = !debug;
      if (debug)
      {
        sendInfoMessage("DEBUGGER ACTIVE", false);
      }
      else
      {
        sendInfoMessage("DEBUGGER INACTIVE", false);
      }
    }
    if (cmd.indexOf("OFF") >= 0)
    {
      setIgnition(0);
      //blinkOnce();
    }
    if (cmd.indexOf("ACC") >= 0)
    {
      setIgnition(1);
      //blinkOnce();
    }
    if (cmd.indexOf("ON") >= 0)
    {
      setIgnition(2);
      //blinkOnce();
    }
    if (cmd.indexOf("STRT") >= 0)
    {
      setIgnition(3);
      //blinkOnce();
    }
    if (cmd.indexOf("NLK") >= 0)
    {
      if (locks == LOCKED)
      {
        setDoors(UNLOCK);
        sendInfoMessage("Vehicle remotely unlocked!", true);
      }
      //blinkOnce();
    }
    if (cmd.indexOf("LCK") >= 0)
    {
      if (locks == UNLOCKED)
      {
        setDoors(LOCK);
        sendInfoMessage("Vehicle remotely locked!", true);
      }
      //blinkOnce();
    }
    if (cmd.indexOf("LGHI") >= 0)
    {
      setLights(ENABLED);
      //blinkOnce();
    }
    if (cmd.indexOf("LGHO") >= 0)
    {
      setLights(DISABLED);
      //blinkOnce();
    }
    if (cmd.indexOf("ALI") >= 0)
    {
      autolock = true;
      sendInfoMessage("Auto lock enabled!", true);
      //blinkOnce();
    }
    if (cmd.indexOf("ALO") >= 0)
    {
      autolock = false;
      sendInfoMessage("Auto lock disabled!", true);
      //blinkOnce();
    }
    if (cmd.indexOf("AUI") >= 0)
    {
      autounlock = true;
      sendInfoMessage("Auto unlock enabled!", true);
      //blinkOnce();
    }
    if (cmd.indexOf("AUO") >= 0)
    {
      autounlock = false;
      sendInfoMessage("Auto unlock disabled!", true);
      //blinkOnce();
    }
    if (cmd.indexOf("CLI") >= 0)
    {
      centralLocking = true;
      sendInfoMessage("Central locking enabled!", true);
      //blinkOnce();
    }
    if (cmd.indexOf("CLO") >= 0)
    {
      centralLocking = false;
      sendInfoMessage("Central locking disabled!", true);
      //blinkOnce();
    }
    if (cmd.indexOf("CSB") >= 0)
    {
      streamWrite(String(carStatusByte), true);
      //streamWrite(String(carStatusByte), false);
      //blinkOnce();
    }
    if ((cmd.indexOf("CNET") >= 0) && (cmd.indexOf("<") >= 0) && (cmd.indexOf(">") >= 0) && (cmd.indexOf("{") >= 0) && (cmd.indexOf("}") >= 0))
    {
      String netname = cmd.substring(cmd.indexOf('<') + 1, cmd.indexOf('>'));
      String netpass = cmd.substring(cmd.indexOf('{') + 1, cmd.indexOf('}'));
      configureNetwork(netname, netpass, "1", "3");
      setupModule();
      //blinkOnce();
    }
  }
}

boolean getResponse()
{
  boolean response = false;
  if (Serial.available() > 0)
  {
    if (Serial.find("OK"))
    {
      //blinkTwice();
      response = true;
    }
  }
  return response;
}

void queryConnection()
{
  if (cycleCounter == COUNTER_LIMIT)
  {
    linkedPast = linkedPresent;
    Serial.println("AT+CIPSTATUS");
    delay(25); // WAS 50ms
  }
}

void getFeedback()
{
  //if (cycleCounter == COUNTER_LIMIT)
  //{
    //Electrical accessories
    int keyaccpast = keyacc;
    keyacc = digitalRead(IN_ACC);
    bitWrite(carStatusByte, 0, keyacc);
    if (keyacc != keyaccpast)
    {
      if (keyacc == 1)
      {
        digitalWrite(OUT_ACC, HIGH); 
      }
      else
      {
        digitalWrite(OUT_ACC, LOW);
      }
    }
    
    //Engine prime
    int keyonpast = keyon;
    keyon = digitalRead(IN_ON);
    bitWrite(carStatusByte, 4, keyon);
    if (keyon != keyonpast)
    {
      if (keyon == 1)
      {
          //digitalWrite(OUT_ACC, HIGH);
          digitalWrite(OUT_ON, HIGH);
      }
      /*
      else
      {
        if (keyacc == 0)
        {
          digitalWrite(OUT_ON, LOW);
        }
      }
      */
    }
    //Engine
    engine = digitalRead(IN_ENGINE);
    if (engine == 1)
    {
      digitalWrite(OUT_START, LOW); //starterfailsafe
    }
    bitWrite(carStatusByte, 1, engine);

    //Locks
    locks = digitalRead(IN_LOCKS);
    bitWrite(carStatusByte, 2, locks);

    //Doors
    doors = digitalRead(IN_DOORS);
    bitWrite(carStatusByte, 3, doors);

    //Continuity Auxiliary
    contaux = digitalRead(IN_AUX);
    bitWrite(carStatusByte, 5, contaux);

    //Connection
    bitWrite(carStatusByte, 6, linkedPresent);
    //delay(250);
  //}
}

/*Method which determines the natue of the connection*/
void determineConnection()
{
  if (cycleCounter == COUNTER_LIMIT)
  {
    if (linkedPresent == 0 && linkedPast == 1)
    {
      lostConnection = true;
      newConnection = false;
    }
    else if (linkedPresent == 1 && linkedPast == 0)
    {
      newConnection = true;
      lostConnection = false;
    }
    else if (linkedPresent == linkedPast)
    {
      lostConnection = false;
      newConnection = false;
    }
  }
}

/*Method for checking the car locks*/
void checkLocks(int past)
{
  if (cycleCounter == COUNTER_LIMIT)
  {
    int present = digitalRead(IN_LOCKS);
    /*if previous state is not equal to current then we've had a change
    in lock state meaninig that if central locking is enabled we must sync
    all the other locks to match this new lock state*/
    if (past != present)
    {
      if (centralLocking)
      {
        if (present == LOCKED)
        {
          setDoors(LOCK);
          sendInfoMessage("Vehicle manually locked!", true);
        }
        else if (present == UNLOCKED)
        {
          setDoors(UNLOCK);
          sendInfoMessage("Vehicle manually unlocked!", true);
        }
      }
    }
    /* if connection was lost then attempt to secure the vehicle*/
    if (lostConnection)
    {
      sendInfoMessage("Lost connection! Securing vehicle..", true);
      secureVehicle();
    }
    /* if new connection and autounlock enabled then unlock the vehicle*/
    if (newConnection && autounlock)
    {
      if (locks == LOCKED)
      {
        setDoors(UNLOCK);
        sendInfoMessage("Doors auto-unlocked!", true);
      }
    }
  }
}

void setupModule()
{
  delay(1000);
  resetNetwork();
  multipleConnections(true);
  setServer("1", "5050");
}

boolean setServer(String state, String port)
{
  String cmd = "AT+CIPSERVER=" + state + "," + port;
  Serial.println(cmd);
  delay(150);
  return getResponse();
}

boolean multipleConnections(boolean condition)
{
  if (condition)
  {
    Serial.println("AT+CIPMUX=1");
  }
  else
  {
    Serial.println("AT+CIPMUX=0");
  }
  delay(150);
  return getResponse();
}

boolean resetNetwork()
{
  Serial.println("AT+RST");
  delay(2000);
  return getResponse();
}

boolean setNetworkMode(String mode)
{
  String cmd = "AT+CWMODE=" + mode;
  Serial.println(cmd);
  delay(100);
  return getResponse();
}

boolean configureNetwork(String name, String password, String channel, String protection)
{
  String quote = "\"";
  name = quote + name + quote;
  password = quote + password + quote;
  String cmd = "AT+CWSAP=" + name + "," + password + "," + channel + "," + protection;
  Serial.println(cmd);
  delay(100);
  return getResponse();
}

void measureInactivity(int state)
{
  if (state == RESET)
  {
    inactivityCounter = 0;
  }
  else if (state == INCREMENT)
  {
    inactivityCounter++;
    if (inactivityCounter == LIMIT)
    {
      sendInfoMessage("Vehicle inactive for long time! Securing vehicle..", true);
      secureVehicle();
      inactivityCounter = 0; // or measureInactivity(RESET);
    }
  }
}

void secureVehicle()
{
  digitalWrite(OUT_START, LOW);
  if (engine == DISABLED && keyacc == DISABLED && doors == CLOSED && locks == UNLOCKED)
  {
    if (autolock)
    {
      setDoors(LOCK);
      sendInfoMessage("Doors auto-locked!", true);
    }
  }
  sendInfoMessage("Vehicle secured.", true);
}

/*Method which sets the state of the car ignition*/
void setIgnition(int state)
{
  if (state == 0)
  {
    digitalWrite(OUT_ACC, LOW);
    digitalWrite(OUT_ON, LOW);
    digitalWrite(OUT_START, LOW);
    cranking = false;
    bitWrite(carStatusByte, 7, 0);
  }
  else if (state == 1)
  {
    digitalWrite(OUT_ACC, HIGH);
    digitalWrite(OUT_ON, LOW);
    digitalWrite(OUT_START, LOW);
    cranking = false;
    bitWrite(carStatusByte, 7, 1);
  }
  else if (state == 2)
  {
    digitalWrite(OUT_ACC, HIGH);
    digitalWrite(OUT_ON, HIGH);
    digitalWrite(OUT_START, LOW);
    cranking = false;
    bitWrite(carStatusByte, 7, 1);
  }
  else if (state == 3)
  {
    digitalWrite(OUT_ACC, HIGH);
    digitalWrite(OUT_ON, HIGH);
    digitalWrite(OUT_START, HIGH);
    cranking = true;
    bitWrite(carStatusByte, 7, 1);
  }
}

/*Method which sets the state of the car door locks*/
void setDoors(int state)
{
  if (state == LOCK)
  {
    digitalWrite(OUT_LOCK, HIGH);
    delay(PULSE);
    digitalWrite(OUT_LOCK, LOW);
  }
  else if (state == UNLOCK)
  {
    digitalWrite(OUT_UNLOCK, HIGH);
    delay(PULSE);
    digitalWrite(OUT_UNLOCK, LOW);
  }
}

/*Method which sets the state of the car parking lights*/
void setLights(int state)
{
  if (state == ENABLED)
  {
    digitalWrite(OUT_LIGHTS, HIGH);
    bitWrite(carStatusByte, 5, 1);
    sendInfoMessage("Lights on!", true);

  }
  else if (state == DISABLED)
  {
    digitalWrite(OUT_LIGHTS, LOW);
    bitWrite(carStatusByte, 5, 0);
    sendInfoMessage("Lights off!", true);
  }
}
/*
void blinkOnce()
{
  digitalWrite(LED, HIGH);
  delay(150);
  digitalWrite(LED, LOW);
  //sendInfoMessage("LED BLINKED", true);
}

void blinkTwice()
{
  digitalWrite(LED, HIGH);
  delay(75);
  digitalWrite(LED, LOW);
  delay(75);
  digitalWrite(LED, HIGH);
  delay(75);
  digitalWrite(LED, LOW);
}
*/
