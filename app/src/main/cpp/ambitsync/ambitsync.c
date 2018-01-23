#include <stdio.h>
#include <android/log.h>
#include <malloc.h>
#include <libambit.h>
#include "idv_markkuo_ambitsync_MainActivity.h"
#include "../libambit/protocol.h"
#include "../libambit/android_def.h"

const static char *TAG = "AmbitSync";

/* structure for communicating with ambit device */
struct ambit_device {
    int vid;
    int pid;
    int fd;
    ambit_object_t *obj;
    ambit_device_status_t status;
    ambit_personal_settings_t settings;

    jobject record_obj;
    JNIEnv *env;
};

/* structure for holding java class/method global reference */
struct java_ref {
    jclass cls;
    jmethodID set_progress;
    jmethodID set_personal_setting;
    jmethodID add_header;
    jmethodID entry_needs_download;

    jmethodID add_periodic;
    jmethodID add_ibi;
    jmethodID add_ttff;
    jmethodID add_distance_source;
    jmethodID add_lapinfo;
    jmethodID add_altitude_source;
    jmethodID add_position;
    jmethodID add_gpstiny;
    jmethodID add_gpssmall;
    jmethodID add_gpsbase;
    jmethodID add_time;
    jmethodID add_swimming_turn;
    jmethodID add_activity;
    jmethodID add_cadence_source;
    jmethodID add_fwinfo;
    jmethodID add_unknown;
};

/* the java reference static instance */
static struct java_ref J;

/* for Java to connect to Ambit watch, the initialization function */
static struct ambit_device *setup_ambit(int vid, int pid, int fd, const char *path)
{
    struct ambit_device *dev;

    dev = malloc(sizeof(*dev));
    if (!dev)
        return NULL;

    /* save VID/PID/fd locally */
    dev->vid = vid;
    dev->pid = pid;
    dev->fd = fd;

    /* set fd/path in libambit, so we can re-use this same fd/path
     * in libusb with granted permission from Java layer */
    libambit_set_device(fd, path);

    /* actually create an ambit object for communication */
    dev->obj = libambit_create((unsigned short)vid, (unsigned short)pid);

    if (!dev->obj) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "unable to connect to Ambit");
        free(dev);
        return NULL;
    }

    /* get initial status and user setting */
    if (libambit_device_status_get(dev->obj, &dev->status))
        __android_log_print(ANDROID_LOG_WARN, TAG, "Failed to read status");
    if (libambit_personal_settings_get(dev->obj, &dev->settings))
        __android_log_print(ANDROID_LOG_WARN, TAG, "Failed to read user setting");

    /* print some user setting for logging */
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Ambit user is %smale", dev->settings.is_male ? "" : "fe");
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Ambit user weight: %.1f kg", dev->settings.weight / 100.0);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Ambit user max_hr: %d bpm", dev->settings.max_hr);

    return dev;
}

/* for Java to disconnect the Ambit watch, the finalization function */
static void kill_ambit(struct ambit_device *ambit)
{
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s", __func__);
    libambit_close(ambit->obj);
    libambit_set_device(-1, NULL);
    free(ambit);
}

/* callback for libambit to determine if a specific move is to be skipped or not */
static int log_skip_cb(void *obj, ambit_log_header_t *log_header)
{
    struct ambit_device * ambit = (struct ambit_device *)obj;
    JNIEnv *env = ambit->env;

    __android_log_print(ANDROID_LOG_INFO, TAG, "Log header \"%s\" %d-%02d-%02d %02d:%02d:%02d\n",
           log_header->activity_name,
           log_header->date_time.year, log_header->date_time.month,
           log_header->date_time.day, log_header->date_time.hour,
           log_header->date_time.minute, log_header->date_time.msec/1000);

    /* call Java to know if we need to skip it, because only the app layer knows it */
    ambit_log_header_t *h = log_header;
    if((*env)->CallBooleanMethod(env, ambit->record_obj, J.entry_needs_download, h->date_time.year, h->date_time.month,
          h->date_time.day, h->date_time.hour, h->date_time.minute, h->date_time.msec, h->duration)) {

        /* skip this one if it's added before */
        __android_log_print(ANDROID_LOG_INFO, TAG, "Entry needs download!");
        return 1; /* download it */
    }

    return 0; /* skip it */

}

