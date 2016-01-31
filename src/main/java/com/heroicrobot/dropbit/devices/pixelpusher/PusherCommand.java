package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.Arrays;

public class PusherCommand {


 /*  const uint8_t pp_command_magic[16] = 
  *   { 0x40, 0x09, 0x2d, 0xa6, 0x15, 0xa5, 0xdd, 0xe5, 0x6a, 0x9d, 0x4d, 0x5a, 0xcf, 0x09, 0xaf, 0x50  };
  *
  * #define COMMAND_RESET                0x01
  * #define COMMAND_GLOBALBRIGHTNESS_SET 0x02
  * #define COMMAND_WIFI_CONFIGURE       0x03
  * #define COMMAND_LED_CONFIGURE        0x04
  * #define COMMAND_STRIPBRIGHTNESS_SET  0x05
  */
  
  private static final byte pp_command_magic[] = 
    { (byte) 0x40, (byte) 0x09, (byte) 0x2d, (byte) 0xa6, (byte) 0x15, (byte) 0xa5, (byte) 0xdd, (byte) 0xe5,
      (byte) 0x6a, (byte) 0x9d, (byte)0x4d, (byte)0x5a, (byte) 0xcf, (byte) 0x09,(byte) 0xaf,(byte) 0x50  };

  public static final byte RESET = 0x01;
  public static final byte GLOBALBRIGHTNESS_SET = 0x02;
  public static final byte WIFI_CONFIGURE = 0x03;
  public static final byte LED_CONFIGURE = 0x04;
  public static final byte STRIPBRIGHTNESS_SET = 0x05;
  
  public static final byte STRIP_LPD8806 = 0;
  public static final byte STRIP_WS2801 = 1;
  public static final byte STRIP_WS2811 = 2;
  public static final byte STRIP_APA102 = 3;
  
  public byte command;
  private byte stripNumber;
  private short parameter;
  private String ssid;
  private String key;
  private byte  security;
  
  private int num_strips;
  private int strip_length;
  private byte[] strip_type;
  private byte[] colour_order;

  private short group;
  private short controller;
  
  private short artnet_universe;
  private short artnet_channel;
  
/*  enum Security {
    NONE = 0,
    WEP  = 1,
    WPA  = 2,
    WPA2 = 3
 };
 
 typedef enum ColourOrder {RGB=0, RBG=1, GBR=2, GRB=3, BGR=4, BRG=5} ColourOrder;
 
  */
  
  public static final byte ORDER_RGB = 0;
  public static final byte ORDER_RBG = 1;
  public static final byte ORDER_GBR = 2;
  public static final byte ORDER_GRB = 3;
  public static final byte ORDER_BGR = 4;
  public static final byte ORDER_BRG = 5;
  
  public PusherCommand(byte command) {
    this.command = command;
  }
  
  public PusherCommand(byte command, short parameter) {
    this.command = command;
    this.parameter = parameter;
  }
  
  public PusherCommand(byte command, byte stripNumber, short parameter) {
     this.command = command;
     this.stripNumber = stripNumber;
     this.parameter = parameter;
  }
  
  public PusherCommand(byte command, String ssid, String key, String security) {
    this.command = command;
    this.ssid = ssid;
    this.key = key;
    if (security.toLowerCase().compareTo("none") == 0) 
      this.security = 0;
    if (security.toLowerCase().compareTo("wep") == 0) 
      this.security = 1;
    if (security.toLowerCase().compareTo("wpa") == 0) 
      this.security = 2;
    if (security.toLowerCase().compareTo("wpa2") == 0) 
      this.security = 3;
  }
  
  public PusherCommand(byte command, int numStrips, int stripLength, byte[] stripType, byte[] colourOrder) {
    this.command = command;
    this.num_strips = numStrips;
    this.strip_length = stripLength;
    this.strip_type = Arrays.copyOf(stripType, 8);
    this.colour_order = Arrays.copyOf(colourOrder, 8);
    this.group = 0;
    this.controller = 0;
    this.artnet_channel = 0;
    this.artnet_universe = 0;
  }
  
  public PusherCommand(byte command, int numStrips, int stripLength, byte[] stripType, byte[] colourOrder, short group, short controller) {
    this.command = command;
    this.num_strips = numStrips;
    this.strip_length = stripLength;
    this.strip_type = Arrays.copyOf(stripType, 8);
    this.colour_order = Arrays.copyOf(colourOrder, 8);
    this.group = group;
    this.controller = controller;
    this.artnet_channel = 0;
    this.artnet_universe = 0;
  }
  
  public PusherCommand(byte command, int numStrips, int stripLength, byte[] stripType, byte[] colourOrder, short group, short controller, 
                       short artnet_universe, short artnet_channel) {
    this.command = command;
    this.num_strips = numStrips;
    this.strip_length = stripLength;
    this.strip_type = Arrays.copyOf(stripType, 8);
    this.colour_order = Arrays.copyOf(colourOrder, 8);
    this.group = group;
    this.controller = controller;
    this.artnet_channel = artnet_channel;
    this.artnet_universe = artnet_universe;
  }
  
