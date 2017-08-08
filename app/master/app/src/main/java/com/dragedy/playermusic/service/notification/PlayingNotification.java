package com.dragedy.playermusic.service.notification;

import com.dragedy.playermusic.service.MusicService;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public interface PlayingNotification {
    int NOTIFICATION_ID = 1;

    void init(MusicService service);

    void update();

    void stop();
}
