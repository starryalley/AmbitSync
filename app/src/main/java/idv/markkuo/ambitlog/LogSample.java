package idv.markkuo.ambitlog;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;
import java.util.zip.DeflaterOutputStream;

class SAMPLE_TYPE{
    public static final int UNKNOWN = 0xf000;
    public static final int PERIODIC = 0x0200;
    public static final int LOGPAUSE = 0x0304; //not implemented
    public static final int LOGRESTART = 0x0305; //not implemented
    public static final int IBI = 0x0306;
    public static final int TTFF = 0x0307;
    public static final int DISTANCE_SOURCE = 0x0308;
    public static final int LAP_INFO = 0x0309;
    public static final int ALTITUDE_SOURCE = 0x030d;
    public static final int GPS_BASE = 0x030f;
    public static final int GPS_SMALL = 0x0310;
    public static final int GPS_TINY = 0x0311;
    public static final int TIME = 0x0312;
    public static final int SWIMMING_TURN = 0x0314;
    public static final int SWIMMING_STROKE = 0x0315; //not implemented
    public static final int ACTIVITY = 0x0318;
    public static final int CADENCE_SOURCE = 0x031a;
    public static final int POSITION = 0x031b;
    public static final int FW_INFO = 0x031c;
}

// interface for a storing a log sample read directly from Ambit
abstract class LogSampleInterface implements Parcelable, Serializable {
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public abstract String toString();

    @Override
    public int describeContents() {
        return 0;
    }
}

// the base class for a log sample
public class LogSample extends LogSampleInterface {
    protected static final String TAG = "LogSample";
    protected int type = SAMPLE_TYPE.UNKNOWN; //LogSample type
    int time; //time from zero
    Date utc_time;
    Date actual_time;

    public String toString() {
        return "";
    }

    public JSONObject toJSON() throws JSONException { return null; }

    public LogSample() {}

    public JSONObject createJSONWithTime() {
        JSONObject j = new JSONObject(); // JSON representation of this sample
        try {
            j.put("LocalTime", sdf.format(actual_time));
            j.put("UTCTime", sdf.format(utc_time));
        } catch (JSONException e) {
            Log.w(TAG, e);
        }
        return j;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(type);
        out.writeInt(time);
        out.writeSerializable(utc_time);
        out.writeSerializable(actual_time);
    }

    protected void readFromParcel(Parcel in) {
        type = in.readInt();
        time = in.readInt();
        utc_time = (Date) in.readSerializable();
        actual_time = (Date) in.readSerializable();
    }

    private LogSample(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<LogSample> CREATOR
            = new Parcelable.Creator<LogSample>() {
        public LogSample createFromParcel(Parcel in) {
            return new LogSample(in);
        }

        public LogSample[] newArray(int size) {
            return new LogSample[size];
        }
    };
}

class PeriodicValue implements Parcelable {
    int type;
    int value;

    public PeriodicValue() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(type);
        out.writeInt(value);
    }

    private PeriodicValue(Parcel in) {
        type = in.readInt();
        value = in.readInt();
    }

    public static final Parcelable.Creator<PeriodicValue> CREATOR
            = new Parcelable.Creator<PeriodicValue>() {
        public PeriodicValue createFromParcel(Parcel in) {
            return new PeriodicValue(in);
        }

        public PeriodicValue[] newArray(int size) {
            return new PeriodicValue[size];
        }
    };
}

class PeriodicLogSample extends LogSample {
    private final static String TAG = "PeriodicLogSample";
    public PeriodicLogSample() { type = SAMPLE_TYPE.PERIODIC; }

