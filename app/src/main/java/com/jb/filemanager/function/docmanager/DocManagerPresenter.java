package com.jb.filemanager.function.docmanager;

import android.content.Intent;
import android.text.TextUtils;

import com.jb.filemanager.R;
import com.jb.filemanager.TheApplication;
import com.jb.filemanager.commomview.GroupSelectBox;
import com.jb.filemanager.database.provider.DocFileProvider;
import com.jb.filemanager.eventbus.DocFileScanFinishEvent;
import com.jb.filemanager.eventbus.FileOperateEvent;
import com.jb.filemanager.manager.spm.IPreferencesIds;
import com.jb.filemanager.manager.spm.SharedPreferencesManager;
import com.jb.filemanager.os.ZAsyncTask;
import com.jb.filemanager.util.Logger;
import com.jb.filemanager.util.StorageUtil;
import com.jb.filemanager.util.file.FileUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.jb.filemanager.function.docmanager.DocManagerSupport.DOC;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.DOCX;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.PDF;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.PPT;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.PPTX;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.TXT;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.XLS;
import static com.jb.filemanager.function.docmanager.DocManagerSupport.XLSX;

/**
 * Desc:
 * Author lqf
 * Email: liqf@m15.cn
 * Date: 2017/7/4 11:07
 */

public class DocManagerPresenter implements DocManagerContract.Presenter{
    public static final String TAG = "DocManagerPresenter";
    private DocManagerContract.View mView;
    private DocManagerContract.Support mSupport;
    private DocScanListener mDocScanListener;
    private boolean mIsFirstIn = true;

    public DocManagerPresenter(DocManagerContract.View view, DocManagerContract.Support support) {
        mView = view;
        mSupport = support;
    }

