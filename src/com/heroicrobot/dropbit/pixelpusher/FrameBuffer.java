package com.heroicrobot.dropbit.pixelpusher;

import java.util.Arrays;
import java.util.List;

public class FrameBuffer {

  private Pixel[][] buffer;
  private List<Strip> strips;
  
  public FrameBuffer(Strip[] strips) {
    this.strips = Arrays.asList(strips);
    int maxLength = findMaxLengthOfStrips(strips);
    buffer = new Pixel[maxLength][strips.length];
  }
  
  public synchronized void reconfigure(Strip[] strips) {
    List<Strip> newStrips = Arrays.asList(strips);
    int maxLength = findMaxLengthOfStrips(strips);
    Pixel[][] newBuffer = new Pixel[maxLength][strips.length];
    for (int x = 0; x < Math.min(maxLength, buffer.length); x++) {
      for (int y = 0; y < Math.min(strips.length, buffer[x].length); y++) {
        newBuffer[x][y] = buffer[x][y];
      }
    }
    this.buffer = newBuffer;
    this.strips = newStrips;
  }

  private int findMaxLengthOfStrips(Strip[] strips) {
    int maxLength = 0;
    for (Strip strip : strips) {
      if (strip.getLength() > maxLength) {
        maxLength = strip.getLength();
      }
    }
    return maxLength;
  }
  
  private void updateStrips() {
    
    for (int strip = 0; strip < buffer[0].length; strip++) {
      Pixel[] stripArray = new Pixel[buffer.length];
      for (int x = 0; strip < buffer.length; x++) {
        stripArray[x] = buffer[x][strip];
      }
      this.strips.get(strip).setPixels(stripArray);
    }
  }
  
  public void push() {
    updateStrips();
    for (Strip strip: this.strips) {
      strip.serialize();
    }
    
  }

  public synchronized void putPixel(int x, int y, Pixel pixel) {
    if (x > buffer.length) {
      throw new IllegalArgumentException();
    }
    if (y > buffer[x].length) {
      throw new IllegalArgumentException();
    }
    buffer[x][y] = pixel;
  }

}
