package com.hnidesu.timer.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.hnidesu.timer.R
import com.hnidesu.timer.manager.AppPrefManager
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "settings"
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            findPreference<Preference>("test_permission")?.setOnPreferenceClickListener {
                var result = false
                when(AppPrefManager.getShellProvider()){
                    "shizuku"->{
                        result = Shizuku.pingBinder()
                    }
                    "magisk"-> {
                        result = Shell.getShell().isRoot
                    }
                    else -> {
                        Toast.makeText(requireContext(),
                            getString(R.string.unknown_shell_provider),Toast.LENGTH_LONG).show()
                    }
                }
                Toast.makeText(requireContext(),
                    if(result) getString(R.string.permission_granted) else getString(R.string.permission_denied),
                    Toast.LENGTH_LONG).show()
                true
            }
        }
    }
}