package com.assisted.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.assisted.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set factory BEFORE super.onCreate
        supportFragmentManager.fragmentFactory = object : androidx.fragment.app.FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return when (className) {
                    HomeFragment::class.java.name -> HomeFragment()
                    CaptionFragment::class.java.name -> CaptionFragment()
                    ScanFragment::class.java.name -> ScanFragment()
                    NotesFragment::class.java.name -> NotesFragment()
                    SettingsFragment::class.java.name -> SettingsFragment()
                    SosFragment::class.java.name -> SosFragment()
                    else -> super.instantiate(classLoader, className)
                }
            }
        }
        
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started (Single-Activity)")

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment())
                    .commit()
            }

            requestRequiredPermissions()
            setupBackPressHandling()

        } catch (e: Exception) {
            Log.e("MainActivity", "CRASH in onCreate", e)
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    fun navigateTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }
}