/*
 * callback for libambit to determine if a specific move is to be skipped or not
 *  it's used by fetching header only for UI display, so by default all log download is skipped
 *  this function also adds header to Java and store the header info in Java
 */
static int log_skip_header_cb(void *obj, ambit_log_header_t *log_header)
{
    struct ambit_device * ambit = (struct ambit_device *)obj;
    JNIEnv *env = ambit->env;
    ambit_log_header_t *h = log_header;

    __android_log_print(ANDROID_LOG_INFO, TAG, "Log header \"%s\" %d-%02d-%02d %02d:%02d:%02d\n",
                        log_header->activity_name,
                        log_header->date_time.year, log_header->date_time.month,
                        log_header->date_time.day, log_header->date_time.hour,
                        log_header->date_time.minute, log_header->date_time.msec/1000);

    /* Java string to pass to Java */
    jstring activity_name = (*env)->NewStringUTF(env, h->activity_name);

    /* call Java method to add this header */
    (*env)->CallVoidMethod(env, ambit->record_obj, J.add_header,
                                 activity_name,
                                 h->date_time.year, h->date_time.month, h->date_time.day, h->date_time.hour,
                                 h->date_time.minute, h->date_time.msec,
                                 h->duration, h->ascent, h->descent, h->ascent_time, h->descent_time, h->recovery_time,
                                 h->speed_avg, h->speed_max, h->speed_max_time, h->altitude_max, h->altitude_min,
                                 h->altitude_max_time, h->altitude_min_time, h->heartrate_avg, h->heartrate_max,
                                 h->heartrate_min, h->heartrate_max_time, h->heartrate_min_time, h->peak_training_effect,
                                 h->activity_type, h->temperature_max, h->temperature_min, h->temperature_max_time,
                                 h->temperature_min_time, h->distance, h->samples_count, h->energy_consumption,
                                 h->first_fix_time, h->battery_start, h->battery_end, h->distance_before_calib,
                                 h->cadence_max, h->cadence_avg, h->swimming_pool_lengths, h->cadence_max_time,
                                 h->swimming_pool_length);
    (*env)->DeleteLocalRef(env, activity_name);

    /* always skip fetching entry samples */
    return 0;

}

/* the callback to download data, it's empty so the data is not downloaded anywhere */
static void log_no_data_cb(void *obj, ambit_log_entry_t *log_entry)
{
}

