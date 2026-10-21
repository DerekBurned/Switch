package com.example.messenger.testutil

import com.example.messenger.data.local.obx.MyObjectBox
import io.objectbox.Box
import io.objectbox.BoxStore
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.nio.file.Files

class ObjectBoxExtension : BeforeEachCallback, AfterEachCallback {

    lateinit var store: BoxStore
    private lateinit var directory: File

    override fun beforeEach(context: ExtensionContext) {
        directory = Files.createTempDirectory("obx").toFile()
        store = MyObjectBox.builder().directory(directory).build()
    }

    override fun afterEach(context: ExtensionContext) {
        store.close()
        BoxStore.deleteAllFiles(directory)
        directory.delete()
    }

    inline fun <reified T> box(): Box<T> = store.boxFor(T::class.java)
}