    public static final int TYPE_LATITUDE = 0x01;
    public static final int TYPE_LONGITUDE = 0x02;
    public static final int TYPE_DISTANCE = 0x03;
    public static final int TYPE_SPEED = 0x04;
    public static final int TYPE_HR = 0x05;
    public static final int TYPE_TIME = 0x06;
    public static final int TYPE_GPSSPEED = 0x07;
    public static final int TYPE_WRISTACCSPEED = 0x08;
    public static final int TYPE_BIKEPODSPEED = 0x09;
    public static final int TYPE_EHPE = 0x0A;
    public static final int TYPE_EVPE = 0x0B;
    public static final int TYPE_ALTITUDE = 0x0c;
    public static final int TYPE_ABSPRESSURE = 0x0d;
    public static final int TYPE_ENERGY = 0x0E;
    public static final int TYPE_TEMPERATURE = 0x0f;
    public static final int TYPE_CHARGE = 0x10;
    public static final int TYPE_GPSALTITUDE = 0x11;
    public static final int TYPE_GPSHEADING = 0x12;
    public static final int TYPE_GPSHDOP = 0x13;
    public static final int TYPE_GPSVDOP = 0x14;
    public static final int TYPE_WRISTCADENCE = 0x15;
    public static final int TYPE_SNR = 0x16;
    public static final int TYPE_NOOFSATELLITES = 0x17;
    public static final int TYPE_SEALEVELPRESSURE = 0x18;
    public static final int TYPE_VERTICALSPEED = 0x19;
    public static final int TYPE_CADENCE = 0x1A;
    public static final int TYPE_BIKEPOWER = 0x1f;
    public static final int TYPE_SWIMINGSTROKECNT = 0x20;
    public static final int TYPE_RULEOUTPUT1 = 0x64;
    public static final int TYPE_RULEOUTPUT2 = 0x65;
    public static final int TYPE_RULEOUTPUT3 = 0x66;
    public static final int TYPE_RULEOUTPUT4 = 0x67;
    public static final int TYPE_RULEOUTPUT5 = 0x68;

    ArrayList<PeriodicValue> values = new ArrayList<>();

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeList(values);
    }

    private PeriodicLogSample(Parcel in) {
        readFromParcel(in);
        values = in.readArrayList(PeriodicValue.class.getClassLoader());
    }

    public static final Parcelable.Creator<PeriodicLogSample> CREATOR
            = new Parcelable.Creator<PeriodicLogSample>() {
        public PeriodicLogSample createFromParcel(Parcel in) {
            return new PeriodicLogSample(in);
        }

        public PeriodicLogSample[] newArray(int size) {
            return new PeriodicLogSample[size];
        }
    };

    public String toString() { return "[Periodic]"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        int i, temp;
        StringBuilder sb = new StringBuilder();

        j.put("SampleType", toString());
        //ref:writePeriodicSample() in movescountjson.cpp:867

        for (PeriodicValue v : values) {
            j.put("PeriodicType", v.type);
            switch (v.type) {
                case TYPE_LATITUDE:
                    j.put("Latitude", (double) v.value / 10000000);
                    break;
                case TYPE_LONGITUDE:
                    j.put("Longitude", (double) v.value / 10000000);
                    break;
                case TYPE_DISTANCE:
                    if (v.value != 0xffffffff)
                        j.put("Distance", v.value);
                    break;
                case TYPE_SPEED:
                    if (v.value != 0xffff)
                        j.put("Speed", (double) v.value / 100.0);
                    break;
                case TYPE_HR:
                    if (v.value != 0xff)
                        j.put("HR", (double) v.value);
                    break;
                case TYPE_TIME:
                    j.put("Time", (double) v.value / 1000.0);
                    break;
                case TYPE_GPSSPEED:
                    if (v.value != 0xffff)
                        j.put("GPSSpeed", (double) v.value / 100.0);
                    break;
                case TYPE_WRISTACCSPEED:
                    if (v.value != 0xffff)
                        j.put("WristAccSpeed", (double) v.value / 100.0);
                    break;
                case TYPE_BIKEPODSPEED:
                    if (v.value != 0xffff)
                        j.put("BikePodSpeed", (double) v.value / 100.0);
                    break;
                case TYPE_EHPE:
                    j.put("EHPE", v.value);
                    break;
                case TYPE_EVPE:
                    j.put("EVPE", v.value);
                    break;
                case TYPE_ALTITUDE:
                    if (v.value >= -1000 && v.value <= 10000)
                        j.put("Altitude", v.value);
                    break;
                case TYPE_ABSPRESSURE:
                    j.put("AbsPressure", v.value / 10.0);
                    break;
                case TYPE_ENERGY:
                    j.put("EnergyConsumption", (double)v.value / 10.0);
                    break;
                case TYPE_TEMPERATURE:
                    if (v.value >= -1000 && v.value <= 1000)
                        j.put("Temperature", (double)v.value / 10.0);
                    break;
                case TYPE_CHARGE:
                    if (v.value <= 100)
                        j.put("BatteryCharge", (double)v.value / 100.0);
                    break;
                case TYPE_GPSALTITUDE:
                    if (v.value >= -1000 && v.value <= 10000)
                        j.put("GPSAltitude", v.value);
                    break;
                case TYPE_GPSHEADING:
                    if (v.value != 0xffff)
                        j.put("GPSHeading", (double) v.value / 10000000);
                    //j.put("GPSHeading", (double) v.value * Math.PI / 180 / 10000000);
                    break;
                case TYPE_GPSHDOP:
                    if (v.value != 0xff)
                        j.put("GpsHDOP", v.value);
                    break;
                case TYPE_GPSVDOP:
                    if (v.value != 0xff)
                        j.put("GpsVDOP", v.value);
                    break;
                case TYPE_WRISTCADENCE:
                    if (v.value != 0xffff)
                        j.put("WristCadence", v.value);
                    break;
                case TYPE_SNR: {
                    for (i = 0; i < 16; i++) {
                        temp = (v.value >> (i * 2)) & 0x3; //assume each SNR value is 2 bits
                        sb.append(String.format("%02x", temp));
                    }
                    j.put("SNR", sb.toString());
                    break;
                }
                case TYPE_NOOFSATELLITES:
                    if (v.value != 0xff)
                        j.put("NumberOfSatellites", v.value);
                    break;
                case TYPE_SEALEVELPRESSURE:
                    if (v.value >= 8500 && v.value < 11000)
                        j.put("SeaLevelPressure", v.value / 10.0);
                    break;
                case TYPE_VERTICALSPEED:
                    j.put("VerticalSpeed", (double)v.value / 100.0);
                    break;
                case TYPE_CADENCE:
                    if (v.value != 0xff)
                        j.put("Cadence", v.value);
                    break;
                case TYPE_BIKEPOWER:
                    if (v.value != 0xffff)
                        j.put("BikePower", v.value);
                    break;
                case TYPE_SWIMINGSTROKECNT:
                    j.put("SwimmingStrokeCount", v.value);
                    break;
                case TYPE_RULEOUTPUT1:
                    if (v.value != -2147483648)
                        j.put("RuleOutput1", v.value);
                    break;
                case TYPE_RULEOUTPUT2:
                    if (v.value != -2147483648)
                        j.put("RuleOutput2", v.value);
                    break;
                case TYPE_RULEOUTPUT3:
                    if (v.value != -2147483648)
                        j.put("RuleOutput3", v.value);
                    break;
                case TYPE_RULEOUTPUT4:
                    if (v.value != -2147483648)
                        j.put("RuleOutput4", v.value);
                    break;
                case TYPE_RULEOUTPUT5:
                    if (v.value != -2147483648)
                        j.put("RuleOutput5", v.value);
                    break;
            }
        } //end of for

        return j;
    }
}

