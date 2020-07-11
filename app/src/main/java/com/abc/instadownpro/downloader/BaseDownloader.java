package com.abc.instadownpro.downloader;

import com.abc.instadownpro.db.DownloadContentItem;
import com.abc.instadownpro.base.HttpRequestSpider;

public abstract class BaseDownloader {

    protected String startRequest(String htmlUrl) {
        return HttpRequestSpider.getInstance().request(htmlUrl);
    }

    public abstract String getVideoUrl(String content);

    public abstract DownloadContentItem startSpideThePage(String htmlUrl);
}

