package com.example.flashcards.decks

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.os.Bundle
import android.support.design.card.MaterialCardView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.example.flashcards.*
import com.example.flashcards.deck.instantiateDeckActivityIntent
import java.lang.ref.WeakReference

class DeckFragment : Fragment() {
    private lateinit var deckAdapter: SimpleAdapter<Deck>
    private lateinit var sqLiteOpenHelper: SQLiteOpenHelper
    private var selectedDecks: MutableList<Deck> = mutableListOf()
    private var selectedMaterialCardViews: MutableList<MaterialCardView> = mutableListOf()
    private var actionMode: ActionMode? = null
    private lateinit var updatedDeckListener: (Deck) -> Unit

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_decks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appCompatActivity = activity!! as AppCompatActivity
        val activityContext: Context = appCompatActivity
        val applicationContext = activityContext.applicationContext
        sqLiteOpenHelper = instantiateSQLiteOpenHelper(applicationContext)

        val instantiateDeckViewHolder = apply(::DeckViewHolder, Pair(
            {
                materialCardView, deck ->
                    if (selectedDecks.count() == 0) {
                        startActivity(
                            instantiateDeckActivityIntent(activityContext, deck.deckID)
                        )
                    } else {
                        selectOrUnselect(deck, materialCardView)
                    }
            },
            {
                materialCardView, deck ->
                    if (selectedDecks.count() == 0) {
                        actionMode = appCompatActivity.startSupportActionMode(actionModeCallback)
                    }
                    selectOrUnselect(deck, materialCardView)
            }
        ))
        val recyclerViewDecks: RecyclerView = view.findViewById(R.id.recyclerViewDecks)
        deckAdapter = SimpleAdapter(recyclerViewDecks, instantiateDeckViewHolder)
        val weakReferenceDeckAdapter = WeakReference(deckAdapter)
        recyclerViewDecks.run {
            layoutManager = LinearLayoutManager(activityContext)
            adapter = deckAdapter
        }


        updatedDeckListener = {
            deckAdapter.update({ deck -> deck.deckID == it.deckID }, it)
        }
        subscribeToUpdatedDeck(updatedDeckListener)

        val editTextViewDeckName: EditText = view.findViewById(R.id.editTextDeckName)
        editTextViewDeckName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(ignore: Editable) {}

            override fun beforeTextChanged(ignore: CharSequence, ignore1: Int, ignore2: Int, ignore3: Int) {}

            override fun onTextChanged(ignore: CharSequence, ignore1: Int, ignore2: Int, ignore3: Int) {
                editTextViewDeckName.error = null
            }
        })

        view.findViewById<ImageButton>(R.id.imageButtonAddDeck).setOnClickListener {
            val deckName = editTextViewDeckName.text.toString()
            if (deckName.isBlank()) {
                editTextViewDeckName.error = "Deck name cannot be blank"
            } else {
                editTextViewDeckName.run {
                    setText("", TextView.BufferType.EDITABLE)
                    clearFocus()
                }
                postOnDataThread {
                    val deck = createDeck(sqLiteOpenHelper, deckName)
                    postOnMainThread {
                        weakReferenceDeckAdapter.get()?.add(deck)
                    }
                }
            }
        }

        postOnDataThread {
            val decks = readDecks(sqLiteOpenHelper)
            postOnMainThread {
                weakReferenceDeckAdapter.get()?.dataList = decks
            }
        }
    }

    override fun onDestroy() {
        unsubscribeToUpdatedDeck(updatedDeckListener)
        super.onDestroy()
    }

    private fun selectOrUnselect(deck: Deck, materialCardView: MaterialCardView) {
        if (selectedDecks.contains(deck)) {
            selectedDecks.remove(deck)
            selectedMaterialCardViews.remove(materialCardView)
            materialCardView.run {
                setBackgroundColor(Color.WHITE)
                strokeWidth = 0
                strokeColor = Color.WHITE
            }
            if (selectedDecks.count() == 0) {
                actionMode?.finish()
            }
        } else {
            selectedDecks.add(deck)
            selectedMaterialCardViews.add(materialCardView)
            materialCardView.run {
                setBackgroundColor(Color.LTGRAY)
                strokeWidth = 5
                strokeColor = Color.BLACK
            }
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            actionMode.menuInflater.inflate(R.menu.menu_decks_multiselect, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(ignore: ActionMode, menuItem: MenuItem): Boolean =
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    val weakReferenceDeckAdapter = WeakReference(deckAdapter)
                    val weakReferenceActionMode = WeakReference(actionMode)
                    postOnDataThread {
                        deleteDecks(sqLiteOpenHelper, selectedDecks.map { it.deckID })
                        postOnMainThread {
                            weakReferenceDeckAdapter.get()?.delete(selectedDecks)
                            weakReferenceActionMode.get()?.finish()
                        }
                    }
                    true
                }
                else -> true
            }

        override fun onDestroyActionMode(ignore: ActionMode) {
            selectedMaterialCardViews.forEach {
                it.run {
                    setBackgroundColor(Color.WHITE)
                    strokeWidth = 0
                    strokeColor = Color.WHITE
                }
            }
            selectedDecks.clear()
            selectedMaterialCardViews.clear()
        }
    }
}

private class DeckViewHolder(private val pairOfClickListeners: Pair<(MaterialCardView, Deck) -> Unit, (MaterialCardView, Deck) -> Unit>, viewGroup: ViewGroup) : SimpleViewHolder<Deck>(viewGroup, R.layout.item_deck) {
    private val textViewName: TextView = itemView.findViewById(R.id.textViewName)
    private val textViewNumberOfFlashCards: TextView = itemView.findViewById(R.id.textViewNumberOfFlashcards)

    override val bind: Deck.() -> Unit = {
        textViewName.text = name
        textViewNumberOfFlashCards.text =
            activityContext
                .resources
                .getQuantityString(R.plurals.number_of_flashcards, numberOfFlashCards, numberOfFlashCards)
        itemView.setOnClickListener {
            pairOfClickListeners.first(itemView as MaterialCardView, this)
        }
        itemView.setOnLongClickListener {
            pairOfClickListeners.second(itemView as MaterialCardView, this)
            true
        }
    }
}