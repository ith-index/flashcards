package com.example.flashcards.decks

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.flashcards.R

class DecksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_single)

        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentById(R.id.frameLayoutFragmentContainer) == null) {
            fragmentManager
                .beginTransaction()
                .add(R.id.frameLayoutFragmentContainer, DeckFragment())
                .commit()
        }
    }
}
