package com.example.energymonitor;

import android.os.Handler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class FetchBlackoutRounds extends Thread{
    public interface FetchResult {
        public void onFetched(String result);
    }

    FetchResult fetchResult;
    Handler handler;

    FetchBlackoutRounds(FetchResult fetchResult, Handler handler){
        this.fetchResult=fetchResult;
        this.handler=handler;
    }

    @Override
    public void run() {
        try {
            Document doc  = Jsoup.connect("https://kiroe.com.ua/energy").get();
            Element elem = doc.select("div.login_warn_desc").first();
            handler.post(() -> fetchResult.onFetched(elem.html()));
        } catch (IOException e) {
            e.printStackTrace();
            handler.post(() -> fetchResult.onFetched(null));
        }
    }
}