package com.jb.filemanager.manager.file.task;

import com.jb.filemanager.TheApplication;
import com.jb.filemanager.util.FileUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by bill wang on 2017/6/29.
 *
 */

public class CopyFileTask {

    public static final int COPY_SUCCESS_START = 0;
    public static final int COPY_ERROR_UNKNOWN = -1;
    public static final int COPY_ERROR_NOT_ENOUGH_SPACE = -2;
    public static final int COPY_ERROR_DEST_IN_SOURCE = -3;

    private Thread mWorkerThread;
    private Listener mListener;

    private ArrayList<File> mSource;
    private String mDest;

    public CopyFileTask(ArrayList<File> source, String dest, Listener listener) {

        mListener = listener;
        mSource = new ArrayList<> (source);
        mDest = dest;
        mWorkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = false;
                if (mSource != null && mSource.size() > 0) {
                    result = true;
                    for (final File file : mSource) {
                        TheApplication.postRunOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mListener != null) {
                                    mListener.onProgressUpdate(file);
                                }
                            }
                        });
                        result = result && FileUtil.copyFileOrDirectory(file.getAbsolutePath(), mDest);

                        // TODO wait for handle duplicated
                    }
                }

                final boolean finalResult = result;
                TheApplication.postRunOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mListener != null) {
                            mListener.onPostExecute(finalResult);
                        }
                    }
                });
            }
        });
    }

    public int start() {
        int resultCode = COPY_SUCCESS_START;
        if (mWorkerThread != null) {
            int checkResult = FileUtil.checkCanPaste(mSource, mDest);

            switch (checkResult) {
                case FileUtil.PASTE_CHECK_SUCCESS:
                    mWorkerThread.start();
                    break;
                case FileUtil.PASTE_CHECK_ERROR_UNKNOWN:
                    resultCode = COPY_ERROR_UNKNOWN;
                    break;
                case FileUtil.PASTE_CHECK_ERROR_DEST_IN_SOURCE:
                    resultCode = COPY_ERROR_DEST_IN_SOURCE;
                    break;
                case FileUtil.PASTE_CHECK_ERROR_NOT_ENOUGH_SPACE:
                    resultCode = COPY_ERROR_NOT_ENOUGH_SPACE;
                    break;
                default:
                    break;
            }

        }
        return resultCode;
    }

    public interface Listener {
        void onProgressUpdate(File file);
        void onPostExecute(Boolean aBoolean);
    }
}
