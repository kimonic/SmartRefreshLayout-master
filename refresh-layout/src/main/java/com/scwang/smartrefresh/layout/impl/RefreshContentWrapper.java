package com.scwang.smartrefresh.layout.impl;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.PointF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerAdapterWrapper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.Space;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import com.scwang.smartrefresh.layout.api.RefreshContent;
import com.scwang.smartrefresh.layout.api.RefreshKernel;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshScrollBoundary;
import com.scwang.smartrefresh.layout.constant.RefreshState;
import com.scwang.smartrefresh.layout.util.ScrollBoundaryUtil;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.scwang.smartrefresh.layout.util.ScrollBoundaryUtil.isTransformedTouchPointInView;

/**
 * 刷新内容包装
 * Created by SCWANG on 2017/5/26.
 */

public class RefreshContentWrapper implements RefreshContent {

    /**
     * 头部高度
     */
    private int mHeaderHeight = Integer.MAX_VALUE;
    /**
     * 底部高度
     */
    private int mFooterHeight = mHeaderHeight - 1;
    /**
     * 内容视图
     */
    private View mContentView;
    /**
     * 真正的内容视图
     */
    private View mRealContentView;
    /**
     * 壳滑动的视图
     */
    private View mScrollableView;
    /**
     * 固定的头部视图
     */
    private View mFixedHeader;
    /**
     * 固定的底部视图
     */
    private View mFixedFooter;
    /**
     * 触摸事件
     */
    private MotionEvent mMotionEvent;
    /**
     * 刷新滚动边界适配器
     */
    private RefreshScrollBoundaryAdapter mBoundaryAdapter = new RefreshScrollBoundaryAdapter();

    /**
     * 构造函数
     *
     * @param view 传入可滑动视图
     */
    public RefreshContentWrapper(View view) {
        this.mContentView = mRealContentView = view;
        this.findScrollableView(view);//查找出可滑动视图
    }

    /**
     * 构造函数--自己创建内容视图
     *
     * @param context 传入上下文
     */
    public RefreshContentWrapper(Context context) {
        this.mContentView = mRealContentView = new View(context);//新建view控件????
        this.findScrollableView(mContentView);
    }

    //<editor-fold desc="findScrollableView">

    /**
     * 查找可以滑动的视图
     *
     * @param content 内容视图
     */
    private void findScrollableView(View content) {
        //查找是否有内部可滑动视图
        mScrollableView = findScrollableViewInternal(content, true);
        //嵌套滚动父视图不是嵌套滚动子视图,查找内部可滚动视图,不可以时自身
        if (mScrollableView instanceof NestedScrollingParent
                && !(mScrollableView instanceof NestedScrollingChild)) {
            mScrollableView = findScrollableViewInternal(mScrollableView, false);
        }
        //viewpager的情况
        if (mScrollableView instanceof ViewPager) {
            wrapperViewPager((ViewPager) this.mScrollableView);
        }
        //无可滑动视图的情况
        if (mScrollableView == null) {
            mScrollableView = content;
        }
    }

    /**
     * viewpager的情况处理滑动视图
     */
    private void wrapperViewPager(final ViewPager viewPager) {
        wrapperViewPager(viewPager, null);
    }

    /**
     * viewpager的情况处理滑动视图
     */
    private void wrapperViewPager(final ViewPager viewPager, PagerPrimaryAdapter primaryAdapter) {
        //用户界面县城运行
        viewPager.post(new Runnable() {
            int count = 0;
            //主viewpager适配器
            PagerPrimaryAdapter mAdapter = primaryAdapter;

            @Override
            public void run() {
                count++;
//                viewpager的适配器
                PagerAdapter adapter = viewPager.getAdapter();
                if (adapter != null) {
                    if (adapter instanceof PagerPrimaryAdapter) {
                        if (adapter == primaryAdapter) {
                            viewPager.postDelayed(this, 500);
                        }
                    } else {
                        if (mAdapter == null) {
                            mAdapter = new PagerPrimaryAdapter(adapter);
                        } else {
                            mAdapter.wrapper(adapter);
                        }
                        mAdapter.attachViewPager(viewPager);
                    }
                } else if (count < 10) {
                    viewPager.postDelayed(this, 500);//延迟执行
                }
            }
        });
    }

