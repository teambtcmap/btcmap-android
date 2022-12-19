package app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class InstrumentationTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(classLoader, TestApp::class.java.name, context)
    }
}