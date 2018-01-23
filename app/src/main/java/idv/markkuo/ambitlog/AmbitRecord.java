package idv.markkuo.ambitlog;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

// represents the personal settings stored on the Ambit watch
// TODO: should be Parcelable instead of Serializable. Getting lazy here..
class PersonalSetting implements Serializable {
    int  sportmode_button_lock;
    int  timemode_button_lock;
    int compass_declination;
    int  units_mode;

    int pressure_unit;
    int altitude_unit;
    int distance_unit;
    int height_unit;
    int temperature_unit;
    int verticalspeed_unit;
    int weight_unit;
    int compass_unit;
    int heartrate_unit;
    int speed_unit;

    int gps_position_format;
    int language;
    int navigation_style;
    int sync_time_w_gps;
    int time_format;

    int alarm_hour;
    int alarm_minute;
    int alarm_enable;

    int dual_time_hour;
    int dual_time_minute;

    int date_format;
    int tones_mode;
    int backlight_mode;
    int backlight_brightness;
    int display_brightness;
    int display_is_negative;
    int weight;                  // kg scale 0.01
    int birthyear;
    int max_hr;
    int rest_hr;
    int fitness_level;
    int is_male;
    int length;
    int alti_baro_mode;
    int storm_alarm;
    int fused_alti_disabled;
    int bikepod_calibration;     // scale 0.0001
    int bikepod_calibration2;    // scale 0.0001
    int bikepod_calibration3;    // scale 0.0001
    int footpod_calibration;     // scale 0.0001
    int automatic_bikepower_calib;
    int automatic_footpod_calib;
    int training_program;
}

/**
 * The class which stores all log (could be header-only if not downloaded) currently present in
 * the Ambit watch. This class is the main log access point from outside world.
 *
 * The "Moves" are stored in "entries" ArrayList
 *
 * Note: Used both by native code and Java
 */
public class AmbitRecord implements Parcelable {
    private static final String TAG = "AmbitRecord";

    public static PersonalSetting setting = new PersonalSetting();

    // this in the main structure which stores all Ambit records in the watch
    private ArrayList<LogEntry> entries = new ArrayList<LogEntry>();

    // reference to current processing LogEntry
    private LogEntry currentEntry;

    // storing current sync progress (%), used for UI update
    private int currentSyncProgress;

