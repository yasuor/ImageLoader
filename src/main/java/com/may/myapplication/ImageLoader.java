package com.may.myapplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2016/5/4.
 */
public class ImageLoader {
    private Context mContext;
    private LruCache<String, Bitmap> mLruCache;
    private DiskLruCache mDiskLruCache;
    private final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024;//磁盘最大缓存容量50M
    private final int DISK_CACHE_INDEX = 0;
    private final int IO_BUFFER_SIZE = 1024;
    private static final String TAG = "ImageLoader";
    private final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;
    private final int KEEP_ALIVE_TIME = 10;

    //初始化ThreadFactory
    private final ThreadFactory mThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, TAG + "#" + mCount.getAndIncrement());
        }
    };

    //初始化线程池
    private final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, 10, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), mThreadFactory);


    //初始化内存缓存、磁盘缓存
    public ImageLoader(Context context) {
        mContext = context.getApplicationContext();
        initLruCache();
        initDiskCache();
    }


    private void initLruCache() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };
    }

    private void initDiskCache() {
        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        try {
            mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, MAX_DISK_CACHE_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //获取缓存路径
    private File getDiskCacheDir(Context context, String fileName) {
        //检测是否有SD卡
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        String cacheDir;
        if (externalStorageAvailable) {
            //注意添加写SD卡的权限，否则空指针异常
            cacheDir = context.getExternalCacheDir().getPath();
        } else {
            cacheDir = context.getCacheDir().getPath();//手机内存缓存
        }
        return new File(cacheDir, fileName);
    }


    //高效加载Bitmap,压缩图片资源(从资源加载)
    public static Bitmap decodeBitmapFromResource(Resources resources, int resId, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resId, options);
    }

    //从文件输入流的描述加载
    public static Bitmap decodeBitmapFromFileDescriptor(FileDescriptor fd, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }

    //从网络字节流加载
    public static Bitmap decodeBitmapFromInputStream(InputStream inputStream, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream,null,options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(inputStream,null,options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halHeight = height / 2;
            final int halWidth = width / 2;
            while ((halHeight / inSampleSize >= reqHeight) &&
                    (halWidth / inSampleSize >= reqWidth)) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    //url转换成hashKey（MD5加密）
    public static String hashKey(String url) {
        String hashKey = null;
        try {
            MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            hashKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashKey;
    }

    private static String bytesToHexString(byte[] btyes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < btyes.length; i++) {
            String hex = Integer.toHexString(0xFF & btyes[i]);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }


    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (mLruCache.get(key) == null) {
            mLruCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key) {
        return mLruCache.get(key);
    }

    private Bitmap loadBitmapFromHttpToDiskCache(String url, int reqWidth, int reqHeight) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("cannot visit network from UI Thread");
        }
        if (mDiskLruCache == null) {
            return null;
        }
        String key = hashKey(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if (downloadUrlToStream(url, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            mDiskLruCache.flush();//重要的一步
        }

        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(TAG, "cannot visit network from UI Thread");
        }
        if (mDiskLruCache == null) {
            return null;
        }

        Bitmap bitmap = null;
        String key = hashKey(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);

        if (snapshot != null) {

            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = decodeBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight);
            if (bitmap != null) {
                addBitmapToMemoryCache(key, bitmap);
            }
        }

        return bitmap;
    }

    //网络请求图片写入磁盘的BufferedOutputStream
    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection connection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(connection.getInputStream(), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //网络加载，直接InputStream转Bitmap
    private Bitmap downloadBitmapFromHttp(String urlString, int reqWidth, int reqHeight) throws IOException {
        HttpURLConnection connection = null;
        BufferedInputStream in = null;
        URL url = null;
        Bitmap bitmap=null;
        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(connection.getInputStream(), IO_BUFFER_SIZE);
            bitmap =decodeBitmapFromInputStream(in,reqWidth,reqHeight);
            if(bitmap!=null){
                addBitmapToMemoryCache(hashKey(urlString),bitmap);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    private Bitmap loadBitmap(String url, int reqWidth, int reqHeight) {
        Bitmap bitmap = loadBitmapFromMemoryCache(url);
        if (bitmap != null) {
            return bitmap;
        }
        try {
            //磁盘查找
            bitmap = loadBitmapFromDiskCache(url, reqWidth, reqHeight);
            if (bitmap != null) {
                return bitmap;
            }
            //网络加载到磁盘，并从磁盘取出
            bitmap = loadBitmapFromHttpToDiskCache(url, reqWidth, reqHeight);
            if(bitmap != null){
                return bitmap;
            }
            //网络加载，直接InputStream转Bitmap
            bitmap =downloadBitmapFromHttp(url, reqWidth, reqHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    //核心调用的方法
    public void bindBitmap(final String url, final ImageView imageView, final int reqWidth, final int reqHeight) {
        imageView.setTag(url);
        Bitmap bitmap = loadBitmapFromMemoryCache(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(url, reqWidth, reqHeight);
                if (bitmap != null) {
                    LoaderResult result = new LoaderResult(url, imageView, bitmap);
                    Message msg = mHandler.obtainMessage(1, result);
                    mHandler.sendMessage(msg);
                }
            }
        };

        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    private Bitmap loadBitmapFromMemoryCache(String url) {
        String key = hashKey(url);
        Bitmap bitmap = getBitmapFromMemoryCache(key);
        return bitmap;
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            LoaderResult result = (LoaderResult) msg.obj;
            ImageView imageView = result.imageView;
            imageView.setImageBitmap(result.bitmap);
            String url = (String) imageView.getTag();
            if (url.equals(result.url)) {
                imageView.setImageBitmap(result.bitmap);
            } else {
                Log.e(TAG, "set image bitmap,but url has changed,ignored!");
            }
        }
    };


    class LoaderResult {
        public ImageView imageView;
        public String url;
        public Bitmap bitmap;

        public LoaderResult(String url, ImageView imageView, Bitmap bitmap) {
            this.url = url;
            this.imageView = imageView;
            this.bitmap = bitmap;
        }
    }

    public static DisplayMetrics getScreenSize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getMetrics(metrics);
        return metrics;
    }
}
