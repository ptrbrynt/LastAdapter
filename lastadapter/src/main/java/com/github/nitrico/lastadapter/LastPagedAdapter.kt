package com.github.nitrico.lastadapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.OnRebindCallback
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class LastPagedAdapter<T : Any>(lifecycleOwner: LifecycleOwner, list: LiveData<PagedList<T>>, private val variable: Int? = null, stableIds: Boolean = false, diffCallback: DiffUtil.ItemCallback<T> = defaultDiffCallback()) : PagedListAdapter<T, Holder<ViewDataBinding>>(diffCallback) {

    private val dataInvalidation = Any()
    private val map = mutableMapOf<Class<*>, BaseType>()

    private var layoutHandler: LayoutHandler? = null
    private var typeHandler: TypeHandler? = null

    private var recyclerView: RecyclerView? = null
    private var inflater: LayoutInflater? = null

    init {
        setHasStableIds(stableIds)

        list.observe(lifecycleOwner, Observer {
            submitList(it)
        })

    }

    fun <T> map(clazz: Class<T>, type: AbsType<*>) = apply {
        map[clazz] = type
    }

    inline fun <reified T> map(type: AbsType<*>) = map(T::class.java, type)

    fun into(recyclerView: RecyclerView) = apply { recyclerView.adapter = this }

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): Holder<ViewDataBinding> {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(inflater!!, viewType, view, false)
        val holder = Holder(binding)
        binding.addOnRebindCallback(object : OnRebindCallback<ViewDataBinding>() {
            override fun onPreBind(binding: ViewDataBinding) = recyclerView?.isComputingLayout
                    ?: false

            override fun onCanceled(binding: ViewDataBinding) {
                if (recyclerView?.isComputingLayout != false) {
                    return
                }
                val position = holder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position, dataInvalidation)
                }
            }
        })
        return holder
    }

    override fun onBindViewHolder(holder: Holder<ViewDataBinding>, position: Int) {
        val type = getType(position)
        if (type != null) {
            holder.binding.setVariable(getVariable(type), getItem(position))
            holder.binding.executePendingBindings()
            @Suppress("UNCHECKED_CAST")
            if (type is AbsType<*>) {
                if (!holder.created) {
                    notifyCreate(holder, type as AbsType<ViewDataBinding>)
                }
                notifyBind(holder, type as AbsType<ViewDataBinding>)
            }
        }
    }

    override fun onBindViewHolder(holder: Holder<ViewDataBinding>, position: Int, payloads: List<Any>) {
        if (isForDataBinding(payloads)) {
            holder.binding.executePendingBindings()
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewRecycled(holder: Holder<ViewDataBinding>) {
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION && position < itemCount) {
            val type = getType(position)!!
            if (type is AbsType<*>) {
                @Suppress("UNCHECKED_CAST")
                notifyRecycle(holder, type as AbsType<ViewDataBinding>)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return if (hasStableIds()) {
            val item = getItem(position)
            when {
                item is StableId -> item.stableId
                item != null -> throw IllegalStateException("${item.javaClass.simpleName} must implement StableId interface.")
                else -> super.getItemId(position)
            }
        } else {
            super.getItemId(position)
        }
    }

    override fun onAttachedToRecyclerView(rv: RecyclerView) {
        recyclerView = rv
        inflater = LayoutInflater.from(rv.context)
    }

    override fun onDetachedFromRecyclerView(rv: RecyclerView) {
        recyclerView = null
    }


    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item != null) {
            layoutHandler?.getItemLayout(item, position)
                    ?: typeHandler?.getItemType(item, position)?.layout
                    ?: getType(position)?.layout
                    ?: throw RuntimeException("Invalid object at position $position: ${item.javaClass}")
        } else super.getItemViewType(position)
    }

    private fun isForDataBinding(payloads: List<Any>): Boolean {
        if (payloads.isEmpty()) {
            return false
        }
        payloads.forEach {
            if (it != dataInvalidation) {
                return false
            }
        }
        return true
    }

    private fun getType(position: Int): BaseType? {
        val item = getItem(position)
        return if (item != null) {
            typeHandler?.getItemType(item, position)
                    ?: map[item.javaClass]
        } else null
    }

    private fun getVariable(type: BaseType) = type.variable
            ?: variable
            ?: throw IllegalStateException("No variable specified for type ${type.javaClass.simpleName}")

    private fun notifyCreate(holder: Holder<ViewDataBinding>, type: AbsType<ViewDataBinding>) {
        when (type) {
            is Type -> {
                setClickListeners(holder, type)
                type.onCreate?.invoke(holder)
            }
            is ItemType -> type.onCreate(holder)
        }
        holder.created = true
    }

    private fun notifyBind(holder: Holder<ViewDataBinding>, type: AbsType<ViewDataBinding>) {
        when (type) {
            is Type -> type.onBind?.invoke(holder)
            is ItemType -> type.onBind(holder)
        }
    }

    private fun notifyRecycle(holder: Holder<ViewDataBinding>, type: AbsType<ViewDataBinding>) {
        when (type) {
            is Type -> type.onRecycle?.invoke(holder)
            is ItemType -> type.onRecycle(holder)
        }
    }

    private fun setClickListeners(holder: Holder<ViewDataBinding>, type: Type<ViewDataBinding>) {
        val onClick = type.onClick
        if (onClick != null) {
            holder.itemView.setOnClickListener {
                onClick(holder)
            }
        }
        val onLongClick = type.onLongClick
        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener {
                onLongClick(holder)
                true
            }
        }
    }

    companion object {

        fun <T> defaultDiffCallback() = object : DiffUtil.ItemCallback<T>() {
            override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
            override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem == newItem
        }

    }

}