class IbiLogSample extends LogSample {
    public IbiLogSample() { type = SAMPLE_TYPE.IBI; }
    int ibi[];

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeIntArray(ibi);
    }

    private IbiLogSample(Parcel in) {
        readFromParcel(in);
        in.readIntArray(ibi);
    }

    public static final Parcelable.Creator<IbiLogSample> CREATOR
            = new Parcelable.Creator<IbiLogSample>() {
        public IbiLogSample createFromParcel(Parcel in) {
            return new IbiLogSample(in);
        }

        public IbiLogSample[] newArray(int size) {
            return new IbiLogSample[size];
        }
    };

    public String toString() { return "[Ibi]"; }

    public JSONObject toJSON() throws JSONException {
        JSONArray a = new JSONArray(Arrays.asList(ibi));
        JSONObject j = new JSONObject();
        j.put("SampleType", toString());
        try {
            byte[] b64 = Base64.encode(compress(a.toString().getBytes()), Base64.DEFAULT);
            j.put("IBIData", b64.toString());//TODO: well, no idea what this is. Not used anyway
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return j;
    }

    private static byte[] compress(byte[] input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(out);
        try {
            dos.write(input);
        } finally {
            dos.close();
        }
        return out.toByteArray();
    }

}

class TtffLogSample extends LogSample {
    public TtffLogSample() { type = SAMPLE_TYPE.TTFF; }
    int ttff;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(ttff);
    }

    private TtffLogSample(Parcel in) {
        readFromParcel(in);
        ttff = in.readInt();
    }

    public static final Parcelable.Creator<TtffLogSample> CREATOR
            = new Parcelable.Creator<TtffLogSample>() {
        public TtffLogSample createFromParcel(Parcel in) {
            return new TtffLogSample(in);
        }

        public TtffLogSample[] newArray(int size) {
            return new TtffLogSample[size];
        }
    };

    public String toString() { return "[Ttff]"; }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("TTFF", ttff);
        return j;
    }
}

