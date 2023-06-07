package com.example.learningapp
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class HomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)
        val startButton = findViewById<Button>(R.id.startbutton)
        val randomword1 = findViewById<TextView>(R.id.randomword1)
        val randomword2 = findViewById<TextView>(R.id.randomword2)
        val counterTextView = findViewById<TextView>(R.id.wordslearned)
        val wcounterTextView = findViewById<TextView>(R.id.totalwords)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val progressPrefs = getSharedPreferences("ProgressPrefs", Context.MODE_PRIVATE)
        val currentProgress = progressPrefs.getInt("progress", 0)
        progressBar.progress = currentProgress
        
        var percentage = 0.0
        var randomWord1: String
        var randomWord2: String

        fun increaseProgress() {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            val maxProgress = progressBar.max
            if (progressBar.progress < maxProgress) { //if the progress hasnt reached maximum progress
                progressBar.progress = progressBar.progress + 1 //add one to the progress
            }
        }

        startButton.setOnClickListener {
            val maxProgress = progressBar.max
            if (progressBar.progress < maxProgress) {
                increaseProgress() //add progress to progress bar
                val editor = progressPrefs.edit() //saves new progress
                editor.putInt("progress", progressBar.progress)
                editor.apply()
            }
            else {
                progressBar.progress = 0 //full progress bar is reset at next click
                val editor = progressPrefs.edit() //saves reset progress
                editor.putInt("progress", 0)
                editor.apply()
            }
            val intent = Intent(this, MainActivity::class.java) //starts MainActivity
            startActivity(intent)
        }

        val counterPreferences = getSharedPreferences("CounterPrefs", Context.MODE_PRIVATE) //preferences for counter
        val correctCounter = counterPreferences.getInt("correctCounter", 0)
        counterTextView.text = correctCounter.toString() //gets  number of correct answers and displays it


        val wcounterPreferences = getSharedPreferences("WCounterPrefs", Context.MODE_PRIVATE)
        val wcorrectCounter = wcounterPreferences.getInt("wcorrectCounter", 0) //gets the total number of words the user generated


        if (wcorrectCounter != 0) { //if the total number of words isnt 0
            val decimalFormat = DecimalFormat("0.00") //decimal rounding formatting
            percentage = decimalFormat.format((correctCounter.toFloat()/ wcorrectCounter.toFloat() * 100.0)).toDouble() //calculates the accuracy by dividing the number of correct answers with the total answers
            wcounterTextView.text = percentage.toString()
        }
        else {
           percentage = 0.0 //counter is 0 because user hasnt interacted with the app yet.
            wcounterTextView.text = percentage.toString()
        }
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val wordArraySet = sharedPreferences.getStringSet("wordArray", emptySet())


        val wordArray = if (wordArraySet != null) {
            wordArraySet.toMutableList()
        }
        else {
            mutableListOf()
        }

        val wordArrayLength = wordArray.size //gets size of array
        if (wordArrayLength >= 2) { //if word array has more than two words
            val uniqueWords = wordArray.distinct() //gets unique words (words that are different from each other)
            val shuffledWords = uniqueWords.shuffled() //shuffles them to prevent the same word from being displayed
            randomWord1 = shuffledWords[0].dropLast(2) //removes brackets
            randomWord1 = randomWord1.drop(2) //removes brackets
            randomWord2 = shuffledWords[1].dropLast(2)
            randomWord2 = randomWord2.drop(2)
        }
        else if (wordArrayLength == 1) { //only one word is available
            randomWord1 = wordArray[0].dropLast(2) //removes brackets
            randomWord1 = randomWord1.drop(2)
            randomWord2 = "Learn more!" //placeholder until users learns more words
        }

        else { //no words are available
            randomWord1 = "Learn more!"
            randomWord2 = "Learn more!"
        }

        randomword1.text = randomWord1 //display randomwords
        randomword2.text = randomWord2

    }


}
