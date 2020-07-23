package com.nstore.usb;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.usb.serial.util.LogWriter;


public class LogActivity extends AppCompatActivity {
    TextView txtLog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        txtLog=findViewById(R.id.txtRawResource);
        Handler handler=new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                LogWriter logWriter=new LogWriter();
                String logData=logWriter.readFile();
                if(TextUtils.isEmpty(logData)){
                    txtLog.setText("No Logs Found");
                }else{
                    txtLog.setText(logData);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
