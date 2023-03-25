package com.refing.tmdbbrowserapp.core.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.refing.tmdbbrowserapp.core.data.source.remote.network.ApiResponse
import com.refing.tmdbbrowserapp.core.utils.AppExecutors
import kotlinx.coroutines.flow.*

abstract class NetworkBoundResource<ResultType, RequestType>(private val mExecutors: AppExecutors) {


    private var result: Flow<Resource<ResultType>> = flow {
        emit(Resource.Loading())
        val dbSource = loadFromDB().first()
        if (shouldFetch(dbSource)) {
            emit(Resource.Loading())
            when (val apiResponse = createCall().first()) {
                is ApiResponse.Success -> {
                    saveCallResult(apiResponse.data)
                    emitAll(loadFromDB().map { Resource.Success(it) })
                }
                is ApiResponse.Empty -> {
                    emitAll(loadFromDB().map { Resource.Success(it) })
                }
                is ApiResponse.Error -> {
                    onFetchFailed()
                    emit(Resource.Error<ResultType>(apiResponse.errorMessage))
                }
            }
        } else {
            emitAll(loadFromDB().map { Resource.Success(it) })
        }
    }

    protected open fun onFetchFailed() {}

    protected abstract fun loadFromDB(): Flow<ResultType>

    protected abstract fun shouldFetch(data: ResultType?): Boolean

    protected abstract suspend fun createCall(): Flow<ApiResponse<RequestType>>

    protected abstract suspend fun saveCallResult(data: RequestType)



    fun asFlow(): Flow<Resource<ResultType>> = result
}