/* the callback to download data. It saves data to Java by calling AmbitRecord instance's methods */
static void log_data_cb(void *obj, ambit_log_entry_t *log_entry)
{
    struct ambit_device * ambit = (struct ambit_device *)obj;
    JNIEnv *env = ambit->env;
    int i, j, k;

    __android_log_print(ANDROID_LOG_INFO, TAG, "Got log entry \"%s\" %d-%02d-%02d %02d:%02d:%02d\n",
           log_entry->header.activity_name,
           log_entry->header.date_time.year,log_entry->header.date_time.month,
           log_entry->header.date_time.day, log_entry->header.date_time.hour,
           log_entry->header.date_time.minute, log_entry->header.date_time.msec/1000);

    /* shorter name */
    ambit_log_header_t *h = &log_entry->header;

    /* Java string to pass to Java */
    jstring activity_name = (*env)->NewStringUTF(env, h->activity_name);

    /* call Java method to add this header */
    (*env)->CallVoidMethod(env, ambit->record_obj, J.add_header,
                                 activity_name,
                                 h->date_time.year, h->date_time.month, h->date_time.day, h->date_time.hour,
                                 h->date_time.minute, h->date_time.msec,
                                 h->duration, h->ascent, h->descent, h->ascent_time, h->descent_time, h->recovery_time,
                                 h->speed_avg, h->speed_max, h->speed_max_time, h->altitude_max, h->altitude_min,
                                 h->altitude_max_time, h->altitude_min_time, h->heartrate_avg, h->heartrate_max,
                                 h->heartrate_min, h->heartrate_max_time, h->heartrate_min_time, h->peak_training_effect,
                                 h->activity_type, h->temperature_max, h->temperature_min, h->temperature_max_time,
                                 h->temperature_min_time, h->distance, h->samples_count, h->energy_consumption,
                                 h->first_fix_time, h->battery_start, h->battery_end, h->distance_before_calib,
                                 h->cadence_max, h->cadence_avg, h->swimming_pool_lengths, h->cadence_max_time,
                                 h->swimming_pool_length);
    (*env)->DeleteLocalRef(env, activity_name);

    /* loop through all samples in the entry */
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Total sample count: %d", log_entry->header.samples_count);
    for (i=0; i<log_entry->header.samples_count; i++) {
        /* shorter name */
        ambit_log_sample_t *s = &log_entry->samples[i];

        __android_log_print(ANDROID_LOG_VERBOSE, TAG, "Sample #%d, type: %d, time: %04u-%02u-%02u %02u:%02u:%2.3f\n",
               i, log_entry->samples[i].type, log_entry->samples[i].utc_time.year,
               log_entry->samples[i].utc_time.month, log_entry->samples[i].utc_time.day,
               log_entry->samples[i].utc_time.hour, log_entry->samples[i].utc_time.minute,
               (1.0*log_entry->samples[i].utc_time.msec)/1000);

        /* temp values to pass to Java */
        jintArray values, values2;
        jcharArray char_values;
        int *temp;

        /* call different Java function based on sample type, to pass the sample data to Java */
        switch(s->type) {

            case ambit_log_sample_type_periodic:
                values = (*env)->NewIntArray(env, s->u.periodic.value_count);
                /* reuse this mem for type and their values */
                temp = calloc(s->u.periodic.value_count, sizeof(int));

                /* copy types in those samples to "temp" array */
                for (j = 0; j < s->u.periodic.value_count; j++) {
                    temp[j] = s->u.periodic.values[j].type;
                }
                (*env)->SetIntArrayRegion(env, values, 0, s->u.periodic.value_count, temp);

                /* copy values in those samples to "temp" array */
                values2 = (*env)->NewIntArray(env, s->u.periodic.value_count);
                for (j = 0; j < s->u.periodic.value_count; j++) {

                    /* __android_log_print(ANDROID_LOG_DEBUG, TAG, "periodic[%d] type:%d", j, s->u.periodic.values[j].type); */
                    switch(s->u.periodic.values[j].type) {
                        case ambit_log_sample_periodic_type_latitude:
                            temp[j] = s->u.periodic.values[j].u.latitude;
                            break;
                        case ambit_log_sample_periodic_type_longitude:
                            temp[j] = s->u.periodic.values[j].u.longitude;
                            break;
                        case ambit_log_sample_periodic_type_distance:
                            temp[j] = s->u.periodic.values[j].u.distance;
                            break;
                        case ambit_log_sample_periodic_type_speed:
                            temp[j] = s->u.periodic.values[j].u.speed;
                            break;
                        case ambit_log_sample_periodic_type_hr:
                            temp[j] = s->u.periodic.values[j].u.hr;
                            break;
                        case ambit_log_sample_periodic_type_time:
                            temp[j] = s->u.periodic.values[j].u.time;
                            break;
                        case ambit_log_sample_periodic_type_gpsspeed:
                            temp[j] = s->u.periodic.values[j].u.gpsspeed;
                            break;
                        case ambit_log_sample_periodic_type_wristaccspeed:
                            temp[j] = s->u.periodic.values[j].u.wristaccspeed;
                            break;
                        case ambit_log_sample_periodic_type_bikepodspeed:
                            temp[j] = s->u.periodic.values[j].u.bikepodspeed;
                            break;
                        case ambit_log_sample_periodic_type_ehpe:
                            temp[j] = s->u.periodic.values[j].u.ehpe;
                            break;
                        case ambit_log_sample_periodic_type_evpe:
                            temp[j] = s->u.periodic.values[j].u.evpe;
                            break;
                        case ambit_log_sample_periodic_type_altitude:
                            temp[j] = s->u.periodic.values[j].u.altitude;
                            break;
                        case ambit_log_sample_periodic_type_abspressure:
                            temp[j] = s->u.periodic.values[j].u.abspressure;
                            break;
                        case ambit_log_sample_periodic_type_energy:
                            temp[j] = s->u.periodic.values[j].u.energy;
                            break;
                        case ambit_log_sample_periodic_type_temperature:
                            temp[j] = s->u.periodic.values[j].u.temperature;
                            break;
                        case ambit_log_sample_periodic_type_charge:
                            temp[j] = s->u.periodic.values[j].u.charge;
                            break;
                        case ambit_log_sample_periodic_type_gpsaltitude:
                            temp[j] = s->u.periodic.values[j].u.gpsaltitude;
                            break;
                        case ambit_log_sample_periodic_type_gpsheading:
                            temp[j] = s->u.periodic.values[j].u.gpsheading;
                            break;
                        case ambit_log_sample_periodic_type_gpshdop:
                            temp[j] = s->u.periodic.values[j].u.gpshdop;
                            break;
                        case ambit_log_sample_periodic_type_gpsvdop:
                            temp[j] = s->u.periodic.values[j].u.gpsvdop;
                            break;
                        case ambit_log_sample_periodic_type_wristcadence:
                            temp[j] = s->u.periodic.values[j].u.wristcadence;
                            break;
                        case ambit_log_sample_periodic_type_snr:
                            temp[j] = 0;
                            for (k = 0; k < 16; k++) /* TODO: find out how to store this 16 values */
                                temp[j] |= s->u.periodic.values[j].u.snr[k] << 2 * k;
                            break;
                        case ambit_log_sample_periodic_type_noofsatellites:
                            temp[j] = s->u.periodic.values[j].u.noofsatellites;
                            break;
                        case ambit_log_sample_periodic_type_sealevelpressure:
                            temp[j] = s->u.periodic.values[j].u.sealevelpressure;
                            break;
                        case ambit_log_sample_periodic_type_verticalspeed:
                            temp[j] = s->u.periodic.values[j].u.verticalspeed;
                            break;
                        case ambit_log_sample_periodic_type_cadence:
                            temp[j] = s->u.periodic.values[j].u.cadence;
                            break;
                        case ambit_log_sample_periodic_type_bikepower:
                            temp[j] = s->u.periodic.values[j].u.bikepower;
                            break;
                        case ambit_log_sample_periodic_type_swimingstrokecnt:
                            temp[j] = s->u.periodic.values[j].u.swimingstrokecnt;
                            break;
                        case ambit_log_sample_periodic_type_ruleoutput1:
                            temp[j] = s->u.periodic.values[j].u.ruleoutput1;
                            break;
                        case ambit_log_sample_periodic_type_ruleoutput2:
                            temp[j] = s->u.periodic.values[j].u.ruleoutput2;
                            break;
                        case ambit_log_sample_periodic_type_ruleoutput3:
                            temp[j] = s->u.periodic.values[j].u.ruleoutput3;
                            break;
                        case ambit_log_sample_periodic_type_ruleoutput4:
                            temp[j] = s->u.periodic.values[j].u.ruleoutput4;
                            break;
                        case ambit_log_sample_periodic_type_ruleoutput5:
                            temp[j] = s->u.periodic.values[j].u.ruleoutput5;
                            break;
                    }
                }

                (*env)->SetIntArrayRegion(env, values2, 0, s->u.periodic.value_count, temp);

                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_periodic,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             values, values2);
                (*env)->DeleteLocalRef(env, values);
                (*env)->DeleteLocalRef(env, values2);
                free(temp);
                break;
            case ambit_log_sample_type_logpause:
                __android_log_print(ANDROID_LOG_INFO, TAG, "unsupported sample type: logpause");
                break;
            case ambit_log_sample_type_logrestart:
                __android_log_print(ANDROID_LOG_INFO, TAG, "unsupported sample type: logstart");
                break;
            case ambit_log_sample_type_ibi:
                values = (*env)->NewIntArray(env, s->u.ibi.ibi_count);
                (*env)->SetIntArrayRegion(env, values, 0, s->u.ibi.ibi_count, (jint *)s->u.ibi.ibi);

                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_ibi,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             values);
                (*env)->DeleteLocalRef(env, values);
                break;
            case ambit_log_sample_type_ttff:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_ttff,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.ttff);
                break;
            case ambit_log_sample_type_distance_source:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_distance_source,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.distance_source);
                break;
            case ambit_log_sample_type_lapinfo:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_lapinfo,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.lapinfo.event_type,
                                             s->u.lapinfo.date_time.year, s->u.lapinfo.date_time.month,
                                             s->u.lapinfo.date_time.day, s->u.lapinfo.date_time.hour,
                                             s->u.lapinfo.date_time.minute, s->u.lapinfo.date_time.msec,
                                             s->u.lapinfo.duration, s->u.lapinfo.distance);
                break;
            case ambit_log_sample_type_altitude_source:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_altitude_source,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.altitude_source.source_type, s->u.altitude_source.altitude_offset,
                                             s->u.altitude_source.pressure_offset);
                break;
            case ambit_log_sample_type_gps_base:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_gpsbase,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.gps_base.latitude, s->u.gps_base.longitude,
                                             s->u.gps_base.ehpe, s->u.gps_base.noofsatellites,
                                             s->u.gps_base.navvalid, s->u.gps_base.navtype,
                                             s->u.gps_base.utc_base_time.year, s->u.gps_base.utc_base_time.month,
                                             s->u.gps_base.utc_base_time.day, s->u.gps_base.utc_base_time.hour,
                                             s->u.gps_base.utc_base_time.minute, s->u.gps_base.utc_base_time.msec,
                                             s->u.gps_base.altitude, s->u.gps_base.speed, s->u.gps_base.heading,
                                             s->u.gps_base.hdop);
                break;
            case ambit_log_sample_type_gps_small:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_gpssmall,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.gps_small.latitude, s->u.gps_small.longitude,
                                             s->u.gps_small.ehpe, s->u.gps_small.noofsatellites);
                break;
            case ambit_log_sample_type_gps_tiny:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_gpstiny,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.gps_tiny.latitude, s->u.gps_tiny.longitude,
                                             s->u.gps_tiny.ehpe);
                break;
            case ambit_log_sample_type_time:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_time,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.time.hour, s->u.time.minute, s->u.time.second);
                break;
            case ambit_log_sample_type_swimming_turn:
                values = (*env)->NewIntArray(env, 4);
                (*env)->SetIntArrayRegion(env, values, 0, 4, (jint *)s->u.swimming_turn.classification);

                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_swimming_turn,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.swimming_turn.distance, s->u.swimming_turn.lengths, values,
                                             s->u.swimming_turn.style);
                (*env)->DeleteLocalRef(env, values);
                break;
            case ambit_log_sample_type_swimming_stroke:
                __android_log_print(ANDROID_LOG_INFO, TAG, "unsupported sample type: swimming_stroke");
                break;
            case ambit_log_sample_type_activity:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_activity,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.activity.activitytype, s->u.activity.sportmode);
                break;
            case ambit_log_sample_type_cadence_source:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_cadence_source,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.cadence_source);
                break;
            case ambit_log_sample_type_position:
                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_position,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             s->u.position.latitude, s->u.position.longitude);
                break;
            case ambit_log_sample_type_fwinfo:
                values = (*env)->NewIntArray(env, 4);
                (*env)->SetIntArrayRegion(env, values, 0, 4, (jint *)s->u.fwinfo.version);

                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_fwinfo,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             values, s->u.fwinfo.build_date.year, s->u.fwinfo.build_date.month,
                                             s->u.fwinfo.build_date.day, s->u.fwinfo.build_date.hour,
                                             s->u.fwinfo.build_date.minute, s->u.fwinfo.build_date.msec);
                (*env)->DeleteLocalRef(env, values);
                break;
            case ambit_log_sample_type_unknown:
                char_values = (*env)->NewCharArray(env, s->u.unknown.datalen);
                (*env)->SetCharArrayRegion(env, char_values, 0, s->u.unknown.datalen, (jchar *)s->u.unknown.data);

                (*env)->CallVoidMethod(env, ambit->record_obj, J.add_unknown,
                                             s->time,
                                             s->utc_time.year, s->utc_time.month, s->utc_time.day,
                                             s->utc_time.hour, s->utc_time.minute, s->utc_time.msec,
                                             char_values);
                (*env)->DeleteLocalRef(env, char_values);
            default:
                __android_log_print(ANDROID_LOG_WARN, TAG, "sample type error");
        }

    }

}

