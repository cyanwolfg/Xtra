package com.github.exact7.xtra.repository.datasource

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.github.exact7.xtra.repository.LoadingState
import com.github.exact7.xtra.util.nullIfEmpty

abstract class BasePageKeyedDataSource<T> : PageKeyedDataSource<String, T>(), PagingDataSource {

    protected val tag: String = javaClass.simpleName
    private var retry: (() -> Any)? = null

    override val loadingState = MutableLiveData<LoadingState>()
    override val pagingState = MutableLiveData<LoadingState>()

    override fun retry() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
//            retryExecutor.execute { it.invoke() }
        }
    }

    override fun loadInitial(params: PageKeyedDataSource.LoadInitialParams<String>, callback: PageKeyedDataSource.LoadInitialCallback<String, T>) {
        Log.d(tag, "Loading data. Size: " + params.requestedLoadSize)
        loadingState.postValue(LoadingState.LOADING)
    }

    override fun loadAfter(params: PageKeyedDataSource.LoadParams<String>, callback: PageKeyedDataSource.LoadCallback<String, T>) {
        Log.d(tag, "Loading data. Size: " + params.requestedLoadSize)
        pagingState.postValue(LoadingState.LOADING)
    }

    override fun loadBefore(params: PageKeyedDataSource.LoadParams<String>, callback: PageKeyedDataSource.LoadCallback<String, T>) {
    }

    protected fun PageKeyedDataSource.LoadInitialCallback<String, T>.onSuccess(data: List<T>, cursor: String) {
        this.onResult(data, 0, data.size, null, cursor.nullIfEmpty())
        Log.d(tag, "Successfully loaded data")
        loadingState.postValue(LoadingState.LOADED)
        retry = null
    }

    protected fun PageKeyedDataSource.LoadCallback<String, T>.onSuccess(data: List<T>, cursor: String) {
        this.onResult(data, cursor.nullIfEmpty())
        Log.d(tag, "Successfully loaded data")
        pagingState.postValue(LoadingState.LOADED)
        retry = null
    }

    protected fun PageKeyedDataSource.LoadInitialCallback<String, T>.onFailure(t: Throwable, params: PageKeyedDataSource.LoadInitialParams<String>) {
        Log.e(tag, "Error finished data: ${t.message}")
        t.printStackTrace()
        retry = { loadInitial(params, this) }
        loadingState.postValue(LoadingState.FAILED)
    }

    protected fun PageKeyedDataSource.LoadCallback<String, T>.onFailure(t: Throwable, params: PageKeyedDataSource.LoadParams<String>) {
        Log.e(tag, "Error finished data: ${t.message}")
        t.printStackTrace()
        retry = { loadAfter(params, this) }
        pagingState.postValue(LoadingState.FAILED)
    }
}