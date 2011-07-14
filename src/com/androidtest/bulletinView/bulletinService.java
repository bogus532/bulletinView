package com.androidtest.bulletinView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.androidtest.bulletinView.bulletinView;

public class bulletinService extends Service {
	
	private static final String TAG = "BulletinView-service";
	public static String BULLETIN_REFRESHED = "com.androidtest.bulletinView.BULLETIN_REFRESHED";
	
	AlarmManager alarms;
	PendingIntent alarmIntent;
	
	private boolean autoUpdate = false;
	private int updateFreq = 0;
	
	BulletinWidgetLookupTask lastLookup =null;
	
	boolean isWifi;
	
	//private int appstate;
	
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
	    
	    ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
	    
	    if(!isWifi)
	    {
	    	Log.d(TAG,"dont connect by wifi");
	    	return Service.START_NOT_STICKY;
	    }
	    
	    if(autoUpdate) {
	    	
	    	int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
	        long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq*60*1000;
	        alarms.setRepeating(alarmType, timeToRefresh, updateFreq*60*1000, alarmIntent);
	        
	        Log.d(TAG,"Widget Update.................. : ");
	    }
	    else
	    	alarms.cancel(alarmIntent);
	    
	    updateWidget();
	   
	    return Service.START_NOT_STICKY;
	};
	
	private void updateWidget()
	{
		if (lastLookup == null || lastLookup.getStatus().equals(AsyncTask.Status.FINISHED)) 
		{
			Log.d(TAG,"AsyncTask execute");
	        lastLookup = new BulletinWidgetLookupTask();
	        lastLookup.execute((Void[])null);
	    }
		else
		{
			Log.d(TAG,"AsyncTask NOT execute");
		}
	}
	
	public class BulletinWidgetLookupTask extends AsyncTask<Void, Void, Void> {
	    @Override
	    protected Void doInBackground(Void... params) {
	    	
			int totalIndex = 0;
			//if(appstate>0)
			{
				totalIndex = refreshBulletinInWidget();
				Log.d(TAG,"doInBackground : "+ totalIndex);
			}			
			
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
	
	 private int refreshBulletinInWidget() {
		    // XML을 가져온다.
		    URL url;
		    int result=0;
		    try {
		    	String bulletinFeed = getString(R.string.bulletin_feed);
		        url = new URL(bulletinFeed);

		        URLConnection connection;
		        connection = url.openConnection();

		        HttpURLConnection httpConnection = (HttpURLConnection)connection;
		        int responseCode = httpConnection.getResponseCode();

		        if (responseCode == HttpURLConnection.HTTP_OK) {
		            InputStream in = httpConnection.getInputStream(); 
		            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		            DocumentBuilder db = dbf.newDocumentBuilder();

		            //  정보 피드를 파싱한다.
		            Document dom = db.parse(in);
		            Element docEle = dom.getDocumentElement();

		            // 정보로 구성된 리스트를 얻어온다.
		            NodeList nl = docEle.getElementsByTagName("item");
		            result = nl.getLength();
		            if (nl != null && nl.getLength() > 0) {
		                for (int i = 0 ; i < nl.getLength(); i++) {
		                    Element items = (Element)nl.item(i);
		                    Element title = (Element)items.getElementsByTagName("title").item(0);
		                    Element link = (Element)items.getElementsByTagName("link").item(0);
		                    Element date = (Element)items.getElementsByTagName("pubDate").item(0); 
		                    Element author = (Element)items.getElementsByTagName("author").item(0);
		                   
		                    String details = title.getFirstChild().getNodeValue();
		                    
		                    String linkString = link.getFirstChild().getNodeValue()+"&"+link.getLastChild().getNodeValue();
		                   
		                    String dt = date.getFirstChild().getNodeValue();
		                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		                    Date qdate = new GregorianCalendar(0,0,0).getTime();
		                    try {
		                        qdate = sdf.parse(dt);
		                    } catch (ParseException e) {
		                        e.printStackTrace();
		                    }
		                    
		                    String strAuthor = author.getFirstChild().getNodeValue();
		                    
		                    int readcheck = 0;

		                    bulletin bulletinData = new bulletin(details, linkString,qdate,strAuthor,readcheck);

		                    addNewBulletinInWidget(bulletinData);
		                }
		            }
		        }
		    } catch (MalformedURLException e) {
		        e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    } catch (ParserConfigurationException e) {
		        e.printStackTrace();
		    } catch (SAXException e) {
		        e.printStackTrace();
		    } finally {
		    }
		    
		    return result;
		}
	 
	 private void addNewBulletinInWidget(bulletin _bulletin) {
		    ContentResolver cr = getContentResolver();
		    String w = bulletinProvider.KEY_DATE + " = " + _bulletin.getDate().getTime();
		    	    
		    //Log.d(TAG,"addNewBulletin : "+w);
		    
		    if(cr.query(bulletinProvider.CONTENT_URI, null, w, null, null).getCount() == 0)
		    {
		    	ContentValues values = new ContentValues();
		    	
		    	values.put(bulletinProvider.KEY_TITLE,_bulletin.getTitle());
		    	values.put(bulletinProvider.KEY_LINK,_bulletin.getLink());
		    	values.put(bulletinProvider.KEY_DATE,_bulletin.getDate().getTime());
		    	values.put(bulletinProvider.KEY_AUTHOR,_bulletin.getAuthor());
		    	values.put(bulletinProvider.KEY_READCHECK,"0");
		    	
		    	cr.insert(bulletinProvider.CONTENT_URI, values);	
		    	
		    } 
		    
	   	}

}