  public byte [] generateBytes() {
    byte[] returnVal= null;
    if (command == RESET) {
      returnVal = Arrays.copyOf(pp_command_magic, pp_command_magic.length+1);
      returnVal[pp_command_magic.length] = RESET;
    } else if (command == GLOBALBRIGHTNESS_SET) {
      returnVal = Arrays.copyOf(pp_command_magic, pp_command_magic.length+3);
      returnVal[pp_command_magic.length] = GLOBALBRIGHTNESS_SET;
      returnVal[pp_command_magic.length+1] = (byte) (parameter & 0xff);
      returnVal[pp_command_magic.length+2] = (byte) ((parameter>>8) & 0xff);
    } else if (command == STRIPBRIGHTNESS_SET) {
      returnVal = Arrays.copyOf(pp_command_magic, pp_command_magic.length+4);
      returnVal[pp_command_magic.length] = STRIPBRIGHTNESS_SET;
      returnVal[pp_command_magic.length+1] = (byte) (stripNumber & 0xff);
      returnVal[pp_command_magic.length+2] = (byte) (parameter & 0xff);
      returnVal[pp_command_magic.length+3] = (byte) ((parameter>>8) & 0xff);
    } else if (command == WIFI_CONFIGURE) {
      byte[] ssidBytes = ssid.getBytes();
      byte[] keyBytes = key.getBytes();
      int bufLength = 0;
      bufLength += (pp_command_magic.length) + 1; /* length of command */
      bufLength += 1; // length of key type
      bufLength += ssidBytes.length + 1; // ssid plus null terminator
      bufLength += keyBytes.length + 1;  // key plus null terminator
      
      returnVal = Arrays.copyOf(pp_command_magic, bufLength);
      
      returnVal[pp_command_magic.length] = command;
      
      for (int i=0; i<ssidBytes.length; i++ )
        returnVal[pp_command_magic.length+ 1 + i] 
            = ssidBytes[i];
      
      for (int i=0; i<keyBytes.length; i++ )
        returnVal[pp_command_magic.length+ 1 + ssidBytes.length + 1 + i] 
            = keyBytes[i];
      
      returnVal[pp_command_magic.length+ 1 + keyBytes.length + 1 + ssidBytes.length + 1] = security;
    } else if (command == LED_CONFIGURE) {
      returnVal = Arrays.copyOf(pp_command_magic, pp_command_magic.length+33); // two ints, eight bytes, eight bytes, plus command, plus group and controller
                                                                               // plus artnet universe and channel
      returnVal[pp_command_magic.length] = LED_CONFIGURE;

      returnVal[pp_command_magic.length+1+0] = (byte) (num_strips & 0xFF);   
      returnVal[pp_command_magic.length+1+1] = (byte) ((num_strips >> 8) & 0xFF);   
      returnVal[pp_command_magic.length+1+2] = (byte) ((num_strips >> 16) & 0xFF);   
      returnVal[pp_command_magic.length+1+3] = (byte) ((num_strips >> 24) & 0xFF);
      
      returnVal[pp_command_magic.length+5+0] = (byte) (strip_length & 0xFF);   
      returnVal[pp_command_magic.length+5+1] = (byte) ((strip_length >> 8) & 0xFF);   
      returnVal[pp_command_magic.length+5+2] = (byte) ((strip_length >> 16) & 0xFF);   
      returnVal[pp_command_magic.length+5+3] = (byte) ((strip_length >> 24) & 0xFF); 
      
      for (int i = pp_command_magic.length+9; i< pp_command_magic.length+17; i++)
        returnVal[i] = strip_type[i-(pp_command_magic.length+9)];
      for (int i = pp_command_magic.length+17; i< pp_command_magic.length+25; i++)
        returnVal[i] = colour_order[i-(pp_command_magic.length+17)];
      
      returnVal[pp_command_magic.length+25+0] = (byte) (group & 0xFF);   
      returnVal[pp_command_magic.length+25+1] = (byte) ((group >> 8) & 0xFF);
      
      returnVal[pp_command_magic.length+27+0] = (byte) (controller & 0xFF);   
      returnVal[pp_command_magic.length+27+1] = (byte) ((controller >> 8) & 0xFF);
      
      returnVal[pp_command_magic.length+29+0] = (byte) (artnet_universe & 0xFF);   
      returnVal[pp_command_magic.length+29+1] = (byte) ((artnet_universe >> 8) & 0xFF);
      
      returnVal[pp_command_magic.length+31+0] = (byte) (artnet_channel & 0xFF);   
      returnVal[pp_command_magic.length+31+1] = (byte) ((artnet_channel >> 8) & 0xFF);
    
      
    } // end if(command)
    return returnVal;
  }
}
