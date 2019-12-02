package com.dfrobot.angelo.blunobasicdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;

public class DisplayPageFragment extends Fragment {

    OrientationViewer ov;
    GraphView graph;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_display_page, container, false);
        ov = rootView.findViewById(R.id.orient_viewview);
        graph = rootView.findViewById(R.id.graphview);

        return rootView;
    }
}