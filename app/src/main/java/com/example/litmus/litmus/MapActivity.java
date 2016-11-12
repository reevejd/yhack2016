package com.example.litmus.litmus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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


    public class User {
        // going to need to handle user duplicates at some point
        public String id;
        public String name;
        //public String[] friends;

        public User() {

        };

        public User(String id, String name/*, String[] friends*/) {
            this.id = id;
            this.name = name;
            //this.friends = friends;
        }

        private Map<String, Object> innerMap() {
            HashMap<String, Object> result = new HashMap<>();
            result.put("id", id);
            result.put("name", name);

            return result;
        }

        private Map<String, Object> toMap() {
            HashMap<String, Object> result = new HashMap<>();
            result.put(id, innerMap());

            return result;
        }
    }

    User thisUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            thisUser = new User(extras.getString("id"), extras.getString("name"));
            Log.d("user", thisUser.id + thisUser.name);
        }

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
                    boolean joined = false;
                    for (DataSnapshot person : location.child("people").getChildren()) {
                        if (person.getKey().equals(thisUser.id)) { joined = true; };
                        attendees++;
                        // get some data from these people later, to get who's present, demographics, etc.
                    }
                    int[] ids = createPointOfInterest(thisLocationLatitude, thisLocationLongitude, thisLocationName, attendees, joined);

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

                final String eventName = graphicIdToDBRef.get(thisPointId).child("name").getValue().toString();
                final String eventId = graphicIdToDBRef.get(thisPointId).getKey();
                boolean joined;
                locations.child(eventId).child("people").child(thisUser.id).addListenerForSingleValueEvent(new ValueEventListener() {

                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(MapActivity.this);
                            builder1.setMessage("Leave this event?");
                            builder1.setTitle(eventName);
                            builder1.setCancelable(true);

                            builder1.setPositiveButton(
                                    "Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // leave event
                                            locations.child(eventId).child("people").child(thisUser.id).removeValue();
                                        }
                                    });

                            builder1.setNegativeButton(
                                    "No",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    }
                            );

                            AlertDialog eventAlert = builder1.create();
                            eventAlert.show();
                        } else {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(MapActivity.this);
                            builder1.setMessage("Join this event?");
                            builder1.setTitle(eventName);
                            builder1.setCancelable(true);

                            builder1.setPositiveButton(
                                    "Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //join event
                                            AttemptJoinEvent(eventId);

                                        }
                                    });

                            builder1.setNegativeButton(
                                    "No",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    }
                            );

                            AlertDialog eventAlert = builder1.create();
                            eventAlert.show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


                //Context context = getApplicationContext();
                //Toast toast = Toast.makeText(context, text, LENGTH_SHORT);
                //toast.show();

            }
        });
    }

    protected void AttemptJoinEvent(final String eventId) {

        locations.child(eventId).child("people").child(thisUser.id).addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //already joined!
                    Toast toast = Toast.makeText(MapActivity.this, "You've already joined this event!", LENGTH_SHORT);
                    toast.show();

                } else {
                    joinEvent(thisUser, eventId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    };

    protected void joinEvent(final User joiner, String locationID) {
        final DatabaseReference thisLocation = database.getReference("Locations").child(locationID).child("people");
        thisLocation.updateChildren(joiner.toMap());
    };

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

    protected int[] createPointOfInterest(Double latitude, Double longitude, String description, int population, boolean joined) {
        int[] ids = new int[3];

        Log.d("POI", "entered point of interest creator");
        Point pnt = GeometryEngine.project(longitude, latitude, webSR);

        if (joined) {
            SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.BLACK, 45, SimpleMarkerSymbol.STYLE.CIRCLE);
            Graphic graphic = new Graphic(pnt, sms);
            ids[0] = graphicsLayer.addGraphic(graphic);
        }



        SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.RED, 40, SimpleMarkerSymbol.STYLE.CIRCLE);
        Graphic graphic = new Graphic(pnt, sms);
        ids[1] = graphicsLayer.addGraphic(graphic);


        TextSymbol textSymbol = new TextSymbol(30, Integer.toString(population), Color.WHITE, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
        Graphic textGraphic = new Graphic(pnt, textSymbol);
        ids[2] = graphicsLayer.addGraphic(textGraphic);

        return ids;
    }





}

