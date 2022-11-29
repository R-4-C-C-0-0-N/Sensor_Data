package ca.teamgeneric.sensordata;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

public class DBHelper extends SQLiteOpenHelper {
    public static final String TAG = "DBHelper";

    SQLiteDatabase db;
    CSVWriter csvWrite;
    Cursor curCSV;
    static Handler messageHandler;
    private static DBHelper sInstance;

    public static final String DATABASE_NAME = "sensorData.db";
    private static final int DATABASE_VERSION = 1;
    public String databasePath = "";

    public static final String SESSION_TABLE_NAME = "sessions";
    public static final String SESSION_TABLE_NAME_TEMP = "sessions_temp";
    public static final String SESSION_ID = "session";
    public static final String SESSION_DATE = "date";
    public static final String SESSION_START_TIME = "starttime";

    private static final String SESSION_TABLE_STRUCTURE =
            " (" + SESSION_ID + " INTEGER PRIMARY KEY, " +
                    SESSION_DATE + " TEXT default CURRENT_TIMESTAMP, " +
                    SESSION_START_TIME + " INTEGER " +
                    ")";

    private static final String SESSION_TABLE_CREATE = "CREATE TABLE " + SESSION_TABLE_NAME + SESSION_TABLE_STRUCTURE;

    private static final String SESSION_TABLE_CREATE_TEMP = "CREATE TEMP TABLE " + SESSION_TABLE_NAME_TEMP + SESSION_TABLE_STRUCTURE;

    public static final String DATA_TABLE_NAME = "data";
    public static final String DATA_TABLE_NAME_TEMP = "data_temp";
    public static final String DATA_SESSION = "session";
    public static final String DATA_TIME = "time";
    public static final String DATA_ACCX = "accX";
    public static final String DATA_ACCY = "accY";
    public static final String DATA_ACCZ = "accZ";
    public static final String DATA_GYROX = "gyroX";
    public static final String DATA_GYROY = "gyroY";
    public static final String DATA_GYROZ = "gyroZ";
    public static final String DATA_GRAVX = "gravX";
    public static final String DATA_GRAVY = "gravY";
    public static final String DATA_GRAVZ = "gravZ";
    public static final String DATA_MAGX = "magX";
    public static final String DATA_MAGY = "magY";
    public static final String DATA_MAGZ = "magZ";

    private static final String DATA_TABLE_STRUCTURE =
            " (" + DATA_SESSION + " INTEGER, " +
                    DATA_TIME + " INTEGER, " +
                    DATA_ACCX + " REAL, " +
                    DATA_ACCY + " REAL, " +
                    DATA_ACCZ + " REAL, " +
                    DATA_GYROX + " REAL, " +
                    DATA_GYROY + " REAL, " +
                    DATA_GYROZ + " REAL, " +
                    DATA_GRAVX + " REAL, " +
                    DATA_GRAVY + " REAL, " +
                    DATA_GRAVZ + " REAL, " +
                    DATA_MAGX + " REAL, " +
                    DATA_MAGY + " REAL, " +
                    DATA_MAGZ + " REAL, " +
                    "FOREIGN KEY(" + DATA_SESSION + ") REFERENCES " + SESSION_TABLE_NAME + "(" + SESSION_ID + ")" +
                    ")";

    private static final String DATA_TABLE_CREATE = "CREATE TABLE " + DATA_TABLE_NAME + DATA_TABLE_STRUCTURE;

    private static final String DATA_TABLE_CREATE_TEMP = "CREATE TEMP TABLE " + DATA_TABLE_NAME_TEMP + DATA_TABLE_STRUCTURE;

    public static synchronized DBHelper getInstance(Context context) {

        if (sInstance == null) {
            sInstance = new DBHelper(context.getApplicationContext());
            Log.d(TAG, "New DBHelper created");
        }

        return sInstance;
    }

    public static synchronized DBHelper getInstance(Context context, Handler handler) {

        if (sInstance == null) {
            sInstance = new DBHelper(context.getApplicationContext());
            Log.d(TAG, "New DBHelper created");
        }

        messageHandler = handler;
        return sInstance;
    }

    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        databasePath = context.getDatabasePath(DATABASE_NAME).getPath();
        db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate called");

        db.execSQL(SESSION_TABLE_CREATE);
        db.execSQL(DATA_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade called");

        db.execSQL("DROP TABLE IF EXISTS " + SESSION_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_NAME);

        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        createTempTables(db);
    }

    public void closeDB(){
        if (db != null){
            Log.d(TAG, "closeDB: closing db");
            db.close();
            db = null;
        }

        if (sInstance != null){
            Log.d(TAG, "closeDB: closing DBHelper instance");
            sInstance.close();
            sInstance = null;
        }
    }

    public void createTempTables(SQLiteDatabase db){
        Log.d(TAG, "Creating Temp tables");

        db.execSQL(SESSION_TABLE_CREATE_TEMP);
        db.execSQL(DATA_TABLE_CREATE_TEMP);
    }

    public boolean checkSessionExists(Short sessionNum) throws SQLException {
        String query = "SELECT * FROM " + SESSION_TABLE_NAME + " WHERE " + SESSION_ID + "=" + sessionNum;

        Cursor c = db.rawQuery(query, null);
        boolean exists = (c.getCount() > 0);
        c.close();

        return exists;
    }

