#include "cdi_firmware.h"
#include "driver/gpio.h"
#include "esp_adc/adc_oneshot.h"
#include "esp_attr.h"
#include "esp_ota_ops.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "nvs.h"
#include "nvs_flash.h"
#include <string.h>

#define CDI_GPIO_REFERENCE GPIO_NUM_4
#define CDI_GPIO_IGNITION_CENTER GPIO_NUM_25
#define CDI_GPIO_IGNITION_SIDE GPIO_NUM_26
#define CDI_GPIO_CHARGER_A GPIO_NUM_18
#define CDI_GPIO_CHARGER_B GPIO_NUM_19
#define CDI_GPIO_FAN GPIO_NUM_13
#define CDI_ADC_TPS ADC_CHANNEL_0   /* GPIO36 */
#define CDI_ADC_TEMP ADC_CHANNEL_3  /* GPIO39 */
#define CDI_ADC_HV_CENTER ADC_CHANNEL_7 /* GPIO35 */
#define CDI_ADC_HV_SIDE ADC_CHANNEL_4   /* GPIO32 */
#define CDI_HV_FULL_SCALE_X10 4000u

static cdi_context_t s_cdi;
static TaskHandle_t s_trigger_task;
static esp_timer_handle_t s_delay_timer;
static esp_timer_handle_t s_fire_timer;
static volatile uint32_t s_reference_timestamp_us;
static const esp_partition_t *s_ota_partition;
static esp_ota_handle_t s_ota_handle;
static nvs_handle_t s_nvs;
static adc_oneshot_unit_handle_t s_adc;

extern void cdi_ble_init(void (*rx)(const uint8_t*,size_t));
extern void cdi_ble_notify(const uint8_t *data,size_t size);

static uint32_t port_micros(void){ return (uint32_t)esp_timer_get_time(); }
static void set_ignition(bool v){
    gpio_set_level(CDI_GPIO_IGNITION_CENTER,v);
    gpio_set_level(CDI_GPIO_IGNITION_SIDE,v);
}
static void set_charger(bool v){
    gpio_set_level(CDI_GPIO_CHARGER_A,v);
    gpio_set_level(CDI_GPIO_CHARGER_B,v);
}
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

static void fire_off(void *arg){ (void)arg;set_ignition(false); }
static void fire_now(void *arg){
    (void)arg;
    if(s_cdi.telemetry.ota_active) return;
    set_ignition(true);
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
    gpio_config_t out={.pin_bit_mask=(1ULL<<CDI_GPIO_IGNITION_CENTER)|
        (1ULL<<CDI_GPIO_IGNITION_SIDE)|(1ULL<<CDI_GPIO_CHARGER_A)|
        (1ULL<<CDI_GPIO_CHARGER_B)|(1ULL<<CDI_GPIO_FAN),.mode=GPIO_MODE_OUTPUT};
    gpio_config(&out);
    adc_oneshot_unit_init_cfg_t adc_unit={.unit_id=ADC_UNIT_1};
    adc_oneshot_new_unit(&adc_unit,&s_adc);
    adc_oneshot_chan_cfg_t adc_cfg={.atten=ADC_ATTEN_DB_11,.bitwidth=ADC_BITWIDTH_12};
    adc_oneshot_config_channel(s_adc,CDI_ADC_TPS,&adc_cfg);
    adc_oneshot_config_channel(s_adc,CDI_ADC_TEMP,&adc_cfg);
    adc_oneshot_config_channel(s_adc,CDI_ADC_HV_CENTER,&adc_cfg);
    adc_oneshot_config_channel(s_adc,CDI_ADC_HV_SIDE,&adc_cfg);
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
    for(;;){
        int tps=0,temp=0,hvc=0,hvs=0;
        adc_oneshot_read(s_adc,CDI_ADC_TPS,&tps);
        adc_oneshot_read(s_adc,CDI_ADC_TEMP,&temp);
        adc_oneshot_read(s_adc,CDI_ADC_HV_CENTER,&hvc);
        adc_oneshot_read(s_adc,CDI_ADC_HV_SIDE,&hvs);
        uint8_t load=(uint8_t)((uint32_t)tps*100u/4095u);
        uint16_t hv=(uint16_t)((uint32_t)(hvc>hvs?hvc:hvs)*CDI_HV_FULL_SCALE_X10/4095u);
        cdi_set_inputs(&s_cdi,load,(uint16_t)temp,hv);
        cdi_tick(&s_cdi);
        vTaskDelay(pdMS_TO_TICKS(10));
    }
}
