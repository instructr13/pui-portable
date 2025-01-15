package dev.wycey.mido.fraiselait

import processing.serial.Serial

public interface SerialReceiver {
  public fun serialEvent(serial: Serial)
}
