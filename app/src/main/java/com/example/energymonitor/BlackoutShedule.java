package com.example.energymonitor;

import androidx.annotation.NonNull;

import java.util.Locale;

public class BlackoutShedule {
    public final int[] hours;
    public final String date;
    public static final String NO_SHEDULE = "none";
    public static final String SHEDULE_UNKNOWN = "unknown";
    private static final int HOUR_OFF=0;
    private static final int HOUR_ON=1;
    private static final int HOUR_UNKNOWN=2;

    BlackoutShedule(String date,int[] hours){
        this.date=date;
        this.hours=hours;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if(hours[0]==HOUR_UNKNOWN) sb.append(SHEDULE_UNKNOWN);

        int start = -1;
        for (int i = 0; i < hours.length; i++) {
            if (hours[i] == HOUR_OFF) {
                if (start == -1) {
                    start = i;
                }
            } else {
                if (start != -1) {
                    appendHourRange(sb, start, i - 1);
                    start = -1;
                }
            }
        }

        // Check if the last hour was a blackout hour
        if (start != -1) {
            appendHourRange(sb, start, hours.length - 1);
        }

        if(sb.toString().isEmpty()) sb.append(NO_SHEDULE);

        return sb.toString();
    }

    private void appendHourRange(StringBuilder sb, int start, int end) {
        sb
                .append(String.format(Locale.getDefault(),"%02d ", start))
                .append("-").append(String.format(Locale.getDefault()," %02d", end + 1)).append("\n");
    }

}
