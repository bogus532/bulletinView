package com.androidtest.bulletinView;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

public class bulletinWidget extends AppWidgetProvider {
	
	private static final String TAG = "bulletinView-widget";
	
	public void updateBulletin(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Cursor lastbulletin;
		ContentResolver cr = context.getContentResolver();
		lastbulletin = cr.query(bulletinProvider.CONTENT_URI, null, null, null, null);
		RemoteViews views;

		String details = "-- None --";
		String author = "";
		String uridata = "";
		
		Log.d(TAG,"updateBulletin");
				
		if (lastbulletin != null) {
			try 
			{
				int temp = lastbulletin.getCount();
				Log.d("bulletinView-widget","getPosition : "+temp);
				int index = (int)(Math.random()*temp);
				lastbulletin.moveToPosition(index);
				details = lastbulletin.getString(bulletinProvider.TITLE_COLUMN);
				author = lastbulletin.getString(bulletinProvider.AUTHOR_COLUMN);
				uridata = lastbulletin.getString(bulletinProvider.LINK_COLUMN);
				
				Log.d(TAG,"details : "+details+", author : "+author);
				
				/*
				if (lastbulletin.moveToFirst()) 
				{
					details = lastbulletin.getString(bulletinProvider.TITLE_COLUMN);
					Log.d("bulletinView-widget","widget : "+details);
				}
				*/
			} 
			finally 
			{
				lastbulletin.close();
			}
		}
		
		final int N = appWidgetIds.length;
	    for (int i = 0; i < N; i++) {
	        int appWidgetId = appWidgetIds[i];
	        views = new RemoteViews(context.getPackageName(), R.layout.bulletin_widget);
	        views.setTextViewText(R.id.widget_details, details);
	        views.setTextViewText(R.id.widget_author, author);
	        
	        Uri uri = Uri.parse(uridata);
			Intent detailIntent  = new Intent(Intent.ACTION_VIEW,uri);
			
			PendingIntent pending = PendingIntent.getActivity(context, 0, detailIntent, 0);

	        views.setOnClickPendingIntent(R.id.widget_layout, pending);
			
	        appWidgetManager.updateAppWidget(appWidgetId, views);
	    }
	}

	public void updateBulletin(Context context) {
	    ComponentName thisWidget = new ComponentName(context, bulletinWidget.class);
	    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
	    updateBulletin(context, appWidgetManager, appWidgetIds);
	}
 
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		updateBulletin(context, appWidgetManager, appWidgetIds);
	}
	
	@Override
	public void onReceive(Context context, Intent intent){
	    super.onReceive(context, intent);
	    
	    Log.d(TAG,"onReceive");
	    
	    if (intent.getAction().equals(bulletinService.BULLETIN_REFRESHED))
	        updateBulletin(context);
	}

}
