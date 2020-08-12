package com.usb.serial;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.usb.serial.driver.CdcAcmSerialDriver;
import com.usb.serial.driver.ProbeTable;
import com.usb.serial.driver.UsbSerialDriver;
import com.usb.serial.driver.UsbSerialPort;
import com.usb.serial.driver.UsbSerialProber;
import com.usb.serial.listener.OnUsbReadResponse;
import com.usb.serial.listener.OnUsbWriteResponse;
import com.usb.serial.util.LogWriter;
import com.usb.serial.util.SerialInputOutputManager;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

public class USBManager {
    private static String TAG=USBManager.class.getSimpleName();
    private Context context;
    public final static String INTENT_ACTION_GRANT_USB =  ".GRANT_USB";

    UsbInterface usbInterface;
    UsbEndpoint usbEndpointIN, usbEndpointOUT;
    UsbDeviceConnection usbDeviceConnection;
    public UsbDevice deviceFound = null;

    ArrayList<String> listInterface;
    ArrayList<UsbInterface> listUsbInterface;
    ArrayList<String> listEndPoint;
    ArrayList<UsbEndpoint> listUsbEndpoint;


    UsbEndpoint  mControlEndpoint = null;
    UsbEndpoint mReadEndpoint = null;
    UsbEndpoint mWriteEndpoint = null;

    UsbSerialDriver driver;
    UsbSerialPort usbSerialPort;
    SerialInputOutputManager  usbIoManager;
    private static final int WRITE_WAIT_MILLIS = 0;// infinite
    private static final int READ_WAIT_MILLIS = 0;// infinite

    public enum UsbPermission { Unknown, Requested, Granted, Denied };
    public static UsbPermission usbPermission = UsbPermission.Unknown;

//    private String tranData;
//    private int requestDatatype;
    private Thread runnableThread;
    private static final int BUFSIZ = 4096;
    private final ByteBuffer mReadBuffer = ByteBuffer.allocate(BUFSIZ);

    private LogWriter logWriter;
    private OnUsbReadResponse onUsbResponse;

    public USBManager(Context context, OnUsbReadResponse onUsbResponse){
        this.context=context;
        this.onUsbResponse=onUsbResponse;
        deviceFound = null;
        logWriter=new LogWriter();
    }

    public void disconnect(){
        if(deviceFound!=null){
            usbPermission= UsbPermission.Granted;
            deviceFound=null;
            usbInterface=null;
            usbEndpointOUT=null;
            usbEndpointIN=null;
            if(usbDeviceConnection!=null)
                usbDeviceConnection.close();
            if(usbIoManager!=null)
                usbIoManager.stop();
        }
        Log.i(TAG,"************USB Disconnected******************");
    }