class DistanceSourceLogSample extends LogSample {
    public DistanceSourceLogSample() { type = SAMPLE_TYPE.DISTANCE_SOURCE; }
    int distance_source;/* 0x00 = Bikepod;
                           0x01 = Footpod,
                           0x02 = GPS,
                           0x03 = Wrist,
                           0x04 = Indoorswimming,
                           0x05 = Outdoorswimming */

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(distance_source);
    }

    private DistanceSourceLogSample(Parcel in) {
        readFromParcel(in);
        distance_source = in.readInt();
    }

    public static final Parcelable.Creator<DistanceSourceLogSample> CREATOR
            = new Parcelable.Creator<DistanceSourceLogSample>() {
        public DistanceSourceLogSample createFromParcel(Parcel in) {
            return new DistanceSourceLogSample(in);
        }

        public DistanceSourceLogSample[] newArray(int size) {
            return new DistanceSourceLogSample[size];
        }
    };

    public String toString() { return "[DistanceSource]"; }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("DistanceSource", distance_source);
        return j;
    }
}

class LapInfoLogSample extends LogSample {
    public LapInfoLogSample() { type = SAMPLE_TYPE.LAP_INFO; }
    int event_type;/* 0x01 = manual lap,
                       0x14 = high interval end,
                       0x15 = low interval end,
                       0x16 = interval start,
                       0x1e = pause,
                       0x1f = start */
    Date date_time;
    int duration; //ms
    int distance; //meters
    private boolean paused = false;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(event_type);
        out.writeLong(date_time.getTime());
        out.writeInt(duration);
        out.writeInt(distance);
        out.writeInt(paused ? 1 : 0);
    }

    private LapInfoLogSample(Parcel in) {
        readFromParcel(in);
        event_type = in.readInt();
        date_time = new Date(in.readLong());
        duration = in.readInt();
        distance = in.readInt();
        paused = in.readInt() == 1 ? true : false;
    }

    public static final Parcelable.Creator<LapInfoLogSample> CREATOR
            = new Parcelable.Creator<LapInfoLogSample>() {
        public LapInfoLogSample createFromParcel(Parcel in) {
            return new LapInfoLogSample(in);
        }

        public LapInfoLogSample[] newArray(int size) {
            return new LapInfoLogSample[size];
        }
    };

    public String toString() { return "[LapInfo]"; }
    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        switch (event_type) {//TODO: add "LocalTime"
            case 0x00://autolap = 5
                j.put("Type", 5);
                break;
            case 0x01://manual = 0
            case 0x16://interval = 0
                if (!paused) {
                    j.put("Type", 0);
                }
                break;
            case 0x1f://start = 1
                paused = false;
                break;
            case 0x1e://pause = 2
                j.put("Type", 2);
                paused = true;
                break;
            case 0x14://high interval = 3
            case 0x15://low interval = 3
                j.put("Type", 3);
                break;
        }

        return j;
    }
}

class AltitudeSourceLogSample extends LogSample {
    public AltitudeSourceLogSample() { type = SAMPLE_TYPE.ALTITUDE_SOURCE; }
    int source_type; /* 0x04 = pressure */
    int altitute_offset;
    int pressure_offset;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(source_type);
        out.writeInt(altitute_offset);
        out.writeInt(pressure_offset);
    }

    private AltitudeSourceLogSample(Parcel in) {
        readFromParcel(in);
        source_type = in.readInt();
        altitute_offset = in.readInt();
        pressure_offset = in.readInt();
    }

    public static final Parcelable.Creator<AltitudeSourceLogSample> CREATOR
            = new Parcelable.Creator<AltitudeSourceLogSample>() {
        public AltitudeSourceLogSample createFromParcel(Parcel in) {
            return new AltitudeSourceLogSample(in);
        }

        public AltitudeSourceLogSample[] newArray(int size) {
            return new AltitudeSourceLogSample[size];
        }
    };

    public String toString() { return "[AltitudeSource]"; }
    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("SourceType", source_type);
        j.put("AltituteOffset", altitute_offset);
        j.put("PressureOffset", pressure_offset);
        return j;
    }
}

class PositionLogSample extends LogSample {
    public PositionLogSample() { type = SAMPLE_TYPE.POSITION; }
    int latitude;/* degree, scale: 0.0000001, -90 <= latitude <= 90 */
    int longitude;/* degree, scale: 0.0000001, -180 <= latitude <= 180 */

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(latitude);
        out.writeInt(longitude);
    }

    protected PositionLogSample(Parcel in) {
        readFromParcel(in);
        latitude = in.readInt();
        longitude = in.readInt();
    }

    public static final Parcelable.Creator<PositionLogSample> CREATOR
            = new Parcelable.Creator<PositionLogSample>() {
        public PositionLogSample createFromParcel(Parcel in) {
            return new PositionLogSample(in);
        }

        public PositionLogSample[] newArray(int size) {
            return new PositionLogSample[size];
        }
    };

    public String toString() { return "[Position]"; }
    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("Latitude", latitude / 10000000.0);
        j.put("Longitude", longitude / 10000000.0);
        return j;
    }
}

