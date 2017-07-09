package com.jb.filemanager.function.samefile;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

import static com.squareup.haha.guava.base.Joiner.checkNotNull;

/**
 * Created by boole on 7/7/17.
 */

public class VideosLoader extends AsyncTaskLoader {
    private SameFileSupport mSameFileSupport;
    public VideosLoader(@NonNull Context context, @NonNull SameFileSupport support) {
        super(context);
        mSameFileSupport = checkNotNull(support);
    }

    @Override
    public GroupList<String, FileInfo> loadInBackground() {
        return mSameFileSupport.getAllVideoInfo();
    }
}
