package com.example.arbattleship;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import android.Manifest;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private boolean center_map = true;
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;
    TextView txtLat;
    String lat;
    String provider;
    protected String latitude, longitude;
    protected boolean gps_enabled, network_enabled;
    private MapView map;
    private IMapController mapController;

    private static final String TAG = "OsmActivity";


    private static final int PERMISSION_REQUEST_CODE = 1;
    Location oldLocation = null;
    double currentLat = 0.0;
    double currentLon = 0.0;
    double seaMovingOffset = 0.0;
    Marker startMarker = null;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    CalculateTileNo tileCalc;
    static double latMod = 0.0;
    static double lonMod = 0.0;
    double a = 0.0001;
    void change_location(){
        GeoPoint startPoint = new GeoPoint(oldLocation.getLatitude()+latMod, oldLocation.getLongitude()+lonMod);
        if(center_map)
            mapController.setCenter(startPoint);
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //handle permissions first, before map is created. not depicted here
        setContentView(R.layout.activity_main);

        txtLat = (TextView) findViewById(R.id.textview1);
        tileCalc = new CalculateTileNo();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);


        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //inflate and create the map

        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= 23) {
            if (isStoragePermissionGranted()) {

            }
        }



        findViewById(R.id.goUp).setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                System.out.println("pressed");
                latMod+=a;
                change_location();
                return false;
            }
        });
        findViewById(R.id.goDown).setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                System.out.println("pressed");
                latMod-=a;
                change_location();
                return false;
            }
        });
        findViewById(R.id.left).setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                System.out.println("pressed");
                lonMod-=a;
                change_location();
                return false;
            }
        });
        findViewById(R.id.right).setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                System.out.println("pressed");
                lonMod+=a;
                change_location();
                return false;
            }
        });


        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        mapController = map.getController();
