package com.heroicrobot.dropbit.pixelpusher;

import java.util.Arrays;

import com.heroicrobot.dropbit.devices.PixelPusher;

public class Strip {

  private Pixel[] pixels;
  private PixelPusher pusher;
  private int stripNumber;
  
  public Strip(PixelPusher pusher, int stripNumber, int length) {
    this.pixels = new Pixel[length];
    this.pusher = pusher;
    this.stripNumber = stripNumber;
  }
  
  public int getLength() {
    return pixels.length;
  }
  
  public String getMacAddress() {
    return this.pusher.getMacAddress();
  }
  
  public int getStripNumber() {
    return stripNumber;
  }
  
  public synchronized void setPixels(Pixel[] pixels) {
    this.pixels = Arrays.copyOfRange(pixels, 0, this.pixels.length);
  }
  
  public byte[] serialize() {
    byte[] msg = new byte[pixels.length * 3];
    int i = 0;
    for (Pixel pixel : pixels) {
      msg[i++] = pixel.red;
      msg[i++] = pixel.green;
      msg[i++] = pixel.blue;
    }
    return msg;
  }

}
