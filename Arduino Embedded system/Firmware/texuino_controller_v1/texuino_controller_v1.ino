#define ON 1
#define OFF 0
//Output pins:
#define R1 5
#define R2 6
#define R3 7
#define R4 8
#define R5 9
#define R6 10
#define R7 11
#define R8 12
//Input pins:
#define IN_CURR1 A0
#define IN_CURR2 A1
#define IN_CONT1 A2
#define IN_CONT2 A3
#define IN_CONT3 A4
#define IN_CURR3 A5
#define CONT_INTER 2
#define CURR_INTER 3
#define IN_CONT4 4
#define IN_CURR4 13

boolean messages = false;
boolean safeDisco = false;
int relayByte = 0;
int connectionStatus = -1;

void setup()
{
  Serial.begin(115200);
  //Relays:
  pinMode(R1, OUTPUT);
  pinMode(R2, OUTPUT);
  pinMode(R3, OUTPUT);
  pinMode(R4, OUTPUT);
  pinMode(R5, OUTPUT);
  pinMode(R6, OUTPUT);
  pinMode(R7, OUTPUT);
  pinMode(R8, OUTPUT);
  //Contuinity inputs:
  pinMode(IN_CONT1, INPUT_PULLUP);
  pinMode(IN_CONT2, INPUT_PULLUP);
  pinMode(IN_CONT3, INPUT_PULLUP);
  pinMode(IN_CONT4, INPUT_PULLUP);
  pinMode(CONT_INTER, INPUT_PULLUP);
  //Current inputs:
  pinMode(IN_CURR1, INPUT);
  pinMode(IN_CURR2, INPUT);
  pinMode(IN_CURR3, INPUT);
  pinMode(IN_CURR4, INPUT);
  pinMode(CURR_INTER, INPUT);
  //Initialise Wi-Fi module:
  setupModule();
}

void loop()
{
  queryConnection();
  processCommand(streamRead());
}

void sendInfoMessage(String msg, boolean timestamp)
{
  if (messages)
  {
    if (timestamp)
    {
      long tickCount = millis();
      msg = String(tickCount) + " : " + msg;
    }
    streamWrite(msg, true);
    delay(msg.length() * 5);
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
    // CONNECTION WHOIS
    if (cmd.indexOf("STATUS:2") >= 0)
    {
      connectionStatus = 2;
      sendInfoMessage("ip obt", true);
      //streamWrite("ip obt", true);
    }
    if (cmd.indexOf("STATUS:3") >= 0)
    {
      connectionStatus = 1;
      sendInfoMessage("connected", true);
      //streamWrite("connected", true);
    }
    if (cmd.indexOf("STATUS:4") >= 0)
    {
      connectionStatus = 0;
      sendInfoMessage("disconnected", true);
      if (safeDisco)
      {
        setAllRelays(OFF);
      }
      //streamWrite("discon", true);
    }
    // DEBUG STUFF
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
    // PREFERENCES
    if (cmd.indexOf("OSD") >= 0)
    {
      safeDisco = false;
      sendInfoMessage("Safe disconnect off", true);
    }
    if (cmd.indexOf("ISD") >= 0)
    {
      safeDisco = true;
      sendInfoMessage("Safe disconnect on", true);
    }
    // RELAY CONTROL
    if (cmd.indexOf("OR1") >= 0)
    {
      digitalWrite(R1, LOW);
      bitWrite(relayByte, 0, 0);
      sendInfoMessage("Relay 1 off", true);
    }
    if (cmd.indexOf("IR1") >= 0)
    {
      digitalWrite(R1, HIGH);
      bitWrite(relayByte, 0, 1);
      sendInfoMessage("Relay 1 on", true);
    }
    
    if (cmd.indexOf("OR2") >= 0)
    {
      digitalWrite(R2, LOW);
      bitWrite(relayByte, 1, 0);
      sendInfoMessage("Relay 2 off", true);
    }
    if (cmd.indexOf("IR2") >= 0)
    {
      digitalWrite(R2, HIGH);
      bitWrite(relayByte, 1, 1); 
      sendInfoMessage("Relay 2 on", true); 
    }
    
    if (cmd.indexOf("OR3") >= 0)
    {
      digitalWrite(R3, LOW);
      bitWrite(relayByte, 2, 0);
      sendInfoMessage("Relay 3 off", true);
    }
    if (cmd.indexOf("IR3") >= 0)
    {
      digitalWrite(R3, HIGH);
      bitWrite(relayByte, 2, 1); 
      sendInfoMessage("Relay 3 on", true);   
    }
    
    if (cmd.indexOf("OR4") >= 0)
    {
      digitalWrite(R4, LOW);
      bitWrite(relayByte, 3, 0);
      sendInfoMessage("Relay 4 off", true);
    }
    if (cmd.indexOf("IR4") >= 0)
    {
      digitalWrite(R4, HIGH);
      bitWrite(relayByte, 3, 1);  
      sendInfoMessage("Relay 4 on", true);  
    }
    
    if (cmd.indexOf("OR5") >= 0)
    {
      digitalWrite(R5, LOW);
      bitWrite(relayByte, 4, 0);
      sendInfoMessage("Relay 5 off", true);
    }
    if (cmd.indexOf("IR5") >= 0)
    {
      digitalWrite(R5, HIGH);
      bitWrite(relayByte, 4, 1); 
      sendInfoMessage("Relay 5 on", true);   
    }
    
    if (cmd.indexOf("OR6") >= 0)
    {
      digitalWrite(R6, LOW);
      bitWrite(relayByte, 5, 0);
      sendInfoMessage("Relay 6 off", true);
    }
    if (cmd.indexOf("IR6") >= 0)
    {
      digitalWrite(R6, HIGH); 
      bitWrite(relayByte, 5, 1); 
      sendInfoMessage("Relay 6 on", true); 
    }
    
    if (cmd.indexOf("OR7") >= 0)
    {
      digitalWrite(R7, LOW);
      bitWrite(relayByte, 6, 0);
      sendInfoMessage("Relay 7 off", true);
    }
    if (cmd.indexOf("IR7") >= 0)
    {
      digitalWrite(R7, HIGH);  
      bitWrite(relayByte, 6, 1);  
      sendInfoMessage("Relay 7 on", true);
    }
    
    if (cmd.indexOf("OR8") >= 0)
    {
      digitalWrite(R8, LOW);
      bitWrite(relayByte, 7, 0);
      sendInfoMessage("Relay 8 off", true);
    }
    if (cmd.indexOf("IR8") >= 0)
    {
      digitalWrite(R8, HIGH); 
      bitWrite(relayByte, 7, 1);
      sendInfoMessage("Relay 8 on", true);   
    }

    if (cmd.indexOf("OAR") >= 0)
    {
      setAllRelays(OFF);
      sendInfoMessage("All relays off", true);
    }
    if (cmd.indexOf("IAR") >= 0)
    {
      setAllRelays(ON);
      sendInfoMessage("All relays on", true);   
    }

    if (cmd.indexOf("GRB") >= 0)
    {
      streamWrite(String(relayByte), true);
    }

    if (cmd.indexOf("GIB") >= 0)
    {
      streamWrite(String(getInputs()), true);
    }

    if (cmd.indexOf("INRPT") >= 0)
    {
      streamWrite(String(getInterupts()), true);
    }

    if (cmd.indexOf("GFB") >= 0)
    {
      streamWrite(String(getInputs()) + ":" + String(relayByte) + ":" + String(getInterupts()) + ":", true);
    }
    
    if ((cmd.indexOf("CNET") >= 0) && (cmd.indexOf("<") >= 0) && (cmd.indexOf(">") >= 0) && (cmd.indexOf("{") >= 0) && (cmd.indexOf("}") >= 0))
    {
      String netname = cmd.substring(cmd.indexOf('<') + 1, cmd.indexOf('>'));
      String netpass = cmd.substring(cmd.indexOf('{') + 1, cmd.indexOf('}'));
      configureNetwork(netname, netpass, "1", "3");
      setupModule();
    }
    
  }
}

