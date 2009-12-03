/*******************************************************************************
 * Copyright (c) 2009 .
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Prasanna Gautam - initial API and implementation
 *     Ralph Morelli - Supervisor
 *     Trishan deLanerolle - Director
 *     Antonio Alcorn - Summer 2009 Intern
 *     Gong Chen - Summer 2009 Intern
 *     Chris Fei - Summer 2009 Intern
 *     Phil Fritzsche - Summer 2009 Intern
 *     James Jackson - Summer 2009 Intern
 *     Qianqian Lin - Summer 2009 Intern 
 *     Khanh Pham - Summer 2009 Intern
 ******************************************************************************/
package org.hfoss.posit;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Represents a specific find for a project, with a unique identifier
 * 
 * @author Prasanna Gautam
 * @author Ralph Morelli
 * @author Qianqian Lin
 * @author Phil Fritzsche
 */
public class Find {

	private MyDBHelper mDbHelper;  // Handles all the DB actions
	private long mId;  	         // The Find's rowID (should be changed to DB ID)
	public Cursor images = null;
	private Context mContext;

	/**
	 * This constructor is used for a new Find
	 * @param context is the Activity
	 */
	public Find (Context context) {
		mDbHelper = new MyDBHelper(context);
		mContext = context;
	}
	
	/**
	 * This constructor is used for an existing Find.
	 * @param context is the Activity
	 * @param id is the Find's rowID (should be changed to its DB ID)
	 */
	public Find (Context context, long id) {
		this(context);
		mId = id;
		images = getImages();
		mContext = context;
	}

	/**
	 * 	getContent() returns the Find's <attr:value> pairs in a ContentValues array. 
	 *  This method assumes the Find object has been instantiated with a context
	 *  and an id.
	 *  @return A ContentValues with <key, value> pairs
	 */
	public ContentValues getContent() {
		ContentValues values = mDbHelper.fetchFindData(mId);
		getMediaUrisFromDb(values);
		return values;
	}
	
	private void getMediaUrisFromDb(ContentValues values) {
		mDbHelper.getImages(mId, values);
	}
	
	public Uri getImageUriByPosition(long findId, int position) {
		return mDbHelper.getPhotoUriByPosition(findId, position);
	}
	
	
	/**
	 * Returns a hashmap of string key and value
	 * this is used primarily for sending and receiving over Http, which is why it 
	 * somewhat works as everything is sent as string eventually. 
	 * @return
	 */
	public HashMap<String,String> getContentMap(){
		return mDbHelper.fetchFindMap(mId);
	}
	
	/** 
	 * insertToDB() assumes that the context has be passed through a constructor.
	 * @param content contains the Find's attributes and values.  
	 * @param images list of contentvalues containing the image references to add
	 * @return whether the DB operation succeeds
	 */
	public boolean insertToDB(ContentValues content, List<ContentValues> images) {
		if (content != null) {
			if (!content.containsKey(MyDBHelper.COLUMN_REVISION))
				content.put(MyDBHelper.COLUMN_REVISION, 1);
			if(!content.containsKey(MyDBHelper.COLUMN_SID))
				content.put(MyDBHelper.COLUMN_SID, 0);
			mId = mDbHelper.addNewFind(content); // returns rowId
		}
		if (images != null && images.size() > 0) {
			ListIterator<ContentValues> it = images.listIterator();
			while (it.hasNext()) {
				ContentValues imageValues = it.next();
				mDbHelper.addNewPhoto(imageValues, mId);
			}
		}
		return mId != -1;
	}
	
	/** 
	 * updateDB() assumes that the context and rowID has be passed through a constructor.
	 * @param content contains the Find's attributes and values.  
	 * @return whether the DB operation succeeds
	 */
	public boolean updateToDB(ContentValues content) {//, ContentValues images) {
		if (isSynced()) { //TODO think of a better way to do this
			//set it as unsynced
			content.put(MyDBHelper.COLUMN_SYNCED, false);
			//add revision only once. Don't count revisions for in-phone updates.
			content.put(MyDBHelper.COLUMN_REVISION, getRevision()+1);
		}
		return mDbHelper.updateFind(mId, content);
	}
	
	/**
	 * deletes the Find object form the DB
	 * @return whether the DB operation was successful
	 */
	public boolean delete() {
		if (mDbHelper.deleteFind(mId)) {
			return deleteImages(mId);
		}else 
			return false;
	}

	/**
	 * @return the mId
	 */
	public long getId() {
		return mId;
	}
	
	/**
	 * Get all images attached to this find
	 * @return the cursor that points to the images
	 */
	public Cursor getImages() {
		return mDbHelper.getImagesCursor(this.mId);
	}
	
	/**
	 * Delete all images from this find
	 * @return whether or not the images have been deleted
	 */
	private boolean deleteImages(long mId) {
		if ((images=getImages())!=null) {
			return mDbHelper.deleteImages(images,mId);
		}else {
			return true;
		}
	}
	
	/**
	 * @return whether or not there are images attached to this find
	 */
	public boolean hasImages(){
		return getImages().getCount() > 0;
	}
	
	/**
	 * Delete a particular image from this find
	 * @param id the id of the photo to be deleted
	 * @return whether the photo has been successfully deleted or not
	 */
	public boolean deleteImage(long id) {
		return mDbHelper.deletePhoto(id);
	}
	
	public boolean deleteImageByPosition(int position) {
		return mDbHelper.deletePhotoByPosition(mId, position);
	}
	
	
	public void setServerId(int serverId){
		ContentValues content = new ContentValues();
		content.put(MyDBHelper.COLUMN_SID, serverId);
		updateToDB(content);
	}
	
	public void setRevision(int revision){
		ContentValues content = new ContentValues();
		content.put(MyDBHelper.COLUMN_REVISION, revision);
		updateToDB(content);
	}
	
	public void setSyncStatus(boolean status){
		ContentValues content = new ContentValues();
		content.put(MyDBHelper.COLUMN_SYNCED, status);
		updateToDB(content);
	}
	
	public int getRevision() {
		ContentValues value = mDbHelper.fetchFindColumns(mId, new String[] { MyDBHelper.COLUMN_REVISION});
		return value.getAsInteger(MyDBHelper.COLUMN_REVISION);
	}
	
	public boolean isSynced() {
		ContentValues value = mDbHelper.fetchFindColumns(mId, new String[] { MyDBHelper.COLUMN_SYNCED});
		return value.getAsInteger(MyDBHelper.COLUMN_SYNCED)==1;
	}	
}