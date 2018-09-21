package me.blackdroid.navcomponent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.NavComponent;

@NavComponent
public class MainActivity extends AppCompatActivity {

    @Extra
    String userName;

    @Extra
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
