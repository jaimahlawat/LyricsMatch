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

package com.dragedy.playermusic.lyricspack.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.dragedy.playermusic.lyricspack.MainActivity;
import com.dragedy.playermusic.R;
import com.dragedy.playermusic.lyricspack.adapter.DrawerAdapter;
import com.dragedy.playermusic.lyricspack.broadcastReceiver.MusicBroadcastReceiver;
import com.dragedy.playermusic.lyricspack.model.Lyrics;
import com.dragedy.playermusic.lyricspack.services.NotificationListenerService;
import com.dragedy.playermusic.lyricspack.tasks.CoverArtLoader;
import com.dragedy.playermusic.lyricspack.tasks.DownloadThread;
import com.dragedy.playermusic.lyricspack.tasks.Id3Reader;
import com.dragedy.playermusic.lyricspack.tasks.Id3Writer;
import com.dragedy.playermusic.lyricspack.tasks.ParseTask;
import com.dragedy.playermusic.lyricspack.tasks.PresenceChecker;
import com.dragedy.playermusic.lyricspack.tasks.WriteToDatabaseTask;
import com.dragedy.playermusic.lyricspack.utils.ColorUtils;
import com.dragedy.playermusic.lyricspack.utils.CoverCache;
import com.dragedy.playermusic.lyricspack.utils.CustomSelectionCallback;
import com.dragedy.playermusic.lyricspack.utils.DatabaseHelper;
import com.dragedy.playermusic.lyricspack.utils.LyricsTextFactory;
import com.dragedy.playermusic.lyricspack.utils.NightTimeVerifier;
import com.dragedy.playermusic.lyricspack.utils.OnlineAccessVerifier;
import com.dragedy.playermusic.lyricspack.utils.PermissionsChecker;
import com.dragedy.playermusic.lyricspack.view.ControllableAppBarLayout;
import com.dragedy.playermusic.lyricspack.view.FadeInNetworkImageView;
import com.dragedy.playermusic.lyricspack.view.LrcView;
import com.dragedy.playermusic.lyricspack.view.MaterialSuggestionsSearchView;
import com.dragedy.playermusic.lyricspack.view.RefreshIcon;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.dragedy.playermusic.R.menu.lyrics;

public class LyricsViewFragment extends Fragment implements Lyrics.Callback, SwipeRefreshLayout.OnRefreshListener {

    private static BroadcastReceiver broadcastReceiver;
    public boolean lyricsPresentInDB;
    public boolean isActiveFragment = false;
    public boolean showTransitionAnim = true;
    private Lyrics mLyrics;
    private String mSearchQuery;
    private boolean mSearchFocused;
    private NestedScrollView mScrollView;
    private boolean startEmpty = false;
    public boolean manualUpdateLock;
    private SwipeRefreshLayout mRefreshLayout;
    private Thread mLrcThread;
    private boolean mExpandedSearchView;
    public boolean updateChecked = false;
    private boolean threadCancelled;

    public LyricsViewFragment() {
    }