    public AmbitRecord() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeSerializable(setting);
        out.writeList(entries);
        out.writeParcelable(currentEntry, flags);
    }

    private AmbitRecord(Parcel in) {
        setting = (PersonalSetting) in.readSerializable();
        entries = in.readArrayList(LogEntry.class.getClassLoader());
        currentEntry = in.readParcelable(LogEntry.class.getClassLoader());
    }

    public static final Parcelable.Creator<AmbitRecord> CREATOR
            = new Parcelable.Creator<AmbitRecord>() {
        public AmbitRecord createFromParcel(Parcel in) {
            return new AmbitRecord(in);
        }

        public AmbitRecord[] newArray(int size) {
            return new AmbitRecord[size];
        }
    };

    public ArrayList<LogEntry> getEntries() {
        return entries;
    }


    // query current stored records and print to Android logcat
    // TODO: find a place to get it called and printed. Not used.
    public void query() {
        Log.d(TAG, "Total Entry: " + entries.size());
        Log.d(TAG, "======================");
        for (LogEntry e: entries) {
            Log.d(TAG, e.toString());
            e.query();
        }
    }

    // get a reference to an entry by search for the time and duration
    private LogEntry getEntry(int year, int month, int day, int hour, int minute, int msec,
                                     int duration) {
        for (LogEntry e: entries) {
            if (e.resemble(year, month, day, hour, minute, msec, duration))
                return e;
        }
        return null;
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public boolean entryNeedsDownload(int year, int month, int day, int hour, int minute, int msec,
                                      int duration) {
        LogEntry e = getEntry(year, month, day, hour, minute, msec, duration);
        if (e == null) {
            Log.w(TAG, "oops, no matching entry in record!! Something might be wrong");
            return false;
        }
        // entry not added because there is header only
        if (e.samples.size() > 0)
            return false; //already downloaded
        if (e.isToSync() && e.samples.size() == 0) {
            return true;
        }

        return false;
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void setSyncProgress(int logCount, int logCurrent, int percent) {
        Log.v(TAG, String.format("[%d%%]:%d/%d", percent, logCurrent, logCount));
        currentSyncProgress = percent;
    }

    public int getCurrentSyncProgress() { return currentSyncProgress; }


    /**
     * Callback from native side. Should not be called in Java!
     */
    public void setPersonalSetting(int sportmode_button_lock,
                                   int timemode_button_lock,
                                   int compass_declination,
                                   int units_mode,
                                   int pressure_unit,
                                   int altitude_unit,
                                   int distance_unit,
                                   int height_unit,
                                   int temperature_unit,
                                   int verticalspeed_unit,
                                   int weight_unit,
                                   int compass_unit,
                                   int heartrate_unit,
                                   int speed_unit,
                                   int gps_position_format,
                                   int language,
                                   int navigation_style,
                                   int sync_time_w_gps,
                                   int time_format,
                                   int alarm_hour,
                                   int alarm_minute,
                                   int alarm_enable,
                                   int dual_time_hour,
                                   int dual_time_minute,
                                   int date_format,
                                   int tones_mode,
                                   int backlight_mode,
                                   int backlight_brightness,
                                   int display_brightness,
                                   int display_is_negative,
                                   int weight,                  // kg scale 0.01
                                   int birthyear,
                                   int max_hr,
                                   int rest_hr,
                                   int fitness_level,
                                   int is_male,
                                   int length,
                                   int alti_baro_mode,
                                   int storm_alarm,
                                   int fused_alti_disabled,
                                   int bikepod_calibration,     // scale 0.0001
                                   int bikepod_calibration2,    // scale 0.0001
                                   int bikepod_calibration3,    // scale 0.0001
                                   int footpod_calibration,     // scale 0.0001
                                   int automatic_bikepower_calib,
                                   int automatic_footpod_calib,
                                   int training_program) {
        setting.sportmode_button_lock = sportmode_button_lock;
        setting.timemode_button_lock = timemode_button_lock;
        setting.compass_declination = compass_declination;
        setting.units_mode = units_mode;
        setting.pressure_unit = pressure_unit;
        setting.altitude_unit = altitude_unit;
        setting.distance_unit = distance_unit;
        setting.height_unit = height_unit;
        setting.temperature_unit = temperature_unit;
        setting.verticalspeed_unit = verticalspeed_unit;
        setting.weight_unit = weight_unit;
        setting.compass_unit = compass_unit;
        setting.heartrate_unit = heartrate_unit;
        setting.speed_unit = speed_unit;
        setting.gps_position_format = gps_position_format;
        setting.language = language;
        setting.navigation_style = navigation_style;
        setting.sync_time_w_gps = sync_time_w_gps;
        setting.time_format = time_format;
        setting.alarm_hour = alarm_hour;
        setting.alarm_minute = alarm_minute;
        setting.alarm_enable = alarm_enable;
        setting.dual_time_hour = dual_time_hour;
        setting.dual_time_minute = dual_time_minute;
        setting.date_format = date_format;
        setting.tones_mode = tones_mode;
        setting.backlight_mode = backlight_mode;
        setting.backlight_brightness = backlight_brightness;
        setting.display_brightness = display_brightness;
        setting.display_is_negative = display_is_negative;
        setting.weight = weight;
        setting.birthyear = birthyear;
        setting.max_hr = max_hr;
        setting.rest_hr = rest_hr;
        setting.fitness_level = fitness_level;
        setting.is_male = is_male;
        setting.length = length;
        setting.alti_baro_mode = alti_baro_mode;
        setting.storm_alarm = storm_alarm;
        setting.fused_alti_disabled = fused_alti_disabled;
        setting.bikepod_calibration = bikepod_calibration;
        setting.bikepod_calibration2 = bikepod_calibration2;
        setting.bikepod_calibration3 = bikepod_calibration3;
        setting.footpod_calibration = footpod_calibration;
        setting.automatic_bikepower_calib = automatic_bikepower_calib;
        setting.automatic_footpod_calib = automatic_footpod_calib;
        setting.training_program = training_program;
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addHeader(String activity_name,
                                 int year, int month, int day, int hour, int minute, int msec,
                                 int duration,
                                 int ascent,
                                 int descent,
                                 int ascent_time,
                                 int descent_time,
                                 int recovery_time,
                                 int speed_avg,
                                 int speed_max,
                                 int speed_max_time,
                                 int altitude_max,
                                 int altitude_min,
                                 int altitude_max_time,
                                 int altitude_min_time,
                                 int heartrate_avg,
                                 int heartrate_max,
                                 int heartrate_min,
                                 int heartrate_max_time,
                                 int heartrate_min_time,
                                 int peak_training_effect,
                                 int activity_type,
                                 int temperature_max,
                                 int temperature_min,
                                 int temperature_max_time,
                                 int temperature_min_time,
                                 int distance,
                                 int samples_count,
                                 int energy_consumption,
                                 int first_fix_time,
                                 int battery_start,
                                 int battery_end,
                                 int distance_before_calib,
                                 int cadence_max,
                                 int cadence_avg,
                                 int swimming_pool_lengths,
                                 int cadence_max_time,
                                 int swimming_pool_length) {

        LogEntry e = getEntry(year, month, day, hour, minute, msec, duration);
        LogHeader h;
        if (e == null) {
            h = new LogHeader();
            e = new LogEntry(h);
            entries.add(e);
            Log.v(TAG, "addHeader: created a new LogEntry");
        } else {
            h = e.header;
            Log.v(TAG, "addHeader: using an added LogEntry");
        }

        // set header
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, minute, msec/1000);
        h.datetime = (Date)c.getTime().clone();
        h.duration = duration;
        h.ascent = ascent;
        h.descent = descent;
        h.ascent_time=ascent_time;
        h.descent_time=descent_time;
        h.recovery_time=recovery_time;
        h.speed_avg=speed_avg;
        h.speed_max=speed_max;
        h.speed_max_time=speed_max_time;
        h.altitude_max=altitude_max;
        h.altitude_min=altitude_min;
        h.altitude_max_time=altitude_max_time;
        h.altitude_min_time=altitude_min_time;
        h.heartrate_avg=heartrate_avg;
        h.heartrate_max=heartrate_max;
        h.heartrate_min=heartrate_min;
        h.heartrate_max_time=heartrate_max_time;
        h.heartrate_min_time=heartrate_min_time;
        h.peak_training_effect=peak_training_effect;
        h.activity_type=activity_type;
        h.activity_name=activity_name;
        h.temperature_max=temperature_max;
        h.temperature_min=temperature_min;
        h.temperature_max_time=temperature_max_time;
        h.temperature_min_time=temperature_min_time;
        h.distance=distance;
        h.samples_count=samples_count;
        h.energy_consumption=energy_consumption;
        h.first_fix_time=first_fix_time;
        h.battery_start=battery_start;
        h.battery_end=battery_end;
        h.distance_before_calib=distance_before_calib;
        h.cadence_max=cadence_max;
        h.cadence_avg=cadence_avg;
        h.swimming_pool_lengths=swimming_pool_lengths;
        h.cadence_max_time=cadence_max_time;
        h.swimming_pool_length=swimming_pool_length;

        //remember this current entry
        currentEntry = e;

        Log.v(TAG, "Header: " + h + " Details:" + h.getMoveDetail());
    }

    /**
     * Process common part in a LogSample
     */
    private void setCommonSample(LogSample s, int time,
                                 int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec) {
        s.time = time;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(u_year, u_month - 1, u_day, u_hour, u_minute, u_msec / 1000);
        s.utc_time = (Date)c.getTime().clone();

        //calculate actual time by adding "time" as msec to header's datetime for this sample
        c.clear();
        c.setTime(currentEntry.header.datetime);
        c.add(Calendar.MILLISECOND, time);
        s.actual_time = (Date)c.getTime().clone();
    }

    /**
     * Add the LogSample with LogEntry
     */
    private void addSample(LogSample s) {
        currentEntry.samples.add(s);
        Log.v(TAG, "== " + s + " in " + currentEntry.header + " count:" + currentEntry.samples.size() + " ==");
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addPeriodicSample(int time,
                                 int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                 int types[], int values[]) {
        PeriodicLogSample s = new PeriodicLogSample();
        int i;

        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        for (i = 0; i < types.length; i++) {
            PeriodicValue v = new PeriodicValue();
            v.type = types[i];
            v.value = values[i];
            s.values.add(v);
        }

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addIbiSample(int time,
                                         int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                         int ibi[]) {
        IbiLogSample s = new IbiLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.ibi = ibi;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addTtffSample(int time,
                                    int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                    int ttff) {
        TtffLogSample s = new TtffLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.ttff = ttff;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addDistanceSourceSample(int time,
                                     int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                     int distance_source) {
        DistanceSourceLogSample s = new DistanceSourceLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.distance_source = distance_source;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addLapInfoSample(int time,
                                       int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                       int event_type,
                                       int e_year, int e_month, int e_day, int e_hour, int e_minute, int e_msec,
                                        int duration, int distance) {
        LapInfoLogSample s = new LapInfoLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(e_year, e_month - 1, e_day, e_hour, e_minute, e_msec/1000);

        s.event_type = event_type;
        s.date_time = (Date)c.getTime().clone();
        s.duration = duration;
        s.distance = distance;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addAltitudeSourceSample(int time,
                                               int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                               int source_type, int altitude_offset, int pressure_offset) {
        AltitudeSourceLogSample s = new AltitudeSourceLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.source_type = source_type;
        s.altitute_offset = altitude_offset;
        s.pressure_offset = pressure_offset;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addPositionSample(int time,
                                               int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                               int latitude, int longitude) {
        PositionLogSample s = new PositionLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.latitude = latitude;
        s.longitude = longitude;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addGPSTinySample(int time,
                                         int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                         int latitude, int longitude, int ehpe) {
        GPSTinyLogSample s = new GPSTinyLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.latitude = latitude;
        s.longitude = longitude;
        s.ehpe = ehpe;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addGPSSmallSample(int time,
                                        int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                        int latitude, int longitude, int ehpe, int noofsatellites) {
        GPSSmallLogSample s = new GPSSmallLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.latitude = latitude;
        s.longitude = longitude;
        s.ehpe = ehpe;
        s.noofsatellites = noofsatellites;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addGPSBaseSample(int time,
                                         int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                        int latitude, int longitude, int ehpe, int noofsatellites,
                                        int navvalid, int navtype,
                                        int b_year, int b_month, int b_day, int b_hour, int b_minute, int b_msec,
                                        int altitude, int speed, int heading, int hdop

                                        ) {
        GPSBaseLogSample s = new GPSBaseLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(b_year, b_month - 1, b_day, b_hour, b_minute, b_msec/1000);

        s.latitude = latitude;
        s.longitude = longitude;
        s.ehpe = ehpe;
        s.noofsatellites = noofsatellites;

        s.navvalid = navvalid;
        s.navtype = navtype;
        s.utc_base_time = (Date)c.getTime().clone();
        s.altitude = altitude;
        s.speed = speed;
        s.heading = heading;
        s.hdop = hdop;

        //TODO: add Vector<Satellite>

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addTimeSample(int time,
                                               int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                               int hour, int minute, int second) {
        TimeLogSample s = new TimeLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.hour = hour;
        s.minute = minute;
        s.second = second;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addSwimmingTurnSample(int time,
                                     int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                     int distance, int lengths, int classifications[], int style) {
        SwimmingTurnLogSample s = new SwimmingTurnLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.distance = distance;
        s.lengths = lengths;
        s.classification = classifications;
        s.style = style;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addActivitySample(int time,
                                    int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                    int activity_type, int custom_mode) {
        ActivityLogSample s = new ActivityLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.activity_type = activity_type;
        s.custom_mode = custom_mode;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addCadenceSourceSample(int time,
                                         int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                         int cadence_source) {
        CadenceSourceLogSample s = new CadenceSourceLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.cadence_source = cadence_source;

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addFWInfoSourceSample(int time,
                                              int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                              int version[],
                                             int b_year, int b_month, int b_day, int b_hour, int b_minute, int b_msec) {
        FWInfoLogSample s = new FWInfoLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(b_year, b_month - 1, b_day, b_hour, b_minute, b_msec/1000);

        s.version = version;
        s.build_date = (Date)c.getTime().clone();

        addSample(s);
    }

    /**
     * Callback from native side. Should not be called in Java!
     */
    public void addUnknownSample(int time,
                                              int u_year, int u_month, int u_day, int u_hour, int u_minute, int u_msec,
                                              char data[]) {
        UnknownLogSample s = new UnknownLogSample();
        setCommonSample(s, time, u_year, u_month, u_day, u_hour, u_minute, u_msec);

        s.data = data;

        addSample(s);
    }
}