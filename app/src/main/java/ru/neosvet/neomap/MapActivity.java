package ru.neosvet.neomap;


import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private LocationManager mLocManager;
    private View fabMarker, fabOk, fabDelete, ivPointer;
    private TextView tvInfo;
    private Marker marSelect;
    private boolean boolMe = false;
    private double len = 0;
    private LatLng prevLoc = null;
    private long time = 0;
    private String dis = null;
    private Timer tiStop = null;
    private boolean containsResult = false;
    private List<Polyline> line = new ArrayList<Polyline>();
    private Map<String, LatLng> markers = new HashMap<String, LatLng>();
    private Handler hStop = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            tvInfo.setVisibility(View.GONE);
            return false;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        initPermission();
        initViews();
        setViews();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000, 1, locationListener);
            mLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000, 1, locationListener);
        }
    }

    @Override
    public void onBackPressed() {
        if (fabOk.getVisibility() == View.VISIBLE) {
            fabMarker.setVisibility(View.VISIBLE);
            fabOk.setVisibility(View.GONE);
            ivPointer.setVisibility(View.GONE);
        } else if (fabDelete.getVisibility() == View.VISIBLE) {
            clearDistance();
            fabDelete.setVisibility(View.GONE);
            fabMarker.setVisibility(View.VISIBLE);
        } else if (containsResult) {
            clearResult();
        } else {
            super.onBackPressed();
        }
    }

    private void initPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //http://stackoverflow.com/questions/35484767/activitycompat-requestpermissions-not-showing-dialog-box
        if (grantResults.length > 0 && grantResults[0] == 0) {
            LatLng loc = getMeLoc();
            if (loc != null)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 13F));
        } else {
            //Permission Denied
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (marSelect != null) {
                    clearDistance();
                    marSelect = null;
                }
                if (fabDelete.getVisibility() == View.VISIBLE) {
                    fabDelete.setVisibility(View.GONE);
                    fabMarker.setVisibility(View.VISIBLE);
                }
            }
        });
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                len -= SphericalUtil.computeDistanceBetween(polyline.getPoints().get(0), polyline.getPoints().get(1));
                if (line.get(0).equals(polyline))
                    boolMe = false;
                line.remove(polyline);
                polyline.remove();
                if (len == 0)
                    clearDistance();
                else
                    changeDistance();
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (fabOk.getVisibility() == View.GONE) {
                    boolMe = false;
                    LatLng loc = null;
                    if (marker.getTag() != null) { // if search marker
                        loc = getMeLoc();
                        if (fabDelete.getVisibility() == View.VISIBLE) {
                            fabDelete.setVisibility(View.GONE);
                            fabMarker.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (fabMarker.getVisibility() == View.VISIBLE) {
                            loc = getMeLoc();
                            boolMe = true;
                            fabMarker.setVisibility(View.GONE);
                            fabDelete.setVisibility(View.VISIBLE);
                        } else
                            loc = marSelect.getPosition();
                    }
                    if (loc != null) {
                        PolylineOptions lineOpt = new PolylineOptions();
                        lineOpt.add(loc).add(marker.getPosition());
                        lineOpt.color(getResources().getColor(R.color.colorAccent));
                        lineOpt.clickable(true);
                        line.add(mMap.addPolyline(lineOpt));
                        len += SphericalUtil.computeDistanceBetween(loc, marker.getPosition());
                        tvInfo.setVisibility(View.VISIBLE);
                        changeDistance();
                    }
                    marSelect = marker;
                }
                return false;
            }
        });
        LatLng loc = getMeLoc();
        if (loc != null)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 13F));
        loadMarkers();
    }

    private void changeDistance() {
        dis = doubleToString(len, 1) + " m";
        tvInfo.setText(dis);
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            if (tiStop != null)
                tiStop.cancel();
            if (prevLoc == null) {
                prevLoc = new LatLng(loc.getLatitude(), loc.getLongitude());
                time = System.currentTimeMillis();
            } else {
                LatLng newLoc = new LatLng(loc.getLatitude(), loc.getLongitude());
                Double d = SphericalUtil.computeDistanceBetween(prevLoc, newLoc);
                long t = (System.currentTimeMillis() - time) / 1000;
                d = d / t / 360000;
                //tut
                if (boolMe) {
                    LatLng a = line.get(0).getPoints().get(0);
                    LatLng b = line.get(0).getPoints().get(1);
                    PolylineOptions lineOpt = new PolylineOptions();
                    lineOpt.add(newLoc).add(b);
                    lineOpt.color(getResources().getColor(R.color.colorAccent));
                    lineOpt.clickable(true);
                    len -= SphericalUtil.computeDistanceBetween(prevLoc, b);
                    line.get(0).remove();
                    line.remove(0);
                    line.add(0, mMap.addPolyline(lineOpt));
                    len += SphericalUtil.computeDistanceBetween(newLoc, b);
                    dis = doubleToString(len, 1) + " m";
                }
                prevLoc = newLoc;
                tvInfo.setVisibility(View.VISIBLE);
                tvInfo.setText(doubleToString(d, 2) + " km/h" +
                        (dis == null ? "" : "\n" + dis));
                time = System.currentTimeMillis();
            }
            tiStop = new Timer();
            tiStop.schedule(new TimerTask() {
                @Override
                public void run() {
                    prevLoc = null;
                    time = 0;
                    hStop.sendEmptyMessage(0);
                    tiStop = null;
                }
            }, 10000);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private String doubleToString(double d, int k) {
        String s = Double.toString(d);
        s = s.substring(0, s.indexOf(".") + 1 + k);
        return s;
    }

    private LatLng getMeLoc() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location loc = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) {
                loc = mLocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc == null)
                    loc = mLocManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
            if (loc != null) {
                LatLng target = new LatLng(loc.getLatitude(), loc.getLongitude());
                return target;
            }
        }
        return null;
    }

    private void loadMarkers() {
        DataBase db = new DataBase(this);
        SQLiteDatabase sq = db.getWritableDatabase();
        Cursor cursor = sq.query(DataBase.TABLE, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            int iName = cursor.getColumnIndex(DataBase.NAME);
            int iLat = cursor.getColumnIndex(DataBase.LAT);
            int iLng = cursor.getColumnIndex(DataBase.LNG);
            do {
                LatLng loc = new LatLng(cursor.getDouble(iLat), cursor.getDouble(iLng));
                markers.put(cursor.getString(iName), loc);
                addMarker(loc, cursor.getString(iName), false);
            } while (cursor.moveToNext());
        }
        db.close();
    }

    private void addMarker(LatLng loc, String name, boolean boolSearch) {
        MarkerOptions m = new MarkerOptions();
        m.position(loc);
        m.title(name);
        m.snippet(doubleToString(loc.latitude, 4) + "x" + doubleToString(loc.longitude, 4));
        if (boolSearch) {
            m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            Marker mrk = mMap.addMarker(m);
            mrk.setTag("s"); //tag for search marker
        } else
            mMap.addMarker(m);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DataBase db = new DataBase(this);
        SQLiteDatabase sq = db.getWritableDatabase();
        for (String name : markers.keySet()) {
            ContentValues cv = new ContentValues();
            cv.put(DataBase.NAME, name);
            cv.put(DataBase.LAT, markers.get(name).latitude);
            cv.put(DataBase.LNG, markers.get(name).longitude);
            int r = sq.update(DataBase.TABLE, cv, DataBase.NAME + " = ?", new String[]{name});
            if (r == 0) // no update
                sq.insert(DataBase.TABLE, null, cv);
        }
        db.close();
    }

    private void clearDistance() {
        boolMe = false;
        while (line.size() > 0) {
            line.get(0).remove();
            line.remove(0);
        }
        len = 0;
        dis = null;
        tvInfo.setVisibility(View.GONE);
    }

    private void setViews() {
        fabMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearDistance();
                fabMarker.setVisibility(View.GONE);
                fabOk.setVisibility(View.VISIBLE);
                ivPointer.setVisibility(View.VISIBLE);
            }
        });
        fabOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearDistance();
                fabMarker.setVisibility(View.VISIBLE);
                fabOk.setVisibility(View.GONE);
                ivPointer.setVisibility(View.GONE);
                pasteMarker();
            }
        });
        fabDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearDistance();
                DataBase db = new DataBase(MapActivity.this);
                SQLiteDatabase sq = db.getWritableDatabase();
                sq.delete(DataBase.TABLE, DataBase.NAME + " = ?", new String[]{marSelect.getTitle()});
                db.close();
                markers.remove(marSelect.getTitle());
                marSelect.remove();
                fabDelete.setVisibility(View.GONE);
                fabMarker.setVisibility(View.VISIBLE);
            }
        });
        findViewById(R.id.fabSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearDistance();
                searchPlaces();
            }
        });
    }

    private void searchPlaces() {
        clearResult();
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this);
        alertDialog.setMessage(getResources().getString(R.string.search_places));
        final EditText input = new EditText(MapActivity.this);
        input.setBackgroundDrawable(getResources().getDrawable(R.drawable.border));
        input.setText("кафе");
