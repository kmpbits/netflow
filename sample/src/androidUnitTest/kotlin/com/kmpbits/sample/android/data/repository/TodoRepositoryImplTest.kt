package com.kmpbits.sample.android.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.sample.android.data.database.AppDatabase
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.data.mapper.toEntity
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TodoRepositoryImplTest {

    private fun buildDatabase(): AppDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return AppDatabase(driver)
    }

    private val todoJson = """{"userId":1,"id":1,"title":"Buy milk","completed":false}"""

    // region addTodo

    @Test
    fun `addTodo posts to correct path`() = runTest {
        val mockClient = MockNetFlowClient { NetFlowMockResponse.success(todoJson) }
        val repo = TodoRepositoryImpl(mockClient, buildDatabase())

        repo.addTodo("Buy milk", false)
            .filterNot { it is ResultState.Loading }
            .first()

        mockClient.assertCalled("todos", HttpMethod.Post)
    }

    @Test
    fun `addTodo returns mapped model on success`() = runTest {
        val mockClient = MockNetFlowClient { NetFlowMockResponse.success(todoJson) }
        val repo = TodoRepositoryImpl(mockClient, buildDatabase())

        val result = repo.addTodo("Buy milk", false)
            .filterNot { it is ResultState.Loading }
            .first()

        assertIs<ResultState.Success<*>>(result)
        assertEquals(1, (result as ResultState.Success).data.id)
        assertEquals("Buy milk", result.data.title)
    }

    @Test
    fun `addTodo inserts into database on success`() = runTest {
        val db = buildDatabase()
        val mockClient = MockNetFlowClient { NetFlowMockResponse.success(todoJson) }
        val repo = TodoRepositoryImpl(mockClient, db)

        repo.addTodo("Buy milk", false)
            .filterNot { it is ResultState.Loading }
            .first()

        assertEquals(1L, db.todoQueries.countTodos().executeAsOne())
        assertEquals("Buy milk", db.todoQueries.getTodos().executeAsList().first().title)
    }

    @Test
    fun `addTodo does not insert into database on error`() = runTest {
        val db = buildDatabase()
        val mockClient = MockNetFlowClient { NetFlowMockResponse.serverError() }
        val repo = TodoRepositoryImpl(mockClient, db)

        repo.addTodo("Buy milk", false)
            .filterNot { it is ResultState.Loading }
            .first()

        assertEquals(0L, db.todoQueries.countTodos().executeAsOne())
    }

    @Test
    fun `addTodo returns Error on server error`() = runTest {
        val mockClient = MockNetFlowClient { NetFlowMockResponse.serverError() }
        val repo = TodoRepositoryImpl(mockClient, buildDatabase())

        val result = repo.addTodo("Buy milk", false)
            .filterNot { it is ResultState.Loading }
            .first()

        assertIs<ResultState.Error<*>>(result)
    }

    // endregion

    // region updateTodo

    @Test
    fun `updateTodo puts to correct path`() = runTest {
        val mockClient = MockNetFlowClient { NetFlowMockResponse.success(todoJson) }
        val repo = TodoRepositoryImpl(mockClient, buildDatabase())

        repo.updateTodo(1, "Buy milk", false)
            .filterNot { it is ResultState.Loading }
            .first()

        mockClient.assertCalled("todos/1", HttpMethod.Put)
    }

    @Test
    fun `updateTodo is called exactly once`() = runTest {
        val mockClient = MockNetFlowClient { NetFlowMockResponse.success(todoJson) }
        val repo = TodoRepositoryImpl(mockClient, buildDatabase())

        repo.updateTodo(1, "Buy milk", false)
            .filterNot { it is ResultState.Loading }
            .first()

        mockClient.assertCalledTimes("todos/1", HttpMethod.Put, times = 1)
    }

    @Test
    fun `updateTodo updates record in database on success`() = runTest {
        val db = buildDatabase()
        db.todoQueries.insertTodo(TodoDto(userId = 1, id = 1, title = "Old title", completed = false).toEntity())

        val updatedJson = """{"userId":1,"id":1,"title":"New title","completed":true}"""
        val mockClient = MockNetFlowClient { NetFlowMockResponse.success(updatedJson) }
        val repo = TodoRepositoryImpl(mockClient, db)

        repo.updateTodo(1, "New title", true)
            .filterNot { it is ResultState.Loading }
            .first()

        val todo = db.todoQueries.getTodos().executeAsList().first()
        assertEquals("New title", todo.title)
        assertEquals(true, todo.completed)
    }

    // endregion

    // region deleteTodo

    private val deleteSuccessResponse = NetFlowMockResponse.success()

    @Test
    fun `deleteTodo deletes from correct path`() = runTest {
        val mockClient = MockNetFlowClient { deleteSuccessResponse }
        val repo = TodoRepositoryImpl(mockClient, buildDatabase())

        repo.deleteTodo(1)

        mockClient.assertCalled("todos/1", HttpMethod.Delete)
    }

    @Test
    fun `deleteTodo returns Success`() = runTest {
        val mockClient = MockNetFlowClient { deleteSuccessResponse }
        val repo = TodoRepositoryImpl(mockClient, buildDatabase())

        val result = repo.deleteTodo(1)

        assertIs<AsyncState.Success<Unit>>(result)
    }

    @Test
    fun `deleteTodo removes item from database on success`() = runTest {
        val db = buildDatabase()
        db.todoQueries.insertTodo(TodoDto(userId = 1, id = 1, title = "Buy milk", completed = false).toEntity())

        val mockClient = MockNetFlowClient { deleteSuccessResponse }
        val repo = TodoRepositoryImpl(mockClient, db)

        repo.deleteTodo(1)

        assertEquals(0L, db.todoQueries.countTodos().executeAsOne())
    }

    @Test
    fun `deleteTodo does not call delete path on wrong id`() = runTest {
        val mockClient = MockNetFlowClient { deleteSuccessResponse }
        val repo = TodoRepositoryImpl(mockClient, buildDatabase())

        repo.deleteTodo(99)

        mockClient.assertNotCalled("todos/1", HttpMethod.Delete)
    }

    // endregion
}