    public int checkDeviceInfo() {
        deviceFound = null;
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        if (deviceList.size() > 0) {

            ArrayList<UsbDevice> deviceValue=new ArrayList<>(deviceList.values());
            if(deviceList.size()==1){
                ArrayList<String> deviceKey=new ArrayList<>(deviceList.keySet());
                String key=deviceKey.get(0);
                UsbDevice device=deviceList.get(key);
                if(device!=null){
                    int vendorId=device.getVendorId();
                    int productId= device.getProductId();
                    String name=device.getDeviceName();
                    String deviceLog;
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                        String manufacturer = device.getManufacturerName();
                        String productName = device.getProductName();
                        deviceLog="Device Details :\nManufacturer : "+manufacturer+",\nProduct Name : "+productName+",\nVendorId : "+vendorId+",\nProduct Id : "+productId;
                    }else {
                        deviceLog="Device Details :\nDevice Name : " + name + ",\nVendorId : " + vendorId + ",\nProduct Id : " + productId;
                    }
                    Log.i(TAG,deviceLog);
                    logWriter.appendLog(deviceLog);
                    GetInterface(device);
                    GetEndpoint(device);

                    if(usbInterface!=null && usbEndpointIN!=null && usbEndpointOUT !=null){
                        deviceFound=device;
                       int usbStatus= startUSB();
                       if(usbStatus==0){ // connection permission requested
                           return 0;
                       }else { // connection success
                           return 1;
                       }
                    }
                    else{
                        Log.e(TAG,"****************Device interrupted***********");
                        logWriter.appendLog("***Device interrupted***");
                        return -3;
                    }
                }
                else{
                    Log.e(TAG,"****************No Devices Found***********");
                    logWriter.appendLog("***No Devices Found***");
                    return -1;
                }

            }
            else{
                Log.e(TAG,"****************More than 1 device connected***********");
                logWriter.appendLog("***More than 1 device connected***");
                return -2;
            }

        }
        else {
            Log.e(TAG,"****************No Devices Found***********");
            logWriter.appendLog("***No Devices Found***");
            Toast.makeText(context,"No Devices found",Toast.LENGTH_SHORT).show();
            return -1;
        }

    }

    private void GetInterface(UsbDevice d) {
        listInterface = new ArrayList<String>();
        listUsbInterface = new ArrayList<UsbInterface>();
        for (int i = 0; i < d.getInterfaceCount(); i++) {
            UsbInterface usbif = d.getInterface(i);
            listInterface.add(usbif.toString());
            listUsbInterface.add(usbif);
        }

        if (d.getInterfaceCount() > 0) {
            if(d.getInterfaceCount()>1)
            usbInterface = listUsbInterface.get(1);
            else usbInterface=listUsbInterface.get(0);
        } else usbInterface = null;
    }

    private void GetEndpoint(UsbDevice d) {
        int EndpointCount = usbInterface.getEndpointCount();
        listEndPoint = new ArrayList<String>();
        listUsbEndpoint = new ArrayList<UsbEndpoint>();

        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint usbEP = usbInterface.getEndpoint(i);
            listEndPoint.add(usbEP.toString());
            listUsbEndpoint.add(usbEP);
            if ((usbEP.getDirection() == UsbConstants.USB_DIR_IN) &&
                    (usbEP.getType() == UsbConstants.USB_ENDPOINT_XFER_INT)) {
                Log.i(TAG,"Found controlling endpoint");
                logWriter.appendLog("Found controlling endpoint");
                mControlEndpoint = usbEP;
            } else if ((usbEP.getDirection() == UsbConstants.USB_DIR_IN) &&
                    (usbEP.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)) {
                Log.i(TAG,"Found reading endpoint");
                logWriter.appendLog("Found reading endpoint");
                mReadEndpoint = usbEP;
            } else if ((usbEP.getDirection() == UsbConstants.USB_DIR_OUT) &&
                    (usbEP.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)) {
                Log.i(TAG,"Found writing endpoint");
                logWriter.appendLog("Found writing endpoint");
                mWriteEndpoint = usbEP;
            }
        }

        // deixar fixo para TxBlock USB
        if (EndpointCount > 0) {
            usbEndpointIN = usbInterface.getEndpoint(0);//read
            usbEndpointOUT = usbInterface.getEndpoint(1);//write
        } else {
            usbEndpointIN = null;
            usbEndpointOUT = null;
        }
    }

    public int startUSB() {
        boolean result = false;
        UsbDevice deviceToRead = deviceFound;
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        Boolean permitToRead = manager.hasPermission(deviceToRead);

        if (permitToRead) {
            usbPermission = UsbPermission.Granted;
            openDevice(deviceToRead);
            return 1;
        } else {
           /* Toast.makeText(context, "err_no_permission, permitToRead",
                    Toast.LENGTH_LONG).show();*/
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            manager.requestPermission(deviceToRead, usbPermissionIntent);
            return 0;
        }

    }

    private void openDevice(final UsbDevice device){
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            // Probe for our custom CDC devices, which use VID 0x1234
            // and PIDS 0x0001 and 0x0002.
            ProbeTable customTable = new ProbeTable();
            customTable.addProduct(device.getVendorId(), device.getProductId(), CdcAcmSerialDriver.class);

            UsbSerialProber prober = new UsbSerialProber(customTable);
            availableDrivers = prober.findAllDrivers(manager);
            if(availableDrivers.isEmpty()){
                onUsbResponse.onReadFailure("Device driver not found");
            }
        }

//        Open a connection to the first available driver.
        driver = availableDrivers.get(0);
