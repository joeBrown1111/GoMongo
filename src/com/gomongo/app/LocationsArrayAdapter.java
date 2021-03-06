package com.gomongo.app;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class LocationsArrayAdapter extends ArrayAdapter<MongoLocation> {

	private Handler mAsyncAdapterActions;
	
	private static final String LOCATION_KEY = "location";
	
	private final int ADD_MESSAGE = 0x01;
	
	public LocationsArrayAdapter(Context context, List<MongoLocation> objects) {
		super(context, R.id.location_title, objects);
		
		final LocationsArrayAdapter itemToSyncOn = this;
		
		mAsyncAdapterActions = getUIThreadHandlerForUpdates( itemToSyncOn );
	}

	private Handler getUIThreadHandlerForUpdates( final LocationsArrayAdapter itemToSyncOn) {
		return new Handler() {
			@Override
			public void handleMessage( Message message ) {
				switch ( message.arg1 ) {
				case ADD_MESSAGE:    
				    synchronized( itemToSyncOn ) {
    					add( (MongoLocation)message.getData().getParcelable(LOCATION_KEY) );
    				}
				    break;
				}
			}
		};
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent ) {
		LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
		convertView = inflater.inflate(R.layout.location_list_item, null);
		
		final MongoLocation locationToDisplay = getItem(position);
		final Context context = getContext();
		
		setTextOnView(convertView, R.id.location_title, locationToDisplay.getTitle());
		setTextOnView(convertView, R.id.location_address, locationToDisplay.getSnippet());
		
		String distanceFormat = context.getResources().getString( R.string.distance_away_format );
		setTextOnView(convertView, R.id.location_distance, String.format( distanceFormat, locationToDisplay.getDistance() ) );
		
		convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View clickedView) {
                Intent moreDetailsIntent = new Intent( context, LocationDetails.class );
                moreDetailsIntent.putExtra(MongoLocation.EXTRA_LOCATION, locationToDisplay);
                context.startActivity(moreDetailsIntent);
            }
		});
		
		return convertView;
	}

	public void postAddItem(MongoLocation location) {
		Message addLocationMessage = Message.obtain(mAsyncAdapterActions);
		addLocationMessage.arg1 = ADD_MESSAGE;
		
		Bundle locationData = new Bundle();
		locationData.putParcelable( LOCATION_KEY, location );
		addLocationMessage.setData( locationData );
		
		addLocationMessage.sendToTarget();
	}
	
	private void setTextOnView(View convertView, int textResourceId, String text) {
		TextView textView = (TextView)convertView.findViewById(textResourceId);
		textView.setText(text);
	}
	
}