//                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                lp.setMargins(10, 10, 10, 10);
//                input.setLayoutParams(lp);
        alertDialog.setView(input);
        alertDialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String s = input.getText().toString();
                        if (s != null && s.length() > 0) {
                            try {
                                LatLng loc = mMap.getCameraPosition().target;
                                double lat1 = loc.latitude - 0.2;
                                double lat2 = loc.latitude + 0.2;
                                double lng1 = loc.longitude - 0.2;
                                double lng2 = loc.longitude + 0.2;
                                Geocoder geo = new Geocoder(MapActivity.this, Locale.getDefault());
                                //getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude)
                                List<Address> list = geo.getFromLocationName(s, 10, lat1, lng1, lat2, lng2);
                                for (int i = 0; i < list.size(); i++) {
                                    loc = new LatLng(list.get(i).getLatitude(), list.get(i).getLongitude());
                                    addMarker(loc, list.get(i).getFeatureName(), true);
                                }
                                containsResult = list.size() > 0;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
        alertDialog.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }

    private void clearResult() {
        containsResult = false;
        mMap.clear();
        for (String name : markers.keySet()) {
            LatLng loc = new LatLng(markers.get(name).latitude, markers.get(name).longitude);
            markers.put(name, loc);
            addMarker(loc, name, false);
        }
    }

    private void pasteMarker() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this);
        alertDialog.setMessage(getResources().getString(R.string.marker_name));
        final EditText input = new EditText(MapActivity.this);
        input.setBackgroundDrawable(getResources().getDrawable(R.drawable.border));
        input.setText("Marker " + (markers.size() + 1));
