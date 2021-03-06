package com.jb.filemanager.function.image;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.jb.filemanager.function.image.app.BaseFragmentActivity;

/**
 * Created by bill wang on 2017/6/27.
 *
 */

public class ImageActivity extends BaseFragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //不用设置视图 直接使用Content视图 减少没必要的层次
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
//            boolean internal = bundle.getBoolean(ImageManagerFragment.ARG_IS_INTERNAL_STORAGE);
            ImageManagerFragment imageManagerFragment = new ImageManagerFragment();
//            imageManagerFragment.setExtras(internal);
            imageManagerFragment.setExtras(false);
            setDefaultFragment(imageManagerFragment);
        }
    }
}