/* the progress update callback. It calls Java to save the progress there */
static void log_progress_cb(void *obj, uint16_t log_count, uint16_t log_current, uint8_t progress_percent)
{
    struct ambit_device * ambit = (struct ambit_device *)obj;
    JNIEnv *env = ambit->env;

    /* callback to Java to update progress in AmbitRecord instance where main UI thread can fetch and update UI */
    (*env)->CallVoidMethod(env, ambit->record_obj, J.set_progress, log_count, log_current, progress_percent);
}

/* the function for downloading a log (move) */
static int sync(struct ambit_device *ambit)
{
    int ret;

    libambit_sync_display_show(ambit->obj);
    ret = libambit_log_read(ambit->obj, log_skip_cb, log_data_cb, log_progress_cb, ambit);
    libambit_sync_display_clear(ambit->obj);

    return ret;
}

/* the function for downloading header only */
static int sync_header(struct ambit_device *ambit)
{
    int ret;

    /* add personal setting */
    (*ambit->env)->CallVoidMethod(ambit->env, ambit->record_obj, J.set_personal_setting,
                                  ambit->settings.sportmode_button_lock,
                                  ambit->settings.timemode_button_lock,
                                  ambit->settings.compass_declination,
                                  ambit->settings.units_mode,
                                  ambit->settings.units.pressure,
                                  ambit->settings.units.altitude,
                                  ambit->settings.units.distance,
                                  ambit->settings.units.height,
                                  ambit->settings.units.temperature,
                                  ambit->settings.units.verticalspeed,
                                  ambit->settings.units.weight,
                                  ambit->settings.units.compass,
                                  ambit->settings.units.heartrate,
                                  ambit->settings.units.speed,
                                  ambit->settings.gps_position_format,
                                  ambit->settings.language,
                                  ambit->settings.navigation_style,
                                  ambit->settings.sync_time_w_gps,
                                  ambit->settings.time_format,
                                  ambit->settings.alarm.hour,
                                  ambit->settings.alarm.minute,
                                  ambit->settings.alarm_enable,
                                  ambit->settings.dual_time.hour,
                                  ambit->settings.dual_time.minute,
                                  ambit->settings.date_format,
                                  ambit->settings.tones_mode,
                                  ambit->settings.backlight_mode,
                                  ambit->settings.backlight_brightness,
                                  ambit->settings.display_brightness,
                                  ambit->settings.display_is_negative,
                                  ambit->settings.weight,
                                  ambit->settings.birthyear,
                                  ambit->settings.max_hr,
                                  ambit->settings.rest_hr,
                                  ambit->settings.fitness_level,
                                  ambit->settings.is_male,
                                  ambit->settings.length,
                                  ambit->settings.alti_baro_mode,
                                  ambit->settings.storm_alarm,
                                  ambit->settings.fused_alti_disabled,
                                  ambit->settings.bikepod_calibration,
                                  ambit->settings.bikepod_calibration2,
                                  ambit->settings.bikepod_calibration3,
                                  ambit->settings.footpod_calibration,
                                  ambit->settings.automatic_bikepower_calib,
                                  ambit->settings.automatic_footpod_calib,
                                  ambit->settings.training_program);

    /* read log header */
    libambit_sync_display_show(ambit->obj);
    ret = libambit_log_read(ambit->obj, log_skip_header_cb, log_no_data_cb, log_progress_cb, ambit);
    libambit_sync_display_clear(ambit->obj);

    return ret;
}

