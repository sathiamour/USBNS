package com.usb.serial.listener;

public interface OnUsbReadResponse {
    void onReadSuccess(String data);
    void onReadFailure(String error);
}
