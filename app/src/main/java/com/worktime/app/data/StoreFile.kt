package com.worktime.app.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json

/**
 * 单个 JSON 文件的读写：原子落盘、损坏留档。
 * 不依赖 Android API，便于直接写 JVM 单测。
 */
class StoreFile(
    private val file: File,
    private val json: Json,
) {

    sealed interface ReadResult {
        data object Empty : ReadResult
        data class Ok(val store: Store) : ReadResult
        data class Corrupt(val archive: File?) : ReadResult
        data class Failed(val cause: Throwable) : ReadResult
    }

    fun read(): ReadResult {
        if (!file.isFile) return ReadResult.Empty
        return try {
            ReadResult.Ok(json.decodeFromString(Store.serializer(), file.readText()))
        } catch (e: java.io.IOException) {
            // 文件本身读不了（权限、IO），别动它，交给上层决定是否允许写入。
            ReadResult.Failed(e)
        } catch (e: Exception) {
            // 内容是坏的：先留档，再把 data.json 让出来，避免下次保存直接覆盖。
            ReadResult.Corrupt(archiveCorrupt())
        }
    }

    /** 写临时文件 → fsync → 原子改名；失败时旧文件保持不动。 */
    fun write(value: Store): Boolean {
        val dir = file.parentFile ?: return false
        val tmp = File(dir, file.name + ".tmp")
        return try {
            if (!dir.exists() && !dir.mkdirs()) return false
            FileOutputStream(tmp).use { out ->
                out.write(json.encodeToString(Store.serializer(), value).toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.io.IOException) {
                // 少数文件系统/平台不支持“原子覆盖”，退回到带替换的普通移动。
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            true
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }

    private fun archiveCorrupt(): File? {
        val dir = file.parentFile ?: return null
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .format(LocalDateTime.now())
        val target = File(dir, "data.corrupt-$stamp.json")
        return when {
            file.renameTo(target) -> target
            else -> runCatching { file.copyTo(target, overwrite = false) }.getOrNull()
        }
    }
}