void setAllRelays(int state)
{
  if (state == OFF)
  {
    digitalWrite(R1, LOW);
    digitalWrite(R2, LOW);
    digitalWrite(R3, LOW);
    digitalWrite(R4, LOW);
    digitalWrite(R5, LOW);
    digitalWrite(R6, LOW);
    digitalWrite(R7, LOW);
    digitalWrite(R8, LOW);
    bitWrite(relayByte, 0, 0);
    bitWrite(relayByte, 1, 0);
    bitWrite(relayByte, 2, 0);
    bitWrite(relayByte, 3, 0);
    bitWrite(relayByte, 4, 0);
    bitWrite(relayByte, 5, 0);
    bitWrite(relayByte, 6, 0);
    bitWrite(relayByte, 7, 0);
  }
  else if (state == ON)
  {
    digitalWrite(R1, HIGH);
    digitalWrite(R2, HIGH);
    digitalWrite(R3, HIGH);
    digitalWrite(R4, HIGH);
    digitalWrite(R5, HIGH);
    digitalWrite(R6, HIGH);
    digitalWrite(R7, HIGH);
    digitalWrite(R8, HIGH);
    bitWrite(relayByte, 0, 1);
    bitWrite(relayByte, 1, 1);
    bitWrite(relayByte, 2, 1);
    bitWrite(relayByte, 3, 1);
    bitWrite(relayByte, 4, 1);
    bitWrite(relayByte, 5, 1);
    bitWrite(relayByte, 6, 1);
    bitWrite(relayByte, 7, 1); 
  }
}

void queryConnection()
{
    Serial.println("AT+CIPSTATUS");
    delay(25); // WAS 50ms
}

boolean getResponse()
{
  boolean response = false;
  if (Serial.available() > 0)
  {
    if (Serial.find("OK"))
    {
      response = true;
    }
  }
  return response;
}

int getInterupts()
{
  int input = -1;
  int interuptByte = 0;

  input = digitalRead(CONT_INTER);
  bitWrite(interuptByte, 0, input);

  input = digitalRead(CURR_INTER);
  bitWrite(interuptByte, 1, input);

  return interuptByte;
}

int getInputs()
{
  int input = -1;
  int inputsByte = 0;

  input = digitalRead(IN_CONT1);
  bitWrite(inputsByte, 0, input);

  input = digitalRead(IN_CONT2);
  bitWrite(inputsByte, 1, input);
  
  input = digitalRead(IN_CONT3);
  bitWrite(inputsByte, 2, input);
  
  input = digitalRead(IN_CONT4);
  bitWrite(inputsByte, 3, input);
  
  input = digitalRead(IN_CURR1);
  bitWrite(inputsByte, 4, input);
  
  input = digitalRead(IN_CURR2);
  bitWrite(inputsByte, 5, input);

  input = digitalRead(IN_CURR3);
  bitWrite(inputsByte, 6, input);

  input = digitalRead(IN_CURR4);
  bitWrite(inputsByte, 7, input);
  
  return inputsByte;
}

int getOutputs()
{
  return relayByte;
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