    /**
     * * 查找可滑动的内部视图
     * selfable 是否必须传入true?????
     *
     * @param content  内容视图
     * @param selfable 可滑动视图可以时自己本身
     * @return 可滑动视图或者null
     */
    private View findScrollableViewInternal(View content, boolean selfable) {
        View scrollableView = null;
        //LinkedBlockingQueue是一个基于链表实现的可选容量的阻塞队列。队头的元素是插入时间最长的，
        // 队尾的元素是最新插入的。新的元素将会被插入到队列的尾部。
        // LinkedBlockingQueue的容量限制是可选的，如果在初始化时没有指定容量，那么默认使用int的最大值作为队列容量。
        //Collections.singletonList(content)--一个只包含指定对象的不可变列表
        Queue<View> views = new LinkedBlockingQueue<>(Collections.singletonList(content));
        while (!views.isEmpty() && scrollableView == null) {
            //检索并删除此队列的头部,并返回头部对象或null
            View view = views.poll();
            if (view != null) {
                //可滑动控件会自己处理滑动事件,故不需要再次查找其中的子view
                if ((selfable || view != content) && (view instanceof AbsListView
                        || view instanceof ScrollView
                        || view instanceof ScrollingView
                        || view instanceof NestedScrollingChild
                        || view instanceof NestedScrollingParent
                        || view instanceof WebView
                        || view instanceof ViewPager)) {


                    scrollableView = view;


                } else if (view instanceof ViewGroup) {


                    ViewGroup group = (ViewGroup) view;
                    for (int j = 0; j < group.getChildCount(); j++) {
                        views.add(group.getChildAt(j));//将viewgroup中的view添加到队列中
                    }


                }
            }
        }
        return scrollableView;
    }
    //</editor-fold>

    //<editor-fold desc="implements">

    /**
     * 获取可滑动视图
     */
    @NonNull
    public View getView() {
        return mContentView;
    }

    /**
     * 是嵌套滚动子视图
     */
    @Override
    public boolean isNestedScrollingChild(MotionEvent e) {
        MotionEvent event = MotionEvent.obtain(e);//复制触摸事件
        //重新定义坐标系
        event.offsetLocation(-mContentView.getLeft(), -mContentView.getTop() - mRealContentView.getTranslationY());
        boolean isNested = isNestedScrollingChild(mContentView, event);
        event.recycle();//触摸事件回收
        return isNested;
    }

    /**
     * 是嵌套滚动子视图
     */
    private boolean isNestedScrollingChild(View targetView, MotionEvent event) {
        if (targetView instanceof NestedScrollingChild
                || (Build.VERSION.SDK_INT >= 21 && targetView.isNestedScrollingEnabled())) {
            return true;
        }
        if (targetView instanceof ViewGroup && event != null) {
            ViewGroup viewGroup = (ViewGroup) targetView;
            final int childCount = viewGroup.getChildCount();
            PointF point = new PointF();
            for (int i = childCount; i > 0; i--) {
                View child = viewGroup.getChildAt(i - 1);
                if (isTransformedTouchPointInView(viewGroup, child, event.getX(), event.getY(), point)) {
                    event = MotionEvent.obtain(event);
                    event.offsetLocation(point.x, point.y);
                    return isNestedScrollingChild(child, event);//递归调用
                }
            }
        }
        return false;
    }

    /**
     * 移动微调
     */
    @Override
    public void moveSpinner(int spinner) {
        //Y方向上移动一段距离,该距离移动后会保持这个效果不变
        mRealContentView.setTranslationY(spinner);//Y方向移动视图
        if (mFixedHeader != null) {
            mFixedHeader.setTranslationY(Math.max(0, spinner));
        }
        if (mFixedFooter != null) {
            mFixedFooter.setTranslationY(Math.min(0, spinner));
        }
    }

