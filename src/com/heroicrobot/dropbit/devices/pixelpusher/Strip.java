package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.Arrays;

public class Strip {

  private Pixel[] pixels;
  private PixelPusher pusher;
  /**
   * @return the pusher
   */
  public PixelPusher getPusher() {
    return pusher;
  }

  private long pushedAt;
  private int stripNumber;
  private boolean touched;
  private double powerScale;
  private boolean isRGBOW;
  private byte[] msg;
  private boolean useAntiLog;
  private boolean isMotion;
  private boolean isNotIdempotent;
  private boolean hasBrightness;
  private boolean isMonochrome;

  static final byte sLinearExp[] = { (byte) 0,(byte) 0,(byte) 0,(byte) 0,(byte) 0,(byte) 0,(byte) 1,(byte) 1,(byte) 1,(byte) 1,
    (byte) 1,(byte) 2,(byte) 2,(byte) 2,(byte) 2,(byte) 2,(byte) 3,(byte) 3,(byte) 3,(byte) 3,(byte) 4,(byte) 4,
    (byte) 4,(byte) 4,(byte) 5,(byte) 5,(byte) 5,(byte) 5,(byte) 6,(byte) 6,(byte) 6,(byte) 6,(byte) 7,(byte) 7,
    (byte) 7,(byte) 7,(byte) 8,(byte) 8,(byte) 8,(byte) 8,(byte) 9,(byte) 9,(byte) 9,(byte) 10,(byte) 10,(byte) 10,
    (byte) 10,(byte) 11,(byte) 11,(byte) 11,(byte) 12,(byte) 12,(byte) 12,(byte) 13,(byte) 13,(byte) 13,(byte) 14,
    (byte) 14,(byte) 14,(byte) 15,(byte) 15,(byte) 15,(byte) 16,(byte) 16,(byte) 17,(byte) 17,(byte) 17,(byte) 18,
    (byte) 18,(byte) 18,(byte) 19,(byte) 19,(byte) 20,(byte) 20,(byte) 20,(byte) 21,(byte) 21,(byte) 22,(byte) 22,
    (byte) 22,(byte) 23,(byte) 23,(byte) 24,(byte) 24,(byte) 25,(byte) 25,(byte) 26,(byte) 26,(byte) 27,(byte) 27,
    (byte) 28,(byte) 28,(byte) 29,(byte) 29,(byte) 30,(byte) 30,(byte) 31,(byte) 31,(byte) 32,(byte) 32,(byte) 33,
    (byte) 33,(byte) 34,(byte) 34,(byte) 35,(byte) 36,(byte) 36,(byte) 37,(byte) 37,(byte) 38,(byte) 38,(byte) 39,
    (byte) 40,(byte) 40,(byte) 41,(byte) 42,(byte) 42,(byte) 43,(byte) 44,(byte) 44,(byte) 45,(byte) 46,(byte) 46,
    (byte) 47,(byte) 48,(byte) 48,(byte) 49,(byte) 50,(byte) 51,(byte) 51,(byte) 52,(byte) 53,(byte) 54,(byte) 54,
    (byte) 55,(byte) 56,(byte) 57,(byte) 57,(byte) 58,(byte) 59,(byte) 60,(byte) 61,(byte) 62,(byte) 62,(byte) 63,
    (byte) 64,(byte) 65,(byte) 66,(byte) 67,(byte) 68,(byte) 69,(byte) 70,(byte) 71,(byte) 72,(byte) 73,(byte) 74,
    (byte) 75,(byte) 76,(byte) 77,(byte) 78,(byte) 79,(byte) 80,(byte) 81,(byte) 82,(byte) 83,(byte) 84,(byte) 85,
    (byte) 86,(byte) 87,(byte) 89,(byte) 90,(byte) 91,(byte) 92,(byte) 93,(byte) 94,(byte) 96,(byte) 97,(byte) 98,
    (byte) 99,(byte) 101,(byte) 102,(byte) 103,(byte) 105,(byte) 106,(byte) 107,(byte) 109,(byte) 110,(byte) 111,
    (byte) 113,(byte) 114,(byte) 116,(byte) 117,(byte) 119,(byte) 120,(byte) 121,(byte) 123,(byte) 125,(byte) 126,
    (byte) 128,(byte) 129,(byte) 131,(byte) 132,(byte) 134,(byte) 136,(byte) 137,(byte) 139,(byte) 141,(byte) 142,
    (byte) 144,(byte) 146,(byte) 148,(byte) 150,(byte) 151,(byte) 153,(byte) 155,(byte) 157,(byte) 159,(byte) 161,
    (byte) 163,(byte) 165,(byte) 167,(byte) 169,(byte) 171,(byte) 173,(byte) 175,(byte) 177,(byte) 179,(byte) 181,
    (byte) 183,(byte) 186,(byte) 188,(byte) 190,(byte) 192,(byte) 195,(byte) 197,(byte) 199,(byte) 202,(byte) 204,
    (byte) 206,(byte) 209,(byte) 211,(byte) 214,(byte) 216,(byte) 219,(byte) 221,(byte) 224,(byte) 227,(byte) 229,
    (byte) 232,(byte) 235,(byte) 237,(byte) 240,(byte) 243,(byte) 246,(byte) 249,(byte) 252 };
    
