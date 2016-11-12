package com.example.litmus.litmus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;





public class MainActivity extends AppCompatActivity {
    FirebaseDatabase database = FirebaseDatabase.getInstance();

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

        protected Map<String, Object> innerMap() {
            HashMap<String, Object> result = new HashMap<>();
            result.put("id", id);
            result.put("name", name);

            return result;
        }

        protected Map<String, Object> toMap() {
            HashMap<String, Object> result = new HashMap<>();
            result.put(id, innerMap());

            return result;
        }
    }

    public class Location {
        public Double latitude;
        public Double longitude;
        public String name;
        public String id;

        public Location() {

        };

        public Location(Double latitude, Double longitude, String name) {

            Long tsLong = System.currentTimeMillis()/1000;
            String ts = tsLong.toString();

            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
            this.id = name + ts;
        };

        // return

        public Map<String, Object> toMap() {

            HashMap<String, Object> result = new HashMap<>();
            result.put(id, innerMap());

            return result;
        }

        private Map<String, Object> innerMap() {
            HashMap<String, Object> result = new HashMap<>();
            result.put("latitude", latitude);
            result.put("longitude", longitude);
            result.put("name", name);
            result.put("people", false);

            return result;
        }
    }

    User thisUser;
    Location thisLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Location eventlocation = new Location(45.4, 32.4, "YHack");
        //User creator = new User("creatorid", "James");

        // for testing:
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();

        thisUser = new User("id:" + ts, "User Name");

    }


    protected void createEvent(final User creator, final Location location) {
        final DatabaseReference locationsRef = database.getReference("Locations");
        locationsRef.updateChildren(location.toMap(), new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Log.e("update complete", "" + databaseReference);
                locationsRef.child(location.id).child("people").updateChildren(creator.toMap());
            }
        });
    }

    protected void joinEvent(final User joiner, String locationID) {
        final DatabaseReference thisLocation = database.getReference("Locations").child(locationID).child("people");
        thisLocation.updateChildren(joiner.toMap());
    };

    public void viewMap(View view) {

        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("name", thisUser.name);
        intent.putExtra("id", thisUser.id);
        startActivity(intent);
    }

    public void createEventDialog(View view) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setMessage("What's the event?");
        builder1.setTitle("Create Event");
        builder1.setCancelable(true);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder1.setView(input);

        builder1.setPositiveButton(
                "Create Event",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //join event
                        String eventName = input.getText().toString();
                        thisLocation = new Location(41.2963, -72.9223, eventName);
                        createEvent(thisUser, thisLocation);

                        Intent intent = new Intent(MainActivity.this, MapActivity.class);
                        intent.putExtra("name", thisUser.name);
                        intent.putExtra("id", thisUser.id);
                        startActivity(intent);

                    }
                });

        AlertDialog eventAlert = builder1.create();
        eventAlert.show();
    }





}