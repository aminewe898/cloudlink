package com.cloudlink.app.data.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.cloudlink.app.domain.repository.ServerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class SftpTransferService : Service() {

    @Inject
    lateinit var sshManager: SshConnectionManager

    @Inject
    lateinit var serverRepository: ServerRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val transferMutex = Mutex()

    companion object {
        const val ACTION_DOWNLOAD = "ACTION_DOWNLOAD"
        const val ACTION_UPLOAD = "ACTION_UPLOAD"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val EXTRA_SERVER_ID = "EXTRA_SERVER_ID"
        const val EXTRA_REMOTE_PATH = "EXTRA_REMOTE_PATH"
        const val EXTRA_LOCAL_URI = "EXTRA_LOCAL_URI"
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "sftp_transfers"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_CANCEL) {
            serviceJob.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (action == ACTION_DOWNLOAD || action == ACTION_UPLOAD) {
            val serverId = intent.getIntExtra(EXTRA_SERVER_ID, -1)
            val remotePath = intent.getStringExtra(EXTRA_REMOTE_PATH)
            val localUriStr = intent.getStringExtra(EXTRA_LOCAL_URI)

            if (serverId != -1 && remotePath != null && localUriStr != null) {
                startForeground(NOTIFICATION_ID, buildNotification("Starting transfer..."))
                executeTransfer(action, serverId, remotePath, localUriStr, startId)
            } else {
                stopSelfResult(startId)
            }
        } else {
            stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    private fun executeTransfer(
        action: String,
        serverId: Int,
        remotePath: String,
        localUriStr: String,
        startId: Int
    ) {
        serviceScope.launch {
            transferMutex.withLock {
              var sftpChannel: com.jcraft.jsch.ChannelSftp? = null
              try {
                // Ensure connection
                val server = serverRepository.getServerById(serverId)
                if (server != null) {
                    val connection = sshManager.connect(server)
                    if (connection.isFailure) throw connection.exceptionOrNull() ?: IllegalStateException("Connection failed")
                    val channel = sshManager.getSftpChannel(serverId)
                    sftpChannel = channel

                    if (channel != null) {
                        val fileName = remotePath.substringAfterLast("/")

                        // Use a custom SftpProgressMonitor
                        val monitor = object : com.jcraft.jsch.SftpProgressMonitor {
                            var max = 0L
                            var count = 0L
                            override fun init(op: Int, src: String?, dest: String?, max: Long) {
                                this.max = max
                            }

                            override fun count(count: Long): Boolean {
                                this.count += count
                                val progress = if (max > 0) (this.count * 100 / max).toInt() else 0
                                updateNotification("Transferring $fileName", progress)
                                return true // return false to cancel
                            }

                            override fun end() {}
                        }

                        val localUri = localUriStr.toUri()
                        val contentResolver = applicationContext.contentResolver

                        if (action == ACTION_DOWNLOAD) {
                            val outputStream = contentResolver.openOutputStream(localUri)
                                ?: error("The selected download destination could not be opened.")
                            outputStream.use {
                                channel.get(remotePath, it, monitor)
                                it.flush()
                            }
                            updateNotification("Download complete: $fileName", 100, true)
                        } else if (action == ACTION_UPLOAD) {
                            var uploadFileName = "uploaded_file"
                            contentResolver.query(localUri, null, null, null, null)?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (cursor.moveToFirst() && nameIndex >= 0) {
                                    uploadFileName = cursor.getString(nameIndex)
                                        .replace("/", "_")
                                        .replace("\\", "_")
                                        .take(255)
                                        .ifBlank { "uploaded_file" }
                                }
                            }
                            val finalRemotePath = if (remotePath.endsWith("/")) "$remotePath$uploadFileName" else "$remotePath/$uploadFileName"

                            val exists = runCatching { channel.lstat(finalRemotePath) }.isSuccess
                            require(!exists) { "A remote file named $uploadFileName already exists." }
                            val inputStream = contentResolver.openInputStream(localUri)
                                ?: error("The selected upload file could not be opened.")
                            inputStream.use {
                                channel.put(it, finalRemotePath, monitor)
                            }
                            updateNotification("Upload complete: $uploadFileName", 100, true)
                        }
                    } else {
                        error("Unable to open an SFTP channel.")
                    }
                } else {
                    error("Server not found.")
                }
              } catch (e: Exception) {
                updateNotification("Transfer Failed: ${e.message}", 0, true)
              } finally {
                sftpChannel?.runCatching { disconnect() }
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelfResult(startId)
              }
            }
        }
    }

    private fun buildNotification(text: String, progress: Int = 0): Notification {
        val cancelIntent = Intent(this, SftpTransferService::class.java).setAction(ACTION_CANCEL)
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CloudLink SFTP Transfer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download) // Use default for now
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, "Cancel", cancelPendingIntent)

        if (progress in 1..99) {
            builder.setProgress(100, progress, false)
        } else if (progress == 0) {
            builder.setProgress(0, 0, true) // Indeterminate
        }

        return builder.build()
    }

    private fun updateNotification(text: String, progress: Int, finished: Boolean = false) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CloudLink SFTP Transfer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (!finished) {
            builder.setOngoing(true)
            if (progress in 1..99) {
                builder.setProgress(100, progress, false)
            } else {
                builder.setProgress(0, 0, true)
            }
        } else {
            builder.setOngoing(false)
            builder.setProgress(0, 0, false)
        }

        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SFTP Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background file transfers"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
