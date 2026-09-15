#include "cdi_ble.h"
#include "esp_log.h"
#include "nimble/nimble_port.h"
#include "nimble/nimble_port_esp32.h"
#include "host/ble_hs.h"
#include "host/util/util.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"
#include <string.h>

#define TAG "CDI_BLE"
#define DEVICE_NAME "NS200-CDI-R7"

/* 7a8f1000-6c9d-4e40-a45f-0b4b4e533230 */
static const ble_uuid128_t s_svc_uuid =
    BLE_UUID128_INIT(0x30, 0x32, 0x53, 0x4e, 0x4b, 0x0b, 0x5f, 0xa4, 0x40, 0x4e, 0x9d, 0x6c, 0x00, 0x10, 0x8f, 0x7a);

/* 7a8f1001-6c9d-4e40-a45f-0b4b4e533230 (Telemetry Notify) */
static const ble_uuid128_t s_chr_telem_uuid =
    BLE_UUID128_INIT(0x30, 0x32, 0x53, 0x4e, 0x4b, 0x0b, 0x5f, 0xa4, 0x40, 0x4e, 0x9d, 0x6c, 0x01, 0x10, 0x8f, 0x7a);

/* 7a8f1002-6c9d-4e40-a45f-0b4b4e533230 (Command Write) */
static const ble_uuid128_t s_chr_cmd_uuid =
    BLE_UUID128_INIT(0x30, 0x32, 0x53, 0x4e, 0x4b, 0x0b, 0x5f, 0xa4, 0x40, 0x4e, 0x9d, 0x6c, 0x02, 0x10, 0x8f, 0x7a);

/* 7a8f1003-6c9d-4e40-a45f-0b4b4e533230 (Response Notify) */
static const ble_uuid128_t s_chr_resp_uuid =
    BLE_UUID128_INIT(0x30, 0x32, 0x53, 0x4e, 0x4b, 0x0b, 0x5f, 0xa4, 0x40, 0x4e, 0x9d, 0x6c, 0x03, 0x10, 0x8f, 0x7a);

/* 7a8f1004-6c9d-4e40-a45f-0b4b4e533230 (OTA Data Write No Resp) */
static const ble_uuid128_t s_chr_ota_data_uuid =
    BLE_UUID128_INIT(0x30, 0x32, 0x53, 0x4e, 0x4b, 0x0b, 0x5f, 0xa4, 0x40, 0x4e, 0x9d, 0x6c, 0x04, 0x10, 0x8f, 0x7a);

/* 7a8f1005-6c9d-4e40-a45f-0b4b4e533230 (OTA Status Notify) */
static const ble_uuid128_t s_chr_ota_status_uuid =
    BLE_UUID128_INIT(0x30, 0x32, 0x53, 0x4e, 0x4b, 0x0b, 0x5f, 0xa4, 0x40, 0x4e, 0x9d, 0x6c, 0x05, 0x10, 0x8f, 0x7a);

static uint16_t s_telem_val_handle;
static uint16_t s_resp_val_handle;
static uint16_t s_ota_status_val_handle;
static uint16_t s_conn_handle = BLE_HS_CONN_HANDLE_NONE;
static cdi_ble_rx_fn s_rx_cb = NULL;

extern void cdi_esp32_ota_data_rx(const uint8_t *data, size_t size);

static int cdi_gatt_access_cb(uint16_t conn_handle, uint16_t attr_handle,
                             struct ble_gatt_access_ctxt *ctxt, void *arg) {
    if (ctxt->op == BLE_GATT_ACCESS_OP_WRITE_CHR) {
        if (ble_uuid_cmp(ctxt->chr->uuid, &s_chr_cmd_uuid.u) == 0) {
            uint16_t len = OS_MBUF_PKTLEN(ctxt->om);
            uint8_t buf[256];
            if (len > sizeof(buf) - 1) len = sizeof(buf) - 1;
            os_mbuf_copydata(ctxt->om, 0, len, buf);
            buf[len] = '\0';
            if (s_rx_cb) s_rx_cb(buf, len);
            return 0;
        }
        if (ble_uuid_cmp(ctxt->chr->uuid, &s_chr_ota_data_uuid.u) == 0) {
            uint16_t len = OS_MBUF_PKTLEN(ctxt->om);
            uint8_t buf[256];
            if (len > sizeof(buf)) len = sizeof(buf);
            os_mbuf_copydata(ctxt->om, 0, len, buf);
            cdi_esp32_ota_data_rx(buf, len);
            return 0;
        }
    }
    return 0;
}

