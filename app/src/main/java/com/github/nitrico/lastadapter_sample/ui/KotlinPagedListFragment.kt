package com.github.nitrico.lastadapter_sample.ui

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PositionalDataSource
import com.github.nitrico.lastadapter.LastPagedAdapter
import com.github.nitrico.lastadapter.Type
import com.github.nitrico.lastadapter_sample.BR
import com.github.nitrico.lastadapter_sample.R
import com.github.nitrico.lastadapter_sample.data.Car
import com.github.nitrico.lastadapter_sample.databinding.ItemCarBinding
import com.github.nitrico.lastadapter_sample.databinding.ItemIntBinding
import java.util.*

class KotlinPagedListFragment : ListFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        LastPagedAdapter(this, items, BR.item)
                .map<Int>(Type<ItemIntBinding>(R.layout.item_int))
                .map<Car>(Type<ItemCarBinding>(R.layout.item_car))
                .into(list)

    }

    private val items = LivePagedListBuilder<Int, Any>(
            TheDataSourceFactory(),
            PagedList.Config.Builder()
                    .setPageSize(20)
                    .setEnablePlaceholders(false)
                    .build())
            .build()

    private class TheDataSource : PositionalDataSource<Any>() {

        private val random = Random()

        override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Any>) {
            val list = ArrayList<Any>()
            for (item in 0 until params.requestedLoadSize) {
                list.add(if (item % 2 == 0) item else Car(random.nextLong(), "Car $item"))
            }
            assert(list.size == params.requestedLoadSize)
            callback.onResult(list, 0)
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Any>) {
            val list = ArrayList<Any>()
            for (item in params.startPosition until (params.startPosition + params.loadSize)) {
                list.add(if (item % 2 == 0) item else Car(random.nextLong(), "Car $item"))
            }
            assert(list.size == params.loadSize)
            callback.onResult(list)
        }

    }

    private class TheDataSourceFactory : DataSource.Factory<Int, Any>() {

        val sourceLiveData = MutableLiveData<TheDataSource>()

        override fun create(): DataSource<Int, Any> {
            val source = TheDataSource()
            sourceLiveData.postValue(source)
            return source
        }
    }
}