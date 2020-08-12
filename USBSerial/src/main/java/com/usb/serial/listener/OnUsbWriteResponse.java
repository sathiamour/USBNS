package com.usb.serial.listener;

public interface OnUsbWriteResponse {
    void onWriteSuccess(String data);
    void onWriteFailure(String error);
}
