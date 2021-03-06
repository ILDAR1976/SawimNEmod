package ru.sawimmod;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import ru.sawimmod.R;
import ru.sawimmod.activities.SawimActivity;
import ru.sawimmod.chat.ChatHistory;
import ru.sawimmod.comm.JLocale;
import ru.sawimmod.roster.RosterHelper;

import java.util.HashMap;

/**
 * Created by admin on 27.01.14.
 */
public class SawimNotification {

    private static final int VIBRA_OFF = 0;
    private static final int VIBRA_ON = 1;
    private static final int VIBRA_LOCKED_ONLY = 2;

    public static final int NOTIFY_ID = 1;
    private static final HashMap<String, Integer> idsMap = new HashMap<String, Integer>();

    public static Notification get(Context context, boolean silent) {
        int unread = ChatHistory.instance.getPersonalUnreadMessageCount(false);
        int allUnread = ChatHistory.instance.getPersonalUnreadMessageCount(true);
        CharSequence stateMsg = "";

        final int icon;
        if (0 < allUnread) {
            icon = R.drawable.ic_tray_msg;
        } else if (RosterHelper.getInstance().isConnected()) {
            icon = R.drawable.ic_tray_on;
            stateMsg = context.getText(R.string.online);
        } else {
            icon = R.drawable.ic_tray_off;
            if (RosterHelper.getInstance().isConnecting()) {
                stateMsg = context.getText(R.string.connecting);
            } else {
                stateMsg = context.getText(R.string.offline);
            }
        }

        long when = 0;
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        Intent notificationIntent = new Intent(context, SawimActivity.class);
        notificationIntent.setAction(SawimActivity.NOTIFY);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (0 < unread) {
            notification.setLights(0xff00ff00, 1000, 3000);
            if (silent) {
                if (Options.getBoolean(Options.OPTION_VIBRATION)) {
                    int dat = 70;
                    long[] pattern = {0,3 * dat, dat, dat};
                    notification.setVibrate(pattern);
                }
                String ringtone = Options.getString(Options.OPTION_MESS_RINGTONE, null);
                if (ringtone != null) {
                    notification.setSound(Uri.parse(ringtone));
                }
            }
        }
        if (0 < allUnread) {
            notification.setNumber(unread);
            stateMsg = String.format((String) context.getText(R.string.unread_messages), unread);
        }
        stateMsg = ChatHistory.instance.getLastMessage(stateMsg);
        notification.setAutoCancel(true);
        notification.setWhen(when);
        notification.setContentIntent(contentIntent);
        CharSequence lastMessageNick = ChatHistory.instance.getLastMessageNick();
        if (lastMessageNick == null) {
            notification.setContentTitle(context.getString(R.string.app_name));
        } else {
            notification.setContentTitle(context.getString(R.string.message_from) + " " + lastMessageNick);
        }
        notification.setContentText(stateMsg);
        notification.setSmallIcon(icon);
        return notification.build();
    }

    public static void alarm(String processedText) {
    }

    public static void fileProgress(String filename, int percent, String text) {
        Context context = SawimApplication.getContext();
        Intent intent = new Intent(context, SawimActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(contentIntent);
        builder.setOngoing(false);
        builder.setContentTitle(filename);
        if (percent != -1) {
            if (percent == 100) {
                builder.setTicker(JLocale.getString(R.string.sending_complete));
                builder.setSmallIcon(android.R.drawable.stat_sys_upload_done);
            } else {
                builder.setTicker(JLocale.getString(R.string.sending_file));
                builder.setSmallIcon(android.R.drawable.stat_sys_upload);
            }
            builder.setContentText(percent + "% " + text);
            builder.setAutoCancel(true);
        } else {
            builder.setSmallIcon(android.R.drawable.stat_sys_upload_done);
            builder.setContentText(text);
            builder.setTicker(text);
            builder.setAutoCancel(false);
        }
        int id = filename.hashCode();
        if (idsMap.containsKey(filename))
            id = idsMap.get(filename);
        else idsMap.put(filename, id);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(id, builder.build());
    }

    public static void captcha(String title) {
        int id = title.hashCode();
        Context context = SawimApplication.getContext();
        Intent intent = new Intent(context, SawimActivity.class);
        intent.setAction(SawimActivity.NOTIFY_CAPTCHA);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SawimActivity.NOTIFY_CAPTCHA, title);
        PendingIntent contentIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(contentIntent);
        builder.setOngoing(false);
        builder.setContentTitle(title);
        builder.setTicker(JLocale.getString(R.string.captcha));
        builder.setContentText(JLocale.getString(R.string.captcha));
        builder.setSmallIcon(android.R.drawable.stat_notify_more);
        builder.setAutoCancel(true);
        if (idsMap.containsKey(title))
            id = idsMap.get(title);
        else idsMap.put(title, id);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(id, builder.build());
    }

    /*public static void sendNotify(Context context, final String title, final String text) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        long when = 0;
        int icon = R.drawable.ic_tray_msg;
        long[] vibraPattern = {0, 500, 250, 500};
        int unread = ChatHistory.instance.getPersonalUnreadMessageCount(false);
        int allUnread = ChatHistory.instance.getPersonalUnreadMessageCount(true);
        if (0 < allUnread) {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
            CharSequence contentTitle = context.getText(R.string.notify_title);
            CharSequence contentText = null;
            Intent notificationIntent = new Intent(context, SawimActivity.class);
            notificationIntent.setAction(SawimActivity.NOTIFY);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            //Intent replyIntent = new Intent(context, SawimActivity.class);
            //replyIntent.setAction(SawimActivity.NOTIFY_REPLY);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //replyIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //PendingIntent piReply = PendingIntent.getActivity(context, 0, replyIntent, PendingIntent.FLAG_ONE_SHOT);
            //notification.setVibrate(vibraPattern);
            if (0 < unread) {
                notification.setLights(0xff00ff00, 300, 1000);
                //notification.number = unread;
                contentText = String.format((String) context.getText(R.string.unread_messages), unread);
            }
            contentText = ChatHistory.instance.getLastMessage(contentText.toString());
            notification.setWhen(when);
            notification.setDefaults(android.app.Notification.DEFAULT_ALL);
            notification.setContentIntent(contentIntent);
            notification.setContentTitle(contentTitle);
            notification.setContentText(contentText);
            notification.setSmallIcon(icon);
            //NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            //style.addLine(text).setBigContentTitle(title).setSummaryText(contentText);
            //notification.addAction(android.R.drawable.sym_action_chat, context.getText(R.string.reply), piReply);
            //notification.setStyle(style);
            notificationManager.notify(NOTIFY_ID, notification.build());
        }
    }*/

    public static void clear(String id) {
        if (idsMap.get(id) == null) return;
        ((NotificationManager) SawimApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(idsMap.get(id));
        idsMap.remove(id);
    }
}
