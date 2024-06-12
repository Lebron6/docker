package com.aros.apron.base;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public abstract class BaseAdapter<T,V extends ViewBinding> extends RecyclerView.Adapter<BaseHolder> {

    @NonNull
    @Override
    public BaseHolder<V> onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new BaseHolder(onBindingView(viewGroup));
    }
 
    @Override
    public void onBindViewHolder(@NonNull BaseHolder holder, int position) {
//        onBindingData(holder,data.get(position),position);
        onBindingData(holder,null,position);
    }
 
    protected abstract void onBindingData(BaseHolder<V> holder, T t, int position);
    protected abstract V onBindingView(ViewGroup viewGroup);
 

}
