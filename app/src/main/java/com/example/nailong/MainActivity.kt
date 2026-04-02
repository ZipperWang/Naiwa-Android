package com.example.nailong

import android.media.MediaPlayer
import android.os.Bundle
import android.view.TextureView
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.nailong.ui.theme.NailongTheme

class MainActivity : ComponentActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            NailongTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    var isPlaying by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { context ->
                                TextureView(context).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                            try {
                                                mediaPlayer = MediaPlayer().apply {
                                                    setDataSource(resources.openRawResourceFd(com.example.nailong.R.raw.nl))
                                                    setSurface(android.view.Surface(surface))
                                                    isLooping = true
                                                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                                    setOnPreparedListener { mp ->
                                                        mp.start()
                                                        mp.pause()
                                                    }
                                                    prepare()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean { 
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            return true 
                                        }
                                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Button(
                            onClick = {
                                mediaPlayer?.let { mp ->
                                    if (isPlaying) {
                                        mp.pause()
                                        mp.seekTo(0)
                                        isPlaying = false
                                    } else {
                                        mp.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 32.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A1A1A).copy(alpha = 0.85f)
                            )
                        ) {
                            Text(
                                text = if (isPlaying) "停止" else "循环播放",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}