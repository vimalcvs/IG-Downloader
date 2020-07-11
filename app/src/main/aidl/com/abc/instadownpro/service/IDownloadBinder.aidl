// IDownloadBinder.aidl
package com.abc.instadownpro.service;

import com.abc.instadownpro.service.IDownloadCallback;
// Declare any non-default types here with import statements

interface IDownloadBinder {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void registerCallback(IDownloadCallback callback);

    void unregisterCallback(IDownloadCallback callback);


}
