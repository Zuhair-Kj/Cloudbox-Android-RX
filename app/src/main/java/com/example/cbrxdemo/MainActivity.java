package com.example.cbrxdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.duriana.cloudbox.CloudBox;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {

    TextView tv;
    CloudBox cb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cb = MyApplication.cloudBoxObject;
        tv = (TextView) findViewById(R.id.tv);
        Observable<CloudBox.RESULT> o = cb.getFileFromServerRX(this, "Options_duriana", ".json");

        o.observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<CloudBox.RESULT>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                tv.append("Old version of file:\n" + cb.getFileAsString(MainActivity.this, "Options_duriana", ".json"));
            }

            @Override
            public void onNext(CloudBox.RESULT result) {

//                Toast.makeText(MyApplication.this, result.toString(), Toast.LENGTH_LONG).show();
                tv.append(cb.getFileAsString(MainActivity.this, "Options_duriana", ".json"));
            }
        });
    }
}
