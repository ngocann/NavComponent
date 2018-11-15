package me.blackdroid.navcomponent.test;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.NavComponent;
import me.blackdroid.annotation.NavComponents;

@NavComponent
public class NavFragment extends Fragment {
    @Extra public String userName;
    @Extra public Integer age;
    @Extra(parceler = true)
    public Info info = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        NavComponents.bind(this);
        Toast.makeText(getActivity(), "toast" + userName + age, Toast.LENGTH_SHORT).show();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
