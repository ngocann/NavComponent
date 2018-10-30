package me.blackdroid.navcomponent.test;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.NavComponent;

@NavComponent
public class NavFragment extends Fragment {
    @Extra
    public String userName;
    public static NavFragment newInstance(String username) {
        Bundle args = new Bundle();
        args.putString("", "");
        args.putInt("", 0);

        NavFragment fragment = new NavFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getArguments().getParcelable("aaa");
    }
}
