package com.example.energymonitor;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchBlackoutRounds extends Thread{
    public interface FetchResult {
        void onFetched(int result, String date, String raw, String[] rounds);
    }

    FetchResult fetchResult;
    private final String INFO_URL;
    public static final int OkResult = 0;
    public static final int BadResult = 1;
    public static final int NetworkError = 2;
    private static final String LOG_TAG = "EnergyMonitor_FetchBlackoutRounds_LOG";

    FetchBlackoutRounds(FetchResult fetchResult){
        //INFO_URL = "http://192.168.192.75/test.html";
        INFO_URL = "https://kiroe.com.ua/energy";
        this.fetchResult=fetchResult;
    }

    @Override
    public void run() {
        try {
            Document doc  = Jsoup.connect(INFO_URL).get();
            Element elem = doc.select("div.login_warn_desc").first();
            if(elem != null){
                String data = elem.text();
                fetchResult.onFetched(OkResult,parseDate(data),data,parseRounds(data));
            }else{
                fetchResult.onFetched(BadResult,null,null,null);
            }
        } catch (IOException e) {
            String msg = e.getLocalizedMessage();
            Log.e(LOG_TAG, msg != null ? msg : "Unknown Error!");
            fetchResult.onFetched(NetworkError,null,null,null);
        }
    }

    private static String parseDate(String data){
        Pattern pattern = Pattern.compile("\\b\\d{2}\\.\\d{2}\\.\\d{4}\\b");
        Matcher matcher = pattern.matcher(data);
        return matcher.find() ? matcher.group() : null;
    }
    private static String[] parseRounds(String data){
        ArrayList<String> rounds = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\d+ черга): ((\\d+-\\d+, )*(\\d+-\\d+))");
        Matcher matcher = pattern.matcher(data);
        String round;
        while (matcher.find()) {
            round = matcher.group(2);
            if (round != null) rounds.add(round.replaceAll("\\s*,\\s*", "\n"));
        }
        return rounds.toArray(new String[0]);
    }

}