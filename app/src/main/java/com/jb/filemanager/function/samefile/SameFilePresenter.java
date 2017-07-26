package com.jb.filemanager.function.samefile;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.TimeUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.jb.filemanager.Const;
import com.jb.filemanager.TheApplication;
import com.jb.filemanager.eventbus.FileOperateEvent;
import com.jb.filemanager.function.filebrowser.FileBrowserActivity;
import com.jb.filemanager.function.search.view.SearchActivity;
import com.jb.filemanager.home.MainActivity;
import com.jb.filemanager.util.Logger;
import com.jb.filemanager.util.TimeUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.sql.Time;
import java.util.ArrayList;

import static com.jb.filemanager.function.docmanager.DocManagerSupport.DOC;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.DOCX;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.PDF;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.PPT;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.PPTX;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.TXT;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.XLS;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.XLSX;


/**
 * Created by bool on 17-6-30.
 *
 */

public class SameFilePresenter implements SameFileContract.Presenter,
        LoaderManager.LoaderCallbacks<GroupList<String, FileInfo>> {
    private static final String TAG = "SameFilePresenter.class";
    private SameFileActivity mView;
    private SameFileContract.Support mSupport;
    private final LoaderManager mLoaderManager;
    private AsyncTaskLoader mFileLoader;
    private GroupList<String, FileInfo> mFileGroupList;

    private int mCategoryType = Const.CategoryType.CATEGORY_TYPE_ALL;

    public SameFilePresenter(@NonNull SameFileActivity view, @NonNull SameFileContract.Support support,
                             @NonNull LoaderManager manager){
        mView = view;
        mSupport = support;
        mLoaderManager = manager;
    }
    @Override
    public Loader<GroupList<String,FileInfo>> onCreateLoader(int id, Bundle args) {
        return mFileLoader;
    }

    @Override
    public void onLoadFinished(Loader<GroupList<String, FileInfo>> loader, GroupList<String, FileInfo> data) {
        mFileGroupList = data;
        if (mFileGroupList == null || mFileGroupList.size() == 0) {
            mView.onNoFileFindShow();
        } else {
            // 显示列表
            mView.showFileList(mFileGroupList);
        }
    }

    @Override
    public void onLoaderReset(Loader<GroupList<String, FileInfo>> loader) {

    }

    @Override
    public void onCreate(Intent intent) {
        EventBus globalEventBus = TheApplication.getGlobalEventBus();
        if (!globalEventBus.isRegistered(this)) {
            globalEventBus.register(this);
        }
        if (intent != null) {
            mCategoryType = intent.getIntExtra(SameFileActivity.PARAM_CATEGORY_TYPE,
                    Const.CategoryType.CATEGORY_TYPE_PHOTO);// 默认给出一个错误值，以免混乱，避免获取不到时加载错误的选项造成疑惑
            mView.initView(mCategoryType);
            this.start(mCategoryType);
        }
    }

    @Override
    public void onDestroy() {
        EventBus globalEventBus = TheApplication.getGlobalEventBus();
        if (globalEventBus.isRegistered(this)) {
            globalEventBus.unregister(this);
        }
        mView = null;
        mSupport = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FileOperateEvent fileOperateEvent){
        if (FileOperateEvent.OperateType.COPY.equals(fileOperateEvent.mOperateType)){
            handleFileRename(fileOperateEvent);
            Toast.makeText(mView, "COPY", Toast.LENGTH_LONG);
        }else if (FileOperateEvent.OperateType.CUT.equals(fileOperateEvent.mOperateType)){
            handleFileRename(fileOperateEvent);
            Toast.makeText(mView, "CUT", Toast.LENGTH_LONG);
        }else if (FileOperateEvent.OperateType.RENAME.equals(fileOperateEvent.mOperateType)){
            handleFileRename(fileOperateEvent);
            Toast.makeText(mView, "RENAME", Toast.LENGTH_LONG);
        }else if (FileOperateEvent.OperateType.DELETE.equals(fileOperateEvent.mOperateType)){
        }else {
           // handleError();
        }
    }

    private void handleFileRename(FileOperateEvent fileOperateEvent) {
        String oldFile = fileOperateEvent.mOldFile.getAbsolutePath();
        String newFile = fileOperateEvent.mNewFile.getAbsolutePath();
        String s = newFile.toLowerCase();
        mSupport.updateDatabaseRename(oldFile, newFile);
        reloadData();
        Logger.d(TAG, "rename   " + newFile + "      " + fileOperateEvent.mNewFile.getParent());
    }


    @Override
    public void onClickBackButton(boolean finishActivity) {
        if (mView != null) {
            if (finishActivity) {
                ((AppCompatActivity)mView).finish();
            } else {
                ((AppCompatActivity)mView).finish();
            }
        }
    }

    @Override
    public void onClickSearchButton() {
        if (mView != null) {
            SearchActivity.showSearchResult(mSupport.getContext(), mCategoryType);
        }
    }

    @Override
    public void start(final int categoryType) {
        switch (categoryType) {
            case Const.CategoryType.CATEGORY_TYPE_MUSIC:
                mFileLoader = new MusicsLoader(mView, (SameFileSupport)mSupport);
                break;
            case Const.CategoryType.CATEGORY_TYPE_VIDEO:
                mFileLoader = new VideosLoader(mView, (SameFileSupport)mSupport);
                break;
            case Const.CategoryType.CATEGORY_TYPE_DOWNLOAD:
                mFileLoader = new DownloadLoader(mView, (SameFileSupport)mSupport);
                break;
            case Const.CategoryType.CATEGORY_TYPE_PHOTO:// Image file

            case Const.CategoryType.CATEGORY_TYPE_APP: // Application file

            case Const.CategoryType.CATEGORY_TYPE_DOC:

            default:
                Log.d(TAG, "No sutch filt type: " + categoryType);
        }
        mLoaderManager.initLoader(categoryType, null, this).forceLoad();
    }


    @Override
    public ArrayList<File> getSelectFile() {
        ArrayList<File> selectedFile = new ArrayList<>();
        for (int i = 0; i < mFileGroupList.size(); i++) {
            for (FileInfo info : mFileGroupList.valueAt(i)) {
                if (info.isSelected) {
                    selectedFile.add(new File(info.mFullPath));
                }
            }
        }
        return  selectedFile;

    }

    protected ArrayList<String> selectedPositon2PathList(boolean[] position) {
        ArrayList<String> selectedFile = new ArrayList<>();
        for (int i = 0; i < position.length; i++) {
            selectedFile.add(mFileGroupList.getItem(i).mFullPath);
        }
        return  selectedFile;
    }

    @Override
    public void jumpToFileBrowserPage(){
        FileBrowserActivity.startBrowser(mView, null);
    }

    @Override
    public void reloadData() {
        mView.fileSelectShow(0);
        onCreate(mView.getIntent());
    }

    @Override
    public void selectAllFile() {
        if (mFileGroupList != null) {
            for (int i = 0; i < mFileGroupList.size(); i++) {
                for (FileInfo info : mFileGroupList.valueAt(i)) {
                    info.isSelected = true;
                }
            }
            mView.showFileList(mFileGroupList);
            mView.fileSelectShow(mFileGroupList.getAllSize());
        }
    }

    @Override
    public void cleanSelect() {
        if (mFileGroupList != null) {
            for (int i = 0; i < mFileGroupList.size(); i++) {
                for (FileInfo info : mFileGroupList.valueAt(i)) {
                    info.isSelected = false;
                }
            }
            mView.showFileList(mFileGroupList);
            mView.fileSelectShow(0);
        }
    }
}
