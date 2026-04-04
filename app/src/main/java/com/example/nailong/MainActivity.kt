package com.example.nailong

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Surface
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
    private var textureView: TextureView? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    
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
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    textureView = this
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                            Log.d("Nailong", "Surface available: ${width}x${height}")
                                            surfaceWidth = width
                                            surfaceHeight = height
                                            initMediaPlayer(surface)
                                        }
                                        
                                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                                            surfaceWidth = width
                                            surfaceHeight = height
                                            adjustTextureView()
                                        }
                                        
                                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                            Log.d("Nailong", "Surface destroyed")
                                            return true
                                        }
                                        
                                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
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

    private fun initMediaPlayer(surface: SurfaceTexture) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val rawFd = resources.openRawResourceFd(R.raw.nl)
                setDataSource(rawFd.fileDescriptor, rawFd.startOffset, rawFd.length)
                setSurface(Surface(surface))
                isLooping = true
                setOnPreparedListener { mp ->
                    Log.d("Nailong", "MediaPlayer prepared, video size: ${mp.videoWidth}x${mp.videoHeight}")
                    adjustTextureView()
                    mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("Nailong", "MediaPlayer error: what=$what, extra=$extra")
                    false
                }
                setOnVideoSizeChangedListener { _, width, height ->
                    Log.d("Nailong", "Video size changed: ${width}x${height}")
                    adjustTextureView()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("Nailong", "Failed to init MediaPlayer", e)
        }
    }

    private fun adjustTextureView() {
        val mp = mediaPlayer ?: return
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        
        if (videoWidth == 0 || videoHeight == 0 || surfaceWidth == 0 || surfaceHeight == 0) return
        
        Log.d("Nailong", "Adjusting: view=${surfaceWidth}x${surfaceHeight}, video=${videoWidth}x${videoHeight}")
        
        val viewRatio = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
        
        val scaleX: Float
        val scaleY: Float
        
        if (viewRatio > videoRatio) {
            scaleX = viewRatio / videoRatio
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = videoRatio / viewRatio
        }
        
        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY)
        matrix.postTranslate(
            (surfaceWidth - surfaceWidth * scaleX) / 2f,
            (surfaceHeight - surfaceHeight * scaleY) / 2f
        )
        
        textureView?.setTransform(matrix)
        
        Log.d("Nailong", "Transform applied: scaleX=$scaleX, scaleY=$scaleY")
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Nailong", "Activity destroyed")
        mediaPlayer?.release()
        mediaPlayer = null
        textureView = null
    }
}
