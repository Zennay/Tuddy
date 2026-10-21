package com.livelingo.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livelingo.android.ui.LiveLingoScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LiveLingoApp() }
    }
}

@Composable
private fun LiveLingoApp(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val requestMicrophone = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) vm.start() }
    // Stop even when app merely goes into the background (Activity is not disposed).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.stop()
        }
    }
    LiveLingoScreen(
        state = vm.state,
        speechAvailable = vm.speechAvailable,
        canAutoSwitchVoice = vm.canAutoSwitchVoice,
        onPrepare = vm::prepare,
        onSelectTarget = vm::selectTarget,
        onPeekWord = vm::peekWord,
        onDismissPeek = vm::dismissWord,
        onStop = vm::stop,
        onClear = vm::clear,
        onStart = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) vm.start()
            else requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)
        },
    )
}
