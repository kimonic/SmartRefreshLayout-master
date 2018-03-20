package com.scwang.smartrefresh.layout.impl;

import android.view.MotionEvent;
import android.view.View;

import com.scwang.smartrefresh.layout.api.RefreshScrollBoundary;

import static com.scwang.smartrefresh.layout.util.ScrollBoundaryUtil.canScrollDown;
import static com.scwang.smartrefresh.layout.util.ScrollBoundaryUtil.canScrollUp;

/**
 * 滚动边界
 * Created by SCWANG on 2017/7/8.
 */

public class RefreshScrollBoundaryAdapter implements RefreshScrollBoundary {

    //<editor-fold desc="Internal">
    /**触摸事件*/
    MotionEvent mActionEvent;
    /**刷新滚动边界*/
    RefreshScrollBoundary boundary;

    /**设置滚动边界状态实例,实现RefreshScrollBoundary接口的类*/
    void setRefreshScrollBoundary(RefreshScrollBoundary boundary){
        this.boundary = boundary;
    }

    /**设置触摸事件*/
    void setActionEvent(MotionEvent event) {
        mActionEvent = event;
    }
    //</editor-fold>

    //<editor-fold desc="RefreshScrollBoundary">
    /**可以下拉*/
    @Override
    public boolean canPullDown(View content) {
        if (boundary != null) {
            return boundary.canPullDown(content);
        }
        //此处导入了ScrollBoundaryUtil的静态方法,故不需要引用类名
        //import static com.scwang.smartrefresh.layout.util.ScrollBoundaryUtil.canScrollDown;
        return canScrollUp(content, mActionEvent);
    }

    @Override
    public boolean canPullUp(View content) {
        if (boundary != null) {
            return boundary.canPullUp(content);
        }
        return canScrollDown(content, mActionEvent);
    }
    //</editor-fold>
}
