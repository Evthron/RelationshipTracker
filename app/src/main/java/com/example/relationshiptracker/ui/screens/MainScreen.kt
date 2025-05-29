package com.example.relationshiptracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

import com.example.relationshiptracker.data.db.entities.Conversation
import com.example.relationshiptracker.ui.viewmodel.MainViewModel
import com.example.relationshiptracker.data.db.entities.Person

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var selectedPerson by remember { mutableStateOf<Person?>(null) }
    var persons by remember { mutableStateOf<List<Person>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.allPersons.collectLatest { persons = it }
    }

    if (selectedPerson == null) {
        PersonListScreen(
            persons = persons,
            onAddPerson = { showAddPersonDialog = true },
            onPersonClick = { person -> selectedPerson = person }
        )
    } else {
        PersonDetailScreen(
            person = selectedPerson!!,
            viewModel = viewModel,
            onBack = { selectedPerson = null }
        )
    }

    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onAdd = { name, impression, interests, goals, category ->
                viewModel.addPerson(name, impression, interests, goals, category)
                showAddPersonDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonListScreen(
    persons: List<Person>,
    onAddPerson: () -> Unit,
    onPersonClick: (Person) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relationship Tracker") },
                actions = {
                    Button(onClick = onAddPerson) {
                        Text("Add Person")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(persons) { person ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onPersonClick(person) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Category: ${person.category}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Last Contact: ${
                                SimpleDateFormat("yyyy-MM-dd").format(Date(person.lastContactTime))
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddPersonDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue()) }
    var impression by remember { mutableStateOf(TextFieldValue()) }
    var interests by remember { mutableStateOf(TextFieldValue()) }
    var goals by remember { mutableStateOf(TextFieldValue()) }
    var category by remember { mutableStateOf(TextFieldValue()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Person") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = impression,
                    onValueChange = { impression = it },
                    label = { Text("Impression") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = interests,
                    onValueChange = { interests = it },
                    label = { Text("Interests") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = goals,
                    onValueChange = { goals = it },
                    label = { Text("Goals") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g., Friend, Colleague)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.text.isNotBlank()) {
                        onAdd(
                            name.text,
                            impression.text,
                            interests.text,
                            goals.text,
                            category.text
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    person: Person,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showAddConversationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(person, selectedTag) {
        if (selectedTag == null) {
            viewModel.getConversationsByPerson(person.id).collectLatest { conversations = it }
        } else {
            viewModel.getConversationsByTag(person.id, selectedTag!!).collectLatest { conversations = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Back")
                    }
                },
                actions = {
                    Button(onClick = { showAddConversationDialog = true }) {
                        Text("Add Conversation")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Impression: ${person.impression}", style = MaterialTheme.typography.bodyMedium)
            Text("Interests: ${person.interests}", style = MaterialTheme.typography.bodyMedium)
            Text("Goals: ${person.goals}", style = MaterialTheme.typography.bodyMedium)
            Text("Category: ${person.category}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Tag filter buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("All", "Emotional", "Practical", "Validation", "Share", "Information", "Casual").forEach { tag ->
                    Button(
                        onClick = { selectedTag = if (tag == "All") null else tag },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTag == tag || (tag == "All" && selectedTag == null))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(tag)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(conversations) { conversation ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = conversation.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Tag: ${conversation.tag}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(conversation.timestamp)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddConversationDialog) {
        AddConversationDialog(
            onDismiss = { showAddConversationDialog = false },
            onAdd = { content, tag ->
                viewModel.addConversation(person.id, content, tag)
                showAddConversationDialog = false
            }
        )
    }
}

@Composable
fun AddConversationDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var content by remember { mutableStateOf(TextFieldValue()) }
    var selectedTag by remember { mutableStateOf("Emotional") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Conversation") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Conversation Content") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select Tag:")
                listOf("Emotional", "Practical", "Validation", "Share", "Information", "Casual").forEach { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag }
                        )
                        Text(tag)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.text.isNotBlank()) {
                        onAdd(content.text, selectedTag)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}