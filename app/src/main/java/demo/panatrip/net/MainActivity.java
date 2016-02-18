package demo.panatrip.net;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
        walkView.load("http://www.baidu.com", null);
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
            XWalkLibraryLoader.startDownload(this, this, "http://xxx/libxwalkcore.so.armeabi_v7a");
        }else{
            this.mActivityDelegate.onResume();
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
        mypDialog.setMessage(text);     
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
    }

    @Override
    public void onDownloadCompleted(Uri var1) {
        if(mypDialog.isShowing())
            mypDialog.dismiss();

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + "libxwalkcore.so.armeabi_v7a");
        XWalkLibraryLoader.startDecompress(this, this);
    }

    @Override
    public void onDownloadFailed(int var1, int var2) {
        if(mypDialog.isShowing())
            mypDialog.dismiss();
    }
}
