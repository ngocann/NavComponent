package me.blackdroid.navcomponent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import me.blackdroid.annotation.NavComponents;
import me.blackdroid.navcomponent.test.DetailActivity;
import me.blackdroid.navcomponent.test.Info;
import me.blackdroid.navcomponent.test.NavFragment;
import me.blackdroid.navcomponent.test.NavFragmentNavComponent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Info info = new Info();
        info.setName("name");
        info.setAge("age");

//        NavComponents.start(this, DetailActivity.class, "bbb", 13, info);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.frame, NavComponents.newInstance(NavFragment.class, "username", 12))
                .commit()
        ;
    }


}
