# Fraiselait Arduino

Arduino communication program used by Fraiselait. Requires Raspberry Pi Pico.

## Running

```bash
pio project metadata # Recommended to run this first
./scripts/prepare.sh
pio run -t upload
pio -e pico2 run -t upload # For Raspberry Pi Pico 2
```
