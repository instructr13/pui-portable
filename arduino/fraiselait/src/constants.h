#pragma once

#include <cstddef>
#include <cstdint>

constexpr uint32_t CUSTOM_DEVICE_ID = 0;
constexpr bool     SOFTWARE_RESET_ON_DISCONNECT = false;

/* PINS */

constexpr uint8_t PIN_SPEAKER = 8;
constexpr uint8_t PIN_TACT_SWITCH = 9;
constexpr uint8_t PIN_LED_GREEN = 12;
constexpr uint8_t PIN_LED_BLUE = 11;
constexpr uint8_t PIN_LED_RED = 10;
constexpr uint8_t PIN_LIGHT_SENSOR = 28;

/* FIFO COMMANDS */

constexpr uint32_t FIFO_NO_TONE = 0xcafe0000;
constexpr uint32_t FIFO_TONE = 0xcafe000a;
constexpr uint32_t FIFO_RGB_LED = 0xcafe000b;
constexpr uint32_t FIFO_LED_BUILTIN = 0xcafe000c;
constexpr uint32_t FIFO_WAVEFORM = 0xcafe000d;

/* SENSORS */

constexpr size_t ANALOG_READINGS = 24;
