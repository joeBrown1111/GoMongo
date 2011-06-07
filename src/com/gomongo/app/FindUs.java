package com.gomongo.app;

import android.os.Bundle;
import android.view.View;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class FindUs extends MapActivity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.find_us);
        
        View navigationMenu = (View)findViewById(R.id.nav_menu);
        
        NavigationHelper.setupButtonToLaunchActivity(this, navigationMenu, R.id.button_home, Home.class);
        NavigationHelper.setupButtonToLaunchActivity(this, navigationMenu, R.id.button_create, CreateBowl.class);
        NavigationHelper.setupButtonToLaunchActivity(this, navigationMenu, R.id.button_photo, MongoPhoto.class);
        NavigationHelper.setupButtonToLaunchActivity(this, navigationMenu, R.id.button_about, About.class);
        
        MapView mapView = (MapView)findViewById(R.id.find_us_map);
        mapView.setBuiltInZoomControls(true);
	}
	
	// FIXME: dont show any routes for now
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