  /*  
    
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4,
    5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12,
    13, 13, 13, 14, 14, 14, 14, 15, 15, 16, 16, 16, 17, 17, 17, 18, 18, 19, 19, 20, 20, 20, 21, 21, 22, 22, 23, 23, 24, 25, 25, 26, 26, 27,
    27, 28, 29, 29, 30, 31, 31, 32, 33, 34, 34, 35, 36, 37, 38, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 54, 55, 56, 57,
    59, 60, 61, 63, 64, 65, 67, 68, 70, 72, 73, 75, 76, 78, 80, 82, 83, 85, 87, 89, 91, 93, 95, 97, 99, 102, 104, 106, 109, 111, 114, 116,
    119, 121, 124, 127, (byte)129, (byte)132, (byte)135, (byte)138, (byte)141, (byte)144, (byte)148, (byte)151, (byte)154, (byte)158,
    (byte)161, (byte)165, (byte)168, (byte)172, (byte)176, (byte)180, (byte)184, (byte)188, (byte)192, (byte)196, (byte)201, (byte)205,
    (byte)209, (byte)214, (byte)219, (byte)224, (byte)229, (byte)234, (byte)239, (byte)244, (byte)249, (byte)255 };
*/
  public Strip(PixelPusher pusher, int stripNumber, int length, boolean antiLog) {
    this.pixels = new Pixel[length];
    for (int i = 0; i < this.pixels.length; i++) {
      this.pixels[i] = new Pixel();
    }
    this.pusher = pusher;
    this.stripNumber = stripNumber;
    this.touched = false;
    this.powerScale = 1.0;
    this.isRGBOW = false;
    this.useAntiLog = antiLog;
    this.msg = new byte[pixels.length * 3];
  }

  public Strip(PixelPusher pusher, int stripNumber, int length) {
    this.pixels = new Pixel[length];
    for (int i = 0; i < this.pixels.length; i++) {
      this.pixels[i] = new Pixel();
    }
    this.pusher = pusher;
    this.stripNumber = stripNumber;
    this.touched = false;
    this.powerScale = 1.0;
    this.isRGBOW = false;
    this.useAntiLog = false;
    this.isMonochrome = false;
    this.msg = new byte[pixels.length * 3];
  }

  // get the RGBOW state of the strip.
  public boolean getRGBOW() {
    return isRGBOW;
  }

  // set the RGBOW state of the strip;  this function is idempotent.
  public void setRGBOW(boolean state) {
    if (state == isRGBOW)
        return;
    this.touched = true;
    pusher.markTouched();
    int length = pixels.length;
    if (isRGBOW) {  // if we're already set to RGBOW mode
      this.pixels = new Pixel[length*3];   // else go back to RGB mode
      for (int i = 0; i < this.pixels.length; i++) {
        this.pixels[i] = new Pixel();
      }
      this.msg = new byte[pixels.length*3];
      return;
    }
    // otherwise, we were in RGB mode.
    if (state) { // if we are going to RGBOW mode
      this.pixels = new Pixel[(int)length/3];  // shorten the pixel array
      for (int i = 0; i < this.pixels.length; i++) {
        this.pixels[i] = new Pixel();
      }
      this.msg = new byte[pixels.length * 9];  // but lengthen the serialization buffer.
      isRGBOW = state;
      return;
    }
    // otherwise, do nothing.
  }

