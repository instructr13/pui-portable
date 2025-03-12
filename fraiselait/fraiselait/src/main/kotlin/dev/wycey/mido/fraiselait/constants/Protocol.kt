package dev.wycey.mido.fraiselait.constants

public const val PROTOCOL_VERSION: UShort = 300U

public const val COMMAND_DATA_GET_IMMEDIATE: UByte = 0x90U
public const val COMMAND_DATA_GET_LOOP_OFF: UByte = 0x92U
public const val COMMAND_DATA_GET_LOOP_ON: UByte = 0x93U

public const val COMMAND_DATA_SET: UByte = 0xE0U

public const val COMMAND_DEVICE_INFO_GET: UByte = 0xF0U

public const val RESPONSE_DATA_START: UByte = 0x9FU
public const val RESPONSE_RESERVED_ERROR: UByte = 0xFEU
