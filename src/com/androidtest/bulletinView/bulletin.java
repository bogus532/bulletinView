package com.androidtest.bulletinView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class bulletin {
	
	private String title;
	private String link;
	private String author;
	private Date date;
	private int readcheck;
	
	public bulletin(String _title,String _link,Date _date,String _author,int _readcheck)
	{
		title = _title;
		link = _link;
		author = _author;
		date = _date;
		readcheck = _readcheck;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public String getLink()
	{
		return link;
	}
	
	public String getAuthor()
	{
		return author;
	}
	
	public Date getDate()
	{
		return date;
	}
	
	public int getReadCheck()
	{
		return readcheck;
	}
	
	public String getDateToString()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = sdf.format(date);
        return dateString;
	}
	
	@Override
    public String toString() {
        //SimpleDateFormat sdf = new SimpleDateFormat("HH.mm");
        //String dateString = sdf.format(date);
        //return title+" : "+link;
		return title;
    }
	
	/*
	public String toDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH.mm");
        String dateString = sdf.format(date);
        return dateString;
    }
    */

}
