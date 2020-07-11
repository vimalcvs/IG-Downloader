package com.abc.instadownpro.adapter;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.abc.instadownpro.model.ItemViewHolder;
import com.abc.instadownpro.model.NativeAdItemHolder2;
import com.abc.instadownpro.base.Constant;
import com.abc.instadownpro.util.PreferenceUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.abc.instadownpro.R;
import com.abc.instadownpro.base.MainApplication;
import com.abc.instadownpro.db.DBHelper;
import com.abc.instadownpro.db.DownloadContentItem;
import com.abc.instadownpro.db.DownloaderDBHelper;
import com.abc.instadownpro.util.DownloadUtil;
import com.abc.instadownpro.util.MimeTypeUtil;
import com.abc.instadownpro.util.PopWindowUtils;
import com.abc.instadownpro.util.Utils;
import com.google.android.gms.ads.AdRequest;

import java.io.File;
import java.util.HashSet;
import java.util.List;



public class MainListRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<DownloadContentItem> mDataList;
    private RequestManager imageLoader;
    private boolean mFullImageState = false;
    private Context mContext;
    private DBHelper mDBHelper;

    private LinearLayoutManager mLayoutManager;

    private HashSet<DownloadContentItem> mSelectList;
    private boolean mIsSelectMode = false;
    private ISelectChangedListener mListener;

    public MainListRecyclerAdapter(List<DownloadContentItem> dataList, boolean isFullImage) {
        mDataList = dataList;
        imageLoader = Glide.with(MainApplication.getInstance().getApplicationContext());
        mFullImageState = isFullImage;
        mDBHelper = DBHelper.getDefault();
        mContext = MainApplication.getInstance().getApplicationContext();
        mSelectList = new HashSet<>();
    }

    public void setLayoutManager(LinearLayoutManager layoutManager) {
        this.mLayoutManager = layoutManager;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == DownloadContentItem.TYPE_FACEBOOK_AD) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.facebook_native_item_2,
                            parent, false);
            return new NativeAdItemHolder2(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(mFullImageState ? R.layout.item_layout2 : R.layout.item_layout,
                            parent, false);
            return new ItemViewHolder(itemView);
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder baseHolder, final int position) {
        final DownloadContentItem bean = mDataList.get(position);
        baseHolder.itemView.setTag(bean);
        if (baseHolder instanceof ItemViewHolder) {
            final ItemViewHolder holder = (ItemViewHolder) baseHolder;

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   // EventUtil.getDefault().onEvent("history", "openFileList");
                    if (holder.checkBox.getVisibility() == View.VISIBLE) {
                        if (mSelectList.contains(bean)) {
                            mSelectList.remove(bean);
                            holder.checkBox.setChecked(false);
                        } else {
                            mSelectList.add(bean);
                            holder.checkBox.setChecked(true);
                        }
                    } else {
                        File targetFile = new File(bean.pageHOME);
                        if (targetFile.exists() && targetFile.listFiles() != null && targetFile.listFiles().length > 0) {
                            DownloadUtil.openFileList(bean.pageHOME);
                        } else {
                            Toast.makeText(mContext, R.string.download_result_start, Toast.LENGTH_SHORT).show();
                            holder.circleProgress.setVisibility(View.VISIBLE);
                            bean.pageStatus = DownloadContentItem.PAGE_STATUS_DOWNLOADING;
                            DownloadUtil.startForceDownload(bean.pageURL);
                        }
                    }
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    if (!mIsSelectMode) {
                        setSelectMode();
                        //holder.checkBox.setVisibility(View.VISIBLE);
                        return true;
                    }
                    return false;
                }
            });

            if (mIsSelectMode) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(mSelectList.contains(bean));
            } else {
                holder.checkBox.setVisibility(View.GONE);
            }

            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSelectList.contains(bean)) {
                        mSelectList.remove(bean);
                        holder.checkBox.setChecked(false);
                    } else {
                        mSelectList.add(bean);
                        holder.checkBox.setChecked(true);
                    }
                }
            });
            if (bean.pageStatus == DownloadContentItem.PAGE_STATUS_DOWNLOADING) {
                holder.circleProgress.setVisibility(View.VISIBLE);
            } else {
                holder.circleProgress.setVisibility(View.GONE);
            }
            holder.titleTv.setText(bean.pageTitle);
            final boolean isVideo = bean.mimeType == bean.PAGE_MIME_TYPE_VIDEO;
            if (isVideo) {
                holder.playView.setVisibility(View.VISIBLE);
            } else {
                holder.playView.setVisibility(View.GONE);
            }

            holder.albumView.setVisibility(bean.fileCount > 1 ? View.VISIBLE : View.GONE);
            try {
                if (TextUtils.isEmpty(bean.pageThumb)) {
                    File file = new File(bean.pageHOME);
                    if (file != null && file.listFiles() != null && file.listFiles().length > 0) {
                        String path = file.listFiles()[0].getAbsolutePath();
                        if (MimeTypeUtil.isVideoType(path)) {
                            imageLoader.load(path).priority(Priority.IMMEDIATE).into(holder.thumbnailView);
                        }
                    }

                } else {
                    imageLoader.load(bean.pageThumb).diskCacheStrategy(DiskCacheStrategy.DATA).priority(Priority.IMMEDIATE).into(holder.thumbnailView);
                }
            } catch (OutOfMemoryError error) {
                System.gc();
                System.gc();
                System.gc();
            }
            holder.repostView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                 //   EventUtil.getDefault().onEvent("history", "delete");
                    int index = mDataList.indexOf(bean);
                    notifyItemRemoved(index);
                    mDataList.remove(index);
                    sendDeleteVideoBroadcast(bean.pageURL);
                    DownloaderDBHelper.SINGLETON.deleteDownloadTaskAsync(bean.pageURL);
                }
            });

            if (TextUtils.isEmpty(bean.pageTags)) {
                holder.hashTagView.setVisibility(View.GONE);
            } else {
                holder.hashTagView.setVisibility(View.VISIBLE);
                holder.hashTagView.setText(bean.pageTags);
            }

            holder.moreIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopWindowUtils.showVideoMoreOptionWindow(v, false, new IPopWindowClickCallback() {
                        @Override
                        public void onCopyAll() {
                           // EventUtil.getDefault().onEvent("history", "copyAll");
                            String title = bean.pageTitle;
                            String hashTags = bean.pageTags;
                            StringBuilder sb = new StringBuilder(bean.pageURL);
                            if (!TextUtils.isEmpty(title)) {
                                sb.append(title);
                            }

                            if (!TextUtils.isEmpty(hashTags)) {
                                sb.append(hashTags);
                            }

                            Utils.copyText2Clipboard(sb.toString());

                        }

                        @Override
                        public void onCopyHashTags() {
                            //EventUtil.getDefault().onEvent("history", "copyHashTags");
                            String hashTags = bean.pageTags;
                            StringBuilder sb = new StringBuilder();
                            if (!TextUtils.isEmpty(hashTags)) {
                                sb.append(hashTags);
                                Utils.copyText2Clipboard(sb.toString());
                            }
                        }

                        @Override
                        public void launchAppByUrl() {

                            //EventUtil.getDefault().onEvent("history", "launchInstagramByURL");
                            if (bean != null && !TextUtils.isEmpty(bean.pageURL)) {
                                Utils.openInstagramByUrl(bean.pageURL);
                            }
                        }

                        @Override
                        public void onPasteSharedUrl() {
                            //EventUtil.getDefault().onEvent("history", "pasteURL");
                            if (bean != null && !TextUtils.isEmpty(bean.pageURL)) {
                                Utils.copyText2Clipboard(bean.pageURL);
                            }

                        }

                        @Override
                        public void onShare() {
                        }

                        @Override
                        public void onStartDownload() {
                            Toast.makeText(mContext, R.string.download_result_start, Toast.LENGTH_SHORT).show();
                            DownloadUtil.startForceDownload(bean.pageURL);
                            holder.circleProgress.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        } else if (baseHolder instanceof NativeAdItemHolder2) {
            final NativeAdItemHolder2 holder = (NativeAdItemHolder2) baseHolder;
        
        }
    }


    private void sendDeleteVideoBroadcast(String pageURL) {
        Intent intent = new Intent(PreferenceUtils.ACTION_NOTIFY_DATA_CHANGED);
        intent.putExtra(PreferenceUtils.KEY_BEAN_PAGE_URL, pageURL);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }


    public interface IPopWindowClickCallback {
        void onCopyAll();

        void onCopyHashTags();

        void onShare();

        void launchAppByUrl();

        void onPasteSharedUrl();

        void onStartDownload();
    }

    public interface ISelectChangedListener {
        void onEnterSelectMode();

        void onQuitSelectMode();

        void onDeleteDownloadItem(DownloadContentItem downloadContentItem);
    }

    @Override
    public int getItemCount() {
        return mDataList == null ? 0 : mDataList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mDataList.get(position).itemType;
    }

    private void setSelectMode() {
        if (mListener != null) {
            mListener.onEnterSelectMode();
        }
        mIsSelectMode = true;
        mSelectList.clear();
        notifyUIChanged();
    }

    public void quitSelectMode() {
        mIsSelectMode = false;
        mSelectList.clear();
        notifyUIChanged();
    }

    private void notifyUIChanged() {
        final int firstPosition = mLayoutManager.findFirstVisibleItemPosition();
        final int lastPosition = mLayoutManager.findLastVisibleItemPosition();
        notifyItemRangeChanged(0, mDataList.size());
    }

    public boolean isSelectMode() {
        return mIsSelectMode;
    }

    public void selectAll() {
        if (mSelectList.size() == mDataList.size()) {
            mSelectList.clear();
        } else {
            mSelectList.addAll(mDataList);
        }
        notifyUIChanged();
    }

    public HashSet<DownloadContentItem> getSelectList() {
        HashSet hashSet = new HashSet<>(mSelectList);
        return hashSet;
    }

    public void clearSelectedList() {
        mSelectList.clear();
    }

    public void setISelectChangedListener(ISelectChangedListener listener) {
        this.mListener = listener;
    }

}
