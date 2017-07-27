package com.dragedy.playermusic.ui.fragments.player;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.dragedy.playermusic.R;
import com.dragedy.playermusic.dialogs.AddToPlaylistDialog;
import com.dragedy.playermusic.dialogs.SleepTimerDialog;
import com.dragedy.playermusic.dialogs.SongDetailDialog;
import com.dragedy.playermusic.dialogs.SongShareDialog;
import com.dragedy.playermusic.helper.MusicPlayerRemote;
import com.dragedy.playermusic.interfaces.PaletteColorHolder;
import com.dragedy.playermusic.loader.SongLoader;
import com.dragedy.playermusic.model.Song;
import com.dragedy.playermusic.ui.activities.tageditor.AbsTagEditorActivity;
import com.dragedy.playermusic.ui.activities.tageditor.SongTagEditorActivity;
import com.dragedy.playermusic.ui.fragments.AbsMusicServiceFragment;
import com.dragedy.playermusic.util.MusicUtil;
import com.dragedy.playermusic.util.NavigationUtil;

public abstract class AbsPlayerFragment extends AbsMusicServiceFragment implements Toolbar.OnMenuItemClickListener, PaletteColorHolder {
    public static final String TAG = AbsPlayerFragment.class.getSimpleName();

    private Callbacks callbacks;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            callbacks = (Callbacks) context;
        } catch (ClassCastException e) {
            throw new RuntimeException(context.getClass().getSimpleName() + " must implement " + Callbacks.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final Song song = MusicPlayerRemote.getCurrentSong();
        switch (item.getItemId()) {
            case R.id.action_sleep_timer:
                new SleepTimerDialog().show(getFragmentManager(), "SET_SLEEP_TIMER");
                return true;
            case R.id.action_toggle_favorite:
                toggleFavorite(song);
                return true;
            case R.id.action_share:
                SongShareDialog.create(song).show(getFragmentManager(), "SHARE_SONG");
                return true;
            case R.id.action_equalizer:
                NavigationUtil.openEqualizer(getActivity());
                return true;
            case R.id.action_shuffle_all:
                MusicPlayerRemote.openAndShuffleQueue(SongLoader.getAllSongs(getActivity()), true);
                return true;
            case R.id.action_add_to_playlist:
                AddToPlaylistDialog.create(song).show(getFragmentManager(), "ADD_PLAYLIST");
                return true;
            case R.id.action_clear_playing_queue:
                MusicPlayerRemote.clearQueue();
                return true;
            case R.id.action_tag_editor:
                Intent intent = new Intent(getActivity(), SongTagEditorActivity.class);
                intent.putExtra(AbsTagEditorActivity.EXTRA_ID, song.id);
                startActivity(intent);
                return true;
            case R.id.action_details:
                SongDetailDialog.create(song).show(getFragmentManager(), "SONG_DETAIL");
                return true;
            case R.id.action_go_to_album:
                NavigationUtil.goToAlbum(getActivity(), song.albumId);
                return true;
            case R.id.action_go_to_artist:
                NavigationUtil.goToArtist(getActivity(), song.artistId);
                return true;
        }
        return false;
    }

    protected void toggleFavorite(Song song) {
        MusicUtil.toggleFavorite(getActivity(), song);
    }

    public abstract void onShow();

    public abstract void onHide();

    public abstract boolean onBackPressed();

    public Callbacks getCallbacks() {
        return callbacks;
    }

    public interface Callbacks {
        void onPaletteColorChanged();
    }
}
