package com.example.flashcards.editFlashcard

import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.example.flashcards.*
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

private const val KEY_ARGUMENT_FLASHCARD_ID = "KEY_ARGUMENT_FLASHCARD_ID"

fun instantiateEditFlashcardFragment(flashcardID: Long): EditFlashcardFragment =
    EditFlashcardFragment().apply {
        arguments = Bundle().apply {
            putLong(KEY_ARGUMENT_FLASHCARD_ID, flashcardID)
        }
    }

class EditFlashcardFragment : Fragment() {
    private var flashcardID: Long by Delegates.notNull()
    private lateinit var sqLiteOpenHelper: SQLiteOpenHelper
    private lateinit var editTextFlashcardFront: EditText
    private lateinit var editTextFlashcardBack: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_flashcard_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flashcardID = arguments!!.getLongArgumentOrThrow(KEY_ARGUMENT_FLASHCARD_ID)
        sqLiteOpenHelper = instantiateSQLiteOpenHelper(activity!!.applicationContext)
        editTextFlashcardFront = view.findViewById(R.id.editTextFlashcardFront)
        editTextFlashcardBack = view.findViewById(R.id.editTextFlashcardBack)

        val weakReferenceEditTextFlashcardFront = WeakReference(editTextFlashcardFront)
        val weakReferenceEditTextFlashcardBack = WeakReference(editTextFlashcardBack)
        postOnDataThread {
            val flashcard = readFlashcard(sqLiteOpenHelper, flashcardID)
            postOnMainThread {
                flashcard.run {
                    weakReferenceEditTextFlashcardFront.get()?.setText(front, TextView.BufferType.EDITABLE)
                    weakReferenceEditTextFlashcardBack.get()?.setText(back, TextView.BufferType.EDITABLE)
                }
            }
        }
    }

    override fun onDestroy() {
        activity?.run {
            val sqLiteOpenHelper = instantiateSQLiteOpenHelper(applicationContext)
            if (isFinishing) {
                val flashCardFront = editTextFlashcardFront.text.toString()
                val flashCardBack = editTextFlashcardBack.text.toString()
                postOnDataThread {
                    updateFlashcard(sqLiteOpenHelper, flashcardID, flashCardFront, flashCardBack)
                    postOnMainThread {

                    }
                }
            }
        }
        super.onDestroy()
    }
}
