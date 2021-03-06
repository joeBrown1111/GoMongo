package com.gomongo.app;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class MongoPhoto extends Activity {
	
	private static final String TAG = "MongoPhoto";
	
	public static final File PICTURE_STORAGE_DIR = new File( Environment.getExternalStorageDirectory(), "GoMongo" );
	public static final File PICTURE_TEMP_DIR = new File( PICTURE_STORAGE_DIR, "temp" );

	private Uri mImageUri;
    private final int TAKE_PHOTO_REQUEST = 0x01;
    private final int OPEN_FROM_GALLERY_REQUEST = 0x02;
	
    private Typeface mBurweedFont;
    
	static {
		if ( !PICTURE_STORAGE_DIR.exists() ) {
			PICTURE_STORAGE_DIR.mkdir();
		}
		
		if ( !PICTURE_TEMP_DIR.exists() ) {
			PICTURE_TEMP_DIR.mkdir();
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.mongo_photo);
        
        View navigationMenu = (View)findViewById(R.id.nav_menu);
        
        NavigationHelper.setupButtonToLaunchActivity(this, navigationMenu, R.id.button_home, Home.class);
        NavigationHelper.setupButtonToLaunchActivity(this, navigationMenu, R.id.button_find_us, FindUs.class);
        NavigationHelper.setupButtonToLaunchActivity(this, navigationMenu, R.id.button_create, CreateBowl.class);
        ImageButton photoButton = (ImageButton)navigationMenu.findViewById(R.id.button_photo);
        photoButton.setBackgroundResource(R.drawable.navigation_tab);
        photoButton.setSelected(true);
        NavigationHelper.setupButtonToLaunchActivity(this, navigationMenu, R.id.button_about, About.class);
        
        mBurweedFont = Typeface.createFromAsset(getAssets(), "fonts/burweed_icg.ttf");
        
        setUpTakePhotoButtonToLaunchCamera();
        
        setupPhotoLibraryButtonToLaunchGallery();
	}

	private void setUpTakePhotoButtonToLaunchCamera() {
		final Context contextForDisplayingErrors = this;
		
		Button takePhotoButton = (Button)findViewById(R.id.button_take_photo);
		takePhotoButton.setTypeface(mBurweedFont);
        takePhotoButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View view) {
        		try {
	        		mImageUri = createTempImage();
	        		
	        		Intent photoIntent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );
	        		photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
	        		
	        		startActivityForResult(photoIntent, TAKE_PHOTO_REQUEST);
        		}
        		catch ( IOException ex ) {
        			Log.w(TAG, String.format("Problem creating temp file: %s", ex.getMessage() ));
        			Toast.makeText(contextForDisplayingErrors, R.string.error_couldnt_access_sdcard, Toast.LENGTH_LONG).show();
        		}
        	}
        });
	}

	private static final String TEMP_IMAGE_NAME = "temp.jpg";
	public static final String IMAGE_PREFIX = "GoMongo";
	public static final String IMAGE_FORMAT = ".jpg";
	
	private Uri createTempImage() throws IOException {
		Uri newImageUri = null;
		
		try {
			File tempFile = new File(PICTURE_TEMP_DIR, TEMP_IMAGE_NAME);
			tempFile.createNewFile(); // Ignore if file exists
			
			Log.d( TAG, String.format( "Created temp file at %s", tempFile.getAbsolutePath() ) );
			
			newImageUri = Uri.fromFile(tempFile);
		}
		catch ( IllegalArgumentException ex ) {
			Log.e( TAG, "Temp image prefix needs to be more than 3 characters long" );
		}
		
		return newImageUri;
	}
	
	private void setupPhotoLibraryButtonToLaunchGallery() {
		Button photoLibraryButton = (Button)findViewById(R.id.button_view_gallery);
        photoLibraryButton.setTypeface(mBurweedFont);
		
		photoLibraryButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick( View view ) {
        		Intent galleryIntent = new Intent( Intent.ACTION_GET_CONTENT );
        		galleryIntent.setType("image/*");
        		startActivityForResult(galleryIntent, OPEN_FROM_GALLERY_REQUEST);
        	}
        });
	}
	
	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data ){
		if( resultCode != RESULT_CANCELED ) {
			Intent annotateImageIntent = new Intent( this, AnnotateImage.class );
			
			switch( requestCode ) {
			case TAKE_PHOTO_REQUEST:
			    annotateImageIntent.putExtra( Intent.EXTRA_STREAM, mImageUri );
	            break;
			case OPEN_FROM_GALLERY_REQUEST:
			    annotateImageIntent.putExtra( Intent.EXTRA_STREAM, data.getData() );
			    break;
			}
			
			startActivity( annotateImageIntent );
		}
	}
}