//        mapController.setZoom(17.5);
        mapController.setZoom(17.5);
        map.setMaxZoomLevel(19.0);
        map.setMinZoomLevel(16.0);
        GeoPoint startPoint = new GeoPoint(51496994, -134733);
        mapController.setCenter(startPoint);
        findViewById(R.id.button).setOnClickListener(v -> {
            if(center_map){
                center_map = false;
            } else {
                center_map = true;
                GeoPoint point_to_move_to = new GeoPoint(oldLocation.getLatitude()+latMod, oldLocation.getLongitude()+lonMod);
                mapController.setCenter(point_to_move_to);
                redraw_field_on_location_change();
            }
            tileCalc.print_probabilities();

        });
        findViewById(R.id.button2).setOnClickListener(v -> {
            if(oldLocation!=null)
                attack_tile((int)convertLon(oldLocation.getLongitude()+lonMod), (int)convertLat(oldLocation.getLatitude()+latMod));
        });
        findViewById(R.id.button3).setOnClickListener(v -> {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.clear();
            editor.apply();
            empty_map();
        });
        map.addMapListener(new DelayedMapListener(new MapListener() {
            public boolean onZoom(final ZoomEvent e) {
                map_scrolled_or_moved();
                return true;
            }

            public boolean onScroll(final ScrollEvent e) {
                map_scrolled_or_moved();
                return true;
            }
        }, 1000 ));
        start_sea_moving();
    }
    private void start_sea_moving(){
        new Thread() { @Override public void run() { try {
                    while(true) {
                        sleep(1000);
                        seaMovingOffset+=0.1;
//                        empty_map();
                    }
                } catch (InterruptedException e) { e.printStackTrace(); }}
        }.start();
    }

    private void empty_map(){
        for(Map.Entry<String, GroundOverlay> entry : map_tiles.entrySet()) {
            map.getOverlays().remove(entry.getValue());
        }
        map_tiles.clear();
        tileCalc.tileMap.clear();
        tileCalc.seedMap.clear();
        redraw_field_on_location_change();
    }

    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        if (map != null)
            map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        if (map != null)
            map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }


    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ( checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                Log.v(TAG, "Permission is granted");
                return true;
            } else{

                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
            //resume tasks needing this permission
        }
    }
    @Override
    public void onLocationChanged(Location location) {
            oldLocation = location;
            txtLat = (TextView) findViewById(R.id.textview1);
            txtLat.setText("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
            GeoPoint startPoint = new GeoPoint(location.getLatitude()+latMod, location.getLongitude()+lonMod);
            currentLat = location.getLatitude();
            currentLon = location.getLongitude();

            if(center_map)
                mapController.setCenter(startPoint);
    }

    HashMap<String, GroundOverlay> map_tiles = new HashMap<String, GroundOverlay>();
    private void redraw_field_on_location_change(){
        //Get NSEW and Rescale
        BoundingBox corner_box = map.getProjection().getBoundingBox();
        double n = convertLat(corner_box.getLatNorth());
        double s = convertLat(corner_box.getLatSouth());
        double e = convertLon(corner_box.getLonEast());
        double w = convertLon(corner_box.getLonWest());
        int extra_edges = 5;
        for (int lon = (int) (w) - 1 - extra_edges; lon < (int) (e) + 1 + extra_edges; lon++) {
            for (int lat = (int) (s) - extra_edges; lat < (int) (n) + 1 + extra_edges; lat++) {
                if(!map_tiles.containsKey(lon+"a"+lat)) {
                    map_tiles.put(lon+"a"+lat, add_overlay_to_map(lon,lat,get_tile_drawable(lon, lat), 100.0f, 100.0f, 0.0f));
                }
            }
        }
    }
    private Drawable get_tile_drawable(int lon, int lat){
        int[] tile_info = tileCalc.getTileNo(lon, lat);
        String tileName = "empty_ocean";
        float alpha = 0.5f;
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        if(!sharedPref.contains(lon+"a"+lat)) {

        } else {
            tileName = "empty_ocean";
            alpha = 0.15f;
            if (tile_info[0] != 0) {
                tileName = "ship" + (tile_info[0] - 1) + "_" + tile_info[1] + "_" + tile_info[2];
                if (tileName.contains("-"))
                    tileName = tileName.replace("-", "m");
                alpha = 0.75f;
            }
        }
        Drawable d = getResources().getDrawable(getResources().getIdentifier(tileName, "drawable", getPackageName()));
        d.setAlpha((int)(alpha*255));
        return d;
    }
    private GroundOverlay add_overlay_to_map(int lon, int lat, Drawable d, float w, float h, float alpha){
        lon = (int)(unConvertLon(lon)*1E6);
        lat = (int)(unConvertLat(lat)*1E6);
        GeoPoint overlayCenterPoint = new GeoPoint(lat,lon);
        GroundOverlay myGroundOverlay = new GroundOverlay();
        myGroundOverlay.setPosition(overlayCenterPoint);
        myGroundOverlay.setImage(d.mutate());
        myGroundOverlay.setDimensions(w,h);
        myGroundOverlay.setTransparency(alpha);
        myGroundOverlay.setBearing(0);
        map.getOverlays().add(myGroundOverlay);
        map.invalidate();
        return myGroundOverlay;
    }

    private void attack_tile(int lon, int lat){
        Log.d("HELLO", lon+" " + lat);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        if(!sharedPref.contains(lon+"a"+lat)); {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(lon+"a"+lat,0);
            editor.apply();
            if(map_tiles.containsKey(lon+"a"+lat)){
                Log.d("HELLO", lon+"a" + lat);
                GroundOverlay mGroundOverlay = map_tiles.get(lon+"a"+lat);
                mGroundOverlay.setImage(get_tile_drawable(lon, lat));
                map.invalidate();
            }
        }
    }
    int s_lat = 1120;
    int s_lon = 820;
    private double convertLat(double lat){
        return s_lat*lat;
    }
    private double convertLon(double lon){
        return s_lon*lon;
    }
    private double unConvertLat(double lat){
        return lat/s_lat;
    }
    private double unConvertLon(double lon){
        return lon/s_lon;
    }
    private String getKey(int lat, int lon){
        return lat+"a"+lon;
    }


    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }
    private void map_scrolled_or_moved(){
        redraw_field_on_location_change();
    }

    private boolean checkStoragePermissions(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG,"Permission is granted");
                    return true;
                } else {

                    Log.v(TAG,"Permission is revoked");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    return false;
                }
            }
            else { //permission is automatically granted on sdk<23 upon installation
                Log.v(TAG,"Permission is granted");
                return true;
            }
    }
    public class GroundOverlay extends Overlay {

        protected Drawable mImage;
        protected GeoPoint mPosition;
        protected float mBearing;
        protected float mWidth, mHeight;
        protected float mTransparency;
        public final static float NO_DIMENSION = -1.0f;
        protected Point mPositionPixels, mSouthEastPixels;

        public GroundOverlay() {
            super();
            mWidth = 10.0f;
            mHeight = NO_DIMENSION;
            mBearing = 0.0f;
            mTransparency = 0.0f;
            mPositionPixels = new Point();
            mSouthEastPixels = new Point();
        }

        public void setImage(Drawable image){
            mImage = image;
        }

        public Drawable getImage(){
            return mImage;
        }

        public GeoPoint getPosition(){
            return mPosition.clone();
        }

        public void setPosition(GeoPoint position){
            mPosition = position.clone();
        }

        public float getBearing(){
            return mBearing;
        }

        public void setBearing(float bearing){
            mBearing = bearing;
        }

        public void setDimensions(float width){
            mWidth = width;
            mHeight = NO_DIMENSION;
        }

        public void setDimensions(float width, float height){
            mWidth = width;
            mHeight = height;
        }

        public float getHeight(){
            return mHeight;
        }

        public float getWidth(){
            return mWidth;
        }

        public void setTransparency(float transparency){
            mTransparency = transparency;
        }

        public float getTransparency(){
            return mTransparency;
        }

        protected void computeHeight(){
            if (mHeight == NO_DIMENSION && mImage != null){
                mHeight = mWidth * mImage.getIntrinsicHeight() / mImage.getIntrinsicWidth();
            }
        }

        /** @return the bounding box, ignoring the bearing of the GroundOverlay (similar to Google Maps API) */
        public BoundingBox getBoundingBox(){
            computeHeight();
            GeoPoint pEast = mPosition.destinationPoint(mWidth, 90.0f);
            GeoPoint pSouthEast = pEast.destinationPoint(mHeight, -180.0f);
            double north = mPosition.getLatitude()*2 - pSouthEast.getLatitude();
            double west = mPosition.getLongitude()*2 - pEast.getLongitude();
            return new BoundingBox(north, pEast.getLongitude(), pSouthEast.getLatitude(), west);
        }

        public void setPositionFromBounds(BoundingBox bb){
            mPosition = bb.getCenterWithDateLine();
            GeoPoint pEast = new GeoPoint(mPosition.getLatitude(), bb.getLonEast());
            GeoPoint pWest = new GeoPoint(mPosition.getLatitude(), bb.getLonWest());
            mWidth = (float)pEast.distanceToAsDouble(pWest);
            GeoPoint pSouth = new GeoPoint(bb.getLatSouth(), mPosition.getLongitude());
            GeoPoint pNorth = new GeoPoint(bb.getLatNorth(), mPosition.getLongitude());
            mHeight = (float)pSouth.distanceToAsDouble(pNorth);
        }

        @Override public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow)
                return;
            if (mImage == null)
                return;

            computeHeight();

            final Projection pj = mapView.getProjection();

            pj.toPixels(mPosition, mPositionPixels);
            GeoPoint pEast = mPosition.destinationPoint(mWidth/2, 90.0f);
            GeoPoint pSouthEast = pEast.destinationPoint(mHeight/2, -180.0f);
            pj.toPixels(pSouthEast, mSouthEastPixels);
            int hWidth = mSouthEastPixels.x-mPositionPixels.x;
            int hHeight = mSouthEastPixels.y-mPositionPixels.y;
            mImage.setBounds(-hWidth, -hHeight, hWidth, hHeight);

//            mImage.setAlpha(255-(int)(mTransparency*255));

            drawAt(canvas, mImage, mPositionPixels.x, mPositionPixels.y, false, -mBearing);
        }
    }
}