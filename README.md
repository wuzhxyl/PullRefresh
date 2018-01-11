# PullRefresh
> 下拉刷新

## 前言

主要是加深理解Android的事件派发顺序和时机

## PullRefreshLinearLayout

- * 继承LinearLayout实现的下拉刷新
- * 第二个元素为实现LinearLayoutManager的RecyclerView

## PullRefreshActivity

- * 同PullRefreshLinearLayout，原理相同+代码复用.
- * 可作为基类使用

## TestActivity

- * 基于PullRefreshActivity的Demo.
