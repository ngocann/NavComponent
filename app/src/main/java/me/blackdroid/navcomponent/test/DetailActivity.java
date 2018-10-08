package me.blackdroid.navcomponent.test;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.ExtraParcel;
import me.blackdroid.annotation.NavComponent;
import me.blackdroid.annotation.NavComponents;

@NavComponent
public class DetailActivity extends AppCompatActivity {

   @Extra String username;
   @Extra Integer value = 0;
   @ExtraParcel Info info;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavComponents.bind(this);
        Toast.makeText(this, username + info.getAge() + value, Toast.LENGTH_LONG).show();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, DetailActivity.class);
        context.startActivity(intent);
    }
}
