/**
 ** Copyright (c) 2010 Ushahidi Inc
 ** All rights reserved
 ** Contact: team@ushahidi.com
 ** Website: http://www.ushahidi.com
 **
 ** GNU Lesser General Public License Usage
 ** This file may be used under the terms of the GNU Lesser
 ** General Public License version 3 as published by the Free Software
 ** Foundation and appearing in the file LICENSE.LGPL included in the
 ** packaging of this file. Please review the following information to
 ** ensure the GNU Lesser General Public License version 3 requirements
 ** will be met: http://www.gnu.org/licenses/lgpl.html.
 **
 **
 ** If you have questions regarding the use of this file, please contact
 ** Ushahidi developers at team@ushahidi.com.
 **
 **/

package com.ushahidi.android.app.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ushahidi.android.app.entities.Checkin;

/**
 * @author eyedol
 */
public class CheckinDao extends DbContentProvider implements ICheckinDao, ICheckinSchema {

    /**
     * @param db
     */
    public CheckinDao(SQLiteDatabase db) {
        super(db);
    }

    private Cursor cursor;

    private List<Checkin> listCheckin;

    private ContentValues initialValues;

    /*
     * (non-Javadoc)
     * @see com.ushahidi.android.app.database.ICheckinDao#fetchAllCheckins()
     */
    @Override
    public List<Checkin> fetchAllCheckins() {
        listCheckin = new ArrayList<Checkin>();
        final String sortOrder = CHECKIN_DATE + " DESC";
        cursor = super.query(CHECKINS_TABLE, CHECKINS_COLUMNS, null, null, sortOrder);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Checkin checkin = cursorToEntity(cursor);
                listCheckin.add(checkin);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return listCheckin;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.ushahidi.android.app.database.DbContentProvider#cursorToEntity(android
     * .database.Cursor)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Checkin cursorToEntity(Cursor cursor) {
        Checkin checkin = new Checkin();
        int idIndex;
        int checkinIdIndex;
        int userIdIndex;
        int dateIndex;
        int messageIndex;
        int locationIndex;
        int latitudeIndex;
        int longitudeIndex;

        if (cursor != null) {
            if (cursor.getColumnIndex(ID) != -1) {
                idIndex = cursor.getColumnIndexOrThrow(ID);
                checkin.setDbId(cursor.getInt(idIndex));
            }
            
            if (cursor.getColumnIndex(CHECKIN_ID) != -1) {
            	checkinIdIndex = cursor.getColumnIndexOrThrow(CHECKIN_ID);
                checkin.setCheckinId(cursor.getInt(checkinIdIndex));
            }

            if (cursor.getColumnIndex(CHECKIN_USER_ID) != -1) {
                userIdIndex = cursor.getColumnIndexOrThrow(CHECKIN_USER_ID);
                checkin.setUserId(cursor.getInt(userIdIndex));
            }

            if (cursor.getColumnIndex(CHECKIN_DATE) != -1) {
                dateIndex = cursor.getColumnIndexOrThrow(CHECKIN_DATE);
                checkin.setDate(cursor.getString(dateIndex));
            }

            if (cursor.getColumnIndex(CHECKIN_MESG) != -1) {
                messageIndex = cursor.getColumnIndexOrThrow(CHECKIN_MESG);
                checkin.setMessage(cursor.getString(messageIndex));
            }

            if (cursor.getColumnIndex(CHECKIN_LOC_NAME) != -1) {
                locationIndex = cursor.getColumnIndexOrThrow(CHECKIN_LOC_NAME);
                checkin.setLocationName(cursor.getString(locationIndex));
            }

            if (cursor.getColumnIndex(CHECKIN_LOC_LATITUDE) != -1) {
                latitudeIndex = cursor.getColumnIndexOrThrow(CHECKIN_LOC_LATITUDE);
                checkin.setLocationLatitude(cursor.getString(latitudeIndex));
            }

            if (cursor.getColumnIndex(CHECKIN_LOC_LONGITUDE) != -1) {
                longitudeIndex = cursor.getColumnIndexOrThrow(CHECKIN_LOC_LONGITUDE);
                checkin.setLocationLongitude(cursor.getString(longitudeIndex));
            }
        }

        return checkin;

    }

    /*
     * (non-Javadoc)
     * @see
     * com.ushahidi.android.app.database.ICheckinDao#fetchCheckinsByUserdId(int)
     */
    @Override
    public List<Checkin> fetchCheckinsByUserdId(int id) {
        listCheckin = new ArrayList<Checkin>();

        final String sortOrder = CHECKIN_DATE + " DESC";

        final String selectionArgs[] = {
            String.valueOf(id)
        };

        final String selection = CHECKIN_ID + " = ?";

        cursor = super.query(CHECKINS_TABLE, CHECKINS_COLUMNS, selection, selectionArgs, sortOrder);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Checkin checkin = cursorToEntity(cursor);
                listCheckin.add(checkin);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return listCheckin;
    }

    /*
     * (non-Javadoc)
     * @see com.ushahidi.android.app.database.ICheckinDao#deleteAllCheckins()
     */
    @Override
    public boolean deleteAllCheckins() {
        return super.delete(CHECKINS_TABLE, null, null) > 0;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.ushahidi.android.app.database.ICheckinDao#addCheckin(com.ushahidi
     * .android.app.entities.Checkin)
     */
    @Override
    public boolean addCheckin(Checkin checkin) {
        // set values
        setContentValue(checkin);
        return super.insert(CHECKINS_TABLE, getContentValue()) > 0;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.ushahidi.android.app.database.ICheckinDao#addCheckins(java.util.List)
     */
    @Override
    public boolean addCheckins(List<Checkin> checkins) {
        try {
            mDb.beginTransaction();

            for (Checkin checkin : checkins) {

                addCheckin(checkin);
            }

            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return true;
    }

    /**
     * Create table for checkins
     * 
     * @return
     */
    private void setContentValue(Checkin checkins) {

        initialValues = new ContentValues();
        initialValues.put(ID, checkins.getDbId());
        initialValues.put(CHECKIN_ID, checkins.getCheckinId());
        initialValues.put(CHECKIN_USER_ID, checkins.getUserId());
        initialValues.put(CHECKIN_MESG, checkins.getMessage());
        initialValues.put(CHECKIN_DATE, checkins.getDate());
        initialValues.put(CHECKIN_LOC_NAME, checkins.getLocationName());
        initialValues.put(CHECKIN_LOC_LATITUDE, checkins.getLocationLatitude());
        initialValues.put(CHECKIN_LOC_LONGITUDE, checkins.getLocationLongitude());
    }

    private ContentValues getContentValue() {
        return initialValues;
    }
}
