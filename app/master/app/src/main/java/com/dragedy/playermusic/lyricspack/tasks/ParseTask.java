/*
 * *
 *  * This file is part of QuickLyric
 *  * Copyright © 2017 QuickLyric SPRL
 *  *
 *  * QuickLyric is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * QuickLyric is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License
 *  * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.dragedy.playermusic.lyricspack.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Process;
import android.widget.Toast;

import com.dragedy.playermusic.R;
import com.dragedy.playermusic.lyricspack.fragment.LyricsViewFragment;
import com.dragedy.playermusic.lyricspack.model.Lyrics;
import com.dragedy.playermusic.lyricspack.services.NotificationListenerService;

public class ParseTask extends AsyncTask<Object, Object, String[]> {

    private final boolean showMsg;
    private final boolean noDoubleBroadcast;
    private Lyrics currentLyrics;
    private LyricsViewFragment lyricsViewFragment;
    private Context mContext;

    public ParseTask(LyricsViewFragment fragment, boolean showMsg, boolean noDoubleBroadcast) {
        this.lyricsViewFragment = fragment;
        this.showMsg = showMsg;
        this.noDoubleBroadcast = noDoubleBroadcast;
    }

    @Override
    protected String[] doInBackground(Object... arg0) {
        mContext = lyricsViewFragment.getActivity();
        if (mContext == null)
            cancel(true);
        currentLyrics = (Lyrics) arg0[0];
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        SharedPreferences preferences = mContext.getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String[] music = new String[2];
        music[0] = preferences.getString("artist", null);
        music[1] = preferences.getString("track", null);
        return music;
    }

    @Override
    protected void onPostExecute(String[] metaData) {
        if (lyricsViewFragment.getActivity() != null) {
            if (currentLyrics != null && currentLyrics.getOriginalArtist().equalsIgnoreCase(metaData[0])
                    && currentLyrics.getOriginalTrack().equalsIgnoreCase(metaData[1])
                    && (!"Storage".equals(currentLyrics.getSource()) || ("Storage".equals(currentLyrics.getSource()) && noDoubleBroadcast))
                    && currentLyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                        NotificationListenerService.restartNotificationListenerServiceIfNeeded(lyricsViewFragment.getActivity()))
                    new ParseTask(lyricsViewFragment, showMsg, noDoubleBroadcast).execute(currentLyrics);
                else if (showMsg)
                    Toast.makeText(mContext, mContext.getString(R.string.no_refresh), Toast.LENGTH_LONG).show();
                lyricsViewFragment.stopRefreshAnimation();
                lyricsViewFragment.getActivity().findViewById(R.id.edit_tags_btn).setEnabled(true);
                if (currentLyrics.isLRC())
                    lyricsViewFragment.updateLRC();
            } else {
                lyricsViewFragment.fetchLyrics(metaData[0], metaData[1]);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                        NotificationListenerService.restartNotificationListenerServiceIfNeeded(lyricsViewFragment.getActivity()))
                    new ParseTask(lyricsViewFragment, showMsg, noDoubleBroadcast).execute(currentLyrics);
            }
        }
    }
}