    /**
     * 是否可以向上滑动
     */
    @Override
    public boolean canScrollUp() {
        return mBoundaryAdapter.canPullDown(mContentView);
    }

    /**
     * 是否可以向下滑动
     */
    @Override
    public boolean canScrollDown() {
        return mBoundaryAdapter.canPullUp(mContentView);
    }

    /**
     * 测量可滑动视图控件的宽度与高度
     */
    @Override
    public void measure(int widthSpec, int heightSpec) {
        mContentView.measure(widthSpec, heightSpec);
    }

    /**
     * 获取布局参数
     */
    @Override
    public ViewGroup.LayoutParams getLayoutParams() {
        return mContentView.getLayoutParams();
    }

    /**
     * 获取控件的测量宽度
     */
    @Override
    public int getMeasuredWidth() {
        return mContentView.getMeasuredWidth();
    }

    /**
     * 获取控件的测量高度
     */
    @Override
    public int getMeasuredHeight() {
        return mContentView.getMeasuredHeight();
    }

    /**
     * 控件相对于父控件的布局
     */
    @Override
    public void layout(int left, int top, int right, int bottom) {
        mContentView.layout(left, top, right, bottom);
    }

    /**
     * 获取可滑动的视图
     */
    @Override
    public View getScrollableView() {
        return mScrollableView;
    }

    /**
     * 触摸按下
     */
    @Override
    public void onActionDown(MotionEvent e) {
        mMotionEvent = MotionEvent.obtain(e);
        mMotionEvent.offsetLocation(-mContentView.getLeft(), -mContentView.getTop());
        mBoundaryAdapter.setActionEvent(mMotionEvent);
    }

    /**
     * 触摸抬起或取消
     */
    @Override
    public void onActionUpOrCancel() {
        mMotionEvent = null;
        mBoundaryAdapter.setActionEvent(null);
    }

    /**
     * 安装组件
     */
    @Override
    public void setupComponent(RefreshKernel kernel, View fixedHeader, View fixedFooter) {
        //可滑动view时recyclerview时
        if (mScrollableView instanceof RecyclerView) {

            RecyclerViewScrollComponent component = new RecyclerViewScrollComponent(kernel);
            component.attach((RecyclerView) mScrollableView);

        } else if (mScrollableView instanceof AbsListView) {//可滑动view是AbsListView时

            AbsListViewScrollComponent component = new AbsListViewScrollComponent(kernel);
            component.attach(((AbsListView) mScrollableView));

        } else if (Build.VERSION.SDK_INT >= 23 && mScrollableView != null) {//有滑动视图并且版本大于等于23

            mScrollableView.setOnScrollChangeListener(new Api23ViewScrollComponent(kernel));

        }

        if (Build.VERSION.SDK_INT >= 21
                && mScrollableView instanceof ListView
                && !(mScrollableView instanceof NestedScrollingChild)) {

            mScrollableView.setNestedScrollingEnabled(true);

        }
        //固定位置的头部底部
        if (fixedHeader != null || fixedFooter != null) {
            mFixedHeader = fixedHeader;
            mFixedFooter = fixedFooter;

            //将内容视图添加到一个framelayout中,然后该framelayout为新的内容视图
            FrameLayout frameLayout = new FrameLayout(mContentView.getContext());
            kernel.getRefreshLayout().getLayout().removeView(mContentView);
            ViewGroup.LayoutParams layoutParams = mContentView.getLayoutParams();
            frameLayout.addView(mContentView, MATCH_PARENT, MATCH_PARENT);
            kernel.getRefreshLayout().getLayout().addView(frameLayout, layoutParams);
            mContentView = frameLayout;

            if (fixedHeader != null) {
                fixedHeader.setClickable(true);
                ViewGroup.LayoutParams lp = fixedHeader.getLayoutParams();
                ViewGroup parent = (ViewGroup) fixedHeader.getParent();
                int index = parent.indexOfChild(fixedHeader);//指定视图在父控件中的位置
                parent.removeView(fixedHeader);
                lp.height = measureViewHeight(fixedHeader);
                parent.addView(new Space(mContentView.getContext()), index, lp);//Space用于在通用布局中的组件之间创建空隙??
                frameLayout.addView(fixedHeader);
            }
            if (fixedFooter != null) {
                fixedFooter.setClickable(true);
                ViewGroup.LayoutParams lp = fixedFooter.getLayoutParams();
                ViewGroup parent = (ViewGroup) fixedFooter.getParent();
                int index = parent.indexOfChild(fixedFooter);
                parent.removeView(fixedFooter);
                FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(lp);
                lp.height = measureViewHeight(fixedFooter);
                parent.addView(new Space(mContentView.getContext()), index, lp);
                flp.gravity = Gravity.BOTTOM;
                frameLayout.addView(fixedFooter, flp);
            }
        }
    }

