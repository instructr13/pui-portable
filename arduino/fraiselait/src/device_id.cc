#include "device_id.h"

#include "constants.h"
#include "xxh32.h"

#include <pico/unique_id.h>

uint32_t device_id::get() {
  if constexpr (CUSTOM_DEVICE_ID > 0) {
    return CUSTOM_DEVICE_ID;
  } else {
    static uint32_t id;
    static bool output_hash_got = false;

    if (!output_hash_got) {
      pico_unique_board_id_t raw_id;

      pico_get_unique_board_id(&raw_id);

      id = XXH32(raw_id.id, 8, 0);

      output_hash_got = true;
    }

    return id;
  }
}
