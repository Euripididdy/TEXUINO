#define ON 1
#define OFF 0
#define GOT_IP 2
#define CONNCTD 3
#define DSCNCTD 4
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

boolean messages[] = {false, false, false, false, false};
boolean safeDisconnect[] = {false, false, false, false, false};
int connectionStatus[] = {DSCNCTD, DSCNCTD, DSCNCTD, DSCNCTD, DSCNCTD};
int relayByte = 0;
int responseDelay = 10;
int networkJoinDelay = 5000;

String staIp;
String apIp;

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
  processCommand(streamRead());
}

void sendInfoMessage(String connnectionId, String msg, boolean timestamp)
{
  int id = connnectionId.toInt();
  if ((id >= 0) && (id <= 5))
  {
    if (messages[id])
    {
      if (timestamp)
      {
        long tickCount = millis();
        msg = String(tickCount) + " : " + msg;
      }
      streamWrite(connnectionId, msg, true);
    }
  }
}

void streamWrite(String connnectionId, String value, boolean newline)
{
  int msgSize = value.length();
  if (newline)
  {
    msgSize = msgSize + 2;
  }

  Serial.print("AT+CIPSEND=" + connnectionId + ",");
  Serial.println(msgSize);
  delay(50);
  if (newline)
  {
    Serial.println(value);
  }
  else
  {
    Serial.print(value);
  }
  delay(msgSize * responseDelay);
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
    //Serial.flush?
  }
  return content;
}

