package me.blackdroid.navcomponent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.NavComponent;
import me.blackdroid.navcomponent.test.Account;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Account account = new Account("usernaemm", "password");


        NavComponents.startDetailActivity(this, "1234", account );
    }
}
