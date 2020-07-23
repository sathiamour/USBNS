package com.usb.serial.util;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LogWriter {
    private String directoryName="USB NS";
    private String fileName="log.txt";
    private String directoryPath= Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+directoryName;
    private String filePath=Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+directoryName+File.separator+fileName;
    private boolean isLogEnabled=true;

    public LogWriter(){
    }

    public void setPath(String directoryname,String fileName){
        this.directoryName=directoryname;
        this.fileName=fileName;
        this.directoryPath=Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+directoryname;
        File dirFile = new File(directoryPath);
        if(!dirFile.exists() && !dirFile.isDirectory())
            dirFile. mkdirs();
        else{
            if(!this.fileName.endsWith(".txt"))this.fileName=this.fileName.concat(".txt");
            filePath=Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+directoryname+File.separator+this.fileName;
            File logFile = new File(filePath);
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void setLogEnabled(boolean isEnabled){
        this.isLogEnabled=isEnabled;
    }

    public void appendLog(String text)
    {
        if(!isLogEnabled){
            return;
        }
        File dirFile = new File(directoryPath);
        if(!dirFile.exists() && !dirFile.isDirectory()){
            try{
                dirFile.mkdirs();
            }catch (Exception e){

            }
        }
        File logFile = new File(filePath);
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            String data=getTimeStamp().concat(" : ").concat(text);
            buf.append(data);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String readFile(){
        File dirFile = new File(directoryPath);
        if(!dirFile.exists() && !dirFile.isDirectory()){
            return null;
        }
        File logFile = new File(filePath);
        if (!logFile.exists())
        {
            return null;
        }else{
            StringBuilder text = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(logFile));
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line);
//                    Log.i("Test", "text : "+text+" : end");
                    text.append('\n');
                }
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                return text!=null?text.toString():null;
            }
        }
    }


    public void clearLog(){
        File dirFile = new File(directoryPath);
        if(!dirFile.exists() && !dirFile.isDirectory()){
            return;
        }
        File logFile = new File(filePath);
        if (!logFile.exists())
        {
            return;
        }else{
            if(logFile.delete()){
                try
                {
                    logFile.createNewFile();
                }
                catch (IOException e)
                {
                }
            }
        }
    }


    private String getTimeStamp(){
        SimpleDateFormat df=new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS ");
        String date=df.format(Calendar.getInstance().getTime());
        return date;
    }

}
