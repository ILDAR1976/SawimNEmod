package ru.sawimmod.modules.history;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import protocol.Contact;
import ru.sawimmod.SawimApplication;
import ru.sawimmod.chat.Chat;
import ru.sawimmod.chat.MessData;
import ru.sawimmod.chat.message.Message;
import ru.sawimmod.chat.message.PlainMessage;
import ru.sawimmod.comm.Util;

import java.util.List;

public class HistoryStorage {

    private static final int VERSION = 5;
    private static final String CHAT_HISTORY_TABLE = "messages";
    private static final String COLUMN_ID = "_id";
    private static final String INCOMING = "incoming";
    private static final String SENDING_STATE = "sanding_state";
    private static final String AUTHOR = "author";
    private static final String MESSAGE = "msgtext";
    private static final String DATE = "date";
    private static final String ROW_DATA = "row_data";
    private static final String DB_CREATE = "create table if not exists " +
            CHAT_HISTORY_TABLE + " (" + COLUMN_ID + " integer primary key autoincrement, " +
            SENDING_STATE + " integer, " +
            INCOMING + " integer, " +
            AUTHOR + " text not null, " +
            MESSAGE + " text not null, " +
            DATE + " long not null, " +
            ROW_DATA + " integer);";

    private static final String PREFIX = "hist";

    private String uniqueUserId;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private HistoryStorage(String uniqueUserId) {
        this.uniqueUserId = uniqueUserId;
    }

    public static HistoryStorage getHistory(String uniqueUserId) {
        return new HistoryStorage(uniqueUserId);
    }

    private boolean openHistory() {
        if (null == dbHelper) {
            try {
                dbHelper = new DatabaseHelper(SawimApplication.getContext(), getDBName(), DB_CREATE, CHAT_HISTORY_TABLE, VERSION);
                db = dbHelper.getWritableDatabase();
            } catch (Exception e) {
                db = null;
                dbHelper = null;
                e.printStackTrace();
            }
        }
        return dbHelper != null;
    }

    public void closeHistory() {
        if (null != dbHelper) {
            dbHelper.close();
        }
        dbHelper = null;
    }

