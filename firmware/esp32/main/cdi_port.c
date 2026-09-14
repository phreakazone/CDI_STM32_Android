#include "cdi_firmware.h"
#include "driver/gpio.h"
#include "esp_attr.h"
#include "esp_ota_ops.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "nvs.h"
#include "nvs_flash.h"
#include <string.h>

#define CDI_GPIO_REFERENCE GPIO_NUM_4
#define CDI_GPIO_IGNITION GPIO_NUM_18
#define CDI_GPIO_CHARGER GPIO_NUM_19
#define CDI_GPIO_FAN GPIO_NUM_21

static cdi_context_t s_cdi;
static TaskHandle_t s_trigger_task;
static esp_timer_handle_t s_delay_timer;
static esp_timer_handle_t s_fire_timer;
static volatile uint32_t s_reference_timestamp_us;
static const esp_partition_t *s_ota_partition;
static esp_ota_handle_t s_ota_handle;
static nvs_handle_t s_nvs;

extern void cdi_ble_init(void (*rx)(const uint8_t*,size_t));
extern void cdi_ble_notify(const uint8_t *data,size_t size);

static uint32_t port_micros(void){ return (uint32_t)esp_timer_get_time(); }
static void set_ignition(bool v){ gpio_set_level(CDI_GPIO_IGNITION,v); }
static void set_charger(bool v){ gpio_set_level(CDI_GPIO_CHARGER,v); }
static void set_fan(bool v){ gpio_set_level(CDI_GPIO_FAN,v); }
static bool load_config(void *data,size_t size){ size_t n=size;return nvs_get_blob(s_nvs,"config",data,&n)==ESP_OK&&n==size; }
static bool save_config(const void *data,size_t size){ return nvs_set_blob(s_nvs,"config",data,size)==ESP_OK&&nvs_commit(s_nvs)==ESP_OK; }

static bool ota_begin(uint32_t size,uint32_t crc){
    (void)crc;
    s_ota_partition=esp_ota_get_next_update_partition(NULL);
    return s_ota_partition&&esp_ota_begin(s_ota_partition,size,&s_ota_handle)==ESP_OK;
}
static bool ota_write(uint32_t offset,const uint8_t *data,size_t size){
    (void)offset; return esp_ota_write(s_ota_handle,data,size)==ESP_OK;
}
static bool ota_finish(void){
    return esp_ota_end(s_ota_handle)==ESP_OK&&esp_ota_set_boot_partition(s_ota_partition)==ESP_OK;
}
static void ota_abort(void){ esp_ota_abort(s_ota_handle); }

static void fire_off(void *arg){ (void)arg;gpio_set_level(CDI_GPIO_IGNITION,0); }
static void fire_now(void *arg){
    (void)arg;
    if(s_cdi.telemetry.ota_active) return;
    gpio_set_level(CDI_GPIO_IGNITION,1);
    esp_timer_stop(s_fire_timer);
    esp_timer_start_once(s_fire_timer,120);
}

static void IRAM_ATTR reference_isr(void *arg){
    (void)arg;
    BaseType_t wake=pdFALSE;
    s_reference_timestamp_us=(uint32_t)esp_timer_get_time();
    vTaskNotifyGiveFromISR(s_trigger_task,&wake);
    if(wake) portYIELD_FROM_ISR();
}

static void trigger_task(void *arg){
    (void)arg;
    for(;;){
        ulTaskNotifyTake(pdTRUE,portMAX_DELAY);
        cdi_trigger_result_t r=cdi_on_reference_pulse(&s_cdi,s_reference_timestamp_us);
        if(r.fire){
            esp_timer_stop(s_delay_timer);
            esp_timer_start_once(s_delay_timer,r.delay_us);
        }
    }
}

static char s_line[196];
static size_t s_line_len;
static void ble_rx(const uint8_t *data,size_t size){
    for(size_t i=0;i<size;++i){
        char ch=(char)data[i];
        if(ch=='\n'||ch=='\r'){
            if(s_line_len){
                char reply[220];s_line[s_line_len]='\0';
                size_t n=cdi_handle_command(&s_cdi,s_line,reply,sizeof(reply));
                if(n)cdi_ble_notify((const uint8_t*)reply,n);
                s_line_len=0;
            }
        }else if(s_line_len+1u<sizeof(s_line))s_line[s_line_len++]=ch;
        else s_line_len=0;
    }
}

void app_main(void){
    nvs_flash_init();
    nvs_open("cdi",NVS_READWRITE,&s_nvs);
    gpio_config_t out={.pin_bit_mask=(1ULL<<CDI_GPIO_IGNITION)|(1ULL<<CDI_GPIO_CHARGER)|(1ULL<<CDI_GPIO_FAN),.mode=GPIO_MODE_OUTPUT};
    gpio_config(&out);
    gpio_config_t in={.pin_bit_mask=1ULL<<CDI_GPIO_REFERENCE,.mode=GPIO_MODE_INPUT,.intr_type=GPIO_INTR_POSEDGE};
    gpio_config(&in);
    esp_timer_create_args_t delay_args={.callback=fire_now,.name="cdi_delay"};
    esp_timer_create(&delay_args,&s_delay_timer);
    esp_timer_create_args_t fire_args={.callback=fire_off,.name="cdi_fire_off"};
    esp_timer_create(&fire_args,&s_fire_timer);
    cdi_hal_t hal={port_micros,set_ignition,set_charger,set_fan,load_config,save_config,ota_begin,ota_write,ota_finish,ota_abort};
    cdi_init(&s_cdi,&hal);
    xTaskCreatePinnedToCore(trigger_task,"cdi_trigger",4096,NULL,configMAX_PRIORITIES-1,&s_trigger_task,1);
    gpio_install_isr_service(ESP_INTR_FLAG_IRAM);
    gpio_isr_handler_add(CDI_GPIO_REFERENCE,reference_isr,NULL);
    cdi_ble_init(ble_rx);
    for(;;){ cdi_tick(&s_cdi);vTaskDelay(pdMS_TO_TICKS(10)); }
}
