package com.example.relationshiptracker.ui.screens

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
import com.example.relationshiptracker.data.db.entities.ConversationCategory
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var viewMode by remember { mutableStateOf("ContactList") }
    var selectedPerson by remember { mutableStateOf<Person?>(null) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var persons by remember { mutableStateOf<List<Person>>(emptyList()) }
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var conversationStats by remember { mutableStateOf<Map<ConversationCategory, Int>>(emptyMap()) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var sortOption by remember { mutableStateOf("Last Contact") }
    var sortAscending by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedCategories, sortOption, sortAscending) {
        if (selectedCategories.isEmpty()) {
            viewModel.allPersons.collectLatest { persons = it.sortedBySortOption(sortOption, sortAscending) }
        } else {
            viewModel.getPersonsByCategory(selectedCategories.joinToString(",")).collectLatest {
                persons = it.sortedBySortOption(sortOption, sortAscending)
            }
        }
    }

    LaunchedEffect(viewMode, selectedTag) {
        if (viewMode == "AllConversations") {
            if (selectedTag == null) {
                viewModel.getAllConversations().collectLatest { conversations = it }
            } else {
                viewModel.getAllConversationsByTag(selectedTag!!).collectLatest { conversations = it }
            }
            viewModel.getAllConversationStats().collectLatest { conversationStats = it }
        }
    }

    when {
        selectedPerson != null -> {
            PersonDetailScreen(
                person = selectedPerson!!,
                viewModel = viewModel,
                onBack = { selectedPerson = null },
                onPersonUpdate = { updatedPerson ->
                    viewModel.updatePerson(updatedPerson)
                    selectedPerson = updatedPerson
                }
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Relationship Tracker") },
                        actions = {
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
                ) {
                    when (viewMode) {
                        "ContactList" -> PersonListScreen(
                            persons = persons,
                            onPersonClick = { person -> selectedPerson = person },
                            allCategories = persons.map { it.category }.distinct()
                                .filter { it.isNotBlank() }
                        )
                        "AllConversations" -> ConversationView(
                            viewModel = viewModel,
                            conversations = conversations,
                            conversationStats = conversationStats,
                            selectedTag = selectedTag,
                            onTagSelect = { tag -> selectedTag = if (tag == "All") null else tag }
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
                    allCategories = persons.map { it.category }.distinct().filter { it.isNotBlank() },
                    selectedCategories = selectedCategories,
                    onCategoryToggle = { category ->
                        selectedCategories = if (category in selectedCategories) {
                            selectedCategories - category
                        } else {
                            selectedCategories + category
                        }
                    },
                    onDismiss = { showFilterDialog = false }
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
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Category: ${person.category.ifBlank { "None" }}",
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

@Composable
fun FilterDialog(
    allCategories: List<String>,
    selectedCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    onDismiss: () -> Unit
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
            Button(onClick = { onCategoryToggle(""); onDismiss() }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    person: Person,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPersonUpdate: (Person) -> Unit
) {
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showAddConversationDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("Contact") }
    var conversationStats by remember { mutableStateOf<Map<ConversationCategory, Int>>(emptyMap()) }
    var isEditingImpression by remember { mutableStateOf(false) }
    var isEditingInterests by remember { mutableStateOf(false) }
    var isEditingGoals by remember { mutableStateOf(false) }
    var isEditingCategory by remember { mutableStateOf(false) }
    var impression by remember { mutableStateOf(TextFieldValue(person.impression)) }
    var interests by remember { mutableStateOf(TextFieldValue(person.interests)) }
    var goals by remember { mutableStateOf(TextFieldValue(person.goals)) }
    var category by remember { mutableStateOf(TextFieldValue(person.category)) }

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
                        Text("< Back")
                    }
                },
                actions = {
                    Button(onClick = { showAddConversationDialog = true }) {
                        Text("Add Conversation")
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
                    isEditingImpression = isEditingImpression,
                    isEditingInterests = isEditingInterests,
                    isEditingGoals = isEditingGoals,
                    isEditingCategory = isEditingCategory,
                    impression = impression,
                    interests = interests,
                    goals = goals,
                    category = category,
                    onEditToggle = { field ->
                        when (field) {
                            "impression" -> isEditingImpression = !isEditingImpression
                            "interests" -> isEditingInterests = !isEditingInterests
                            "goals" -> isEditingGoals = !isEditingGoals
                            "category" -> isEditingCategory = !isEditingCategory
                        }
                    },
                    onValueChange = { field, value ->
                        when (field) {
                            "impression" -> impression = value
                            "interests" -> interests = value
                            "goals" -> goals = value
                            "category" -> category = value
                        }
                        onPersonUpdate(
                            person.copy(
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
                    onTagSelect = { tag -> selectedTag = if (tag == "All") null else tag }
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
}

@Composable
fun ContactView(
    person: Person,
    conversationStats: Map<ConversationCategory, Int>,
    isEditingImpression: Boolean,
    isEditingInterests: Boolean,
    isEditingGoals: Boolean,
    isEditingCategory: Boolean,
    impression: TextFieldValue,
    interests: TextFieldValue,
    goals: TextFieldValue,
    category: TextFieldValue,
    onEditToggle: (String) -> Unit,
    onValueChange: (String, TextFieldValue) -> Unit
) {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
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
        Spacer(modifier = Modifier.height(16.dp))
        Text("Conversation Statistics", style = MaterialTheme.typography.titleLarge)
        LazyColumn {
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
}

@Composable
fun ConversationView(
    viewModel: MainViewModel,
    conversations: List<Conversation>,
    conversationStats: Map<ConversationCategory, Int>,
    selectedTag: String?,
    onTagSelect: (String) -> Unit
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

fun List<Person>.sortedBySortOption(sortOption: String, ascending: Boolean): List<Person> {
    return when (sortOption) {
        "Name" -> if (ascending) sortedBy { it.name } else sortedByDescending { it.name }
        "Conversation Count" -> if (ascending) sortedBy { it.id } else sortedByDescending { it.id }
        else -> if (ascending) sortedBy { it.lastContactTime } else sortedByDescending { it.lastContactTime }
    }
}