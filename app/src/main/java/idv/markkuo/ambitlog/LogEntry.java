package idv.markkuo.ambitlog;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


/**
 * Represents one "move" with a header and zero to multiple samples
 *
 * When a LogEntry comes without any samples, it is not yet downloaded from Ambit
 */
public class LogEntry implements Parcelable, Serializable {
    private final static String TAG = "AmbitLogEntry";
    LogHeader header;
    ArrayList<LogSample> samples = new ArrayList<LogSample>();

    // for managing status

    // flag to note this log entry is selected by user to be synced
    private boolean toSync = false;

    // this log is already downloaded (samples.size() > 0)
    private boolean downloaded = false;

    // for GPX outputs
    private ArrayList<TrackPoint> trackPoints = new ArrayList<>();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(header, flags);
        out.writeList(samples);
        out.writeInt(toSync ? 1 : 0);
        out.writeInt(downloaded ? 1 : 0);
        out.writeList(trackPoints);
    }

    private LogEntry(Parcel in) {
        header = in.readParcelable(LogHeader.class.getClassLoader());
        samples = in.readArrayList(LogSample.class.getClassLoader());
        toSync = in.readInt() == 1 ? true : false;
        downloaded = in.readInt() == 1 ? true : false;
        trackPoints = in.readArrayList(TrackPoint.class.getClassLoader());
    }

    public static final Parcelable.Creator<LogEntry> CREATOR
            = new Parcelable.Creator<LogEntry>() {
        public LogEntry createFromParcel(Parcel in) {
            return new LogEntry(in);
        }

        public LogEntry[] newArray(int size) {
            return new LogEntry[size];
        }
    };

    public LogEntry(LogHeader header) {
        this.header = header;
    }

    public boolean isToSync() { return toSync; }
    public void markForSync(boolean toSync) { this.toSync = toSync; }

    public boolean isDownloaded() { return downloaded; }
    public void setDownloaded(boolean b) { downloaded = b; }

    public String toString() {
        return header.toString();
    }

    public LogHeader getHeader() { return header; }

    // converts to filename String
    public String getFilename(String ext) { return header.getMovescountFilePrefix() + "." + ext; }

    // this prints current status to Android Log
    void query() {
        boolean downloaded = true;
        if (samples.size() == 0)
            downloaded = false;
        Log.d(TAG, downloaded ? "[o]" : "[x] - " + header.toString() +
                (downloaded ? "[" + samples.size() + "]" : ""));
    }

    // Check the header and see if a new LogEntry is similar to itself
    boolean resemble(int year, int month, int day, int hour, int minute, int msec,
                            int duration) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, minute, msec/1000);
        if (header.datetime.equals(c.getTime()) && header.duration == duration)
            return true;
        return false;
    }


    // reorder samples to be ready for export
    private ArrayList<Integer> reorder() {
        int i, j;
        int lastMatch = 0;
        ArrayList<Integer> order = new ArrayList<Integer>(samples.size());

        for(i = 0; i < samples.size(); i = lastMatch + 1) {
            //First find all entries with the same time
            lastMatch = i;
            while (lastMatch + 1 < samples.size() &&
                    samples.get(i).time == samples.get(lastMatch + 1).time)
                lastMatch ++;

            // Put start/stop at top
            for (j = i; j <= lastMatch; j++) {
                LogSample s = samples.get(j);
                if (s instanceof LapInfoLogSample) {
                    LapInfoLogSample li = (LapInfoLogSample) s;
                    if (li.event_type == 0x1e || li.event_type == 0x1f)
                        order.add(j);
                }
            }

            // Then any gps-entries
            for (j = i; j <= lastMatch; j++) {
                LogSample s = samples.get(j);
                if (s instanceof GPSTinyLogSample) {
                    order.add(j);
                }
            }

            // Then periodic samples
            for (j = i; j <= lastMatch; j++) {
                LogSample s = samples.get(j);
                if (s instanceof PeriodicLogSample) {
                    order.add(j);
                }
            }

            // Then any other!
            for (j = i; j <= lastMatch; j++) {
                LogSample s = samples.get(j);
                if (!(s instanceof GPSTinyLogSample) &&
                        !(s instanceof PeriodicLogSample) &&
                        !(s instanceof LapInfoLogSample)) {
                    order.add(j);
                }
            }
        }

        return order;
    }

    public boolean writeGPX(File file) {
        ArrayList<Integer> order = reorder();
        TrackPoint cur_tp = null;
        Log.d(TAG, "writeGPX(): total sample size: " + order.size());
        int lat = 0, lon = 0;
        for (int i = 0; i < order.size(); i++) {
            LogSample s = samples.get(order.get(i));

            Log.v(TAG, "writeGPX(): reading sample at:" + order.get(i) + " type=" + s.type);
            switch(s.type) {
                case SAMPLE_TYPE.GPS_BASE:
                case SAMPLE_TYPE.GPS_SMALL:
                case SAMPLE_TYPE.GPS_TINY:
                case SAMPLE_TYPE.POSITION:
                    if (lat == ((PositionLogSample)s).latitude &&
                            lon == ((PositionLogSample)s).longitude)
                        //skip this sample
                        continue;
                    if (cur_tp != null) {
                        trackPoints.add(cur_tp);
                    }

                    lat = ((PositionLogSample)s).latitude;
                    lon = ((PositionLogSample)s).longitude;
                    TrackPoint tp = new TrackPoint(lat / 10000000.0, lon / 10000000.0);
                    tp.setUtc(s.utc_time);
                    cur_tp = tp;

                    break;

                case SAMPLE_TYPE.PERIODIC:
                    if (cur_tp == null)
                        continue;

                    PeriodicLogSample ps = (PeriodicLogSample)s;

                    for (PeriodicValue v : ps.values) {
                        switch (v.type) {
                            case PeriodicLogSample.TYPE_DISTANCE:
                                if (v.value != 0xffffffff)
                                    cur_tp.setDist(v.value);
                                break;
                            case PeriodicLogSample.TYPE_SPEED:
                                if (v.value != 0xffff)
                                    cur_tp.setSpeed((double) v.value / 100.0);
                                break;
                            case PeriodicLogSample.TYPE_HR:
                                if (v.value != 0xff)
                                    cur_tp.setHr(v.value);
                                break;
                            case PeriodicLogSample.TYPE_TIME:
                                //TODO: doesn't use this field for now
                                // (double) v.value / 1000.0);
                                break;
                            case PeriodicLogSample.TYPE_ALTITUDE:
                                if (v.value >= -1000 && v.value <= 10000)
                                    cur_tp.setAlt(v.value);
                                break;
                            case PeriodicLogSample.TYPE_ENERGY:
                                cur_tp.setEnergy((double)v.value / 10.0);
                                break;
                            case PeriodicLogSample.TYPE_TEMPERATURE:
                                if (v.value >= -1000 && v.value <= 1000)
                                    cur_tp.setTemp((double)v.value / 10.0);
                                break;
                            case PeriodicLogSample.TYPE_SEALEVELPRESSURE:
                                if (v.value >= 8500 && v.value < 11000)
                                    cur_tp.setSea(v.value / 10.0);
                                break;
                            case PeriodicLogSample.TYPE_VERTICALSPEED:
                                cur_tp.setVspeed((double)v.value / 100.0);
                                break;
                            case PeriodicLogSample.TYPE_CADENCE:
                                if (v.value != 0xff)
                                    cur_tp.setCad(v.value);
                                break;
                            case PeriodicLogSample.TYPE_BIKEPOWER:
                                if (v.value != 0xffff)
                                    cur_tp.setPower(v.value);
                                break;
                        }
                    }
                    break;
            }
        }

        // loop through trackPoints and save
        GPXWriter gpxWriter = new GPXWriter();
        return gpxWriter.write(trackPoints, file);
    }

    // unused for now. We don't output JSON
    public JSONObject toJSON() throws JSONException {
        // MovesCountJSON::generateLogData in movescountjson.cpp
        JSONObject j = new JSONObject();
        ArrayList<Integer> order = reorder();
        int i;

        JSONArray a = new JSONArray();

        for (i = 0; i < order.size(); i++) {
            LogSample s = samples.get(order.get(i));
            JSONObject sj = s.toJSON();
            if (sj != null)
                a.put(sj);
            switch(s.type) {
                case SAMPLE_TYPE.PERIODIC:
                    break;
                case SAMPLE_TYPE.IBI:
                    //j.put("IBIData", sj.toString().getBytes());
                    break;
                //TODO: not complete. But we are not caring for json in any way
            }

        }
        j.put("samples", a);

        return j;
    }
}

