package com.example.springbreak

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import android.speech.RecognizerIntent
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import android.content.Context
import android.media.MediaPlayer

class MainActivity : AppCompatActivity() {

    lateinit var edittext: EditText
    private val REQUEST_CODE_SPEECH_INPUT = 1
    private var lan_string = "en-US"
    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var lastAcceleration = SensorManager.GRAVITY_EARTH

    companion object {
        private const val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edittext = findViewById(R.id.edittext)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        Objects.requireNonNull(sensorManager)!!.registerListener(sensorListener, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH

        // Ref: https://www.geeksforgeeks.org/spinner-in-kotlin/
        // access the items of the list
        val languages = resources.getStringArray(R.array.Languages)
        // access the spinner
        val spinner = findViewById<Spinner>(R.id.spinner)
        if (spinner != null) {
            val adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, languages)
            spinner.adapter = adapter
            spinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    when(languages[position]){
                        "English" -> lan_string = "en-US"
                        "Mandarin" -> lan_string = "zh-Hant-MO"
                        "French" -> lan_string = "fr"
                        "Japanese" -> lan_string = "ja"
                        "Russian" -> lan_string = "ru"
                        "Spanish" -> lan_string = "es"
                    }
                    // Ref: https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), MY_PERMISSIONS_REQUEST_RECORD_AUDIO)
                    }
                    else {
                        // intent setting
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        intent.putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        intent.putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE,
                            lan_string
                        )
                        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")
                        if (intent.resolveActivity(packageManager) == null) {
                            Toast.makeText(this@MainActivity, "Your device does not support speech input", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            try {
                                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
                            } catch (e: Exception) {
                                Toast
                                    .makeText(this@MainActivity, " " + e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val res: ArrayList<String> = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                edittext.setText(
                    Objects.requireNonNull(res)[0]
                )
            }
            else {
                Log.e("SpeechToText", "Recognition was not successful")
                Toast.makeText(this@MainActivity, "Fail on Voice Recognition ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Ref: https://www.tutorialspoint.com/how-to-detect-shake-events-in-kotlin
    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            if (acceleration > 10) {
                Toast.makeText(applicationContext, "Shake event detected", Toast.LENGTH_SHORT).show()
                Log.d("ShakeDetector", "Shake detected: acceleration = $acceleration")
                openMapForLanguage(lan_string)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }
    override fun onResume() {
        sensorManager?.registerListener(sensorListener, sensorManager!!.getDefaultSensor(
            Sensor .TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
        )
        super.onResume()
    }
    override fun onPause() {
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }

    private fun openMapForLanguage(language: String) {
        val geoUri: String = when (language) {
            "en-US" -> "geo:40.7128,-74.0060"     // New York
            "fr" -> "geo:48.8566,2.3522"          // Paris
            "zh-Hant-MO" -> "geo:25.0330,121.5654"// Taipei
            "ja" -> "geo:35.6895,139.6917"        // Tokyo
            "ru" -> "geo:55.7558,37.6173"         // Moscow
            "es" -> "geo:21.1619,-86.8515"        // Cancun
            else -> "geo:0,0"
        }
        Log.d("GeoUri", "Opening Maps with URI: $geoUri")

        val voiceMsg = when (language) {
            "en-US" -> R.raw.hello_en
            "fr" -> R.raw.hello_fr
            "zh-Hant-MO" -> R.raw.hello_ch
            "ja" -> R.raw.hello_ja
            "ru" -> R.raw.hello_ra
            "es" -> R.raw.hello_es
            else -> null
        }

        //Ref: https://www.geeksforgeeks.org/mediaplayer-class-in-android/
        voiceMsg?.let {
            val mediaPlayer = MediaPlayer.create(this, voiceMsg)
            mediaPlayer.start()
        }

        val intentgeo = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(geoUri))
        intentgeo.setPackage("com.google.android.apps.maps")
        if (intentgeo.resolveActivity(packageManager) != null) {
            startActivity(intentgeo)
        }
    }
}