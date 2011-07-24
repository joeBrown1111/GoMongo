package com.gomongo.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gomongo.data.Bowl;
import com.gomongo.data.DatabaseOpenHelper;
import com.gomongo.data.Food;
import com.gomongo.data.IngredientCount;
import com.gomongo.net.StaticWebService;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager.SqliteOpenHelperFactory;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

public class ShareBowl extends OrmLiteBaseActivity<DatabaseOpenHelper> implements OnClickListener {
    
    // Static initialization of DB open helper factory
    static { 
        OpenHelperManager.setOpenHelperFactory(new SqliteOpenHelperFactory() { 
            public OrmLiteSqliteOpenHelper getHelper( Context context ) {
                return new DatabaseOpenHelper(context);
            }
        } );
    }
    
    private static String TAG = "ShareBowl";
    
    private static String CREATE_BOWL_URL = "http://www.gomongo.com/iphone/iPhonePrintRecipe.php";
    
    private static int NUTRITION_INFO_DIALOG_ID = 0x1;
    private static String ROOT = "root";
    private static String BOWL_NAME = "bowlname";
    
    private Food mTotalNutritionContainer = new Food();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.share_bowl);
        
        int bowlId = getIntent().getExtras().getInt(CreateBowl.EXTRA_BOWL_ID);
        try {
            Dao<IngredientCount,Integer> ingredientDao = getHelper().getDao(IngredientCount.class);
            Dao<Food,Integer> foodDao = getHelper().getDao(Food.class);
            
            Dao<Bowl,Integer> bowlDao = getHelper().getDao(Bowl.class);
            Bowl bowl = bowlDao.queryForId(bowlId);
            
            TextView shareTitle = (TextView)findViewById(R.id.textview_bowl_title);
            shareTitle.setText(bowl.getTitle());
            
            QueryBuilder<IngredientCount,Integer> builder = ingredientDao.queryBuilder();
            builder.where().eq( IngredientCount.COL_BOWL_ID, bowlId).and().gt(IngredientCount.COL_COUNT, 0);
            List<IngredientCount> allIngredients = ingredientDao.query( builder.prepare() );
            
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            xmlBuilder.append(String.format("<%s>", ROOT));
            xmlBuilder.append(String.format( "<%1$s>%2$s</%1$s>", BOWL_NAME, bowl.getTitle() ) );
            for( IngredientCount ingredientCount : allIngredients ) {
                Food ingredient = ingredientCount.getIngredient();
                foodDao.refresh(ingredient);
                
                ingredient.writeFoodXml(xmlBuilder, ingredientCount.getCount());
                
                mTotalNutritionContainer.addCalories(ingredient.getTotalCalories() * ingredientCount.getCount());
                mTotalNutritionContainer.addProtein(ingredient.getProtein() * ingredientCount.getCount());
                mTotalNutritionContainer.addTotalFat(ingredient.getTotalFat() * ingredientCount.getCount());
                mTotalNutritionContainer.addSaturatedFat(ingredient.getSaturatedFat() * ingredientCount.getCount());
                mTotalNutritionContainer.addCarbs(ingredient.getCarbs() * ingredientCount.getCount());
                mTotalNutritionContainer.addDietaryFiber(ingredient.getDietaryFiber() * ingredientCount.getCount());
            }
            
            mTotalNutritionContainer.writeSummaryXml(xmlBuilder);
            
            xmlBuilder.append( String.format( "</%s>", ROOT ) );
            
            //DEBUG
            Log.d(TAG, String.format( "Posting XML: %s", xmlBuilder.toString() ) );
            //DEBUG
            
            InputStream response = StaticWebService.postGetResponse(CREATE_BOWL_URL, "printBowl", xmlBuilder.toString(), "printBowlName", bowl.getTitle());
            //InputStreamReader reader = new InputStreamReader( response );
            
            // GET BYTE STREAM HERE
            Bitmap image = BitmapFactory.decodeStream(response);
            
            // DEBUG
            File temp = new File( MongoPhoto.PICTURE_TEMP_DIR, "recipe.jpg" );
            OutputStream fileStream = new FileOutputStream(temp);
            image.compress(CompressFormat.PNG, 100, fileStream);
            fileStream.close();
            //DEBUG
            
        } catch (SQLException ex ) {
            Log.w(TAG, "Couldn't open the database for writing", ex );
            
            Toast.makeText(this, R.string.error_problem_connecting_to_database, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.w(TAG, "Problem connecting to internet on prepare share bowl call." );
            // Might want to explain why bowl wont work...
        }
        
        Button startOver = (Button)findViewById(R.id.button_create_another_bowl);
        startOver.setOnClickListener(this);
        
        Button nutritionInfo = (Button)findViewById(R.id.button_nutrition_info);
        nutritionInfo.setOnClickListener(this);
    }

    @Override
    public Dialog onCreateDialog( int id ) {
        Dialog dialog = new Dialog(this);
        if( id == NUTRITION_INFO_DIALOG_ID ) {
            dialog.setContentView(R.layout.nutrition_info);
            dialog.setTitle(R.string.nut_info_title);
            View nutritionalTableRoot = dialog.findViewById(R.id.nutrition_info_main);
            
            removeUnnecessaryTableRowsFromDialog( nutritionalTableRoot );
            
            NutritionFactsViewHelper.fillOutCommonFactsLayout(nutritionalTableRoot, mTotalNutritionContainer);
        }
        
        return dialog;
    }

    private void removeUnnecessaryTableRowsFromDialog( View tableRoot ) {
        View inTableTitle = tableRoot.findViewById(R.id.nutrition_in_table_title);
        inTableTitle.setVisibility(View.GONE);
        
        View servingSizeRow = tableRoot.findViewById(R.id.nutrition_serving_size_row);
        servingSizeRow.setVisibility(View.GONE);
    }
    
    @Override
    public void onClick(View clickedView) {
        
        switch( clickedView.getId() ) {
        case R.id.button_nutrition_info:
            showDialog(NUTRITION_INFO_DIALOG_ID);
            break;
        case R.id.button_send_bowl_email:
            break;
        case R.id.button_send_bowl_facebook:
            break;
        case R.id.button_send_bowl_twitter:
            break;
        case R.id.button_create_another_bowl:
            Intent createNewBowl = new Intent( this, CreateBowl.class );
            startActivity(createNewBowl);
            break;
        }
        
    }
}