  public void setIsMonochrome(boolean b) {
    if (b != this.isMonochrome) {
      // we are either switching to monochrome or away from it
      if (b) {
        // we are switching to it
        int length = pixels.length;
        this.pixels = new Pixel[length * 3];
        for (int i = 0; i < this.pixels.length; i++) {
          this.pixels[i] = new Pixel();
        }
        this.msg = new byte[pixels.length]; // one byte per pixel for monochrome.
        this.isMonochrome = b;
        pusher.markTouched();
        return;
      }
      // otherwise, we are actually switching away from it.
      int length = pixels.length;
      this.pixels = new Pixel[length / 3];
      for (int i = 0; i < this.pixels.length; i++) {
        this.pixels[i] = new Pixel();
      }
      this.msg = new byte[pixels.length * 3]; // RGB
      this.isMonochrome = b;
      pusher.markTouched();
      return;
    }
  }
  
  public int getLength() {
      return pixels.length;
  }

  public void setPowerScale(double scale)
  {
    this.powerScale = scale;
  }

  public String getMacAddress() {
    return this.pusher.getMacAddress();
  }

  public synchronized boolean isTouched() {
    return this.touched;
  }

  public synchronized void markClean() {
    this.touched = false;
    pusher.markUntouched();
  }

  public int getStripNumber() {
    return stripNumber;
  }

  public long getStripIdentifier() {
    // Return a compact reversible identifier
    return -1;
  }

  public synchronized void setPixels(Pixel[] pixels) {
    this.pixels = Arrays.copyOfRange(pixels, 0, this.pixels.length);
    this.touched = true;
    pushedAt = 0;
    pusher.markTouched();
  }

  public synchronized void setPixelRed(byte intensity, int position) {
    if (position >= this.pixels.length)
      return;
    try {
      if (useAntiLog) {
        this.pixels[position].red = sLinearExp[(int)intensity];
      } else
        this.pixels[position].red = intensity;
    } catch (NullPointerException nope) {
      System.err.println("Tried to write to pixel "+position+" but it wasn't there.");
      nope.printStackTrace();
    }
    this.touched = true;
    pushedAt = 0;
    pusher.markTouched();
  }

  public synchronized void setPixelBlue(byte intensity, int position) {
    if (position >= this.pixels.length)
      return;
    try {
      if (useAntiLog) {
        this.pixels[position].blue = sLinearExp[(int)intensity];
      } else
      this.pixels[position].blue = intensity;
    } catch (NullPointerException nope) {
      System.err.println("Tried to write to pixel "+position+" but it wasn't there.");
      nope.printStackTrace();
    }
    this.touched = true;
    pushedAt = 0;
    pusher.markTouched();
  }

  public synchronized void setPixelGreen(byte intensity, int position) {
    if (position >= this.pixels.length)
      return;
    try {
      if (useAntiLog) {
        this.pixels[position].green = sLinearExp[(int)intensity];
      } else
      this.pixels[position].green = intensity;
    } catch (NullPointerException nope) {
      System.err.println("Tried to write to pixel "+position+" but it wasn't there.");
      nope.printStackTrace();
    }
    this.touched = true;
    pushedAt = 0;
    pusher.markTouched();
  }

  public synchronized void setPixelOrange(byte intensity, int position) {
    if (position >= this.pixels.length)
      return;
    try {
      if (useAntiLog) {
        this.pixels[position].orange = sLinearExp[(int)intensity];
      } else
      this.pixels[position].orange = intensity;
    } catch (NullPointerException nope) {
      System.err.println("Tried to write to pixel "+position+" but it wasn't there.");
      nope.printStackTrace();
    }
    this.touched = true;
    pushedAt = 0;
    pusher.markTouched();
  }

public synchronized void setPixelWhite(byte intensity, int position) {
    if (position >= this.pixels.length)
      return;
    try {
      if (useAntiLog) {
        this.pixels[position].white = sLinearExp[(int)intensity];
      } else
      this.pixels[position].white = intensity;
    } catch (NullPointerException nope) {
      System.err.println("Tried to write to pixel "+position+" but it wasn't there.");
      nope.printStackTrace();
    }
    this.touched = true;
    pushedAt = 0;
    pusher.markTouched();
  }

  public synchronized void setPixel(int color, int position) {
    if (position >= this.pixels.length)
      return;
    try {
      if (useAntiLog)
        this.pixels[position].setColorAntilog(color);
      else
        this.pixels[position].setColor(color);
    } catch (NullPointerException nope) {
      System.err.println("Tried to write to pixel "+position+" but it wasn't there.");
      nope.printStackTrace();
    }
    this.touched = true;
    pushedAt = 0;
    pusher.markTouched();
  }

  public synchronized void setPixel(Pixel pixel, int position) {
    if (useAntiLog)
      this.pixels[position].setColor(pixel, true);
    else
      this.pixels[position].setColor(pixel);
    this.touched = true;
    pushedAt = 0;
    pusher.markTouched();
  }

