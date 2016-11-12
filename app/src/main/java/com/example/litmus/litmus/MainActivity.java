package com.example.litmus.litmus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.content.Intent;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;





public class MainActivity extends AppCompatActivity {
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    public class User {
        // going to need to handle user duplicates at some point
        public String id;
        public String name;

        public User() {

        };

        public User(String id, String name) {
            this.id = id;
            this.name = name;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* for testing:

        Location eventlocation = new Location(45.4, 32.4, "YHack");
        User creator = new User("creatorid", "James");
        User joiner = new User("joinerid", "Ben");

        createEvent(creator, eventlocation);
        joinEvent(joiner, eventlocation.id);

        */
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
        startActivity(intent);
    }

    public void joinEvent(View view) {

    }

    public void createEvent(View view) {

    }





}