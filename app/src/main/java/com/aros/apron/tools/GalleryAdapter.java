package com.aros.apron.tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.aros.apron.R;
import com.aros.apron.base.BaseAdapter;
import com.aros.apron.base.BaseHolder;
import com.aros.apron.databinding.ItemGalleryBinding;

import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.datacenter.media.MediaFileListData;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * 相册
 */

public class GalleryAdapter extends BaseAdapter<String, ItemGalleryBinding> {

    MediaFileListData mediaFileListData;
    private Activity context;

    @Override
    protected void onBindingData(BaseHolder<ItemGalleryBinding> holder, String s, int position) {
        holder.getViewBinding().tvName.setText(mediaFileListData.getData().get(position).getFileName());
        holder.getViewBinding().tvTime.setText(mediaFileListData.getData().get(position).getDate().getMonth() + "--" + mediaFileListData.getData().get(position).getDate().getDay());
        if (mediaFileListData.getData().get(position).getThumbNail() != null) {
            holder.getViewBinding().ivGallery.setImageBitmap(mediaFileListData.getData().get(position).getThumbNail());

        } else {
            mediaFileListData.getData().get(position).pullThumbnailFromCamera(new CommonCallbacks.CompletionCallbackWithParam<Bitmap>() {
                @Override
                public void onSuccess(Bitmap bitmap) {

                    AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
                        @Override
                        public void run() {
                            if ((int) holder.getViewBinding().ivGallery.getTag() == position) {
                                holder.getViewBinding().ivGallery.setImageBitmap(bitmap);

                            }
                        }
                    });


                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
                        @Override
                        public void run() {
                                holder.getViewBinding().ivGallery.setImageDrawable(context.getDrawable(R.mipmap.ic_launcher));


                        }
                    });
                }
            });
        }

    }

    @Override
    protected ItemGalleryBinding onBindingView(ViewGroup viewGroup) {
        ItemGalleryBinding itemGalleryBinding = ItemGalleryBinding.inflate(LayoutInflater.from(viewGroup.getContext()), viewGroup, false);
        return itemGalleryBinding;
    }

    public void setData(MediaFileListData datas) {
        this.mediaFileListData = datas;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mediaFileListData != null) {
            return mediaFileListData.getData().size();
        } else {
            return 0;
        }
    }
}