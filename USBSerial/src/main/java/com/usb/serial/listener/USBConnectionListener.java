package com.usb.serial.listener;

public interface USBConnectionListener {
    void onUsbAttached();
    void onUsbDetached();
    void onUsbAccess(boolean isGranted);
}
