#include "xxh32.h"

static uint32_t const PRIME32_1 =
    0x9E3779B1U; /* 0b10011110001101110111100110110001 */
static uint32_t const PRIME32_2 =
    0x85EBCA77U; /* 0b10000101111010111100101001110111 */
static uint32_t const PRIME32_3 =
    0xC2B2AE3DU; /* 0b11000010101100101010111000111101 */
static uint32_t const PRIME32_4 =
    0x27D4EB2FU; /* 0b00100111110101001110101100101111 */
static uint32_t const PRIME32_5 =
    0x165667B1U; /* 0b00010110010101100110011110110001 */

/* Rotates value left by amt. */
static uint32_t XXH_rotl32(uint32_t const value, uint32_t const amt) {
  return (value << (amt % 32)) | (value >> (32 - (amt % 32)));
}

/* Portably reads a 32-bit little endian integer from data at the given offset.
 */
static uint32_t XXH_read32(uint8_t const *const data, size_t const offset) {
  return (uint32_t)data[offset + 0] | ((uint32_t)data[offset + 1] << 8) |
         ((uint32_t)data[offset + 2] << 16) |
         ((uint32_t)data[offset + 3] << 24);
}

/* Mixes input into acc. */
static uint32_t XXH32_round(uint32_t acc, uint32_t const input) {
  acc += input * PRIME32_2;
  acc = XXH_rotl32(acc, 13);
  acc *= PRIME32_1;
  return acc;
}

/* Mixes all bits to finalize the hash. */
static uint32_t XXH32_avalanche(uint32_t hash) {
  hash ^= hash >> 15;
  hash *= PRIME32_2;
  hash ^= hash >> 13;
  hash *= PRIME32_3;
  hash ^= hash >> 16;
  return hash;
}

/* The XXH32 hash function.
 * input:   The data to hash.
 * length:  The length of input. It is undefined behavior to have length larger
 * than the capacity of input. seed:    A 32-bit value to seed the hash with.
 * returns: The 32-bit calculated hash value. */
uint32_t XXH32(void const *const input, size_t const length,
               uint32_t const seed) {
  uint8_t const *const data = (uint8_t const *)input;
  uint32_t hash;
  size_t remaining = length;
  size_t offset = 0;

  /* Don't dereference a null pointer. The reference implementation notably
   * doesn't check for this by default. */
  if (input == NULL) {
    return XXH32_avalanche(seed + PRIME32_5);
  }

  if (remaining >= 16) {
    /* Initialize our accumulators */
    uint32_t acc1 = seed + PRIME32_1 + PRIME32_2;
    uint32_t acc2 = seed + PRIME32_2;
    uint32_t acc3 = seed + 0;
    uint32_t acc4 = seed - PRIME32_1;

    while (remaining >= 16) {
      acc1 = XXH32_round(acc1, XXH_read32(data, offset));
      offset += 4;
      acc2 = XXH32_round(acc2, XXH_read32(data, offset));
      offset += 4;
      acc3 = XXH32_round(acc3, XXH_read32(data, offset));
      offset += 4;
      acc4 = XXH32_round(acc4, XXH_read32(data, offset));
      offset += 4;
      remaining -= 16;
    }

    hash = XXH_rotl32(acc1, 1) + XXH_rotl32(acc2, 7) + XXH_rotl32(acc3, 12) +
           XXH_rotl32(acc4, 18);
  } else {
    /* Not enough data for the main loop, put something in there instead. */
    hash = seed + PRIME32_5;
  }

  hash += (uint32_t)length;

  /* Process the remaining data. */
  while (remaining >= 4) {
    hash += XXH_read32(data, offset) * PRIME32_3;
    hash = XXH_rotl32(hash, 17);
    hash *= PRIME32_4;
    offset += 4;
    remaining -= 4;
  }

  while (remaining != 0) {
    hash += (uint32_t)data[offset] * PRIME32_5;
    hash = XXH_rotl32(hash, 11);
    hash *= PRIME32_1;
    --remaining;
    ++offset;
  }
  return XXH32_avalanche(hash);
}
