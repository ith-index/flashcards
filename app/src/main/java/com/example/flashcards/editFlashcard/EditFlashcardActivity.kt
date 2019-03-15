package com.example.flashcards.editFlashcard

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.flashcards.R
import com.example.flashcards.getLongExtraOrThrow

private const val KEY_EXTRA_FLASHCARD_ID = "com.example.flashcards.KEY_EXTRA_FLASHCARD_ID"

fun instantiateEditFlashcardActivityIntent(context: Context, flashcardID: Long): Intent =
    Intent(context, EditFlashcardActivity::class.java).apply {
        putExtra(KEY_EXTRA_FLASHCARD_ID, flashcardID)
    }

class EditFlashcardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_single)

        val flashcardID = intent.getLongExtraOrThrow(KEY_EXTRA_FLASHCARD_ID)
        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentById(R.id.frameLayoutFragmentContainer) == null) {
            fragmentManager
                .beginTransaction()
                .add(R.id.frameLayoutFragmentContainer, instantiateEditFlashcardFragment(flashcardID))
                .commit()
        }
    }
}
