package com.androidtest.bulletinView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class bulletinService extends Service {
	
	private static final String TAG = "BulletinView-service";
	public static String BULLETIN_REFRESHED = "com.androidtest.bulletinView.BULLETIN_REFRESHED";
	
	AlarmManager alarms;
	PendingIntent alarmIntent;
	
	private boolean autoUpdate = false;
	private int updateFreq = 0;
	
	BulletinLookupTask lastLookup =null;
	
	@Override
	public void onCreate() {
	
		Log.d(TAG,"service onCreate");
		alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

	    Intent intentToFire = new Intent(BULLETIN_REFRESHED);
	    alarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);
	}

	public bulletinService() {

	}

	@Override
	public IBinder onBind(Intent arg0) {

		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.d(TAG,"service onStartCommand");
	    
	    Context context = getApplicationContext();
	    SharedPreferences prefs =
	        PreferenceManager.getDefaultSharedPreferences(context);

	    autoUpdate = 
	    	prefs.getBoolean(Preferences.PREF_AUTO_UPDATE, false);
	    updateFreq = 
	    	Integer.parseInt(prefs.getString(Preferences.PREF_UPDATE_FREQ, "0"));
	    
	    Log.d(TAG,"onStartCommand - "+"autoUpdate : "+autoUpdate+", updateFreq : "+updateFreq);
	    
	    if(autoUpdate) {
	    	
	    	int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
	        long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq*60*1000;
	        alarms.setRepeating(alarmType, timeToRefresh, updateFreq*60*1000, alarmIntent);
	        
	        Log.d(TAG,"Update.................. : ");
	    }
	    else
	    	alarms.cancel(alarmIntent);
	    
	    updateWidget();
	   
	    return Service.START_NOT_STICKY;
	};
	
	private void updateWidget()
	{
		if (lastLookup == null || lastLookup.getStatus().equals(AsyncTask.Status.FINISHED)) {
	        lastLookup = new BulletinLookupTask();
	        lastLookup.execute((Void[])null);
	    }
	}
	
	public class BulletinLookupTask extends AsyncTask<Void, Void, Void> {
	    @Override
	    protected Void doInBackground(Void... params) {
	    	Intent intent = new Intent(BULLETIN_REFRESHED);
	    	sendBroadcast(intent);
	        return null;
	    }

	    @Override
	    protected void onProgressUpdate(Void... values) {
	        super.onProgressUpdate(values);
	    }

	    @Override
	    protected void onPostExecute(Void result) {
	        super.onPostExecute(result);
	        stopSelf();
	    }
	}

}
