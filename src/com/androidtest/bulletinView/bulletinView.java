package com.androidtest.bulletinView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class bulletinView extends Activity {
	private static final String TAG = "BulletinView";
	public static String BULLETIN_REFRESHED = "com.androidtest.bulletinView.BULLETIN_REFRESHED";
	
	private static final int MENU_UPDATE = Menu.FIRST;
	private static final int MENU_PREFERENCES = Menu.FIRST+1;
	private static final int MENU_DELETE = Menu.FIRST+2;
	private static final int MENU_DELETE_SEEN = Menu.FIRST+3;
	private static final int SHOW_PREFERENCES = 1;
	
	static final private int BULLETIN_DIALOG = 1;
	static final int PROGRESS_DIALOG = 0;
	
	ListView bulletinListView;
	//ArrayAdapter<bulletin> aa;
	bulletinAdapter aa;
	
	ArrayList<bulletin> bulletinArray = new ArrayList<bulletin>();
		
	bulletin selectedbulletin;
	
	ProgressDialog progressDialog;
	public WheelProgressDialog wheelprogressDialog;
	
	boolean myappstate;
	
	//final String bulletin_feed = "http://www.slrclub.com/rss/rss.xml"; //string.xml로 변경
	//final String bulletin_feed ="http://blog.rss.naver.com/htech79.xml";
	//final String bulletin_feed ="http://www.khan.co.kr/rss/rssdata/total_news.xml";
	
	boolean dialog_orientation = false;
	
	boolean autoUpdate = false;
	int updateFreq = 0;
	
	public boolean isWifi;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        myappstate = false;
        
        bulletinListView = (ListView)this.findViewById(R.id.bulletinListView);
        
        bulletinListView.setOnItemClickListener(new OnItemClickListener () {
        	
 			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				
 				selectedbulletin = bulletinArray.get(index);
        		//showDialog(BULLETIN_DIALOG);
 				if(selectedbulletin != null)
 				{
 					String uridata = selectedbulletin.getLink();
 					if(uridata == null)
 					{
 						Toast.makeText(bulletinView.this, R.string.data_null, Toast.LENGTH_SHORT).show();
 						return;
 					}
 					updateBulletinDB(selectedbulletin);
	 				Uri uri = Uri.parse(uridata);
	 				Intent intent  = new Intent(Intent.ACTION_VIEW,uri);
	 				startActivity(intent);
 				}
				
			}
        	
        });
        /*
        int layoutID = android.R.layout.simple_expandable_list_item_1;
        
        aa = new ArrayAdapter<bulletin>(this,layoutID,bulletinArray){
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView txt = new TextView(this.getContext());
				txt.setTextColor(Color.RED);
				txt.setTextSize(25);
				txt.setText(this.getItem(position).toString());
				txt.setAutoLinkMask(Linkify.ALL);
				txt.setLinksClickable(true);
				return txt;
			}
		};
		
        aa = new ArrayAdapter<bulletin>(this,layoutID,bulletinArray);
        */
        int layoutID = R.layout.row;
        
        aa = new bulletinAdapter(this,layoutID,bulletinArray);
        
        bulletinListView.setAdapter(aa);
        
        loadbulletinFromProvider();
        
        updateFromPreferences();
        
        ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
        
        if(isWifi)
        {
        	showDialog(PROGRESS_DIALOG);
        }
        else
        {
        	Toast.makeText(bulletinView.this, R.string.wifi_none, Toast.LENGTH_SHORT).show();
        }
        
        
        //serviceStartFunc();
    }
    
    @Override
    protected void onResume()
    {
        Log.d(TAG, "onResume");
        super.onResume();

        showDialog(PROGRESS_DIALOG);
    }
    
    @Override
    protected void onDestroy()
    {
    	Log.d(TAG, "onDestroy");
    	super.onDestroy();
    	
    	myappstate = true;
    }
    
    @Override
    protected void onStop()
    {
    	Log.d(TAG, "onStop");
    	super.onStop();
    	
    	myappstate = true;
    }
    
        
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);

	    menu.add(0, MENU_UPDATE, Menu.NONE, R.string.menu_update);
	    menu.add(0, MENU_PREFERENCES, Menu.NONE,R.string.preferences_name);
	    menu.add(0, MENU_DELETE, Menu.NONE,R.string.menu_delete);
	    menu.add(0, MENU_DELETE_SEEN, Menu.NONE,R.string.menu_delete_seen);

	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    super.onOptionsItemSelected(item);

	    switch (item.getItemId()) {
	        case (MENU_UPDATE): {
	        	showDialog(PROGRESS_DIALOG);
	            return true;
	        }
	        case (MENU_PREFERENCES) : {
	        	Intent i = new Intent(this, Preferences.class);
	            startActivityForResult(i, SHOW_PREFERENCES);
	            return true;
	        }
	        case (MENU_DELETE): {
	        	deletebulletinFromProvider(0);
	        	showDialog(PROGRESS_DIALOG);
	            return true;
	        }
	        case (MENU_DELETE_SEEN): {
	        	deletebulletinFromProvider(1);
	        	showDialog(PROGRESS_DIALOG);
	            return true;
	        }
	        
	    }
	    return false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);

	    if (requestCode == SHOW_PREFERENCES) {
	        updateFromPreferences();

	        showDialog(PROGRESS_DIALOG);
	        //serviceStartFunc();
	    }
	}
    
    @Override
	public Dialog onCreateDialog(int id) {
	    switch(id) {
	        case (BULLETIN_DIALOG) :
	        	if(dialog_orientation){
	        		removeDialog(BULLETIN_DIALOG);
	        		return null;
	        	}
	            LayoutInflater li = LayoutInflater.from(this);
	            View bulletinDetailsView = li.inflate(R.layout.bulletin_details, null);

	            AlertDialog.Builder bulletinDialog = new AlertDialog.Builder(this);
	            bulletinDialog.setTitle("게시판");
	            bulletinDialog.setView(bulletinDetailsView);
	            dialog_orientation = false;
	            return bulletinDialog.create();
			case PROGRESS_DIALOG:
				
				wheelprogressDialog = WheelProgressDialog.show(this,"","",true,true,null);
				/*
				progressThread = new ProgressThread(handler);
				progressThread.start();
				*/
				new readBulletin().execute();
				//serviceStartFunc();
				
				return wheelprogressDialog;
	    }
	    return null;
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
	    switch(id) {
	        case (BULLETIN_DIALOG) :
	            
	        	if(selectedbulletin == null)
	        	{
	        		dialog_orientation = true;
	        		return;
	        	}
	        		
	            String bulletinText = selectedbulletin.getTitle()+"\n"+selectedbulletin.getLink();
	        	
	            String dateString = "Contents";
	            AlertDialog bulletinDialog = (AlertDialog)dialog;
	            bulletinDialog.setTitle(dateString);
	            TextView tv = (TextView)bulletinDialog.findViewById(R.id.textView1);
	            tv.setText(bulletinText);

	            break;
	    }
	}
	
	private void updateFromPreferences() {
	    Context context = getApplicationContext();
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

	    autoUpdate = prefs.getBoolean(Preferences.PREF_AUTO_UPDATE, false);
	    updateFreq = Integer.parseInt(prefs.getString(Preferences.PREF_UPDATE_FREQ, "0"));
	}
	
	private void serviceStartFunc()
	{
		startService(new Intent(this, bulletinService.class));
	}
    
    private int refreshBulletin() {
	    // XML을 가져온다.
	    URL url;
	    int result=0;
	    try {
	        //String bulletinFeed = bulletin_feed;
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

	            // 이전에 있던 정보들을 모두 삭제한다.
	            bulletinArray.clear();
	            loadbulletinFromProvider();

	            // 정보로 구성된 리스트를 얻어온다.
	            NodeList nl = docEle.getElementsByTagName("item");
	            result = nl.getLength();
	            if (nl != null && nl.getLength() > 0) {
	                for (int i = 0 ; i < nl.getLength(); i++) {
	                    Element items = (Element)nl.item(i);
	                    Element title = (Element)items.getElementsByTagName("title").item(0);
	                    Element link = (Element)items.getElementsByTagName("link").item(0);
	                    //Element date = (Element)items.getElementsByTagName("dc:date").item(0);
	                    Element date = (Element)items.getElementsByTagName("pubDate").item(0); 
	                    Element author = (Element)items.getElementsByTagName("author").item(0);
	                   // Element des = (Element)items.getElementsByTagName("description").item(0);

	                    //String details = (i+1)+". "+title.getFirstChild().getNodeValue();
	                    String details = title.getFirstChild().getNodeValue();
	                    
	                    String linkString = link.getFirstChild().getNodeValue()+"&"+link.getLastChild().getNodeValue();
	                    //below two line for naver blog
	                    //String linkString = link.getFirstChild().getNodeValue();
	                    //linkString = linkString.replace("http://", "http://m.");
	                    
	                    //String qdate = date.getFirstChild().getNodeValue();
	                    String dt = date.getFirstChild().getNodeValue();
	                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	                    Date qdate = new GregorianCalendar(0,0,0).getTime();
	                    try {
	                        qdate = sdf.parse(dt);
	                    } catch (ParseException e) {
	                        e.printStackTrace();
	                    }
	                    
	                    String strAuthor = author.getFirstChild().getNodeValue();
	                    //String strAuthor = "경향신문";
	                    
	                    //Log.d(TAG,linkString);
	                    
	                    //String strDes = des.getFirstChild().getNodeValue();
	                    //Log.d(TAG,strDes);
	                    
	                    int readcheck = 0;

	                    bulletin bulletinData = new bulletin(details, linkString,qdate,strAuthor,readcheck);

	                    // 새로운  정보를 처리한다.
	                    addNewBulletin(bulletinData);
	                    //addNewBulletin_temp(bulletinData);
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
    
    private void addNewBulletin(bulletin _bulletin) {
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
	    	
	    	bulletinArray.add(_bulletin);
	    } 
	    
   	}
    
    private void updateBulletinDB(bulletin _bulletin) {
	    ContentResolver cr = getContentResolver();
	    String w = bulletinProvider.KEY_DATE + " = " + _bulletin.getDate().getTime();
	    	    
	    //Log.d(TAG,"updateBulletinDB : "+w);
	    
	    ContentValues values = new ContentValues();
	    	
	    values.put(bulletinProvider.KEY_READCHECK,"1");
	    	
	    cr.update(bulletinProvider.CONTENT_URI, values, w, null);
	    
   	}
    
    private void addbulletinToArray(bulletin _bulletin)
    {
    	bulletinArray.add(_bulletin);
    }
    
    private void updateListView()
    {
    	int index = aa.getCount();
    	Log.d(TAG,"ListArray count : "+index);
    	aa.notifyDataSetChanged();
    	
    	boolean backup_result = backupDB();
    	Log.d(TAG,"backup restult : "+backup_result);
    }
    
    void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
           inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
           if (inChannel != null)
              inChannel.close();
           if (outChannel != null)
              outChannel.close();
        }
     }
    
    private boolean backupDB()
    {
        File dbFile =
                 new File(Environment.getDataDirectory() + "/data/com.androidtest.bulletinView/databases/bulletin.db");

        File exportDir = new File(Environment.getExternalStorageDirectory(), "exampledata");
        if (!exportDir.exists()) {
           exportDir.mkdirs();
        }
        File file = new File(exportDir, dbFile.getName());

        try {
           file.createNewFile();
           this.copyFile(dbFile, file);
           return true;
        } catch (IOException e) {
           Log.e(TAG, e.getMessage(), e);
           return false;
        }
     }
    
    private void deletebulletinFromProvider(int deletetype)
    {
    	bulletinArray.clear();
    	ContentResolver cr = getContentResolver();
    	
    	String where = null;
    	
    	if(deletetype >0)
    	{
    		where = bulletinProvider.KEY_READCHECK + " = " + "1";
    	}
    	
     	cr.delete(bulletinProvider.CONTENT_URI, where, null);
    }
    
    private void loadbulletinFromProvider() {
	    
	    bulletinArray.clear();

	    ContentResolver cr = getContentResolver();

	    // 저장된 정보를 모두 가져온다.
	    Cursor c = cr.query(bulletinProvider.CONTENT_URI, null, null, null, null);
	    
	    if (c.moveToFirst()) {
	        do {
	            // 세부정보를 얻어온다.
	            
	            String title = c.getString(bulletinProvider.TITLE_COLUMN);
	            String link = c.getString(bulletinProvider.LINK_COLUMN);
	            Long datems = c.getLong(bulletinProvider.DATE_COLUMN);
	            String author = c.getString(bulletinProvider.AUTHOR_COLUMN);
	            int readcheck = c.getInt(bulletinProvider.READCHECK_COLUMN);
	            
	            Date date = new Date(datems);
	            
	            bulletin q = new bulletin(title, link,date,author,readcheck);
	            addbulletinToArray(q);
	        } while(c.moveToNext());
	    }
	    
	    c.close();
	}
    
    private class readBulletin extends AsyncTask<Void, Integer, Integer> {    	
    	   
        
    	// 이곳에 포함된 code는 AsyncTask가 execute 되자 마자 UI 스레드에서 실행됨.
    	// 작업 시작을 UI에 표현하거나
    	// background 작업을 위한 ProgressBar를 보여 주는 등의 코드를 작성.
		@Override
		protected void onPreExecute() {
				
			super.onPreExecute();
		}

		// UI 스레드에서 AsynchTask객체.execute(...) 명령으로 실행되는 callback 
		@Override
		protected Integer doInBackground(Void... arg0) {
			int totalIndex = 0;
			totalIndex = refreshBulletin();
			Log.d(TAG,"doInBackground : "+ totalIndex);
			return totalIndex;
		}  
    	
    	// onInBackground(...)에서 publishProgress(...)를 사용하면
    	// 자동 호출되는 callback으로
    	// 이곳에서 ProgressBar를 증가 시키고, text 정보를 update하는 등의
    	// background 작업 진행 상황을 UI에 표현함.
    	// (예제에서는 UI스레드의 ProgressBar를 update 함) 
    	@Override
    	protected void onProgressUpdate(Integer... progress) {
 
    	}
    	
    	// onInBackground(...)가 완료되면 자동으로 실행되는 callback
    	// 이곳에서 onInBackground가 리턴한 정보를 UI위젯에 표시 하는 등의 작업을 수행함.
    	// (예제에서는 작업에 걸린 총 시간을 UI위젯 중 TextView에 표시함)
    	@Override
    	protected void onPostExecute(Integer result) {
    		bulletinView.this.removeDialog(PROGRESS_DIALOG);
    		
    		Log.d(TAG,"onPostExecute : "+ result);
			
			if(result == 0)
			{
				Toast.makeText(bulletinView.this, R.string.data_null, Toast.LENGTH_SHORT).show();
				return;
			}
			
			updateListView(); 
			sendBroadcast(new Intent(BULLETIN_REFRESHED));
    	}
    	
    	// AsyncTask.cancel(boolean) 메소드가 true 인자로 
    	// 실행되면 호출되는 콜백.
    	// background 작업이 취소될때 꼭 해야될 작업은  여기에 구현.
    	@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		  	
    }
    
    
}