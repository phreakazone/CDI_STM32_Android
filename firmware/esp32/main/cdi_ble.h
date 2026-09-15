#ifndef CDI_BLE_H
#define CDI_BLE_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*cdi_ble_rx_fn)(const uint8_t *data, size_t size);

void cdi_ble_init(cdi_ble_rx_fn rx_callback);
void cdi_ble_notify(const uint8_t *data, size_t size);
void cdi_ble_notify_telemetry(const uint8_t *data, size_t size);
void cdi_ble_notify_ota_status(const uint8_t *data, size_t size);

#ifdef __cplusplus
}
#endif

#endif /* CDI_BLE_H */
