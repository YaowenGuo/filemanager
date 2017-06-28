package com.jb.filemanager.database.params;

import android.content.ContentValues;

/**
 * SQL插入参数
 *
 * 类名称：InstertParams
 * 类描述：
 * 创建人：makai
 * 修改人：makai
 * 修改时间：2014年9月5日 下午4:20:05
 * 修改备注：
 * @version 1.0.0
 *
 */
public class InsertParams {

    private String mTableName;

    private ContentValues mContentValues;

    public InsertParams(String mTableName, ContentValues mContentValues) {
        this.mTableName = mTableName;
        this.mContentValues = mContentValues;
    }

    public String getTableName() {
        return mTableName;
    }

    public ContentValues getContentValues() {
        return mContentValues;
    }

    @Override
    public String toString() {
        String buffer = "mTableName : " + mTableName +
                " mContentValues : " + mContentValues.toString();
        return buffer;
    }
}
