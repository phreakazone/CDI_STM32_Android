# CDI Universal R9 firmware

The same portable C99 ignition core is used by the STM32WB55 and ESP32 targets. The
Android app must negotiate features through `GET,CAPS`; it must not infer limits from
the motorcycle model or MCU name.

## Safety model

A newly flashed or invalid configuration starts with a 3000 rpm limiter and a maximum
10.0 degree advance. `SETUP,DONE` unlocks the configured limiter only after wiring,
pulser geometry and timing have been checked with a timing light. Ignition and charger
outputs are forced off during configuration writes and OTA. Advance beyond the physical
trigger angle is clipped and reported with `CDI_FAULT_ADVANCE_CLIPPED`.

These software checks do not replace an isolated HV input, proper SCR/IGBT gate driver,
fusing, kill switch, watchdog, flyback protection or validation on a current-limited
bench supply.

## Universal format limits

| Field | Protocol format | Default profile |
|---|---:|---:|
| RPM | 0..30000 | 300..22000 |
| Advance | -30.0..80.0 deg | -15.0..60.0 deg |
| Map | up to 32 x 16 | 12 x 6 |
| Slots | 4 | application-selected |
| Pulser pulses/rev | 1..12 | 1 |

Format limits are storage/protocol ranges, not a promise that every coil, power stage
or engine is safe at those values. Profiles keep vehicle-specific geometry out of the
application.

## Commands used by Android

- Android framing: `@sequence,command*CRC16-CCITT` (plain commands remain available for bench consoles)
- `PING`, `GET,CAPS`, `GET,PROFILE`, `GET,TELEM`, `GET,TEMP`, `GET,STATUS`, `GET,META`, `GET,SETUP`
- `MAP,SELECT,slot`
- `SET,PROFILE,name,rpmMin,rpmMax,advMinX10,advMaxX10,ppr,triggerX10`
- `SET,LIMIT,rpm`
- `SET,FAN,OFF|ON|AUTO,onX10,offX10`
- `TEMP,CAL,adc1,temp1X10,adc2,temp2X10,adc3,temp3X10`
- `MAP,BEGIN,rpmCount,loadCount`, `MAP,RPM`, `MAP,LOAD`, `MAP,CELL`, `MAP,SAVE,slot|ABORT`
- `DYNO,BEGIN|TRIM|COMMIT|ABORT`
- `OTA,BEGIN,size,crc32`, `OTA,DATA,offset,hex`, `OTA,END|ABORT`
- `SETUP,DONE`

## Port notes

STM32 OTA is disabled by default (`CDI_STM32_OTA_ENABLE=0`) until a bootloader,
signed-image policy and linker layout are supplied. ESP32 uses native A/B OTA
partitions. The ESP32 GPIO assignments are examples and must match the PCB. ISR work
is deliberately limited; lookup and timer scheduling run in the highest-priority task.
