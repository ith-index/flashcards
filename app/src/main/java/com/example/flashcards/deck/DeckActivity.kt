package com.example.flashcards.deck

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.flashcards.R
import com.example.flashcards.getLongExtraOrThrow

private const val KEY_EXTRA_DECK_ID = "com.example.flashcards.KEY_EXTRA_DECK_ID"

fun instantiateDeckActivityIntent(context: Context, deckID: Long): Intent =
    Intent(context, DeckActivity::class.java).apply {
        putExtra(KEY_EXTRA_DECK_ID, deckID)
    }

class DeckActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_single)

        val deckID = intent.getLongExtraOrThrow(KEY_EXTRA_DECK_ID)
        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentById(R.id.frameLayoutFragmentContainer) == null) {
            fragmentManager
                .beginTransaction()
                .add(R.id.frameLayoutFragmentContainer, instantiateDeckFragment(deckID))
                .commit()
        }
    }
}
