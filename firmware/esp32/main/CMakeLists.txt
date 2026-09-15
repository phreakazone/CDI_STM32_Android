idf_component_register(
    SRCS "cdi_port.c" "cdi_ble.c" "../../common/src/cdi_firmware.c"
    INCLUDE_DIRS "." "../../common/include"
    REQUIRES driver esp_adc esp_timer app_update nvs_flash bt
)
