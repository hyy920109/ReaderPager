package com.hyy.scrolldemo

import android.app.Application
import com.didichuxing.doraemonkit.DoraemonKit

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
       // DoraemonKit.install(this)
    }
}