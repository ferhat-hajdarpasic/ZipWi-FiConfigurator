package com.whitespider.wificonfig.zip.zipwi_ficonfigurator.command;

/**
 * Created by ferhat on 5/9/2016.
 */
public class ZipReadProductInfoCommand extends MotionCommand {
    private static final String TAG = ZipReadProductInfoCommand.class.getSimpleName();

    @Override
    public byte[] getBytes() {
        return new byte[] { (byte)'V', 2  };
    }
}
