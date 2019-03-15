package com.example.flashcards

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Deck(val deckID: Long, val name: String, val numberOfFlashCards: Int)

fun createDeck(sqLiteOpenHelper: SQLiteOpenHelper, name: String): Deck =
    sqLiteOpenHelper.writableDatabase.use {
        val deckID =
            it.compileStatement(INSERT_INTO_DECKS).run {
                bindString(1, name)
                bindLong(2, System.currentTimeMillis())
                executeInsert()
            }
        it.rawQuery(generateSelectDeck(deckID), null).use {
            it.moveToFirst()
            extractDeck(it)
        }
    }

fun updateDeck(sqLiteOpenHelper: SQLiteOpenHelper, deckID: Long, name: String) {
    sqLiteOpenHelper.writableDatabase.use {
        it.compileStatement(UPDATE_DECK).run {
            bindString(1, name)
            bindLong(2, deckID)
            executeUpdateDelete()
        }
    }
}

fun readDeck(sqLiteOpenHelper: SQLiteOpenHelper, deckID: Long): Deck =
    sqLiteOpenHelper.readableDatabase.use {
        it.rawQuery(generateSelectDeck(deckID), null).use {
            it.moveToFirst()
            extractDeck(it)
        }
    }

fun readDecks(sqLiteOpenHelper: SQLiteOpenHelper): MutableList<Deck> =
    sqLiteOpenHelper.readableDatabase.use {
        it.rawQuery(SELECT_DECKS, null).use { it.run {
            val decks: MutableList<Deck> = mutableListOf()
            repeat(count) {
                moveToNext()
                decks.add(
                    extractDeck(this)
                )
            }
            decks
        } }
    }

fun deleteDecks(sqLiteOpenHelper: SQLiteOpenHelper, deckIDs: List<Long>) {
    sqLiteOpenHelper.writableDatabase.use {
        it.execSQL(
            generateDeleteDecks(deckIDs)
        )
    }
}

private fun extractDeck(cursor: Cursor): Deck =
    cursor.run {
        Deck(
            deckID = getLong(getColumnIndex("deck_id")),
            name = getString(getColumnIndex("deck_name")),
            numberOfFlashCards = getInt(getColumnIndex("number_of_flashcards"))
        )
    }

data class Flashcard(val id: Long, val front: String, val back: String)

fun createFlashcard(sqLiteOpenHelper: SQLiteOpenHelper, deckID: Long, front: String, back: String): Flashcard =
    sqLiteOpenHelper.writableDatabase.use {
        val flashCardID =
            it.compileStatement(INSERT_INTO_FLASHCARDS).run {
                bindLong(1, deckID)
                bindString(2, front)
                bindString(3, back)
                bindLong(4, System.currentTimeMillis())
                executeInsert()
            }
        it.rawQuery(generateSelectFlashcard(flashCardID), null).use {
            it.moveToFirst()
            extractFlashcard(it)
        }
    }

fun updateFlashcard(sqLiteOpenHelper: SQLiteOpenHelper, flashCardID: Long, front: String, back: String) {
    sqLiteOpenHelper.writableDatabase.use {
        it.compileStatement(UPDATE_FLASHCARD).run {
            bindString(1, front)
            bindString(2, back)
            bindLong(3, flashCardID)
            executeUpdateDelete()
        }
    }
}

fun readFlashcard(sqLiteOpenHelper: SQLiteOpenHelper, flashCardID: Long): Flashcard =
    sqLiteOpenHelper.readableDatabase.use {
        it.rawQuery(generateSelectFlashcard(flashCardID), null).use {
            it.moveToFirst()
            extractFlashcard(it)
        }
    }

fun readFlashcards(sqLiteOpenHelper: SQLiteOpenHelper, deckID: Long): MutableList<Flashcard> =
    sqLiteOpenHelper.readableDatabase.use {
        it.rawQuery(generateSelectFlashcards(deckID), null).use { it.run {
            val flashcards: MutableList<Flashcard> = mutableListOf()
            repeat(count) {
                moveToNext()
                flashcards.add(
                    extractFlashcard(this)
                )
            }
            flashcards
        } }
    }

private fun extractFlashcard(cursor: Cursor): Flashcard =
    cursor.run {
        Flashcard(
            id = getLong(getColumnIndex("flashcard_id")),
            front = getString(getColumnIndex("front")),
            back = getString(getColumnIndex("back"))
        )
    }

fun instantiateSQLiteOpenHelper(context: Context): SQLiteOpenHelper =
    TheSQLiteOpenHelper(context)

private const val DATABASE_NAME = "flashcards.db"
private const val DATABASE_VERSION = 1
private class TheSQLiteOpenHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onConfigure(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL("PRAGMA foreign_keys=ON;")
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.run {
            execSQL(CREATE_TABLE_DECKS)
            execSQL(CREATE_TABLE_FLASHCARDS)
        }
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}

private val CREATE_TABLE_DECKS =
    """
        CREATE TABLE decks (
            deck_id INTEGER PRIMARY KEY,
            deck_name TEXT NOT NULL,
            datetime INTEGER NOT NULL
        );
    """.trimIndent()

private fun generateSelectDeck(deckID: Long): String =
    """
        SELECT deck_id, deck_name, (SELECT COUNT(*) FROM flashcards WHERE deck_id = $deckID) AS number_of_flashcards
        FROM decks WHERE deck_id = $deckID
    """.trimIndent()

private val SELECT_DECKS =
    """
        SELECT deck_id, deck_name, (SELECT COUNT(*) FROM flashcards WHERE deck_id = decks.deck_id) AS number_of_flashcards
        FROM decks ORDER BY datetime DESC
    """.trimIndent()

private const val INSERT_INTO_DECKS = "INSERT INTO decks (deck_name, datetime) VALUES (?, ?);"

private const val UPDATE_DECK = "UPDATE decks SET deck_name = ? WHERE deck_id = ?"

private fun generateDeleteDecks(deckIDs: List<Long>): String =
    "DELETE FROM decks WHERE deck_id IN (${deckIDs.joinToString()})"


private val CREATE_TABLE_FLASHCARDS =
    """
        CREATE TABLE flashcards (
            flashcard_id INTEGER PRIMARY KEY,
            deck_id INTEGER NOT NULL,
            front TEXT NOT NULL,
            back TEXT NOT NULL,
            datetime INTEGER NOT NULL,
            FOREIGN KEY (deck_id) REFERENCES decks(deck_id) ON DELETE CASCADE
        );
    """.trimIndent()

private fun generateSelectFlashcard(flashCardID: Long) =
    """
        SELECT flashcard_id, front, back
        FROM flashcards WHERE flashcard_id = $flashCardID
    """.trimIndent()

private fun generateSelectFlashcards(deckID: Long): String =
    """
        SELECT flashcard_id, front, back
        FROM flashcards WHERE deck_id = $deckID
        ORDER BY datetime DESC
    """.trimIndent()

private const val INSERT_INTO_FLASHCARDS = "INSERT INTO flashcards (deck_id, front, back, datetime) VALUES (?, ?, ?, ?);"

private const val UPDATE_FLASHCARD = "UPDATE flashcards SET front = ?, back = ? WHERE flashcard_id = ?"