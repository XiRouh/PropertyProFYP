package com.example.propertyprofyp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.propertyprofyp.BotConstants.RECEIVE_ID
import com.example.propertyprofyp.BotConstants.SEND_ID
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import com.example.propertyprofyp.databinding.SearchbotBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class SearchBotActivity : AppCompatActivity() {
    private lateinit var adapter: MessagingAdapter
    private lateinit var binding: SearchbotBinding
    private val searchCriteria = mutableMapOf<String, String>()
    private val dummyProperties = listOf(
        Property(id = "1", propertyType = "Condo", area = "Downtown", price = 250000.0),
        Property(id = "2", propertyType = "House", area = "Suburbs", price = 300000.0),
        // Add more dummy properties as needed
    )
    private var isRestartMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SearchbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickEvents()

        customBotMessage("Hello! What type of property are you searching for?")
    }

    private fun setupRecyclerView() {
        adapter = MessagingAdapter()
        binding.rvMessages.adapter = adapter // Correctly using rv_messages via binding
        binding.rvMessages.layoutManager = LinearLayoutManager(applicationContext)
    }

    private fun setupClickEvents() {
        binding.btnSend.setOnClickListener {
            if (isRestartMode) {
                restartActivity()
            } else {
                sendMessage()
            }
        }
    }

    private fun restartActivity() {
        // Reset UI state
        isRestartMode = false
        binding.btnSend.text = "Send"
        binding.etMessage.isEnabled = true

        // Clear search criteria and messages
        searchCriteria.clear()
        messagesList.clear()
        adapter.notifyDataSetChanged()

        // Display initial message
        customBotMessage("Hello! What type of property are you searching for?")
    }

    private fun customBotMessage(message: String) {
        GlobalScope.launch {
            delay(1000) // Simulate network delay
            withContext(Dispatchers.Main) {
                val timeStamp = currentTime()
                messagesList.add(Message(message, RECEIVE_ID, timeStamp))
                adapter.insertMessage(Message(message, RECEIVE_ID, timeStamp))
                binding.rvMessages.scrollToPosition(adapter.itemCount - 1) // Correctly using rv_messages via binding
            }
        }
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString()
        if (message.isNotEmpty()) {
            // Insert user message
            adapter.insertMessage(Message(message, SEND_ID, currentTime()))

            binding.etMessage.setText("")
            botResponse(message)
        }
    }

    private fun botResponse(message: String) {
        GlobalScope.launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                val response = processSearchQuery(message)
                Log.d("SearchBotActivity", "Bot Response: $response")
                if (response.isNotEmpty()) {
                    adapter.insertMessage(Message(response, RECEIVE_ID, currentTime()))
                    binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
                }
                if (searchCriteriaReady()) {
                    Log.d("SearchBotActivity", "Search Criteria Ready, calling fetchAndDisplayProperties")
                    fetchAndDisplayProperties()
                }
            }
        }
    }

    private fun processSearchQuery(userInput: String): String {
        val response: String
        when {
            searchCriteria["propertyType"] == null -> {
                searchCriteria["propertyType"] = userInput
                response = "What area are you looking in?"
            }
            searchCriteria["area"] == null -> {
                searchCriteria["area"] = userInput
                response = "What is your budget?"
            }
            searchCriteria["budget"] == null -> {
                // Check if the budget input is a valid number
                val budget = userInput.toDoubleOrNull()
                if (budget == null) {
                    response = "Please enter a valid budget (numeric value)."
                } else {
                    searchCriteria["budget"] = userInput
                    response = searchForProperties()
                }
            }
            else -> {
                response = "I didn't understand that. Can you please specify what you're looking for?"
            }
        }
        Log.d("SearchBotActivity", "Process Search Query - Criteria: $searchCriteria, Response: $response")

        return response
    }

    private fun searchForProperties(): String {
        // Setting up the budget, property type, and area from search criteria
        val budget = searchCriteria["budget"]?.toDoubleOrNull() ?: Double.MAX_VALUE
        val propertyType = searchCriteria["propertyType"]
        val area = searchCriteria["area"]

        // Check if dummy properties match the criteria
        val matchedProperties = dummyProperties.filter {
            (it.price <= budget) &&
                    (it.propertyType.equals(propertyType, ignoreCase = true) || it.area.equals(area, ignoreCase = true))
        }

        Log.d("SearchBotActivity", "Matched Properties: $matchedProperties")

        // Instead of returning a message, just indicate that properties need to be fetched
        return if (matchedProperties.isNotEmpty() || searchCriteriaReady()) {
            // Indicate that the next step is to fetch and display properties
            "Fetching properties..."
        } else {
            // If no criteria are set yet, ask for more input
            "Please provide more details for your property search."
        }
    }

    private fun searchCriteriaReady(): Boolean {
        return searchCriteria["propertyType"] != null && searchCriteria["area"] != null && searchCriteria["budget"] != null
    }

    private fun fetchAndDisplayProperties() {
        Log.d("SearchBotActivity", "Fetching and Displaying Properties")

        val propertiesRef = FirebaseDatabase.getInstance().getReference("properties")
        propertiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val properties = mutableListOf<Property>()
                for (propertySnapshot in dataSnapshot.children) {
                    val property = propertySnapshot.getValue(Property::class.java)
                    property?.let { properties.add(it) }
                }

                val filteredProperties = filterProperties(properties, searchCriteria)
                val perfectMatches = filteredProperties.filter { it.price <= (searchCriteria["budget"]?.toDouble() ?: Double.MAX_VALUE) }

                if (perfectMatches.isNotEmpty()) {
                    adapter.insertMessage(Message("Below are the properties that matched all your requirements:", RECEIVE_ID, currentTime()))
                    displayProperties(perfectMatches)
                    updateRestartMode()
                } else if (filteredProperties.isNotEmpty() && filteredProperties != perfectMatches) {
                    adapter.insertMessage(Message("There are no properties perfectly matching your requirement. Below are the properties that matched the property type and area but exceed your budget:", RECEIVE_ID, currentTime()))
                    displayProperties(filteredProperties)
                    updateRestartMode()
                } else {
                    adapter.insertMessage(Message("No properties found matching your criteria.", RECEIVE_ID, currentTime()))
                    updateRestartMode()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Error handling
            }
        })
    }

    private fun updateRestartMode() {
        // Update UI for restart mode
        isRestartMode = true
        binding.btnSend.text = "Restart"
        binding.etMessage.isEnabled = false
    }

    private fun filterProperties(properties: List<Property>, criteria: Map<String, String>): List<Property> {
        Log.d("SearchBotActivity", "Applying filter with criteria: $criteria")

        return properties.filter { property ->
            val budgetMatchOrExceed = property.price <= (criteria["budget"]?.toDouble() ?: Double.MAX_VALUE)
            val typeMatch = property.propertyType.contains(criteria["propertyType"] ?: "", ignoreCase = true) // Use contains for partial match in property type
            val areaMatch = property.area.contains(criteria["area"] ?: "", ignoreCase = true) // Use contains for partial match in area

            Log.d("SearchBotActivity", "Budget match or exceed: $budgetMatchOrExceed, Type match: $typeMatch, Area match: $areaMatch")

            typeMatch && areaMatch // Note: We're not filtering by budget here
        }
    }

    private fun displayProperties(properties: List<Property>) {
        properties.forEach { property ->
            val message = Message("", BotConstants.PROPERTY_ID, currentTime(), property)
            Log.d("SearchBotActivity", "Displaying Property: $property")
            adapter.insertMessage(message)
        }
    }

    private fun currentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    companion object {
        private val messagesList = mutableListOf<Message>()
    }
}