    public static void sendIntent(Context context, Intent intent) {
        if (broadcastReceiver != null)
            broadcastReceiver.onReceive(context, intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLyrics != null)
            try {
                outState.putByteArray("lyrics", mLyrics.toBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        View searchView = getActivity().findViewById(R.id.search_view);
        if (searchView instanceof SearchView) {
            outState.putString("searchQuery", ((SearchView) searchView).getQuery().toString());
            outState.putBoolean("searchFocused", searchView.hasFocus());
        }

        outState.putBoolean("refreshFabEnabled", getActivity().findViewById(R.id.refresh_fab).isEnabled());

        EditText editedLyrics = (EditText) getActivity().findViewById(R.id.edit_lyrics);
        if (editedLyrics.getVisibility() == View.VISIBLE) {
            EditText editedTitle = (EditText) getActivity().findViewById(R.id.song);
            EditText editedArtist = (EditText) getActivity().findViewById(R.id.artist);
            outState.putCharSequence("editedLyrics", editedLyrics.getText());
            outState.putCharSequence("editedTitle", editedTitle.getText());
            outState.putCharSequence("editedArtist", editedArtist.getText());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        View layout = inflater.inflate(R.layout.lyrics_view, container, false);
        if (savedInstanceState != null)
            try {
                Lyrics l = Lyrics.fromBytes(savedInstanceState.getByteArray("lyrics"));
                if (l != null)
                    this.mLyrics = l;
                mSearchQuery = savedInstanceState.getString("searchQuery");
                mSearchFocused = savedInstanceState.getBoolean("searchFocused");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        else {
            Bundle args = getArguments();
            if (args != null)
                try {
                    Lyrics lyrics = Lyrics.fromBytes(args.getByteArray("lyrics"));
                    this.mLyrics = lyrics;
                    if (lyrics != null && lyrics.getText() == null && lyrics.getArtist() != null) {
                        String artist = lyrics.getArtist();
                        String track = lyrics.getTitle();
                        String url = lyrics.getURL();
                        fetchLyrics(artist, track, url);
                        mRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.refresh_layout);
                        startRefreshAnimation();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
        }
        if (layout != null) {
            Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();

            boolean screenOn = PreferenceManager
                    .getDefaultSharedPreferences(getActivity()).getBoolean("pref_force_screen_on", false);

            TextSwitcher textSwitcher = (TextSwitcher) layout.findViewById(R.id.switcher);
            textSwitcher.setFactory(new LyricsTextFactory(layout.getContext()));
            ActionMode.Callback callback = new CustomSelectionCallback(getActivity());
            ((TextView) textSwitcher.getChildAt(0)).setCustomSelectionActionModeCallback(callback);
            ((TextView) textSwitcher.getChildAt(1)).setCustomSelectionActionModeCallback(callback);
            textSwitcher.setKeepScreenOn(screenOn);
            layout.findViewById(R.id.lrc_view).setKeepScreenOn(screenOn);

            EditText artistTV = (EditText) getActivity().findViewById(R.id.artist);
            EditText songTV = (EditText) getActivity().findViewById(R.id.song);

            if (args != null && args.containsKey("editedLyrics")) {
                EditText editedLyrics = (EditText) layout.findViewById(R.id.edit_lyrics);
                textSwitcher.setVisibility(View.GONE);
                editedLyrics.setVisibility(View.VISIBLE);
                songTV.setInputType(InputType.TYPE_CLASS_TEXT);
                artistTV.setInputType(InputType.TYPE_CLASS_TEXT);
                songTV.setBackgroundResource(R.drawable.abc_textfield_search_material);
                artistTV.setBackgroundResource(R.drawable.abc_textfield_search_material);
                editedLyrics.setText(args.getCharSequence("editedLyrics"), TextView.BufferType.EDITABLE);
                songTV.setText(args.getCharSequence("editedTitle"), TextView.BufferType.EDITABLE);
                artistTV.setText(args.getCharSequence("editedArtist"), TextView.BufferType.EDITABLE);
            }

            artistTV.setTypeface(LyricsTextFactory.FontCache.get("regular", getActivity()));
            songTV.setTypeface(LyricsTextFactory.FontCache.get("medium", getActivity()));

            final RefreshIcon refreshFab = (RefreshIcon) getActivity().findViewById(R.id.refresh_fab);
            refreshFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mRefreshLayout.isRefreshing())
                        fetchCurrentLyrics(true);
                }
            });
            if (args != null)
                refreshFab.setEnabled(args.getBoolean("refreshFabEnabled", true));

            mScrollView = (NestedScrollView) layout.findViewById(R.id.scrollview);
            mRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.refresh_layout);

            mRefreshLayout.setColorSchemeResources(ColorUtils.getPrimaryColorResource(getActivity()), ColorUtils.getAccentColorResource(getActivity()));
            float offset = getResources().getDisplayMetrics().density * 64;
            mRefreshLayout.setProgressViewEndTarget(true, (int) offset);
            mRefreshLayout.setOnRefreshListener(this);

            final ImageButton editTagsButton = (ImageButton) getActivity().findViewById(R.id.edit_tags_btn);

            View.OnClickListener startEditClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startEditTagsMode();
                    final View.OnClickListener startEditClickListener = this;
                    editTagsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            exitEditTagsMode();
                            editTagsButton.setOnClickListener(startEditClickListener);
                        }
                    });
                }
            };
            editTagsButton.setOnClickListener(startEditClickListener);

            if (mLyrics == null) {
                if (!startEmpty)
                    fetchCurrentLyrics(false);
            } else if (mLyrics.getFlag() == Lyrics.SEARCH_ITEM) {
                mRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.refresh_layout);
                startRefreshAnimation();
                if (mLyrics.getArtist() != null)
                    fetchLyrics(mLyrics.getArtist(), mLyrics.getTitle());
                ((TextView) (getActivity().findViewById(R.id.artist))).setText(mLyrics.getArtist());
                ((TextView) (getActivity().findViewById(R.id.song))).setText(mLyrics.getTitle());
            } else //Rotation, resume
                update(mLyrics, layout, false);
        }
        if (broadcastReceiver == null)
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    manualUpdateLock = false;
                    String artist = intent.getStringExtra("artist");
                    String track = intent.getStringExtra("track");
                    if (artist != null && track != null && mRefreshLayout.isEnabled()) {
                        startRefreshAnimation();
                        new ParseTask(LyricsViewFragment.this, false, true).execute(mLyrics);
                    }
                }
            };
        return layout;
    }

    private void startEditTagsMode() {
        ImageButton editButton = (ImageButton) getActivity().findViewById(R.id.edit_tags_btn);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            editButton.setImageResource(R.drawable.ic_edit_anim);
            ((Animatable) editButton.getDrawable()).start();
        } else
            editButton.setImageResource(R.drawable.ic_done);

        ((DrawerLayout) ((MainActivity) getActivity()).drawer).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mRefreshLayout.setEnabled(false);
        getActivity().findViewById(R.id.refresh_fab).setEnabled(false);
        ((RefreshIcon) getActivity().findViewById(R.id.refresh_fab)).hide();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).getMenu().clear();

        TextSwitcher textSwitcher = ((TextSwitcher) getActivity().findViewById(R.id.switcher));
        EditText songTV = (EditText) getActivity().findViewById(R.id.song);
        TextView artistTV = ((TextView) getActivity().findViewById(R.id.artist));

        EditText newLyrics = (EditText) getActivity().findViewById(R.id.edit_lyrics);
        newLyrics.setTypeface(LyricsTextFactory.FontCache.get("light", getActivity()));
        newLyrics.setText(((TextView) textSwitcher.getCurrentView()).getText(), TextView.BufferType.EDITABLE);

        textSwitcher.setVisibility(View.GONE);
        newLyrics.setVisibility(View.VISIBLE);

        songTV.setInputType(InputType.TYPE_CLASS_TEXT);
        artistTV.setInputType(InputType.TYPE_CLASS_TEXT);
        songTV.setBackgroundResource(R.drawable.abc_textfield_search_material);
        artistTV.setBackgroundResource(R.drawable.abc_textfield_search_material);


        if (songTV.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void exitEditTagsMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((ImageButton) getActivity().findViewById(R.id.edit_tags_btn)).setImageResource(R.drawable.ic_done_anim);
            Drawable editIcon = ((ImageButton) getActivity().findViewById(R.id.edit_tags_btn)).getDrawable();
            ((Animatable) editIcon).start();
        } else
            ((ImageButton) getActivity().findViewById(R.id.edit_tags_btn)).setImageResource(R.drawable.ic_edit);

        if (getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isAcceptingText())
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }

        EditText songTV = (EditText) getActivity().findViewById(R.id.song);
        EditText artistTV = ((EditText) getActivity().findViewById(R.id.artist));
        EditText newLyrics = (EditText) getActivity().findViewById(R.id.edit_lyrics);

        songTV.setInputType(InputType.TYPE_NULL);
        artistTV.setInputType(InputType.TYPE_NULL);
        songTV.setBackgroundColor(Color.TRANSPARENT);
        artistTV.setBackgroundColor(Color.TRANSPARENT);

        String txt = mLrcThread == null ? null : mLyrics.getText();
        if (txt == null)
            txt = "";

        File musicFile = Id3Reader.getFile(getActivity(), mLyrics.getOriginalArtist(), mLyrics.getOriginalTrack());

        if (!mLyrics.getArtist().equals(artistTV.getText().toString())
                || !mLyrics.getTitle().equals(songTV.getText().toString())
                || !Html.fromHtml(txt).toString().equals(newLyrics.getText().toString())) {
            mLyrics.setArtist(artistTV.getText().toString());
            mLyrics.setTitle(songTV.getText().toString());
            mLyrics.setText(newLyrics.getText().toString().replaceAll("\n", "<br/>"));
            if (PermissionsChecker.requestPermission(getActivity(),
                    "android.permission.WRITE_EXTERNAL_STORAGE", 0, Id3Writer.REQUEST_CODE))
                new Id3Writer(this).execute(mLyrics, musicFile);
        } else
            new Id3Writer(this).onPreExecute();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.isHidden())
            return;

        DrawerAdapter drawerAdapter = ((DrawerAdapter) ((ListView) this.getActivity().findViewById(R.id.drawer_list)).getAdapter());
        if (drawerAdapter.getSelectedItem() != 0) {
            drawerAdapter.setSelectedItem(0);
            drawerAdapter.notifyDataSetChanged();
        }
        this.isActiveFragment = true;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            this.onViewCreated(getView(), null);
            if (mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT && lyricsPresentInDB)
                new PresenceChecker().execute(this, new String[]{mLyrics.getArtist(), mLyrics.getTitle(),
                        mLyrics.getOriginalArtist(), mLyrics.getOriginalTrack()});
        } else
            this.isActiveFragment = false;
    }

    public void startRefreshAnimation() {
        if (mRefreshLayout == null)
            if (getActivity() != null && getView() != null)
                mRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.refresh_layout);
        if (mRefreshLayout != null)
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    if (!mRefreshLayout.isRefreshing())
                        mRefreshLayout.setRefreshing(true);
                }
            });
    }

    public void stopRefreshAnimation() {
        if (mRefreshLayout == null)
            if (getActivity() != null && getView() != null)
                mRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.refresh_layout);
        if (mRefreshLayout != null)
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setRefreshing(false);
                }
            });
    }

    public void fetchLyrics(String... params) {
        if (getActivity() == null)
            return;

        String artist = params[0];
        String title = params[1];
        String url = null;
        if (params.length > 2)
            url = params[2];
        startRefreshAnimation();

        Lyrics lyrics = null;
        if (artist != null && title != null) {
            if (url == null &&
                    (getActivity().getSharedPreferences("intro_slides", Context.MODE_PRIVATE).getBoolean("seen", false))
                    && (mLyrics == null || mLyrics.getFlag() != Lyrics.POSITIVE_RESULT ||
                    !("Storage".equals(mLyrics.getSource())
                            && mLyrics.getArtist().equalsIgnoreCase(artist)
                            && mLyrics.getTitle().equalsIgnoreCase(title))
            ))
                lyrics = Id3Reader.getLyrics(getActivity(), artist, title);

            if (lyrics == null)
                lyrics = DatabaseHelper.getInstance(getActivity()).get(new String[]{artist, title});

            if (lyrics == null)
                lyrics = DatabaseHelper.getInstance(getActivity()).get(DownloadThread.correctTags(artist, title));
        } else if (url == null) {
            showFirstStart();
            return;
        }
        boolean prefLRC = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean("pref_lrc", true);
        if (OnlineAccessVerifier.check(getActivity()) && (lyrics == null || (!lyrics.isLRC() && prefLRC))) {
            Set<String> providersSet = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getStringSet("pref_providers", new TreeSet<String>());
            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_lrc", true))
                providersSet.add("ViewLyrics");
            DownloadThread.setProviders(providersSet);

            if (mLyrics == null) {
                TextView artistTV = (TextView) getActivity().findViewById(R.id.artist);
                TextView songTV = (TextView) getActivity().findViewById(R.id.song);
                artistTV.setText(artist);
                songTV.setText(title);
            }

            SharedPreferences preferences = getActivity()
                    .getSharedPreferences("current_music", Context.MODE_PRIVATE);
            boolean positionAvailable = preferences.getLong("position", 0) != -1;

            if (url == null)
                new DownloadThread(this, positionAvailable, artist, title).start();
            else
                new DownloadThread(this, positionAvailable, url, artist, title).start();
        } else if (lyrics != null)
            onLyricsDownloaded(lyrics);
        else {
            lyrics = new Lyrics(Lyrics.ERROR);
            lyrics.setArtist(artist);
            lyrics.setTitle(title);
            onLyricsDownloaded(lyrics);
        }
    }

    public void fetchCurrentLyrics(boolean showMsg) {
        manualUpdateLock = false;
        getActivity().findViewById(R.id.edit_tags_btn).setEnabled(false);
        if (mLyrics != null && mLyrics.getArtist() != null && mLyrics.getTitle() != null)
            new ParseTask(this, showMsg, false).execute(mLyrics);
        else
            new ParseTask(this, showMsg, false).execute((Object) null);
    }

    @TargetApi(16)
    private void beamLyrics(final Lyrics lyrics, Activity activity) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            if (lyrics.getText() != null) {
                nfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                    @Override
                    public NdefMessage createNdefMessage(NfcEvent event) {
                        try {
                            byte[] payload = lyrics.toBytes(); // whatever data you want to send
                            NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "application/lyrics".getBytes(), new byte[0], payload);
                            return new NdefMessage(new NdefRecord[]{
                                    record, // your data
                                    NdefRecord.createApplicationRecord("com.dragedy.playermusic.lyricspack"), // the "application record"
                            });
                        } catch (IOException e) {
                            return null;
                        }
                    }
                }, activity);
            }
        }
    }

    @Override
    public void onLyricsDownloaded(Lyrics lyrics) {
        if (getActivity() != null && !((MainActivity) getActivity()).hasBeenDestroyed() && getView() != null)
            update(lyrics, getView(), true);
        else
            mLyrics = lyrics;
    }

    @SuppressLint("SetTextI18n")
    public void update(Lyrics lyrics, View layout, boolean animation) {
        File musicFile = null;
        Bitmap cover = null;
        if (PermissionsChecker.hasPermission(getActivity(), "android.permission.READ_EXTERNAL_STORAGE")) {
            musicFile = Id3Reader.getFile(getActivity(), lyrics.getOriginalArtist(), lyrics.getOriginalTrack());
            cover = Id3Reader.getCover(getActivity(), lyrics.getArtist(), lyrics.getTitle());
        }
        setCoverArt(cover, null);
        boolean artCellDownload =
                Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .getString("pref_artworks", "0")) == 0;
        if (cover == null)
            new CoverArtLoader().execute(lyrics, this.getActivity(), artCellDownload || OnlineAccessVerifier.isConnectedWifi(getActivity()));
        getActivity().findViewById(R.id.edit_tags_btn).setEnabled(true);
        getActivity().findViewById(R.id.edit_tags_btn)
                .setVisibility(musicFile == null || !musicFile.canWrite() || lyrics.isLRC()
                        || Id3Reader.getLyrics(getActivity(), lyrics.getArtist(), lyrics.getTitle()) == null
                        ? View.GONE : View.VISIBLE);
        TextSwitcher textSwitcher = ((TextSwitcher) layout.findViewById(R.id.switcher));
        LrcView lrcView = (LrcView) layout.findViewById(R.id.lrc_view);
        View v = getActivity().findViewById(R.id.tracks_msg);
        if (v != null)
            ((ViewGroup) v.getParent()).removeView(v);
        TextView artistTV = (TextView) getActivity().findViewById(R.id.artist);
        TextView songTV = (TextView) getActivity().findViewById(R.id.song);
        final TextView id3TV = (TextView) layout.findViewById(R.id.source_tv);
        TextView writerTV = (TextView) layout.findViewById(R.id.writer_tv);
        TextView copyrightTV = (TextView) layout.findViewById(R.id.copyright_tv);
        RelativeLayout bugLayout = (RelativeLayout) layout.findViewById(R.id.error_msg);
        this.mLyrics = lyrics;
        if (SDK_INT >= ICE_CREAM_SANDWICH)
            beamLyrics(lyrics, this.getActivity());
        new PresenceChecker().execute(this, new String[]{lyrics.getArtist(), lyrics.getTitle(),
                lyrics.getOriginalArtist(), lyrics.getOriginalTrack()});

        if (lyrics.getArtist() != null)
            artistTV.setText(lyrics.getArtist());
        else
            artistTV.setText("");
        if (lyrics.getTitle() != null)
            songTV.setText(lyrics.getTitle());
        else
            songTV.setText("");
        if (lyrics.getCopyright() != null) {
            copyrightTV.setText("Copyright: " + lyrics.getCopyright());
            copyrightTV.setVisibility(View.VISIBLE);
        } else {
            copyrightTV.setText("");
            copyrightTV.setVisibility(View.GONE);
        }
        if (lyrics.getWriter() != null) {
            if (lyrics.getWriter().contains(","))
                writerTV.setText("Writers:\n" + lyrics.getWriter());
            else
                writerTV.setText("Writer:" + lyrics.getWriter());
            writerTV.setVisibility(View.VISIBLE);
        } else {
            writerTV.setText("");
            writerTV.setVisibility(View.GONE);
        }
        if (isActiveFragment)
            ((RefreshIcon) getActivity().findViewById(R.id.refresh_fab)).show();
        EditText newLyrics = (EditText) getActivity().findViewById(R.id.edit_lyrics);
        if (newLyrics != null)
            newLyrics.setText("");

        if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
            if (!lyrics.isLRC()) {
                textSwitcher.setVisibility(View.VISIBLE);
                lrcView.setVisibility(View.GONE);
                if (animation)
                    textSwitcher.setText(Html.fromHtml(lyrics.getText()));
                else
                    textSwitcher.setCurrentText(Html.fromHtml(lyrics.getText()));
            } else {
                textSwitcher.setVisibility(View.GONE);
                lrcView.setVisibility(View.VISIBLE);
                lrcView.setOriginalLyrics(lyrics);
                lrcView.setSourceLrc(lyrics.getText());
                if (isActiveFragment)
                    ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar)).expandToolbar(true);
                updateLRC();
            }

            bugLayout.setVisibility(View.INVISIBLE);
            id3TV.setMovementMethod(LinkMovementMethod.getInstance());
            if ("Storage".equals(lyrics.getSource())) {
                id3TV.setVisibility(View.VISIBLE);
                SpannableString text = new SpannableString(getString(R.string.from_id3));
                text.setSpan(new UnderlineSpan(), 1, text.length() - 1, 0);
                id3TV.setText(text);
                id3TV.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity) getActivity()).id3PopUp(id3TV);
                    }
                });
            } else {
                id3TV.setOnClickListener(null);
                id3TV.setVisibility(View.GONE);
            }
            mScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.scrollTo(0, 0); //only useful when coming from localLyricsFragment
                    mScrollView.smoothScrollTo(0, 0);
                }
            });
        } else {
            textSwitcher.setText("");
            textSwitcher.setVisibility(View.INVISIBLE);
            lrcView.setVisibility(View.INVISIBLE);
            bugLayout.setVisibility(View.VISIBLE);
            int message;
            int whyVisibility;
            if (lyrics.getFlag() == Lyrics.ERROR || !OnlineAccessVerifier.check(getActivity())) {
                message = R.string.connection_error;
                whyVisibility = TextView.GONE;
            } else {
                message = R.string.no_results;
                whyVisibility = TextView.VISIBLE;
                updateSearchView(false, lyrics.getTitle(), false);
            }
            TextView whyTextView = ((TextView) bugLayout.findViewById(R.id.bugtext_why));
            ((TextView) bugLayout.findViewById(R.id.bugtext)).setText(message);
            whyTextView.setVisibility(whyVisibility);
            whyTextView.setPaintFlags(whyTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            id3TV.setVisibility(View.GONE);
        }
        stopRefreshAnimation();
        getActivity().getIntent().setAction("");
        getActivity().invalidateOptionsMenu();
    }

    private void showFirstStart() {
        stopRefreshAnimation();
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup parent = (ViewGroup) ((ViewGroup) getActivity().findViewById(R.id.scrollview)).getChildAt(0);
        if (parent.findViewById(R.id.tracks_msg) == null)
            inflater.inflate(R.layout.no_tracks, parent);

        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.firstLaunchCoverDrawable, typedValue, true);
        int firstLaunchBGid = typedValue.resourceId;
        @SuppressWarnings("deprecation")
        BitmapDrawable bd = ((BitmapDrawable) getResources().getDrawable(firstLaunchBGid));

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        setCoverArt(bd != null ? bd.getBitmap() : null, null);
        ((TextSwitcher) getActivity().findViewById(R.id.switcher)).setText("");

        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "7"));
        if (themeNum > 0 && themeNum != 7) {
            TypedValue darkColorValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.colorPrimaryDark, darkColorValue, true);
            ((FadeInNetworkImageView) getActivity().findViewById(R.id.cover))
                    .setColorFilter(darkColorValue.data, PorterDuff.Mode.OVERLAY);
        }

        getActivity().findViewById(R.id.error_msg).setVisibility(View.INVISIBLE);
        ((TextView) getActivity().findViewById(R.id.artist)).setText("");
        ((TextView) getActivity().findViewById(R.id.song)).setText("");
        getActivity().findViewById(R.id.top_gradient).setVisibility(View.INVISIBLE);
        getActivity().findViewById(R.id.bottom_gradient).setVisibility(View.INVISIBLE);
        getActivity().findViewById(R.id.edit_tags_btn).setVisibility(View.INVISIBLE);
    }

    public void checkPreferencesChanges() {
        boolean screenOn = PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getBoolean("pref_force_screen_on", false);
        boolean dyslexic = PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getBoolean("pref_opendyslexic", false);

        TextSwitcher switcher = (TextSwitcher) getActivity().findViewById(R.id.switcher);
        View lrcView = getActivity().findViewById(R.id.lrc_view);

        if (switcher != null) {
            switcher.setKeepScreenOn(screenOn);
            if (switcher.getCurrentView() != null)
                ((TextView) switcher.getCurrentView()).setTypeface(
                        LyricsTextFactory.FontCache.get(dyslexic ? "dyslexic" : "light", getActivity())
                );
            View nextView = switcher.getNextView();
            if (nextView != null) {
                ((TextView) nextView).setTypeface(
                        LyricsTextFactory.FontCache.get(dyslexic ? "dyslexic" : "light", getActivity())
                );
            }
        }
        if (lrcView != null)
            lrcView.setKeepScreenOn(screenOn);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        TypedValue outValue = new TypedValue();
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.getTheme().resolveAttribute(R.attr.themeName, outValue, false);
        if ("Night".equals(outValue.string) != NightTimeVerifier.check(getActivity()) ||
                mainActivity.themeNum != Integer.valueOf(sharedPrefs.getString("pref_theme", "7"))) {
            getActivity().finish();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setAction("android.intent.action.MAIN");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().overridePendingTransition(0, 0);
        }
    }

    public void showWhyPopup() {
        String title = mLyrics.getTitle();
        String artist = mLyrics.getArtist();
        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.why_popup_title))
                .setMessage(String.format(String.valueOf(Html.fromHtml(getString(R.string.why_popup_text))),
                        title, artist))
                .show();
    }

    public void enablePullToRefresh(boolean enabled) {
        mRefreshLayout.setEnabled(enabled && !isInEditMode());
    }

    public boolean isInEditMode() {
        return getActivity().findViewById(R.id.edit_lyrics).getVisibility() == View.VISIBLE;
    }

    @Override
    public void onRefresh() {
        fetchCurrentLyrics(true);
    }

    public String getSource() {
        if (mLyrics != null)
            return mLyrics.getSource();
        return null;
    }

    public boolean isLRC() {
        return mLyrics != null && mLyrics.isLRC();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_action:
                final Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                if (mLyrics != null && mLyrics.getURL() != null) {
                    sendIntent.putExtra(Intent.EXTRA_TEXT, mLyrics.getURL());
                    startActivity(Intent.createChooser(sendIntent, getString(R.string.share)));
                }
                return true;
            case R.id.action_search:
                getActivity().startService(new Intent(getActivity(), NotificationListenerService.class));
                MaterialSuggestionsSearchView suggestionsSearchView =
                        (MaterialSuggestionsSearchView) getActivity()
                                .findViewById(R.id.material_search_view);
                if (suggestionsSearchView.isSearchOpen())
                    ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar))
                            .expandToolbar(true);
                break;
            case R.id.save_action:
                if (mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT)
                    new WriteToDatabaseTask().execute(this, item, this.mLyrics);
                break;
            case R.id.convert_action:
                if (mLyrics.isLRC()) {
                    LrcView lrcView = (LrcView) getActivity().findViewById(R.id.lrc_view);
                    if (lrcView != null && lrcView.dictionnary != null)
                        update(lrcView.getStaticLyrics(), getView(), true);
                } else
                    update(DatabaseHelper.getInstance(getActivity())
                            .get(new String[]{mLyrics.getArtist(), mLyrics.getTitle(),
                                    mLyrics.getOriginalArtist(), mLyrics.getOriginalTrack()}), getView(), true);
        }
        return false;
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        final MainActivity mainActivity = (MainActivity) getActivity();
        Animator anim = null;
        if (showTransitionAnim) {
            if (nextAnim != 0)
                anim = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
            showTransitionAnim = false;
            if (anim != null)
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        if (mainActivity.drawer instanceof DrawerLayout)
                            ((DrawerLayout) mainActivity.drawer).closeDrawer(mainActivity.drawerView);
                        mainActivity.setDrawerListener(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override
                    public void onAnimationStart(Animator animator) {
                        mainActivity.setDrawerListener(false);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                    }
                });
        } else
            anim = AnimatorInflater.loadAnimator(getActivity(), R.animator.none);
        return anim;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return;
        CollapsingToolbarLayout toolbarLayout =
                (CollapsingToolbarLayout) mainActivity.findViewById(R.id.toolbar_layout);
        toolbarLayout.setTitle(getString(R.string.app_name));

        if (((DrawerLayout) mainActivity.drawer) // drawer is locked
                .getDrawerLockMode(mainActivity.drawerView) == DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            return;

        inflater.inflate(lyrics, menu);
        // Get the SearchView and set the searchable configuration
        final MaterialSuggestionsSearchView materialSearchView = (MaterialSuggestionsSearchView) mainActivity.findViewById(R.id.material_search_view);
        materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                materialSearchView.setSuggestions(null);
                materialSearchView.requestFocus();
                materialSearchView.post(new Runnable() {
                    @Override
                    public void run() {
                        ((InputMethodManager) getActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE))
                                .hideSoftInputFromWindow(materialSearchView.getWindowToken(), 0);
                    }
                });
                materialSearchView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity) getActivity()).search(query);
                        materialSearchView.setSuggestions(null);
                    }
                }, 90);
                mExpandedSearchView = false;
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!materialSearchView.hasSuggestions())
                    materialSearchView.setSuggestions(materialSearchView.getHistory());
                return true;
            }
        });

        materialSearchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                if (getActivity() == null)
                    return;
                ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar))
                        .expandToolbar(true);
                mExpandedSearchView = true;
            }

            @Override
            public void onSearchViewClosed() {
                mExpandedSearchView = false;
            }
        });

        final Resources resources = getResources();
        final int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            statusBarHeight = 0;
        else if (resourceId > 0)
            statusBarHeight = resources.getDimensionPixelSize(resourceId);
        else
            statusBarHeight = (int) Math.ceil((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 24 : 25) * resources.getDisplayMetrics().density);
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) materialSearchView.getLayoutParams();
        lp.setMargins(lp.leftMargin, statusBarHeight, lp.rightMargin, lp.bottomMargin);
        materialSearchView.setLayoutParams(lp);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        materialSearchView.setMenuItem(searchItem);

        if (!materialSearchView.isSearchOpen() && mExpandedSearchView) {
            materialSearchView.showSearch();
            mExpandedSearchView = false;
        } else if (!mExpandedSearchView)
            materialSearchView.closeSearch();

        materialSearchView.setHint(getString(R.string.search_hint));
        if (mSearchQuery != null && !mSearchQuery.equals("")) {
            searchItem.expandActionView();
            materialSearchView.setQuery(mSearchQuery, false);
            if (mSearchFocused)
                materialSearchView.requestFocus();
            else
                materialSearchView.clearFocus();
            mSearchQuery = null;
        }
        Lyrics storedLyrics = mLyrics == null ? null :
                DatabaseHelper.getInstance(getActivity()).get(new String[]{
                        mLyrics.getArtist(),
                        mLyrics.getTitle(),
                        mLyrics.getOriginalArtist(),
                        mLyrics.getOriginalTrack()});


        MenuItem saveMenuItem = menu.findItem(R.id.save_action);
        if (saveMenuItem != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (mLyrics == null)
                saveMenuItem.setVisible(false);
            else if (mLyrics.getFlag() == Lyrics.POSITIVE_RESULT
                    && sharedPref.getBoolean("pref_auto_save", true)) {
                if (storedLyrics == null || (mLyrics.isLRC() && !storedLyrics.isLRC())) {
                    lyricsPresentInDB = true;
                    new WriteToDatabaseTask().execute(this, saveMenuItem, mLyrics);
                }
                saveMenuItem.setVisible(false);
            } else {
                saveMenuItem.setIcon(lyricsPresentInDB ? R.drawable.ic_trash : R.drawable.ic_save);
                saveMenuItem.setTitle(lyricsPresentInDB ? R.string.remove_action : R.string.save_action);
            }
        }
        MenuItem resyncMenuItem = menu.findItem(R.id.resync_action);
        MenuItem convertMenuItem = menu.findItem(R.id.convert_action);
        if (resyncMenuItem != null)
            resyncMenuItem.setVisible(mLyrics != null && mLyrics.isLRC());
        if (convertMenuItem != null) {
            Lyrics stored = mLyrics == null || mLyrics.isLRC() ? null : storedLyrics;
            convertMenuItem.setVisible((mLyrics != null && (mLyrics.isLRC())) || (stored != null && stored.isLRC()));
            convertMenuItem.setTitle(stored == null ? R.string.full_text_action : R.string.pref_lrc);
        }

        MenuItem shareMenuItem = menu.findItem(R.id.share_action);
        if (shareMenuItem != null)
            shareMenuItem.setVisible(mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT && mLyrics.getURL() != null);
    }

    @Override
    public void onDestroy() {
        broadcastReceiver = null;
        threadCancelled = true;
        super.onDestroy();

    }

    public void setCoverArt(String url, FadeInNetworkImageView coverView) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return;
        mainActivity.findViewById(R.id.top_gradient).setVisibility(View.VISIBLE);
        mainActivity.findViewById(R.id.bottom_gradient).setVisibility(View.VISIBLE);
        if (coverView == null)
            coverView = (FadeInNetworkImageView) mainActivity.findViewById(R.id.cover);
        if (url == null)
            url = "";
        if (mLyrics != null) {
            mLyrics.setCoverURL(url);
            coverView.setLyrics(mLyrics);
        }
        coverView.clearColorFilter();
        if (url.startsWith("/")) {
            coverView.setImageBitmap(BitmapFactory.decodeFile(url));
        } else {
            coverView.setImageUrl(url,
                    new ImageLoader(Volley.newRequestQueue(mainActivity), CoverCache.instance()));
            if (!url.isEmpty() && mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT)
                DatabaseHelper.getInstance(getActivity()).updateCover(mLyrics.getArtist(), mLyrics.getTitle(), url);
        }
    }

    public void setCoverArt(Bitmap cover, FadeInNetworkImageView coverView) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return;
        if (coverView == null)
            coverView = (FadeInNetworkImageView) mainActivity.findViewById(R.id.cover);
        if (coverView != null)
            coverView.setLocalImageBitmap(cover);
        coverView.clearColorFilter();
        getActivity().findViewById(R.id.top_gradient).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.bottom_gradient).setVisibility(View.VISIBLE);
    }

    public void expandToolbar() {
        ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar)).expandToolbar(true);
    }

    public void updateLRC() {
        if (mLrcThread == null || !mLrcThread.isAlive()) {
            mLrcThread = new Thread(lrcUpdater);
            mLrcThread.start();
        }
    }

    public void startEmpty(boolean startEmpty) {
        this.startEmpty = startEmpty;
    }

    private Runnable lrcUpdater = new Runnable() {
        @Override
        public void run() {
            if (threadCancelled)
                return;
            boolean ran = false;
            if (getActivity() == null)
                return;
            SharedPreferences preferences = getActivity().getSharedPreferences("current_music", Context.MODE_PRIVATE);
            long position = preferences.getLong("position", 0);
            final LrcView[] lrcView = {((LrcView) LyricsViewFragment.this.getActivity().findViewById(R.id.lrc_view))};

            if (lrcView[0] != null)
                if (getActivity() != null && (position == -1 || !getActivity().getPreferences(Context.MODE_PRIVATE).getBoolean("pref_lrc", true))) {
                    final Lyrics staticLyrics = lrcView[0].getStaticLyrics();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update(staticLyrics, getView(), true);
                        }
                    });
                    return;
                } else if (getActivity() != null) {
                    final long finalPosition = position;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Activity activity = LyricsViewFragment.this.getActivity();
                            if (activity != null)
                                ((LrcView) activity.findViewById(R.id.lrc_view))
                                        .changeCurrent(finalPosition);
                        }
                    });
                }

            MusicBroadcastReceiver.forceAutoUpdate(true);
            while (getActivity() != null &&
                    preferences.getString("track", "").equalsIgnoreCase(mLyrics.getOriginalTrack()) &&
                    preferences.getString("artist", "").equalsIgnoreCase(mLyrics.getOriginalArtist()) &&
                    preferences.getBoolean("playing", true)) {
                if (threadCancelled)
                    return;
                ran = true;
                position = preferences.getLong("position", 0);
                long startTime = preferences.getLong("startTime", System.currentTimeMillis());
                long distance = System.currentTimeMillis() - startTime;
                if (preferences.getBoolean("playing", true))
                    position += distance;
                final long finalPosition = position;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (lrcView[0] == null)
                            lrcView[0] = ((LrcView) LyricsViewFragment.this.getActivity().findViewById(R.id.lrc_view));
                        if (lrcView[0] != null)
                            lrcView[0].changeCurrent(finalPosition);
                    }
                });
                //String time = String.valueOf((position / 60000)) + " min ";
                //time += String.valueOf((position / 1000) % 60) + " sec";
                //Log.i("QuickLyric", time);
                //Log.d("QuickLyric", "Playing:"+preferences.getBoolean("playing", true));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            MusicBroadcastReceiver.forceAutoUpdate(true);
            if (preferences.getBoolean("playing", true) && ran && mLyrics.isLRC() && getActivity() != null)
                fetchCurrentLyrics(false);
        }
    };

    public void updateSearchView(boolean collapsed, String query, boolean focused) {
        this.mExpandedSearchView = !collapsed;
        if (query != null)
            this.mSearchQuery = query;
        this.mSearchFocused = focused;
        getActivity().invalidateOptionsMenu();
    }
}