    @Override
    public void onCreate(Intent intent) {
        EventBus globalEventBus = TheApplication.getGlobalEventBus();
        if (!globalEventBus.isRegistered(this)) {
            globalEventBus.register(this);
        }
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void refreshData(boolean keepUserCheck, boolean shouldScanAgain) {
        onCreate(null);
        mView.refreshList(keepUserCheck, shouldScanAgain);
    }

    @Override
    public void onClickBackButton(boolean isSearchBack) {
        if (mView != null) {
            if (!isSearchBack) {
                mView.finishActivity();
            }
        }
    }

    @Override
    public void onPressHomeKey() {

    }

    @Override
    public void getDocInfo(boolean keepUserCheck, boolean shouldScanAgain) {
        if (mIsFirstIn) {//第一次进入先从数据库读取数据 然后再扫描
            ArrayList<DocChildBean> mDocList = mSupport.getDocFileInfo();
            ArrayList<DocChildBean> mTxtList = mSupport.getTextFileInfo();
            ArrayList<DocChildBean> mPdfList = mSupport.getPdfFileInfo();

            sortResult(mDocList);
            sortResult(mTxtList);
            sortResult(mPdfList);

            DocGroupBean txtGroupBean = new DocGroupBean(mTxtList, TheApplication.getAppContext().getString(R.string.file_type_txt), GroupSelectBox.SelectState.NONE_SELECTED, false);
            DocGroupBean docGroupBean = new DocGroupBean(mDocList, TheApplication.getAppContext().getString(R.string.file_type_doc), GroupSelectBox.SelectState.NONE_SELECTED, false);
            DocGroupBean pdfGroupBean = new DocGroupBean(mPdfList, TheApplication.getAppContext().getString(R.string.file_type_pdf), GroupSelectBox.SelectState.NONE_SELECTED, false);

            ArrayList<DocGroupBean> groups = new ArrayList<>();
            if (mDocList.size() > 0) {
                groups.add(txtGroupBean);
            }
            if (mPdfList.size() > 0) {
                groups.add(pdfGroupBean);
            }
            if (mTxtList.size() > 0) {
                groups.add(docGroupBean);
            }

            if (mDocScanListener != null && groups.size() > 0) {
                mDocScanListener.onScanFinish(groups, false);
            }
            mIsFirstIn = false;
        }
        ScanDocFileTask scanDocFileTask = new ScanDocFileTask();
        scanDocFileTask.executeOnExecutor(ZAsyncTask.THREAD_POOL_EXECUTOR, keepUserCheck, shouldScanAgain);
    }

    @Override
    public void handleFileDelete(List<File> docPathList) {
        //处理删除进度的问题
        int size = docPathList.size();
        for (int i = 0; i < size; i++) {
            mSupport.handleFileDelete(docPathList.get(i).getAbsolutePath());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FileOperateEvent fileOperateEvent){
        if (FileOperateEvent.OperateType.COPY.equals(fileOperateEvent.mOperateType)){
            handleFileCopy(fileOperateEvent);
        }else if (FileOperateEvent.OperateType.CUT.equals(fileOperateEvent.mOperateType)){
            handleFileCut(fileOperateEvent);
        }else if (FileOperateEvent.OperateType.RENAME.equals(fileOperateEvent.mOperateType)){
            handleFileRename(fileOperateEvent);
        }else if (FileOperateEvent.OperateType.DELETE.equals(fileOperateEvent.mOperateType)){
            handleFileDelete(fileOperateEvent);
        }else {
            handleError();
        }
    }

    private void handleFileCopy(FileOperateEvent fileOperateEvent) {
        File newFile = fileOperateEvent.mNewFile;
        mSupport.handleFileCopy(fileOperateEvent.mOldFile.getAbsolutePath(), fileOperateEvent.mNewFile.getAbsolutePath());
        refreshData(false, false);
        Logger.d(TAG, "copy   " + newFile.getAbsolutePath() + "      " + newFile.getParent());
    }

    private void handleFileCut(FileOperateEvent fileOperateEvent) {
        mSupport.handleFileCut(fileOperateEvent.mOldFile.getAbsolutePath(), fileOperateEvent.mNewFile.getAbsolutePath());
        refreshData(false, false);
        Logger.d(TAG, "cut   " + fileOperateEvent.mNewFile.getAbsolutePath() + "      " + fileOperateEvent.mNewFile.getParent());
    }

    private void handleFileDelete(FileOperateEvent fileOperateEvent) {
        mSupport.handleFileDelete(fileOperateEvent.mOldFile.getParent());
        Logger.d(TAG, "delete   " + fileOperateEvent.mOldFile.getAbsolutePath() + "      " + fileOperateEvent.mNewFile.getParent());
    }

    private void handleFileRename(FileOperateEvent fileOperateEvent) {
        String oldFile = fileOperateEvent.mOldFile.getAbsolutePath();
        String newFile = fileOperateEvent.mNewFile.getAbsolutePath();
        String s = newFile.toLowerCase();
        if (s.endsWith(DOC)||s.endsWith(DOCX)||s.endsWith(PPT)||s.endsWith(PPTX)
                ||s.endsWith(XLS)||s.endsWith(XLSX)||s.endsWith(TXT)||s.endsWith(PDF)){
            mSupport.handleFileRename(oldFile, newFile);
        }else {
            mSupport.handleFileDelete(oldFile);
        }
        refreshData(false, false);
        Logger.d(TAG, "rename   " + newFile + "      " + fileOperateEvent.mNewFile.getParent());
    }

    private void handleError() {
        Logger.d(TAG, "some thing wrong");
    }

    public void setDocScanListener(DocScanListener scanListener) {
        this.mDocScanListener = scanListener;
    }

    public class ScanDocFileTask extends ZAsyncTask<Boolean, Integer, Boolean> {

        private static final int DEPTH_THRESHOLD = 3;
        private ArrayList<DocChildBean> mDocList = new ArrayList<>();
        private ArrayList<DocChildBean> mTxtList = new ArrayList<>();
        private ArrayList<DocChildBean> mPdfList = new ArrayList<>();
        private int mFileCount;
        private long mFileTotalSize;

        @Override
        protected void onPreExecute() {
            mDocList.clear();
            mTxtList.clear();
            mPdfList.clear();
            if (mDocScanListener != null) {
                mDocScanListener.onScanStart();
            }
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            long lastTime = SharedPreferencesManager.getInstance(TheApplication.getAppContext()).getLong(IPreferencesIds.KEY_SCAN_DOC_TIME, 0);
            boolean shouldScan = System.currentTimeMillis() - lastTime > 5 * 60 * 1000;
            if (params[1] && shouldScan) {//重头开始扫描
                for (String path : StorageUtil.getAllExternalPaths(TheApplication.getAppContext())) {
                    File root = new File(path);
                    scanPath(root, 0);
                }
                //记录扫描时间
                SharedPreferencesManager.getInstance(TheApplication.getAppContext()).commitLong(IPreferencesIds.KEY_SCAN_DOC_TIME, System.currentTimeMillis());
            } else {//取数据库
                mDocList = mSupport.getDocFileInfo();
                mTxtList = mSupport.getTextFileInfo();
                mPdfList = mSupport.getPdfFileInfo();
            }

            sortResult(mDocList);
            sortResult(mPdfList);
            sortResult(mTxtList);

            DocGroupBean txtGroupBean = new DocGroupBean(mTxtList, "TXT", GroupSelectBox.SelectState.NONE_SELECTED, false);
            DocGroupBean docGroupBean = new DocGroupBean(mDocList, "DOC", GroupSelectBox.SelectState.NONE_SELECTED, false);
            DocGroupBean pdfGroupBean = new DocGroupBean(mPdfList, "PDF", GroupSelectBox.SelectState.NONE_SELECTED, false);

            ArrayList<DocGroupBean> groups = new ArrayList<>();
            if (mDocList.size() > 0) {
                groups.add(txtGroupBean);
            }
            if (mPdfList.size() > 0) {
                groups.add(pdfGroupBean);
            }
            if (mTxtList.size() > 0) {
                groups.add(docGroupBean);
            }

            if (mDocScanListener != null) {
                mDocScanListener.onScanFinish(groups, params[0]);
            }

            TheApplication.getGlobalEventBus().post(new DocFileScanFinishEvent(mFileCount, mFileTotalSize));
            return null;
        }

        // 扫描传入的路径
        private void scanPath(File dir, int depth) {
            if (depth > DEPTH_THRESHOLD) return;
            if (!dir.exists() || !dir.isDirectory()) return;
            DocFileProvider docFileProvider = DocFileProvider.getInstance();
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    scanPath(file, depth + 1);
                } else {
                    String extension = FileUtil.getExtension(file.getName());
                    if (TextUtils.isEmpty(extension)) {
                        continue;
                    }
                    if (extension.equalsIgnoreCase(DOC) || extension.equalsIgnoreCase(DOCX)) {
                        DocChildBean childBean = new DocChildBean();
                        childBean.mDocSize = file.length() + "";
                        childBean.mDocName = file.getName();
                        childBean.mFileType = DocChildBean.TYPE_DOC;
                        childBean.mDocPath = file.getAbsolutePath();
                        childBean.mAddDate = file.lastModified();
                        childBean.mModifyDate = file.lastModified();
                        docFileProvider.insertDocItem(childBean);
                        mFileCount++;
                        mFileTotalSize += file.length();
                        mDocList.add(childBean);
                    } else if (extension.equalsIgnoreCase(XLS) || extension.equalsIgnoreCase(XLSX)) {
                        DocChildBean childBean = new DocChildBean();
                        childBean.mDocSize = file.length() + "";
                        childBean.mDocName = file.getName();
                        childBean.mFileType = DocChildBean.TYPE_XLS;
                        childBean.mDocPath = file.getAbsolutePath();
                        childBean.mAddDate = file.lastModified();
                        childBean.mModifyDate = file.lastModified();
                        docFileProvider.insertDocItem(childBean);
                        mFileCount++;
                        mFileTotalSize += file.length();
                        mDocList.add(childBean);
                    } else if (extension.equalsIgnoreCase(PPT) || extension.equalsIgnoreCase(PPTX)) {
                        DocChildBean childBean = new DocChildBean();
                        childBean.mDocSize = file.length() + "";
                        childBean.mDocName = file.getName();
                        childBean.mFileType = DocChildBean.TYPE_PPT;
                        childBean.mDocPath = file.getAbsolutePath();
                        childBean.mAddDate = file.lastModified();
                        childBean.mModifyDate = file.lastModified();
                        docFileProvider.insertDocItem(childBean);
                        mFileCount++;
                        mFileTotalSize += file.length();
                        mDocList.add(childBean);
                    } else if (extension.equalsIgnoreCase(TXT)) {
                        DocChildBean childBean = new DocChildBean();
                        childBean.mDocSize = file.length() + "";
                        childBean.mDocName = file.getName();
                        childBean.mFileType = DocChildBean.TYPE_TXT;
                        childBean.mDocPath = file.getAbsolutePath();
                        childBean.mAddDate = file.lastModified();
                        childBean.mModifyDate = file.lastModified();
                        mFileCount++;
                        mFileTotalSize += file.length();
                        docFileProvider.insertDocItem(childBean);
                        mTxtList.add(childBean);
                    } else if (extension.equalsIgnoreCase(PDF)) {
                        DocChildBean childBean = new DocChildBean();
                        childBean.mDocSize = file.length() + "";
                        childBean.mDocName = file.getName();
                        childBean.mFileType = DocChildBean.TYPE_PDF;
                        childBean.mDocPath = file.getAbsolutePath();
                        childBean.mAddDate = file.lastModified();
                        childBean.mModifyDate = file.lastModified();
                        mFileCount++;
                        mFileTotalSize += file.length();
                        docFileProvider.insertDocItem(childBean);
                        mPdfList.add(childBean);
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mDocScanListener != null) {
                mDocScanListener.onLoadProgress(values[0]);
            }
        }

        @Override
        protected void onCancelled() {
        }

        @Override
        protected void onPostExecute(Boolean keepCheck) {

        }
    }

    private void sortResult(ArrayList<DocChildBean> arrayList) {
        Collections.sort(arrayList, new Comparator<DocChildBean>() {
            @Override
            public int compare(DocChildBean o1, DocChildBean o2) {
                return (int) (Long.valueOf(o1.mDocSize) - Long.valueOf(o2.mDocSize));
            }
        });
    }
}
