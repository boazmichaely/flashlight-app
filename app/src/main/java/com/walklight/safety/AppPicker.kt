package com.walklight.safety

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class AppPicker(private val fragment: Fragment) {

    private lateinit var prefs: SharedPreferences
    
    // Callback interface to notify when app is selected
    interface AppSelectionCallback {
        fun onAppSelected(packageName: String, className: String, appName: String)
    }
    
    private var callback: AppSelectionCallback? = null

    // Launcher to handle the result of the app picker
    private val pickAppLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("APP_PICKER", "🔍 === KOTLIN APP PICKER RESULT ===")
        Log.d("APP_PICKER", "Result code: ${result.resultCode}")
        Log.d("APP_PICKER", "RESULT_OK = $RESULT_OK")
        Log.d("APP_PICKER", "Data is null: ${result.data == null}")
        
        if (result.resultCode == RESULT_OK && result.data != null) {
            val component = result.data!!.component
            Log.d("APP_PICKER", "📱 Component: $component")
            
            if (component != null) {
                val pkg = component.packageName
                val cls = component.className
                
                Log.d("APP_PICKER", "✅ SUCCESS: Kotlin picker got component")
                Log.d("APP_PICKER", "✅ Package: $pkg")
                Log.d("APP_PICKER", "✅ Class: $cls")
                
                // Store in SharedPreferences
                prefs.edit().putString("companion_app_package", pkg)
                    .putString("companion_app_class", cls)
                    .apply()
                
                                // Get app name using multiple approaches
                var appName = pkg // fallback to package name
                try {
                    val pm = fragment.requireContext().packageManager

                    // Try approach 1: Get application info with different flags
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0) // Use 0 instead of GET_META_DATA
                        appName = pm.getApplicationLabel(appInfo).toString()
                        Log.d("APP_PICKER", "✅ App name via ApplicationInfo: $appName")
                    } catch (e1: Exception) {
                        Log.d("APP_PICKER", "⚠️ ApplicationInfo failed, trying LauncherActivity approach...")
                        
                        // Try approach 2: Get activity info for launcher activity
                        try {
                            val component = ComponentName(pkg, cls)
                            val activityInfo = pm.getActivityInfo(component, 0) // Use 0 instead of GET_META_DATA
                            appName = pm.getActivityInfo(component, 0).loadLabel(pm).toString()
                            Log.d("APP_PICKER", "✅ App name via ActivityInfo: $appName")
                        } catch (e2: Exception) {
                            Log.d("APP_PICKER", "⚠️ ActivityInfo failed, trying installed packages...")
                            
                            // Try approach 3: Search through all installed packages
                            try {
                                val installedApps = pm.getInstalledApplications(0) // Use 0 to get ALL apps including user apps
                                // Search through installed apps for fallback
                                for (app in installedApps) {
                                    if (app.packageName == pkg) {
                                        try {
                                            val label = pm.getApplicationLabel(app).toString()
                                            appName = label
                                            Log.d("APP_PICKER", "✅ App name via installed apps: $appName")
                                            break
                                        } catch (e: Exception) {
                                            Log.w("APP_PICKER", "⚠️ Found app but couldn't get label: ${e.message}")
                                        }
                                    }
                                }
                            } catch (e3: Exception) {
                                Log.w("APP_PICKER", "⚠️ Exception in installed apps search: ${e3.message}")
                            }
                        }
                    }
                    
                    prefs.edit().putString("companion_app_name", appName).apply()
                    Log.d("APP_PICKER", "✅ FINAL STORED: $appName ($pkg)")
                    
                    // Notify callback
                    callback?.onAppSelected(pkg, cls, appName)
                    
                } catch (e: Exception) {
                    Log.e("APP_PICKER", "❌ Error getting app info", e)
                    // Still store with package name as fallback
                    prefs.edit().putString("companion_app_name", pkg).apply()
                    callback?.onAppSelected(pkg, cls, pkg)
                }
            } else {
                Log.w("APP_PICKER", "⚠️ No component in result data")
            }
        } else {
            Log.d("APP_PICKER", "❌ User cancelled or result not OK")
        }
        
        Log.d("APP_PICKER", "🔍 === END KOTLIN APP PICKER RESULT ===")
    }

    init {
        prefs = fragment.requireContext().getSharedPreferences("walklight_settings", Context.MODE_PRIVATE)
    }
    
    fun setCallback(callback: AppSelectionCallback) {
        this.callback = callback
    }

    fun openAppPicker() {
        Log.d("APP_PICKER", "🚀 Kotlin openAppPicker() called")
        
        // Native approach from other developer: ACTION_PICK_ACTIVITY
        // Base intent: use launcher intent to show all launchable apps (including Spotify)
        val base = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Now wrap it in the native "pick activity" intent
        val picker = Intent(Intent.ACTION_PICK_ACTIVITY).apply {
            putExtra(Intent.EXTRA_INTENT, base)
            putExtra(Intent.EXTRA_TITLE, "Choose a companion app")
        }
        
        Log.d("APP_PICKER", "🚀 Launching ACTION_PICK_ACTIVITY picker: $picker")
        pickAppLauncher.launch(picker)
    }

    fun getSavedApp(): Triple<String?, String?, String?> {
        val pkg = prefs.getString("companion_app_package", null)
        val cls = prefs.getString("companion_app_class", null)
        val name = prefs.getString("companion_app_name", null)
        return Triple(pkg, cls, name)
    }
}
