package com.abc.instadownpro.model;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bcgdv.asia.lib.fanmenu.FanMenuButtons;
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.github.lzyzsd.circleprogress.CircleProgress;
import com.abc.instadownpro.R;
import com.google.android.gms.ads.AdView;



public class ItemViewHolder extends RecyclerView.ViewHolder {


    public ImageView thumbnailView;
    public ImageView operationBtn;
    public NumberProgressBar progressBar;
    public TextView titleTv;
    public ImageView moreIv;
    public FanMenuButtons fanMenuButtons;
    public View repostView;
    public View playView;
    public View albumView;
    public TextView taskCountView;
    public TextView hashTagView;
    public CircleProgress circleProgress;
    public CheckBox checkBox;

    public AdView adView;

    public ItemViewHolder(View itemView) {
        super(itemView);
        thumbnailView = (ImageView) itemView.findViewById(R.id.thumbnail);
        operationBtn = (ImageView) itemView.findViewById(R.id.btn_operation);
        progressBar = (NumberProgressBar) itemView.findViewById(R.id.progressbar);
        titleTv = (TextView) itemView.findViewById(R.id.title);
        moreIv = (ImageView) itemView.findViewById(R.id.more);
        fanMenuButtons = (FanMenuButtons) itemView.findViewById(R.id.myFABSubmenu);
        repostView = itemView.findViewById(R.id.repost);
        playView = itemView.findViewById(R.id.play_view);
        albumView = itemView.findViewById(R.id.album_icon);
        taskCountView = (TextView) itemView.findViewById(R.id.download_task_info);
        hashTagView = (TextView) itemView.findViewById(R.id.hash_tag);
        circleProgress = (CircleProgress) itemView.findViewById(R.id.circle_progress);
        checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);
        adView = (AdView) itemView.findViewById(R.id.admob_adview);

    }

}
