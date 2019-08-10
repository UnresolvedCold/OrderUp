package com.schwifty.qrcodescan.decode;

import com.google.zxing.Result;

public interface DecodeImageCallback {

    void decodeSucceed(Result result);

    void decodeFail(int type, String reason);
}
