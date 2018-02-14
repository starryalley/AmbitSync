package idv.markkuo.ambitlog;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Represents a log entry's header
 */
public class LogHeader implements Parcelable, Serializable {
    Date datetime;
    int duration;              /* ms */
    int ascent;                /* m */
    int descent;               /* m */
    int ascent_time;           /* ms */
    int descent_time;          /* ms */
    int recovery_time;         /* ms */
    int speed_avg;             /* m/h */
    int speed_max;             /* m/h */
    int speed_max_time;        /* ms */
    int altitude_max;          /* m */
    int altitude_min;          /* m */
    int altitude_max_time;     /* ms */
    int altitude_min_time;     /* ms */
    int heartrate_avg;         /* bpm */
    int heartrate_max;         /* bpm */
    int heartrate_min;         /* bpm */
    int heartrate_max_time;    /* ms */
    int heartrate_min_time;    /* ms */
    int peak_training_effect;  /* effect scale 0.1 */
    int activity_type;
    String activity_name;         /* name of activity in UTF-8 */
    int temperature_max;       /* degree celsius scale 0.1 */
    int temperature_min;       /* degree celsius scale 0.1 */
    int temperature_max_time;  /* ms */
    int temperature_min_time;  /* ms */
    int distance;              /* m */
    int samples_count;         /* number of samples in log */
    int energy_consumption;    /* kcal */
    int first_fix_time;        /* ms */
    int battery_start;         /* percent */
    int battery_end;           /* percent */
    int distance_before_calib; /* m */

    int cadence_max;           /* rpm */
    int cadence_avg;           /* rpm */
    int swimming_pool_lengths;
    int cadence_max_time;      /* ms */
    int swimming_pool_length;  /* m */

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    public LogHeader() {}

    private static String formatDuration(int dur) {
        int s = dur / 1000; //ms to sec
        if (s/3600 >= 1)
            return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
        else
            return String.format("%02d:%02d", (s % 3600) / 60, s % 60);
    }

    public String toString() {
        return "[" + activity_name + "] @ " + sdf.format(datetime) + "(" + formatDuration(duration) + ")";
    }

    public String getMoveDetail() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(),"%.1f", distance/1000.0)).append("km ")
          .append("Elevation: ").append(ascent).append("m ");
        if (heartrate_avg != 0)
            sb.append("HR:").append(heartrate_avg).append(" bpm\n");
        else
            sb.append("\n");
        sb.append("AvgSpd: ").append(String.format(Locale.getDefault(), "%.1f km/h ", speed_avg/1000.0));
        if (peak_training_effect != 0)
            sb.append("PTE:").append(String.format(Locale.getDefault(), "%.1f ", peak_training_effect/10.0));

        return sb.toString();
    }

    public String getMoveType() {
        return activity_name;
    }

    public String getMoveTime() {
        return sdf.format(datetime);
    }

    public String getMoveDuration() { return formatDuration(duration); }

    public String getMovescountFilePrefix() {
        // yeah, make it similar to Movescount's GPX export filename
        return "Move_" + sdf2.format(datetime) + "_" + activity_name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(datetime.getTime());
        out.writeInt(duration);
        out.writeInt(ascent);
        out.writeInt(descent);
        out.writeInt(ascent_time);
        out.writeInt(descent_time);
        out.writeInt(recovery_time);
        out.writeInt(speed_avg);
        out.writeInt(speed_max);
        out.writeInt(speed_max_time);
        out.writeInt(altitude_max);
        out.writeInt(altitude_min);
        out.writeInt(altitude_max_time);
        out.writeInt(altitude_min_time);
        out.writeInt(heartrate_avg);
        out.writeInt(heartrate_max);
        out.writeInt(heartrate_min);
        out.writeInt(heartrate_max_time);
        out.writeInt(heartrate_min_time);
        out.writeInt(peak_training_effect);
        out.writeInt(activity_type);
        out.writeString(activity_name);
        out.writeInt(temperature_max);
        out.writeInt(temperature_min);
        out.writeInt(temperature_max_time);
        out.writeInt(temperature_min_time);
        out.writeInt(distance);
        out.writeInt(samples_count);
        out.writeInt(energy_consumption);
        out.writeInt(first_fix_time);
        out.writeInt(battery_start);
        out.writeInt(battery_end);
        out.writeInt(distance_before_calib);
        out.writeInt(cadence_max);
        out.writeInt(cadence_avg);
        out.writeInt(swimming_pool_length);
        out.writeInt(cadence_max_time);
        out.writeInt(swimming_pool_lengths);
    }


    private LogHeader(Parcel in) {
        datetime = new Date(in.readLong());
        duration = in.readInt();
        ascent = in.readInt();
        descent = in.readInt();
        ascent_time = in.readInt();
        descent_time = in.readInt();
        recovery_time = in.readInt();
        speed_avg = in.readInt();
        speed_max = in.readInt();
        speed_max_time = in.readInt();
        altitude_max = in.readInt();
        altitude_min = in.readInt();
        altitude_max_time = in.readInt();
        altitude_min_time = in.readInt();
        heartrate_avg = in.readInt();
        heartrate_max = in.readInt();
        heartrate_min = in.readInt();
        heartrate_max_time = in.readInt();
        heartrate_min_time = in.readInt();
        peak_training_effect = in.readInt();
        activity_type = in.readInt();
        activity_name = in.readString();
        temperature_max = in.readInt();
        temperature_min = in.readInt();
        temperature_max_time = in.readInt();
        temperature_min_time = in.readInt();
        distance = in.readInt();
        samples_count = in.readInt();
        energy_consumption = in.readInt();
        first_fix_time = in.readInt();
        battery_start = in.readInt();
        battery_end = in.readInt();
        distance_before_calib = in.readInt();
        cadence_max = in.readInt();
        cadence_avg = in.readInt();
        swimming_pool_length = in.readInt();
        cadence_max_time = in.readInt();
        swimming_pool_lengths = in.readInt();
    }

    public static final Parcelable.Creator<LogHeader> CREATOR
            = new Parcelable.Creator<LogHeader>() {
        public LogHeader createFromParcel(Parcel in) {
            return new LogHeader(in);
        }

        public LogHeader[] newArray(int size) {
            return new LogHeader[size];
        }
    };
}