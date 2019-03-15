package com.example.flashcards

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

private const val KEY_EXTRA_DECK_ID = "com.example.flashcards.KEY_EXTRA_DECK_ID"
private const val KEY_CURRENT_INDEX = "KEY_CURRENT_INDEX"
private const val KEY_BACK_SHOWN = "KEY_BACK_SHOWN"
private const val KEY_FLASHCARD_ID_LIST = "KEY_FLASHCARD_ID_LIST"

fun instantiateReviewActivityIntent(context: Context, deckID: Long): Intent =
    Intent(context, ReviewActivity::class.java).apply {
        putExtra(KEY_EXTRA_DECK_ID, deckID)
    }

class ReviewActivity : AppCompatActivity() {
    private var currentIndex = 0
    private var backShown = false
    private lateinit var flashcardIDArray: LongArray
    private lateinit var flashCardMap: Map<Long, Pair<String, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val deckID = intent.getLongExtraOrThrow(KEY_EXTRA_DECK_ID)

        savedInstanceState?.run {
            currentIndex = getInt(KEY_CURRENT_INDEX)
            backShown = getBoolean(KEY_BACK_SHOWN)
            flashcardIDArray = getLongArray(KEY_FLASHCARD_ID_LIST)!!
        }

        val textViewFlashcardFront: TextView = findViewById(R.id.textViewFlashcardFront)
        val textViewFlashcardBack: TextView = findViewById(R.id.textViewFlashcardBack)
        val divider: View = findViewById(R.id.divider)
        val buttonShowBack: Button = findViewById(R.id.buttonShowBack)
        val buttonNext: Button = findViewById(R.id.buttonNext)
        val buttonFinish: Button = findViewById(R.id.buttonFinish)

        val sqLiteOpenHelper = instantiateSQLiteOpenHelper(applicationContext)
        postOnDataThread {
            val flashcardList = readFlashcards(sqLiteOpenHelper, deckID).apply {
                shuffle()
            }
            val flashcardIDList = flashcardList.map { it.id}
            val newFlashcardIDArray = LongArray(flashcardIDList.size).apply {
                flashcardIDList.forEachIndexed {
                    index, flashcardID -> this[index] = flashcardID
                }
            }
            val newFlashCardMap = mutableMapOf<Long, Pair<String, String>>().apply {
                flashcardList.forEach {
                    put(it.id, Pair(it.front, it.back))
                }
            }
            postOnMainThread {
                if (!::flashcardIDArray.isInitialized) {
                    flashcardIDArray = newFlashcardIDArray
                }
                flashCardMap = newFlashCardMap

                val pairFrontBack = flashCardMap[
                    flashcardIDArray[currentIndex]
                ]
                textViewFlashcardFront.text = pairFrontBack!!.first
                if (backShown) {
                    divider.visibility = View.VISIBLE
                    textViewFlashcardBack.visibility = View.VISIBLE
                    buttonShowBack.visibility = View.GONE
                    textViewFlashcardBack.text = pairFrontBack.second
                    if (currentIndex != flashcardIDArray.size - 1) {
                        buttonNext.visibility = View.VISIBLE
                    } else {
                        buttonFinish.visibility = View.VISIBLE
                    }
                }
            }
        }

        buttonShowBack.setOnClickListener {
            backShown = true
            divider.visibility = View.VISIBLE
            textViewFlashcardBack.visibility = View.VISIBLE
            buttonShowBack.visibility = View.GONE
            val pairFrontBack = flashCardMap[
                flashcardIDArray[currentIndex]
            ]
            textViewFlashcardBack.text = pairFrontBack!!.second
            if (currentIndex != flashcardIDArray.size - 1) {
                buttonNext.visibility = View.VISIBLE
            } else {
                buttonFinish.visibility = View.VISIBLE
            }
        }

        buttonNext.setOnClickListener {
            currentIndex++
            backShown = false
            divider.visibility = View.GONE
            textViewFlashcardBack.visibility = View.GONE
            buttonShowBack.visibility = View.VISIBLE
            buttonNext.visibility = View.GONE
            val pairFrontBack = flashCardMap[
                    flashcardIDArray[currentIndex]
            ]
            textViewFlashcardFront.text = pairFrontBack!!.first
        }

        buttonFinish.setOnClickListener {
            finish()
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putInt(KEY_CURRENT_INDEX, currentIndex)
        savedInstanceState.putBoolean(KEY_BACK_SHOWN, backShown)
        if (::flashcardIDArray.isInitialized) {
            savedInstanceState.putLongArray(KEY_FLASHCARD_ID_LIST, flashcardIDArray)
        }
        super.onSaveInstanceState(savedInstanceState)
    }
}
