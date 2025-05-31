package com.example.relationshiptracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.relationshiptracker.data.db.dao.ConversationWithPerson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.*

import com.example.relationshiptracker.data.db.entities.Conversation
import com.example.relationshiptracker.ui.viewmodel.MainViewModel
import com.example.relationshiptracker.data.db.entities.Person
import com.example.relationshiptracker.data.db.entities.ConversationCategory
import kotlinx.coroutines.flow.collect

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // viewMode: AllConversations or ContactList
    var viewMode by remember { mutableStateOf("ContactList") }

    // ContactList:
    // selectedPerson: The person picked on ContactList
    var selectedPerson by remember { mutableStateOf<Person?>(null) }
    // persons: The filtered and sorted people list shown on ContactList
    var persons by remember { mutableStateOf<List<Person>>(emptyList()) }


    // AllConversations:
    // conversations: The conversations shown on AllConversations
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    // conversationStats: Stats of the conversation show on the tag buttons on AllConversations
    var conversationStats by remember { mutableStateOf<Map<ConversationCategory, Int>>(emptyMap()) }
    // selectedTag: The selected one of the tags (All, Emotional, Practical ...) shown on AllConversations
    var selectedTag by remember { mutableStateOf<String?>(null) }

    var conversationsWithPersons by remember { mutableStateOf<List<ConversationWithPerson>>(emptyList()) }


    // FilterDialog:
    // allCategories: For FilterDialog to show all the categories correctly, unaffected by changes in persons
    var allCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    // selectedCategories: The categories for FilterDialog to filter persons
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }


    // Flags
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showEditConversationDialog by remember { mutableStateOf<Conversation?>(null) }
    var sortOption by remember { mutableStateOf("Last Contact") }
    var sortAscending by remember { mutableStateOf(false) }
    var pendingDeleteConversation by remember { mutableStateOf<Conversation?>(null) }


    // SAF launcher for exporting database
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportDatabaseToCsv(it) }
    }

    // Log initial state
    LaunchedEffect(Unit) {
        Log.d("MainScreen", "Initial state: " +
                "viewMode=$viewMode, " +
                "selectedPerson=${selectedPerson?.id}, " +
                "personsCount=${persons.size}, " +
                "conversationsCount=${conversations.size}, " +
                "selectedCategories=$selectedCategories, " +
                "sortOption=$sortOption, " +
                "sortAscending=$sortAscending, " +
                "selectedTag=$selectedTag"
        )
        viewModel.allPersons.collectLatest { allPersons ->
            allCategories = allPersons.map { it.category }.distinct().filter { it.isNotBlank() }
        }
    }

    // Log when state changes
    LaunchedEffect(viewMode) {
        Log.d("MainScreen", "viewMode changed to: $viewMode")
    }

    LaunchedEffect(selectedPerson) {
        Log.d("MainScreen", "selectedPerson changed to: ${selectedPerson?.id ?: "null"}")
    }

    LaunchedEffect(persons.size) {
        Log.d("MainScreen", "persons list updated. Count: ${persons.size}")
    }

    LaunchedEffect(conversations.size) {
        Log.d("MainScreen", "conversations list updated. Count: ${conversations.size}")
    }

    LaunchedEffect(selectedCategories) {
        Log.d("MainScreen", "selectedCategories changed to: $selectedCategories")
    }

    // Re-order the AllContacts page when sort or filter options changes
    LaunchedEffect(selectedCategories, sortOption, sortAscending) {
        Log.d("MainScreen", "Collecting persons with filters: " +
                "categories=$selectedCategories, " +
                "sort=$sortOption, " +
                "ascending=$sortAscending"
        )
        if (selectedCategories.isEmpty()) {
            viewModel.allPersons.collectLatest {
                persons = it.sortedBySortOption(sortOption, sortAscending)
                Log.d("MainScreen", "Updated all persons: ${it.size} items")
            }
        } else {
            viewModel.getPersonsByCategories(selectedCategories).collectLatest {
                persons = it.sortedBySortOption(sortOption, sortAscending)
                Log.d("MainScreen", "Updated filtered persons: ${it.size} items")
            }
        }
    }

    // Collect conversation data when chosen conversation tag changes
    // Not sure why need to re-collect conversation when viewMode changes, kept for safe
    LaunchedEffect(viewMode, selectedTag) {
        Log.d("MainScreen", "Collecting conversations: " +
                "viewMode=$viewMode, " +
                "selectedTag=$selectedTag"
        )

        if (viewMode == "AllConversations") {
            // Determine the conversations flow based on selectedTag
            val conversationsFlow = selectedTag?.let { tag ->
                viewModel.getAllConversationsByTag(tag)
            } ?: viewModel.getAllConversations()

            // Combine both flows
            combine(
                conversationsFlow,
                viewModel.getAllConversationStats()
            ) { convs, stats ->
                // Update both values together
                conversations = convs
                conversationStats = stats

                // Log updates
                Log.d("MainScreen", "Updated conversations: ${convs.size} items")
                Log.d("MainScreen", "Updated conversation stats: $stats")
            }.collect() // Collect the combined flow
        }
    }

    // If selected some person, the whole screen becomes PersonDetailScreen
    when {
        selectedPerson != null -> {
            PersonDetailScreen(
                person = selectedPerson!!,
                viewModel = viewModel,
                onBack = { selectedPerson = null },
                onPersonUpdate = { updatedPerson ->
                    viewModel.updatePerson(updatedPerson)
                    selectedPerson = updatedPerson
                },
                onPersonDelete = {
                    viewModel.deletePerson(selectedPerson!!)
                    selectedPerson = null
                }
            )
        }
        // If not selected, the main screen keeps the same top and bottom bar
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Relationship Tracker") },
                        actions = {
                            // Only show the sorting function in Contact list
                            if (viewMode == "ContactList") {
                                IconButton(onClick = { showFilterDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.FilterList,
                                        contentDescription = "Filter by category"
                                    )
                                }
                                IconButton(onClick = { showSortDialog = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort options"
                                    )
                                }
                                IconButton(onClick = { showAddPersonDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add new person"
                                    )
                                }
                            }
                            // Only show export button in All Conversations view
                            else{
                                IconButton(onClick = {
                                    exportLauncher.launch("relationship_tracker_export_${System.currentTimeMillis()}.csv")
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Upload,
                                        contentDescription = "Export database"
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewMode = "ContactList" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewMode == "ContactList")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("All Contacts")
                        }
                        Button(
                            onClick = { viewMode = "AllConversations" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewMode == "AllConversations")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("All Conversations")
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    // Switch between AllConversations and ContactList for inner view.
                    when (viewMode) {
                        "ContactList" -> PersonListScreen(
                            persons = persons,
                            onPersonClick = { person -> selectedPerson = person },
                            allCategories = persons.map { it.category }.distinct()
                                .filter { it.isNotBlank() }
                        )
                        "AllConversations" -> AllConversationsView(
                            viewModel = viewModel,
                            conversations = conversations,
                            conversationStats = conversationStats,
                            selectedTag = selectedTag,
                            onTagSelect = { tag -> selectedTag = if (tag == "All") null else tag },
                            onConversationEdit = { conversation -> showEditConversationDialog = conversation }
                        )
                    }
                }
            }

            if (showAddPersonDialog) {
                AddPersonDialog(
                    onDismiss = { showAddPersonDialog = false },
                    onAdd = { name, category ->
                        viewModel.addPerson(name, "", "", "", category)
                        showAddPersonDialog = false
                    },
                    existingCategories = persons.map { it.category }.distinct().filter { it.isNotBlank() }
                )
            }
            if (showFilterDialog) {
                FilterDialog(
                    allCategories = allCategories,
                    selectedCategories = selectedCategories,
                    onCategoryToggle = { category ->
                        selectedCategories = if (category in selectedCategories) {
                            selectedCategories - category
                        } else {
                            selectedCategories + category
                        }
                    },
                    onDismiss = { showFilterDialog = false },
                    onClear = { selectedCategories = setOf() }
                )
            }
            if (showSortDialog) {
                SortDialog(
                    currentOption = sortOption,
                    currentAscending = sortAscending,
                    onSortSelect = { option, ascending ->
                        sortOption = option
                        sortAscending = ascending
                        showSortDialog = false
                    },
                    onDismiss = { showSortDialog = false }
                )
            }
            if (showEditConversationDialog != null) {
                EditConversationDialog(
                    conversation = showEditConversationDialog!!,
                    onDismiss = { showEditConversationDialog = null },
                    onUpdate = { updatedConversation ->
                        viewModel.updateConversation(updatedConversation)
                        showEditConversationDialog = null
                    },
                    onDelete = { pendingDeleteConversation = showEditConversationDialog }
                )
            }
            if (pendingDeleteConversation != null) {
                ConfirmDeleteDialog(
                    message = "Are you sure you want to delete this conversation?",
                    onConfirm = {
                        viewModel.deleteConversation(pendingDeleteConversation!!)
                        pendingDeleteConversation = null
                        showEditConversationDialog = null
                    },
                    onCancel = { pendingDeleteConversation = null }
                )
            }
        }
    }
}

