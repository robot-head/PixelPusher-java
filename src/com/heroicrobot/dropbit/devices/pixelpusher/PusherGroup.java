package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class PusherGroup {

  private Set<PixelPusher> pushers;

  PusherGroup(Set<PixelPusher> pushers) {
    this.pushers = pushers;
  }

  public PusherGroup() {
    pushers = new TreeSet<PixelPusher>();
  }

  public Set<PixelPusher> getPushers() {
    return this.pushers;
  }

  public List<Strip> getStrips() {
    List<Strip> strips = new ArrayList<Strip>();
    for (PixelPusher p : this.pushers) {
      strips.addAll(p.getStrips());
    }
    return strips;
  }

  public void removePusher(PixelPusher pusher) {
    this.pushers.remove(pusher);
  }

  public void addPusher(PixelPusher pusher) {
    this.pushers.add(pusher);
  }
}
