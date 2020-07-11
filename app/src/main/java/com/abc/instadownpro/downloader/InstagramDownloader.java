package com.abc.instadownpro.downloader;

import android.text.TextUtils;
import android.util.Log;

import com.abc.instadownpro.BuildConfig;
import com.abc.instadownpro.db.DownloadContentItem;
import com.abc.instadownpro.util.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstagramDownloader extends BaseDownloader {

    public static final String CDN_IMAGE_SUFFIX = "cdninstagram.com/";

    public String getVideoUrl(String content) {
        String regex;
        String videoUrl = null;
        regex = "<meta property=\"og:video\" content=\"(.*?)\" />";
        Pattern pa = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher ma = pa.matcher(content);

        while (ma.find()) {
            videoUrl = ma.group(1);
            Log.e("ok", "videoURL=" + videoUrl);

        }
        return videoUrl;
    }

    public String getImageUrl(String content) {
        Log.e("fan", "getImageURL:" + content);
        String regex;
        String imageUrl = "";
        regex = "<meta property=\"og:image\" content=\"(.*?)\"";
        Pattern pa = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher ma = pa.matcher(content);

        if (ma.find()) {
            imageUrl = ma.group(1);
         //   LogUtil.e("ok", "origin_imageUrl=" + imageUrl);
        }

        return imageUrl;
    }

    public String getPageTitle(String content) {
        String regex;
        String pageDesc = "";
        regex = "<meta property=\"og:description\" content=\"(.*?)\"";
        Pattern pa = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher ma = pa.matcher(content);

        if (ma.find()) {
            Log.v("fan2", "" + ma.group());
            pageDesc = ma.group(1);
        }

        if (!TextUtils.isEmpty(pageDesc)) {
            String array[] = pageDesc.split("Instagram:");
            if (array != null) {
                String originTitle = array[array.length - 1];
                originTitle = originTitle.replace("“", "");
                originTitle = originTitle.replace("”", "");
                return originTitle;
            }
        }
        return null;
    }

    public String getDescription(String content) {

        String regex;
        String pageDesc = "";
        regex = "\"node\": \\{\"text\": \"(.*?)\"";
        Pattern pa = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher ma = pa.matcher(content);

        if (ma.find()) {
            Log.v("fan2", "" + ma.group());
            pageDesc = ma.group(1);
          //  LogUtil.e("ins", "pageDescritpin:" + pageDesc);
        }

        if (!TextUtils.isEmpty(pageDesc)) {
            return pageDesc;
        }
        return null;
    }

    public String getPageHashTags(String content) {
        String regex;
        String hashTags = "";
        regex = "<meta property=\"instapp:hashtags\" content=\"(.*?)\"";
        Pattern pa = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher ma = pa.matcher(content);
        StringBuilder hashTagsBuilder = new StringBuilder();
        while (ma.find()) {
            Log.v("fan2", "" + ma.group());
            hashTags = ma.group(1);
          //  LogUtil.e("ins", "hashTags=" + hashTags);
            hashTagsBuilder.append("#");
            hashTagsBuilder.append(hashTags);

        }

        return hashTagsBuilder.toString();
    }

    public void getImageUrlFromJs(String content, DownloadContentItem data) {
        String regex;
        String imageUrl = "";
        regex = "\"display_url\":\"(.*?)\"";
        Pattern pa = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher ma = pa.matcher(content);
        while (ma.find()) {
            imageUrl = ma.group(1);
            if (!TextUtils.isEmpty(imageUrl)) {
                if (imageUrl.contains(CDN_IMAGE_SUFFIX)) {
                    //  String tempArray[] = imageUrl.split(CDN_IMAGE_SUFFIX);
                    //  imageUrl = REPLACE_SUFFIX + tempArray[tempArray.length - 1];
                    //.e("ok", "display_url=" + imageUrl);
                    data.addImage(imageUrl);
                } else {
                //    LogUtil.e("ok", "display_url=" + imageUrl);
                    data.addImage(imageUrl);
                }
            }
        }
    }

    public void getVideoUrlFromJs(String content, DownloadContentItem data) {
        String regex;
        String videoUrl = "";
        regex = "\"video_url\":\"(.*?)\"";
        Pattern pa = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher ma = pa.matcher(content);
        while (ma.find()) {
            videoUrl = ma.group(1);
            if (BuildConfig.DEBUG) {
                Log.e("ok", "videoUrl:" + videoUrl);
            }

            data.addVideo(videoUrl);
        }
    }

    public void showAllMetaList(String content) {
        String regex;
        String hashTags = "";
        regex = "<meta property=\"(.*?)\" content=\"(.*?)\"";
        Pattern pa = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher ma = pa.matcher(content);
        while (ma.find()) {
            String key = ma.group(1);
            String value = ma.group(2);
            Log.e("list", key + "-->" + value);
        }
    }

    public DownloadContentItem startSpideThePage(String htmlUrl) {
        String content = startRequest(htmlUrl);
        showAllMetaList(content);
        DownloadContentItem data = new DownloadContentItem();
        getVideoUrlFromJs(content, data);
        data.pageThumb = getImageUrl(content);
        getImageUrlFromJs(content, data);
        data.pageTitle = getPageTitle(content);
        data.pageDesc = getDescription(content);
        data.pageURL = htmlUrl;
        data.pageTags = getPageHashTags(content);
        if (data.futureImageList == null || data.futureImageList.size() == 0 ) {
            String imageURL = getImageUrl(content);
            if (!TextUtils.isEmpty(imageURL)) {
                data.addImage(imageURL);
            }

        }

        if (data.futureVideoList == null || data.futureVideoList.size() == 0) {
            String videoURL = getVideoUrl(content);
            if(!TextUtils.isEmpty(videoURL)) {
                data.getVideoList().add(videoURL);
            }

        }

       // LogUtil.e("ok", "data.futerImageList.size:" + (data.futureImageList == null ? 0 : data.futureImageList.size()));
        //useFileDownloader(data);
        //return null;
        data.homeDirectory = "instagram";
        return data;
    }




//    private void useFileDownloader(DownloadContentItem downloadContentItem) {
//        final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(queueTarget);
//
//        final List<BaseDownloadTask> tasks = new ArrayList<>();
//        int imageCount = (downloadContentItem == null || downloadContentItem.futureImageList == null) ? 0 : downloadContentItem.futureImageList.size();
//
//        for (int i = 0; i < imageCount; i++) {
//            String url = downloadContentItem.futureImageList.get(i);
//            url = url.replace("\\u0026","&");
//            tasks.add(FileDownloader.getImpl().create(url).setTag(i + 1).setPath(FileUtils.getFilePath(FileUtils.getFileNameByURL(url))).addHeader("User-Agent",HttpRequestSpider.UA_1));
//        }
//
//        int videoCount = (downloadContentItem == null || downloadContentItem.futureVideoList == null) ? 0 : downloadContentItem.futureVideoList.size();
//
//        for (int index = 0; index < videoCount; index++) {
//            String url = downloadContentItem.futureVideoList.get(index);
//            url = url.replace("\\u0026","&");
//            tasks.add(FileDownloader.getImpl().create(url).setTag(imageCount + index + 1).setPath(FileUtils.getFilePath(FileUtils.getFileNameByURL(url))).addHeader("User-Agent",HttpRequestSpider.USER_AGENT));
//        }
//
//        queueSet.disableCallbackProgressTimes(); // 由于是队列任务, 这里是我们假设了现在不需要每个任务都回调`FileDownloadListener#progress`, 我们只关系每个任务是否完成, 所以这里这样设置可以很有效的减少ipc.
//// 所有任务在下载失败的时候都自动重试一次
//        queueSet.setAutoRetryTimes(1);
//
//        queueSet.downloadTogether(tasks);
//        queueSet.start();
//        Log.e("ok", "filedownloader.startdownload");
//    }

    public String getLaunchInstagramUrl(String content) {
        String regex;
        String instagramUrl = "";
        regex = "<meta property=\"al:android:url\" content=\"(.*?)\"";
        Pattern pa = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher ma = pa.matcher(content);

        if (ma.find()) {
            instagramUrl = ma.group(1);
        }
        return instagramUrl;
    }
}