/* abort downloading. Not implemented yet */
static void sync_abort(struct ambit_device *ambit, JNIEnv *env)
{
    /* TODO: abort the sync if it's in progress. Not used so not implemented */
}

/* get total entry (move) count by querying the Ambit watch */
static int get_entry_count(struct ambit_device *ambit)
{
    int count = -1;
    uint8_t *reply_data = NULL;
    size_t replylen = 0;

    if (libambit_protocol_command(ambit->obj, ambit_command_log_count, NULL, 0, &reply_data, &replylen, 0) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to read log count");
        return count;
    }
    count = le16toh(*(uint16_t*)(reply_data + 2));
    libambit_protocol_free(reply_data);

    return count;
}

/* exported functions for Java to call */

JNIEXPORT jint JNICALL
Java_idv_markkuo_ambitsync_MainActivity_getBatteryPercent(JNIEnv *env, jclass type, jlong device)
{
    struct ambit_device *ambit = (struct ambit_device *)device;

    if (!ambit)
        return 0;

    if (libambit_device_status_get(ambit->obj, &ambit->status) == 0)
        return ambit->status.charge;

    return 0;
}

JNIEXPORT jlong JNICALL
Java_idv_markkuo_ambitsync_MainActivity_notifyDeviceAttached(JNIEnv *env, jclass type,
                                                                  jint vid, jint pid, jint fd,
                                                                  jstring path_)
{
    struct ambit_device *ambit;
    const char *path = (*env)->GetStringUTFChars(env, path_, 0);

    ambit = setup_ambit(vid, pid, fd, path);

    (*env)->ReleaseStringUTFChars(env, path_, path);

    __android_log_print(ANDROID_LOG_INFO, TAG, "Ambit FD set: %d, path:%s, ambit pointer:%p", fd, path, ambit);

    return (jlong)ambit;
}

