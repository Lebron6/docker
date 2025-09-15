package com.aros.apron.tools

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {

    /**
     * 在 UI 线程执行操作
     */
    fun launchOnUI(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch { block() }
    }

    /**
     * 在 io 线程执行操作
     */
    fun <T> launchOnIO(block: suspend CoroutineScope.() -> T) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
        }
    }

}