package com.androidtest.bulletinView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class bulletinAlarmReceiver extends BroadcastReceiver {
	private static final String TAG = "BulletinView-alarmreceiver";

	public static final String ACTION_REFRESH_BULLETIN_ALARM =
	    "com.androidtest.bulletinView.ACTION_REFRESH_BULLETIN_ALARM";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG,"alaramreceiver  onReceive");
	    Intent startIntent = new Intent(context, bulletinService.class);
	    context.startService(startIntent);
	}
}
