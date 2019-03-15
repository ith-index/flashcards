package com.example.flashcards.deckEdit

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.flashcards.R
import com.example.flashcards.getLongExtraOrThrow

private const val KEY_EXTRA_DECK_ID = "com.example.flashcards.KEY_EXTRA_DECK_ID"

fun instantiateEditDeckActivityIntent(context: Context, deckID: Long): Intent =
    Intent(context, EditDeckActivity::class.java).apply {
        putExtra(KEY_EXTRA_DECK_ID, deckID)
    }

class EditDeckActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_single)

        val deckID = intent.getLongExtraOrThrow(KEY_EXTRA_DECK_ID)
        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentById(R.id.frameLayoutFragmentContainer) == null) {
            fragmentManager
                .beginTransaction()
                .add(R.id.frameLayoutFragmentContainer, instantiateEditDeckFragment(deckID))
                .commit()
        }
    }
}