@Composable
fun PersonListScreen(
    persons: List<Person>,
    onPersonClick: (Person) -> Unit,
    allCategories: List<String>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Category: ${person.category.ifBlank { "None" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (person.lastContactTime != 0L) {  // Only show if time is not 0
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
fun AllConversationsView(
    viewModel: MainViewModel,
    conversations: List<Conversation>,
    conversationStats: Map<ConversationCategory, Int>,
    selectedTag: String?,
    onTagSelect: (String) -> Unit,
    onConversationEdit: (Conversation) -> Unit
) {
    var localSelectedTag by remember { mutableStateOf(selectedTag) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Emotional", "Practical", "Validation", "Share", "Information", "Casual").forEach { tag ->
                val count = when (tag) {
                    "All" -> conversationStats.values.sum()
                    "Emotional" -> conversationStats[ConversationCategory.EMOTIONAL] ?: 0
                    "Practical" -> conversationStats[ConversationCategory.PRACTICAL] ?: 0
                    "Validation" -> conversationStats[ConversationCategory.VALIDATION] ?: 0
                    "Share" -> conversationStats[ConversationCategory.SHARE] ?: 0
                    "Information" -> conversationStats[ConversationCategory.INFORMATION] ?: 0
                    "Casual" -> conversationStats[ConversationCategory.CASUAL] ?: 0
                    else -> 0
                }
                Button(
                    onClick = {
                        localSelectedTag = if (tag == "All") null else tag
                        onTagSelect(tag)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (localSelectedTag == tag || (tag == "All" && localSelectedTag == null))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("$tag ($count)")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(conversations) { conversation ->
                var personName by remember { mutableStateOf("") }

                // Fetch person name for this conversation
                LaunchedEffect(conversation.personId) {
                    viewModel.getPersonById(conversation.personId)?.let { person ->
                        personName = person.name
                    } ?: run { personName = "Unknown" }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onConversationEdit(conversation) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = conversation.content,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Tag: ${conversation.tag}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "With: ${personName}",
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

@Composable
fun FilterDialog(
    allCategories: List<String>,
    selectedCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Category") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                allCategories.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = category in selectedCategories,
                            onCheckedChange = { onCategoryToggle(category) }
                        )
                        Text(category)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            Button(onClick = { onCategoryToggle(""); onClear() }) {
                Text("Clear")
            }
        }
    )
}

@Composable
fun SortDialog(
    currentOption: String,
    currentAscending: Boolean,
    onSortSelect: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sortOptions = listOf("Last Contact", "Name", "Conversation Count")
    var selectedOption by remember { mutableStateOf(currentOption) }
    var ascending by remember { mutableStateOf(currentAscending) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Options") },
        text = {
            Column {
                sortOptions.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option }
                        )
                        Text(option)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { ascending = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ascending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Ascending")
                    }
                    Button(
                        onClick = { ascending = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!ascending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Descending")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSortSelect(selectedOption, ascending); onDismiss() }) {
                Text("Apply")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddPersonDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    existingCategories: List<String>
) {
    var name by remember { mutableStateOf(TextFieldValue()) }
    var selectedCategory by remember { mutableStateOf(TextFieldValue()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Person") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select Category:")
                existingCategories.forEach { category ->
                    Button(
                        onClick = { selectedCategory = TextFieldValue(category) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory.text == category)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(category)
                    }
                }
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { selectedCategory = it },
                    label = { Text("Or enter new category") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.text.isNotBlank()) {
                        onAdd(name.text, selectedCategory.text)
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

@Composable
fun ConfirmDeleteDialog(
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Confirm Deletion") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
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
    onBack: () -> Unit,
    onPersonUpdate: (Person) -> Unit,
    onPersonDelete: () -> Unit
) {
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showAddConversationDialog by remember { mutableStateOf(false) }
    var showEditConversationDialog by remember { mutableStateOf<Conversation?>(null) }
    var viewMode by remember { mutableStateOf("Contact") }
    var conversationStats by remember { mutableStateOf<Map<ConversationCategory, Int>>(emptyMap()) }
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingImpression by remember { mutableStateOf(false) }
    var isEditingInterests by remember { mutableStateOf(false) }
    var isEditingGoals by remember { mutableStateOf(false) }
    var isEditingCategory by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(TextFieldValue(person.name)) }
    var impression by remember { mutableStateOf(TextFieldValue(person.impression)) }
    var interests by remember { mutableStateOf(TextFieldValue(person.interests)) }
    var goals by remember { mutableStateOf(TextFieldValue(person.goals)) }
    var category by remember { mutableStateOf(TextFieldValue(person.category)) }
    var pendingDeletePerson by remember { mutableStateOf<Person?>(null) }
    var pendingDeleteConversation by remember { mutableStateOf<Conversation?>(null) }

    LaunchedEffect(person, selectedTag, viewMode) {
        if (viewMode == "Conversation") {
            if (selectedTag == null) {
                viewModel.getConversationsByPerson(person.id).collectLatest { conversations = it }
            } else {
                viewModel.getConversationsByTag(person.id, selectedTag!!).collectLatest { conversations = it }
            }
        } else {
            viewModel.getConversationStats(person.id.toLong()).collectLatest { conversationStats = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddConversationDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Conversation"
                        )
                    }
                    IconButton(onClick = { pendingDeletePerson = person }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete Contact"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewMode = "Contact" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewMode == "Contact")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Contact View")
                }
                Button(
                    onClick = { viewMode = "Conversation" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewMode == "Conversation")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Conversation View")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (viewMode == "Contact") {
                ContactView(
                    person = person,
                    conversationStats = conversationStats,
                    isEditingName = isEditingName,
                    isEditingImpression = isEditingImpression,
                    isEditingInterests = isEditingInterests,
                    isEditingGoals = isEditingGoals,
                    isEditingCategory = isEditingCategory,
                    name = name,
                    impression = impression,
                    interests = interests,
                    goals = goals,
                    category = category,
                    onEditToggle = { field ->
                        when (field) {
                            "name" -> isEditingName = !isEditingName
                            "impression" -> isEditingImpression = !isEditingImpression
                            "interests" -> isEditingInterests = !isEditingInterests
                            "goals" -> isEditingGoals = !isEditingGoals
                            "category" -> isEditingCategory = !isEditingCategory
                        }
                    },
                    onValueChange = { field, value ->
                        when (field) {
                            "name" -> name = value
                            "impression" -> impression = value
                            "interests" -> interests = value
                            "goals" -> goals = value
                            "category" -> category = value
                        }
                        onPersonUpdate(
                            person.copy(
                                name = name.text,
                                impression = impression.text,
                                interests = interests.text,
                                goals = goals.text,
                                category = category.text
                            )
                        )
                    }
                )
            } else {
                ConversationView(
                    viewModel = viewModel,
                    conversations = conversations,
                    conversationStats = conversationStats,
                    selectedTag = selectedTag,
                    onTagSelect = { tag -> selectedTag = if (tag == "All") null else tag },
                    onConversationEdit = { conversation -> showEditConversationDialog = conversation }
                )
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

    if (showEditConversationDialog != null) {
        EditConversationDialog(
            conversation = showEditConversationDialog!!,
            onDismiss = { showEditConversationDialog = null },
            onUpdate = { updatedConversation ->
                viewModel.updateConversation(updatedConversation)
                showEditConversationDialog = null
            },
            onDelete = { pendingDeleteConversation = showEditConversationDialog }
        )
    }

    if (pendingDeletePerson != null) {
        ConfirmDeleteDialog(
            message = "Are you sure you want to delete ${pendingDeletePerson!!.name}?",
            onConfirm = {
                viewModel.deletePerson(pendingDeletePerson!!)
                pendingDeletePerson = null
                onBack()
            },
            onCancel = { pendingDeletePerson = null }
        )
    }

    if (pendingDeleteConversation != null) {
        ConfirmDeleteDialog(
            message = "Are you sure you want to delete this conversation?",
            onConfirm = {
                viewModel.deleteConversation(pendingDeleteConversation!!)
                pendingDeleteConversation = null
                showEditConversationDialog = null
            },
            onCancel = { pendingDeleteConversation = null }
        )
    }
}

@Composable
fun ContactView(
    person: Person,
    conversationStats: Map<ConversationCategory, Int>,
    isEditingName: Boolean,
    isEditingImpression: Boolean,
    isEditingInterests: Boolean,
    isEditingGoals: Boolean,
    isEditingCategory: Boolean,
    name: TextFieldValue,
    impression: TextFieldValue,
    interests: TextFieldValue,
    goals: TextFieldValue,
    category: TextFieldValue,
    onEditToggle: (String) -> Unit,
    onValueChange: (String, TextFieldValue) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onEditToggle("name") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name", style = MaterialTheme.typography.titleMedium)
                    if (isEditingName) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { onValueChange("name", it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(name.text.ifBlank { "Click to edit" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onEditToggle("impression") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Impression", style = MaterialTheme.typography.titleMedium)
                    if (isEditingImpression) {
                        OutlinedTextField(
                            value = impression,
                            onValueChange = { onValueChange("impression", it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(impression.text.ifBlank { "Click to edit" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onEditToggle("interests") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interests", style = MaterialTheme.typography.titleMedium)
                    if (isEditingInterests) {
                        OutlinedTextField(
                            value = interests,
                            onValueChange = { onValueChange("interests", it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(interests.text.ifBlank { "Click to edit" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onEditToggle("goals") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Goals", style = MaterialTheme.typography.titleMedium)
                    if (isEditingGoals) {
                        OutlinedTextField(
                            value = goals,
                            onValueChange = { onValueChange("goals", it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(goals.text.ifBlank { "Click to edit" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onEditToggle("category") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Category", style = MaterialTheme.typography.titleMedium)
                    if (isEditingCategory) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { onValueChange("category", it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(category.text.ifBlank { "Click to edit" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Conversation Statistics", style = MaterialTheme.typography.titleLarge)
        }
        items(ConversationCategory.values().toList()) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${category.name}: ${conversationStats[category] ?: 0}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationView(
    viewModel: MainViewModel,
    conversations: List<Conversation>,
    conversationStats: Map<ConversationCategory, Int>,
    selectedTag: String?,
    onTagSelect: (String) -> Unit,
    onConversationEdit: (Conversation) -> Unit
) {
    var localSelectedTag by remember { mutableStateOf(selectedTag) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Emotional", "Practical", "Validation", "Share", "Information", "Casual").forEach { tag ->
                val count = when (tag) {
                    "All" -> conversationStats.values.sum()
                    "Emotional" -> conversationStats[ConversationCategory.EMOTIONAL] ?: 0
                    "Practical" -> conversationStats[ConversationCategory.PRACTICAL] ?: 0
                    "Validation" -> conversationStats[ConversationCategory.VALIDATION] ?: 0
                    "Share" -> conversationStats[ConversationCategory.SHARE] ?: 0
                    "Information" -> conversationStats[ConversationCategory.INFORMATION] ?: 0
                    "Casual" -> conversationStats[ConversationCategory.CASUAL] ?: 0
                    else -> 0
                }
                Button(
                    onClick = {
                        localSelectedTag = if (tag == "All") null else tag
                        onTagSelect(tag)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (localSelectedTag == tag || (tag == "All" && localSelectedTag == null))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("$tag ($count)")
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
                        .clickable { onConversationEdit(conversation) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = conversation.content,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Tag: ${conversation.tag}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(conversation.timestamp)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
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
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Select Tag:")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp), // Reduced gap
                    verticalArrangement = Arrangement.spacedBy(2.dp) // Reduced gap
                ) {
                    listOf("Emotional", "Practical", "Validation", "Share", "Information", "Casual").forEach { tag ->
                        Button(
                            onClick = { selectedTag = tag },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTag == tag)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.padding(vertical = 1.dp), // Reduced padding
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp) // Tighter content padding
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium // Smaller font size
                            )
                        }
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

@Composable
fun EditConversationDialog(
    conversation: Conversation,
    onDismiss: () -> Unit,
    onUpdate: (Conversation) -> Unit,
    onDelete: () -> Unit
) {
    var content by remember { mutableStateOf(TextFieldValue(conversation.content)) }
    var selectedTag by remember { mutableStateOf(conversation.tag ?: "Casual") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Conversation") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Conversation Content") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Select Tag:")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp), // Reduced gap
                    verticalArrangement = Arrangement.spacedBy(2.dp) // Reduced gap
                ) {
                    listOf("Emotional", "Practical", "Validation", "Share", "Information", "Casual").forEach { tag ->
                        Button(
                            onClick = { selectedTag = tag },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTag == tag)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.padding(vertical = 1.dp), // Reduced padding
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp) // Tighter content padding
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium // Smaller font size
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.text.isNotBlank()) {
                        onUpdate(
                            conversation.copy(
                                content = content.text,
                                tag = selectedTag,
                                category = when (selectedTag) {
                                    "Emotional" -> ConversationCategory.EMOTIONAL
                                    "Practical" -> ConversationCategory.PRACTICAL
                                    "Validation" -> ConversationCategory.VALIDATION
                                    "Share" -> ConversationCategory.SHARE
                                    "Information" -> ConversationCategory.INFORMATION
                                    else -> ConversationCategory.CASUAL
                                }
                            )
                        )
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            Row {
                Button(onClick = onDelete) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
fun List<Person>.sortedBySortOption(sortOption: String, ascending: Boolean): List<Person> {
    return when (sortOption) {
        "Name" -> if (ascending) sortedBy { it.name } else sortedByDescending { it.name }
        "Conversation Count" -> if (ascending) sortedBy { it.id } else sortedByDescending { it.id }
        else -> if (ascending) sortedBy { it.lastContactTime } else sortedByDescending { it.lastContactTime }
    }
}