package com.station.stationdownloader.data.source.local.engine.aria2.connection.profile

import android.content.Context
import android.util.Base64
import android.util.Log
import com.gianlu.aria2lib.commonutils.Prefs
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.local.engine.aria2.connection.common.Aria2UserPK
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

class UserProfileManager(
    val context: Context,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val profilesPath: File = context.filesDir

    suspend fun getInAppProfile(): UserProfile {
        val lastProfile = getLastProfile()?.let {
            if (it.isInAppProfile())
                it
            else
                null
        }
        if (lastProfile != null)
            return lastProfile

        val profileIds = getProfileIds()
        for (id in profileIds) {
            val profile = retrieveProfile(id)
            if (profile.isInAppProfile()) {
                setLastProfile(profile)
                return profile
            }
        }

        val profile = UserProfile.forInAppDownloader()
        save(profile)
        setLastProfile(profile)
        return profile
    }

    private fun getProfileIds(): Array<String> {
        val profiles = profilesPath.listFiles { dir: File?, name: String ->
            name.endsWith(
                ".profile"
            )
        } ?: return emptyArray()
        val idList = mutableListOf<String>()
        for (i in profiles.indices) {
            val file = profiles[i]
            var id = file.name.substring(0, file.name.length - 8)
            if (id.contains("+")) {
                id = id.replace('+', '-')
                if (!file.renameTo(File(file.parentFile, "$id.profile"))) Log.e(
                    "UserProfileManager",
                    "Failed renaming profile: $id"
                )
            }
            idList.add(id)
        }
        return idList.toTypedArray()
    }

    private fun profileExists(id: String): Boolean {
        val file = File(profilesPath, "$id.profile")
        return file.exists() && file.canRead()
    }

    @Throws(IOException::class, JSONException::class)
    private suspend fun retrieveProfile(id: String): UserProfile = withContext(ioDispatcher) {
        if (!profileExists(id)) throw FileNotFoundException("Profile $id doesn't exists!")
        val reader = BufferedReader(
            InputStreamReader(
                FileInputStream(
                    File(
                        profilesPath,
                        "$id.profile"
                    )
                )
            )
        )
        val builder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) builder.append(line)
        return@withContext UserProfile(JSONObject(builder.toString()))
    }

    private suspend fun getLastProfile(): UserProfile? {
        val id: String = Prefs.getString(Aria2UserPK.LAST_USED_PROFILE, null) ?: return null
        return try {
            retrieveProfile(id)
        } catch (ex: Exception) {
            Logger.e(ex, "Failed getting profile $id")
            null
        }
    }


    private fun setLastProfile(profile: UserProfile) {
        Prefs.putString(Aria2UserPK.LAST_USED_PROFILE, profile.id)
    }

    fun unsetLastProfile() {
        Prefs.remove(Aria2UserPK.LAST_USED_PROFILE)
    }

    @Throws(IOException::class, JSONException::class)
    private suspend fun save(profile: UserProfile) = withContext(ioDispatcher) {
        val file = File(profilesPath, profile.id + ".profile")
        FileOutputStream(file).use { out ->
            out.write(profile.toJson().toString().toByteArray())
            out.flush()
        }
    }

    companion object {
        @JvmStatic
        fun getId(name: String): String {
            return Base64.encodeToString(name.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        }
    }

}

