package com.dragedy.playermusic.lyricspack.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dragedy.playermusic.lyricspack.MainActivity;
import com.dragedy.playermusic.R;
import com.dragedy.playermusic.lyricspack.fragment.LyricsViewFragment;
import com.dragedy.playermusic.lyricspack.model.Lyrics;
import com.dragedy.playermusic.lyricspack.utils.Net;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import static com.dragedy.playermusic.lyricspack.model.Lyrics.ERROR;
import static com.dragedy.playermusic.lyricspack.utils.Net.getUrlAsString;

/**
 * This file is part of QuickLyric
 * Copyright © 2017 QuickLyric SPRL
 * <p/>
 * QuickLyric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * QuickLyric is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 */

public class IdDecoder extends AsyncTask<String, Integer, Lyrics> {
    private Context mContext;
    private LyricsViewFragment lyricsViewFragment;

    public IdDecoder(Context context, LyricsViewFragment lyricsFragment) {
        this.mContext = context;
        this.lyricsViewFragment = lyricsFragment;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (lyricsViewFragment != null)
            lyricsViewFragment.startRefreshAnimation();
    }

    @Override
    protected Lyrics doInBackground(String... strings) {
        String url = strings[0];
        String artist;
        String track;
        if (url == null)
            return new Lyrics(ERROR);
        if (url.contains("//www.soundhound.com/")) {
            try { // todo switch to Jsoup
                String html = getUrlAsString(url);
                int preceding = html.indexOf("root.App.trackDa") + 19;
                int following = html.substring(preceding).indexOf(";");
                String data = html.substring(preceding, preceding + following);
                JsonObject jsonData = new JsonParser().parse(data).getAsJsonObject();
                artist = jsonData.get("artist_display_name").getAsString();
                track = jsonData.get("track_name").getAsString();
            } catch (Exception e) {
                e.printStackTrace();
                return new Lyrics(ERROR);
            }

        } else if (url.contains("//shz.am/")) {
            String id = url.split(".am/t")[1];
            url = "https://www.shazam.com/discovery/v1/en/US/web/-/track/"+id;
            try {
                String jsonString = Net.getUrlAsString(url);
                JsonObject jsonData = new JsonParser().parse(jsonString).getAsJsonObject();
                jsonData = jsonData.getAsJsonObject("heading");
                artist = jsonData.get("subtitle").getAsString();
                track = jsonData.get("title").getAsString();
            } catch (Exception e) {
                e.printStackTrace();
                return new Lyrics(ERROR);
            }
        } else if (url.contains("//play.google.com/store/music/")) {
            String docID = url.substring(url.indexOf("&tid=") + 5);
            try {
                Document doc = Jsoup.connect(url).get();
                Element playCell =
                        doc.getElementsByAttributeValue("data-track-docid", docID)
                                .get(0);
                artist = doc.getElementsByClass("primary").text();
                track = playCell.parent().parent().child(1).getElementsByClass("title").text();
            } catch (Exception e) {
                e.printStackTrace();
                return new Lyrics(ERROR);
            }
        } else
            return new Lyrics(ERROR);
        Lyrics res = new Lyrics(Lyrics.SEARCH_ITEM);
        res.setArtist(artist);
        res.setTitle(track);
        return res;
    }

    @Override
    protected void onPostExecute(Lyrics lyrics) {
        super.onPostExecute(lyrics);
        if (lyricsViewFragment != null) {
            if (lyrics.getFlag() == ERROR || (lyrics.getArtist() == null && lyrics.getTitle() == null))
                lyricsViewFragment.stopRefreshAnimation();
            else
                lyricsViewFragment.fetchLyrics(lyrics.getArtist(), lyrics.getTitle());
        } else
            ((MainActivity) mContext).updateLyricsFragment(0, lyrics.getArtist(), lyrics.getTitle());
        if (lyrics.getFlag() == ERROR)
            Toast.makeText(mContext, R.string.wrong_musicID, Toast.LENGTH_LONG).show();
    }
}