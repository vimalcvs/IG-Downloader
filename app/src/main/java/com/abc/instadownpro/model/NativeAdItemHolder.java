package com.abc.instadownpro.model;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.abc.instadownpro.R;
import com.google.android.gms.ads.AdView;

public class NativeAdItemHolder extends RecyclerView.ViewHolder {

    public AdView adView;

    public NativeAdItemHolder(View itemView) {
        super(itemView);
        adView = (AdView) itemView.findViewById(R.id.admob_adview);
    }
}
