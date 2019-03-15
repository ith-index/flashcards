package com.example.flashcards

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import java.lang.Exception

private val updatedDeckListeners: MutableList<(Deck) -> Unit> = mutableListOf()

fun publishUpdatedDeck(deck: Deck) {
    updatedDeckListeners.forEach {
        it(deck)
    }
}

fun subscribeToUpdatedDeck(listener: (Deck) -> Unit) {
    updatedDeckListeners.add(listener)
}

fun unsubscribeToUpdatedDeck(listener: (Deck) -> Unit) {
    updatedDeckListeners.remove(listener)
}



private class MissingExtraException(key: String) : Exception("missing key: $key")
private class MissingArgumentException(key: String) : Exception("missing argument: $key")

fun Intent.getLongExtraOrThrow(key: String): Long =
    if (hasExtra(key)) {
        getLongExtra(key, -1)
    } else {
        throw MissingExtraException(key)
    }

fun Bundle.getLongArgumentOrThrow(key: String): Long =
    if (containsKey(key)) {
        getLong(key)
    } else {
        throw MissingArgumentException(key)
    }

fun <R, S, T> apply(f: (R, S) -> T, r: R): (S) -> T = {
    s -> f(r, s)
}

fun postOnMainThread(run: () -> Unit) {
    MainHandler.post(run)
}

fun postOnDataThread(run: () -> Unit) {
    DataHandler.post(run)
}

private object DataHandlerThread : HandlerThread("DATA_HANDLER_THREAD")
private object MainHandler : Handler(Looper.getMainLooper())
private object DataHandler : Handler(
    DataHandlerThread.run {
        start()
        looper
    }
)

class SimpleAdapter<Data>(
    private val recyclerView: RecyclerView,
    private val instantiateSimpleViewHolder: (ViewGroup) -> SimpleViewHolder<Data>,
    dataList: MutableList<Data> = mutableListOf()
)
    : RecyclerView.Adapter<SimpleViewHolder<Data>>()
{
    var dataList: MutableList<Data> = dataList
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun add(data: Data) {
        dataList.add(0, data)
        notifyItemInserted(0)
        recyclerView.scrollToPosition(0)
    }

    fun update(predicate: (Data) -> Boolean, data: Data) {
        val index = dataList.indexOfFirst(predicate)
        dataList[index] = data
        notifyItemChanged(index)
    }

    fun delete(deletedDataList: List<Data>) {
        dataList.removeAll(deletedDataList)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = dataList.size

    override fun onCreateViewHolder(parentViewGroup: ViewGroup, ignore: Int): SimpleViewHolder<Data> =
        instantiateSimpleViewHolder(parentViewGroup)

    override fun onBindViewHolder(simpleViewHolder: SimpleViewHolder<Data>, index: Int) {
        simpleViewHolder.bind(
            dataList[index]
        )
    }
}

abstract class SimpleViewHolder<Data>(parentViewGroup: ViewGroup, resourceLayoutID: Int) : RecyclerView.ViewHolder(
    LayoutInflater
        .from(parentViewGroup.context)
        .inflate(resourceLayoutID, parentViewGroup, false)
) {
    protected val activityContext = itemView.context
    protected val applicationContext = activityContext.applicationContext
    abstract val bind: Data.() -> Unit
}