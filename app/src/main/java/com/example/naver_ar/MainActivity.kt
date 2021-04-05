package com.example.naver_ar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.databinding.DataBindingUtil
import com.example.naver_ar.databinding.ActivityMainBinding
import com.google.ar.core.ArCoreApk

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        //Enable AR-related functionality on ARCore supported devices only.
        isSupportArDevice()
    }

    private fun isSupportArDevice() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            //Continue to query availability at 5Hz? while compatibility is checked in the background
            Handler().postDelayed({
                isSupportArDevice()
            }, 200)
        }

        if (availability.isSupported) {
            binding.btnAr.visibility = View.VISIBLE
            binding.btnAr.isEnabled = true
        } else {
            binding.btnAr.visibility = View.INVISIBLE
            binding.btnAr.isEnabled = false
        }
    }
}