    public synchronized void addText(MessData md) {
        if (!openHistory()) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(SENDING_STATE, (int) md.getIconIndex());
            values.put(INCOMING, md.isIncoming() ? 0 : 1);
            values.put(AUTHOR, md.getNick());
            values.put(MESSAGE, md.getText().toString());
            values.put(DATE, md.getTime());
            values.put(ROW_DATA, md.getRowData());
            db.insert(CHAT_HISTORY_TABLE, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateText(MessData md) {
        if (!openHistory()) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(SENDING_STATE, (int) md.getIconIndex());
            String wh = AUTHOR + "='" + md.getNick() + "' AND " + MESSAGE + "='" + md.getText().toString() + "'";
            db.update(CHAT_HISTORY_TABLE, values, wh, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void deleteText(MessData md) {
        if (!openHistory()) {
            return;
        }
        try {
            String wh = AUTHOR + "='" + md.getNick() + "' AND " + MESSAGE + "='" + md.getText().toString() + "'";
            db.delete(CHAT_HISTORY_TABLE, wh, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getLastMessageTime() {
        long lastMessageTime = 0;
        openHistory();
        Cursor cursor = db.query(CHAT_HISTORY_TABLE, null, null, null, null, null, null);
        if (cursor.moveToLast()) {
            do {
                boolean isIncoming = cursor.getInt(cursor.getColumnIndex(INCOMING)) == 0;
                int sendingState = cursor.getInt(cursor.getColumnIndex(SENDING_STATE));
                short rowData = cursor.getShort(cursor.getColumnIndex(ROW_DATA));
                boolean isMessage = (rowData & MessData.PRESENCE) == 0 && (rowData & MessData.SERVICE) == 0 && (rowData & MessData.PROGRESS) == 0;
                if ((isMessage && sendingState == Message.NOTIFY_FROM_SERVER && !isIncoming) || isMessage) {
                    lastMessageTime = cursor.getLong(cursor.getColumnIndex(DATE));
                    break;
                }
            } while (cursor.moveToPrevious());
        }
        cursor.close();
        return lastMessageTime;
    }

    public MessData getLastMessage(Chat chat) {
        MessData lastMessage = null;
        openHistory();
        Cursor cursor = db.query(CHAT_HISTORY_TABLE, null, null, null, null, null, null);
        if (cursor.moveToLast()) {
            lastMessage = buildMessage(chat, cursor);
        }
        cursor.close();
        return lastMessage;
    }

    public boolean hasMessage(Chat chat, Message message, int limit, int offset) {
        final String selectCount = "select * from " + CHAT_HISTORY_TABLE
                + " order by " + COLUMN_ID
                + " ASC limit " + limit
                + " OFFSET " + offset;
        MessData mess = chat.buildMessage(message, chat.getContact().isConference() ? message.getName() : chat.getFrom(message),
                false, Chat.isHighlight(message.getProcessedText(), chat.getContact().getMyName()));
        boolean hasMessage = false;
        try {
            openHistory();
            Cursor cursor = db.rawQuery(selectCount, new String[]{});
            if (cursor.moveToLast()) {
                do {
                    MessData messFromDataBase = buildMessage(chat, cursor);
                    if (hasMessage(mess, messFromDataBase)) {
                        hasMessage = true;
                        break;
                    }
                } while (cursor.moveToPrevious());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasMessage;
    }

    private boolean hasMessage(MessData mess, MessData messFromDataBase) {
        return mess.getNick().equals(messFromDataBase.getNick())
                && mess.getText().toString().equals(messFromDataBase.getText().toString());
    }

    public void addNextListMessages(List<MessData> messDataList, final Chat chat, int limit, int offset, boolean addedAtTheBeginning) {
        final String selectCount = "select * from " + CHAT_HISTORY_TABLE
                + " order by " + COLUMN_ID
                + " DESC limit " + limit
                + " OFFSET " + offset;
        try {
            openHistory();
            Cursor cursor = db.rawQuery(selectCount, new String[]{});
            if (addedAtTheBeginning) {
                if (cursor.moveToFirst()) {
                    do {
                        MessData mess = buildMessage(chat, cursor);
                        messDataList.add(0, mess);
                    } while (cursor.moveToNext());
                }
            } else {
                if (cursor.moveToLast()) {
                    do {
                        MessData mess = buildMessage(chat, cursor);
                        messDataList.add(mess);
                    } while (cursor.moveToPrevious());
                }
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MessData buildMessage(Chat chat, Cursor cursor) {
        Contact contact = chat.getContact();
        int sendingState = cursor.getInt(cursor.getColumnIndex(SENDING_STATE));
        boolean isIncoming = cursor.getInt(cursor.getColumnIndex(INCOMING)) == 0;
        String from = cursor.getString(cursor.getColumnIndex(AUTHOR));
        String text = cursor.getString(cursor.getColumnIndex(MESSAGE));
        long date = Util.createLocalDate(Util.getLocalDateString(cursor.getLong(cursor.getColumnIndex(DATE)), false));
        short rowData = cursor.getShort(cursor.getColumnIndex(ROW_DATA));
        PlainMessage message;
        if (isIncoming) {
            message = new PlainMessage(from, chat.getProtocol(), date, text, true);
        } else {
            message = new PlainMessage(chat.getProtocol(), contact, date, text);
        }
        MessData messData;
        if (rowData == 0) {
            messData = chat.buildMessage(message, contact.isConference() ? from : chat.getFrom(message),
                    false, Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
        } else if ((rowData & MessData.ME) != 0 || (rowData & MessData.PRESENCE) != 0) {
            messData = new MessData(contact, message.getNewDate(), text, from, rowData);
        } else {
            messData = chat.buildMessage(message, contact.isConference() ? from : chat.getFrom(message),
                    rowData, Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
        }
        if (!message.isIncoming() && !messData.isMe()) {
            messData.setIconIndex((byte) sendingState);
        }
        return messData;
    }

    public int getHistorySize() {
        int num = 0;
        String selectCount = "SELECT COUNT(*) FROM " + CHAT_HISTORY_TABLE;
        openHistory();
        try {
            Cursor cursor = db.rawQuery(selectCount, new String[]{});
            if (cursor.moveToFirst()) {
                num = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return num;
    }

    private String getDBName() {
        return PREFIX + uniqueUserId.replace('@', '_').replace('.', '_').replace('/', '_');
    }

    public void removeHistory() {
        closeHistory();
        removeRMS(getDBName());
    }

    private void removeRMS(String rms) {
        SawimApplication.getContext().deleteDatabase(rms);
    }

    public void dropTable() {
        dbHelper.dropTable(db);
    }

    public void clearAll(boolean except) {
        closeHistory();
        String exceptRMS = (except ? getDBName() : null);
        String[] stores = SawimApplication.getContext().databaseList();

        for (int i = 0; i < stores.length; ++i) {
            String store = stores[i];
            if (!store.startsWith(PREFIX)) {
                continue;
            }
            if (store.equals(exceptRMS)) {
                continue;
            }
            removeRMS(store);
        }
    }

    static class DatabaseHelper extends SQLiteOpenHelper {

        private String sqlCreateEntries;
        private String tableName;

        public DatabaseHelper(Context context, String baseName, String sqlCreateEntries, String tableName, int version) {
            super(context, baseName, null, version);
            this.sqlCreateEntries = sqlCreateEntries;
            this.tableName = tableName;
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(sqlCreateEntries);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //dropTable(db);
            if (oldVersion < 5 && newVersion >= 5) {
                db.execSQL("ALTER TABLE " + tableName + " ADD " + ROW_DATA + " integer");
            }
        }

        public void dropTable(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            onCreate(db);
        }
    }
}
