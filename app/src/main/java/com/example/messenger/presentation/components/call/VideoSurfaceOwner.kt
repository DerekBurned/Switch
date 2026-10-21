package com.example.messenger.presentation.components.call

import androidx.annotation.MainThread
import com.example.messenger.presentation.components.call.model.VideoStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VideoSurfaceOwner {

    enum class Host(val priority: Int) {
        FLOATING_TILE(1),
        FULLSCREEN(2),
        SYSTEM_PIP(3),
    }

    private val mounted = mutableMapOf<VideoStream, MutableMap<Host, Int>>()

    private val _owners = MutableStateFlow<Map<VideoStream, Host>>(emptyMap())
    val owners: StateFlow<Map<VideoStream, Host>> = _owners.asStateFlow()

    @MainThread
    fun mount(host: Host, stream: VideoStream) {
        val hosts = mounted.getOrPut(stream) { mutableMapOf() }
        hosts[host] = (hosts[host] ?: 0) + 1
        recompute()
    }

    @MainThread
    fun unmount(host: Host, stream: VideoStream) {
        val hosts = mounted[stream] ?: return
        val remaining = (hosts[host] ?: 0) - 1
        if (remaining <= 0) hosts.remove(host) else hosts[host] = remaining
        if (hosts.isEmpty()) mounted.remove(stream)
        recompute()
    }

    @MainThread
    fun ownerOf(stream: VideoStream): Host? = _owners.value[stream]

    @MainThread
    fun reset() {
        mounted.clear()
        recompute()
    }

    private fun recompute() {
        _owners.value = buildMap {
            mounted.forEach { (stream, hosts) ->
                hosts.keys.maxByOrNull { it.priority }?.let { put(stream, it) }
            }
        }
    }
}
