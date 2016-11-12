package com.example.litmus.litmus;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.util.Log;
import java.util.Map;
import android.os.Parcelable;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.popup.PopupContainer;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.symbol.TextSymbol.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.view.View;
import android.widget.Toast;

import java.util.HashMap;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

public class MapActivity extends Activity {

    FirebaseDatabase database;
    DatabaseReference locations;

    Map<Integer, DataSnapshot> graphicIdToDBRef;


    MapView mMapView;
    GraphicsLayer graphicsLayer;
    SpatialReference webSR;
    PopupContainer popupContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        graphicIdToDBRef = new HashMap<Integer, DataSnapshot>();

        webSR = SpatialReference.create(3857);


        // after the content of this activity is set
        // the map can be accessed from the layout
        mMapView = (MapView)findViewById(R.id.map);
        graphicsLayer = new GraphicsLayer();
        mMapView.addLayer(graphicsLayer);
        popupContainer = new PopupContainer(mMapView);

        database = FirebaseDatabase.getInstance();
        locations = database.getReference("Locations");

        locations.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("firebase listener", "value is " + snapshot);
                // every time this is modified, redraw the map
                //graphicsLayer = new GraphicsLayer(); // clear the graphics layer
                graphicsLayer.removeAll();
                for (DataSnapshot location : snapshot.getChildren()) {
                    String thisLocationName = (String) location.child("name").getValue();
                    Double thisLocationLatitude = (Double) location.child("latitude").getValue();
                    Double thisLocationLongitude = (Double) location.child("longitude").getValue();
                    int attendees = 0;
                    for (DataSnapshot person : location.child("people").getChildren()) {
                        attendees++;
                        // get some data from these people later, to get who's present, demographics, etc.
                    }
                    int[] ids = createPointOfInterest(thisLocationLatitude, thisLocationLongitude, thisLocationName, attendees);

                    for (int thisId : ids) {
                        graphicIdToDBRef.put(thisId, location);
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w("firebase listener error",error.toException());
            }
        });


        //createPointOfInterest(41.3163, -72.9223, "Placeholder", 5);

        mMapView.setOnSingleTapListener(new OnSingleTapListener() {
            @Override
            public void onSingleTap(float x, float y) {
                if (!mMapView.isLoaded()) {
                    return;
                }

                //Point identifyPoint = mMapView.toMapPoint(x, y);

                int thisPointId = graphicsLayer.getGraphicIDs(x, y, 200, 1)[0];
                Log.d("result", "" + thisPointId);

                String text = graphicIdToDBRef.get(thisPointId).child("name").getValue().toString();

                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, text, LENGTH_SHORT);
                toast.show();

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected int[] createPointOfInterest(Double latitude, Double longitude, String description, int population) {
        int[] ids = new int[2];

        Log.d("POI", "entered point of interest creator");
        Point pnt = GeometryEngine.project(longitude, latitude, webSR);

        SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.RED, 40, SimpleMarkerSymbol.STYLE.CIRCLE);
        Graphic graphic = new Graphic(pnt, sms);
        ids[0] = graphicsLayer.addGraphic(graphic);


        TextSymbol textSymbol = new TextSymbol(30, Integer.toString(population), Color.WHITE, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
        Graphic textGraphic = new Graphic(pnt, textSymbol);
        ids[1] = graphicsLayer.addGraphic(textGraphic);

        return ids;
    }





}

