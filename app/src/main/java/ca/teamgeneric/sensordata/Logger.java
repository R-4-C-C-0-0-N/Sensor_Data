package ca.teamgeneric.sensordata;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Logger{

    private File logFileDir;
    private File logFile;

    public Logger(File logFileDir){
        this.logFileDir = logFileDir;
        this.logFile = new File(logFileDir, "log.txt");
    }

    public void e(Context context, String logMessageTag, String logMessage){
        if (!Log.isLoggable(logMessageTag, Log.ERROR))
            return;

        int logResult = Log.e(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    public void e(Context context, String logMessageTag, String logMessage, Throwable throwableException){
        if (!Log.isLoggable(logMessageTag, Log.ERROR))
            return;

        int logResult = Log.e(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    public void w(Context context, String logMessageTag, String logMessage){
        if (!Log.isLoggable(logMessageTag, Log.WARN))
            return;

        int logResult = Log.w(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    public void w(Context context, String logMessageTag, String logMessage, Throwable throwableException){
        if (!Log.isLoggable(logMessageTag, Log.WARN))
            return;

        int logResult = Log.w(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    public void i(Context context, String logMessageTag, String logMessage){
        if (!Log.isLoggable(logMessageTag, Log.INFO))
            return;

        int logResult = Log.i(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    public void i(Context context, String logMessageTag, String logMessage, Throwable throwableException){
        if (!Log.isLoggable(logMessageTag, Log.INFO))
            return;

        int logResult = Log.i(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    public void v(Context context, String logMessageTag, String logMessage){
        if (!BuildConfig.DEBUG || !Log.isLoggable(logMessageTag, Log.VERBOSE))
            return;

        int logResult = Log.v(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    public void v(Context context, String logMessageTag, String logMessage, Throwable throwableException){
        if (!BuildConfig.DEBUG || !Log.isLoggable(logMessageTag, Log.VERBOSE))
            return;

        int logResult = Log.v(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    public void d(Context context, String logMessageTag, String logMessage){
        if (!BuildConfig.DEBUG || !Log.isLoggable(logMessageTag, Log.DEBUG))
            return;

        int logResult = Log.d(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    public void d(Context context,String logMessageTag, String logMessage, Throwable throwableException){
        if (!BuildConfig.DEBUG || !Log.isLoggable(logMessageTag, Log.DEBUG))
            return;

        int logResult = Log.d(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    private String getDateTimeStamp(){
        Date dateNow = Calendar.getInstance().getTime();
        return (DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH).format(dateNow));
    }

    private void logToFile(Context context, String logMessageTag, String logMessage){
        try{
            if (!logFileDir.exists()) {
                Boolean created = logFileDir.mkdirs();
            }

            if (!logFile.exists())
                logFile.createNewFile();

            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.write(String.format("%1s [%2s]: %3s\r\n", getDateTimeStamp(), logMessageTag, logMessage));
            writer.close();
            MediaScannerConnection.scanFile(context, new String[]{logFile.toString()}, null, null);
        }
        catch (IOException e){
            Log.e("Logger", "Unable to log exception to file.");
        }
    }
}