//                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                lp.setMargins(10, 10, 10, 10);
//                input.setLayoutParams(lp);
        alertDialog.setView(input);
        alertDialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        if (name != null && name.length() > 0) {
                            if (markers.containsKey(name)) {
                                Toast.makeText(MapActivity.this,
                                        getResources().getString(R.string.name_exist),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                LatLng loc = mMap.getCameraPosition().target;
                                markers.put(name, loc);
                                addMarker(loc, name, false);
                            }
                        }
                    }
                });
        alertDialog.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fabMarker = findViewById(R.id.fabMarker);
        fabOk = findViewById(R.id.fabOk);
        fabDelete = findViewById(R.id.fabDelete);
        ivPointer = findViewById(R.id.ivPointer);
        tvInfo = (TextView) findViewById(R.id.tvInfo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.getItem(1).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.getItem(2).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item != null ? item.getItemId() : 0;
        if (id == R.id.menu_map_mode_normal) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (id == R.id.menu_map_mode_satellite) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            return true;
        } else if (id == R.id.menu_map_mode_terrain) {
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            return true;
        } else if (id == R.id.menu_map_traffic) {
            mMap.setTrafficEnabled(!mMap.isTrafficEnabled());
        } else if (id == R.id.menu_map_location && mMap.isMyLocationEnabled()) {
            LatLng loc = getMeLoc();
            if (loc != null)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15F));
        }
        return super.onOptionsItemSelected(item);
    }
}
