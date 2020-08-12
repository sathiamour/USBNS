package com.nstore.usb;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.usb.serial.USBManager;
import com.usb.serial.listener.OnUsbReadResponse;
import com.usb.serial.listener.OnUsbWriteResponse;
import com.usb.serial.listener.USBConnectionListener;
import com.usb.serial.receiver.USBConnectionReceiver;
import com.usb.serial.util.LogWriter;

import org.json.JSONObject;

import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, USBConnectionListener,OnUsbReadResponse {
    private static String TAG=MainActivity.class.getSimpleName();
    Toolbar toolbar;
    ProgressBar progressBar;
    TextView txtUsbStatus;
    EditText edtAmount,edtTranId,edtdata1,edtData2,edtData3;
    RadioGroup rg;
    RadioButton rbJson,rbXml;
    Button btnClear,btnTransfer;
    ImageView imgConnection;

    USBManager usbManager;
    boolean isConnected =false;
    boolean isTransactionInProcess=false;

    private USBConnectionReceiver usbConnectionReceiver;
    private LogWriter logWriter;

    @Override
    public void onUsbAccess(boolean isGranted) {
        if(isGranted){
            usbManager.usbPermission=USBManager.UsbPermission.Granted;
            if(usbManager.deviceFound!=null) {
                int usbStatus = usbManager.startUSB();
            }else {
                //usbManager.checkDeviceInfo();
            }
        }else{
            usbManager.usbPermission=USBManager.UsbPermission.Denied;
            //Toast.makeText(context, "USB device permission denied",Toast.LENGTH_SHORT).show();
        }
        checkConnection();
    }

    @Override
    public void onUsbAttached() {
        Log.i(TAG,"USB ATTACHED");
        int deviceInfo=  usbManager.checkDeviceInfo();
        checkConnection();
    }

    @Override
    public void onUsbDetached() {
        Log.i(TAG,"USB DETACHED");
        isConnected=false;
        txtUsbStatus.setText("Device Disconnected");
        imgConnection.setBackgroundResource(R.mipmap.ic_action_connect);
        usbManager.disconnect();
        logWriter.appendLog("Device Disconnected");
        if(isTransactionInProcess){
            logWriter.appendLog("Transaction Interrupted");
            isTransactionInProcess=false;
//            clearData();
            progressBar.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this,"Transaction Interrupted",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onReadSuccess(String data) {
        Log.i(TAG,"Response : "+data);
        isTransactionInProcess=false;
        try {
            JSONObject jsonData = new JSONObject(data);
            String requestID = jsonData.has("requestID") ? jsonData.getString("requestID") : null;
            final String responseMsg = jsonData.has("responseMsg") ? jsonData.getString("responseMsg") : null;
            String responsecode = jsonData.has("responsecode") ? jsonData.getString("responsecode") : null;
            final String responseCode = jsonData.has("responseCode") ? jsonData.getString("responseCode") : null;
            String rrnnumber = jsonData.has("rrnnumber") ? jsonData.getString("rrnnumber") : null;
            String transactionID = jsonData.has("transactionID") ? jsonData.getString("transactionID") : null;

           final StringBuilder text = new StringBuilder();
            text.append("requestID : "+requestID+"\n");
            text.append("responseMsg : "+responseMsg+"\n");
            text.append("responsecode : "+responsecode+"\n");
            text.append("responseCode : "+responseCode+"\n");
            text.append("rrnnumber : "+rrnnumber+"\n");
            text.append("transactionID : "+transactionID);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    clearData();
                    progressBar.setVisibility(View.GONE);
                    if(!TextUtils.isEmpty(responseCode) && responseCode.equalsIgnoreCase("0")){
                        Toast.makeText(MainActivity.this, "Transaction Success : "+text.toString(), Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this, responseMsg, Toast.LENGTH_SHORT).show();
                    }

                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onReadFailure(final String error) {
        logWriter.appendLog(error);
        isTransactionInProcess=false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logWriter=new LogWriter();
        usbManager=new USBManager(MainActivity.this,MainActivity.this);
        isConnected=false;
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        usbConnectionReceiver=new USBConnectionReceiver(MainActivity.this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(USBManager.INTENT_ACTION_GRANT_USB);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbConnectionReceiver,filter);
//        LocalBroadcastManager.getInstance(this).registerReceiver(usbConnectionReceiver, filter); // local receiver

        progressBar=findViewById(R.id.progressBar);
        txtUsbStatus=findViewById(R.id.textView5);
        edtAmount=findViewById(R.id.textInputEditText2);
        edtTranId=findViewById(R.id.textInputEditText);
        edtdata1=findViewById(R.id.editText2);
        edtData2=findViewById(R.id.editText4);
        edtData3=findViewById(R.id.editText5);
        rg=findViewById(R.id.id_rg);
        rbJson=findViewById(R.id.radioButton);
        rbXml=findViewById(R.id.radioButton2);
        btnClear=findViewById(R.id.button);
        btnTransfer=findViewById(R.id.button2);
        imgConnection=findViewById(R.id.ic_connection);
        btnClear.setOnClickListener(this);
        btnTransfer.setOnClickListener(this);
        imgConnection.setOnClickListener(this);
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                if(multiplePermissionsReport.areAllPermissionsGranted()) {
                    Log.i(TAG,"READ and WRITE - Permission Granted");
                    logWriter.appendLog("Log access permission granted");
                }else{
                    Log.i(TAG,"READ and WRITE - Permission not Granted");
                    Toast.makeText(MainActivity.this,"Unable to write logs - Permission denied",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                Log.i(TAG,"READ and WRITE - Permission not Granted");
                permissionToken.continuePermissionRequest();
            }
        }).check();
        logWriter.appendLog("***App Started***");

    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.button)
            clearData();
        else if(v.getId()==R.id.button2) {
            if(!isTransactionInProcess)
            setOnTransferData();
            else{
                Toast.makeText(MainActivity.this,"Please wait, Transaction is in progress",Toast.LENGTH_SHORT).show();
            }
        }else if(v.getId()==R.id.ic_connection){
            if(isConnected){ // do Disconnect
                isConnected=false;
                imgConnection.setBackgroundResource(R.mipmap.ic_action_connect);
                usbManager.disconnect();
                txtUsbStatus.setText("Device Disconnected");
                logWriter.appendLog("Device Disconnected");
                if(isTransactionInProcess){
                    isTransactionInProcess=false;
                    clearData();
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this,"Transaction Interrupted",Toast.LENGTH_SHORT).show();
                }
            }else{ // do Connect
                int deviceInfo=  usbManager.checkDeviceInfo();
                checkConnection();
            }
        }
    }

    private void checkConnection(){
        if(usbManager.deviceFound==null){ // no Usb Device
            isConnected=false;
            int deviceInfo=  usbManager.checkDeviceInfo();
            txtUsbStatus.setText("No Device");
        }else{
            if(usbManager.usbPermission== USBManager.UsbPermission.Granted)  {
                isConnected=true;
                txtUsbStatus.setText("Device Connected");
                imgConnection.setBackgroundResource(R.mipmap.ic_action_disconnect);
                logWriter.appendLog("Device Connected");
            }else if(usbManager.usbPermission== USBManager.UsbPermission.Denied){
                isConnected=false;
                txtUsbStatus.setText("Device Permission Denied");
            }else if(usbManager.usbPermission== USBManager.UsbPermission.Requested){
                isConnected=false;
                txtUsbStatus.setText("Device Connection Requested");
            }else{
                isConnected=false;
                txtUsbStatus.setText("Device Connection Unknown");
            }
        }
    }

    private void clearData(){
        edtAmount.setText("");
        edtTranId.setText("");
        edtdata1.setText("");
        edtData2.setText("");
        edtData3.setText("");
        edtAmount.requestFocus();
        edtAmount.setSelection(0);
        rbJson.setChecked(true);
    }

    private void setOnTransferData(){
        if(validateData()){
            if(isConnected){
                String amount=edtAmount.getText().toString();
                String tranId=edtTranId.getText().toString();
                String extra1=edtdata1.getText().toString();
                String extra2=edtData2.getText().toString();
                String extra3=edtData3.getText().toString();
                JSONObject jsonData= addJsonData(amount,tranId,extra1,extra2,extra3); // Convert to Json data
                if(jsonData!=null){
                    String writeData=jsonData.toString();
                    if(usbManager.isDeviceReadyToTransfer())  {
                            isTransactionInProcess=true;
                            progressBar.setVisibility(View.VISIBLE);
                            usbManager.onWriteData(writeData, new OnUsbWriteResponse() {
                                @Override
                                public void onWriteSuccess(String data) {
                                    Toast.makeText(MainActivity.this,"Transaction initiated.",Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onWriteFailure(String error) {
                                    isTransactionInProcess=false;
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(MainActivity.this,"Transaction not initiated.",Toast.LENGTH_SHORT).show();
                                }
                            });
                    }
                    else{
                        checkConnection();
                    }
                }

            }else{
                logWriter.appendLog("No Device Connected");
                Toast.makeText(this,"Check the device is connected to transfer",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validateData(){
        if(edtAmount.getText().toString().length()==0) {
            edtAmount.setError("Enter Valid Amount");
            edtAmount.requestFocus();
            edtAmount.setPressed(true);
            return false;
        }else if(edtTranId.getText().toString().length()==0) {
            edtTranId.setError("Enter Valid Transaction Id");
            edtTranId.requestFocus();
            edtTranId.setPressed(true);
            return false;
        }
        /*else if(edtdata1.getText().toString().length()==0) {
            edtdata1.setError("Enter Valid Additional Data 1");
            return false;
        }else if(edtData2.getText().toString().length()==0) {
            edtData2.setError("Enter Valid Additional Data 2");
            return false;
        }else if(edtData3.getText().toString().length()==0) {
            edtData3.setError("Enter Valid Additional Data 3");
            return false;
        }*/
        else return true;
    }

    private String addXMLData(String amount,String tranId,String extra1,String extra2,String extra3){
        if(TextUtils.isEmpty(amount) || TextUtils.isEmpty(tranId)) {
            return null;
        }
        if (TextUtils.isEmpty(extra1))extra1=" ";
        if (TextUtils.isEmpty(extra2))extra2=" ";
        if (TextUtils.isEmpty(extra3))extra3=" ";
        return "<TransactionRequest RequestID=\"01\"><TotalAmount>"+amount+"</TotalAmount><PrivateData>transactionID=" +tranId+"</PrivateData><additionalData1>"+extra1+"</additionalData1><additionalData2>"+extra2+"</additionalData2><additionalData3>"+extra3+"</additionalData3></TransactionRequest>";
    }

    private JSONObject addJsonData(String amount,String tranId,String extra1,String extra2,String extra3){
        if(TextUtils.isEmpty(amount) || TextUtils.isEmpty(tranId)) {
            return null;
        }
        if (TextUtils.isEmpty(extra1))extra1=" ";
        if (TextUtils.isEmpty(extra2))extra2=" ";
        if (TextUtils.isEmpty(extra3))extra3=" ";
        JSONObject jsonObject=new JSONObject();
        try {
            jsonObject.put("requestID", "01");
            jsonObject.put("transactionID", tranId);
            jsonObject.put("totalAmount", amount);
            jsonObject.put("additionalData1", extra1);
            jsonObject.put("additionalData2", extra2);
            jsonObject.put("additionalData3", extra3);
            return jsonObject;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.open){
            Intent i=new Intent(MainActivity.this,LogActivity.class);
            startActivity(i);
        }else if(item.getItemId()==R.id.clear){
            logWriter.clearLog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbConnectionReceiver);
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(usbConnectionReceiver);
    }
}
