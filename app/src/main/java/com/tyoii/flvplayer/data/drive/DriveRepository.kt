package com.tyoii.flvplayer.data.drive

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.tyoii.flvplayer.data.model.DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var driveService: Drive? = null
    private var credential: GoogleAccountCredential? = null

    fun isSignedIn(): Boolean = driveService != null

    fun initDrive(accountName: String) {
        credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_READONLY)
        ).apply {
            selectedAccountName = accountName
        }

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("FLV Player").build()
    }

    fun signOut() {
        driveService = null
        credential = null
    }

    suspend fun listFiles(folderId: String? = null): List<DriveFile> = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Not signed in")

        val query = buildString {
            if (folderId != null) {
                append("'$folderId' in parents")
            } else {
                append("'root' in parents")
            }
            append(" and trashed = false")
        }

        val result = drive.files().list()
            .setQ(query)
            .setFields("files(id,name,size,modifiedTime,mimeType,parents)")
            .setOrderBy("modifiedTime desc")
            .setPageSize(100)
            .execute()

        result.files?.map { file ->
            DriveFile(
                id = file.id,
                name = file.name,
                size = file.getSize()?.toLong() ?: 0,
                modifiedTime = file.modifiedTime?.value ?: 0,
                mimeType = file.mimeType ?: "",
                parentId = file.parents?.firstOrNull(),
                isFolder = file.mimeType == "application/vnd.google-apps.folder"
            )
        } ?: emptyList()
    }

    suspend fun searchFlvFiles(query: String = ""): List<DriveFile> = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Not signed in")

        val searchQuery = buildString {
            if (query.isNotEmpty()) {
                append("name contains '$query' and ")
            }
            append("name contains '.flv' and trashed = false")
        }

        val result = drive.files().list()
            .setQ(searchQuery)
            .setFields("files(id,name,size,modifiedTime,mimeType,parents)")
            .setOrderBy("modifiedTime desc")
            .setPageSize(50)
            .execute()

        result.files?.map { file ->
            DriveFile(
                id = file.id,
                name = file.name,
                size = file.getSize()?.toLong() ?: 0,
                modifiedTime = file.modifiedTime?.value ?: 0,
                mimeType = file.mimeType ?: "",
                parentId = file.parents?.firstOrNull(),
                isFolder = false
            )
        } ?: emptyList()
    }

    suspend fun getFileStream(fileId: String): InputStream = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Not signed in")
        drive.files().get(fileId).executeMediaAsInputStream()
    }

    /** Get direct download URL for streaming with ExoPlayer */
    fun getStreamUrl(fileId: String): String {
        return "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
    }

    fun getAccessToken(): String? {
        return credential?.token
    }
}
