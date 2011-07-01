package com.androidtest.bulletinView;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class bulletinProvider extends ContentProvider {
	
	public static final Uri CONTENT_URI = Uri.parse("content://com.androidtest.provider.bulletinView/bulletin");
	
	// 하부 데이터베이스.
	private SQLiteDatabase bulletinDB;
	private static final String TAG = "BulletinProvider";
	private static final String DATABASE_NAME = "bulletin.db";
	private static final int DATABASE_VERSION = 1;
	private static final String BULLETIN_TABLE = "bulletin";

	// 열 이름.
	public static final String KEY_ID = "_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_LINK = "link";
	public static final String KEY_DATE = "date";
	public static final String KEY_AUTHOR = "author";
	public static final String KEY_READCHECK = "readcheck";
	

	// 열 인덱스.
	public static final int TITLE_COLUMN = 1;
	public static final int LINK_COLUMN = 2;
	public static final int DATE_COLUMN = 3;
	public static final int AUTHOR_COLUMN = 4;
	public static final int READCHECK_COLUMN = 5;
	
	private static final int B_ALL = 1;
	private static final int B_ID = 2;
	
	private static final UriMatcher uriMatcher;
	
	static {
	    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	    uriMatcher.addURI("com.androidtest.provider.bulletinView", "bulletin", B_ALL);
	    uriMatcher.addURI("com.androidtest.provider.bulletinView", "bulletin/#", B_ID);
	 }
	
	private static class BulletinDatabaseHelper extends SQLiteOpenHelper {
		
		private static final String DATABASE_CREATE = 
			"create table " + BULLETIN_TABLE + " ("
	        + KEY_ID + " integer primary key autoincrement, "
	        + KEY_TITLE + " TEXT, "
	        + KEY_LINK + " TEXT, "
	        + KEY_DATE + " INTEGER, "
	        + KEY_AUTHOR + " TEXT, "
	        + KEY_READCHECK + " INTEGER);";
		
		public BulletinDatabaseHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
			
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
	                   + newVersion + ", which will destroy all old data");

	        db.execSQL("DROP TABLE IF EXISTS " + BULLETIN_TABLE);
	        onCreate(db);
		}

		
	}

	@Override
	public int delete(Uri uri, String where, String[] wArgs) {
		int count;

	    switch (uriMatcher.match(uri)) {
	        case B_ALL:
	            count = bulletinDB.delete(BULLETIN_TABLE, where, wArgs);
	            break;

	        case B_ID:
	            String segment = uri.getPathSegments().get(1);
	            count = bulletinDB.delete(BULLETIN_TABLE, KEY_ID + "="
	                                                + segment
	                                                + (!TextUtils.isEmpty(where) ? " AND ("
	                                                + where + ')' : ""), wArgs);
	            break;

	        default: throw new IllegalArgumentException("Unsupported URI: " + uri);
	    }

	    getContext().getContentResolver().notifyChange(uri, null);
	    return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
    	case B_ALL: return "vnd.android.cursor.dir/vnd.androidtest.bulletinView";
        case B_ID: return "vnd.android.cursor.item/vnd.androidtest.bulletinView";
        default: throw new IllegalArgumentException("Unsupported URI: " + uri);
    }
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// 새로운 행을 삽입한다.
	    // 행이 성공적으로 삽입된 경우에는, 행 번호가 리턴 될 것이다.
	    long rowID = bulletinDB.insert(BULLETIN_TABLE, "bulletin", initialValues);

	    // 성공적으로 삽입된 새로운 행의 URI를 리턴한다.
	    if (rowID > 0) {
	        Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
	        getContext().getContentResolver().notifyChange(_uri, null);
	        return _uri;
	    }
	    throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		Context context = getContext();

		BulletinDatabaseHelper dbHelper = new BulletinDatabaseHelper(context,
	        DATABASE_NAME, null, DATABASE_VERSION);
		bulletinDB = dbHelper.getWritableDatabase();
	    return (bulletinDB == null) ? false : true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sort) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

	    qb.setTables(BULLETIN_TABLE);

	    // URI가 하나의 행을 요청하는 형식이라면,
	    // 결과 셋이 요청된 행만을 가지도록 제한한다.
	    switch (uriMatcher.match(uri)) {
	        case B_ID:
	        	qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
	            break;
	        default :
	        	break;
	    }

	    // 지정된 정렬 순서가 없을 때는, 날짜/시간 순으로 정렬한다.
	    
	    String orderBy;
	    if (TextUtils.isEmpty(sort)) {
	        orderBy = KEY_DATE;
	    } else {
	        orderBy = sort;
	    }

	    // 데이터베이스에 질의한다.
	    Cursor c = qb.query(bulletinDB, projection,
                            selection, selectionArgs,
                            null, null,
                            null);

	    // 커서의 결과 셋이 변경될 경우 통지 받을 수 있도록
	    // 컨텍스트의 ContentResolver를 등록한다.
	    c.setNotificationUri(getContext().getContentResolver(), uri);

	    // 질의 결과 커서를 리턴한다.
	    return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] wArgs) {
		int count;
	    switch (uriMatcher.match(uri)) {
	        case B_ALL: count = bulletinDB.update(BULLETIN_TABLE, values, where, wArgs);
	            break;

	        case B_ID: String segment = uri.getPathSegments().get(1);
	            count = bulletinDB.update(BULLETIN_TABLE, values, KEY_ID
	                                        + "=" + segment
	                                        + (!TextUtils.isEmpty(where) ? " AND ("
	                                        + where + ')' : ""), wArgs);
	            break;

	        default: throw new IllegalArgumentException("Unknown URI " + uri);
	    }

	    getContext().getContentResolver().notifyChange(uri, null);
	    return count;
	}

}
