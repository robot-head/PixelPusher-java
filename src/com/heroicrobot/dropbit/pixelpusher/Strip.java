package com.heroicrobot.dropbit.pixelpusher;

import java.util.Arrays;

public class Strip {

  Pixel[] pixels;
  
  public Strip(int length) {
    this.pixels = new Pixel[length];
  }
  
  public int getLength() {
    return pixels.length;
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
