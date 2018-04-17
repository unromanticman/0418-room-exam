package com.example.a0321;

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    // 存放匯率資料
    RateData rateNow;

    // 顯示更新狀態
    private Handler handler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // onClick event for getRateButton
    public void onRefreshRate(View v){
        // create a object of ExchangeRateUpdateThread
        // and execute start()
        new ExchangeRateUpdateThread().start();
    }

    protected class ExchangeRateUpdateThread extends Thread
    {
        @Override
        public void run() {
            // create a object of BotRateParser and execute parse()
            // get  data of RateData class
            RateData rateTmp = new BotRateParser().parse();
        }
    }

    public class RateData
    {
        // there are three currencies
        public static final String USD = "USD";
        public static final String EUR = "EUR";
        public static final String TWD = "TWD";
        public float usd = 0.0F;
        public float eur = 0.0F;
        public float twd = 0.0F;
    }

    public class BotRateParser {
        public static final String BOT_ADDRESS_NEW =
                "http://rate.bot.com.tw/xrt?Lang=zh-TW";

        public RateData parse() {
            String csvAddress = getRateCsvLink();
            RateData RateTemp = getRateCsvData(csvAddress);
            if (RateTemp != null) {
                rateNow = RateTemp;
                handler.sendEmptyMessage(1);
            }
            return null;
        }

        public RateData getRateCsvData(String csvAddress){
            RateData rateTmp = new RateData();
            // TWD is the base
            rateTmp.twd = 1;

            try {
                // claim variable of URL class
                URL url = new URL(csvAddress);

                // claim variable of HttpURLConnection
                HttpURLConnection httpURLConnection =
                        (HttpURLConnection) url.openConnection();

                httpURLConnection.setConnectTimeout(2000);
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.connect();

                InputStream is = httpURLConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, "utf8");
                BufferedReader reader = new BufferedReader(isr);

                String line = "";
                while((line = reader.readLine()) != null) {
                    Log.e("line>>>",line);
                    String[] rateArray = line.split(",");
                    float value = 0;
                    try {
                        // 現金賣出匯率
                        value = Float.parseFloat(rateArray[12].trim());
                        Log.e("value>>>", Float.toString(value));
                        //return null;
                    }
                    catch(RuntimeException e) {
                        Log.e("","get rate error");
                    }
                    switch((rateArray[0].trim())) {
                        case RateData.USD: rateTmp.usd = value; break;
                        case RateData.EUR: rateTmp.eur = value; break;
                    }
                }
            }
            catch (IOException e) {
                return null;
            }
            return rateTmp;
        }


        public String getRateCsvLink() {
            try
            {
                // claim variable of URL class
                URL url = new URL(BOT_ADDRESS_NEW);

                // claim variable of HttpURLConnection
                HttpURLConnection httpURLConnection =
                        (HttpURLConnection)url.openConnection();

                httpURLConnection.setConnectTimeout(2000);
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.connect();

                InputStream is = httpURLConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(is,"utf8");
                BufferedReader reader = new BufferedReader(isr);

                String line="";

                // read each line of html script
                while((line = reader.readLine()) != null)
                {
                    if(line.indexOf("下載 Excel 檔") != -1)
                    {
                        line = line.substring(line.indexOf("href=\"") + 6,
                                line.lastIndexOf("\">"));
                        line = line.replace("&amp;", "&");
                        line = "http://rate.bot.com.tw" + line;

                        // 可以在android monitor 查看訊息
                        //Log.e("URL>>>>>",line);
                        return line;
                    }
                }

            }
            catch(IOException e)
            {

            }
            return null; // if the url is available
        }
    }

    private static class MyHandler extends Handler{
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity)
        {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            MainActivity act = mActivity.get();

            if (act != null) {
                if (msg.what == 1) {
                    Toast.makeText(act,
                            "匯率已更新完畢",
                            Toast.LENGTH_SHORT).show();
                }
                else if(msg.what == -1){
                    Toast.makeText(act,
                            "請先按取得匯率",
                            Toast.LENGTH_SHORT).show();
                }
                else if (msg.what == -2){
                    Toast.makeText(act,
                            "匯率已清除",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void rateInfo(View v)
    {
        if(rateNow == null) {
            handler.sendEmptyMessage(-1);
            return;
        }

        AlertDialog.Builder myDlg = new AlertDialog.Builder(this);

        myDlg.setTitle("匯率資訊(貨幣單位：TW)");

        myDlg.setMessage(
                String.format("%s:%10.5f\n", "usd", rateNow.usd) +
                        String.format("%s:%10.5f\n", "eur", rateNow.eur)) ;


        AlertDialog dlg = myDlg.show();

        TextView textView = (TextView)dlg.findViewById(android.R.id.message);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
    }

    public void clearRateInfo(View v){
        rateNow = null;
        handler.sendEmptyMessage(-2);
    }
}
