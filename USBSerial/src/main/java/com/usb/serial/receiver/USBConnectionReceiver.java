package com.usb.serial.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

import com.usb.serial.USBManager;
import com.usb.serial.listener.USBConnectionListener;

public class USBConnectionReceiver extends BroadcastReceiver {
//    private static String TAG=USBConnectionReceiver.class.getSimpleName();
    private USBConnectionListener usbListener;
    public USBConnectionReceiver(){
        super();
    }

    public USBConnectionReceiver(USBConnectionListener usbListener){
        this.usbListener=usbListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.i(TAG,"+++++++++++++++++++  USBConnectionReceiver  ++++++++++++++++++++++");
        if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
            if(usbListener!=null)
            usbListener.onUsbAttached();
        }else if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)){
            if(usbListener!=null)
            usbListener.onUsbDetached();
        }else if(intent.getAction().equals(USBManager.INTENT_ACTION_GRANT_USB)) {
            if(usbListener!=null) {
             boolean isGranted=   intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        ? true : false;
             usbListener.onUsbAccess(isGranted);
            }
        }
    }
}
