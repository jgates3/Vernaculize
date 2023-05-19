package com.example.learningapp
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity


class Settings : AppCompatActivity() {
    private lateinit var talkspeedPrefs: SharedPreferences
    private lateinit var pitchPrefs: SharedPreferences
    private lateinit var voiceSpinner: Spinner
    private lateinit var ilSpinner: Spinner
    private lateinit var olSpinner: Spinner
    private  var selectedVoice: String = ""
    private var selectedil: String = ""
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var ilPreferences: SharedPreferences
    //var dropdown1 = findViewById<ImageButton>(R.id.spinoption)
    //var dropdown2 = findViewById<ImageButton>(R.id.spinoptions2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        talkspeedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        pitchPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        val speedBar = findViewById<SeekBar>(R.id.speedbar)
        val pitchBar = findViewById<SeekBar>(R.id.pitchbar)
        ilSpinner = findViewById(R.id.ilspinner)

        speedBar.progress = talkspeedPrefs.getInt("speed", 50)
        pitchBar.progress = pitchPrefs.getInt("pitch", 50)


        speedBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val prefeditor = talkspeedPrefs.edit()
                prefeditor.putInt("speed", progress)
                prefeditor.apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        pitchBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pref2editor = pitchPrefs.edit()
                pref2editor.putInt("pitch", progress)
                pref2editor.apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val backButton = findViewById<ImageButton>(R.id.backbutton)
        backButton.setOnClickListener {
            onBackPressed() //back button

        }
        val ilOptions = arrayOf("English", "Spanish", "Italian", "German", "Chinese") //language options
        val inputAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ilOptions)
        ilSpinner.adapter = inputAdapter

        ilPreferences = getSharedPreferences("ilPreferences", MODE_PRIVATE)
        selectedil = ilPreferences.getString("Selectedil", "").toString()
        ilSpinner.setSelection(ilOptions.indexOf(selectedil))

        ilSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedil = parent.getItemAtPosition(position).toString()
                val ileditor = ilPreferences.edit()
                ileditor.putString("Selectedil", selectedil)
                ileditor.apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        voiceSpinner = findViewById(R.id.spinner)
        val voiceOptions = arrayOf("Female 1 (UK)", "Female 2 (US)", "Female 3 (IN)", "Female 4 (ES)", "Male 1 (UK)", "Male 2 (US)", "Male 3 (IN)", "Male 4 (ES)")
        val voiceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, voiceOptions)
        voiceSpinner.adapter = voiceAdapter
        sharedPreferences = getSharedPreferences("VoicePreferences", MODE_PRIVATE)

        selectedVoice = sharedPreferences.getString("SelectedVoice", "").toString()
        voiceSpinner.setSelection(voiceOptions.indexOf(selectedVoice))

        voiceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedVoice = parent.getItemAtPosition(position).toString()
                val voiceeditor = sharedPreferences.edit()
                voiceeditor.putString("SelectedVoice", selectedVoice)
                voiceeditor.apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        //dropdown1.setOnClickListener{
           // ilSpinner.performClick() //input language selection


       //}
       //dropdown2.setOnClickListener{
         //   voiceSpinner.performClick() //voice selection

           //}
    }

}