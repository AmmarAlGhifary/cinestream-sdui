package com.ammar.network.source

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirebaseDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase
) : FirebaseDataSource {

    override fun getScreenBlueprint(screenId: String): Flow<Result<String>> = callbackFlow {
        val ref = database.getReference("sdui_screens").child(screenId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rawValue = snapshot.value

                if (rawValue != null) {
                    val jsonString = Gson().toJson(rawValue)
                    trySend(Result.success(jsonString))
                } else {
                    trySend(Result.failure(Exception("Json not found for screen: $screenId")))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                val exception = error.toException()
                trySend(Result.failure(exception))
                close(exception)
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }
}