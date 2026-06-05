package org.jiangstack.mytavern.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jiangstack.mytavern.data.local.AppDatabase
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val TAG = "BackupRepository"
private const val BACKUP_DB_NAME = "mytavern.db"
private const val BACKUP_DATASTORE_NAME = "user_preferences.preferences_pb"
private const val BACKUP_DATASTORE_DIR = "datastore"
private const val BACKUP_METADATA_NAME = "metadata.json"
private const val BACKUP_AVATAR_MAP_NAME = "avatar_map.json"
private const val BACKUP_AVATAR_DIR = "avatars"

class BackupRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    private val json = Json { prettyPrint = true }

    // region Export

    suspend fun exportToZip(outputUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 0. Read avatar URIs before closing database
            val characters = database.characterDao().getAll().first()
            val avatarUriMap = characters.mapNotNull { char ->
                char.avatarUri?.let { char.id to it }
            }.toMap()

            val tempDir = File(context.cacheDir, "backup_export_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                // 1. Close database to ensure WAL is flushed
                database.close()

                // 2. Copy database files
                val dbFile = context.getDatabasePath(BACKUP_DB_NAME)
                val dbShmFile = File(dbFile.parent, "$BACKUP_DB_NAME-shm")
                val dbWalFile = File(dbFile.parent, "$BACKUP_DB_NAME-wal")

                dbFile.copyTo(File(tempDir, BACKUP_DB_NAME), overwrite = true)
                if (dbShmFile.exists()) {
                    dbShmFile.copyTo(File(tempDir, "$BACKUP_DB_NAME-shm"), overwrite = true)
                }
                if (dbWalFile.exists()) {
                    dbWalFile.copyTo(File(tempDir, "$BACKUP_DB_NAME-wal"), overwrite = true)
                }

                // 3. Copy DataStore file
                val dataStoreDir = File(context.filesDir.parent, BACKUP_DATASTORE_DIR)
                val dataStoreFile = File(dataStoreDir, BACKUP_DATASTORE_NAME)
                if (dataStoreFile.exists()) {
                    dataStoreFile.copyTo(File(tempDir, BACKUP_DATASTORE_NAME), overwrite = true)
                }

                // 4. Export avatars
                val avatarMap = mutableMapOf<Long, String>()
                val avatarDir = File(tempDir, BACKUP_AVATAR_DIR)
                avatarDir.mkdirs()

                avatarUriMap.forEach { (charId, uriStr) ->
                    try {
                        val uri = Uri.parse(uriStr)
                        val ext = getExtensionFromUri(uri) ?: "jpg"
                        val fileName = "avatar_${charId}.$ext"
                        val outFile = File(avatarDir, fileName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (outFile.exists() && outFile.length() > 0) {
                            avatarMap[charId] = fileName
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to export avatar for character $charId: ${e.message}")
                    }
                }

                // 5. Write metadata
                val metadata = BackupMetadata(
                    appVersion = getAppVersionName(),
                    appVersionCode = getAppVersionCode(),
                    dbVersion = 9, // Current Room database version
                    createdAt = System.currentTimeMillis()
                )
                File(tempDir, BACKUP_METADATA_NAME)
                    .writeText(json.encodeToString(BackupMetadata.serializer(), metadata))

                // 6. Write avatar map
                if (avatarMap.isNotEmpty()) {
                    File(tempDir, BACKUP_AVATAR_MAP_NAME)
                        .writeText(json.encodeToString(AvatarMap.serializer(), AvatarMap(avatarMap)))
                }

                // 7. Pack ZIP
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    ZipOutputStream(BufferedOutputStream(output)).use { zos ->
                        tempDir.listFiles()?.forEach { file ->
                            addFileToZip(zos, file, "")
                        }
                    }
                } ?: throw IllegalStateException("Cannot open output stream for URI")

                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(metadata.createdAt))
                "备份成功：$formattedDate"
            } finally {
                tempDir.deleteRecursively()
            }
        }.onFailure { e ->
            Log.e(TAG, "Export failed", e)
        }
    }

    // endregion

    // region Import

    suspend fun importFromZip(zipUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val tempDir = File(context.cacheDir, "backup_import_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                // 1. Extract ZIP
                context.contentResolver.openInputStream(zipUri)?.use { input ->
                    ZipInputStream(BufferedInputStream(input)).use { zis ->
                        var entry: ZipEntry?
                        while (zis.nextEntry.also { entry = it } != null) {
                            val entryFile = File(tempDir, entry!!.name)
                            if (entry!!.isDirectory) {
                                entryFile.mkdirs()
                            } else {
                                entryFile.parentFile?.mkdirs()
                                entryFile.outputStream().use { output ->
                                    zis.copyTo(output)
                                }
                            }
                            zis.closeEntry()
                        }
                    }
                } ?: throw IllegalStateException("Cannot open input stream for URI")

                // 2. Validate metadata
                val metadataFile = File(tempDir, BACKUP_METADATA_NAME)
                if (!metadataFile.exists()) {
                    throw IllegalStateException("备份文件无效：缺少 metadata.json")
                }
                val metadata = json.decodeFromString(
                    BackupMetadata.serializer(),
                    metadataFile.readText()
                )
                if (metadata.dbVersion > 9) {
                    throw IllegalStateException(
                        "备份文件版本(${metadata.dbVersion})高于当前应用支持版本，请升级应用后重试"
                    )
                }

                // 3. Validate database file
                val backupDbFile = File(tempDir, BACKUP_DB_NAME)
                if (!backupDbFile.exists()) {
                    throw IllegalStateException("备份文件无效：缺少数据库文件")
                }

                // 4. Close current database
                database.close()

                // 5. Backup current data (for rollback)
                val dbFile = context.getDatabasePath(BACKUP_DB_NAME)
                val rollbackDir = File(
                    context.cacheDir,
                    "backup_rollback_${System.currentTimeMillis()}"
                )
                rollbackDir.mkdirs()
                val rollbackDbFile = File(rollbackDir, BACKUP_DB_NAME)
                if (dbFile.exists()) {
                    dbFile.copyTo(rollbackDbFile, overwrite = true)
                }
                val dataStoreDir = File(context.filesDir.parent, BACKUP_DATASTORE_DIR)
                val dataStoreFile = File(dataStoreDir, BACKUP_DATASTORE_NAME)
                val rollbackDataStoreFile = File(rollbackDir, BACKUP_DATASTORE_NAME)
                if (dataStoreFile.exists()) {
                    dataStoreFile.copyTo(rollbackDataStoreFile, overwrite = true)
                }

                try {
                    // 6. Replace database files
                    dbFile.parentFile?.mkdirs()
                    backupDbFile.copyTo(dbFile, overwrite = true)
                    val backupShm = File(tempDir, "$BACKUP_DB_NAME-shm")
                    val backupWal = File(tempDir, "$BACKUP_DB_NAME-wal")
                    if (backupShm.exists()) {
                        backupShm.copyTo(
                            File(dbFile.parent, "$BACKUP_DB_NAME-shm"),
                            overwrite = true
                        )
                    } else {
                        File(dbFile.parent, "$BACKUP_DB_NAME-shm").delete()
                    }
                    if (backupWal.exists()) {
                        backupWal.copyTo(
                            File(dbFile.parent, "$BACKUP_DB_NAME-wal"),
                            overwrite = true
                        )
                    } else {
                        File(dbFile.parent, "$BACKUP_DB_NAME-wal").delete()
                    }

                    // 7. Replace DataStore file
                    dataStoreDir.mkdirs()
                    val backupDataStore = File(tempDir, BACKUP_DATASTORE_NAME)
                    if (backupDataStore.exists()) {
                        backupDataStore.copyTo(dataStoreFile, overwrite = true)
                    }

                    // 8. Restore avatars using raw SQLite to avoid Room migration issues
                    val avatarMapFile = File(tempDir, BACKUP_AVATAR_MAP_NAME)
                    if (avatarMapFile.exists()) {
                        val avatarMap = json.decodeFromString(
                            AvatarMap.serializer(),
                            avatarMapFile.readText()
                        ).map
                        val backupAvatarDir = File(tempDir, BACKUP_AVATAR_DIR)
                        val appAvatarDir = File(context.filesDir, "avatars")
                        appAvatarDir.mkdirs()

                        // Clean old avatars
                        appAvatarDir.listFiles()?.forEach { it.delete() }

                        // Open database directly with SQLiteDatabase
                        SQLiteDatabase.openDatabase(
                            dbFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READWRITE
                        ).use { sqliteDb ->
                            avatarMap.forEach { (charId, fileName) ->
                                val backupAvatarFile = File(backupAvatarDir, fileName)
                                if (backupAvatarFile.exists()) {
                                    val appAvatarFile = File(appAvatarDir, fileName)
                                    backupAvatarFile.copyTo(appAvatarFile, overwrite = true)
                                    val newUri = appAvatarFile.toURI().toString()
                                    sqliteDb.execSQL(
                                        "UPDATE characters SET avatarUri = ? WHERE id = ?",
                                        arrayOf(newUri, charId.toString())
                                    )
                                }
                            }
                        }
                    }

                    // 9. Clean rollback on success
                    rollbackDir.deleteRecursively()

                    "数据导入成功，请重启应用以完成恢复"
                } catch (e: Exception) {
                    // Rollback on failure
                    Log.e(TAG, "Import failed, rolling back", e)
                    if (rollbackDbFile.exists()) {
                        rollbackDbFile.copyTo(dbFile, overwrite = true)
                    }
                    if (rollbackDataStoreFile.exists()) {
                        rollbackDataStoreFile.copyTo(dataStoreFile, overwrite = true)
                    }
                    throw e
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }.onFailure { e ->
            Log.e(TAG, "Import failed", e)
        }
    }

    // endregion

    // region Helpers

    private fun addFileToZip(zos: ZipOutputStream, file: File, parentPath: String) {
        val entryName = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
        if (file.isDirectory) {
            zos.putNextEntry(ZipEntry("$entryName/"))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(zos, child, entryName)
            }
        } else {
            zos.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    private fun getExtensionFromUri(uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
    }

    private fun getAppVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun getAppVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (_: Exception) {
            0
        }
    }

    // endregion
}

// region Data Classes

@kotlinx.serialization.Serializable
private data class BackupMetadata(
    val appVersion: String,
    val appVersionCode: Int,
    val dbVersion: Int,
    val createdAt: Long
)

@kotlinx.serialization.Serializable
private data class AvatarMap(
    val map: Map<Long, String>
)

// endregion
