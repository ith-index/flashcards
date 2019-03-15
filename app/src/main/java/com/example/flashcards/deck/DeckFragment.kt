package com.example.flashcards.deck

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.Button
import android.widget.TextView
import com.example.flashcards.*
import com.example.flashcards.deckEdit.instantiateEditDeckActivityIntent
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

private const val KEY_ARGUMENT_DECK_ID = "KEY_ARGUMENT_DECK_ID"

fun instantiateDeckFragment(deckID: Long): DeckFragment =
    DeckFragment().apply {
        arguments = Bundle().apply {
            putLong(KEY_ARGUMENT_DECK_ID, deckID)
        }
    }

class DeckFragment : Fragment() {
    private var deckID: Long by Delegates.notNull()
    private lateinit var updatedDeckListener: (Deck) -> Unit

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_deck, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_deck, menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean =
        when (menuItem.itemId) {
            R.id.action_edit -> {
                startActivity(
                    instantiateEditDeckActivityIntent(context!!, deckID)
                )
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deckID = arguments!!.getLongArgumentOrThrow(KEY_ARGUMENT_DECK_ID)
        val activityContext: Context = activity!!
        val applicationContext = activityContext.applicationContext
        val sqLiteOpenHelper = instantiateSQLiteOpenHelper(applicationContext)

        val textViewName: TextView = view.findViewById(R.id.textViewName)
        val textViewNumberOfFlashcards: TextView = view.findViewById(R.id.textViewNumberOfFlashcards)
        val buttonReview: Button = view.findViewById(R.id.buttonReview)

        updatedDeckListener = {
            deck -> deck.run {
                textViewName.text = name
                textViewNumberOfFlashcards.text =
                    activityContext.resources.getQuantityString(R.plurals.number_of_flashcards, numberOfFlashCards, numberOfFlashCards)
                buttonReview.isEnabled = numberOfFlashCards > 0
            }
        }
        subscribeToUpdatedDeck(updatedDeckListener)

        val weakReferenceTextViewName = WeakReference(textViewName)
        val weakReferenceTextViewNumberOfFlashcards = WeakReference(textViewNumberOfFlashcards)
        val weakReferenceButtonReview = WeakReference(buttonReview)
        postOnDataThread {
            val deck = readDeck(sqLiteOpenHelper, deckID)
            postOnMainThread {
                deck.run {
                    weakReferenceTextViewName.get()?.text = name
                    weakReferenceTextViewNumberOfFlashcards.get()?.text =
                        activityContext.resources.getQuantityString(R.plurals.number_of_flashcards, numberOfFlashCards, numberOfFlashCards)
                    if (numberOfFlashCards > 0) {
                        weakReferenceButtonReview.get()?.run {
                            isEnabled = true
                            setOnClickListener {
                                startActivity(
                                    instantiateReviewActivityIntent(activityContext, deckID)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        unsubscribeToUpdatedDeck(updatedDeckListener)
        super.onDestroy()
    }
}
