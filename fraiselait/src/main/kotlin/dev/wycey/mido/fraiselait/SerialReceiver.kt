package dev.wycey.mido.fraiselait

import processing.serial.Serial

interface SerialReceiver {
  fun serialEvent(serial: Serial)
}
