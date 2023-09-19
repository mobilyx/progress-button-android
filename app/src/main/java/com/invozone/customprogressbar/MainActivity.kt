package com.invozone.customprogressbar

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.invozone.customprogressbar.databinding.ActivityMainBinding
import com.invozone.progress_button.CustomProgressBar


class MainActivity : AppCompatActivity(),View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private val THREAD_DELAYED_TIME = 300
    private var mCounter = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewWithListener(R.id.animated_cloud_progressbar1)
    }

    private fun initViewWithListener(@IdRes vararg animated_cloud_progressbarIds: Int) {
        for (animated_cloud_progressbarId in animated_cloud_progressbarIds) findViewById<View>(
            animated_cloud_progressbarId
        ).setOnClickListener(this)
    }

    private fun startProgressDemo(progressBar: CustomProgressBar) {
        if (mCounter == -1) {
            progressBar.postDelayed(Runnable {
                progressBar.clearAnimation()
                progressBar.post(getProgressRunnable(progressBar, mCounter))
            }, 10)
        }
    }

    private fun getProgressRunnable(
        progressBar: CustomProgressBar, progress: Int
    ): Runnable? {
        return object : Runnable {
            override fun run() {
                // This condition not needed for real scenario. It just used for demo
                if (mCounter <= 100 && progress <= progressBar.getMaximumProgress()) {
                    progressBar.setProgress(progress)
                    Log.e("###############", "mCounter: $mCounter")
                    mCounter++
                    progressBar.postDelayed(
                        getProgressRunnable(progressBar, mCounter),
                        THREAD_DELAYED_TIME.toLong()
                    )
                } else {
                    mCounter = -1
                    progressBar.reset()
                    progressBar.removeCallbacks(this)
                    Log.e("***************", "mCounter: $mCounter")
                }
            }
        }
    }

    override fun onClick(v: View) {
        startProgressDemo(v as CustomProgressBar)
    }
}