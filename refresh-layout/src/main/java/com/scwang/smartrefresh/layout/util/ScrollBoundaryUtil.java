package com.scwang.smartrefresh.layout.util;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

/**
 * 滚动边界--判断视图是否可以上下滑动
 * Created by SCWANG on 2017/7/8.
 */

public class ScrollBoundaryUtil {

    //<editor-fold desc="滚动判断">
    /**可以向上滚动*/
    public static boolean canScrollUp(View targetView, MotionEvent event) {
        //直接判断该view是否可向上滑动
        if (canScrollUp(targetView)) {
            return true;
        }
        //如果该view为viewgroup时,且不可滑动,则递归判断当前的子view中是否有壳向上滑动的view
        if (targetView instanceof ViewGroup && event != null) {
            ViewGroup viewGroup = (ViewGroup) targetView;
            final int childCount = viewGroup.getChildCount();
            PointF point = new PointF();
            for (int i = childCount; i > 0; i--) {
                View child = viewGroup.getChildAt(i - 1);
                if (isTransformedTouchPointInView(viewGroup,child, event.getX(), event.getY() , point)) {
                    event = MotionEvent.obtain(event);//复制触摸事件
                    event.offsetLocation(point.x, point.y);//设置触摸事件偏移量
                    return canScrollUp(child, event);//递归调用
                }
            }
        }
        return false;
    }

    /**可以向上滚动*/
    public static boolean canScrollUp(View targetView) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (targetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) targetView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return targetView.getScrollY() > 0;
            }
        } else {
            return targetView.canScrollVertically(-1);
        }
    }

    /**
     * 可以向下滚动
     */
    public static boolean canScrollDown(View targetView, MotionEvent event) {
        if (canScrollDown(targetView)) {
            return true;
        }
        if (targetView instanceof ViewGroup && event != null) {
            ViewGroup viewGroup = (ViewGroup) targetView;
            final int childCount = viewGroup.getChildCount();
            PointF point = new PointF();
            for (int i = 0; i < childCount; i++) {
                View child = viewGroup.getChildAt(i);
                if (isTransformedTouchPointInView(viewGroup,child, event.getX(), event.getY() , point)) {
                    event = MotionEvent.obtain(event);
                    event.offsetLocation(point.x, point.y);
                    return canScrollDown(child, event);
                }
            }
        }
        return false;
    }

    /**当前view可以向下滚动*/
    public static boolean canScrollDown(View targetView) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (targetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) targetView;
                return absListView.getChildCount() > 0
                        && (absListView.getLastVisiblePosition() < absListView.getChildCount() - 1
                        || absListView.getChildAt(absListView.getChildCount() - 1).getBottom() > absListView.getPaddingBottom());
            } else {
                return targetView.getScrollY() < 0;
            }
        } else {
            return targetView.canScrollVertically(1);
        }
    }

    //</editor-fold>

    //<editor-fold desc="transform Point">


    /**判断给出坐标点是否在当前view的坐标系中*/
    public static boolean pointInView(View view, float localX, float localY, float slop) {
        final float left = /*Math.max(view.getPaddingLeft(), 0)*/ - slop;
        final float top = /*Math.max(view.getPaddingTop(), 0)*/ - slop;
        final float width = view.getWidth()/* - Math.max(view.getPaddingLeft(), 0) - Math.max(view.getPaddingRight(), 0)*/;
        final float height = view.getHeight()/* - Math.max(view.getPaddingTop(), 0) - Math.max(view.getPaddingBottom(), 0)*/;
        return localX >= left && localY >= top && localX < ((width) + slop) &&
                localY < ((height) + slop);
    }

    /**判断当前触摸点是否在可滑动的view内*/
    public static boolean isTransformedTouchPointInView(ViewGroup group, View child, float x, float y,PointF outLocalPoint) {
        final float[] point = new float[2];
        point[0] = x;
        point[1] = y;
        //将当前触摸点转为为view的本地坐标系坐标点
        transformPointToViewLocal(group, child, point);
        final boolean isInView = pointInView(child, point[0], point[1], 0);
        if (isInView && outLocalPoint != null) {
            //还原坐标系坐标
            outLocalPoint.set(point[0]-x, point[1]-y);
        }
        return isInView;
    }

    /**转化给出的坐标点为view的本地坐标系中的点*/
    public static void transformPointToViewLocal(ViewGroup group, View child, float[] point) {
        point[0] += group.getScrollX() - child.getLeft();
        point[1] += group.getScrollY() - child.getTop();
    }
    //</editor-fold>
}
