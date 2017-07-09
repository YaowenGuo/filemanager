package com.jb.filemanager.function.samefile;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.graphics.Color;
import android.view.ViewGroup;

/**
 * Created by gavin
 * View悬浮
 * 利用分割线实现悬浮
 */


public class StickyDecoration extends RecyclerView.ItemDecoration {
    @ColorInt
    private int mGroupBackground = Color.parseColor("#00000000");//group背景色，默认灰色

    private GroupListener mGroupListener;

    private int mGroupHeight = 80;  //悬浮栏高度
    private boolean isAlignLeft = true; //是否靠左边
    private Paint mGroutPaint;

    private StickyDecoration(GroupListener groupListener) {
        this.mGroupListener = groupListener;
        //设置悬浮栏的画笔---mGroutPaint
        mGroutPaint = new Paint();
        mGroutPaint.setColor(mGroupBackground);
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int pos = parent.getChildAdapterPosition(view);
        String groupId = getGroupName(pos);
        if (groupId == null) return;
        //只有是同一组的第一个才显示悬浮栏
        if (pos == 0 || isFirstInGroup(pos)) {
            outRect.top = mGroupHeight;
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        int itemCount = state.getItemCount();
        int childCount = parent.getChildCount();
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        String preGroupName;
        String currentGroupName = null;
        for (int i = 0; i < childCount; i++) {
            View view = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(view);
            preGroupName = currentGroupName;
            currentGroupName = getGroupName(position);
            if (currentGroupName == null || TextUtils.equals(currentGroupName, preGroupName))
                continue;
            int viewBottom = view.getBottom();
            int top = Math.max(mGroupHeight, view.getTop());//top 决定当前顶部第一个悬浮Group的位置
            if (position + 1 < itemCount) {
                //获取下个GroupName
                String nextGroupName = getGroupName(position + 1);
                //下一组的第一个View接近头部
                if (!currentGroupName.equals(nextGroupName) && viewBottom < top) {
                    top = viewBottom;
                }
            }
            c.drawRect(left, top - mGroupHeight, right, top, mGroutPaint);
            //根据position获取View
            View groupView = getGroupView(position);
            if (groupView == null) return;
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(right, mGroupHeight);
            groupView.setLayoutParams(layoutParams);
            groupView.setDrawingCacheEnabled(true);
            groupView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            //指定高度、宽度的groupView
            groupView.layout(0, 0, right, mGroupHeight);
            groupView.buildDrawingCache();
            l("groupView.getWidth() after: " + groupView.getWidth());
            Bitmap bitmap = groupView.getDrawingCache();
            int marginLeft = isAlignLeft ? 0 : right - groupView.getMeasuredWidth();
            c.drawBitmap(bitmap, left + marginLeft, top - mGroupHeight, null);
        }
    }

    /**
     * 判断是不是组中的第一个位置
     * 根据前一个组名，判断当前是否为新的组
     */
    private boolean isFirstInGroup(int pos) {
        if (pos == 0) {
            return true;
        } else {
            String prevGroupId = getGroupName(pos - 1);
            String groupId = getGroupName(pos);
            return !TextUtils.equals(prevGroupId, groupId);
        }
    }

    /**
     * 获取组名
     *
     * @param position position
     * @return 组名
     */
    private String getGroupName(int position) {
        if (mGroupListener != null) {
            return mGroupListener.getGroupName(position);
        } else {
            return null;
        }
    }

    /**
     * 获取组View
     *
     * @param position position
     * @return 组名
     */
    private View getGroupView(int position) {
        if (mGroupListener != null) {
            return mGroupListener.getGroupView(position);
        } else {
            return null;
        }
    }

    public static class Builder {
        StickyDecoration mDecoration;

        private Builder(GroupListener listener) {
            mDecoration = new StickyDecoration(listener);
        }

        public static Builder init(GroupListener listener) {
            return new Builder(listener);
        }

        /**
         * 设置Group高度
         * @param groutHeight 高度
         * @return this
         */
        public Builder setGroupHeight(int groutHeight) {
            mDecoration.mGroupHeight = groutHeight;
            return this;
        }


        /**
         * 设置Group背景
         *
         * @param background 背景色
         */
        public Builder setGroupBackground(@ColorInt int background) {
            mDecoration.mGroupBackground = background;
            mDecoration.mGroutPaint.setColor(mDecoration.mGroupBackground);
            return this;
        }

        /**
         * 是否靠左边
         * true 靠左边（默认）、false 靠右边
         * @param b b
         * @return  this
         */
        public Builder isAlignLeft(boolean b){
            mDecoration.isAlignLeft = b;
            return this;
        }

        public StickyDecoration build() {
            return mDecoration;
        }
    }

    private void l(String message) {
        Log.i("TAG", message);
    }
}
