package com.ammar.network.source

import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirebaseDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase
) : FirebaseDataSource {

    override fun getScreenBlueprint(screenId: String): Flow<Result<String>> = callbackFlow {
        val ref = database.getReference("sdui_screens").child(screenId)

        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val jsonString = snapshot.getValue(String::class.java)
                if (jsonString != null) {
                    trySend(Result.success(jsonString))
                } else {
                    trySend(Result.failure(Exception("Blueprint not found for screen: $screenId")))                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))            }
        }

        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }
}