    /**初始化头部底部的高度*/
    @Override
    public void onInitialHeaderAndFooter(int headerHeight, int footerHeight) {
        mHeaderHeight = headerHeight;
        mFooterHeight = footerHeight;
    }

    /**设置刷新滚动边界*/
    @Override
    public void setRefreshScrollBoundary(RefreshScrollBoundary boundary) {
        if (boundary instanceof RefreshScrollBoundaryAdapter) {
            mBoundaryAdapter = ((RefreshScrollBoundaryAdapter) boundary);
        } else {
            mBoundaryAdapter.setRefreshScrollBoundary(boundary);
        }
    }

    /**动画监听器,加载结束*/
    @Override
    public AnimatorUpdateListener onLoadingFinish(RefreshKernel kernel, int footerHeight, int startDelay,
                                                  Interpolator interpolator, int duration) {
        if (mScrollableView != null && kernel.getRefreshLayout().isEnableScrollContentWhenLoaded()) {
            if (mScrollableView instanceof AbsListView && Build.VERSION.SDK_INT < 19) {
                if (startDelay > 0) {
                    kernel.getRefreshLayout().getLayout()
                            .postDelayed(() -> ((AbsListView) mScrollableView).smoothScrollBy(footerHeight, duration), startDelay);
                } else {
                    ((AbsListView) mScrollableView).smoothScrollBy(footerHeight, duration);
                }
                return null;
            }
            if (!ScrollBoundaryUtil.canScrollDown(mScrollableView)) {
                return null;
            }
            return new AnimatorUpdateListener() {
                int lastValue = kernel.getSpinner();//回弹高度

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int thisValue = (int) animation.getAnimatedValue();
                    if (mScrollableView instanceof AbsListView && Build.VERSION.SDK_INT >= 19) {
                        ((AbsListView) mScrollableView).scrollListBy(thisValue - lastValue);
                    } else {
                        mScrollableView.scrollBy(0, thisValue - lastValue);
                    }
                    lastValue = thisValue;
                }
            };
//                    if (mScrollableView instanceof RecyclerView) ((RecyclerView) mScrollableView).smoothScrollBy(0, footerHeight, interpolator);
//                    else if (mScrollableView instanceof ScrollView) ((ScrollView) mScrollableView).smoothScrollBy(0, footerHeight);
//                    else if (mScrollableView instanceof AbsListView) ((AbsListView) mScrollableView).smoothScrollBy(footerHeight, duration);
//                    else {
//                        try {
//                            Method method = mScrollableView.getClass().getDeclaredMethod("smoothScrollBy", Integer.class, Integer.class);
//                            method.invoke(mScrollableView, 0, footerHeight);
//                        } catch (Exception ignored) {
//                        }
//                    }
        }
        return null;
    }
    //</editor-fold>

    //<editor-fold desc="滚动组件">
    //最低使用版本23
    @RequiresApi(api = Build.VERSION_CODES.M)
    private class Api23ViewScrollComponent implements View.OnScrollChangeListener {
        /**最后滑动时间*/
        long lastTime = 0;
        /**上一次的最后滑动时间*/
        long lastTimeOld = 0;
        /**最后y轴滑动距离*/
        int lastScrollY = 0;
        /**上一次最后y轴滑动距离*/
        int lastOldScrollY = 0;
        /**刷新内核*/
        RefreshKernel kernel;

        /**构造函数*/
        Api23ViewScrollComponent(RefreshKernel kernel) {
            this.kernel = kernel;
        }

        /**滑动改变
         * Called when the scroll position of a view changes.
         *
         * @param v The view whose scroll position has changed.
         * @param scrollX Current horizontal scroll origin.
         * @param scrollY Current vertical scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        @Override
        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            if (lastScrollY == scrollY && lastOldScrollY == oldScrollY) {
                return;
            }
//            System.out.printf("%d,%d,%d,%d\n", scrollX, scrollY, oldScrollX, oldScrollY);
            RefreshLayout layout = kernel.getRefreshLayout();
            boolean overScroll = layout.isEnableOverScrollBounce() || layout.isRefreshing() || layout.isLoading();
            if (scrollY <= 0
                    && oldScrollY > 0
                    && mMotionEvent == null
                    && lastTime - lastTimeOld > 1000
                    && overScroll
                    && layout.isEnableRefresh()) {
                //time:16000000 value:160
                final int velocity = (lastOldScrollY - oldScrollY) * 16000 / (int) ((lastTime - lastTimeOld) / 1000f);
//                    System.out.println("ValueAnimator - " + (lastTime - lastTimeOld) + " - " + velocity+"("+(lastOldScrollY - oldScrollY)+")");
                kernel.animSpinnerBounce(Math.min(velocity, mHeaderHeight));
            } else if (oldScrollY < scrollY
                    && mMotionEvent == null
                    && overScroll
                    && layout.isEnableLoadmore()) {


                if (lastTime - lastTimeOld > 1000 && !ScrollBoundaryUtil.canScrollDown(mScrollableView)) {
                    final int velocity = (lastOldScrollY - oldScrollY) * 16000 / (int) ((lastTime - lastTimeOld) / 1000f);
//                    System.out.println("ValueAnimator - " + (lastTime - lastTimeOld) + " - " + velocity+"("+(lastOldScrollY - oldScrollY)+")");
                    kernel.animSpinnerBounce(Math.max(velocity, -mFooterHeight));
                }
            }
            lastScrollY = scrollY;
            lastOldScrollY = oldScrollY;
            lastTimeOld = lastTime;
            lastTime = System.nanoTime();//纳秒时间
        }
    }

    /**AbsListView滑动控制组件*/
    private class AbsListViewScrollComponent implements AbsListView.OnScrollListener {

        /**y轴方向上的滚动距离*/
        int scrollY;
        int scrollDy;
        int lastScrolly;
        int lastScrollDy;
        /**刷新布局内核*/
        RefreshKernel kernel;
        /**稀疏数组*/
        SparseArray<ItemRecod> recordSp = new SparseArray<>(0);

        AbsListViewScrollComponent(RefreshKernel kernel) {
            this.kernel = kernel;
        }

        /**状态改变*/
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        /**正在滑动*/
        @Override
        public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            lastScrolly = scrollY;
            lastScrollDy = scrollDy;
            scrollY = getScrollY(absListView, firstVisibleItem);
            scrollDy = lastScrolly - scrollY;

            //距离顶部的拉伸距离
            final int dy = lastScrollDy + scrollDy;
            if (totalItemCount > 0) {
                RefreshLayout layout = kernel.getRefreshLayout();
                boolean overScroll = (layout.isEnableOverScrollBounce() || layout.isRefreshing() || layout.isLoading());
                if (mMotionEvent == null && dy > 0 && firstVisibleItem == 0) {//触摸事件为空,第一个显示item的位置为0,
                    if (overScroll//越界
                            && layout.isEnableRefresh()//启动刷新
                            && !ScrollBoundaryUtil.canScrollUp(absListView)) {//可以上滑
                        kernel.animSpinnerBounce(Math.min(dy, mHeaderHeight));//启动回弹动画
                    }
                } else if (dy < 0) {
                    int lastVisiblePosition = absListView.getLastVisiblePosition();
                    if (lastVisiblePosition == totalItemCount - 1 && lastVisiblePosition > 0) {
                        if (layout.isEnableLoadmore()//启动加载更多
                                && !layout.isLoadmoreFinished()//加载更多未结束
                                && layout.isEnableAutoLoadmore()//启动自动加载更多
                                && layout.getState() == RefreshState.None//刷新布局无状态
                                && !ScrollBoundaryUtil.canScrollDown(absListView)) {//不可以向下滑动
                            kernel.getRefreshLayout().autoLoadmore(0, 1);//自动加载更多
                        } else if (mMotionEvent == null &&//无触摸事件
                                overScroll && //越界
                                !ScrollBoundaryUtil.canScrollDown(absListView)) {//不可以下滑
                            kernel.animSpinnerBounce(Math.max(dy, -mFooterHeight));//回弹动画
                        }
                    }
                }
            }

        }

        /**添加依附*/
        void attach(AbsListView listView) {
            listView.setOnScrollListener(this);
        }

        /**获得y轴方向上的 滚动高度*/
        private int getScrollY(AbsListView view, int firstVisibleItem) {
            View firstView = view.getChildAt(0);
            if (null != firstView) {
                ItemRecod itemRecord = recordSp.get(firstVisibleItem);
                if (null == itemRecord) {
                    itemRecord = new ItemRecod();
                }
                itemRecord.height = firstView.getHeight();//控件的高度
                itemRecord.top = firstView.getTop();//控件相对于父控件的顶部距离
                recordSp.append(firstVisibleItem, itemRecord);//值加入稀疏数组

                int height = 0, lastheight = 0;
                //当前显示位置的item之前的所有有数据的view高度相加
                for (int i = 0; i < firstVisibleItem; i++) {
                    ItemRecod itemRecod = recordSp.get(i);
                    if (itemRecod != null) {
                        height += itemRecod.height;
                        lastheight = itemRecod.height;
                    } else {
                        height += lastheight;
                    }
                }
                ItemRecod itemRecod = recordSp.get(firstVisibleItem);
                if (null == itemRecod) {
                    itemRecod = new ItemRecod();
                }
                return height - itemRecod.top;//叠加高度--顶部距离
            }
            return 0;
        }

        /**包括高度与上边距的内部类*/
        class ItemRecod {
            int height = 0;
            int top = 0;
        }
    }

    /**
     * RecyclerView滑动组件,滑动时的事件处理
     */
    private class RecyclerViewScrollComponent extends RecyclerView.OnScrollListener {
        /**
         * 最后滑动位置的y坐标
         */
        int lastDy;
        /**
         * 最后的抛掷时间
         */
        long lastFlingTime;
        /**
         * 刷新布局内核
         */
        RefreshKernel kernel;

        RecyclerViewScrollComponent(RefreshKernel kernel) {
            this.kernel = kernel;
        }

        /**
         * 滑动状态改变
         */
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//            获取刷新布局
            RefreshLayout layout = kernel.getRefreshLayout();
            //RecyclerView停止滚动
            if (newState == RecyclerView.SCROLL_STATE_IDLE && mMotionEvent == null) {
                //最后抛掷的时间到当前时间的时间差
                boolean intime = System.currentTimeMillis() - lastFlingTime < 1000;
                //是否越界滚动
                boolean overScroll = layout.isEnableOverScrollBounce() || layout.isRefreshing() || layout.isLoading();
                if (lastDy < -1 && intime && overScroll && layout.isEnableRefresh()) {
                    //启动回弹动画
                    kernel.animSpinnerBounce(Math.min(-lastDy * 2, mHeaderHeight));
                } else if (layout.isEnableLoadmore()//启用加载更多
                        && !layout.isLoadmoreFinished()//加载更多未结束
                        && layout.isEnableAutoLoadmore()//启动自动加载
                        && layout.getState() == RefreshState.None) {//刷新布局无状态
//                    RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
//                    if (manager instanceof LinearLayoutManager) {
//                        LinearLayoutManager linearManager = ((LinearLayoutManager) manager);
//                        int lastVisiblePosition = linearManager.findLastVisibleItemPosition();
//                        if(lastVisiblePosition >= linearManager.getItemCount() - 1){
//                            kernel.getRefreshLayout().autoLoadmore(0,1);
//                        }启用自动加载
//                    }
                } else if (lastDy > 1 && intime && overScroll && layout.isEnableLoadmore()) {
                    kernel.animSpinnerBounce(Math.max(-lastDy * 2, -mFooterHeight));//启动回弹
                }
                lastDy = 0;
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            //最后滑动位置的y坐标
            lastDy = dy;
            //获取刷新布局
            RefreshLayout layout = kernel.getRefreshLayout();
            if (dy > 0
                    && layout.isEnableLoadmore()//启用加载更多
                    && !layout.isLoadmoreFinished()//加载更多未结束
                    && layout.isEnableAutoLoadmore()//启用自动加载
                    && layout.getState() == RefreshState.None) {//刷新布局无状态
                //获取RecyclerView的布局管理器
                RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
                //如果时线性布局管理器
                if (manager instanceof LinearLayoutManager) {
                    LinearLayoutManager linearManager = ((LinearLayoutManager) manager);
                    //最后显示的位置
                    int lastVisiblePosition = linearManager.findLastVisibleItemPosition();

                    //自动加载
                    if (lastVisiblePosition >= linearManager.getItemCount() - 1//最后位置
                            && lastVisiblePosition > 0//有item
                            && !ScrollBoundaryUtil.canScrollDown(recyclerView)) {//不能下滑
                        kernel.getRefreshLayout().autoLoadmore(0, 1);
                    }
                }
            }
        }

        /**
         * RecyclerView注册监听器
         */
        void attach(RecyclerView recyclerView) {
            recyclerView.addOnScrollListener(this);
            recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
                @Override
                public boolean onFling(int velocityX, int velocityY) {
                    lastFlingTime = System.currentTimeMillis();
                    return false;
                }
            });
        }
    }
    //</editor-fold>

    //<editor-fold desc="private">

    /**
     * 测量控件的高度
     */
    private static int measureViewHeight(View view) {
        ViewGroup.LayoutParams p = view.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        }
        int childHeightSpec;
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
        if (p.height > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(p.height, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        view.measure(childWidthSpec, childHeightSpec);
        return view.getMeasuredHeight();
    }
    //</editor-fold>


    /**
     * 主viewpager适配器
     */
    private class PagerPrimaryAdapter extends PagerAdapterWrapper {
        private ViewPager mViewPager;

        /**
         * 构造函数
         */
        PagerPrimaryAdapter(PagerAdapter wrapped) {
            super(wrapped);
        }

        void wrapper(PagerAdapter adapter) {
            wrapped = adapter;
        }

        /**
         * 附加viewpager
         */
        @Override
        public void attachViewPager(ViewPager viewPager) {
            mViewPager = viewPager;
            super.attachViewPager(viewPager);
        }

        /**
         * 设置viewpager观察者
         */
        @Override
        public void setViewPagerObserver(DataSetObserver observer) {
            super.setViewPagerObserver(observer);
            if (observer == null) {
                wrapperViewPager(mViewPager, this);
            }
        }

        /**
         * 设置主item
         */
        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            if (object instanceof View) {
                mScrollableView = ((View) object);
            } else if (object instanceof Fragment) {
                mScrollableView = ((Fragment) object).getView();
            }
            if (mScrollableView != null) {
                mScrollableView = findScrollableViewInternal(mScrollableView, true);
                if (mScrollableView instanceof NestedScrollingParent
                        && !(mScrollableView instanceof NestedScrollingChild)) {
                    mScrollableView = findScrollableViewInternal(mScrollableView, false);
                }
            }
        }
    }
}
