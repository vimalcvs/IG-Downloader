package com.abc.instadownpro.service;

import android.util.Log;

import com.abc.instadownpro.base.HttpRequestSpider;
import com.abc.instadownpro.db.DownloadContentItem;
import com.abc.instadownpro.util.FileUtils;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
public class PowerfulDownloader {


    public static final int CODE_OK = 0;
    public static final int CODE_DOWNLOAD_FAILED = -1;
    public static final int CODE_DOWNLOAD_CANCELED = 100;
    public volatile static PowerfulDownloader sInstance;

    public int THREAD_COUNT = 1;

    private volatile int mReadBytesCount = 0;

    // private AtomicBoolean mSepcDownloadThreadError = new AtomicBoolean(false);
    private AtomicBoolean mInternalErrorInterupted = new AtomicBoolean(false);
    // private IPowerfulDownloadCallback mCallback;

    private String mCurrentTaskId;
    private int mFilePosition;

    private PowerfulDownloader() {
//        int cpuCount = CpuUtils.getNumberOfCPUCores() - 1;
//        if (cpuCount < 4) {
//            cpuCount = 4;
//        }
        THREAD_COUNT = 1;
    }


    public static PowerfulDownloader getDefault() {
        synchronized (PowerfulDownloader.class) {
            if (sInstance == null) {
                sInstance = new PowerfulDownloader();
            }
        }
        return sInstance;
    }


    public void interupted() {
        mInternalErrorInterupted.set(true);
    }


    private class CustomFileDownloadListener extends FileDownloadListener {


        private IPowerfulDownloadCallback callback;
        private List<String> fileList;
        private DownloadContentItem downloadContentItem;

        public CustomFileDownloadListener(List<String> fileList, IPowerfulDownloadCallback callback, DownloadContentItem downloadContentItem) {
            this.callback = callback;
            this.fileList = fileList;
            this.downloadContentItem = downloadContentItem;

        }

        @Override
        protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            Log.e("ok", "pending:" + soFarBytes + ":" + totalBytes);
        }

        @Override
        protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            if (callback != null) {
                int position = fileList.indexOf(task.getPath());
                callback.onProgress(downloadContentItem.pageURL, position, task.getPath(), (int) (100 * soFarBytes / (totalBytes * 1.0)));
            }
        }

        @Override
        protected void completed(BaseDownloadTask task) {
            if (callback != null) {
                int position = fileList.indexOf(task.getPath());
                callback.onFinish(CODE_OK, downloadContentItem.pageURL, position, task.getPath());
            }
        }

        @Override
        protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            if (callback != null) {
                int position = fileList.indexOf(task.getPath());
                callback.onFinish(CODE_DOWNLOAD_CANCELED, downloadContentItem.pageURL, position, task.getPath());
            }
        }

        @Override
        protected void error(BaseDownloadTask task, Throwable e) {
            if (callback != null) {
                int position = fileList.indexOf(task.getPath());
                callback.onError(CODE_DOWNLOAD_FAILED);
            }
        }

        @Override
        protected void warn(BaseDownloadTask task) {
            Log.e("ok", "warn:" + task.getFilename());
        }

    }

    /**
     * 下载文件
     *
     * @param home
     * @param downloadContentItem
     */
    public void startDownload(String home, DownloadContentItem downloadContentItem, final IPowerfulDownloadCallback callback) {

        final List<BaseDownloadTask> tasks = new ArrayList<>();
        int imageCount = (downloadContentItem == null || downloadContentItem.futureImageList == null) ? 0 : downloadContentItem.futureImageList.size();
        List<String> downloadFileList = new ArrayList<>();
        for (int i = 0; i < imageCount; i++) {
            String url = downloadContentItem.futureImageList.get(i);
            url = url.replace("\\u0026", "&");
            String path = FileUtils.getFilePath(downloadContentItem.getPageHome(), FileUtils.getFileNameByURL(url));
            downloadFileList.add(path);
            tasks.add(FileDownloader.getImpl().create(url).setTag(i + 1).setPath(path).addHeader("User-Agent", HttpRequestSpider.UA_1));
        }

        int videoCount = (downloadContentItem == null || downloadContentItem.futureVideoList == null) ? 0 : downloadContentItem.futureVideoList.size();
        for (int index = 0; index < videoCount; index++) {
            String url = downloadContentItem.futureVideoList.get(index);
            url = url.replace("\\u0026", "&");
            String path = FileUtils.getFilePath(downloadContentItem.getPageHome(), FileUtils.getFileNameByURL(url));
            downloadFileList.add(path);
            tasks.add(FileDownloader.getImpl().create(url).setTag(imageCount + index + 1).setPath(path).addHeader("User-Agent", HttpRequestSpider.USER_AGENT));
        }


        final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(new CustomFileDownloadListener(downloadFileList, callback, downloadContentItem));

        if (tasks.size() > 1) {
            queueSet.disableCallbackProgressTimes(); // 由于是队列任务, 这里是我们假设了现在不需要每个任务都回调`FileDownloadListener#progress`, 我们只关系每个任务是否完成, 所以这里这样设置可以很有效的减少ipc.
        }
        queueSet.setAutoRetryTimes(1);
        queueSet.downloadTogether(tasks);
        queueSet.start();
    }

    //    public void startDownload(String taskId, final int filePosition, String fileUrl, String targetPath, IPowerfulDownloadCallback callback) {
