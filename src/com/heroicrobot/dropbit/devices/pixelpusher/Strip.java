package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.Arrays;

public class Strip {

  private Pixel[] pixels;
  private PixelPusher pusher;
  private int stripNumber;
  private boolean touched;
  private double powerScale;
  
  public Strip(PixelPusher pusher, int stripNumber, int length) {
    this.pixels = new Pixel[length];
    for (int i = 0; i < this.pixels.length; i++) {
      this.pixels[i] = new Pixel();
    }
    this.pusher = pusher;
    this.stripNumber = stripNumber;
    this.touched = false;
    this.powerScale = 1.0;
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

  public boolean isTouched() {
    return this.touched;
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
  }

  public synchronized void setPixel(int color, int position) {
    this.pixels[position].setColor(color);
    this.touched = true;
  }
  
  public synchronized void setPixel(Pixel pixel, int position) {
    this.pixels[position].setColor(pixel);
    this.touched = true;
  }

  public byte[] serialize() {
    byte[] msg = new byte[pixels.length * 3];
    int i = 0;
    for (Pixel pixel : pixels) {
      if (pixel == null)
        pixel = new Pixel();
      msg[i++] = (byte) (((double)pixel.red) * powerScale);
      msg[i++] = (byte) (((double)pixel.green) * powerScale);
      msg[i++] = (byte) (((double)pixel.blue) * powerScale);
    }
    this.touched = false;
    return msg;
  }
}
