package com.example.ran.odyssey2;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.layers.FeatureCollectionLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.util.ListenableList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import spinner.ItemData;
import spinner.SpinnerAdapter;

public class MainActivity extends AppCompatActivity {

    ////////////////// Private Fields ///////////////////
    private MapView mMapView;
    private LocationDisplay mLocationDisplay;
    private Spinner mSpinner;

    private SearchView mSearchView = null;
    private GraphicsOverlay mGraphicsOverlay;
    private LocatorTask mLocatorTask = null;
    private GeocodeParameters mGeocodeParameters = null;

    // Search

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        if (searchMenuItem != null) {
            mSearchView = (SearchView) searchMenuItem.getActionView();
            if (mSearchView != null) {
                SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
                mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                mSearchView.setIconifiedByDefault(false);
            }
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            queryLocator(intent.getStringExtra(SearchManager.QUERY));
        }
    }

    private void queryLocator(final String query) {
        if (query != null && query.length() > 0) {
            mLocatorTask.cancelLoad();
            final ListenableFuture<List<GeocodeResult>> geocodeFuture = mLocatorTask.geocodeAsync(query, mGeocodeParameters);
            geocodeFuture.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<GeocodeResult> geocodeResults = geocodeFuture.get();
                        if (geocodeResults.size() > 0) {
                            displaySearchResult(geocodeResults.get(0));
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.nothing_found) + " " + query, Toast.LENGTH_LONG).show();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        // ... determine how you want to handle an error
                    }
                    geocodeFuture.removeDoneListener(this); // Done searching, remove the listener.
                }
            });
        }
    }

    private void displaySearchResult(GeocodeResult geocodedLocation) {
        String displayLabel = geocodedLocation.getLabel();
        TextSymbol textLabel = new TextSymbol(18, displayLabel, Color.rgb(192, 32, 32), TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.BOTTOM);
        Graphic textGraphic = new Graphic(geocodedLocation.getDisplayLocation(), textLabel);
        Graphic mapMarker = new Graphic(geocodedLocation.getDisplayLocation(), geocodedLocation.getAttributes(),
                new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, Color.rgb(255, 0, 0), 12.0f));
        ListenableList allGraphics = mGraphicsOverlay.getGraphics();
        allGraphics.clear();
        allGraphics.add(mapMarker);
        allGraphics.add(textGraphic);
        mMapView.setViewpointCenterAsync(geocodedLocation.getDisplayLocation());
    }

    private void setupLocator() {
        String locatorLocation = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer";
        mLocatorTask = new LocatorTask(locatorLocation);
        mLocatorTask.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                if (mLocatorTask.getLoadStatus() == LoadStatus.LOADED) {
                    mGeocodeParameters = new GeocodeParameters();
                    mGeocodeParameters.getResultAttributeNames().add("*");
                    mGeocodeParameters.setMaxResults(1);
                    mGraphicsOverlay = new GraphicsOverlay();
                    mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
                } else if (mSearchView != null) {
                    mSearchView.setEnabled(false);
                }
            }
        });
        mLocatorTask.loadAsync();
    }


    /////////////////// Map View ////////////////////////////
    @Override
    protected void onPause(){
        mMapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.dispose();
    }


    /////////////////// Location Service ///////////////////////

    private void setupLocationDisplay() {
        mLocationDisplay = mMapView.getLocationDisplay();

        // button 1
        final Button testButton = (Button) findViewById(R.id.button1);
        testButton.setTag(1);
        testButton.setText("Hide Location");
        testButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                final int status =(Integer) v.getTag();
                if(status == 1) {
                    mLocationDisplay.stop();
                    testButton.setText("Show Location");
                    v.setTag(0); //pause
                } else {
                    mLocationDisplay.startAsync();
                    mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
                    testButton.setText("Hide Location");
                    v.setTag(1); //pause
                }
            }
        });

        // button 2
        final Button testButton2 = (Button) findViewById(R.id.button2);
        testButton2.setTag(1);
        testButton2.setText("Start Navigation");
        testButton2.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                final int status =(Integer) v.getTag();
                if(status == 1) {
                    mLocationDisplay.startAsync();
                    mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
                    testButton2.setText("Stop Navigation");
                    v.setTag(0); //pause
                } else {
                    mLocationDisplay.stop();
                    testButton2.setText("Start Navigation");
                    v.setTag(1); //pause
                }
            }
        });

        /*
        mLocationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                if (dataSourceStatusChangedEvent.isStarted() || dataSourceStatusChangedEvent.getError() == null) {
                    return;
                }

                int requestPermissionsCode = 2;
                String[] requestPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

                if (!(ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[0]) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[1]) == PackageManager.PERMISSION_GRANTED)) {
                    ActivityCompat.requestPermissions(MainActivity.this, requestPermissions, requestPermissionsCode);
                } else {
                    String message = String.format("Error in DataSourceStatusChangedListener: %s",
                            dataSourceStatusChangedEvent.getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    mSpinner.setSelection(0, true);
                }

            }
        });

        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        mLocationDisplay.startAsync();
        */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationDisplay.startAsync();
        } else {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }


    /////////////////// Initial Setup //////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // spinner
        mSpinner = (Spinner) findViewById(R.id.spinner);

        // map view
        mMapView = findViewById(R.id.mapView);
        // New map is defined here !!!!!!!!!!!!!!!!!!!!!
        ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 34.056295, -117.195800, 16);
        mMapView.setMap(map);

        // location
        setupLocationDisplay();

        mMapView.setOnTouchListener(new MapSingleTapListener(this, mMapView));

        // search
        setupLocator();

    }
}
