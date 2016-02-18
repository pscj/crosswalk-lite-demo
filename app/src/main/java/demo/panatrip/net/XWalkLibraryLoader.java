package demo.panatrip.net;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import junit.framework.Assert;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import SevenZip.Compression.LZMA.Decoder;

/**
 * Created by pscj on 2016-02-17.
 */
public class XWalkLibraryLoader {

    private static final String[] MANDATORY_LIBRARIES = new String[]{"libxwalkcore.so"};
    private static final String TAG = "XWalkLibraryLoader";

    public interface DecompressListener {
        void onDecompressStarted();

        void onDecompressCancelled();

        void onDecompressCompleted();
    }

    public interface DownloadListener {
        void onDownloadStarted();

        void onDownloadUpdated(int var1);

        void onDownloadCancelled();

        void onDownloadCompleted(Uri var1);

        void onDownloadFailed(int var1, int var2);
    }

    public static void startDecompress(XWalkLibraryLoader.DecompressListener listener, Context context) {
        (new XWalkLibraryLoader.DecompressTask(listener, context)).execute(new Void[0]);
    }
    public static void startDownload(XWalkLibraryLoader.DownloadListener listener, Context context, String url) {
        (new XWalkLibraryLoader.DownloadTask(listener, context, url)).execute(new Void[0]);
    }

