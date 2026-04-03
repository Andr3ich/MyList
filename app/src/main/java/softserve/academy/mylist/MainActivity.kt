package softserve.academy.mylist

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import softserve.academy.mylist.ui.theme.MyListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyListTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        ShoppingListScreen()
                    }
                }
            }
        }
    }
}

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    val name: String,
    val isBought: Boolean = false,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY id DESC")
    fun getAllItems(): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem)

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Delete
    suspend fun deleteItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}

@Database(entities = [ShoppingItem::class], version = 1)
abstract class ShoppingDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: ShoppingDatabase? = null

        fun getInstance(context: Context): ShoppingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShoppingDatabase::class.java,
                    "shopping_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {
    private val dao: ShoppingDao = ShoppingDatabase.getInstance(application).shoppingDao()
    var shoppingList by mutableStateOf<List<ShoppingItem>>(emptyList())
        private set
    val boughtCount: Int get() = shoppingList.count { it.isBought }

    init {
        viewModelScope.launch {
            dao.getAllItems().collect { items -> shoppingList = items }
        }
    }

    fun addItem(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertItem(ShoppingItem(name = name.trim()))
        }
    }

    fun toggleBought(item: ShoppingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateItem(item.copy(isBought = !item.isBought))
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteItem(item)
        }
    }
}

@Composable
fun ShoppingItemCard(item: ShoppingItem, onToggleBought: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.LightGray, MaterialTheme.shapes.medium)
            .clickable { onToggleBought() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.isBought, onCheckedChange = { onToggleBought() })
        Text(text = item.name, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), fontSize = 18.sp)
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
        }
    }
}

@Composable
fun AddItemSection(addItem: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 30) { text = it; isError = false } },
            label = { Text("Назва товару") },
            modifier = Modifier.fillMaxWidth().testTag("input_field"),
            isError = isError,
            supportingText = { if (isError) Text("Назва не може бути порожньою") else Text("${text.length}/30") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { if (text.isBlank()) isError = true else { addItem(text); text = ""; isError = false } },
            modifier = Modifier.fillMaxWidth().testTag("add_button")
        ) {
            Text("Додати у список")
        }
    }
}

@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel = viewModel(
    factory = ShoppingListViewModelFactory(LocalContext.current.applicationContext as Application)
)) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AddItemSection { viewModel.addItem(it) }
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = "Куплено: ${viewModel.boughtCount} / ${viewModel.shoppingList.size}",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp).testTag("counter_text")
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(viewModel.shoppingList, key = { it.id }) { item ->
                ShoppingItemCard(item = item, onToggleBought = { viewModel.toggleBought(item) }, onDelete = { viewModel.deleteItem(item) })
            }
        }
    }
}

class ShoppingListViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShoppingListViewModel(application) as T
    }
}