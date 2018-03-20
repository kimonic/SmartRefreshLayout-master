package com.scwang.smartrefresh.layout.constant;

public enum RefreshState {
    /**无状态*/
    None,
    /**下拉刷新*/
    PullDownToRefresh,
    /**上拉加载*/
    PullToUpLoad,
    /**下拉取消*/
    PullDownCanceled,
    /**上拉取消*/
    PullUpCanceled,
    /**释放刷新*/
    ReleaseToRefresh,
    /**释放加载*/
    ReleaseToLoad,
    /**正在刷新*/
    Refreshing,
    /**正在加载*/
    Loading,
    /**刷新结束*/
    RefreshFinish,
    /**加载结束*/
    LoadingFinish,
}