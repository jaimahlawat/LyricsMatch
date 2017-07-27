package com.dragedy.playermusic.lyricspack.event;

import com.dragedy.playermusic.lyricspack.model.Lyrics;

/**
 * Created by steve on 4/9/17.
 */

public class RecentsRetrievedEvent {
    public final Lyrics lyrics;

    public RecentsRetrievedEvent(Lyrics lyrics)
    {
        this.lyrics = lyrics;
    }
}
