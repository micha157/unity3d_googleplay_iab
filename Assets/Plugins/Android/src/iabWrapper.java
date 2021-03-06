package com.macaronics.iab;
 
import com.unity3d.player.UnityPlayer;
 
import android.app.Activity;
import android.util.Log;
import android.os.Bundle;
import android.os.Looper;
import android.content.Intent;
 
import java.util.*;
 
import com.macaronics.iab.util.*;
import com.macaronics.iab.overrideActivity;

public class iabWrapper{
    private Activity mActivity;
    private IabHelper mHelper;
    private String mEventHandler;  

    public iabWrapper(String base64EncodedPublicKey, String strEventHandler){
    mActivity =UnityPlayer.currentActivity;
    mEventHandler =strEventHandler;
 
    if (mHelper !=null){
        dispose();
    }
 
    mHelper =new IabHelper(mActivity, base64EncodedPublicKey);
    mHelper.enableDebugLogging(true);
 
    mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
        public void onIabSetupFinished(IabResult result){
            if (!result.isSuccess()){
                //回呼 Unity GameObject 之函數 "msgReceiver", 並傳送字串訊息 (JSON 格式)
                UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"code\":\"1\",\"ret\":\"false\",\"desc\":\""+result.toString()+"\"}");
                dispose();
                return;
            }
             
            //回呼 Unity GameObject 之函數 "msgReceiver", 並傳送字串訊息 (JSON 格式)
            UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"code\":\"1\",\"ret\":\"true\",\"desc\":\""+result.toString()+"\"}");
 
            //register mHelper
            //向 overrideActivity 註冊 onActivityResult 回呼函數，並將資料 Relay 給 mHelper
            overrideActivity.registerOnActivityResultCBFunc(
                new overrideActivity.cbEvent(){
                        public boolean cbEvent(int requestCode, int resultCode, Intent data)
                        {
                             
                            if (mHelper.handleActivityResult(requestCode, resultCode, data)){
                                return true;
                            }
                            else{
                                return false;
                            }
                        }
                    }
                );
            }
        });
    }


    public void dispose()
    {
        if (mHelper !=null)
        {
            mHelper.dispose();
        }
        mHelper =null;
    }

    public void purchase(String strSKU, String reqCode, String payloadString)
    {
        int intVal =Integer.parseInt(reqCode);
        if (mHelper !=null)
            mHelper.launchPurchaseFlow(mActivity, strSKU, intVal, mPurchaseFinishedListener, payloadString);
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()){
                UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"code\":\"2\",\"ret\":\"false\",\"desc\":\"\",\"sign\":\"\"}");
                return;
            }
             
            boolean ret =false;
            String result_json ="";
            String result_sign ="";
            if (purchase !=null)
            {
                ret =true;
                result_json =purchase.getOriginalJson().replace('\"', '\'');
                result_sign =purchase.getSignature();
            }
     
            UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"code\":\"2\",\"ret\":\""+ret+"\",\"desc\":\""+result_json+"\",\"sign\":\""+result_sign+"\"}");
        }
    };

    public void consume(String itemType, String jsonPurchaseInfo, String signature)
    {
        String transedJSON =jsonPurchaseInfo.replace('\'', '\"');
        if (mHelper ==null)
            return;
     
        Purchase pp =null;
        try{
            pp =new Purchase(itemType, transedJSON, signature);
        }
        catch(Exception e)
        {
            pp=null;
        }
     
        if (pp !=null)
        {
            final Purchase currpp =pp;
            mActivity.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    mHelper.consumeAsync(currpp, mConsumeFinishedListener);
                }
     
            });
        }
    }

    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (result.isSuccess()){
                Log.d("iabWrapper", "Consumption successful. Provisioning");
                UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"code\":\"3\",\"ret\":\"true\",\"desc\":\""+purchase.getOriginalJson().replace('\"', '\'')+"\",\"sign\":\""+purchase.getSignature()+"\"}");
            }
            else
            {
                UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"code\":\"3\",\"ret\":\"false\",\"desc\":\"\",\"sign\":\"\"}");
            }
        }
    };
}