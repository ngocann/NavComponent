package me.blackdroid.navcomponent.test

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import me.blackdroid.annotation.Extra
import me.blackdroid.annotation.ExtraParcel
import me.blackdroid.annotation.NavComponent
import me.blackdroid.annotation.NavComponents

@NavComponent
class DetailActivityKotlin : AppCompatActivity() {
    @Extra var username : String? = null
    @Extra(key = "BUNDLE_AGE") var age : Int? = null

    @ExtraParcel(key = "BUNDLE_INFO")
    var info : Info? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NavComponents.bind(this);
        Toast.makeText(this, username + info?.getAge() + age , Toast.LENGTH_LONG).show();

    }
}