// GPX <trkpt> tag representation
class TrackPoint implements Parcelable, Serializable {
    private double lat, lon;
    private Date utc;

    private ArrayList<Double> temp = new ArrayList<>();
    private ArrayList<Double> energy = new ArrayList<>();
    private ArrayList<Double> sea = new ArrayList<>();
    private ArrayList<Double> speed = new ArrayList<>();
    private ArrayList<Double> vspeed = new ArrayList<>();
    private ArrayList<Double> power = new ArrayList<>();

    private ArrayList<Integer> hr = new ArrayList<>();
    private ArrayList<Integer> cad = new ArrayList<>();
    private ArrayList<Integer> dist = new ArrayList<>();
    private ArrayList<Integer> alt = new ArrayList<>();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(lat);
        out.writeDouble(lon);
        out.writeLong(utc.getTime());
        out.writeList(temp);
        out.writeList(energy);
        out.writeList(sea);
        out.writeList(speed);
        out.writeList(vspeed);
        out.writeList(power);
        out.writeList(hr);
        out.writeList(cad);
        out.writeList(dist);
        out.writeList(alt);
    }

    private TrackPoint(Parcel in) {
        lat = in.readDouble();
        lon = in.readDouble();
        utc = new Date(in.readLong());
        temp = in.readArrayList(Double.class.getClassLoader());
        energy = in.readArrayList(Double.class.getClassLoader());
        sea = in.readArrayList(Double.class.getClassLoader());
        speed = in.readArrayList(Double.class.getClassLoader());
        vspeed = in.readArrayList(Double.class.getClassLoader());
        power = in.readArrayList(Double.class.getClassLoader());
        hr = in.readArrayList(Integer.class.getClassLoader());
        cad = in.readArrayList(Integer.class.getClassLoader());
        dist = in.readArrayList(Integer.class.getClassLoader());
        alt = in.readArrayList(Integer.class.getClassLoader());
    }

    public static final Parcelable.Creator<TrackPoint> CREATOR
            = new Parcelable.Creator<TrackPoint>() {
        public TrackPoint createFromParcel(Parcel in) {
            return new TrackPoint(in);
        }

        public TrackPoint[] newArray(int size) {
            return new TrackPoint[size];
        }
    };


    public TrackPoint(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public void setTemp(double temp) {
        this.temp.add(temp);
    }

    public void setEnergy(double energy) {
        this.energy.add(energy);
    }

    public void setSea(double sea) {
        this.sea.add(sea);
    }

    public void setSpeed(double speed) {
        this.speed.add(speed);
    }

    public void setVspeed(double vspeed) {
        this.vspeed.add(vspeed);
    }

    public void setHr(int hr) {
        this.hr.add(hr);
    }

    public void setCad(int cad) { this.cad.add(cad); }

    public void setDist(int dist) {
        this.dist.add(dist);
    }

    public void setAlt(int alt) {
        this.alt.add(alt);
    }

    public void setUtc(Date utc) {
        this.utc = utc;
    }

    public void setPower(double power) {
        this.power.add(power);
    }

    /*
    Example tag
      <trkpt lat="-37.811326" lon="145.186015">
        <ele>128</ele>
        <time>2018-01-08T08:03:34.287Z</time>
        <extensions>
          <gpxtpx:TrackPointExtension xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">
            <gpxtpx:hr>148</gpxtpx:hr>
          </gpxtpx:TrackPointExtension>
          <gpxdata:cadence>77</gpxdata:cadence>
          <gpxdata:temp>21.7999992370605</gpxdata:temp>
          <gpxdata:distance>5657</gpxdata:distance>
          <gpxdata:altitude>128</gpxdata:altitude>
          <gpxdata:energy>11.065199660293</gpxdata:energy>
          <gpxdata:seaLevelPressure>1014</gpxdata:seaLevelPressure>
          <gpxdata:speed>2.20000004768372</gpxdata:speed>
          <gpxdata:verticalSpeed>0.0717400081243613</gpxdata:verticalSpeed>
        </extensions>
      </trkpt>
    */
    private int calculateIntAverage(ArrayList<Integer> a) {
        if (a == null || a.isEmpty()) {
            return 0;
        }

        double sum = 0;
        for (Integer i : a) {
            sum += i;
        }

        return (int) (sum / a.size());
    }

    private double calculateAverage(ArrayList<Double> a) {
        if (a == null || a.isEmpty()) {
            return 0;
        }

        double sum = 0;
        for (Double i : a) {
            sum += i;
        }

        return sum / a.size();
    }

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public String toGPX() {

        StringBuilder sb = new StringBuilder();
        sb.append("      <trkpt lat=\"").append(lat).append("\" lon=\"").append(lon).append("\">\n");
        if (!alt.isEmpty())
            sb.append("        <ele>").append(calculateIntAverage(alt)).append("</ele>\n");
        sb.append("        <time>").append(df.format(utc)).append("</time>\n")
          .append("        <extensions>\n");
        if (!hr.isEmpty()) {
            sb.append("          <gpxtpx:TrackPointExtension xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\">\n")
              .append("            <gpxtpx:hr>").append(calculateIntAverage(hr)).append("</gpxtpx:hr>\n")
              .append("          </gpxtpx:TrackPointExtension>\n");
        }
        if (!cad.isEmpty())
            sb.append("          <gpxdata:cadence>").append(calculateIntAverage(cad)).append("</gpxdata:cadence>\n");
        if (!temp.isEmpty())
            sb.append("          <gpxdata:temp>").append(calculateAverage(temp)).append("</gpxdata:temp>\n");
        if (!dist.isEmpty())
            sb.append("          <gpxdata:distance>").append(calculateIntAverage(dist)).append("</gpxdata:distance>\n");
        if (!alt.isEmpty())
            sb.append("          <gpxdata:altitude>").append(calculateIntAverage(alt)).append("</gpxdata:altitude>\n");
        if (!energy.isEmpty())
            sb.append("          <gpxdata:energy>").append(calculateAverage(energy)).append("</gpxdata:energy>\n");
        if (!sea.isEmpty())
            sb.append("          <gpxdata:seaLevelPressure>").append(calculateAverage(sea)).append("</gpxdata:seaLevelPressure>\n");
        if (!speed.isEmpty())
            sb.append("          <gpxdata:speed>").append(calculateAverage(speed)).append("</gpxdata:speed>\n");
        if (!vspeed.isEmpty())
            sb.append("          <gpxdata:verticalSpeed>").append(calculateAverage(vspeed)).append("</gpxdata:verticalSpeed>\n");
        sb.append("        </extensions>\n")
          .append("      </trkpt>\n");
        return sb.toString();
    }

}

// class for writing GPX format file
class GPXWriter {
    private static final String TAG = "AmbitGPXWriter";

    private static final String header = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
            "<gpx version=\"1.1\" creator=\"AmbitSync\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.cluetrust.com/XML/GPXDATA/1/0 http://www.cluetrust.com/Schemas/gpxdata10.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\" xmlns:gpxdata=\"http://www.cluetrust.com/XML/GPXDATA/1/0\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n" +
            "  <trk>\n" +
            "    <name>Move</name>\n" +
            "    <trkseg>\n";
    private static final String footer = "    </trkseg>\n" +
            "  </trk>\n" +
            "</gpx>";

    public boolean write(ArrayList<TrackPoint> tps, File file) {

        try {
            FileWriter writer = new FileWriter(file, false);
            writer.append(header);
            for (TrackPoint tp: tps) {
                writer.append(tp.toGPX());
            }
            writer.append(footer);
            writer.flush();
            writer.close();
            Log.d(TAG, "gpx written");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error Writing Path:", e);
            return false;
        }

    }
}