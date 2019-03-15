package com.example.flashcards.deckEdit

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.support.design.card.MaterialCardView
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.example.flashcards.*
import com.example.flashcards.editFlashcard.instantiateEditFlashcardActivityIntent
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

private const val KEY_ARGUMENT_DECK_ID = "KEY_ARGUMENT_DECK_ID"

fun instantiateEditDeckFragment(deckID: Long): EditDeckFragment =
    EditDeckFragment().apply {
        arguments = Bundle().apply {
            putLong(KEY_ARGUMENT_DECK_ID, deckID)
        }
    }

class EditDeckFragment : Fragment() {
    private var deckID: Long by Delegates.notNull()
    private lateinit var sqLiteOpenHelper: SQLiteOpenHelper
    private lateinit var editTextDeckName: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_deck_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deckID = arguments!!.getLongArgumentOrThrow(KEY_ARGUMENT_DECK_ID)
        val activityContext: Context = activity!!
        val applicationContext = activityContext.applicationContext
        sqLiteOpenHelper = instantiateSQLiteOpenHelper(applicationContext)

        editTextDeckName = view.findViewById(R.id.editTextDeckName)
        val editTextFlashcardFront: EditText = view.findViewById(R.id.editTextFlashcardFront)
        val editTextFlashcardBack: EditText = view.findViewById(R.id.editTextFlashcardBack)
        val imageButtonAddFlashCard: ImageButton = view.findViewById(R.id.imageButtonAddFlashcard)
        val recyclerViewFlashcards: RecyclerView = view.findViewById(R.id.recyclerViewFlashcards)

        val instantiateFlashcardViewHolder = apply(::FlashcardViewHolder, Pair(
            {
                flashcard, materialCardView ->
                    startActivity(
                        instantiateEditFlashcardActivityIntent(activityContext, flashcard.id)
                    )
            },
            {
                flashcard, materialCardView ->

            }
        ))
        val flashcardAdapter = SimpleAdapter(recyclerViewFlashcards, instantiateFlashcardViewHolder)
        val weakReferenceFlashcardAdapter = WeakReference(flashcardAdapter)

        recyclerViewFlashcards.run {
            layoutManager = LinearLayoutManager(activityContext)
            adapter = flashcardAdapter
        }

        imageButtonAddFlashCard.setOnClickListener {
            val flashcardFront = editTextFlashcardFront.text.toString()
            val flashcardBack = editTextFlashcardBack.text.toString()
            editTextFlashcardFront.setText("")
            editTextFlashcardBack.setText("")
            editTextFlashcardFront.clearFocus()
            editTextFlashcardBack.clearFocus()
            postOnDataThread {
                val flashcard = createFlashcard(sqLiteOpenHelper, deckID, flashcardFront, flashcardBack)
                postOnMainThread {
                    weakReferenceFlashcardAdapter.get()?.add(flashcard)
                }
            }
        }

        val weakReferenceEditTextDeckName = WeakReference(editTextDeckName)
        postOnDataThread {
            val deck = readDeck(sqLiteOpenHelper, deckID)
            val flashCards = readFlashcards(sqLiteOpenHelper, deckID)
            postOnMainThread {
                weakReferenceEditTextDeckName.get()?.setText(deck.name, TextView.BufferType.EDITABLE)
                weakReferenceFlashcardAdapter.get()?.dataList = flashCards
            }
        }
    }


    override fun onDestroy() {
        activity?.run {
            if (isFinishing) {
                val deckName = editTextDeckName.text.toString()
                postOnDataThread {
                    updateDeck(sqLiteOpenHelper, deckID, deckName)
                    val deck = readDeck(sqLiteOpenHelper, deckID)
                    postOnMainThread {
                        publishUpdatedDeck(deck)
                    }
                }
            }
        }
        super.onDestroy()
    }
}

private class FlashcardViewHolder(private val onClickListeners: Pair<(Flashcard, MaterialCardView) -> Unit, (Flashcard, MaterialCardView) -> Unit>, viewGroup: ViewGroup) : SimpleViewHolder<Flashcard>(viewGroup, R.layout.item_flashcard) {
    private val textViewFront: TextView = itemView.findViewById(R.id.textViewName)
    private val textViewBack: TextView = itemView.findViewById(R.id.textViewNumberOfFlashcards)

    override val bind: Flashcard.() -> Unit = {
        textViewFront.text = front
        textViewBack.text = back
        itemView.setOnClickListener {
            onClickListeners.first(this, itemView as MaterialCardView)
        }
        itemView.setOnLongClickListener {
            onClickListeners.second(this, itemView as MaterialCardView)
            true
        }
    }
}