    public boolean checkSessionDataExists(Short sessionNum) throws SQLException {
        String query = "SELECT * FROM " + DATA_TABLE_NAME_TEMP + " WHERE " + DATA_SESSION + "=" + sessionNum;

        Cursor c = db.rawQuery(query, null);
        boolean exists = (c.getCount() > 0);
        c.close();

        return exists;
    }

    public void insertSessionTemp(Short sessionNum, String date, long time) throws SQLException {

        ContentValues sessionData = new ContentValues();
        sessionData.put(SESSION_ID, sessionNum);
        sessionData.put(SESSION_DATE, date);
        sessionData.put(SESSION_START_TIME, time);

        db.insertOrThrow(SESSION_TABLE_NAME_TEMP, null, sessionData);
    }

    public void setStartTime(Short sessionNum, long time) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put(SESSION_START_TIME, time);
        db.update(SESSION_TABLE_NAME_TEMP, cv, SESSION_ID + " = " + sessionNum, null);
    }

    public String getTempSessionInfo(String type) throws SQLException {
        String result = "";

        String query = "SELECT * FROM " + SESSION_TABLE_NAME_TEMP;
        Cursor c = db.rawQuery(query, null);

        if(c.getCount() > 0){
            c.moveToFirst();
            switch(type){
                case "sessionNum":
                    result = c.getString(c.getColumnIndex(SESSION_ID));
                    break;
                case "date":
                    result = c.getString(c.getColumnIndex(SESSION_DATE));
                    break;
                case "time":
                    result = c.getString(c.getColumnIndex(SESSION_START_TIME));
                    break;
            }
        }
        c.close();

        return result;
    }

    public void deleteSession() throws SQLException {
        db.delete(SESSION_TABLE_NAME_TEMP, null, null);
        db.delete(DATA_TABLE_NAME_TEMP, null,null);
    }

    public void insertDataTemp(short sessionNum, long time,
                               float[] acc,
                               float[] gyro,
                               float[] grav,
                               float[] mag) throws SQLException {

        ContentValues sensorValues = new ContentValues();

        sensorValues.put(DATA_SESSION, sessionNum);
        sensorValues.put(DATA_TIME, time);
        sensorValues.put(DATA_ACCX, acc[0]);
        sensorValues.put(DATA_ACCY, acc[1]);
        sensorValues.put(DATA_ACCZ, acc[2]);
        sensorValues.put(DATA_GYROX, gyro[0]);
        sensorValues.put(DATA_GYROY, gyro[1]);
        sensorValues.put(DATA_GYROZ, gyro[2]);
        sensorValues.put(DATA_GRAVX, grav[0]);
        sensorValues.put(DATA_GRAVY, grav[1]);
        sensorValues.put(DATA_GRAVZ, grav[2]);
        sensorValues.put(DATA_MAGX, mag[0]);
        sensorValues.put(DATA_MAGY, mag[1]);
        sensorValues.put(DATA_MAGZ, mag[2]);

        db.insertOrThrow(DATA_TABLE_NAME_TEMP, null, sensorValues);
    }

    public void copyTempData() throws SQLException{
        String copySessionSQL = "INSERT INTO " + SESSION_TABLE_NAME + " SELECT * FROM " + SESSION_TABLE_NAME_TEMP;
        db.execSQL(copySessionSQL);

        String copyDataSQL = "INSERT INTO " + DATA_TABLE_NAME + " SELECT * FROM " + DATA_TABLE_NAME_TEMP;
        db.execSQL(copyDataSQL);
    }

    public void exportTrackingSheet(File outputFile) throws SQLException, IOException {

        csvWrite = new CSVWriter(new FileWriter(outputFile));

        curCSV = db.rawQuery("SELECT * FROM " + SESSION_TABLE_NAME, null);

        csvWrite.writeNext(curCSV.getColumnNames());

        while (curCSV.moveToNext()) {

            String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2)};

            csvWrite.writeNext(arrStr);
        }

        csvWrite.close();
        curCSV.close();
    }

    public void exportSessionData(File outputFile, String sessionNum) throws IOException, SQLException {

        csvWrite = new CSVWriter(new FileWriter(outputFile));

        curCSV = db.rawQuery("SELECT * FROM " + DATA_TABLE_NAME + " WHERE session = " + sessionNum, null);

        csvWrite.writeNext(curCSV.getColumnNames());

        Integer writeCounter = 0;
        Integer numRows = curCSV.getCount();

        while (curCSV.moveToNext()) {
            writeCounter++;

            String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2),
                    curCSV.getString(3), curCSV.getString(4), curCSV.getString(5),
                    curCSV.getString(6), curCSV.getString(7), curCSV.getString(8),
                    curCSV.getString(9), curCSV.getString(10), curCSV.getString(11),
                    curCSV.getString(12), curCSV.getString(13)};

            csvWrite.writeNext(arrStr);

            if ((writeCounter % 1000) == 0){
                csvWrite.flush();
            }

            Double progressPercent = Math.ceil(((float) writeCounter / (float) numRows)*100);
            Message msg = Message.obtain();
            msg.obj = progressPercent;
            msg.setTarget(messageHandler);
            msg.sendToTarget();
        }

        csvWrite.close();
        curCSV.close();
    }
}
