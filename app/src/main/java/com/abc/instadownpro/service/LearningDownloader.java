package com.abc.instadownpro.service;

import android.text.TextUtils;

import com.abc.instadownpro.base.HttpRequestSpider;
import com.abc.instadownpro.util.DeviceUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class LearningDownloader {



    public static final String TAG = "learning";
    public static final int CODE_OK = 0;
    public static final int CODE_DOWNLOAD_FAILED = -1;
    public static final int CODE_DOWNLOAD_CANCELED = 100;

    public static final String COOKIES_HEADER = "Set-Cookie";
    public volatile static LearningDownloader sInstance;

    public static final int MAX_RETRY_TIMES = 5;

    public int THREAD_COUNT = 1;

    private volatile int mReadBytesCount = 0;

    private AtomicBoolean mInternalErrorInterupted = new AtomicBoolean(false);
    private IPowerfulDownloadCallback mCallback;

    private ConcurrentHashMap<Integer, DownloadingThread> mDownloadingTaskMap = new ConcurrentHashMap<>();

    private String mCurrentTaskId;
    private int mFilePos;

    private LearningDownloader() {
        int cpuCount = DeviceUtil.getNumberOfCPUCores() + 1;
        THREAD_COUNT = cpuCount;
    }


    public static LearningDownloader getDefault() {
        synchronized (LearningDownloader.class) {
            if (sInstance == null) {
                sInstance = new LearningDownloader();
            }
        }
        return sInstance;
    }


    public void interupted() {
       // LogUtil.e(TAG, "interrupted");
        mInternalErrorInterupted.set(true);
    }

    public void startDownload(int filePos, String pageURL, String fileUrl, String tagetPath, IPowerfulDownloadCallback callback) {
        try {

            mCallback = callback;
            mCurrentTaskId = pageURL;
            mFilePos = filePos;
          //  LogUtil.e(TAG, "startDownload:" + pageURL);
          //  LogUtil.e(TAG, "targetPath=" + tagetPath);
          //  LogUtil.v(TAG, "fileURL=" + fileUrl);
            long start = System.currentTimeMillis();
            download(filePos, fileUrl, tagetPath, THREAD_COUNT, 0, true);
            //LogUtil.e(TAG, "time = " + (System.currentTimeMillis() - start));
        } catch (OutOfMemoryError error) {
            //EventUtil.getDefault().onEvent("error", "LearingDownloader.OutOfMemory:" + error.toString());
            System.gc();
            System.gc();
            System.gc();
        }
    }

    /**
     * 下载文件
     */
    private void download(final int filePos, String fileUrl, String targetPath, int threadNum, int retryTime, boolean notifyCallback) {
        int codeStatus = CODE_OK;
        CountDownLatch latch = null;
        if (threadNum > 1) {
            latch = new CountDownLatch(threadNum);
        }
        mDownloadingTaskMap.clear();
        mInternalErrorInterupted.set(false);
        mReadBytesCount = 0;
        HttpURLConnection conn = null;
        boolean firstRequestFailed = false;
        int targetLength = 0;
        boolean acceptRange = false;
        try {
            //通过下载路径获取连接
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            //设置连接的相关属性
            conn.setRequestMethod(HttpRequestSpider.METHOD_GET);
            conn.setReadTimeout(HttpRequestSpider.CONNECTION_TIMEOUT);
//            Map<String, List<String>> headerFields = conn.getHeaderFields();
//            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
//            for(String cookie: cookiesHeader) {
//                LogUtil.v("download","cookie=" + cookie);
//            }
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
            conn.setDefaultUseCaches(false);
            conn.setUseCaches(false);
            String acceptRanges = conn.getHeaderField("Accept-Ranges");
           // LogUtil.e(TAG, "acceptRangs=" + acceptRanges);
            if (!TextUtils.isEmpty(acceptRanges) && "bytes".equals(acceptRanges)) {
                //判断连接是否正确。
                acceptRange = true;
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // 获取文件大小。
                    int fileSize = conn.getContentLength();

                    targetLength = fileSize;
                    if (fileSize <= 0) {
                        if (mCallback != null) {
                            mCallback.onFinish(CODE_DOWNLOAD_FAILED, mCurrentTaskId, filePos, targetPath);
                        }
                        return;
                    }
                    File file = new File(targetPath);
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    raf.setLength(fileSize);
                    raf.close();
                    // 将文件分成threadNum = 5份。
                    int block = fileSize % threadNum == 0 ? fileSize / threadNum
                            : fileSize / threadNum + 1;
                    for (int threadId = 0; threadId < threadNum; threadId++) {
                        new DownloadThread(threadId, fileSize, block, file, mFilePos, url, latch).start();

                    }
                  //  LogUtil.e(TAG, "wait all downloading thread");
                    if (latch != null) {
                        latch.await();
                    }
                }
            } else {
               // LogUtil.e(TAG, "dont support paratition download file");
            }
        } catch (Exception e) {
            e.printStackTrace();
            firstRequestFailed = true;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        //LogUtil.e(TAG, "internalErrorIntruppted:" + mInternalErrorInterupted.get());
        if (mInternalErrorInterupted.get()) {
            if (mInternalErrorInterupted.get()) {
                codeStatus = CODE_DOWNLOAD_CANCELED;
            }
        } else {
            if (firstRequestFailed) {
                codeStatus = CODE_DOWNLOAD_FAILED;
            } else {
                if (acceptRange) {
                    if (threadNum > 1) {
                        retryLearningDownload(1);
                        if (mDownloadingTaskMap.size() > 0) {
                            codeStatus = CODE_DOWNLOAD_FAILED;
                        }
                    }
                }
            }

          //  LogUtil.e("download", targetLength + ":" + new File(targetPath).length());

            if (targetLength != new File(targetPath).length() || !acceptRange) {
              //  LogUtil.e(TAG, "multi task download file size different:" + targetPath);
                int retrySingleTimes = 0;
                boolean finalResult = false;
                while (retrySingleTimes < MAX_RETRY_TIMES) {
                    int time = retrySingleTimes++;
                    boolean result = startDownloadBySingleThread(fileUrl, targetPath, mFilePos, time);
                    finalResult = result;
                    if (result) {
                        break;
                    }
                }
                if (!finalResult) {
                    codeStatus = CODE_DOWNLOAD_FAILED;
                }
            }
        }


       // LogUtil.e(TAG, "codeStatus:" + codeStatus);
        if (notifyCallback) {
            if (mCallback != null) {
                if (mInternalErrorInterupted.get()) {
                    codeStatus = CODE_DOWNLOAD_CANCELED;
                }
                mCallback.onFinish(codeStatus, mCurrentTaskId, filePos, targetPath);
            }

        }
        mReadBytesCount = 0;
        mCurrentTaskId = null;
    }


    private void retryLearningDownload(int retryTime) {

        if (mInternalErrorInterupted.get()) {
            return;
        }
        if (retryTime > MAX_RETRY_TIMES) {
            return;
        }
        try {
            if (mDownloadingTaskMap.size() > 0) {
              //  LogUtil.e(TAG, "retryLearningDownload:" + retryTime);
                if (retryTime <= MAX_RETRY_TIMES) {
                    if (mDownloadingTaskMap.size() == THREAD_COUNT) {
                        //TODO:开启多线程下载是失败的策略，在这里选择单线程下载
                     //   LogUtil.e(TAG, "start single thread");
                        DownloadingThread thread = mDownloadingTaskMap.get(0);
                        startDownloadBySingleThread(thread.url, thread.file, mFilePos, retryTime++);
                    } else {
                     //   LogUtil.e(TAG, "multi task download error:" + mDownloadingTaskMap.size());
                        CountDownLatch retryLatch = new CountDownLatch(mDownloadingTaskMap.size());
                        HashMap<Integer, DownloadingThread> tempTaskMap = new HashMap(mDownloadingTaskMap);
                        mDownloadingTaskMap.clear();
                        for (Integer threadId : tempTaskMap.keySet()) {
                            DownloadingThread futureTask = tempTaskMap.get(threadId);
                            if (futureTask != null) {
                             //   LogUtil.e(TAG, "futureTask:" + futureTask.threadId + ":" + mReadBytesCount);
                                new DownloadThread(futureTask, mFilePos, retryLatch).start();
                            }
                        }
                        tempTaskMap.clear();
                        try {
                         //   LogUtil.e(TAG, "learing downloader retry  wait");
                            retryLatch.await();
                            retryLearningDownload(++retryTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                  //  LogUtil.e(TAG, "beyond max retry times");
                }
            }
        } catch (OutOfMemoryError memoryError) {
            System.gc();
            System.gc();
            System.gc();
        }
    }

    public String getCurrentDownloadingTaskId() {
        return mCurrentTaskId;
    }

    private boolean startDownloadBySingleThread(String stringUrl, String targetpath, final int filePos, int retryTimes) {
        try {
            URL requestUrl = new URL(stringUrl);
            return startDownloadBySingleThread(requestUrl, new File(targetpath), filePos, retryTimes);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean startDownloadBySingleThread(URL requestUrl, File targetFile, final int filePos, final int retryTimes) {

        if (mInternalErrorInterupted.get()) {
            return false;
        }
        if (retryTimes > MAX_RETRY_TIMES) {
          //  LogUtil.v("error", "Learing.StartDownloadBySingleThread.retryTimes=" + retryTimes);
          //  EventUtil.getDefault().onEvent("error", "LearingStartDownloadBySingleThread");
            return false;
        }
        //LogUtil.e(TAG, "startDownloadBySingleThread");
        HttpURLConnection conn = null;
        //通过下载路径获取连接
        try {

            conn = (HttpURLConnection) requestUrl.openConnection();
            //设置连接的相关属性
            conn.setRequestMethod(HttpRequestSpider.METHOD_GET);
            conn.setReadTimeout(HttpRequestSpider.CONNECTION_TIMEOUT);
            //判断连接是否正确。
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // 获取文件大小。
                int fileSize = conn.getContentLength();
                if (fileSize <= 0) {
                    if (mCallback != null) {
                        mCallback.onFinish(CODE_DOWNLOAD_FAILED, mCurrentTaskId, filePos, targetFile.getAbsolutePath());
                    }
                    return false;
                }
                //得到文件名
                //根据文件大小及文件名，创建一个同样大小，同样文件名的文件
                InputStream fis = conn.getInputStream();
                // int i = httpUrlConnection.getContentLength();
                int i = 1024;
                byte[] buffer = new byte[i];
                // fis.read(b);
                int byteCount;
                FileOutputStream fos = new FileOutputStream(targetFile);
                byte[] temp;
                mReadBytesCount = 0;
                String targetPath = targetFile.getAbsolutePath();
                while ((byteCount = fis.read(buffer)) != -1) {
                    // 重点注意这样的写法
                    if (mInternalErrorInterupted.get()) {
                        break;
                    }
                    fos.write(buffer, 0, byteCount);
                    fos.flush();
                    mReadBytesCount += byteCount;
                    if (mCallback != null) {
                        mCallback.onProgress(mCurrentTaskId, filePos, targetPath, (int) (1 + 100 * (mReadBytesCount * 1.0f / fileSize)));
                    }
                }
                fis.close();
                fos.close();

                return true;
            }

            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;

    }

    //文件下载线程
    class DownloadThread extends Thread {
        int start, end, threadId;
        File file = null;
        URL url = null;
        CountDownLatch latch;
        String filePath;
        int fileSize;
        int filePositon;

        public DownloadThread(int threadId, int fileSize, int block, File file, final int filePos, URL url, CountDownLatch latch) {
            this.threadId = threadId;
            start = block * threadId;
            end = block * (threadId + 1) - 1;
            this.file = file;
            this.url = url;
            this.latch = latch;
            filePath = file.getAbsolutePath();
            this.fileSize = fileSize;
            DownloadingThread thread = new DownloadingThread();
            thread.threadId = threadId;
            thread.fileSize = fileSize;
            thread.block = block;
            thread.file = file;
            thread.url = url;
            mDownloadingTaskMap.put(threadId, thread);
            filePositon = filePos;
            setPriority(Thread.NORM_PRIORITY);
        }

        public DownloadThread(DownloadingThread downloadingThread, final int filePos, CountDownLatch latch) {
            this.threadId = downloadingThread.threadId;
            start = downloadingThread.block * threadId;
            end = downloadingThread.block * (threadId + 1) - 1;
            this.file = downloadingThread.file;
            this.url = downloadingThread.url;
            this.latch = latch;
            filePath = downloadingThread.file.getAbsolutePath();
            this.fileSize = downloadingThread.fileSize;
            mDownloadingTaskMap.put(threadId, downloadingThread);
            this.latch = latch;
            filePositon = filePos;
        }

        public void run() {
            HttpURLConnection conn = null;
            InputStream inputStream = null;
            int partiionLength = 0;
            try {
                //获取连接并设置相关属性。
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(HttpRequestSpider.METHOD_GET);
                conn.setReadTimeout(HttpRequestSpider.CONNECTION_TIMEOUT);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
                //此步骤是关键。
                conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
                int responseCode = conn.getResponseCode();
               // LogUtil.e(TAG, threadId + ": responseCode=" + responseCode);
                if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    //移动指针至该线程负责写入数据的位置。
                    raf.seek(start);
                    //读取数据并写入
                    inputStream = conn.getInputStream();
                    byte[] b = new byte[1024];
                    int len = 0;
                    while ((len = inputStream.read(b)) != -1) {
                        if (mInternalErrorInterupted.get()) {
                            break;
                        }

                        partiionLength += len;
                        mReadBytesCount += len;
                        if (mCallback != null) {
                            mCallback.onProgress(mCurrentTaskId, filePositon, filePath, (int) (1 + 100 * (mReadBytesCount * 1.0f / fileSize)));
                        }
                        raf.write(b, 0, len);
                    }
                  //  LogUtil.e("download", "线程:" + threadId + ":" + partiionLength + ":download success");
                }
                mDownloadingTaskMap.remove(threadId);
            } catch (IOException e) {
              //  LogUtil.e("download", "线程:" + threadId + ":download failed。error=" + e);
                e.printStackTrace();
                mReadBytesCount -= partiionLength;
            } finally {
                if (latch != null) {
                    latch.countDown();
                }
                if (conn != null) {
                    conn.disconnect();
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public interface IPowerfulDownloadCallback {
        void onStart(String path);

        void onFinish(int statusCode, String pageURL, int filePositon, String path);

        void onError(int errorCode);

        void onProgress(String pageURL, int filePositon, String path, final int progress);
    }


    private class DownloadingThread {
        public int threadId;
        public int fileSize;
        public int start;
        public int end;
        public int block;
        public File file;
        public URL url;
        public boolean result;
    }
}