void JNICALL
Java_idv_markkuo_ambitsync_MainActivity_notifyDeviceDetached(JNIEnv *env, jclass type,
                                                                 jlong device)
{
    struct ambit_device *ambit = (struct ambit_device *)device;

    kill_ambit(ambit);
}

JNIEXPORT void JNICALL
Java_idv_markkuo_ambitsync_MainActivity_stopSync(JNIEnv *env, jclass type, jlong device) {

    struct ambit_device *ambit = (struct ambit_device *)device;

    sync_abort(ambit, env);
}

JNIEXPORT jint JNICALL
Java_idv_markkuo_ambitsync_MainActivity_startSync(JNIEnv *env, jclass type, jlong device,
                                                        jobject record) {

    struct ambit_device *ambit = (struct ambit_device *)device;

    ambit->record_obj = record;
    ambit->env = env;

    return sync(ambit);
}

JNIEXPORT jint JNICALL
Java_idv_markkuo_ambitsync_MainActivity_syncHeader(JNIEnv *env, jclass type, jlong device,
                                                       jobject record) {

    struct ambit_device *ambit = (struct ambit_device *)device;

    ambit->record_obj = record;
    ambit->env = env;

    return sync_header(ambit);
}

JNIEXPORT jint JNICALL
Java_idv_markkuo_ambitsync_MainActivity_getEntryCount(JNIEnv *env, jclass type, jlong device) {

    struct ambit_device *ambit = (struct ambit_device *)device;

    return get_entry_count(ambit);
}

