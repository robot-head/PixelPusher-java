package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.Arrays;

public class Strip {

  private Pixel[] pixels;
  private PixelPusher pusher;
  private int stripNumber;
  private boolean touched;
  private double powerScale;
  private boolean isRGBOW;
  private byte[] msg;
  private boolean useAntiLog;

  static final byte sLinearExp[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4,
    5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12,
    13, 13, 13, 14, 14, 14, 14, 15, 15, 16, 16, 16, 17, 17, 17, 18, 18, 19, 19, 20, 20, 20, 21, 21, 22, 22, 23, 23, 24, 25, 25, 26, 26, 27,
    27, 28, 29, 29, 30, 31, 31, 32, 33, 34, 34, 35, 36, 37, 38, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 54, 55, 56, 57,
    59, 60, 61, 63, 64, 65, 67, 68, 70, 72, 73, 75, 76, 78, 80, 82, 83, 85, 87, 89, 91, 93, 95, 97, 99, 102, 104, 106, 109, 111, 114, 116,
    119, 121, 124, 127, (byte)129, (byte)132, (byte)135, (byte)138, (byte)141, (byte)144, (byte)148, (byte)151, (byte)154, (byte)158,
    (byte)161, (byte)165, (byte)168, (byte)172, (byte)176, (byte)180, (byte)184, (byte)188, (byte)192, (byte)196, (byte)201, (byte)205,
    (byte)209, (byte)214, (byte)219, (byte)224, (byte)229, (byte)234, (byte)239, (byte)244, (byte)249, (byte)255 };

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
    pusher.markTouched();
  }

  public synchronized void setPixel(Pixel pixel, int position) {
    if (useAntiLog)
      this.pixels[position].setColor(pixel, true);
    else
      this.pixels[position].setColor(pixel);
    this.touched = true;
    pusher.markTouched();
  }

  public byte[] serialize() {
    int i = 0;
    if (isRGBOW) {
      for (Pixel pixel : pixels) {
        if (pixel == null)
          pixel = new Pixel();
          msg[i++] = (byte) (((double)pixel.red)   * powerScale);    // C
          msg[i++] = (byte) (((double)pixel.green) * powerScale);
          msg[i++] = (byte) (((double)pixel.blue)  * powerScale);

          msg[i++] = (byte) (((double)pixel.orange) * powerScale);   // O
          msg[i++] = (byte) (((double)pixel.orange) * powerScale);
          msg[i++] = (byte) (((double)pixel.orange) * powerScale);

          msg[i++] = (byte) (((double)pixel.white) * powerScale);    // W
          msg[i++] = (byte) (((double)pixel.white) * powerScale);
          msg[i++] = (byte) (((double)pixel.white) * powerScale);
      }
    } else {
      for (Pixel pixel : pixels) {
        if (pixel == null)
          pixel = new Pixel();
        msg[i++] = (byte) (((double)pixel.red) * powerScale);
        msg[i++] = (byte) (((double)pixel.green) * powerScale);
        msg[i++] = (byte) (((double)pixel.blue) * powerScale);
      }
    }
    return msg;
  }

  public void useAntiLog(boolean antiLog) {
    useAntiLog = antiLog;
  }
}