class GPSTinyLogSample extends PositionLogSample {
    public GPSTinyLogSample() { type = SAMPLE_TYPE.GPS_TINY; }
    int ehpe; /* meters scale: 0.01 */

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(ehpe);
    }

    protected GPSTinyLogSample(Parcel in) {
        super(in);
        ehpe = in.readInt();
    }

    public static final Parcelable.Creator<GPSTinyLogSample> CREATOR
            = new Parcelable.Creator<GPSTinyLogSample>() {
        public GPSTinyLogSample createFromParcel(Parcel in) {
            return new GPSTinyLogSample(in);
        }

        public GPSTinyLogSample[] newArray(int size) {
            return new GPSTinyLogSample[size];
        }
    };

    public String toString() { return "[GPSTiny]"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("Altitude", 0.0);
        j.put("EHPE", ehpe / 100.0);
        j.put("Latitude", latitude / 10000000.0);
        j.put("Longitude", longitude / 10000000.0);
        return j;
    }
}

class GPSSmallLogSample extends GPSTinyLogSample {
    public GPSSmallLogSample() { type = SAMPLE_TYPE.GPS_SMALL; }
    int noofsatellites;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(noofsatellites);
    }

    protected GPSSmallLogSample(Parcel in) {
        super(in);
        noofsatellites = in.readInt();
    }

    public static final Parcelable.Creator<GPSSmallLogSample> CREATOR
            = new Parcelable.Creator<GPSSmallLogSample>() {
        public GPSSmallLogSample createFromParcel(Parcel in) {
            return new GPSSmallLogSample(in);
        }

        public GPSSmallLogSample[] newArray(int size) {
            return new GPSSmallLogSample[size];
        }
    };

    public String toString() { return "[GPSSmall]"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = super.toJSON();
        j.put("SampleType", toString());
        return j;
    }
}

class GPSBaseLogSample extends GPSSmallLogSample {
    public GPSBaseLogSample() { type = SAMPLE_TYPE.GPS_BASE; }
    int navvalid;
    int navtype;
    Date utc_base_time;

    int altitude;/* meters scale: 0.01, -1000 <= altitude <= 10000 */
    int speed; /* m/s scale: 0.01 */
    int heading; /* degrees, scale: 0.01, 0 <= heading <= 360 */

    int hdop; /* ? scale: 0.2 */

    class Satellite {
        int sv;
        int snr;
        int state;
    }

    Vector<Satellite> satellites;

    //TODO: parcelable not implemented for this class's extra memebers! Ignored now.

    public String toString() { return "[GPSBase]"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = super.toJSON();
        j.put("SampleType", toString());
        j.put("Altitude", altitude / 100.0);
        return j;
    }
}

class TimeLogSample extends LogSample {
    public TimeLogSample() { type = SAMPLE_TYPE.TIME; }
    int hour;
    int minute;
    int second;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(hour);
        out.writeInt(minute);
        out.writeInt(second);
    }

    private TimeLogSample(Parcel in) {
        readFromParcel(in);
        hour = in.readInt();
        minute = in.readInt();
        second = in.readInt();
    }

    public static final Parcelable.Creator<TimeLogSample> CREATOR
            = new Parcelable.Creator<TimeLogSample>() {
        public TimeLogSample createFromParcel(Parcel in) {
            return new TimeLogSample(in);
        }

        public TimeLogSample[] newArray(int size) {
            return new TimeLogSample[size];
        }
    };

    public String toString() { return "[Time]"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("Time", "" + hour + ":" + minute + ":" + second);
        return null;
    }
}