//        mCallback = callback;
//        mCurrentTaskId = taskId;
//        LogUtil.e("download", "power.startDownload:" + targetPath);
//        mFilePosition = filePosition;
//        try {
//            download(fileUrl, targetPath, THREAD_COUNT, 0, true);
//        } catch (OutOfMemoryError error) {
//            EventUtil.getDefault().onEvent("error", "PowerfulDownloader.OutOfMemory:" + error.toString());
//            System.gc();
//            System.gc();
//            System.gc();
//        }
//    }
//
//    /**
//     * 下载文件
//     */
//    private void download(String fileUrl, String targetPath, int threadNum, int retryTime, boolean notifyCallback) {
//        int codeStatus = CODE_OK;
//        CountDownLatch latch = null;
//        if (threadNum > 1) {
//            latch = new CountDownLatch(threadNum);
//        }
//
//        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
//        mSepcDownloadThreadError.set(false);
//        mInternalErrorInterupted.set(false);
//        mReadBytesCount = 0;
//        HttpURLConnection conn = null;
//        try {
//            //通过下载路径获取连接
//            URL url = new URL(fileUrl);
//            conn = (HttpURLConnection) url.openConnection();
//            //设置连接的相关属性
//            conn.setRequestMethod(HttpRequestSpider.METHOD_GET);
//            conn.setReadTimeout(HttpRequestSpider.CONNECTION_TIMEOUT);
//
//            //判断连接是否正确。
//            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                // 获取文件大小。
//                int fileSize = conn.getContentLength();
//                if (fileSize <= 0) {
//                    if (mCallback != null) {
//                        mCallback.onFinish(CODE_DOWNLOAD_FAILED, mCurrentTaskId, mFilePosition, targetPath);
//                    }
//                    return;
//                }
//                //得到文件名
//                //根据文件大小及文件名，创建一个同样大小，同样文件名的文件
//                if (threadNum == 1) {
//                    InputStream fis = conn.getInputStream();
//                    // int i = httpUrlConnection.getContentLength();
//                    int i = 1024;
//                    byte[] buffer = new byte[i];
//                    // fis.read(b);
//                    int byteCount;
//                    File targetFile = new File(targetPath);
//                    FileOutputStream fos = new FileOutputStream(targetFile);
//                    byte[] temp;
//                    while ((byteCount = fis.read(buffer)) != -1) {
//                        // 重点注意这样的写法
//                        if (mInternalErrorInterupted.get()) {
//                            break;
//                        }
//                        fos.write(buffer, 0, byteCount);
//                        fos.flush();
//                        mReadBytesCount += byteCount;
//                        if (mCallback != null) {
//                            mCallback.onProgress(mCurrentTaskId, mFilePosition, targetPath, (int) (1 + 100 * (mReadBytesCount * 1.0f / fileSize)));
//                        }
//                    }
//                    fis.close();
//                    fos.close();
//                } else {
//                    File file = new File(targetPath);
//                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
//                    raf.setLength(fileSize);
//                    raf.close();
//                    // 将文件分成threadNum = 5份。
//                    int block = fileSize % threadNum == 0 ? fileSize / threadNum
//                            : fileSize / threadNum + 1;
//                    LogUtil.e("download", "block:" + block);
//                    for (int threadId = 0; threadId < threadNum; threadId++) {
//                        //传入线程编号，并开始下载。
//                        //executorService.execute(new DownloadPartialFileRunnable(threadId, fileSize, block, file, url, latch));
//                        new DownloadThread(threadId, fileSize, block, file, url, latch).start();
//
//                    }
//                    LogUtil.e("download", "wait all downloading thread");
//                    if (latch != null) {
//                        latch.await();
//                    }
//                }
//
//                mReadBytesCount = 0;
//            }
//
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//            mSepcDownloadThreadError.set(true);
//        } catch (IOException e) {
//            e.printStackTrace();
//            mSepcDownloadThreadError.set(true);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } finally {
//            if (conn != null) {
//                conn.disconnect();
//            }
//        }
//
//        if (mSepcDownloadThreadError.get()) {
//            LogUtil.e("download", "multi task download error");
//            if (retryTime <= MAX_RETRY_TIMES) {
//                LogUtil.e("download", "download retry " + retryTime);
//                download(fileUrl, targetPath, threadNum, retryTime++, false);
//            }
//
//        } else {
//            LogUtil.e("download", threadNum + " thread download success");
//        }
//
//        if (notifyCallback) {
//            if (mCallback != null) {
//                if (mInternalErrorInterupted.get()) {
//                    codeStatus = CODE_DOWNLOAD_CANCELED;
//                }
//                mCallback.onFinish(codeStatus, mCurrentTaskId, mFilePosition, targetPath);
//            }
//
//        }
//        mCurrentTaskId = null;
//        LogUtil.e("download", "all thread executed finished");
//    }
//
    public String getCurrentDownloadingTaskId() {
        return mCurrentTaskId;
    }