  public byte[] serialize() {
    int i = 0;
    if (isRGBOW) {
      for (Pixel pixel : pixels) {
        if (pixel == null)
          pixel = new Pixel();
        msg[i++] = (byte) (((double)(pixel.red & 0xff))   * powerScale);    // C
        msg[i++] = (byte) (((double)(pixel.green & 0xff)) * powerScale);
        msg[i++] = (byte) (((double)(pixel.blue & 0xff))  * powerScale);

        msg[i++] = (byte) (((double)(pixel.orange & 0xff)) * powerScale);   // O
        msg[i++] = (byte) (((double)(pixel.orange & 0xff)) * powerScale);
        msg[i++] = (byte) (((double)(pixel.orange & 0xff)) * powerScale);

        msg[i++] = (byte) (((double)(pixel.white & 0xff)) * powerScale);    // W
        msg[i++] = (byte) (((double)(pixel.white & 0xff)) * powerScale);
        msg[i++] = (byte) (((double)(pixel.white & 0xff)) * powerScale);
      }
    } else {
      for (Pixel pixel : pixels) {
        if (pixel == null)
          pixel = new Pixel();
        msg[i++] = (byte) ((double)((pixel.red & 0xff)) * powerScale);
        msg[i++] = (byte) ((double)((pixel.green & 0xff)) * powerScale);
        msg[i++] = (byte) ((double)((pixel.blue & 0xff)) * powerScale);
      }
    }
    return msg;
  }

  public void useAntiLog(boolean antiLog) {
    useAntiLog = antiLog;
  }

  public void setPusher(PixelPusher pixelPusher) {
    this.pusher = pixelPusher;
  }

  public byte[] serialize(double overallBrightnessScale) {
    int i = 0;
    if (isRGBOW) {
      for (Pixel pixel : pixels) {
        if (pixel == null)
          pixel = new Pixel();
        msg[i++] = (byte) ((double)(pixel.red & 0xff)  * powerScale * overallBrightnessScale);    // C
        msg[i++] = (byte) ((double)(pixel.green & 0xff) * powerScale * overallBrightnessScale);
        msg[i++] = (byte) ((double)(pixel.blue & 0xff)  * powerScale * overallBrightnessScale);

        msg[i++] = (byte) ((double)(pixel.orange & 0xff) * powerScale * overallBrightnessScale);   // O
        msg[i++] = (byte) ((double)(pixel.orange & 0xff) * powerScale * overallBrightnessScale);
        msg[i++] = (byte) ((double)(pixel.orange & 0xff) * powerScale * overallBrightnessScale);

        msg[i++] = (byte) ((double)(pixel.white & 0xff) * powerScale * overallBrightnessScale);    // W
        msg[i++] = (byte) ((double)(pixel.white & 0xff) * powerScale * overallBrightnessScale);
        msg[i++] = (byte) ((double)(pixel.white & 0xff) * powerScale * overallBrightnessScale);
      }
    } else {
      if (!isMonochrome) {
        for (Pixel pixel : pixels) {
          if (pixel == null)
            pixel = new Pixel();
          msg[i++] = (byte) ((double)(pixel.red & 0xff) * powerScale * overallBrightnessScale);
          msg[i++] = (byte) ((double)(pixel.green & 0xff) * powerScale * overallBrightnessScale);
          msg[i++] = (byte) ((double)(pixel.blue & 0xff) * powerScale * overallBrightnessScale);
        } 
      } else {
        // isMonochrome
        for (Pixel pixel : pixels) {
          if (pixel == null)
            pixel = new Pixel();
          msg[i++] = (byte) ((double)(pixel.red & 0xff) * powerScale * overallBrightnessScale);
        }
      }
    }
    return msg;
  }

  public boolean isMotion() {
    return isMotion;
  }

  public void setMotion(boolean isMotion) {
    this.isMotion = isMotion;
  }

  public boolean isNotIdempotent() {
    return isNotIdempotent;
  }

  public void setNotIdempotent(boolean isNotIdempotent) {
    this.isNotIdempotent = isNotIdempotent;
  }

  public long getPushedAt() {
    return pushedAt;
  }

  public void setPushedAt(long pushedAt) {
    this.pushedAt = pushedAt;
  }

  public boolean hasBrightness() {
    return this.hasBrightness;
  }
  public void setHasBrightness(boolean b) {
    this.hasBrightness = b;
  }
}
