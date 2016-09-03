/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui;

import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimationCompat.AnimatorListenerAdapterProxy;
import org.telegram.messenger.AnimationCompat.AnimatorSetProxy;
import org.telegram.messenger.AnimationCompat.ObjectAnimatorProxy;
import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.FileLoader;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.messenger.AnimationCompat.ViewProxy;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.TextInfoCell;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.AvatarUpdater;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberPicker;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class SettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider {

    private ListView listView;
    private ListAdapter listAdapter;
    private BackupImageView avatarImage;
    private TextView nameTextView;
    private TextView onlineTextView;
    private ImageView writeButton;
    private AnimatorSetProxy writeButtonAnimation;
    private AvatarUpdater avatarUpdater = new AvatarUpdater();
    private View extraHeightView;
    private View shadowView;

    private int extraHeight;

    private int overscrollRow;
    private int emptyRow;
    private int numberSectionRow;
    private int numberRow;
    private int usernameRow;
    private int settingsSectionRow;
    private int settingsSectionRow2;
    private int enableAnimationsRow;
    private int notificationRow;
    private int backgroundRow;
    private int languageRow;
    private int privacyRow;
    private int mediaDownloadSection;
    private int mediaDownloadSection2;
    private int mobileDownloadRow;
    private int wifiDownloadRow;
    private int roamingDownloadRow;
    private int saveToGalleryRow;
    private int messagesSectionRow;
    private int messagesSectionRow2;
    private int customTabsRow;
    private int directShareRow;
    private int textSizeRow;
    private int stickersRow;
    private int cacheRow;
    private int raiseToSpeakRow;
    private int sendByEnterRow;
   //private int useSystemEmojiRow;
    private int supportSectionRow;
    private int supportSectionRow2;
    //  private int askQuestionRow;
    //private int telegramFaqRow;
    private int privacyPolicyRow;
    private int sendLogsRow;
    private int clearLogsRow;
    private int switchBackendButtonRow;
    private int versionRow;
    private int contactsSectionRow;
    private int contactsReimportRow;
    private int contactsSortRow;
    private int autoplayGifsRow;
    private int rowCount;

    private final static int edit_name = 1;
    private final static int logout = 2;

    private static class LinkMovementMethodMy extends LinkMovementMethod {
        @Override
        public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer, @NonNull MotionEvent event) {
            try {
                return super.onTouchEvent(widget, buffer, event);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
            return false;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        avatarUpdater.parentFragment = this;
        avatarUpdater.delegate = new AvatarUpdater.AvatarUpdaterDelegate() {
            @Override
            public void didUploadedPhoto(TLRPC.InputFile file, TLRPC.PhotoSize small, TLRPC.PhotoSize big) {
                TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                req.caption = "";
                req.crop = new TLRPC.TL_inputPhotoCropAuto();
                req.file = file;
                req.geo_point = new TLRPC.TL_inputGeoPointEmpty();
                ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        if (error == null) {
                            TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                            if (user == null) {
                                user = UserConfig.getCurrentUser();
                                if (user == null) {
                                    return;
                                }
                                MessagesController.getInstance().putUser(user, false);
                            } else {
                                UserConfig.setCurrentUser(user);
                            }
                            TLRPC.TL_photos_photo photo = (TLRPC.TL_photos_photo) response;
                            ArrayList<TLRPC.PhotoSize> sizes = photo.photo.sizes;
                            TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 100);
                            TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 1000);
                            user.photo = new TLRPC.TL_userProfilePhoto();
                            user.photo.photo_id = photo.photo.id;
                            if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            if (bigSize != null) {
                                user.photo.photo_big = bigSize.location;
                            } else if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            MessagesStorage.getInstance().clearUserPhotos(user.id);
                            ArrayList<TLRPC.User> users = new ArrayList<>();
                            users.add(user);
                            MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
                                    UserConfig.saveConfig(true);
                                }
                            });
                        }
                    }
                });
            }
        };
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);

        rowCount = 0;
        overscrollRow = rowCount++;
        emptyRow = rowCount++;
        numberSectionRow = rowCount++;
        numberRow = rowCount++;
        usernameRow = rowCount++;
        settingsSectionRow = rowCount++;
        settingsSectionRow2 = rowCount++;
        notificationRow = rowCount++;
        privacyRow = rowCount++;
        backgroundRow = rowCount++;
        languageRow = rowCount++;
        enableAnimationsRow = rowCount++;
        mediaDownloadSection = rowCount++;
        mediaDownloadSection2 = rowCount++;
        mobileDownloadRow = rowCount++;
        wifiDownloadRow = rowCount++;
        roamingDownloadRow = rowCount++;
        if (Build.VERSION.SDK_INT >= 11) {
            autoplayGifsRow = rowCount++;
        }
        saveToGalleryRow = rowCount++;
        messagesSectionRow = rowCount++;
        messagesSectionRow2 = rowCount++;
        customTabsRow = rowCount++;
        if (Build.VERSION.SDK_INT >= 23) {
            directShareRow = rowCount++;
        }
        textSizeRow = rowCount++;
        stickersRow = rowCount++;
        cacheRow = rowCount++;
        raiseToSpeakRow = rowCount++;
        sendByEnterRow = rowCount++;
      // useSystemEmojiRow = rowCount++;
        supportSectionRow = rowCount++;
        supportSectionRow2 = rowCount++;
        //  askQuestionRow = rowCount++;
        //telegramFaqRow = rowCount++;
        privacyPolicyRow = rowCount++;
        if (BuildVars.DEBUG_VERSION) {
            sendLogsRow = rowCount++;
            clearLogsRow = rowCount++;
            switchBackendButtonRow = rowCount++;
        }
        versionRow = rowCount++;
        //contactsSectionRow = rowCount++;
        //contactsReimportRow = rowCount++;
        //contactsSortRow = rowCount++;

        MessagesController.getInstance().loadFullUser(UserConfig.getCurrentUser(), classGuid, true);

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (avatarImage != null) {
            avatarImage.setImageDrawable(null);
        }
        MessagesController.getInstance().cancelLoadFullUser(UserConfig.getClientUserId());
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        avatarUpdater.clear();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackgroundColor(AvatarDrawable.getProfileBackColorForId(5));
        actionBar.setItemsBackground(AvatarDrawable.getButtonColorForId(5));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAddToContainer(false);
        extraHeight = 88;
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == edit_name) {
                    presentFragment(new ChangeNameActivity());
                } else if (id == logout) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.getString("AreYouSureLogout", R.string.AreYouSureLogout));
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MessagesController.getInstance().performLogout(true);
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                }
            }
        });
        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_other);
        item.addSubItem(edit_name, LocaleController.getString("EditName", R.string.EditName), 0);
        item.addSubItem(logout, LocaleController.getString("LogOut", R.string.LogOut), 0);

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context) {
            @Override
            protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
                if (child == listView) {
                    boolean result = super.drawChild(canvas, child, drawingTime);
                    if (parentLayout != null) {
                        int actionBarHeight = 0;
                        int childCount = getChildCount();
                        for (int a = 0; a < childCount; a++) {
                            View view = getChildAt(a);
                            if (view == child) {
                                continue;
                            }
                            if (view instanceof ActionBar && view.getVisibility() == VISIBLE) {
                                if (((ActionBar) view).getCastShadows()) {
                                    actionBarHeight = view.getMeasuredHeight();
                                }
                                break;
                            }
                        }
                        parentLayout.drawHeaderShadow(canvas, actionBarHeight);
                    }
                    return result;
                } else {
                    return super.drawChild(canvas, child, drawingTime);
                }
            }
        };
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        AndroidUtilities.setListViewEdgeEffectColor(listView, AvatarDrawable.getProfileBackColorForId(5));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == textSizeRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("TextSize", R.string.TextSize));
                    final NumberPicker numberPicker = new NumberPicker(getParentActivity());
                    numberPicker.setMinValue(12);
                    numberPicker.setMaxValue(30);
                    numberPicker.setValue(MessagesController.getInstance().fontSize);
                    builder.setView(numberPicker);
                    builder.setNegativeButton(LocaleController.getString("Done", R.string.Done), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("fons_size", numberPicker.getValue());
                            MessagesController.getInstance().fontSize = numberPicker.getValue();
                            editor.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    showDialog(builder.create());
                } else if (i == enableAnimationsRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean animations = preferences.getBoolean("view_animations", true);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("view_animations", !animations);
                    editor.commit();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!animations);
                    }
                } else if (i == notificationRow) {
                    presentFragment(new NotificationsSettingsActivity());
                } else if (i == backgroundRow) {
                    presentFragment(new WallpapersActivity());
                } /*else if (i == askQuestionRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    final TextView message = new TextView(getParentActivity());
                    message.setText(Html.fromHtml(LocaleController.getString("AskAQuestionInfo", R.string.AskAQuestionInfo)));
                    message.setTextSize(18);
                    message.setLinkTextColor(0xff316f9f);
                    message.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(5), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
                    message.setMovementMethod(new LinkMovementMethodMy());

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setView(message);
                    builder.setPositiveButton(LocaleController.getString("AskButton", R.string.AskButton), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            performAskAQuestion();
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } */
                else if (i == sendLogsRow) {
                    sendLogs();
                } else if (i == clearLogsRow) {
                    FileLog.cleanupLogs();
                } else if (i == sendByEnterRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean send = preferences.getBoolean("send_by_enter", false);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("send_by_enter", !send);
                    editor.commit();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!send);
                    }
                } else if (i == raiseToSpeakRow) {
                    MediaController.getInstance().toogleRaiseToSpeak();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canRaiseToSpeak());
                    }
                } else if (i == autoplayGifsRow) {
                    MediaController.getInstance().toggleAutoplayGifs();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canAutoplayGifs());
                    }
                } else if (i == saveToGalleryRow) {
                    MediaController.getInstance().toggleSaveToGallery();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canSaveToGallery());
                    }
                }/* else if (i == useSystemEmojiRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean use = preferences.getBoolean("use_system_emoji", false);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("use_system_emoji", !use);
                    editor.commit();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!use);
                    }
                } */else if (i == customTabsRow) {
                    MediaController.getInstance().toggleCustomTabs();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canCustomTabs());
                    }
                } else if(i == directShareRow) {
                    MediaController.getInstance().toggleDirectShare();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canDirectShare());
                    }
                } else if (i == privacyRow) {
                    presentFragment(new PrivacySettingsActivity());
                } else if (i == languageRow) {
                    presentFragment(new LanguageSelectActivity());
                } else if (i == switchBackendButtonRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.getString("AreYouSure", R.string.AreYouSure));
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ConnectionsManager.getInstance().switchBackend();
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } /*else if (i == telegramFaqRow) {
                    AndroidUtilities.openUrl(getParentActivity(), LocaleController.getString("TelegramFaqUrl", R.string.TelegramFaqUrl));
                } */
                else if (i == privacyPolicyRow) {
                    AndroidUtilities.openUrl(getParentActivity(), LocaleController.getString("PrivacyPolicyUrl", R.string.PrivacyPolicyUrl));
                } else if (i == contactsReimportRow) {
                    //not implemented
                } else if (i == contactsSortRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("SortBy", R.string.SortBy));
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("Default", R.string.Default),
                            LocaleController.getString("SortFirstName", R.string.SortFirstName),
                            LocaleController.getString("SortLastName", R.string.SortLastName)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("sortContactsBy", which);
                            editor.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == wifiDownloadRow || i == mobileDownloadRow || i == roamingDownloadRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    final boolean maskValues[] = new boolean[6];
                    BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());

                    int mask = 0;
                    if (i == mobileDownloadRow) {
                        mask = MediaController.getInstance().mobileDataDownloadMask;
                    } else if (i == wifiDownloadRow) {
                        mask = MediaController.getInstance().wifiDownloadMask;
                    } else if (i == roamingDownloadRow) {
                        mask = MediaController.getInstance().roamingDownloadMask;
                    }

                    builder.setApplyTopPaddings(false);
                    LinearLayout linearLayout = new LinearLayout(getParentActivity());
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    for (int a = 0; a < 6; a++) {
                        String name = null;
                        if (a == 0) {
                            maskValues[a] = (mask & MediaController.AUTODOWNLOAD_MASK_PHOTO) != 0;
                            name = LocaleController.getString("AttachPhoto", R.string.AttachPhoto);
                        } else if (a == 1) {
                            maskValues[a] = (mask & MediaController.AUTODOWNLOAD_MASK_AUDIO) != 0;
                            name = LocaleController.getString("AttachAudio", R.string.AttachAudio);
                        } else if (a == 2) {
                            maskValues[a] = (mask & MediaController.AUTODOWNLOAD_MASK_VIDEO) != 0;
                            name = LocaleController.getString("AttachVideo", R.string.AttachVideo);
                        } else if (a == 3) {
                            maskValues[a] = (mask & MediaController.AUTODOWNLOAD_MASK_DOCUMENT) != 0;
                            name = LocaleController.getString("AttachDocument", R.string.AttachDocument);
                        } else if (a == 4) {
                            maskValues[a] = (mask & MediaController.AUTODOWNLOAD_MASK_MUSIC) != 0;
                            name = LocaleController.getString("AttachMusic", R.string.AttachMusic);
                        } else if (a == 5) {
                            if (Build.VERSION.SDK_INT >= 11) {
                                maskValues[a] = (mask & MediaController.AUTODOWNLOAD_MASK_GIF) != 0;
                                name = LocaleController.getString("AttachGif", R.string.AttachGif);
                            } else {
                                continue;
                            }
                        }
                        CheckBoxCell checkBoxCell = new CheckBoxCell(getParentActivity());
                        checkBoxCell.setTag(a);
                        checkBoxCell.setBackgroundResource(R.drawable.list_selector);
                        linearLayout.addView(checkBoxCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                        checkBoxCell.setText(name, "", maskValues[a], true);
                        checkBoxCell.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CheckBoxCell cell = (CheckBoxCell) v;
                                int num = (Integer) cell.getTag();
                                maskValues[num] = !maskValues[num];
                                cell.setChecked(maskValues[num], true);
                            }
                        });
                    }
                    BottomSheet.BottomSheetCell cell = new BottomSheet.BottomSheetCell(getParentActivity(), 2);
                    cell.setBackgroundResource(R.drawable.list_selector);
                    cell.setTextAndIcon(LocaleController.getString("Save", R.string.Save).toUpperCase(), 0);
                    cell.setTextColor(0xff517fad);
                    cell.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                if (visibleDialog != null) {
                                    visibleDialog.dismiss();
                                }
                            } catch (Exception e) {
                                FileLog.e("tmessages", e);
                            }
                            int newMask = 0;
                            for (int a = 0; a < 6; a++) {
                                if (maskValues[a]) {
                                    if (a == 0) {
                                        newMask |= MediaController.AUTODOWNLOAD_MASK_PHOTO;
                                    } else if (a == 1) {
                                        newMask |= MediaController.AUTODOWNLOAD_MASK_AUDIO;
                                    } else if (a == 2) {
                                        newMask |= MediaController.AUTODOWNLOAD_MASK_VIDEO;
                                    } else if (a == 3) {
                                        newMask |= MediaController.AUTODOWNLOAD_MASK_DOCUMENT;
                                    } else if (a == 4) {
                                        newMask |= MediaController.AUTODOWNLOAD_MASK_MUSIC;
                                    } else if (a == 5) {
                                        newMask |= MediaController.AUTODOWNLOAD_MASK_GIF;
                                    }
                                }
                            }
                            SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit();
                            if (i == mobileDownloadRow) {
                                editor.putInt("mobileDataDownloadMask", newMask);
                                MediaController.getInstance().mobileDataDownloadMask = newMask;
                            } else if (i == wifiDownloadRow) {
                                editor.putInt("wifiDownloadMask", newMask);
                                MediaController.getInstance().wifiDownloadMask = newMask;
                            } else if (i == roamingDownloadRow) {
                                editor.putInt("roamingDownloadMask", newMask);
                                MediaController.getInstance().roamingDownloadMask = newMask;
                            }
                            editor.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    linearLayout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
                    builder.setCustomView(linearLayout);
                    showDialog(builder.create());
                } else if (i == usernameRow) {
                    presentFragment(new ChangeUsernameActivity());
                } else if (i == numberRow) {
                    presentFragment(new ChangePhoneHelpActivity());
                } else if (i == stickersRow) {
                    presentFragment(new StickersActivity());
                } else if (i == cacheRow) {
                    presentFragment(new CacheControlActivity());
                }
            }
        });

        frameLayout.addView(actionBar);

        extraHeightView = new View(context);
        ViewProxy.setPivotY(extraHeightView, 0);
        extraHeightView.setBackgroundColor(AvatarDrawable.getProfileBackColorForId(5));
        frameLayout.addView(extraHeightView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 88));

        shadowView = new View(context);
        shadowView.setBackgroundResource(R.drawable.header_shadow);
        frameLayout.addView(shadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3));

        avatarImage = new BackupImageView(context);
        avatarImage.setRoundRadius(AndroidUtilities.dp(21));
        ViewProxy.setPivotX(avatarImage, 0);
        ViewProxy.setPivotY(avatarImage, 0);
        frameLayout.addView(avatarImage, LayoutHelper.createFrame(42, 42, Gravity.TOP | Gravity.LEFT, 64, 0, 0, 0));
        avatarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                if (user != null && user.photo != null && user.photo.photo_big != null) {
                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                    PhotoViewer.getInstance().openPhoto(user.photo.photo_big, SettingsActivity.this);
                }
            }
        });

        nameTextView = new TextView(context);
        nameTextView.setTextColor(0xff000000);      /// EDITED
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setGravity(Gravity.LEFT);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        ViewProxy.setPivotX(nameTextView, 0);
        ViewProxy.setPivotY(nameTextView, 0);
        frameLayout.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 48, 0));

        onlineTextView = new TextView(context);
        onlineTextView.setTextColor(AvatarDrawable.getProfileTextColorForId(5));
        onlineTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        onlineTextView.setLines(1);
        onlineTextView.setMaxLines(1);
        onlineTextView.setSingleLine(true);
        onlineTextView.setEllipsize(TextUtils.TruncateAt.END);
        onlineTextView.setGravity(Gravity.LEFT);
        frameLayout.addView(onlineTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 48, 0));

        writeButton = new ImageView(context);
        writeButton.setBackgroundResource(R.drawable.floating_user_states);
        writeButton.setImageResource(R.drawable.floating_camera);
        writeButton.setScaleType(ImageView.ScaleType.CENTER);
        if (Build.VERSION.SDK_INT >= 21) {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(writeButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, ObjectAnimator.ofFloat(writeButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            writeButton.setStateListAnimator(animator);
            writeButton.setOutlineProvider(new ViewOutlineProvider() {
                @SuppressLint("NewApi")
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }
        frameLayout.addView(writeButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP, 0, 0, 16, 0));
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

                CharSequence[] items;

                TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                if (user == null) {
                    user = UserConfig.getCurrentUser();
                }
                if (user == null) {
                    return;
                }
                boolean fullMenu = false;
                if (user.photo != null && user.photo.photo_big != null && !(user.photo instanceof TLRPC.TL_userProfilePhotoEmpty)) {
                    items = new CharSequence[]{LocaleController.getString("FromCamera", R.string.FromCamera), LocaleController.getString("FromGalley", R.string.FromGalley), LocaleController.getString("DeletePhoto", R.string.DeletePhoto)};
                    fullMenu = true;
                } else {
                    items = new CharSequence[]{LocaleController.getString("FromCamera", R.string.FromCamera), LocaleController.getString("FromGalley", R.string.FromGalley)};
                }

                final boolean full = fullMenu;
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            avatarUpdater.openCamera();
                        } else if (i == 1) {
                            avatarUpdater.openGallery();
                        } else if (i == 2) {
                            MessagesController.getInstance().deleteUserPhoto(null);
                        }
                    }
                });
                showDialog(builder.create());
            }
        });

        needLayout();

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (totalItemCount == 0) {
                    return;
                }
                int height = 0;
                View child = view.getChildAt(0);
                if (child != null) {
                    if (firstVisibleItem == 0) {
                        height = AndroidUtilities.dp(88) + (child.getTop() < 0 ? child.getTop() : 0);
                    }
                    if (extraHeight != height) {
                        extraHeight = height;
                        needLayout();
                    }
                }
            }
        });

        return fragmentView;
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        MediaController.getInstance().checkAutodownloadSettings();
    }

    @Override
    public void updatePhotoAtIndex(int index) {

    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        if (fileLocation == null) {
            return null;
        }
        TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
        if (user != null && user.photo != null && user.photo.photo_big != null) {
            TLRPC.FileLocation photoBig = user.photo.photo_big;
            if (photoBig.local_id == fileLocation.local_id && photoBig.volume_id == fileLocation.volume_id && photoBig.dc_id == fileLocation.dc_id) {
                int coords[] = new int[2];
                avatarImage.getLocationInWindow(coords);
                PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                object.viewX = coords[0];
                object.viewY = coords[1] - AndroidUtilities.statusBarHeight;
                object.parentView = avatarImage;
                object.imageReceiver = avatarImage.getImageReceiver();
                object.user_id = UserConfig.getClientUserId();
                object.thumb = object.imageReceiver.getBitmap();
                object.size = -1;
                object.radius = avatarImage.getImageReceiver().getRoundRadius();
                object.scale = ViewProxy.getScaleX(avatarImage);
                return object;
            }
        }
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
    }

    @Override
    public void willHidePhotoViewer() {
        avatarImage.getImageReceiver().setVisible(true, true);
    }

    @Override
    public boolean isPhotoChecked(int index) {
        return false;
    }

    @Override
    public void setPhotoChecked(int index) {
    }

    @Override
    public boolean cancelButtonPressed() {
        return true;
    }

    @Override
    public void sendButtonPressed(int index) {
    }

    @Override
    public int getSelectedCount() {
        return 0;
    }

    public void performAskAQuestion() {
        final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        int uid = preferences.getInt("support_id", 0);
        TLRPC.User supportUser = null;
        if (uid != 0) {
            supportUser = MessagesController.getInstance().getUser(uid);
            if (supportUser == null) {
                String userString = preferences.getString("support_user", null);
                if (userString != null) {
                    try {
                        byte[] datacentersBytes = Base64.decode(userString, Base64.DEFAULT);
                        if (datacentersBytes != null) {
                            SerializedData data = new SerializedData(datacentersBytes);
                            supportUser = TLRPC.User.TLdeserialize(data, data.readInt32(false), false);
                            if (supportUser != null && supportUser.id == 333000) {
                                supportUser = null;
                            }
                            data.cleanup();
                        }
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                        supportUser = null;
                    }
                }
            }
        }
        if (supportUser == null) {
            final ProgressDialog progressDialog = new ProgressDialog(getParentActivity());
            progressDialog.setMessage(LocaleController.getString("Loading", R.string.Loading));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
            TLRPC.TL_help_getSupport req = new TLRPC.TL_help_getSupport();
            ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {

                        final TLRPC.TL_help_support res = (TLRPC.TL_help_support) response;
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt("support_id", res.user.id);
                                SerializedData data = new SerializedData();
                                res.user.serializeToStream(data);
                                editor.putString("support_user", Base64.encodeToString(data.toByteArray(), Base64.DEFAULT));
                                editor.commit();
                                data.cleanup();
                                try {
                                    progressDialog.dismiss();
                                } catch (Exception e) {
                                    FileLog.e("tmessages", e);
                                }
                                ArrayList<TLRPC.User> users = new ArrayList<>();
                                users.add(res.user);
                                MessagesStorage.getInstance().putUsersAndChats(users, null, true, true);
                                MessagesController.getInstance().putUser(res.user, false);
                                Bundle args = new Bundle();
                                args.putInt("user_id", res.user.id);
                                presentFragment(new ChatActivity(args));
                            }
                        });
                    } else {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    progressDialog.dismiss();
                                } catch (Exception e) {
                                    FileLog.e("tmessages", e);
                                }
                            }
                        });
                    }
                }
            });
        } else {
            MessagesController.getInstance().putUser(supportUser, true);
            Bundle args = new Bundle();
            args.putInt("user_id", supportUser.id);
            presentFragment(new ChatActivity(args));
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        avatarUpdater.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (avatarUpdater != null && avatarUpdater.currentPicturePath != null) {
            args.putString("path", avatarUpdater.currentPicturePath);
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        if (avatarUpdater != null) {
            avatarUpdater.currentPicturePath = args.getString("path");
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer) args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                updateUserData();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        updateUserData();
        fixLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    private void needLayout() {
        FrameLayout.LayoutParams layoutParams;
        int newTop = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();
        if (listView != null) {
            layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
            if (layoutParams.topMargin != newTop) {
                layoutParams.topMargin = newTop;
                listView.setLayoutParams(layoutParams);
                ViewProxy.setTranslationY(extraHeightView, newTop);
            }
        }

        if (avatarImage != null) {
            float diff = extraHeight / (float) AndroidUtilities.dp(88);
            ViewProxy.setScaleY(extraHeightView, diff);
            ViewProxy.setTranslationY(shadowView, newTop + extraHeight);

            if (Build.VERSION.SDK_INT < 11) {
                layoutParams = (FrameLayout.LayoutParams) writeButton.getLayoutParams();
                layoutParams.topMargin = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + extraHeight - AndroidUtilities.dp(29.5f);
                writeButton.setLayoutParams(layoutParams);
            } else {
                ViewProxy.setTranslationY(writeButton, (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + extraHeight - AndroidUtilities.dp(29.5f));
            }

            final boolean setVisible = diff > 0.2f;
            boolean currentVisible = writeButton.getTag() == null;
            if (setVisible != currentVisible) {
                if (setVisible) {
                    writeButton.setTag(null);
                    writeButton.setVisibility(View.VISIBLE);
                } else {
                    writeButton.setTag(0);
                }
                if (writeButtonAnimation != null) {
                    AnimatorSetProxy old = writeButtonAnimation;
                    writeButtonAnimation = null;
                    old.cancel();
                }
                writeButtonAnimation = new AnimatorSetProxy();
                if (setVisible) {
                    writeButtonAnimation.setInterpolator(new DecelerateInterpolator());
                    writeButtonAnimation.playTogether(
                            ObjectAnimatorProxy.ofFloat(writeButton, "scaleX", 1.0f),
                            ObjectAnimatorProxy.ofFloat(writeButton, "scaleY", 1.0f),
                            ObjectAnimatorProxy.ofFloat(writeButton, "alpha", 1.0f)
                    );
                } else {
                    writeButtonAnimation.setInterpolator(new AccelerateInterpolator());
                    writeButtonAnimation.playTogether(
                            ObjectAnimatorProxy.ofFloat(writeButton, "scaleX", 0.2f),
                            ObjectAnimatorProxy.ofFloat(writeButton, "scaleY", 0.2f),
                            ObjectAnimatorProxy.ofFloat(writeButton, "alpha", 0.0f)
                    );
                }
                writeButtonAnimation.setDuration(150);
                writeButtonAnimation.addListener(new AnimatorListenerAdapterProxy() {
                    @Override
                    public void onAnimationEnd(Object animation) {
                        if (writeButtonAnimation != null && writeButtonAnimation.equals(animation)) {
                            writeButton.clearAnimation();
                            writeButton.setVisibility(setVisible ? View.VISIBLE : View.GONE);
                            writeButtonAnimation = null;
                        }
                    }
                });
                writeButtonAnimation.start();
            }

            ViewProxy.setScaleX(avatarImage, (42 + 18 * diff) / 42.0f);
            ViewProxy.setScaleY(avatarImage, (42 + 18 * diff) / 42.0f);
            float avatarY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f * (1.0f + diff) - 21 * AndroidUtilities.density + 27 * AndroidUtilities.density * diff;
            ViewProxy.setTranslationX(avatarImage, -AndroidUtilities.dp(47) * diff);
            ViewProxy.setTranslationY(avatarImage, (float) Math.ceil(avatarY));
            ViewProxy.setTranslationX(nameTextView, -21 * AndroidUtilities.density * diff);
            ViewProxy.setTranslationY(nameTextView, (float) Math.floor(avatarY) - (float) Math.ceil(AndroidUtilities.density) + (float) Math.floor(7 * AndroidUtilities.density * diff));
            ViewProxy.setTranslationX(onlineTextView, -21 * AndroidUtilities.density * diff);
            ViewProxy.setTranslationY(onlineTextView, (float) Math.floor(avatarY) + AndroidUtilities.dp(22) + (float )Math.floor(11 * AndroidUtilities.density) * diff);
            ViewProxy.setScaleX(nameTextView, 1.0f + 0.12f * diff);
            ViewProxy.setScaleY(nameTextView, 1.0f + 0.12f * diff);
        }
    }

    private void fixLayout() {
        if (fragmentView == null) {
            return;
        }
        fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (fragmentView != null) {
                    needLayout();
                    fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    private void updateUserData() {
        TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
        TLRPC.FileLocation photo = null;
        TLRPC.FileLocation photoBig = null;
        if (user.photo != null) {
            photo = user.photo.photo_small;
            photoBig = user.photo.photo_big;
        }
        AvatarDrawable avatarDrawable = new AvatarDrawable(user, true);
        avatarDrawable.setColor(0xff888888);   // EDITED
        if (avatarImage != null) {
            avatarImage.setImage(photo, "50_50", avatarDrawable);
            avatarImage.getImageReceiver().setVisible(!PhotoViewer.getInstance().isShowingImage(photoBig), false);

            nameTextView.setText(UserObject.getUserName(user));
            onlineTextView.setText(LocaleController.getString("Online", R.string.Online));

            avatarImage.getImageReceiver().setVisible(!PhotoViewer.getInstance().isShowingImage(photoBig), false);
        }
    }

    private void sendLogs() {
        try {
            ArrayList<Uri> uris = new ArrayList<>();
            File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            File[] files = dir.listFiles();
            for (File file : files) {
                uris.add(Uri.fromFile(file));
            }

            if (uris.isEmpty()) {
                return;
            }
            Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{BuildVars.SEND_LOGS_EMAIL});
            i.putExtra(Intent.EXTRA_SUBJECT, "last logs");
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            getParentActivity().startActivityForResult(Intent.createChooser(i, "Select email application."), 500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return i == textSizeRow || i == enableAnimationsRow || i == notificationRow || i == backgroundRow || i == numberRow ||
                    i == sendLogsRow || i == sendByEnterRow || i == autoplayGifsRow || i == privacyRow || i == wifiDownloadRow ||
                    i == mobileDownloadRow || i == clearLogsRow || i == roamingDownloadRow || i == languageRow || i == usernameRow ||
                    i == switchBackendButtonRow || i == contactsSortRow || i == contactsReimportRow || i == saveToGalleryRow ||
                   /* i == useSystemEmojiRow ||*/
                    i == stickersRow || i == cacheRow || i == raiseToSpeakRow || i == privacyPolicyRow || i == customTabsRow || i == directShareRow;
        }

        @Override
        public int getCount() {
            return rowCount;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);
            if (type == 0) {
                if (view == null) {
                    view = new EmptyCell(mContext);
                }
                if (i == overscrollRow) {
                    ((EmptyCell) view).setHeight(AndroidUtilities.dp(88));
                } else {
                    ((EmptyCell) view).setHeight(AndroidUtilities.dp(16));
                }
            } else if (type == 1) {
                if (view == null) {
                    view = new ShadowSectionCell(mContext);
                }
            } else if (type == 2) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == textSizeRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int size = preferences.getInt("fons_size", AndroidUtilities.isTablet() ? 18 : 16);
                    textCell.setTextAndValue(LocaleController.getString("TextSize", R.string.TextSize), String.format("%d", size), true);
                } else if (i == languageRow) {
                    textCell.setTextAndValue(LocaleController.getString("Language", R.string.Language), LocaleController.getCurrentLanguageName(), true);
                } else if (i == contactsSortRow) {
                    String value;
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int sort = preferences.getInt("sortContactsBy", 0);
                    if (sort == 0) {
                        value = LocaleController.getString("Default", R.string.Default);
                    } else if (sort == 1) {
                        value = LocaleController.getString("FirstName", R.string.SortFirstName);
                    } else {
                        value = LocaleController.getString("LastName", R.string.SortLastName);
                    }
                    textCell.setTextAndValue(LocaleController.getString("SortBy", R.string.SortBy), value, true);
                } else if (i == notificationRow) {
                    textCell.setText(LocaleController.getString("NotificationsAndSounds", R.string.NotificationsAndSounds), true);
                } else if (i == backgroundRow) {
                    textCell.setText(LocaleController.getString("ChatBackground", R.string.ChatBackground), true);
                } else if (i == sendLogsRow) {
                    textCell.setText("Send Logs", true);
                } else if (i == clearLogsRow) {
                    textCell.setText("Clear Logs", true);
                } /*else if (i == askQuestionRow) {
                    textCell.setText(LocaleController.getString("AskAQuestion", R.string.AskAQuestion), true);
                } */else if (i == privacyRow) {
                    textCell.setText(LocaleController.getString("PrivacySettings", R.string.PrivacySettings), true);
                } else if (i == switchBackendButtonRow) {
                    textCell.setText("Switch Backend", true);
                } /*else if (i == telegramFaqRow) {
                    textCell.setText(LocaleController.getString("TelegramFAQ", R.string.TelegramFaq), true);
                } */
                else if (i == contactsReimportRow) {
                    textCell.setText(LocaleController.getString("ImportContacts", R.string.ImportContacts), true);
                } else if (i == stickersRow) {
                    textCell.setText(LocaleController.getString("Stickers", R.string.Stickers), true);
                } else if (i == cacheRow) {
                    textCell.setText(LocaleController.getString("CacheSettings", R.string.CacheSettings), true);
                } else if (i == privacyPolicyRow) {
                    textCell.setText(LocaleController.getString("PrivacyPolicy", R.string.PrivacyPolicy), true);
                }
            } else if (type == 3) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                }
                TextCheckCell textCell = (TextCheckCell) view;

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                if (i == enableAnimationsRow) {
                    textCell.setTextAndCheck(LocaleController.getString("EnableAnimations", R.string.EnableAnimations), preferences.getBoolean("view_animations", true), false);
                } else if (i == sendByEnterRow) {
                    textCell.setTextAndCheck(LocaleController.getString("SendByEnter", R.string.SendByEnter), preferences.getBoolean("send_by_enter", false), true);
                } /*else if (i == useSystemEmojiRow) {
                    textCell.setTextAndCheck(LocaleController.getString("UseSystemEmoji", R.string.UseSystemEmoji), preferences.getBoolean("use_system_emoji", false), true);
                }  */else if (i == saveToGalleryRow) {
                    textCell.setTextAndCheck(LocaleController.getString("SaveToGallerySettings", R.string.SaveToGallerySettings), MediaController.getInstance().canSaveToGallery(), false);
                } else if (i == autoplayGifsRow) {
                    textCell.setTextAndCheck(LocaleController.getString("AutoplayGifs", R.string.AutoplayGifs), MediaController.getInstance().canAutoplayGifs(), true);
                } else if (i == raiseToSpeakRow) {
                    textCell.setTextAndCheck(LocaleController.getString("RaiseToSpeak", R.string.RaiseToSpeak), MediaController.getInstance().canRaiseToSpeak(), true);
                } else if (i == customTabsRow) {
                    textCell.setTextAndValueAndCheck(LocaleController.getString("ChromeCustomTabs", R.string.ChromeCustomTabs), LocaleController.getString("ChromeCustomTabsInfo", R.string.ChromeCustomTabsInfo), MediaController.getInstance().canCustomTabs(), true);
                } else if (i == directShareRow) {
                    textCell.setTextAndValueAndCheck(LocaleController.getString("DirectShare", R.string.DirectShare), LocaleController.getString("DirectShareInfo", R.string.DirectShareInfo), MediaController.getInstance().canDirectShare(), true);
                }
            } else if (type == 4) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                }
                if (i == settingsSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("SETTINGS", R.string.SETTINGS));
                } else if (i == supportSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("Support", R.string.Support));
                } else if (i == messagesSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("MessagesSettings", R.string.MessagesSettings));
                } else if (i == mediaDownloadSection2) {
                    ((HeaderCell) view).setText(LocaleController.getString("AutomaticMediaDownload", R.string.AutomaticMediaDownload));
                } else if (i == numberSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("Info", R.string.Info));
                }
            } else if (type == 5) {
                if (view == null) {
                    view = new TextInfoCell(mContext);
                    try {
                        PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                        int code = pInfo.versionCode / 10;
                        String abi = "";
                        switch (pInfo.versionCode % 10) {
                            case 0:
                                abi = "arm";
                                break;
                            case 1:
                                abi = "arm-v7a";
                                break;
                            case 2:
                                abi = "x86";
                                break;
                            case 3:
                                abi = "universal";
                                break;
                        }
                        ((TextInfoCell) view).setText(String.format(Locale.US, "Fam v%s (%d) %s\n Dragedy, Inc." + (BuildVars.DEBUG_VERSION ? " [TEST MODE]" : "") + "", pInfo.versionName, code, abi));
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            } else if (type == 6) {
                if (view == null) {
                    view = new TextDetailSettingsCell(mContext);
                }
                TextDetailSettingsCell textCell = (TextDetailSettingsCell) view;

                if (i == mobileDownloadRow || i == wifiDownloadRow || i == roamingDownloadRow) {
                    int mask;
                    String value;
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    if (i == mobileDownloadRow) {
                        value = LocaleController.getString("WhenUsingMobileData", R.string.WhenUsingMobileData);
                        mask = MediaController.getInstance().mobileDataDownloadMask;
                    } else if (i == wifiDownloadRow) {
                        value = LocaleController.getString("WhenConnectedOnWiFi", R.string.WhenConnectedOnWiFi);
                        mask = MediaController.getInstance().wifiDownloadMask;
                    } else {
                        value = LocaleController.getString("WhenRoaming", R.string.WhenRoaming);
                        mask = MediaController.getInstance().roamingDownloadMask;
                    }
                    String text = "";
                    if ((mask & MediaController.AUTODOWNLOAD_MASK_PHOTO) != 0) {
                        text += LocaleController.getString("AttachPhoto", R.string.AttachPhoto);
                    }
                    if ((mask & MediaController.AUTODOWNLOAD_MASK_AUDIO) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("AttachAudio", R.string.AttachAudio);
                    }
                    if ((mask & MediaController.AUTODOWNLOAD_MASK_VIDEO) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("AttachVideo", R.string.AttachVideo);
                    }
                    if ((mask & MediaController.AUTODOWNLOAD_MASK_DOCUMENT) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("AttachDocument", R.string.AttachDocument);
                    }
                    if ((mask & MediaController.AUTODOWNLOAD_MASK_MUSIC) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("AttachMusic", R.string.AttachMusic);
                    }
                    if (Build.VERSION.SDK_INT >= 11) {
                        if ((mask & MediaController.AUTODOWNLOAD_MASK_GIF) != 0) {
                            if (text.length() != 0) {
                                text += ", ";
                            }
                            text += LocaleController.getString("AttachGif", R.string.AttachGif);
                        }
                    }
                    if (text.length() == 0) {
                        text = LocaleController.getString("NoMediaAutoDownload", R.string.NoMediaAutoDownload);
                    }
                    textCell.setTextAndValue(value, text, true);
                } else if (i == numberRow) {
                    TLRPC.User user = UserConfig.getCurrentUser();
                    String value;
                    if (user != null && user.phone != null && user.phone.length() != 0) {
                        value = PhoneFormat.getInstance().format("+" + user.phone);
                    } else {
                        value = LocaleController.getString("NumberUnknown", R.string.NumberUnknown);
                    }
                    textCell.setTextAndValue(value, LocaleController.getString("Phone", R.string.Phone), true);
                } else if (i == usernameRow) {
                    TLRPC.User user = UserConfig.getCurrentUser();
                    String value;
                    if (user != null && user.username != null && user.username.length() != 0) {
                        value = "@" + user.username;
                    } else {
                        value = LocaleController.getString("UsernameEmpty", R.string.UsernameEmpty);
                    }
                    textCell.setTextAndValue(value, LocaleController.getString("Username", R.string.Username), false);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == emptyRow || i == overscrollRow) {
                return 0;
            }
            if (i == settingsSectionRow || i == supportSectionRow || i == messagesSectionRow || i == mediaDownloadSection || i == contactsSectionRow) {
                return 1;
            } else if (i == enableAnimationsRow || i == sendByEnterRow || i == saveToGalleryRow || i == autoplayGifsRow || i == raiseToSpeakRow || i == customTabsRow || i == directShareRow/* || i == useSystemEmojiRow*/) {
                return 3;
            } else if (i == notificationRow || i == backgroundRow || i == sendLogsRow || i == privacyRow || i == clearLogsRow || i == switchBackendButtonRow  || i == contactsReimportRow || i == textSizeRow || i == languageRow || i == contactsSortRow || i == stickersRow || i == cacheRow || i == privacyPolicyRow) {
                return 2;
            } else if (i == versionRow) {
                return 5;
            } else if (i == wifiDownloadRow || i == mobileDownloadRow || i == roamingDownloadRow || i == numberRow || i == usernameRow) {
                return 6;
            } else if (i == settingsSectionRow2 || i == messagesSectionRow2 || i == supportSectionRow2 || i == numberSectionRow || i == mediaDownloadSection2) {
                return 4;
            } else {
                return 2;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 7;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}


/*
package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spannable;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimatorListenerAdapterProxy;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationCenter.NotificationCenterDelegate;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.AbstractSerializedData;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC.FileLocation;
import org.telegram.tgnet.TLRPC.InputFile;
import org.telegram.tgnet.TLRPC.Photo;
import org.telegram.tgnet.TLRPC.PhotoSize;
import org.telegram.tgnet.TLRPC.TL_error;
import org.telegram.tgnet.TLRPC.TL_help_getSupport;
import org.telegram.tgnet.TLRPC.TL_help_support;
import org.telegram.tgnet.TLRPC.TL_inputGeoPointEmpty;
import org.telegram.tgnet.TLRPC.TL_inputPhotoCropAuto;
import org.telegram.tgnet.TLRPC.TL_photos_photo;
import org.telegram.tgnet.TLRPC.TL_photos_uploadProfilePhoto;
import org.telegram.tgnet.TLRPC.TL_userProfilePhoto;
import org.telegram.tgnet.TLRPC.TL_userProfilePhotoEmpty;
import org.telegram.tgnet.TLRPC.User;
import org.telegram.tgnet.TLRPC.UserProfilePhoto;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet.BottomSheetCell;
import org.telegram.ui.ActionBar.BottomSheet.Builder;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.AvatarUpdater;
import org.telegram.ui.Components.AvatarUpdater.AvatarUpdaterDelegate;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberPicker;

public class SettingsActivity extends BaseFragment
  implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider
{
  private static final int edit_name = 1;
  private static final int logout = 2;
  private int askQuestionRow;
  private int autoplayGifsRow;
  private BackupImageView avatarImage;
  private AvatarUpdater avatarUpdater = new AvatarUpdater();
  private int backgroundRow;
  private int cacheRow;
  private int clearLogsRow;
  private int contactsReimportRow;
  private int contactsSectionRow;
  private int contactsSortRow;
  private int customTabsRow;
  private int directShareRow;
  private int emptyRow;
  private int enableAnimationsRow;
  private int extraHeight;
  private View extraHeightView;
  private int languageRow;
  private ListAdapter listAdapter;
  private ListView listView;
  private int mediaDownloadSection;
  private int mediaDownloadSection2;
  private int messagesSectionRow;
  private int messagesSectionRow2;
  private int mobileDownloadRow;
  private TextView nameTextView;
  private int notificationRow;
  private int numberRow;
  private int numberSectionRow;
  private TextView onlineTextView;
  private int overscrollRow;
  private int privacyPolicyRow;
  private int privacyRow;
  private int raiseToSpeakRow;
  private int roamingDownloadRow;
  private int rowCount;
  private int saveToGalleryRow;
  private int sendByEnterRow;
  private int sendLogsRow;
  private int settingsSectionRow;
  private int settingsSectionRow2;
  private View shadowView;
  private int stickersRow;
  private int supportSectionRow;
  private int supportSectionRow2;
  private int switchBackendButtonRow;
  private int telegramFaqRow;
  private int textSizeRow;
  private int usernameRow;
  private int versionRow;
  private int wifiDownloadRow;
  private ImageView writeButton;
  private AnimatorSet writeButtonAnimation;

  private void fixLayout()
  {
    if (this.fragmentView == null)
      return;
    this.fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
    {
      public boolean onPreDraw()
      {
        if (SettingsActivity.this.fragmentView != null)
        {
          SettingsActivity.this.needLayout();
          SettingsActivity.this.fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
        }
        return true;
      }
    });
  }

  private void needLayout()
  {
    float f1;
    label135: boolean bool1;
    label169: boolean bool2;
    if (this.actionBar.getOccupyStatusBar())
    {
      int i = AndroidUtilities.statusBarHeight;
      i += ActionBar.getCurrentActionBarHeight();
      Object localObject;
      if (this.listView != null)
      {
        localObject = (FrameLayout.LayoutParams)this.listView.getLayoutParams();
        if (((FrameLayout.LayoutParams)localObject).topMargin != i)
        {
          ((FrameLayout.LayoutParams)localObject).topMargin = i;
          this.listView.setLayoutParams((ViewGroup.LayoutParams)localObject);
          this.extraHeightView.setTranslationY(i);
        }
      }
      if (this.avatarImage != null)
      {
        f1 = this.extraHeight / AndroidUtilities.dp(88.0F);
        this.extraHeightView.setScaleY(f1);
        this.shadowView.setTranslationY(this.extraHeight + i);
        localObject = this.writeButton;
        if (!this.actionBar.getOccupyStatusBar())
          break label624;
        j = AndroidUtilities.statusBarHeight;
        ((ImageView)localObject).setTranslationY(j + ActionBar.getCurrentActionBarHeight() + this.extraHeight - AndroidUtilities.dp(29.5F));
        if (f1 <= 0.2F)
          break label629;
        bool1 = true;
        if (this.writeButton.getTag() != null)
          break label635;
        bool2 = true;
        label182: if (bool1 != bool2)
        {
          if (!bool1)
            break label641;
          this.writeButton.setTag(null);
          this.writeButton.setVisibility(0);
          label210: if (this.writeButtonAnimation != null)
          {
            localObject = this.writeButtonAnimation;
            this.writeButtonAnimation = null;
            ((AnimatorSet)localObject).cancel();
          }
          this.writeButtonAnimation = new AnimatorSet();
          if (!bool1)
            break label655;
          this.writeButtonAnimation.setInterpolator(new DecelerateInterpolator());
          this.writeButtonAnimation.playTogether(new Animator[] { ObjectAnimator.ofFloat(this.writeButton, "scaleX", new float[] { 1.0F }), ObjectAnimator.ofFloat(this.writeButton, "scaleY", new float[] { 1.0F }), ObjectAnimator.ofFloat(this.writeButton, "alpha", new float[] { 1.0F }) });
          label334: this.writeButtonAnimation.setDuration(150L);
          this.writeButtonAnimation.addListener(new AnimatorListenerAdapterProxy(bool1)
          {
            public void onAnimationEnd(Animator paramAnimator)
            {
              if ((SettingsActivity.this.writeButtonAnimation != null) && (SettingsActivity.this.writeButtonAnimation.equals(paramAnimator)))
              {
                paramAnimator = SettingsActivity.this.writeButton;
                if (!this.val$setVisible)
                  break label56;
              }
              label56: for (int i = 0; ; i = 8)
              {
                paramAnimator.setVisibility(i);
                SettingsActivity.access$3902(SettingsActivity.this, null);
                return;
              }
            }
          });
          this.writeButtonAnimation.start();
        }
        this.avatarImage.setScaleX((42.0F + 18.0F * f1) / 42.0F);
        this.avatarImage.setScaleY((42.0F + 18.0F * f1) / 42.0F);
        if (!this.actionBar.getOccupyStatusBar())
          break label747;
      }
    }
    label641: label655: label747: for (int j = AndroidUtilities.statusBarHeight; ; j = 0)
    {
      float f2 = j + ActionBar.getCurrentActionBarHeight() / 2.0F * (1.0F + f1) - 21.0F * AndroidUtilities.density + 27.0F * AndroidUtilities.density * f1;
      this.avatarImage.setTranslationX(-AndroidUtilities.dp(47.0F) * f1);
      this.avatarImage.setTranslationY((float)Math.ceil(f2));
      this.nameTextView.setTranslationX(-21.0F * AndroidUtilities.density * f1);
      this.nameTextView.setTranslationY((float)Math.floor(f2) - (float)Math.ceil(AndroidUtilities.density) + (float)Math.floor(7.0F * AndroidUtilities.density * f1));
      this.onlineTextView.setTranslationX(-21.0F * AndroidUtilities.density * f1);
      this.onlineTextView.setTranslationY((float)Math.floor(f2) + AndroidUtilities.dp(22.0F) + (float)Math.floor(11.0F * AndroidUtilities.density) * f1);
      this.nameTextView.setScaleX(1.0F + 0.12F * f1);
      this.nameTextView.setScaleY(1.0F + 0.12F * f1);
      return;
      j = 0;
      break;
      label624: j = 0;
      break label135;
      label629: bool1 = false;
      break label169;
      label635: bool2 = false;
      break label182;
      this.writeButton.setTag(Integer.valueOf(0));
      break label210;
      this.writeButtonAnimation.setInterpolator(new AccelerateInterpolator());
      this.writeButtonAnimation.playTogether(new Animator[] { ObjectAnimator.ofFloat(this.writeButton, "scaleX", new float[] { 0.2F }), ObjectAnimator.ofFloat(this.writeButton, "scaleY", new float[] { 0.2F }), ObjectAnimator.ofFloat(this.writeButton, "alpha", new float[] { 0.0F }) });
      break label334;
    }
  }

  private void performAskAQuestion()
  {
    SharedPreferences localSharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
    int i = localSharedPreferences.getInt("support_id", 0);
    Object localObject1 = null;
    Object localObject2;
    Object localObject3;
    if (i != 0)
    {
      localObject2 = MessagesController.getInstance().getUser(Integer.valueOf(i));
      localObject1 = localObject2;
      if (localObject2 == null)
      {
        localObject3 = localSharedPreferences.getString("support_user", null);
        localObject1 = localObject2;
        if (localObject3 == null);
      }
    }
    try
    {
      localObject3 = Base64.decode((String)localObject3, 0);
      localObject1 = localObject2;
      if (localObject3 != null)
      {
        localObject3 = new SerializedData(localObject3);
        localObject2 = TLRPC.User.TLdeserialize((AbstractSerializedData)localObject3, ((SerializedData)localObject3).readInt32(false), false);
        localObject1 = localObject2;
        if (localObject2 != null)
        {
          localObject1 = localObject2;
          if (((TLRPC.User)localObject2).id == 333000)
            localObject1 = null;
        }
        ((SerializedData)localObject3).cleanup();
      }
      if (localObject1 == null)
      {
        localObject1 = new ProgressDialog(getParentActivity());
        ((ProgressDialog)localObject1).setMessage(LocaleController.getString("Loading", 2131165797));
        ((ProgressDialog)localObject1).setCanceledOnTouchOutside(false);
        ((ProgressDialog)localObject1).setCancelable(false);
        ((ProgressDialog)localObject1).show();
        localObject2 = new TLRPC.TL_help_getSupport();
        ConnectionsManager.getInstance().sendRequest((TLObject)localObject2, new RequestDelegate(localSharedPreferences, (ProgressDialog)localObject1)
        {
          public void run(TLObject paramTLObject, TLRPC.TL_error paramTL_error)
          {
            if (paramTL_error == null)
            {
              AndroidUtilities.runOnUIThread(new Runnable((TLRPC.TL_help_support)paramTLObject)
              {
                public void run()
                {
                  Object localObject = SettingsActivity.10.this.val$preferences.edit();
                  ((SharedPreferences.Editor)localObject).putInt("support_id", this.val$res.user.id);
                  SerializedData localSerializedData = new SerializedData();
                  this.val$res.user.serializeToStream(localSerializedData);
                  ((SharedPreferences.Editor)localObject).putString("support_user", Base64.encodeToString(localSerializedData.toByteArray(), 0));
                  ((SharedPreferences.Editor)localObject).commit();
                  localSerializedData.cleanup();
                  try
                  {
                    SettingsActivity.10.this.val$progressDialog.dismiss();
                    localObject = new ArrayList();
                    ((ArrayList)localObject).add(this.val$res.user);
                    MessagesStorage.getInstance().putUsersAndChats((ArrayList)localObject, null, true, true);
                    MessagesController.getInstance().putUser(this.val$res.user, false);
                    localObject = new Bundle();
                    ((Bundle)localObject).putInt("user_id", this.val$res.user.id);
                    SettingsActivity.this.presentFragment(new ChatActivity((Bundle)localObject));
                    return;
                  }
                  catch (Exception localException)
                  {
                    while (true)
                      FileLog.e("tmessages", localException);
                  }
                }
              });
              return;
            }
            AndroidUtilities.runOnUIThread(new Runnable()
            {
              public void run()
              {
                try
                {
                  SettingsActivity.10.this.val$progressDialog.dismiss();
                  return;
                }
                catch (Exception localException)
                {
                  FileLog.e("tmessages", localException);
                }
              }
            });
          }
        });
        return;
      }
    }
    catch (Exception localUser)
    {
      TLRPC.User localUser;
      while (true)
      {
        FileLog.e("tmessages", localException);
        localUser = null;
      }
      MessagesController.getInstance().putUser(localUser, true);
      localObject2 = new Bundle();
      ((Bundle)localObject2).putInt("user_id", localUser.id);
      presentFragment(new ChatActivity((Bundle)localObject2));
    }
  }

  private void sendLogs()
  {
    try
    {
      ArrayList localArrayList = new ArrayList();
      Object localObject = ApplicationLoader.applicationContext.getExternalFilesDir(null);
      localObject = new File(((File)localObject).getAbsolutePath() + "/logs").listFiles();
      int j = localObject.length;
      int i = 0;
      while (i < j)
      {
        localArrayList.add(Uri.fromFile(localObject[i]));
        i += 1;
      }
      if (localArrayList.isEmpty())
        return;
      localObject = new Intent("android.intent.action.SEND_MULTIPLE");
      ((Intent)localObject).setType("message/rfc822");
      ((Intent)localObject).putExtra("android.intent.extra.EMAIL", new String[] { BuildVars.SEND_LOGS_EMAIL });
      ((Intent)localObject).putExtra("android.intent.extra.SUBJECT", "last logs");
      ((Intent)localObject).putParcelableArrayListExtra("android.intent.extra.STREAM", localArrayList);
      getParentActivity().startActivityForResult(Intent.createChooser((Intent)localObject, "Select email application."), 500);
      return;
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }

  private void updateUserData()
  {
    boolean bool2 = true;
    TLRPC.User localUser = MessagesController.getInstance().getUser(Integer.valueOf(UserConfig.getClientUserId()));
    Object localObject = null;
    TLRPC.FileLocation localFileLocation = null;
    if (localUser.photo != null)
    {
      localObject = localUser.photo.photo_small;
      localFileLocation = localUser.photo.photo_big;
    }
    AvatarDrawable localAvatarDrawable = new AvatarDrawable(localUser, true);
    localAvatarDrawable.setColor(-11500111);
    if (this.avatarImage != null)
    {
      this.avatarImage.setImage((TLObject)localObject, "50_50", localAvatarDrawable);
      localObject = this.avatarImage.getImageReceiver();
      if (PhotoViewer.getInstance().isShowingImage(localFileLocation))
        break label174;
      bool1 = true;
      ((ImageReceiver)localObject).setVisible(bool1, false);
      this.nameTextView.setText(UserObject.getUserName(localUser));
      this.onlineTextView.setText(LocaleController.getString("Online", 2131165995));
      localObject = this.avatarImage.getImageReceiver();
      if (PhotoViewer.getInstance().isShowingImage(localFileLocation))
        break label179;
    }
    label174: label179: for (boolean bool1 = bool2; ; bool1 = false)
    {
      ((ImageReceiver)localObject).setVisible(bool1, false);
      return;
      bool1 = false;
      break;
    }
  }

  public boolean cancelButtonPressed()
  {
    return true;
  }

  public View createView(Context paramContext)
  {
    this.actionBar.setBackgroundColor(AvatarDrawable.getProfileBackColorForId(5));
    this.actionBar.setItemsBackgroundColor(AvatarDrawable.getButtonColorForId(5));
    this.actionBar.setBackButtonImage(2130837707);
    this.actionBar.setAddToContainer(false);
    this.extraHeight = 88;
    if (AndroidUtilities.isTablet())
      this.actionBar.setOccupyStatusBar(false);
    this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick()
    {
      public void onItemClick(int paramInt)
      {
        if (paramInt == -1)
          SettingsActivity.this.finishFragment();
        do
        {
          return;
          if (paramInt != 1)
            continue;
          SettingsActivity.this.presentFragment(new ChangeNameActivity());
          return;
        }
        while ((paramInt != 2) || (SettingsActivity.this.getParentActivity() == null));
        AlertDialog.Builder localBuilder = new AlertDialog.Builder(SettingsActivity.this.getParentActivity());
        localBuilder.setMessage(LocaleController.getString("AreYouSureLogout", 2131165315));
        localBuilder.setTitle(LocaleController.getString("AppName", 2131165300));
        localBuilder.setPositiveButton(LocaleController.getString("OK", 2131165993), new DialogInterface.OnClickListener()
        {
          public void onClick(DialogInterface paramDialogInterface, int paramInt)
          {
            MessagesController.getInstance().performLogout(true);
          }
        });
        localBuilder.setNegativeButton(LocaleController.getString("Cancel", 2131165374), null);
        SettingsActivity.this.showDialog(localBuilder.create());
      }
    });
    Object localObject = this.actionBar.createMenu().addItem(0, 2130837715);
    ((ActionBarMenuItem)localObject).addSubItem(1, LocaleController.getString("EditName", 2131165568), 0);
    ((ActionBarMenuItem)localObject).addSubItem(2, LocaleController.getString("LogOut", 2131165807), 0);
    this.listAdapter = new ListAdapter(paramContext);
    this.fragmentView = new FrameLayout(paramContext)
    {
      protected boolean drawChild(@NonNull Canvas paramCanvas, @NonNull View paramView, long paramLong)
      {
        if (paramView == SettingsActivity.this.listView)
        {
          boolean bool = super.drawChild(paramCanvas, paramView, paramLong);
          if (SettingsActivity.this.parentLayout != null)
          {
            int k = 0;
            int m = getChildCount();
            int i = 0;
            int j = k;
            if (i < m)
            {
              View localView = getChildAt(i);
              if (localView == paramView);
              do
              {
                i += 1;
                break;
              }
              while ((!(localView instanceof ActionBar)) || (localView.getVisibility() != 0));
              j = k;
              if (((ActionBar)localView).getCastShadows())
                j = localView.getMeasuredHeight();
            }
            SettingsActivity.this.parentLayout.drawHeaderShadow(paramCanvas, j);
          }
          return bool;
        }
        return super.drawChild(paramCanvas, paramView, paramLong);
      }
    };
    localObject = (FrameLayout)this.fragmentView;
    this.listView = new ListView(paramContext);
    this.listView.setDivider(null);
    this.listView.setDividerHeight(0);
    this.listView.setVerticalScrollBarEnabled(false);
    AndroidUtilities.setListViewEdgeEffectColor(this.listView, AvatarDrawable.getProfileBackColorForId(5));
    ((FrameLayout)localObject).addView(this.listView, LayoutHelper.createFrame(-1, -1, 51));
    this.listView.setAdapter(this.listAdapter);
    this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
    {
      public void onItemClick(AdapterView<?> paramAdapterView, View paramView, int paramInt, long paramLong)
      {
        if (paramInt == SettingsActivity.this.textSizeRow)
          if (SettingsActivity.this.getParentActivity() != null);
        label224:
        do
        {
          while (true)
          {
            return;
            paramAdapterView = new AlertDialog.Builder(SettingsActivity.this.getParentActivity());
            paramAdapterView.setTitle(LocaleController.getString("TextSize", 2131166251));
            paramView = new NumberPicker(SettingsActivity.this.getParentActivity());
            paramView.setMinValue(12);
            paramView.setMaxValue(30);
            paramView.setValue(MessagesController.getInstance().fontSize);
            paramAdapterView.setView(paramView);
            paramAdapterView.setNegativeButton(LocaleController.getString("Done", 2131165562), new DialogInterface.OnClickListener(paramView)
            {
              public void onClick(DialogInterface paramDialogInterface, int paramInt)
              {
                paramDialogInterface = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).edit();
                paramDialogInterface.putInt("fons_size", this.val$numberPicker.getValue());
                MessagesController.getInstance().fontSize = this.val$numberPicker.getValue();
                paramDialogInterface.commit();
                if (SettingsActivity.this.listView != null)
                  SettingsActivity.this.listView.invalidateViews();
              }
            });
            SettingsActivity.this.showDialog(paramAdapterView.create());
            return;
            boolean bool2;
            if (paramInt == SettingsActivity.this.enableAnimationsRow)
            {
              paramAdapterView = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
              bool2 = paramAdapterView.getBoolean("view_animations", true);
              paramAdapterView = paramAdapterView.edit();
              if (!bool2)
              {
                bool1 = true;
                paramAdapterView.putBoolean("view_animations", bool1);
                paramAdapterView.commit();
                if (!(paramView instanceof TextCheckCell))
                  continue;
                paramAdapterView = (TextCheckCell)paramView;
                if (bool2)
                  break label224;
              }
              for (bool1 = true; ; bool1 = false)
              {
                paramAdapterView.setChecked(bool1);
                return;
                bool1 = false;
                break;
              }
            }
            if (paramInt == SettingsActivity.this.notificationRow)
            {
              SettingsActivity.this.presentFragment(new NotificationsSettingsActivity());
              return;
            }
            if (paramInt == SettingsActivity.this.backgroundRow)
            {
              SettingsActivity.this.presentFragment(new WallpapersActivity());
              return;
            }
            if (paramInt == SettingsActivity.this.askQuestionRow)
            {
              if (SettingsActivity.this.getParentActivity() == null)
                continue;
              paramAdapterView = new TextView(SettingsActivity.this.getParentActivity());
              paramAdapterView.setText(Html.fromHtml(LocaleController.getString("AskAQuestionInfo", 2131165324)));
              paramAdapterView.setTextSize(18.0F);
              paramAdapterView.setLinkTextColor(-14255946);
              paramAdapterView.setPadding(AndroidUtilities.dp(8.0F), AndroidUtilities.dp(5.0F), AndroidUtilities.dp(8.0F), AndroidUtilities.dp(6.0F));
              paramAdapterView.setMovementMethod(new SettingsActivity.LinkMovementMethodMy(null));
              paramView = new AlertDialog.Builder(SettingsActivity.this.getParentActivity());
              paramView.setView(paramAdapterView);
              paramView.setPositiveButton(LocaleController.getString("AskButton", 2131165325), new DialogInterface.OnClickListener()
              {
                public void onClick(DialogInterface paramDialogInterface, int paramInt)
                {
                  SettingsActivity.this.performAskAQuestion();
                }
              });
              paramView.setNegativeButton(LocaleController.getString("Cancel", 2131165374), null);
              SettingsActivity.this.showDialog(paramView.create());
              return;
            }
            if (paramInt == SettingsActivity.this.sendLogsRow)
            {
              SettingsActivity.this.sendLogs();
              return;
            }
            if (paramInt == SettingsActivity.this.clearLogsRow)
            {
              FileLog.cleanupLogs();
              return;
            }
            if (paramInt == SettingsActivity.this.sendByEnterRow)
            {
              paramAdapterView = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
              bool2 = paramAdapterView.getBoolean("send_by_enter", false);
              paramAdapterView = paramAdapterView.edit();
              if (!bool2)
              {
                bool1 = true;
                paramAdapterView.putBoolean("send_by_enter", bool1);
                paramAdapterView.commit();
                if (!(paramView instanceof TextCheckCell))
                  continue;
                paramAdapterView = (TextCheckCell)paramView;
                if (bool2)
                  break label581;
              }
              for (bool1 = true; ; bool1 = false)
              {
                paramAdapterView.setChecked(bool1);
                return;
                bool1 = false;
                break;
              }
            }
            if (paramInt == SettingsActivity.this.raiseToSpeakRow)
            {
              MediaController.getInstance().toogleRaiseToSpeak();
              if (!(paramView instanceof TextCheckCell))
                continue;
              ((TextCheckCell)paramView).setChecked(MediaController.getInstance().canRaiseToSpeak());
              return;
            }
            if (paramInt == SettingsActivity.this.autoplayGifsRow)
            {
              MediaController.getInstance().toggleAutoplayGifs();
              if (!(paramView instanceof TextCheckCell))
                continue;
              ((TextCheckCell)paramView).setChecked(MediaController.getInstance().canAutoplayGifs());
              return;
            }
            if (paramInt == SettingsActivity.this.saveToGalleryRow)
            {
              MediaController.getInstance().toggleSaveToGallery();
              if (!(paramView instanceof TextCheckCell))
                continue;
              ((TextCheckCell)paramView).setChecked(MediaController.getInstance().canSaveToGallery());
              return;
            }
            if (paramInt == SettingsActivity.this.customTabsRow)
            {
              MediaController.getInstance().toggleCustomTabs();
              if (!(paramView instanceof TextCheckCell))
                continue;
              ((TextCheckCell)paramView).setChecked(MediaController.getInstance().canCustomTabs());
              return;
            }
            if (paramInt == SettingsActivity.this.directShareRow)
            {
              MediaController.getInstance().toggleDirectShare();
              if (!(paramView instanceof TextCheckCell))
                continue;
              ((TextCheckCell)paramView).setChecked(MediaController.getInstance().canDirectShare());
              return;
            }
            if (paramInt == SettingsActivity.this.privacyRow)
            {
              SettingsActivity.this.presentFragment(new PrivacySettingsActivity());
              return;
            }
            if (paramInt == SettingsActivity.this.languageRow)
            {
              SettingsActivity.this.presentFragment(new LanguageSelectActivity());
              return;
            }
            if (paramInt == SettingsActivity.this.switchBackendButtonRow)
            {
              if (SettingsActivity.this.getParentActivity() == null)
                continue;
              paramAdapterView = new AlertDialog.Builder(SettingsActivity.this.getParentActivity());
              paramAdapterView.setMessage(LocaleController.getString("AreYouSure", 2131165304));
              paramAdapterView.setTitle(LocaleController.getString("AppName", 2131165300));
              paramAdapterView.setPositiveButton(LocaleController.getString("OK", 2131165993), new DialogInterface.OnClickListener()
              {
                public void onClick(DialogInterface paramDialogInterface, int paramInt)
                {
                  ConnectionsManager.getInstance().switchBackend();
                }
              });
              paramAdapterView.setNegativeButton(LocaleController.getString("Cancel", 2131165374), null);
              SettingsActivity.this.showDialog(paramAdapterView.create());
              return;
            }
            if (paramInt == SettingsActivity.this.telegramFaqRow)
            {
              Browser.openUrl(SettingsActivity.this.getParentActivity(), LocaleController.getString("TelegramFaqUrl", 2131166247));
              return;
            }
            if (paramInt == SettingsActivity.this.privacyPolicyRow)
            {
              Browser.openUrl(SettingsActivity.this.getParentActivity(), LocaleController.getString("PrivacyPolicyUrl", 2131166076));
              return;
            }
            if (paramInt == SettingsActivity.this.contactsReimportRow)
              continue;
            Object localObject2;
            Object localObject3;
            if (paramInt == SettingsActivity.this.contactsSortRow)
            {
              if (SettingsActivity.this.getParentActivity() == null)
                continue;
              paramAdapterView = new AlertDialog.Builder(SettingsActivity.this.getParentActivity());
              paramAdapterView.setTitle(LocaleController.getString("SortBy", 2131166220));
              paramView = LocaleController.getString("Default", 2131165531);
              localObject1 = LocaleController.getString("SortFirstName", 2131166221);
              localObject2 = LocaleController.getString("SortLastName", 2131166222);
              localObject3 = new DialogInterface.OnClickListener()
              {
                public void onClick(DialogInterface paramDialogInterface, int paramInt)
                {
                  paramDialogInterface = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).edit();
                  paramDialogInterface.putInt("sortContactsBy", paramInt);
                  paramDialogInterface.commit();
                  if (SettingsActivity.this.listView != null)
                    SettingsActivity.this.listView.invalidateViews();
                }
              };
              paramAdapterView.setItems(new CharSequence[] { paramView, localObject1, localObject2 }, (DialogInterface.OnClickListener)localObject3);
              paramAdapterView.setNegativeButton(LocaleController.getString("Cancel", 2131165374), null);
              SettingsActivity.this.showDialog(paramAdapterView.create());
              return;
            }
            if ((paramInt != SettingsActivity.this.wifiDownloadRow) && (paramInt != SettingsActivity.this.mobileDownloadRow) && (paramInt != SettingsActivity.this.roamingDownloadRow))
              break;
            if (SettingsActivity.this.getParentActivity() == null)
              continue;
            paramView = new boolean[6];
            Object localObject1 = new BottomSheet.Builder(SettingsActivity.this.getParentActivity());
            int i = 0;
            int j;
            if (paramInt == SettingsActivity.this.mobileDownloadRow)
            {
              i = MediaController.getInstance().mobileDataDownloadMask;
              ((BottomSheet.Builder)localObject1).setApplyTopPadding(false);
              ((BottomSheet.Builder)localObject1).setApplyBottomPadding(false);
              localObject2 = new LinearLayout(SettingsActivity.this.getParentActivity());
              ((LinearLayout)localObject2).setOrientation(1);
              j = 0;
              if (j >= 6)
                break label1666;
              paramAdapterView = null;
              if (j != 0)
                break label1458;
              if ((i & 0x1) == 0)
                break label1452;
              bool1 = true;
              paramView[j] = bool1;
              paramAdapterView = LocaleController.getString("AttachPhoto", 2131165333);
            }
            do
            {
              localObject3 = new CheckBoxCell(SettingsActivity.this.getParentActivity());
              ((CheckBoxCell)localObject3).setTag(Integer.valueOf(j));
              ((CheckBoxCell)localObject3).setBackgroundResource(2130837790);
              ((LinearLayout)localObject2).addView((View)localObject3, LayoutHelper.createLinear(-1, 48));
              ((CheckBoxCell)localObject3).setText(paramAdapterView, "", paramView[j], true);
              ((CheckBoxCell)localObject3).setOnClickListener(new View.OnClickListener(paramView)
              {
                public void onClick(View paramView)
                {
                  paramView = (CheckBoxCell)paramView;
                  int i = ((Integer)paramView.getTag()).intValue();
                  boolean[] arrayOfBoolean = this.val$maskValues;
                  if (this.val$maskValues[i] == 0);
                  for (int j = 1; ; j = 0)
                  {
                    arrayOfBoolean[i] = j;
                    paramView.setChecked(this.val$maskValues[i], true);
                    return;
                  }
                }
              });
              j += 1;
              break label1284;
              if (paramInt == SettingsActivity.this.wifiDownloadRow)
              {
                i = MediaController.getInstance().wifiDownloadMask;
                break;
              }
              if (paramInt != SettingsActivity.this.roamingDownloadRow)
                break;
              i = MediaController.getInstance().roamingDownloadMask;
              break;
              bool1 = false;
              break label1308;
              if (j == 1)
              {
                if ((i & 0x2) != 0);
                for (bool1 = true; ; bool1 = false)
                {
                  paramView[j] = bool1;
                  paramAdapterView = LocaleController.getString("AttachAudio", 2131165326);
                  break;
                }
              }
              if (j == 2)
              {
                if ((i & 0x4) != 0);
                for (bool1 = true; ; bool1 = false)
                {
                  paramView[j] = bool1;
                  paramAdapterView = LocaleController.getString("AttachVideo", 2131165335);
                  break;
                }
              }
              if (j == 3)
              {
                if ((i & 0x8) != 0);
                for (bool1 = true; ; bool1 = false)
                {
                  paramView[j] = bool1;
                  paramAdapterView = LocaleController.getString("AttachDocument", 2131165329);
                  break;
                }
              }
              if (j != 4)
                continue;
              if ((i & 0x10) != 0);
              for (bool1 = true; ; bool1 = false)
              {
                paramView[j] = bool1;
                paramAdapterView = LocaleController.getString("AttachMusic", 2131165332);
                break;
              }
            }
            while (j != 5);
            if ((i & 0x20) != 0);
            for (boolean bool1 = true; ; bool1 = false)
            {
              paramView[j] = bool1;
              paramAdapterView = LocaleController.getString("AttachGif", 2131165330);
              break;
            }
            paramAdapterView = new BottomSheet.BottomSheetCell(SettingsActivity.this.getParentActivity(), 1);
            paramAdapterView.setBackgroundResource(2130837790);
            paramAdapterView.setTextAndIcon(LocaleController.getString("Save", 2131166126).toUpperCase(), 0);
            paramAdapterView.setTextColor(-12940081);
            paramAdapterView.setOnClickListener(new View.OnClickListener(paramView, paramInt)
            {
              public void onClick(View paramView)
              {
                int k;
                try
                {
                  if (SettingsActivity.this.visibleDialog != null)
                    SettingsActivity.this.visibleDialog.dismiss();
                  k = 0;
                  j = 0;
                  while (true)
                  {
                    if (j >= 6)
                      break label149;
                    i = k;
                    if (this.val$maskValues[j] != 0)
                    {
                      if (j != 0)
                        break;
                      i = k | 0x1;
                    }
                    j += 1;
                    k = i;
                  }
                }
                catch (Exception paramView)
                {
                  while (true)
                  {
                    int j;
                    FileLog.e("tmessages", paramView);
                    continue;
                    if (j == 1)
                    {
                      i = k | 0x2;
                      continue;
                    }
                    if (j == 2)
                    {
                      i = k | 0x4;
                      continue;
                    }
                    if (j == 3)
                    {
                      i = k | 0x8;
                      continue;
                    }
                    if (j == 4)
                    {
                      i = k | 0x10;
                      continue;
                    }
                    int i = k;
                    if (j != 5)
                      continue;
                    i = k | 0x20;
                  }
                  label149: paramView = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).edit();
                  if (this.val$i != SettingsActivity.this.mobileDownloadRow)
                    break label234;
                }
                paramView.putInt("mobileDataDownloadMask", k);
                MediaController.getInstance().mobileDataDownloadMask = k;
                while (true)
                {
                  paramView.commit();
                  if (SettingsActivity.this.listView != null)
                    SettingsActivity.this.listView.invalidateViews();
                  return;
                  label234: if (this.val$i == SettingsActivity.this.wifiDownloadRow)
                  {
                    paramView.putInt("wifiDownloadMask", k);
                    MediaController.getInstance().wifiDownloadMask = k;
                    continue;
                  }
                  if (this.val$i != SettingsActivity.this.roamingDownloadRow)
                    continue;
                  paramView.putInt("roamingDownloadMask", k);
                  MediaController.getInstance().roamingDownloadMask = k;
                }
              }
            });
            ((LinearLayout)localObject2).addView(paramAdapterView, LayoutHelper.createLinear(-1, 48));
            ((BottomSheet.Builder)localObject1).setCustomView((View)localObject2);
            SettingsActivity.this.showDialog(((BottomSheet.Builder)localObject1).create());
            return;
          }
          if (paramInt == SettingsActivity.this.usernameRow)
          {
            SettingsActivity.this.presentFragment(new ChangeUsernameActivity());
            return;
          }
          if (paramInt == SettingsActivity.this.numberRow)
          {
            SettingsActivity.this.presentFragment(new ChangePhoneHelpActivity());
            return;
          }
          if (paramInt != SettingsActivity.this.stickersRow)
            continue;
          SettingsActivity.this.presentFragment(new StickersActivity());
          return;
        }
        while (paramInt != SettingsActivity.this.cacheRow);
        label581: SettingsActivity.this.presentFragment(new CacheControlActivity());
        label1284: label1308: label1452: label1458: return;
      }
    });
    this.listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
    {
      private int pressCount = 0;

      public boolean onItemLongClick(AdapterView<?> paramAdapterView, View paramView, int paramInt, long paramLong)
      {
        if (paramInt == SettingsActivity.this.versionRow)
        {
          this.pressCount += 1;
          if (this.pressCount >= 2)
          {
            paramAdapterView = new AlertDialog.Builder(SettingsActivity.this.getParentActivity());
            paramAdapterView.setTitle("Debug Menu");
            paramView = new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface paramDialogInterface, int paramInt)
              {
                if (paramInt == 0)
                  ContactsController.getInstance().forceImportContacts();
                do
                  return;
                while (paramInt != 1);
                ContactsController.getInstance().loadContacts(false, true);
              }
            };
            paramAdapterView.setItems(new CharSequence[] { "Import Contacts", "Reload Contacts" }, paramView);
            paramAdapterView.setNegativeButton(LocaleController.getString("Cancel", 2131165374), null);
            SettingsActivity.this.showDialog(paramAdapterView.create());
            return true;
          }
          try
          {
            Toast.makeText(SettingsActivity.this.getParentActivity(), "¯\\_(ツ)_/¯", 0).show();
            return true;
          }
          catch (Exception paramAdapterView)
          {
            FileLog.e("tmessages", paramAdapterView);
            return true;
          }
        }
        return false;
      }
    });
    ((FrameLayout)localObject).addView(this.actionBar);
    this.extraHeightView = new View(paramContext);
    this.extraHeightView.setPivotY(0.0F);
    this.extraHeightView.setBackgroundColor(AvatarDrawable.getProfileBackColorForId(5));
    ((FrameLayout)localObject).addView(this.extraHeightView, LayoutHelper.createFrame(-1, 88.0F));
    this.shadowView = new View(paramContext);
    this.shadowView.setBackgroundResource(2130837700);
    ((FrameLayout)localObject).addView(this.shadowView, LayoutHelper.createFrame(-1, 3.0F));
    this.avatarImage = new BackupImageView(paramContext);
    this.avatarImage.setRoundRadius(AndroidUtilities.dp(21.0F));
    this.avatarImage.setPivotX(0.0F);
    this.avatarImage.setPivotY(0.0F);
    ((FrameLayout)localObject).addView(this.avatarImage, LayoutHelper.createFrame(42, 42.0F, 51, 64.0F, 0.0F, 0.0F, 0.0F));
    this.avatarImage.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View paramView)
      {
        paramView = MessagesController.getInstance().getUser(Integer.valueOf(UserConfig.getClientUserId()));
        if ((paramView != null) && (paramView.photo != null) && (paramView.photo.photo_big != null))
        {
          PhotoViewer.getInstance().setParentActivity(SettingsActivity.this.getParentActivity());
          PhotoViewer.getInstance().openPhoto(paramView.photo.photo_big, SettingsActivity.this);
        }
      }
    });
    this.nameTextView = new TextView(paramContext);
    this.nameTextView.setTextColor(-1);
    this.nameTextView.setTextSize(1, 18.0F);
    this.nameTextView.setLines(1);
    this.nameTextView.setMaxLines(1);
    this.nameTextView.setSingleLine(true);
    this.nameTextView.setEllipsize(TextUtils.TruncateAt.END);
    this.nameTextView.setGravity(3);
    this.nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
    this.nameTextView.setPivotX(0.0F);
    this.nameTextView.setPivotY(0.0F);
    ((FrameLayout)localObject).addView(this.nameTextView, LayoutHelper.createFrame(-2, -2.0F, 51, 118.0F, 0.0F, 48.0F, 0.0F));
    this.onlineTextView = new TextView(paramContext);
    this.onlineTextView.setTextColor(AvatarDrawable.getProfileTextColorForId(5));
    this.onlineTextView.setTextSize(1, 14.0F);
    this.onlineTextView.setLines(1);
    this.onlineTextView.setMaxLines(1);
    this.onlineTextView.setSingleLine(true);
    this.onlineTextView.setEllipsize(TextUtils.TruncateAt.END);
    this.onlineTextView.setGravity(3);
    ((FrameLayout)localObject).addView(this.onlineTextView, LayoutHelper.createFrame(-2, -2.0F, 51, 118.0F, 0.0F, 48.0F, 0.0F));
    this.writeButton = new ImageView(paramContext);
    this.writeButton.setBackgroundResource(2130837694);
    this.writeButton.setImageResource(2130837688);
    this.writeButton.setScaleType(ImageView.ScaleType.CENTER);
    if (Build.VERSION.SDK_INT >= 21)
    {
      paramContext = new StateListAnimator();
      ObjectAnimator localObjectAnimator = ObjectAnimator.ofFloat(this.writeButton, "translationZ", new float[] { AndroidUtilities.dp(2.0F), AndroidUtilities.dp(4.0F) }).setDuration(200L);
      paramContext.addState(new int[] { 16842919 }, localObjectAnimator);
      localObjectAnimator = ObjectAnimator.ofFloat(this.writeButton, "translationZ", new float[] { AndroidUtilities.dp(4.0F), AndroidUtilities.dp(2.0F) }).setDuration(200L);
      paramContext.addState(new int[0], localObjectAnimator);
      this.writeButton.setStateListAnimator(paramContext);
      this.writeButton.setOutlineProvider(new ViewOutlineProvider()
      {
        @SuppressLint({"NewApi"})
        public void getOutline(View paramView, Outline paramOutline)
        {
          paramOutline.setOval(0, 0, AndroidUtilities.dp(56.0F), AndroidUtilities.dp(56.0F));
        }
      });
    }
    ((FrameLayout)localObject).addView(this.writeButton, LayoutHelper.createFrame(-2, -2.0F, 53, 0.0F, 0.0F, 16.0F, 0.0F));
    this.writeButton.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View paramView)
      {
        if (SettingsActivity.this.getParentActivity() == null);
        AlertDialog.Builder localBuilder;
        do
        {
          return;
          localBuilder = new AlertDialog.Builder(SettingsActivity.this.getParentActivity());
          TLRPC.User localUser = MessagesController.getInstance().getUser(Integer.valueOf(UserConfig.getClientUserId()));
          paramView = localUser;
          if (localUser != null)
            continue;
          paramView = UserConfig.getCurrentUser();
        }
        while (paramView == null);
        int i = 0;
        if ((paramView.photo != null) && (paramView.photo.photo_big != null) && (!(paramView.photo instanceof TLRPC.TL_userProfilePhotoEmpty)))
        {
          paramView = new CharSequence[3];
          paramView[0] = LocaleController.getString("FromCamera", 2131165674);
          paramView[1] = LocaleController.getString("FromGalley", 2131165681);
          paramView[2] = LocaleController.getString("DeletePhoto", 2131165548);
          i = 1;
        }
        while (true)
        {
          localBuilder.setItems(paramView, new DialogInterface.OnClickListener()
          {
            public void onClick(DialogInterface paramDialogInterface, int paramInt)
            {
              if (paramInt == 0)
                SettingsActivity.this.avatarUpdater.openCamera();
              do
              {
                return;
                if (paramInt != 1)
                  continue;
                SettingsActivity.this.avatarUpdater.openGallery();
                return;
              }
              while (paramInt != 2);
              MessagesController.getInstance().deleteUserPhoto(null);
            }
          });
          SettingsActivity.this.showDialog(localBuilder.create());
          return;
          paramView = new CharSequence[2];
          paramView[0] = LocaleController.getString("FromCamera", 2131165674);
          paramView[1] = LocaleController.getString("FromGalley", 2131165681);
        }
      }
    });
    needLayout();
    this.listView.setOnScrollListener(new AbsListView.OnScrollListener()
    {
      public void onScroll(AbsListView paramAbsListView, int paramInt1, int paramInt2, int paramInt3)
      {
        int i = 0;
        if (paramInt3 == 0);
        do
        {
          do
          {
            return;
            paramInt2 = 0;
            paramAbsListView = paramAbsListView.getChildAt(0);
          }
          while (paramAbsListView == null);
          if (paramInt1 != 0)
            continue;
          paramInt2 = AndroidUtilities.dp(88.0F);
          paramInt1 = i;
          if (paramAbsListView.getTop() < 0)
            paramInt1 = paramAbsListView.getTop();
          paramInt2 += paramInt1;
        }
        while (SettingsActivity.this.extraHeight == paramInt2);
        SettingsActivity.access$3702(SettingsActivity.this, paramInt2);
        SettingsActivity.this.needLayout();
      }

      public void onScrollStateChanged(AbsListView paramAbsListView, int paramInt)
      {
      }
    });
    return (View)this.fragmentView;
  }

  public void didReceivedNotification(int paramInt, Object[] paramArrayOfObject)
  {
    if (paramInt == NotificationCenter.updateInterfaces)
    {
      paramInt = ((Integer)paramArrayOfObject[0]).intValue();
      if (((paramInt & 0x2) != 0) || ((paramInt & 0x1) != 0))
        updateUserData();
    }
  }

  public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject paramMessageObject, TLRPC.FileLocation paramFileLocation, int paramInt)
  {
    if (paramFileLocation == null);
    do
    {
      do
      {
        return null;
        paramMessageObject = MessagesController.getInstance().getUser(Integer.valueOf(UserConfig.getClientUserId()));
      }
      while ((paramMessageObject == null) || (paramMessageObject.photo == null) || (paramMessageObject.photo.photo_big == null));
      paramMessageObject = paramMessageObject.photo.photo_big;
    }
    while ((paramMessageObject.local_id != paramFileLocation.local_id) || (paramMessageObject.volume_id != paramFileLocation.volume_id) || (paramMessageObject.dc_id != paramFileLocation.dc_id));
    paramMessageObject = new int[2];
    this.avatarImage.getLocationInWindow(paramMessageObject);
    paramFileLocation = new PhotoViewer.PlaceProviderObject();
    paramFileLocation.viewX = paramMessageObject[0];
    paramFileLocation.viewY = (paramMessageObject[1] - AndroidUtilities.statusBarHeight);
    paramFileLocation.parentView = this.avatarImage;
    paramFileLocation.imageReceiver = this.avatarImage.getImageReceiver();
    paramFileLocation.dialogId = UserConfig.getClientUserId();
    paramFileLocation.thumb = paramFileLocation.imageReceiver.getBitmap();
    paramFileLocation.size = -1;
    paramFileLocation.radius = this.avatarImage.getImageReceiver().getRoundRadius();
    paramFileLocation.scale = this.avatarImage.getScaleX();
    return paramFileLocation;
  }

  public int getSelectedCount()
  {
    return 0;
  }

  public Bitmap getThumbForPhoto(MessageObject paramMessageObject, TLRPC.FileLocation paramFileLocation, int paramInt)
  {
    return null;
  }

  public boolean isPhotoChecked(int paramInt)
  {
    return false;
  }

  public void onActivityResultFragment(int paramInt1, int paramInt2, Intent paramIntent)
  {
    this.avatarUpdater.onActivityResult(paramInt1, paramInt2, paramIntent);
  }

  public void onConfigurationChanged(Configuration paramConfiguration)
  {
    super.onConfigurationChanged(paramConfiguration);
    fixLayout();
  }

  protected void onDialogDismiss(Dialog paramDialog)
  {
    MediaController.getInstance().checkAutodownloadSettings();
  }

  public boolean onFragmentCreate()
  {
    super.onFragmentCreate();
    this.avatarUpdater.parentFragment = this;
    this.avatarUpdater.delegate = new AvatarUpdater.AvatarUpdaterDelegate()
    {
      public void didUploadedPhoto(TLRPC.InputFile paramInputFile, TLRPC.PhotoSize paramPhotoSize1, TLRPC.PhotoSize paramPhotoSize2)
      {
        paramPhotoSize1 = new TLRPC.TL_photos_uploadProfilePhoto();
        paramPhotoSize1.caption = "";
        paramPhotoSize1.crop = new TLRPC.TL_inputPhotoCropAuto();
        paramPhotoSize1.file = paramInputFile;
        paramPhotoSize1.geo_point = new TLRPC.TL_inputGeoPointEmpty();
        ConnectionsManager.getInstance().sendRequest(paramPhotoSize1, new RequestDelegate()
        {
          public void run(TLObject paramTLObject, TLRPC.TL_error paramTL_error)
          {
            if (paramTL_error == null)
            {
              paramTL_error = MessagesController.getInstance().getUser(Integer.valueOf(UserConfig.getClientUserId()));
              if (paramTL_error != null)
                break label174;
              paramTL_error = UserConfig.getCurrentUser();
              if (paramTL_error != null);
            }
            else
            {
              return;
            }
            MessagesController.getInstance().putUser(paramTL_error, false);
            paramTLObject = (TLRPC.TL_photos_photo)paramTLObject;
            Object localObject = paramTLObject.photo.sizes;
            TLRPC.PhotoSize localPhotoSize = FileLoader.getClosestPhotoSizeWithSize((ArrayList)localObject, 100);
            localObject = FileLoader.getClosestPhotoSizeWithSize((ArrayList)localObject, 1000);
            paramTL_error.photo = new TLRPC.TL_userProfilePhoto();
            paramTL_error.photo.photo_id = paramTLObject.photo.id;
            if (localPhotoSize != null)
              paramTL_error.photo.photo_small = localPhotoSize.location;
            if (localObject != null)
              paramTL_error.photo.photo_big = ((TLRPC.PhotoSize)localObject).location;
            while (true)
            {
              MessagesStorage.getInstance().clearUserPhotos(paramTL_error.id);
              paramTLObject = new ArrayList();
              paramTLObject.add(paramTL_error);
              MessagesStorage.getInstance().putUsersAndChats(paramTLObject, null, false, true);
              AndroidUtilities.runOnUIThread(new Runnable()
              {
                public void run()
                {
                  NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, new Object[] { Integer.valueOf(1535) });
                  NotificationCenter.getInstance().postNotificationName(NotificationCenter.mainUserInfoChanged, new Object[0]);
                  UserConfig.saveConfig(true);
                }
              });
              return;
              label174: UserConfig.setCurrentUser(paramTL_error);
              break;
              if (localPhotoSize == null)
                continue;
              paramTL_error.photo.photo_small = localPhotoSize.location;
            }
          }
        });
      }
    };
    NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
    this.rowCount = 0;
    int i = this.rowCount;
    this.rowCount = (i + 1);
    this.overscrollRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.emptyRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.numberSectionRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.numberRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.usernameRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.settingsSectionRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.settingsSectionRow2 = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.notificationRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.privacyRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.backgroundRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.languageRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.enableAnimationsRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.mediaDownloadSection = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.mediaDownloadSection2 = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.mobileDownloadRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.wifiDownloadRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.roamingDownloadRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.autoplayGifsRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.saveToGalleryRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.messagesSectionRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.messagesSectionRow2 = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.customTabsRow = i;
    if (Build.VERSION.SDK_INT >= 23)
    {
      i = this.rowCount;
      this.rowCount = (i + 1);
      this.directShareRow = i;
    }
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.textSizeRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.stickersRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.cacheRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.raiseToSpeakRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.sendByEnterRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.supportSectionRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.supportSectionRow2 = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.askQuestionRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.telegramFaqRow = i;
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.privacyPolicyRow = i;
    if (BuildVars.DEBUG_VERSION)
    {
      i = this.rowCount;
      this.rowCount = (i + 1);
      this.sendLogsRow = i;
      i = this.rowCount;
      this.rowCount = (i + 1);
      this.clearLogsRow = i;
      i = this.rowCount;
      this.rowCount = (i + 1);
      this.switchBackendButtonRow = i;
    }
    i = this.rowCount;
    this.rowCount = (i + 1);
    this.versionRow = i;
    MessagesController.getInstance().loadFullUser(UserConfig.getCurrentUser(), this.classGuid, true);
    return true;
  }

  public void onFragmentDestroy()
  {
    super.onFragmentDestroy();
    if (this.avatarImage != null)
      this.avatarImage.setImageDrawable(null);
    MessagesController.getInstance().cancelLoadFullUser(UserConfig.getClientUserId());
    NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
    this.avatarUpdater.clear();
  }

  public void onResume()
  {
    super.onResume();
    if (this.listAdapter != null)
      this.listAdapter.notifyDataSetChanged();
    updateUserData();
    fixLayout();
  }

  public void restoreSelfArgs(Bundle paramBundle)
  {
    if (this.avatarUpdater != null)
      this.avatarUpdater.currentPicturePath = paramBundle.getString("path");
  }

  public void saveSelfArgs(Bundle paramBundle)
  {
    if ((this.avatarUpdater != null) && (this.avatarUpdater.currentPicturePath != null))
      paramBundle.putString("path", this.avatarUpdater.currentPicturePath);
  }

  public void sendButtonPressed(int paramInt)
  {
  }

  public void setPhotoChecked(int paramInt)
  {
  }

  public void updatePhotoAtIndex(int paramInt)
  {
  }

  public void willHidePhotoViewer()
  {
    this.avatarImage.getImageReceiver().setVisible(true, true);
  }

  public void willSwitchFromPhoto(MessageObject paramMessageObject, TLRPC.FileLocation paramFileLocation, int paramInt)
  {
  }

  private static class LinkMovementMethodMy extends LinkMovementMethod
  {
    public boolean onTouchEvent(@NonNull TextView paramTextView, @NonNull Spannable paramSpannable, @NonNull MotionEvent paramMotionEvent)
    {
      try
      {
        boolean bool = super.onTouchEvent(paramTextView, paramSpannable, paramMotionEvent);
        return bool;
      }
      catch (Exception paramTextView)
      {
        FileLog.e("tmessages", paramTextView);
      }
      return false;
    }
  }

  private class ListAdapter extends BaseFragmentAdapter
  {
    private Context mContext;

    public ListAdapter(Context arg2)
    {
      Object localObject;
      this.mContext = localObject;
    }

    public boolean areAllItemsEnabled()
    {
      return false;
    }

    public int getCount()
    {
      return SettingsActivity.this.rowCount;
    }

    public Object getItem(int paramInt)
    {
      return null;
    }

    public long getItemId(int paramInt)
    {
      return paramInt;
    }

    public int getItemViewType(int paramInt)
    {
      int j = 2;
      int i;
      if ((paramInt == SettingsActivity.this.emptyRow) || (paramInt == SettingsActivity.this.overscrollRow))
        i = 0;
      do
      {
        do
        {
          do
          {
            do
            {
              do
              {
                do
                {
                  do
                  {
                    do
                    {
                      do
                      {
                        do
                        {
                          do
                          {
                            do
                            {
                              do
                              {
                                do
                                {
                                  do
                                  {
                                    do
                                    {
                                      return i;
                                      if ((paramInt == SettingsActivity.this.settingsSectionRow) || (paramInt == SettingsActivity.this.supportSectionRow) || (paramInt == SettingsActivity.this.messagesSectionRow) || (paramInt == SettingsActivity.this.mediaDownloadSection) || (paramInt == SettingsActivity.this.contactsSectionRow))
                                        return 1;
                                      if ((paramInt == SettingsActivity.this.enableAnimationsRow) || (paramInt == SettingsActivity.this.sendByEnterRow) || (paramInt == SettingsActivity.this.saveToGalleryRow) || (paramInt == SettingsActivity.this.autoplayGifsRow) || (paramInt == SettingsActivity.this.raiseToSpeakRow) || (paramInt == SettingsActivity.this.customTabsRow) || (paramInt == SettingsActivity.this.directShareRow))
                                        return 3;
                                      i = j;
                                    }
                                    while (paramInt == SettingsActivity.this.notificationRow);
                                    i = j;
                                  }
                                  while (paramInt == SettingsActivity.this.backgroundRow);
                                  i = j;
                                }
                                while (paramInt == SettingsActivity.this.askQuestionRow);
                                i = j;
                              }
                              while (paramInt == SettingsActivity.this.sendLogsRow);
                              i = j;
                            }
                            while (paramInt == SettingsActivity.this.privacyRow);
                            i = j;
                          }
                          while (paramInt == SettingsActivity.this.clearLogsRow);
                          i = j;
                        }
                        while (paramInt == SettingsActivity.this.switchBackendButtonRow);
                        i = j;
                      }
                      while (paramInt == SettingsActivity.this.telegramFaqRow);
                      i = j;
                    }
                    while (paramInt == SettingsActivity.this.contactsReimportRow);
                    i = j;
                  }
                  while (paramInt == SettingsActivity.this.textSizeRow);
                  i = j;
                }
                while (paramInt == SettingsActivity.this.languageRow);
                i = j;
              }
              while (paramInt == SettingsActivity.this.contactsSortRow);
              i = j;
            }
            while (paramInt == SettingsActivity.this.stickersRow);
            i = j;
          }
          while (paramInt == SettingsActivity.this.cacheRow);
          i = j;
        }
        while (paramInt == SettingsActivity.this.privacyPolicyRow);
        if (paramInt == SettingsActivity.this.versionRow)
          return 5;
        if ((paramInt == SettingsActivity.this.wifiDownloadRow) || (paramInt == SettingsActivity.this.mobileDownloadRow) || (paramInt == SettingsActivity.this.roamingDownloadRow) || (paramInt == SettingsActivity.this.numberRow) || (paramInt == SettingsActivity.this.usernameRow))
          return 6;
        if ((paramInt == SettingsActivity.this.settingsSectionRow2) || (paramInt == SettingsActivity.this.messagesSectionRow2) || (paramInt == SettingsActivity.this.supportSectionRow2) || (paramInt == SettingsActivity.this.numberSectionRow))
          break;
        i = j;
      }
      while (paramInt != SettingsActivity.this.mediaDownloadSection2);
      return 4;
    }

    public View getView(int paramInt, View paramView, ViewGroup paramViewGroup)
    {
      int i = getItemViewType(paramInt);
      if (i == 0)
      {
        paramViewGroup = paramView;
        if (paramView == null)
          paramViewGroup = new EmptyCell(this.mContext);
        if (paramInt == SettingsActivity.this.overscrollRow)
          ((EmptyCell)paramViewGroup).setHeight(AndroidUtilities.dp(88.0F));
      }
      Object localObject1;
      Object localObject2;
      while (true)
      {
        return paramViewGroup;
        ((EmptyCell)paramViewGroup).setHeight(AndroidUtilities.dp(16.0F));
        return paramViewGroup;
        if (i == 1)
        {
          paramViewGroup = paramView;
          if (paramView == null)
            return new ShadowSectionCell(this.mContext);
        }
        if (i == 2)
        {
          localObject1 = paramView;
          if (paramView == null)
            localObject1 = new TextSettingsCell(this.mContext);
          localObject2 = (TextSettingsCell)localObject1;
          if (paramInt == SettingsActivity.this.textSizeRow)
          {
            paramView = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
            if (AndroidUtilities.isTablet());
            for (paramInt = 18; ; paramInt = 16)
            {
              paramInt = paramView.getInt("fons_size", paramInt);
              ((TextSettingsCell)localObject2).setTextAndValue(LocaleController.getString("TextSize", 2131166251), String.format("%d", new Object[] { Integer.valueOf(paramInt) }), true);
              return localObject1;
            }
          }
          if (paramInt == SettingsActivity.this.languageRow)
          {
            ((TextSettingsCell)localObject2).setTextAndValue(LocaleController.getString("Language", 2131165747), LocaleController.getCurrentLanguageName(), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.contactsSortRow)
          {
            paramInt = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).getInt("sortContactsBy", 0);
            if (paramInt == 0)
              paramView = LocaleController.getString("Default", 2131165531);
            while (true)
            {
              ((TextSettingsCell)localObject2).setTextAndValue(LocaleController.getString("SortBy", 2131166220), paramView, true);
              return localObject1;
              if (paramInt == 1)
              {
                paramView = LocaleController.getString("FirstName", 2131166221);
                continue;
              }
              paramView = LocaleController.getString("LastName", 2131166222);
            }
          }
          if (paramInt == SettingsActivity.this.notificationRow)
          {
            ((TextSettingsCell)localObject2).setText(LocaleController.getString("NotificationsAndSounds", 2131165980), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.backgroundRow)
          {
            ((TextSettingsCell)localObject2).setText(LocaleController.getString("ChatBackground", 2131165474), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.sendLogsRow)
          {
            ((TextSettingsCell)localObject2).setText("Send Logs", true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.clearLogsRow)
          {
            ((TextSettingsCell)localObject2).setText("Clear Logs", true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.askQuestionRow)
          {
            ((TextSettingsCell)localObject2).setText(LocaleController.getString("AskAQuestion", 2131165323), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.privacyRow)
          {
            ((TextSettingsCell)localObject2).setText(LocaleController.getString("PrivacySettings", 2131166077), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.switchBackendButtonRow)
          {
            ((TextSettingsCell)localObject2).setText("Switch Backend", true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.telegramFaqRow)
          {
            ((TextSettingsCell)localObject2).setText(LocaleController.getString("TelegramFAQ", 2131166246), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.contactsReimportRow)
          {
            ((TextSettingsCell)localObject2).setText(LocaleController.getString("ImportContacts", 2131165710), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.stickersRow)
          {
            ((TextSettingsCell)localObject2).setText(LocaleController.getString("Stickers", 2131166228), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.cacheRow)
          {
            ((TextSettingsCell)localObject2).setText(LocaleController.getString("CacheSettings", 2131165369), true);
            return localObject1;
          }
          paramViewGroup = (ViewGroup)localObject1;
          if (paramInt != SettingsActivity.this.privacyPolicyRow)
            continue;
          ((TextSettingsCell)localObject2).setText(LocaleController.getString("PrivacyPolicy", 2131166075), true);
          return localObject1;
        }
        if (i == 3)
        {
          localObject1 = paramView;
          if (paramView == null)
            localObject1 = new TextCheckCell(this.mContext);
          paramView = (TextCheckCell)localObject1;
          paramViewGroup = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
          if (paramInt == SettingsActivity.this.enableAnimationsRow)
          {
            paramView.setTextAndCheck(LocaleController.getString("EnableAnimations", 2131165574), paramViewGroup.getBoolean("view_animations", true), false);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.sendByEnterRow)
          {
            paramView.setTextAndCheck(LocaleController.getString("SendByEnter", 2131166155), paramViewGroup.getBoolean("send_by_enter", false), false);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.saveToGalleryRow)
          {
            paramView.setTextAndCheck(LocaleController.getString("SaveToGallerySettings", 2131166130), MediaController.getInstance().canSaveToGallery(), false);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.autoplayGifsRow)
          {
            paramView.setTextAndCheck(LocaleController.getString("AutoplayGifs", 2131165344), MediaController.getInstance().canAutoplayGifs(), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.raiseToSpeakRow)
          {
            paramView.setTextAndCheck(LocaleController.getString("RaiseToSpeak", 2131166079), MediaController.getInstance().canRaiseToSpeak(), true);
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.customTabsRow)
          {
            paramView.setTextAndValueAndCheck(LocaleController.getString("ChromeCustomTabs", 2131165484), LocaleController.getString("ChromeCustomTabsInfo", 2131165485), MediaController.getInstance().canCustomTabs(), false, true);
            return localObject1;
          }
          paramViewGroup = (ViewGroup)localObject1;
          if (paramInt != SettingsActivity.this.directShareRow)
            continue;
          paramView.setTextAndValueAndCheck(LocaleController.getString("DirectShare", 2131165557), LocaleController.getString("DirectShareInfo", 2131165558), MediaController.getInstance().canDirectShare(), false, true);
          return localObject1;
        }
        if (i == 4)
        {
          localObject1 = paramView;
          if (paramView == null)
            localObject1 = new HeaderCell(this.mContext);
          if (paramInt == SettingsActivity.this.settingsSectionRow2)
          {
            ((HeaderCell)localObject1).setText(LocaleController.getString("SETTINGS", 2131166123));
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.supportSectionRow2)
          {
            ((HeaderCell)localObject1).setText(LocaleController.getString("Support", 2131166242));
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.messagesSectionRow2)
          {
            ((HeaderCell)localObject1).setText(LocaleController.getString("MessagesSettings", 2131165839));
            return localObject1;
          }
          if (paramInt == SettingsActivity.this.mediaDownloadSection2)
          {
            ((HeaderCell)localObject1).setText(LocaleController.getString("AutomaticMediaDownload", 2131165343));
            return localObject1;
          }
          paramViewGroup = (ViewGroup)localObject1;
          if (paramInt != SettingsActivity.this.numberSectionRow)
            continue;
          ((HeaderCell)localObject1).setText(LocaleController.getString("Info", 2131165717));
          return localObject1;
        }
        if (i != 5)
          break;
        paramViewGroup = paramView;
        if (paramView != null)
          continue;
        paramViewGroup = new TextInfoCell(this.mContext);
      }
      while (true)
      {
        try
        {
          localObject1 = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
          paramInt = ((PackageInfo)localObject1).versionCode / 10;
          paramView = "";
          switch (((PackageInfo)localObject1).versionCode % 10)
          {
          case 0:
            ((TextInfoCell)paramViewGroup).setText(String.format(Locale.US, "Telegram for Android v%s (%d) %s", new Object[] { ((PackageInfo)localObject1).versionName, Integer.valueOf(paramInt), paramView }));
            return paramViewGroup;
          case 3:
          default:
          case 1:
          case 2:
          }
        }
        catch (Exception paramView)
        {
          FileLog.e("tmessages", paramView);
          return paramViewGroup;
        }
        paramView = "arm";
        continue;
        paramView = "universal";
        continue;
        paramViewGroup = paramView;
        if (i != 6)
          break;
        localObject1 = paramView;
        if (paramView == null)
          localObject1 = new TextDetailSettingsCell(this.mContext);
        TextDetailSettingsCell localTextDetailSettingsCell = (TextDetailSettingsCell)localObject1;
        if ((paramInt == SettingsActivity.this.mobileDownloadRow) || (paramInt == SettingsActivity.this.wifiDownloadRow) || (paramInt == SettingsActivity.this.roamingDownloadRow))
        {
          ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
          if (paramInt == SettingsActivity.this.mobileDownloadRow)
          {
            localObject2 = LocaleController.getString("WhenUsingMobileData", 2131166318);
            paramInt = MediaController.getInstance().mobileDataDownloadMask;
          }
          while (true)
          {
            paramViewGroup = "";
            if ((paramInt & 0x1) != 0)
              paramViewGroup = "" + LocaleController.getString("AttachPhoto", 2131165333);
            paramView = paramViewGroup;
            if ((paramInt & 0x2) != 0)
            {
              paramView = paramViewGroup;
              if (paramViewGroup.length() != 0)
                paramView = paramViewGroup + ", ";
              paramView = paramView + LocaleController.getString("AttachAudio", 2131165326);
            }
            paramViewGroup = paramView;
            if ((paramInt & 0x4) != 0)
            {
              paramViewGroup = paramView;
              if (paramView.length() != 0)
                paramViewGroup = paramView + ", ";
              paramViewGroup = paramViewGroup + LocaleController.getString("AttachVideo", 2131165335);
            }
            paramView = paramViewGroup;
            if ((paramInt & 0x8) != 0)
            {
              paramView = paramViewGroup;
              if (paramViewGroup.length() != 0)
                paramView = paramViewGroup + ", ";
              paramView = paramView + LocaleController.getString("AttachDocument", 2131165329);
            }
            paramViewGroup = paramView;
            if ((paramInt & 0x10) != 0)
            {
              paramViewGroup = paramView;
              if (paramView.length() != 0)
                paramViewGroup = paramView + ", ";
              paramViewGroup = paramViewGroup + LocaleController.getString("AttachMusic", 2131165332);
            }
            paramView = paramViewGroup;
            if ((paramInt & 0x20) != 0)
            {
              paramView = paramViewGroup;
              if (paramViewGroup.length() != 0)
                paramView = paramViewGroup + ", ";
              paramView = paramView + LocaleController.getString("AttachGif", 2131165330);
            }
            paramViewGroup = paramView;
            if (paramView.length() == 0)
              paramViewGroup = LocaleController.getString("NoMediaAutoDownload", 2131165891);
            localTextDetailSettingsCell.setTextAndValue((String)localObject2, paramViewGroup, true);
            return localObject1;
            if (paramInt == SettingsActivity.this.wifiDownloadRow)
            {
              localObject2 = LocaleController.getString("WhenConnectedOnWiFi", 2131166316);
              paramInt = MediaController.getInstance().wifiDownloadMask;
              continue;
            }
            localObject2 = LocaleController.getString("WhenRoaming", 2131166317);
            paramInt = MediaController.getInstance().roamingDownloadMask;
          }
        }
        if (paramInt == SettingsActivity.this.numberRow)
        {
          paramView = UserConfig.getCurrentUser();
          if ((paramView != null) && (paramView.phone != null) && (paramView.phone.length() != 0));
          for (paramView = PhoneFormat.getInstance().format("+" + paramView.phone); ; paramView = LocaleController.getString("NumberUnknown", 2131165992))
          {
            localTextDetailSettingsCell.setTextAndValue(paramView, LocaleController.getString("Phone", 2131166043), true);
            return localObject1;
          }
        }
        paramViewGroup = (ViewGroup)localObject1;
        if (paramInt != SettingsActivity.this.usernameRow)
          break;
        paramView = UserConfig.getCurrentUser();
        if ((paramView != null) && (paramView.username != null) && (paramView.username.length() != 0));
        for (paramView = "@" + paramView.username; ; paramView = LocaleController.getString("UsernameEmpty", 2131166289))
        {
          localTextDetailSettingsCell.setTextAndValue(paramView, LocaleController.getString("Username", 2131166286), false);
          return localObject1;
        }
        continue;
        paramView = "arm-v7a";
        continue;
        paramView = "x86";
      }
    }

    public int getViewTypeCount()
    {
      return 7;
    }

    public boolean hasStableIds()
    {
      return false;
    }

    public boolean isEmpty()
    {
      return false;
    }

    public boolean isEnabled(int paramInt)
    {
      return (paramInt == SettingsActivity.this.textSizeRow) || (paramInt == SettingsActivity.this.enableAnimationsRow) || (paramInt == SettingsActivity.this.notificationRow) || (paramInt == SettingsActivity.this.backgroundRow) || (paramInt == SettingsActivity.this.numberRow) || (paramInt == SettingsActivity.this.askQuestionRow) || (paramInt == SettingsActivity.this.sendLogsRow) || (paramInt == SettingsActivity.this.sendByEnterRow) || (paramInt == SettingsActivity.this.autoplayGifsRow) || (paramInt == SettingsActivity.this.privacyRow) || (paramInt == SettingsActivity.this.wifiDownloadRow) || (paramInt == SettingsActivity.this.mobileDownloadRow) || (paramInt == SettingsActivity.this.clearLogsRow) || (paramInt == SettingsActivity.this.roamingDownloadRow) || (paramInt == SettingsActivity.this.languageRow) || (paramInt == SettingsActivity.this.usernameRow) || (paramInt == SettingsActivity.this.switchBackendButtonRow) || (paramInt == SettingsActivity.this.telegramFaqRow) || (paramInt == SettingsActivity.this.contactsSortRow) || (paramInt == SettingsActivity.this.contactsReimportRow) || (paramInt == SettingsActivity.this.saveToGalleryRow) || (paramInt == SettingsActivity.this.stickersRow) || (paramInt == SettingsActivity.this.cacheRow) || (paramInt == SettingsActivity.this.raiseToSpeakRow) || (paramInt == SettingsActivity.this.privacyPolicyRow) || (paramInt == SettingsActivity.this.customTabsRow) || (paramInt == SettingsActivity.this.directShareRow) || (paramInt == SettingsActivity.this.versionRow);
    }
  }
}


*/