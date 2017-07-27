package com.dragedy.playermusic.appshortcuts.shortcuttype;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutInfo;

import com.dragedy.playermusic.R;
import com.dragedy.playermusic.appshortcuts.AppShortcutIconGenerator;
import com.dragedy.playermusic.appshortcuts.AppShortcutLauncherActivity;

/**
 * @author Adrian Campos
 */

@TargetApi(25)
public final class LastAddedShortcutType extends BaseShortcutType {
    public LastAddedShortcutType(Context context) {
        super(context);
    }

    public ShortcutInfo getShortcutInfo() {
        return new ShortcutInfo.Builder(context, getId())
                .setShortLabel(context.getString(R.string.app_shortcut_last_added_short))
                .setLongLabel(context.getString(R.string.app_shortcut_last_added_long))
                .setIcon(AppShortcutIconGenerator.generateThemedIcon(context, R.drawable.ic_app_shortcut_last_added))
                .setIntent(getPlaySongsIntent(AppShortcutLauncherActivity.SHORTCUT_TYPE_LAST_ADDED))
                .build();
    }

    public static String getId(){
        return ID_PREFIX + "last_added";
    }
}
