package me.blackdroid.navcomponent.test;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.NavComponent;
import me.blackdroid.navcomponent.NavComponents;

@NavComponent
public class DetailActivity extends AppCompatActivity {
   @Extra
   String username;

   @Extra Account account;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DetailActivityNavComponent.inject(this);
        Toast.makeText(this, account.password, Toast.LENGTH_LONG).show();
    }
}