class SwimmingTurnLogSample extends LogSample {
    public SwimmingTurnLogSample() { type = SAMPLE_TYPE.SWIMMING_TURN; }
    int distance; /* Total distance, meters scale: 0.01 */
    int lengths; /* Total pool lengths */
    int classification[]; //size of 4
    int style; /* (style of previous length)
                   0x00 = Other,
                   0x01 = Butterfly,
                   0x02 = Backstroke,
                   0x03 = Breaststroke,
                   0x04 = Freestyle,
                   0x05 = Drill */

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(distance);
        out.writeInt(lengths);
        out.writeIntArray(classification);
        out.writeInt(style);
    }

    private SwimmingTurnLogSample(Parcel in) {
        readFromParcel(in);
        distance = in.readInt();
        lengths = in.readInt();
        in.readIntArray(classification);
        style = in.readInt();
    }

    public static final Parcelable.Creator<SwimmingTurnLogSample> CREATOR
            = new Parcelable.Creator<SwimmingTurnLogSample>() {
        public SwimmingTurnLogSample createFromParcel(Parcel in) {
            return new SwimmingTurnLogSample(in);
        }

        public SwimmingTurnLogSample[] newArray(int size) {
            return new SwimmingTurnLogSample[size];
        }
    };

    public String toString() { return "[SwimmingTurn]"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("Distance", distance / 100.0);
        j.put("Style", style);
        j.put("PoolLength", lengths);
        return null;
    }
}

//TODO: where is swimming stroke?

class ActivityLogSample extends LogSample {
    public ActivityLogSample() { type = SAMPLE_TYPE.ACTIVITY; }
    int activity_type;
    int custom_mode;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(activity_type);
        out.writeInt(custom_mode);
    }

    private ActivityLogSample(Parcel in) {
        readFromParcel(in);
        activity_type = in.readInt();
        custom_mode = in.readInt();
    }

    public static final Parcelable.Creator<ActivityLogSample> CREATOR
            = new Parcelable.Creator<ActivityLogSample>() {
        public ActivityLogSample createFromParcel(Parcel in) {
            return new ActivityLogSample(in);
        }

        public ActivityLogSample[] newArray(int size) {
            return new ActivityLogSample[size];
        }
    };

    public String toString() { return "[Activity]"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("NextActivityID", activity_type);
        j.put("Type", 8);
        return j;
    }
}

class CadenceSourceLogSample extends LogSample {
    public CadenceSourceLogSample() { type = SAMPLE_TYPE.CADENCE_SOURCE; }
    int cadence_source; /* 0x40 = Wrist */

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(cadence_source);
    }

    private CadenceSourceLogSample(Parcel in) {
        readFromParcel(in);
        cadence_source = in.readInt();
    }

    public static final Parcelable.Creator<CadenceSourceLogSample> CREATOR
            = new Parcelable.Creator<CadenceSourceLogSample>() {
        public CadenceSourceLogSample createFromParcel(Parcel in) {
            return new CadenceSourceLogSample(in);
        }

        public CadenceSourceLogSample[] newArray(int size) {
            return new CadenceSourceLogSample[size];
        }
    };

    public String toString() { return "[Cadence]"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("CadenceSource", cadence_source);
        return j;
    }
}

class FWInfoLogSample extends LogSample {
    public FWInfoLogSample() { type = SAMPLE_TYPE.FW_INFO; }
    int version[]; //size of 4
    Date build_date;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeIntArray(version);
        out.writeLong(build_date.getTime());
    }

    private FWInfoLogSample(Parcel in) {
        readFromParcel(in);
        in.readIntArray(version);
        build_date = new Date(in.readLong());
    }

    public static final Parcelable.Creator<FWInfoLogSample> CREATOR
            = new Parcelable.Creator<FWInfoLogSample>() {
        public FWInfoLogSample createFromParcel(Parcel in) {
            return new FWInfoLogSample(in);
        }

        public FWInfoLogSample[] newArray(int size) {
            return new FWInfoLogSample[size];
        }
    };

    public String toString() { return "[FWInfo]"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = createJSONWithTime();
        j.put("SampleType", toString());
        j.put("version_length", version.length);
        for (int i = 0; i < version.length; i++)
            j.put("version_" + i, version[i]);
        return j;
    }
}

class UnknownLogSample extends LogSample {
    char data[];

    public UnknownLogSample() {}

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeCharArray(data);
    }

    private UnknownLogSample(Parcel in) {
        readFromParcel(in);
        in.readCharArray(data);
    }

    public static final Parcelable.Creator<UnknownLogSample> CREATOR
            = new Parcelable.Creator<UnknownLogSample>() {
        public UnknownLogSample createFromParcel(Parcel in) {
            return new UnknownLogSample(in);
        }

        public UnknownLogSample[] newArray(int size) {
            return new UnknownLogSample[size];
        }
    };

    public String toString() { return "[Unknown]"; }

    public JSONObject toJSON() throws JSONException { return null; }
}