static const struct ble_gatt_svc_def s_gatt_svcs[] = {
    {
        .type = BLE_GATT_SVC_TYPE_PRIMARY,
        .uuid = &s_svc_uuid.u,
        .characteristics = (struct ble_gatt_chr_def[]) {
            {
                /* 1001: Telemetry Notify */
                .uuid = &s_chr_telem_uuid.u,
                .access_cb = cdi_gatt_access_cb,
                .flags = BLE_GATT_CHR_F_NOTIFY,
                .val_handle = &s_telem_val_handle,
            },
            {
                /* 1002: Command Write */
                .uuid = &s_chr_cmd_uuid.u,
                .access_cb = cdi_gatt_access_cb,
                .flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_NO_RSP,
            },
            {
                /* 1003: Response Notify */
                .uuid = &s_chr_resp_uuid.u,
                .access_cb = cdi_gatt_access_cb,
                .flags = BLE_GATT_CHR_F_NOTIFY,
                .val_handle = &s_resp_val_handle,
            },
            {
                /* 1004: OTA Data Write No Resp */
                .uuid = &s_chr_ota_data_uuid.u,
                .access_cb = cdi_gatt_access_cb,
                .flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_NO_RSP,
            },
            {
                /* 1005: OTA Status Notify */
                .uuid = &s_chr_ota_status_uuid.u,
                .access_cb = cdi_gatt_access_cb,
                .flags = BLE_GATT_CHR_F_NOTIFY,
                .val_handle = &s_ota_status_val_handle,
            },
            {0}
        },
    },
    {0}
};

static void advertise(void) {
    struct ble_gap_adv_params adv_params;
    struct ble_hs_adv_fields fields;
    memset(&fields, 0, sizeof(fields));

    fields.flags = BLE_HS_ADV_F_DISC_GEN | BLE_HS_ADV_F_BREDR_UNSUP;
    fields.name = (uint8_t *)DEVICE_NAME;
    fields.name_len = strlen(DEVICE_NAME);
    fields.name_is_complete = 1;
    fields.uuids128 = (ble_uuid128_t *)&s_svc_uuid;
    fields.num_uuids128 = 1;
    fields.uuids128_is_complete = 1;

    ble_gap_adv_set_fields(&fields);

    memset(&adv_params, 0, sizeof(adv_params));
    adv_params.conn_mode = BLE_GAP_CONN_MODE_UND;
    adv_params.disc_mode = BLE_GAP_DISC_MODE_GEN;
    ble_gap_adv_start(BLE_OWN_ADDR_PUBLIC, NULL, BLE_HS_FOREVER, &adv_params, NULL, NULL);
}

static int gap_event_cb(struct ble_gap_event *event, void *arg) {
    switch (event->type) {
        case BLE_GAP_EVENT_CONNECT:
            if (event->connect.status == 0) {
                s_conn_handle = event->connect.conn_handle;
                ESP_LOGI(TAG, "BLE connected handle=%d", s_conn_handle);
            } else {
                advertise();
            }
            break;
        case BLE_GAP_EVENT_DISCONNECT:
            s_conn_handle = BLE_HS_CONN_HANDLE_NONE;
            ESP_LOGI(TAG, "BLE disconnected, restarting advertising");
            advertise();
            break;
        case BLE_GAP_EVENT_SUBSCRIBE:
            ESP_LOGI(TAG, "BLE subscribe attr=%d cur_notify=%d",
                     event->subscribe.attr_handle, event->subscribe.cur_notify);
            break;
        case BLE_GAP_EVENT_MTU:
            ESP_LOGI(TAG, "BLE MTU update: %d", event->mtu.value);
            break;
        default:
            break;
    }
    return 0;
}

static void on_sync(void) {
    advertise();
}

static void host_task(void *param) {
    nimble_port_run();
    nimble_port_freertos_deinit();
}

void cdi_ble_init(cdi_ble_rx_fn rx_callback) {
    s_rx_cb = rx_callback;
    nimble_port_init();

    ble_svc_gap_init();
    ble_svc_gatt_init();

    ble_gatts_count_cfg(s_gatt_svcs);
    ble_gatts_add_svcs(s_gatt_svcs);

    ble_svc_gap_device_name_set(DEVICE_NAME);

    ble_hs_cfg.sync_cb = on_sync;
    ble_hs_cfg.gatts_register_cb = NULL;
    ble_hs_cfg.store_status_cb = ble_store_util_status_rr;

    nimble_port_freertos_init(host_task);
}

void cdi_ble_notify(const uint8_t *data, size_t size) {
    if (s_conn_handle == BLE_HS_CONN_HANDLE_NONE || !data || size == 0) return;
    struct os_mbuf *om = ble_hs_mbuf_from_flat(data, size);
    if (om) {
        ble_gatts_notify_custom(s_conn_handle, s_resp_val_handle, om);
    }
}

void cdi_ble_notify_telemetry(const uint8_t *data, size_t size) {
    if (s_conn_handle == BLE_HS_CONN_HANDLE_NONE || !data || size == 0) return;
    struct os_mbuf *om = ble_hs_mbuf_from_flat(data, size);
    if (om) {
        ble_gatts_notify_custom(s_conn_handle, s_telem_val_handle, om);
    }
}

void cdi_ble_notify_ota_status(const uint8_t *data, size_t size) {
    if (s_conn_handle == BLE_HS_CONN_HANDLE_NONE || !data || size == 0) return;
    struct os_mbuf *om = ble_hs_mbuf_from_flat(data, size);
    if (om) {
        ble_gatts_notify_custom(s_conn_handle, s_ota_status_val_handle, om);
    }
}
