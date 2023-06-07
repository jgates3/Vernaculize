package com.example.learningapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.view.View
import android.widget.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.concurrent.thread
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import org.json.JSONArray
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var userInput: EditText
    private var translatedText: String = ""
    private lateinit var ttsSource: TextToSpeech
    private var isSpeaking = false
    private var sourceText: String = ""
    private var selectedil: String = ""
    private lateinit var talkspeedPrefs: SharedPreferences
    private lateinit var pitchPrefs: SharedPreferences
    private var pitchvoice: Float = 0.0f
    private var talkspeed: Float = 0.0f
    private val barStarter = 50
    private var correctCounter = 0
    var wordCount = 0
    var counter = 0
    var hintisEnabled = false
    var giveupIsEnabled = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ttsSource = TextToSpeech(this, this, "com.google.android.tts") //tts initialization
        getRandomWord()

        talkspeedPrefs = PreferenceManager.getDefaultSharedPreferences(this) // Initialize prefs
        pitchPrefs = PreferenceManager.getDefaultSharedPreferences(this) // Initialize prefs
        talkspeed = talkspeedPrefs.getInt("speed", barStarter).toFloat()
        pitchvoice = pitchPrefs.getInt("pitch", barStarter).toFloat()/50.0f
    }

    fun getRandomWord() {
        var wordArray = ArrayList<String>()
        val ilPreferences = getSharedPreferences("ilPreferences", MODE_PRIVATE)
        selectedil = ilPreferences.getString("Selectedil", "").toString()
        var defbutton = findViewById<Button>(R.id.definitionbutton)
        var otherbutton = findViewById<Button>(R.id.otherbutton)
        var settingsbutton = findViewById<ImageButton>(R.id.settingbutton)
        val counterTextView = findViewById<TextView>(R.id.counterTextView)
        userInput = findViewById(R.id.inputEditText)
        val submitButton = findViewById<Button>(R.id.submitButton)
        val button = findViewById<Button>(R.id.ts_button)

        thread { //thread because network operations must not be in main!
            val randomWord = getRandom(selectedil) //gets a Random word with specified language
            wordCount++ //counts number of generated words
            val wcounterPrefs = getSharedPreferences("WCounterPrefs", Context.MODE_PRIVATE)
            var wcounterPrefs2 = wcounterPrefs.getInt("wcorrectCounter", 0)
            wcounterPrefs2++ //counts words and updates preferences
            val editor = wcounterPrefs.edit()
            editor.putInt("wcorrectCounter", wcounterPrefs2)
            editor.apply()

            runOnUiThread { //runOnUi is also used to help implement APIs
                val sourceText = findViewById<TextView>(R.id.ts_textView)
                val targetText = findViewById<TextView>(R.id.ts_textView2)
                val hinttext = findViewById<TextView>(R.id.hint_textView)

                sourceText.text = randomWord //displays random word to user
                targetText.text = " " //clears target text
                hinttext.text = " " //clears hint text
                var hintisEnabled = false //reset
                var giveupIsEnabled = false //reset

                var randomWord1 = sourceText.text.dropLast(2) //remove brackets
                randomWord1 = randomWord1.drop(2) //remove brakets
                sourceText.text = randomWord1 //display word
                translateString(randomWord1.toString(), selectedil) //translates randomWord to English

                otherbutton.setOnClickListener {//The give up button
                    targetText.text = translatedText
                    translateString(randomWord1.toString(), selectedil)
                    targetText.visibility = View.VISIBLE //show answer because user gave up ):
                    giveupIsEnabled = true
                    counter = 0 //reset streak counter
                    counterTextView.text = counter.toString() //update streak counter
                }
                targetText.visibility = View.GONE  // hide answer

                submitButton.setOnClickListener {//submit answer
                    val counterPrefs = getSharedPreferences("CounterPrefs", Context.MODE_PRIVATE)
                    var correctPrefs2 = counterPrefs.getInt("correctCounter", 0)

                    val userInputText = userInput.text.toString() //get the userinput
                    if (userInputText == translatedText) { //if input and answer is the same
                        getRandomWord() //generate a new word!
                        Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                        if (hintisEnabled == true|| giveupIsEnabled == true) { //user knew the word without any help!
                            counter++ //add to streak counter
                            correctCounter++ //add to number of correct words counter
                            correctPrefs2++
                            val editor = counterPrefs.edit()
                            editor.putInt("correctCounter", correctPrefs2)
                            editor.apply()
                            counterTextView.text = counter.toString()

                        }
                        else if (hintisEnabled == false || giveupIsEnabled == false) { //user uses hint or give up option
                            counter = 0 //streak is broken
                            counterTextView.text = counter.toString()
                        }
                    }

                    else { //users put wrong word
                        Toast.makeText(this, "Incorrect!", Toast.LENGTH_SHORT).show()
                        counter = 0
                        counterTextView.text = counter.toString()

                        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val wordArraySet = sharedPreferences.getStringSet("wordArray", emptySet())
                        val wordArray = if (wordArraySet != null) {
                            wordArraySet.toMutableList()
                        }
                        else {
                            mutableListOf()
                        }

                        wordArray.add(randomWord.toString()) //add randomword to word array
                        val editor = sharedPreferences.edit()
                        editor.putStringSet("wordArray", HashSet(wordArray)) //convert to hashset for easier saving
                        editor.apply()
                    }
                }
            }

        }
        settingsbutton.setOnClickListener {//settings button
            val intent = Intent(this@MainActivity, Settings::class.java)
            startActivity(intent) //goes to settings page
        }
        defbutton.setOnClickListener {//definition button

            GlobalScope.launch(Dispatchers.IO) {//used to prevent the API from being in main (AS doesn't like network stuff on main)

                fun getWordDefinition(word: String): String? {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://api.dictionaryapi.dev/api/v2/entries/en/$word") //Dictionary Url
                        .build()
                    try {
                        val response = client.newCall(request).execute() //tries to make HTTP request to get info
                        val responseBody = response.body?.string() //recieves response from HTTP request

                        val definition = parseDefinitionFromResponse(responseBody) //parse response to extract definition

                        return definition
                    }
                    catch(e: Exception) { //for cases where the word doesnt have an available definition
                        e.printStackTrace()
                        // Toast.makeText(this@MainActivity,"Definition not found!",Toast.LENGTH_SHORT).show()
                        return null //returns to null to make it print  "Definition not found!"
                    }
                }
                val definition = getWordDefinition(translatedText)
                launch(Dispatchers.Main) {//Toast messages ran on a new coroutine
                    if (definition != null) { //shows definition
                        Toast.makeText(this@MainActivity, definition, Toast.LENGTH_LONG).show()
                    }
                    else {
                        // definition could not be retrieved
                        Toast.makeText(this@MainActivity, "Definition not found", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }


    }

    fun parseDefinitionFromResponse(responseBody: String?): String? { //parses http response
        if (responseBody != null) { //response should not be null
            val jArray = JSONArray(responseBody)
            val jArray_length = jArray.length()
            if (jArray_length > 0) { //length isnt 0
                val jsonEntry = jArray.getJSONObject(0) //gets first element in jsonArray
                val jsonMeaning = jsonEntry.getJSONArray("meanings").getJSONObject(0) //gets meanings
                val jsonDefinition = jsonMeaning.getJSONArray("definitions").getJSONObject(0) //finds definitions
                return jsonDefinition.getString("definition") //returns definition as a string
            }
        }
        return null
    }
 
    fun getRandom(selectedil: String): String? {
        var language = when (selectedil){ //language selection
            "Spanish"-> "es"
            "English"-> "en"
            "German" -> "de"
            "Chinese" -> "zh"
            "Italian" ->"it"
            else -> "en"
        }
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://random-word-api.herokuapp.com/word?lang=$language") //Random Word Api
            .build()
        val response = client.newCall(request).execute() //gets response from API
        return response.body?.string() //returns and stringifies response
    }

    fun translateString(input: String, selectedil: String) {
        val sourceLang: String = when(selectedil) { //select language (again)
            "English" -> TranslateLanguage.ENGLISH
            "Spanish" -> TranslateLanguage.SPANISH
            "Italian" -> TranslateLanguage.ITALIAN
            "German" -> TranslateLanguage.GERMAN
            "Chinese" -> TranslateLanguage.CHINESE
            else -> TranslateLanguage.ENGLISH // Default to English if no language is selected
        }
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang) //sets language
            .setTargetLanguage(TranslateLanguage.ENGLISH) //english output translation
            .build()

        val translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                onModelSuccess(translator, input)
            }
            .addOnFailureListener { Log.d("MODEL", "Model download Failed") }
    }


    fun onModelSuccess(translator: com.google.mlkit.nl.translate.Translator, input: String) {
        translator.translate(input)
            .addOnSuccessListener { translatedText ->

                val targetText = findViewById<TextView>(R.id.ts_textView2)
                if (giveupIsEnabled == true) {
                    targetText.text = translatedText //display target text if giveupIsEnabled
                }
                this.translatedText = translatedText

            }
            .addOnFailureListener { Log.d("TRANSLATION", "Translate Failed") }
    }

    fun speakString(tts: TextToSpeech, input: String) {
        val talkspeed = talkspeedPrefs.getInt("speed", barStarter).toFloat() / 50.0f
        if (talkspeed == 0f) {
            tts.setSpeechRate(0.10f) //adjust speechrate based on progress bar interactions
        }
        val pitchvoice = pitchPrefs.getInt("pitch", barStarter).toFloat() / 50.0f
        if (pitchvoice == 0f) {
            tts.setPitch(0.10f) //adjust pitch based on progress bar interactions
        }
        tts.setSpeechRate(talkspeed)
        tts.setPitch(pitchvoice)

        val sharedPreferences = getSharedPreferences("VoicePreferences", MODE_PRIVATE)
        var selectedvoice = sharedPreferences.getString("SelectedVoice", "").toString()


        when (selectedvoice) { //voice selection
            "Female 1 (UK)" -> { //BRITISH WOMEN
                val chosenVoice = Voice(
                    "en-gb-x-gba-network",
                    Locale.US,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_NORMAL,
                    false,
                    null
                )
                tts.setVoice(chosenVoice)

            }
            "Female 2 (US)" -> { //American WOMEN
                val chosenVoice = Voice(
                    "en-us-x-tpf-local",
                    Locale.US,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_NORMAL,
                    false,
                    null
                )
                tts.setVoice(chosenVoice)

            }
            "Female 3 (IN)" -> { //INDIAN WOMEN
                val chosenVoice = Voice(
                    "en-in-x-enc-network",
                    Locale.US,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_NORMAL,
                    false,
                    null
                )
                tts.setVoice(chosenVoice)

            }
            "Female 4 (ES)" -> { //SPANISH MAN
                val locSpanish = Locale("spa", "MEX")
                val chosenVoice = Voice(
                    "es-es-x-eee-local",
                    locSpanish,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_NORMAL,
                    false,
                    null
                )
                tts.setVoice(chosenVoice)
            }

            "Male 1 (UK)" -> { //BRITISH MAN
                val chosenVoice = Voice(
                    "en-gb-x-gbb-local",
                    Locale.US,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_NORMAL,
                    false,
                    null
                )
                tts.setVoice(chosenVoice)
            }
            "Male 2 (US)" -> { //AMERICAN MAN
                val chosenVoice = Voice(
                    "en-us-x-iol-local",
                    Locale.US,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_NORMAL,
                    false,
                    null
                )
                tts.setVoice(chosenVoice)
            }
            "Male 3 (IN)" -> { //INDIAN MAN
                val chosenVoice = Voice(
                    "en-in-x-ene-network",
                    Locale.US,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_NORMAL,
                    false,
                    null
                )
                tts.setVoice(chosenVoice)
            }
            "Male 4 (ES)" -> { //SPANISH MAN
                val locSpanish = Locale("spa", "MEX")
                val chosenVoice = Voice(
                    "es-es-x-eef-local",
                    locSpanish,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_NORMAL,
                    false,
                    null
                )
                tts.setVoice(chosenVoice)
            }
        }
        tts.speak(input, TextToSpeech.QUEUE_ADD, null) //speak tts
    }

    override fun onPause() { //STOPS TTS FROM SPEAKING OUTSIDE THE APP
        super.onPause()
        if (ttsSource.isSpeaking) {
            ttsSource.stop()
        }
    }

    override fun onInit(p0: Int) {

        userInput = findViewById(R.id.inputEditText)
        val submitButton = findViewById<Button>(R.id.submitButton)
        val button = findViewById<Button>(R.id.ts_button)
        val sourceText = findViewById<TextView>(R.id.ts_textView)
        var hintbutton = findViewById<Button>(R.id.hintbutton)
        var hinttext = findViewById<TextView>(R.id.hint_textView)
        val counterTextView = findViewById<TextView>(R.id.counterTextView)

        button.setOnClickListener {//speak button
            if (isSpeaking) { // STOP BUTTON FUNCTIONALITY
                ttsSource.stop()
            } else {
                speakString(ttsSource, sourceText.text.toString()
                )
            }
            isSpeaking = !isSpeaking
        }
        hintbutton.setOnClickListener {//hint button
            counter = 0 //remove streak
            counterTextView.text = counter.toString()
            hintisEnabled = true

            if (translatedText.isEmpty()) {
                hinttext.text = "No hint available."
            }
            else if (translatedText.length >= 2) {
                val substring = translatedText.substring(0, 2) //get the first two letters of the word
                val hint = "The first two letters are: $substring"
                hinttext.text = hint //display hint
            }
            else {
                hinttext.text = "The translated text is too short for a hint." //for very short words
            }
        }

    }
}