void processCommand(String cmd)
{
  String id = "0";
  if (cmd.length() > 0)
  {

    // **************************************************************************************************QUERY CONNECTION**************************************************************************************** \\ 
    if (cmd.indexOf(",CONNECT") >= 0)
    {
      id = cmd.substring(cmd.indexOf(",CONNECT") - 1, cmd.indexOf(",CONNECT"));
      int idIndex = id.toInt(); //need validation here
      if (idIndex < 5)
      {
        connectionStatus[idIndex] = CONNCTD;
        //DEBUG
        //streamWrite("0", "CONNECTION OPENED, ID: " + id, true);
      }
      else
      {
        //DEBUG
        //streamWrite("0", "ERROR INDEXING ID!", true);
      }
    }

    if (cmd.indexOf(",CLOSED") >= 0)
    {
      id = cmd.substring(cmd.indexOf(",CLOSED") - 1, cmd.indexOf(",CLOSED"));
      int idIndex = id.toInt();
      if (idIndex < 5)
      {
        connectionStatus[idIndex] = DSCNCTD;
        //DEBUG
        //streamWrite("0", "CONNECTION CLOSED, ID: " + id, true);
      }
      else
      {
        //DEBUG
        //streamWrite("0", "ERROR INDEXING ID!", true);
      }
    }

    // *********************************************************************************************************COMMANDS******************************************************************************************** \\ 
    if (cmd.indexOf("+IPD,") >= 0)
    {
      //Get sender ID:
      id = cmd.substring(cmd.indexOf("+IPD,") + 5, cmd.indexOf("+IPD,") + 6);
      //DEBUG
      //streamWrite("0", "DATA RECEIVED FROM ID: " + id, true);
      //Convert string id to int:
      int idIndex = id.toInt();

      //Look up string for the following possible commands:

      //--------------------------------------------------------------QUERY AP IP---------------------------------------------------------------\\
      if (cmd.indexOf("GIPAP") >= 0)
      {
        apIp = getIpAp();
        streamWrite(id, "APIP:" + apIp, true);
        //Serial.println("AP: " + apIp);
      }
      //------------------------------------------------------------------END-------------------------------------------------------------------\\   

      //-------------------------------------------------------------QUERY STA IP---------------------------------------------------------------\\
      if (cmd.indexOf("GIPSTA") >= 0)
      {
        staIp = getIpSta();
        streamWrite(id, "STAIP:(" + staIp + ")", true);
        //Serial.println("STA: " + staIp);
      }
      //------------------------------------------------------------------END-------------------------------------------------------------------\\   

      //-------------------------------------------------------------TOGGLE MESSAGES------------------------------------------------------------\\
      if (cmd.indexOf("MSGS") >= 0)
      {
        messages[idIndex] = !messages[idIndex];
        if (messages[idIndex])
        {
          streamWrite(id, "MESSAGES SHOWING", true);
        }
        else
        {
          streamWrite(id, "MESSAGES HIDDEN", true);
        }
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------SEND AT CMD------------------------------------------------------------------\\
      if ((cmd.indexOf("ATSEND") >= 0) && (cmd.indexOf("<") >= 0) && (cmd.indexOf(">") >= 0) && (cmd.indexOf("[") >= 0) && (cmd.indexOf("]") >= 0))
      {
        String atCmd = cmd.substring(cmd.indexOf('<') + 1, cmd.indexOf('>'));
        String atdelay = cmd.substring(cmd.indexOf('[') + 1, cmd.indexOf(']'));

        atCmd = "AT+" + atCmd;
        Serial.println(atCmd);
        delay(atdelay.toInt());
        
        //sendAtCommand(atCmd, atdelay.toInt());
        sendInfoMessage(id, "Sent AT command: " + atCmd, true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //-------------------------------------------------------------JOIN NETWORK----------------------------------------------------------------\\
      if ((cmd.indexOf("NJN") >= 0) && (cmd.indexOf("<") >= 0) && (cmd.indexOf(">") >= 0) && (cmd.indexOf("[") >= 0) && (cmd.indexOf("]") >= 0))
      {
        String ssid = cmd.substring(cmd.indexOf('<') + 1, cmd.indexOf('>'));
        String pwd = cmd.substring(cmd.indexOf('[') + 1, cmd.indexOf(']'));
        joinNetwork(ssid, pwd);
        delay(networkJoinDelay);        
        streamRead();
        Serial.flush();
        //streamRead();
        delay(100);
        staIp = getIpSta();
        streamWrite(id, "STAIP:(" + staIp + ")", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------RESPONSE DELAY---------------------------------------------------------------\\
      if ( (cmd.indexOf("RESDLY") >= 0) && (cmd.indexOf("<") >= 0) && (cmd.indexOf(">") >= 0) )
      {
        String delaySize = cmd.substring(cmd.indexOf('<') + 1, cmd.indexOf('>'));
        responseDelay = delaySize.toInt();
        sendInfoMessage(id, "Set response delay multiplier to: " + delaySize, true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\    

      //------------------------------------------------------------NET JOIN DELAY---------------------------------------------------------------\\
      if ( (cmd.indexOf("JNDLY") >= 0) && (cmd.indexOf("<") >= 0) && (cmd.indexOf(">") >= 0) )
      {
        String delaySize = cmd.substring(cmd.indexOf('<') + 1, cmd.indexOf('>'));
        networkJoinDelay = delaySize.toInt();
        sendInfoMessage(id, "Set network join delay to: " + delaySize, true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\  

      //------------------------------------------------------------SAFE DISCONNECT--------------------------------------------------------------\\
      if ((idIndex >= 0) && (idIndex <= 4))
      {
        if (cmd.indexOf("OSD") >= 0)
        {
          safeDisconnect[idIndex] = false;
          sendInfoMessage(id, "Safe disconnect deactivated", true);
        }
        if (cmd.indexOf("ISD") >= 0)
        {
          safeDisconnect[idIndex] = true;
          sendInfoMessage(id, "Safe disconnect activated", true);
        }
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------RELAY 1 CONTROL--------------------------------------------------------------\\
      if (cmd.indexOf("OR1") >= 0)
      {
        digitalWrite(R1, LOW);
        bitWrite(relayByte, 0, 0);
        sendInfoMessage(id, "Relay 1 deactivated", true);
      }
      if (cmd.indexOf("IR1") >= 0)
      {
        digitalWrite(R1, HIGH);
        bitWrite(relayByte, 0, 1);
        sendInfoMessage(id, "Relay 1 activated", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------RELAY 2 CONTROL--------------------------------------------------------------\\
      if (cmd.indexOf("OR2") >= 0)
      {
        digitalWrite(R2, LOW);
        bitWrite(relayByte, 1, 0);
        sendInfoMessage(id, "Relay 2 deactivated", true);
      }
      if (cmd.indexOf("IR2") >= 0)
      {
        digitalWrite(R2, HIGH);
        bitWrite(relayByte, 1, 1);
        sendInfoMessage(id, "Relay 2 activated", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------RELAY 3 CONTROL--------------------------------------------------------------\\
      if (cmd.indexOf("OR3") >= 0)
      {
        digitalWrite(R3, LOW);
        bitWrite(relayByte, 2, 0);
        sendInfoMessage(id, "Relay 3 deactivated", true);
      }
      if (cmd.indexOf("IR3") >= 0)
      {
        digitalWrite(R3, HIGH);
        bitWrite(relayByte, 2, 1);
        sendInfoMessage(id, "Relay 3 activated", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------RELAY 4 CONTROL--------------------------------------------------------------\\
      if (cmd.indexOf("OR4") >= 0)
      {
        digitalWrite(R4, LOW);
        bitWrite(relayByte, 3, 0);
        sendInfoMessage(id, "Relay 4 deactivated", true);
      }
      if (cmd.indexOf("IR4") >= 0)
      {
        digitalWrite(R4, HIGH);
        bitWrite(relayByte, 3, 1);
        sendInfoMessage(id, "Relay 4 activated", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------RELAY 5 CONTROL--------------------------------------------------------------\\
      if (cmd.indexOf("OR5") >= 0)
      {
        digitalWrite(R5, LOW);
        bitWrite(relayByte, 4, 0);
        sendInfoMessage(id, "Relay 5 deactivated", true);
      }
      if (cmd.indexOf("IR5") >= 0)
      {
        digitalWrite(R5, HIGH);
        bitWrite(relayByte, 4, 1);
        sendInfoMessage(id, "Relay 5 activated", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------RELAY 6 CONTROL--------------------------------------------------------------\\
      if (cmd.indexOf("OR6") >= 0)
      {
        digitalWrite(R6, LOW);
        bitWrite(relayByte, 5, 0);
        sendInfoMessage(id, "Relay 6 deactivated", true);
      }
      if (cmd.indexOf("IR6") >= 0)
      {
        digitalWrite(R6, HIGH);
        bitWrite(relayByte, 5, 1);
        sendInfoMessage(id, "Relay 6 activated", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------RELAY 7 CONTROL--------------------------------------------------------------\\
      if (cmd.indexOf("OR7") >= 0)
      {
        digitalWrite(R7, LOW);
        bitWrite(relayByte, 6, 0);
        sendInfoMessage(id, "Relay 7 deactivated", true);
      }
      if (cmd.indexOf("IR7") >= 0)
      {
        digitalWrite(R7, HIGH);
        bitWrite(relayByte, 6, 1);
        sendInfoMessage(id, "Relay 7 activated", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------RELAY 8 CONTROL--------------------------------------------------------------\\
      if (cmd.indexOf("OR8") >= 0)
      {
        digitalWrite(R8, LOW);
        bitWrite(relayByte, 7, 0);
        sendInfoMessage(id, "Relay 8 deactivated", true);
      }
      if (cmd.indexOf("IR8") >= 0)
      {
        digitalWrite(R8, HIGH);
        bitWrite(relayByte, 7, 1);
        sendInfoMessage(id, "Relay 8 activated", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //----------------------------------------------------------ALL RELAY CONTROL--------------------------------------------------------------\\
      if (cmd.indexOf("OAR") >= 0)
      {
        setAllRelays(OFF);
        sendInfoMessage(id, "All relays deactivated", true);
      }
      if (cmd.indexOf("IAR") >= 0)
      {
        setAllRelays(ON);
        sendInfoMessage(id, "All relays activated", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //-----------------------------------------------------------PRINT RELAY BYTE--------------------------------------------------------------\\
      if (cmd.indexOf("GRB") >= 0)
      {
        streamWrite(id, String(relayByte), true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //----------------------------------------------------------PRINT INPUTS BYTE--------------------------------------------------------------\\
      if (cmd.indexOf("GIB") >= 0)
      {
        streamWrite(id, String(getInputs()), true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //----------------------------------------------------------PRINT INTERUPT BYTE------------------------------------------------------------\\
      if (cmd.indexOf("INRPT") >= 0)
      {
        streamWrite(id, String(getInterupts()), true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------------PRINT ALL BYTES--------------------------------------------------------------\\
      if (cmd.indexOf("GFB") >= 0)
      {
        streamWrite(id, "FBB:(" + String(getInputs()) + ":" + String(relayByte) + ":" + String(getInterupts()) + ":)", true);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

      //------------------------------------------------------RECONFIGURE NETWORK PARAMS---------------------------------------------------------\\
      if ((cmd.indexOf("CNET") >= 0) && (cmd.indexOf("<") >= 0) && (cmd.indexOf(">") >= 0) && (cmd.indexOf("{") >= 0) && (cmd.indexOf("}") >= 0))
      {
        String netname = cmd.substring(cmd.indexOf('<') + 1, cmd.indexOf('>'));
        String netpass = cmd.substring(cmd.indexOf('{') + 1, cmd.indexOf('}'));
        if (configureNetwork(netname, netpass, "1", "3"))
        {
          sendInfoMessage(id, "Network configuration successfully changed", true);
          setupModule();
        }
        else
        {
          sendInfoMessage(id, "Error in network configuration change!", true);
        }
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\   

      //-----------------------------------------------------------CLOSE CONNECTION--------------------------------------------------------------\\
      if (cmd.indexOf("|CC|") >= 0)
      {
        sendInfoMessage(id, "Closing connection..", true);
        closeConnection(id, 100);
      }
      //------------------------------------------------------------------END--------------------------------------------------------------------\\     

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
  delay(500);
  resetNetwork();
  delay(1500);
  //setNetworkMode("3");
  multipleConnections(true);
  setServer("1", "5050");
}

boolean setServer(String state, String port)
{
  return sendAtCommand("CIPSERVER=" + state + "," + port, 150);
}

boolean multipleConnections(boolean condition)
{
  if (condition)
  {
    return sendAtCommand("CIPMUX=1", 150);
  }
  else
  {
    return sendAtCommand("CIPMUX=0", 150);
  }
}

boolean resetNetwork()
{
  return sendAtCommand("RST", 2000);
}

boolean sendAtCommand(String cmd, int delayTime)
{
  String atCmd = "AT+" + cmd;
  Serial.println(atCmd);
  delay(delayTime);
  return getResponse();
}

boolean setNetworkMode(String mode)
{
  return sendAtCommand("CWMODE=" + mode, 100);
}

String getIpAp()
{
  //sendAtCommand("CIFSR", 250);
  Serial.println("AT+CIFSR");

  //debug
  delay(3000);

  String dataBuffer = streamRead();
  String ip;

  if (dataBuffer.indexOf("APIP,") >= 0)
  {
    ip = dataBuffer.substring( dataBuffer.indexOf("APIP,") + 6,  dataBuffer.indexOf("APIP,") + 22 );
    ip = ip.substring( 0,   ip.indexOf('\"', 3) );
    //Serial.println("WEB: " + webIp);
    //id = cmd.substring(cmd.indexOf("+IPD,") + 5, cmd.indexOf("+IPD,") + 6);
  }
  return ip;
}

String getIpSta()
{
  //sendAtCommand("CIFSR", 250);
  Serial.println("AT+CIFSR");

  //debug
  delay(2000);

  String dataBuffer = streamRead();
  String ip;

  if (dataBuffer.indexOf("STAIP,") >= 0)
  {
    ip = dataBuffer.substring( dataBuffer.indexOf("STAIP,") + 7,  dataBuffer.indexOf("STAIP,") + 23 );
    ip = ip.substring( 0,   ip.indexOf('\"', 3) );
    //Serial.println("STA: " + staticIp);
  }
  return ip;
}

boolean queryConnections()
{
  return sendAtCommand("CIPSTATUS", 100);
}

boolean closeConnection(String connectionId, int delayTime)
{
  delay(delayTime);
  return sendAtCommand("CIPCLOSE=" + connectionId, 100);
}

boolean configureNetwork(String ssid, String password, String channel, String protection)
{
  String quote = "\"";
  ssid = quote + ssid + quote;
  password = quote + password + quote;
  String cmd = "CWSAP=" + ssid + "," + password + "," + channel + "," + protection;
  return sendAtCommand(cmd, 100);
}

void joinNetwork(String ssid, String password)
{
  String quote = "\"";
  ssid = quote + ssid + quote;
  password = quote + password + quote;
  String cmd = "AT+CWJAP=" + ssid + "," + password;
  Serial.println(cmd);
}
