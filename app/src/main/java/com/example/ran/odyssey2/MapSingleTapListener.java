package com.example.ran.odyssey2;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.MapView;

public class MapSingleTapListener extends DefaultMapViewOnTouchListener {

    private MapView mapView;
    private static Context context;

    public MapSingleTapListener(Context c, MapView mapView1) {
        super(c, mapView1);
        context = c;
        mapView = mapView1;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Point mapPoint = mapView.screenToLocation(new android.graphics.Point((int)e.getX(), (int)e.getY()));
        Toast.makeText(context, String.format("User tapped on the map at (%.3f,%.3f)", mapPoint.getX(), mapPoint.getY()), Toast.LENGTH_LONG).show();
        return true;
    }
}