    public static boolean libIsReady(Context context){
        File soDir = context.getDir("xwalkcore", Context.MODE_PRIVATE);
        if(!soDir.exists()){
            return false;
        }
        File soFile = new File(soDir, "libxwalkcore.so");
        if(soFile.exists()){
            return true;
        }
        return false;
    }
    private static boolean  decompressLibrary(Context context) {
        String libDir = context.getDir("xwalkcore", 0).toString();
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + "libxwalkcore.so.armeabi_v7a");
        long start = System.currentTimeMillis();
        boolean success = decompress(context, file);
        long end = System.currentTimeMillis();
        Log.d("XWalkLib", "Decompress library cost: " + (end - start) + " milliseconds.");
        if(success) {
            setLocalVersion(context, 5);
        }
        return success;
    }

    private static void setLocalVersion(Context context, int version) {
        SharedPreferences sp = context.getSharedPreferences("libxwalkcore", 0);
        sp.edit().putInt("version", version).apply();
    }

    /**
     * 解压文件流，并放到默认的目录
     * @param context
     * @param inputStream
     * @return
     */
    private static boolean decompress(Context context,File srcFile) {
        ReentrantLock reentrantLock = new ReentrantLock();
        File libDir = context.getDir("xwalkcore", Context.MODE_PRIVATE);
        if (libDir.exists() && libDir.isFile()) libDir.delete();
        if (!libDir.exists() && !libDir.mkdirs()) return false;

        reentrantLock.lock();

        for (String library : MANDATORY_LIBRARIES) {
            File tmpfile = null;
            InputStream input = null;
            OutputStream output = null;

            try {
                File outfile = new File(libDir, library);
                tmpfile = new File(libDir, library + ".tmp");
                input = new BufferedInputStream(new FileInputStream(srcFile));
                output = new BufferedOutputStream(new FileOutputStream(tmpfile));
                decodeWithLzma(input, output);
                tmpfile.renameTo(outfile);
            } catch (Resources.NotFoundException e) {
                return false;
            } catch (Exception e) {
                return false;
            } finally {
                reentrantLock.unlock();

                if (output != null) {
                    try {
                        output.flush();
                    } catch (IOException e) {
                    }
                    try {
                        output.close();
                    } catch (IOException e) {
                    }
                }
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                    }
                }
                tmpfile.delete();
            }
        }
        return true;
    }
    private static void decodeWithLzma(InputStream input, OutputStream output) throws IOException {
        final int propSize = 5;
        final int outSizeLength = 8;

        byte[] properties = new byte[propSize];
        if (input.read(properties, 0, propSize) != propSize) {
            throw new EOFException("Input .lzma file is too short");
        }

        Decoder decoder = new Decoder();
        if (!decoder.SetDecoderProperties(properties)) {
            Log.w(TAG, "Incorrect stream properties");
        }

        long outSize = 0;
        for (int i = 0; i < outSizeLength; i++) {
            int v = input.read();
            if (v < 0) {
                Log.w(TAG, "Can't read stream size");
            }
            outSize |= ((long)v) << (8 * i);
        }

        if (!decoder.Code(input, output, outSize)) {
            Log.w(TAG, "Error in data stream");
        }
    }

    private static class DecompressTask extends AsyncTask<Void, Integer, Integer> {
        XWalkLibraryLoader.DecompressListener mListener;
        Context mContext;

        DecompressTask(XWalkLibraryLoader.DecompressListener listener, Context context) {
            this.mListener = listener;
            this.mContext = context;
        }

        protected void onPreExecute() {
            Log.d("XWalkLib", "DecompressTask started");
            this.mListener.onDecompressStarted();
//            this.mIsCompressed = XWalkLibraryDecompressor.isCompressed(this.mContext);
//            if(this.mIsCompressed) {
//                this.mIsDecompressed = XWalkLibraryDecompressor.isDecompressed(this.mContext);
//            }
//
//            if(this.mIsCompressed && !this.mIsDecompressed) {
//                this.mListener.onDecompressStarted();
//            }

        }

        protected Integer doInBackground(Void... params) {
            return XWalkLibraryLoader.decompressLibrary(this.mContext)?Integer.valueOf(1):Integer.valueOf(0);
        }

        protected void onCancelled(Integer result) {
            Log.d("XWalkLib", "DecompressTask cancelled");
            this.mListener.onDecompressCancelled();
        }

        protected void onPostExecute(Integer result) {
            Log.d("XWalkLib", "DecompressTask finished, " + result);
            //Assert.assertEquals(result.intValue(), 0);
            this.mListener.onDecompressCompleted();
        }
    }

    private static class DownloadTask extends AsyncTask<Void, Integer, Integer> {
        private static final int QUERY_INTERVAL_MS = 100;
        private static final int MAX_PAUSED_COUNT = 6000;
        private DownloadListener mListener;
        private Context mContext;
        private String mDownloadUrl;
        private DownloadManager mDownloadManager;
        private long mDownloadId;

        DownloadTask(XWalkLibraryLoader.DownloadListener listener, Context context, String url) {
            this.mListener = listener;
            this.mContext = context;
            this.mDownloadUrl = url;
            this.mDownloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
        }

        protected void onPreExecute() {
            Log.d("XWalkLib", "DownloadTask started, " + this.mDownloadUrl);
            String savedFile = "xwalk_download.tmp";

            try {
                String downloadDir = (new File((new URL(this.mDownloadUrl)).getPath())).getName();
                if(!downloadDir.isEmpty()) {
                    savedFile = downloadDir;
                }
            } catch (NullPointerException | MalformedURLException var5) {
                Log.e("XWalkLib", "Invalid download URL " + this.mDownloadUrl);
                this.mDownloadUrl = null;
                return;
            }

            File downloadDir1 = this.mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            File downloadFile = new File(downloadDir1, savedFile);
            if(downloadFile.isFile()) {
                downloadFile.delete();
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(this.mDownloadUrl));
            request.setDestinationInExternalFilesDir(this.mContext, Environment.DIRECTORY_DOWNLOADS, savedFile);
            if(this.isSilentDownload()) {
                request.setNotificationVisibility(2);
            }
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);

            this.mDownloadId = this.mDownloadManager.enqueue(request);
            this.mListener.onDownloadStarted();
        }

        protected Integer doInBackground(Void... params) {
            if(this.mDownloadUrl == null) {
                return Integer.valueOf(16);
            } else {
                DownloadManager.Query query = (new DownloadManager.Query()).setFilterById(new long[]{this.mDownloadId});
                int pausedCount = 0;

                while(true) {
                    if(!this.isCancelled()) {
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException var11) {
                            return Integer.valueOf(2);
                        }

                        Cursor cursor = this.mDownloadManager.query(query);
                        if(cursor == null || !cursor.moveToFirst()) {
                            continue;
                        }

                        int totalIdx = cursor.getColumnIndex("total_size");
                        int downloadIdx = cursor.getColumnIndex("bytes_so_far");
                        int totalSize = cursor.getInt(totalIdx);
                        int downloadSize = cursor.getInt(downloadIdx);
                        if(totalSize > 0) {
                            this.publishProgress(new Integer[]{Integer.valueOf(downloadSize), Integer.valueOf(totalSize)});
                        }

                        int statusIdx = cursor.getColumnIndex("status");
                        int status = cursor.getInt(statusIdx);
                        if(status != 16 && status != 8) {
                            if(status == 4) {
                                ++pausedCount;
                                if(pausedCount == 6000) {
                                    return Integer.valueOf(status);
                                }
                            }
                            continue;
                        }

                        return Integer.valueOf(status);
                    }

                    return Integer.valueOf(2);
                }
            }
        }

        protected void onProgressUpdate(Integer... progress) {
            Log.d("XWalkLib", "DownloadTask updated: " + progress[0] + "/" + progress[1]);
            int percentage = 0;
            if(progress[1].intValue() > 0) {
                percentage = (int)((double)progress[0].intValue() * 100.0D / (double)progress[1].intValue());
            }

            this.mListener.onDownloadUpdated(percentage);
        }

        protected void onCancelled(Integer result) {
            this.mDownloadManager.remove(new long[]{this.mDownloadId});
            Log.d("XWalkLib", "DownloadTask cancelled");
            this.mListener.onDownloadCancelled();
        }

        protected void onPostExecute(Integer result) {
            Log.d("XWalkLib", "DownloadTask finished, " + result);
            if(result.intValue() == 8) {
                Uri error = this.mDownloadManager.getUriForDownloadedFile(this.mDownloadId);
                this.mListener.onDownloadCompleted(error);
            } else {
                int error1 = 1000;
                if(result.intValue() == 16) {
                    DownloadManager.Query query = (new DownloadManager.Query()).setFilterById(new long[]{this.mDownloadId});
                    Cursor cursor = this.mDownloadManager.query(query);
                    if(cursor != null && cursor.moveToFirst()) {
                        int reasonIdx = cursor.getColumnIndex("reason");
                        error1 = cursor.getInt(reasonIdx);
                    }
                }

                this.mListener.onDownloadFailed(result.intValue(), error1);
            }

        }

        private boolean isSilentDownload() {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo(this.mContext.getPackageName(), 4096);
                return Arrays.asList(packageInfo.requestedPermissions).contains("android.permission.DOWNLOAD_WITHOUT_NOTIFICATION");
            } catch (NullPointerException | PackageManager.NameNotFoundException var3) {
                return false;
            }
        }
    }
}
