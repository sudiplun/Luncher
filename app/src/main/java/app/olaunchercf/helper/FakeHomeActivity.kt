package app.olaunchercf.helper

import android.app.Activity
import android.os.Bundle
import app.olaunchercf.R

class FakeHomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_home)
    }
}