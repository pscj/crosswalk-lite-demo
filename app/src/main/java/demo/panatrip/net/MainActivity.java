package demo.panatrip.net;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.LinearLayout;

import org.xwalk.core.XWalkActivity;
import org.xwalk.core.XWalkActivityDelegate;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;

import java.io.File;

public class MainActivity extends Activity  implements XWalkLibraryLoader.DecompressListener, XWalkLibraryLoader.DownloadListener {
    private XWalkView walkView;
    private XWalkActivityDelegate mActivityDelegate;
    private ProgressDialog mypDialog = null;
    ;
    private void onXWalkReady() {
        if(mypDialog.isShowing())
            mypDialog.dismiss();
        walkView.load("https://eco-api.meiqia.com/dist/standalone.html?eid=6624", null);
    }

    public boolean isXWalkReady() {
        return this.mActivityDelegate.isXWalkReady();
    }

    public boolean isSharedMode() {
        return this.mActivityDelegate.isSharedMode();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mypDialog = new ProgressDialog(this);
        Runnable cancelCommand = new Runnable() {
            public void run() {
                finish();
            }
        };
        Runnable completeCommand = new Runnable() {
            public void run() {
                onXWalkReady();
            }
        };
        this.mActivityDelegate = new XWalkActivityDelegate(this, cancelCommand, completeCommand);
        //setContentView(R.layout.activity_main);
        if( !XWalkLibraryLoader.libIsReady(this) ){
            //启动下载
            XWalkLibraryLoader.startDownload(this, this, "http://7xkshp.com2.z0.glb.qiniucdn.com/biqu/apk/libxwalkcore.so.armeabi_v7a");
        }else{
            initWebView();
        }
    }
    private void initWebView(){

        walkView = new XWalkView(this, this);
        walkView.setResourceClient(new XWalkResourceClient(walkView ));
        //设置Ui回调
        walkView.setUIClient(new XWalkUIClientEx(walkView));
        walkView.setEnabled(false);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.addView(walkView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        setContentView(linearLayout);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        walkView.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mActivityDelegate != null){
            this.mActivityDelegate.onResume();
        }
    }
    private void showProgressDialog(String text){
        mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //mypDialog.setTitle("Google");
        mypDialog.setMessage(text);
        //mypDialog.setIcon(R.drawable.android);
        //mypDialog.setButton("Google",this);
        mypDialog.setIndeterminate(false);
        mypDialog.setCancelable(false);
        mypDialog.show();
    }

    @Override
    public void onDecompressStarted() {
        showProgressDialog("数据解压中...");
    }

    @Override
    public void onDecompressCancelled() {
        if(mypDialog.isShowing())
            mypDialog.dismiss();
    }

    @Override
    public void onDecompressCompleted() {
        initWebView();
    }

    @Override
    public void onDownloadStarted() {
        Log.i("MainActivity", "onDownloadStarted");

        showProgressDialog("数据下载中...");

    }

    @Override
    public void onDownloadUpdated(int percent) {
        mypDialog.setMessage("数据下载中 "+ percent+"%");
    }

    @Override
    public void onDownloadCancelled() {
        if(mypDialog.isShowing())
            mypDialog.dismiss();
        Log.i("MainActivity", "onDownloadCancelled");

    }

    @Override
    public void onDownloadCompleted(Uri var1) {
        if(mypDialog.isShowing())
            mypDialog.dismiss();

        //File file = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        XWalkLibraryLoader.startDecompress(this, this);
    }

    @Override
    public void onDownloadFailed(int var1, int reason) {
        if(mypDialog.isShowing())
            mypDialog.dismiss();
        Log.i("MainActivity", "onDownloadFailed reason:"+ reason);
    }
}
