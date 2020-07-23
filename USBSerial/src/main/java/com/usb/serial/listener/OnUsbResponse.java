package com.usb.serial.listener;

public interface OnUsbResponse {
    void onSuccess(String data);
    void onFailure(String error);
}