//        usbDeviceConnection = manager.openDevice(driver.getDevice());
        if(usbDeviceConnection==null) {
            usbDeviceConnection = manager.openDevice(device);
        }else{
            usbDeviceConnection.close();
            usbDeviceConnection = manager.openDevice(device);
        }

        if (usbDeviceConnection != null && usbPermission== UsbPermission.Granted){
            usbSerialPort = driver.getPorts().get(0); // Most devices have just one port (port 0)
            try {
                usbSerialPort.open(usbDeviceConnection);
                usbSerialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                onReadData(); // Initialise device for listening
            }catch (Exception e){
                e.printStackTrace();
                onUsbResponse.onReadFailure("Exception : "+e.getMessage());
            }
        }
        else {
            onUsbResponse.onReadFailure("Error -  No open device");
        }
    }

    public boolean isDeviceReadyToTransfer(){
        if(usbPermission== USBManager.UsbPermission.Granted) {
            if (deviceFound != null) {
                if(usbDeviceConnection!=null){
                    if(usbSerialPort!=null && usbSerialPort.isOpen()){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void onWriteData(String data, OnUsbWriteResponse onUsbWriteResponse){
        if(TextUtils.isEmpty(data)) {
            String response="Error -  Invalid write data";
            logWriter.appendLog(response);
            onUsbWriteResponse.onWriteFailure(response);
        }
        else if(usbSerialPort!=null && usbSerialPort.isOpen()){
            Log.i(TAG,"onWriteData - usbSerialPort Open");
            try {
                int writeBytes = usbSerialPort.write(data.getBytes(), WRITE_WAIT_MILLIS);
                Log.i(TAG, "Actual number of bytes written : " + writeBytes);
                logWriter.appendLog("Actual number of bytes written : " + writeBytes);
                if (writeBytes > 0) {
                    logWriter.appendLog("Request Sent : " + data);
                    Log.i(TAG, "******************** Write Data Success ******************");
                    onUsbWriteResponse.onWriteSuccess(data);
                }else{
                    String response="Write data Failure - write bytes : " + writeBytes;
                    logWriter.appendLog(response);
                    onUsbWriteResponse.onWriteFailure(response);
                }
            }catch (Exception e){
                e.printStackTrace();
                String response="Write Exception : "+e.getMessage();
                logWriter.appendLog(response);
                onUsbWriteResponse.onWriteFailure(response);
            }
        }else{
            Log.e(TAG,"onWriteData - usbSerialPort not Open");
            String response="Write Error - No open device";
            logWriter.appendLog(response);
            onUsbWriteResponse.onWriteFailure(response);
        }
    }

    public void onReadData(){
        Log.i(TAG,"onReadData - called");
        if(usbSerialPort!=null && usbSerialPort.isOpen()){
            Log.i(TAG,"onReadData - usbSerialPort Open");
            runnableThread= new Thread(new Runnable() {
                @Override
                public void run() {
                    usbIoManager=new SerialInputOutputManager(usbSerialPort);
                    usbIoManager.setReadTimeout(READ_WAIT_MILLIS);
                    usbIoManager.setListener(new SerialInputOutputManager.Listener() {
                        @Override
                        public void onNewData(byte[] data) {
                            Log.i(TAG,"************Read Data Received****************");
                            String strData = new String(data);
                            strData=cleanTextContent(strData);
                            onProcessResponse(strData);
                        }

                        @Override
                        public void onRunError(Exception e) {
                            Log.e(TAG,"onRunError : "+e.getMessage());
                            String error=e.getMessage();
                            logWriter.appendLog("onRunError : "+error);
                        }

                    });
                    Executors.newSingleThreadExecutor().submit(usbIoManager);
                    logWriter.appendLog("***Waiting for Response***");
                }
            });
            runnableThread.start();
        }else{
            Log.e(TAG,"onReadData - usbSerialPort not Open");
            onUsbResponse.onReadFailure("Error -  No open device");
        }
    }

    private void onProcessResponse(String data){
        if(TextUtils.isEmpty(data)){
            Log.i(TAG,"Response : "+data);
            logWriter.appendLog("Response : "+data);
            return;
        }else{
                logWriter.appendLog("Raw Response length : "+data.length());
                logWriter.appendLog("Raw Response : "+data);
                if(data.length()>1 && data.contains("{") && data.contains("}")){
                    try {
                        JSONObject jsonData = new JSONObject(data);
                        String requestID=jsonData.has("requestID")?jsonData.getString("requestID"):null;
                        String responseMsg=jsonData.has("responseMsg")?jsonData.getString("responseMsg"):null;
                        String responsecode=jsonData.has("responsecode")?jsonData.getString("responsecode"):null;
                        String responseCode=jsonData.has("responseCode")?jsonData.getString("responseCode"):null;
                        String rrnnumber=jsonData.has("rrnnumber")?jsonData.getString("rrnnumber"):null;
                        String transactionID=jsonData.has("transactionID")?jsonData.getString("transactionID"):null;

                        onUsbResponse.onReadSuccess(jsonData.toString());

                        /*try {
                            if(usbSerialPort!=null && usbSerialPort.isOpen())
                                usbSerialPort.close();
                            if(usbIoManager!=null)
                                usbIoManager.stop();
                        }catch (Exception e){
                            e.printStackTrace();
                            logWriter.appendLog("usbSerialPort close Exception : "+e.getMessage());
                        }*/

                    }catch (Exception e){
                        e.printStackTrace();
                        Log.e(TAG,"Error data : "+data);
                        Log.e(TAG,"Exception : "+e.getMessage());
                        logWriter.appendLog("Error data : "+data);
                        logWriter.appendLog("Exception : "+e.getMessage());
                    }
                }else{
                    Log.i(TAG,"non JSON data received : "+data);
                    logWriter.appendLog("non JSON data received : "+data);
                }
        }
    }

    private static String cleanTextContent(String text)
    {
        // strips off all non-ASCII characters
        text = text.replaceAll("[^\\x00-\\x7F]", "");

        // erases all the ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // removes non-printable characters from Unicode
        text = text.replaceAll("\\p{C}", "");

        return text.trim();
    }
}