//
////    class DownloadPartialFileRunnable implements Runnable {
////
////        int start, end, threadId;
////        File file = null;
////        URL url = null;
////        CountDownLatch latch;
////        String filePath;
////        int fileSize;
////
////        public DownloadPartialFileRunnable(int threadId, int fileSize, int block, File file, URL url, CountDownLatch latch) {
////            this.threadId = threadId;
////            start = block * threadId;
////            end = block * (threadId + 1) - 1;
////            this.file = file;
////            this.url = url;
////            this.latch = latch;
////            filePath = file.getAbsolutePath();
////            this.fileSize = fileSize;
////        }
////
////        @Override
////        public void run() {
////            HttpURLConnection conn = null;
////            InputStream inputStream = null;
////            try {
////                //获取连接并设置相关属性。
////                conn = (HttpURLConnection) url.openConnection();
////                conn.setRequestMethod(HttpRequestSpider.METHOD_GET);
////                conn.setReadTimeout(HttpRequestSpider.CONNECTION_TIMEOUT);
////                //此步骤是关键。
////                conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
////                int responseCode = conn.getResponseCode();
////                if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
////                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
////                    //移动指针至该线程负责写入数据的位置。
////                    raf.seek(start);
////                    //读取数据并写入
////                    inputStream = conn.getInputStream();
////                    byte[] b = new byte[1024];
////                    int len = 0;
////                    int lengthPlus = 0;
////                    while ((len = inputStream.read(b)) != -1) {
////                        if (sIntrupted) {
////                            break;
////                        }
////                        lengthPlus += len;
////                        mReadBytesCount += len;
////                        if (mCallback != null) {
////                            mCallback.onProgress(filePath, (int) (1 + 100 * (mReadBytesCount * 1.0f / fileSize)));
////                        }
////                        raf.write(b, 0, len);
////                    }
////                    LogUtil.e("download", "线程:" + threadId + ":" + lengthPlus + ":下载完毕");
////                }
////            } catch (IOException e) {
////                e.printStackTrace();
////            } finally {
////                latch.countDown();
////                if (conn != null) {
////                    conn.disconnect();
////                }
////                if (inputStream != null) {
////                    try {
////                        inputStream.close();
////                    } catch (IOException e) {
////                        e.printStackTrace();
////                    }
////                }
////            }
////        }
////    }
//
//    //文件下载线程
//    class DownloadThread extends Thread {
//        int start, end, threadId;
//        File file = null;
//        URL url = null;
//        CountDownLatch latch;
//        String filePath;
//        int fileSize;
//
//        public DownloadThread(int threadId, int fileSize, int block, File file, URL url, CountDownLatch latch) {
//            this.threadId = threadId;
//            start = block * threadId;
//            end = block * (threadId + 1) - 1;
//            this.file = file;
//            this.url = url;
//            this.latch = latch;
//            filePath = file.getAbsolutePath();
//            this.fileSize = fileSize;
//        }
//
//        public void run() {
//            HttpURLConnection conn = null;
//            InputStream inputStream = null;
//            try {
//                //获取连接并设置相关属性。
//                conn = (HttpURLConnection) url.openConnection();
//                conn.setRequestMethod(HttpRequestSpider.METHOD_GET);
//                conn.setReadTimeout(HttpRequestSpider.CONNECTION_TIMEOUT);
//                //此步骤是关键。
//                conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
//                int responseCode = conn.getResponseCode();
//                if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
//                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
//                    //移动指针至该线程负责写入数据的位置。
//                    raf.seek(start);
//                    //读取数据并写入
//                    inputStream = conn.getInputStream();
//                    byte[] b = new byte[1024];
//                    int len = 0;
//                    int lengthPlus = 0;
//                    while ((len = inputStream.read(b)) != -1) {
//                        if (mInternalErrorInterupted.get()) {
//                            break;
//                        }
//
//                        lengthPlus += len;
//                        mReadBytesCount += len;
//                        if (mCallback != null) {
//                            mCallback.onProgress(mCurrentTaskId, mFilePosition, filePath, (int) (1 + 100 * (mReadBytesCount * 1.0f / fileSize)));
//                        }
//                        raf.write(b, 0, len);
//                    }
//                    LogUtil.e("download", "线程:" + threadId + ":" + lengthPlus + ":download success");
//                }
//            } catch (IOException e) {
//                LogUtil.e("download", "线程:" + threadId + ":download failed。error=" + e);
//                mSepcDownloadThreadError.set(true);
//                mInternalErrorInterupted.set(true);
//                e.printStackTrace();
//            } finally {
//                latch.countDown();
//                if (conn != null) {
//                    conn.disconnect();
//                }
//                if (inputStream != null) {
//                    try {
//                        inputStream.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }

    public interface IPowerfulDownloadCallback {
        void onStart(String path);

        void onFinish(int statusCode, String pageURL, int filePositon, String path);

        void onError(int errorCode);

        void onProgress(String pageURL, int filePositon, String path, final int progress);
    }

}