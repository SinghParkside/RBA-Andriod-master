package edu.uwp.appfactory.wishope.utils

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * <h1>.</h1>
 * <p>
 * </p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 04-12-2020
 */
class NetworkUtil {
    companion object {
        fun checkConnection(context: Context): Boolean {
            val connectivityManager: ConnectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        }

        /**
         * @param context Information about the application
         * @return if the app is currently running.
         */
        fun isRunning(context: Context): Boolean {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks =
                activityManager.getRunningTasks(Int.MAX_VALUE)
            for (task in tasks) {
                if (context.packageName
                        .equals(task.baseActivity!!.packageName, ignoreCase = true)
                ) return true
            }
//            val activityManager =
//                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//            val runningProcesses =
//                activityManager.runningAppProcesses
//            for (processInfo in runningProcesses) {
//                if (processInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
//                    for (activeProcess in processInfo.pkgList) {
//                        if (activeProcess.toLowerCase() == context.packageName.toLowerCase()) {
//                            //If your app is the process in foreground, then it's not in running in background
//                            return false
//                        }
//                    }
//                }
//            }
            return false
        }
    }
}