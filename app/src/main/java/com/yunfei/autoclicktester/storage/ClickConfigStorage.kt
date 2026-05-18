package com.yunfei.autoclicktester.storage

import com.yunfei.autoclicktester.model.ClickConfig

object ClickConfigStorage {
    private var config = ClickConfig()

    fun get(): ClickConfig = config

    fun save(newConfig: ClickConfig) {
        config = newConfig
    }
}
