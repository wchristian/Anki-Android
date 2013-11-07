/***************************************************************************************
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.widget;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.BackupManager;
import com.ichi2.anki.DeckPicker;
import com.ichi2.anki.MetaDB;
import com.ichi2.anki.services.NotificationService;
import com.ichi2.async.BaseAsyncTask;
import com.ichi2.async.DeckTask.TaskData;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Decks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * The status of the widget.
 * <p>
 * It contains the status of each of the decks.
 */
public final class WidgetStatus {

    private static boolean mediumWidget = false;
    private static boolean smallWidget = false;
    private static boolean bigWidget = false;
    private static boolean notification = false;

    private static DeckStatus sDeckStatus;
    private static float[] sSmallWidgetStatus;
    private static TreeSet<Object[]> sDeckCounts;

    private static AsyncTask<Context, Void, Context> sUpdateDeckStatusAsyncTask;


    /** This class should not be instantiated. */
    private WidgetStatus() {
    }


    /** Request the widget to update its status. */
    public static void update(Context context) {
        update(context, true, null, null, null);
    }


    /** Request the widget to update its status. */
    public static void update(Context context, DeckStatus deckStatus) {
        update(context, true, deckStatus, null, null);
    }


    public static void update(Context context, TreeSet<Object[]> deckCounts) {
        update(context, true, null, null, deckCounts);
    }


    public static void update(Context context, float[] smallWidgetStatus) {
        update(context, true, null, smallWidgetStatus, null);
    }


    public static void update(Context context, boolean updateBigWidget, DeckStatus deckStatus, float[] smallWidgetStatus, TreeSet<Object[]> deckCounts) {
        sDeckStatus = deckStatus;
    	sSmallWidgetStatus = smallWidgetStatus;
    	sDeckCounts = deckCounts;

        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(context);
        if (preferences.getBoolean("widgetMediumEnabled", false)) {
            mediumWidget = true;
        } else {
            mediumWidget = false;
        }
        if (preferences.getBoolean("widgetSmallEnabled", false)) {
            smallWidget = true;
        } else {
            smallWidget = false;
        }
        if (updateBigWidget && preferences.getBoolean("widgetBigEnabled", false)) {
            bigWidget = true;
        } else {
            bigWidget = false;
        }
        if (Integer.parseInt(preferences.getString("minimumCardsDueForNotification", "1000001")) < 1000000
                && sDeckStatus == null) {
            notification = true;
        } else {
            notification = false;
        }
        if ((mediumWidget || smallWidget || bigWidget || notification)
                && ((sUpdateDeckStatusAsyncTask == null) || (sUpdateDeckStatusAsyncTask.getStatus() == AsyncTask.Status.FINISHED))) {
            Log.d(AnkiDroidApp.TAG, "WidgetStatus.update(): updating");
            sUpdateDeckStatusAsyncTask = new UpdateDeckStatusAsyncTask();
            sUpdateDeckStatusAsyncTask.execute(context);
        } else {
            Log.d(AnkiDroidApp.TAG, "WidgetStatus.update(): already running or not enabled");
        }
    }


    public static void waitToFinish() {
        try {
            if ((sUpdateDeckStatusAsyncTask != null)
                    && (sUpdateDeckStatusAsyncTask.getStatus() != AsyncTask.Status.FINISHED)) {
                Log.i(AnkiDroidApp.TAG, "WidgetStatus: wait to finish");
                sUpdateDeckStatusAsyncTask.get();
            }
        } catch (Exception e) {
            return;
        }
    }


    /** Returns the status of each of the decks. */
    public static DeckStatus[] fetch(Context context) {
        return MetaDB.getWidgetStatus(context);
    }


    /** Returns the status of each of the decks. */
    public static int[] fetchSmall(Context context) {
        return MetaDB.getWidgetSmallStatus(context);
    }


    public static int fetchDue(Context context) {
        return MetaDB.getNotificationStatus(context);
    }


    public static DeckStatus getDeckStatus(Decks deck) {
        if (deck == null) {
            return null;
        }
        int dueCards = 0;
        int newCards = 0;
        return null;
    }

    private static class UpdateDeckStatusAsyncTask extends BaseAsyncTask<Context, Void, Context> {
        private static final DeckStatus[] EMPTY_DECK_STATUS = new DeckStatus[0];

        private static DeckStatus[] mDecks = EMPTY_DECK_STATUS;
        private static float[] mSmallWidgetStatus = new float[]{0, 0, 0, 0};


        @Override
        protected Context doInBackground(Context... params) {
            super.doInBackground(params);
            Log.d(AnkiDroidApp.TAG, "WidgetStatus.UpdateDeckStatusAsyncTask.doInBackground()");
            Context context = params[0];

            if (!AnkiDroidApp.isSdCardMounted()) {
                return context;
            }

            try {
            	if (sSmallWidgetStatus == null) {
                    Collection col = AnkiDroidApp.openCollection(AnkiDroidApp.getCollectionPath());
                    mSmallWidgetStatus = col.getSched().progressToday(sDeckCounts, null, true);
                    AnkiDroidApp.closeCollection(false);
            	} else {
            		mSmallWidgetStatus = sSmallWidgetStatus;
            	}
            } catch (SQLException e) {
                Log.i(AnkiDroidApp.TAG, "Widget: Problems on retrieving deck information");
            }

            return context;
        }


        @Override
        protected void onPostExecute(Context context) {
            super.onPostExecute(context);
            Log.d(AnkiDroidApp.TAG, "WidgetStatus.UpdateDeckStatusAsyncTask.onPostExecute()");
            MetaDB.storeSmallWidgetStatus(context, mSmallWidgetStatus);
			
            if (smallWidget) {
                Intent intent;
                intent = new Intent(context, AnkiDroidWidgetSmall.UpdateService.class);
                context.startService(intent);
            }
			
            if (notification) {
                Intent intent;
                intent = new Intent(context, NotificationService.class);
                context.startService(intent);
            }
        }
    }
}