JNIEXPORT void JNICALL
Java_idv_markkuo_ambitsync_MainActivity_nativeInit(JNIEnv *env, jclass type) {

    /* get the Java class for processing log entry, which is, AmbitRecord */
    jclass cls = (*env)->FindClass(env, "idv/markkuo/ambitlog/AmbitRecord");
    J.cls = (jclass)(*env)->NewGlobalRef(env, cls);

    /* getting all AmbitRecord methods necessary for native to call */

    /* for output current sync progress to Java */
    J.set_progress = (*env)->GetMethodID(env, J.cls, "setSyncProgress", "(III)V");

    /* saving ambit watch personal setting to Java */
    J.set_personal_setting = (*env)->GetMethodID(env, J.cls,
                                                 "setPersonalSetting", "(IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII)V");

    /* Java function for adding header */
    J.add_header = (*env)->GetMethodID(env, J.cls,
                                                     "addHeader", "(Ljava/lang/String;IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII)V");

    /* for determining if we need to skip one entry */
    J.entry_needs_download = (*env)->GetMethodID(env, J.cls,
                                                         "entryNeedsDownload", "(IIIIIII)Z");

    /* Java functions for adding different types of samples */
    J.add_periodic = (*env)->GetMethodID(env, J.cls,
                                                       "addPeriodicSample", "(IIIIIII[I[I)V");
    J.add_ibi = (*env)->GetMethodID(env, J.cls,
                                                  "addIbiSample", "(IIIIIII[I)V");
    J.add_ttff = (*env)->GetMethodID(env, J.cls,
                                                   "addTtffSample", "(IIIIIIII)V");
    J.add_distance_source = (*env)->GetMethodID(env, J.cls,
                                                              "addDistanceSourceSample", "(IIIIIIII)V");
    J.add_lapinfo = (*env)->GetMethodID(env, J.cls,
                                                      "addLapInfoSample", "(IIIIIIIIIIIIIIII)V");
    J.add_altitude_source = (*env)->GetMethodID(env, J.cls,
                                                              "addAltitudeSourceSample", "(IIIIIIIIII)V");
    J.add_position = (*env)->GetMethodID(env, J.cls,
                                                       "addPositionSample", "(IIIIIIIII)V");
    J.add_gpstiny = (*env)->GetMethodID(env, J.cls,
                                                      "addGPSTinySample", "(IIIIIIIIII)V");
    J.add_gpssmall = (*env)->GetMethodID(env, J.cls,
                                                       "addGPSSmallSample", "(IIIIIIIIIII)V");
    J.add_gpsbase = (*env)->GetMethodID(env, J.cls,
                                                      "addGPSBaseSample", "(IIIIIIIIIIIIIIIIIIIIIII)V");
    J.add_time = (*env)->GetMethodID(env, J.cls,
                                                   "addTimeSample", "(IIIIIIIIII)V");
    J.add_swimming_turn = (*env)->GetMethodID(env, J.cls,
                                                            "addSwimmingTurnSample", "(IIIIIIIII[II)V");
    J.add_activity = (*env)->GetMethodID(env, J.cls,
                                                       "addActivitySample", "(IIIIIIIII)V");
    J.add_cadence_source = (*env)->GetMethodID(env, J.cls,
                                                             "addCadenceSourceSample", "(IIIIIIII)V");
    J.add_fwinfo = (*env)->GetMethodID(env, J.cls,
                                                     "addFWInfoSourceSample", "(IIIIIII[IIIIIII)V");
    J.add_unknown = (*env)->GetMethodID(env, J.cls,
                                                      "addUnknownSample", "(IIIIIII[C)V");
}

