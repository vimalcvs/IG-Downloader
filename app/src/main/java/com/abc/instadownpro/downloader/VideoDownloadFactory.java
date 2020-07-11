package com.abc.instadownpro.downloader;

import android.os.Looper;
import android.util.Log;

import com.abc.instadownpro.db.DownloadContentItem;
import com.abc.instadownpro.db.DownloaderDBHelper;
import com.abc.instadownpro.util.URLMatcher;
import com.abc.instadownpro.util.Utils;


public final class VideoDownloadFactory {

    private BaseDownloader mDownloader;


    private static VideoDownloadFactory sInstance = new VideoDownloadFactory();


    public static VideoDownloadFactory getInstance() {
        return sInstance;
    }

    /**
     * 下载视频工厂入口
     *
     * @param url
     */
    public DownloadContentItem request(String url) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("video download cannt start from main thread");
        }
        Log.e("fan","request:" + url);
        String handledUrl = URLMatcher.getHttpURL(url);

        BaseDownloader downloader = getSpecDownloader(handledUrl);

        if (downloader == null) {
            //LogUtil.v("fan", "The tools dont support downloading this video");
        }

        if (downloader != null) {
            return downloader.startSpideThePage(url);
        }

        return null;
    }


    private BaseDownloader getSpecDownloader(String url) {
        BaseDownloader downloader;

        if (url.contains("www.instagram.com")) {
            //TODO:匹配instagram的视频下载器
            return new InstagramDownloader();
        }

        return null;
    }

    public boolean isSupportWeb(String url) {
        if (url.contains("www.instagram.com")) {
            return true;
        }


        for (String hostKey : Utils.EXPIRE_SUFFIX_ARRAY) {
            if (url.contains(hostKey)) {
                return true;
            }
        }
        return false;
    }

    public boolean needShowPasteBtn() {
        String normalURL = Utils.getTextFromClipboard();
       // LogUtil.e("main", "needShowPasteBtn====" + normalURL);
        if (DownloaderDBHelper.SINGLETON.isExistPageURL(normalURL)) {
            return false;
        }
        return VideoDownloadFactory.getInstance().isSupportWeb(normalURL);
    }


}
