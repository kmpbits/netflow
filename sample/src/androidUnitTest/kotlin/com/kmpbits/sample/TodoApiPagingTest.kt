package com.kmpbits.sample

import androidx.paging.testing.asSnapshot
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.sample.android.data.repository.TodoApiRepositoryImpl
import com.kmpbits.sample.android.domain.model.Todo
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodoApiPagingTest {

    @Test
    fun pagedTodos_snapshot_returns_mapped_domain_models() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""[{"userId":1,"id":1,"title":"a","completed":false}]""")
        }
        val repo = TodoApiRepositoryImpl(client)

        val items: List<Todo> = repo.pagedTodos().asSnapshot()

        assertEquals(listOf(Todo(userId = 1, id = 1, title = "a", completed = false)), items)
    }

    @Test
    fun generated_impl_uses_network_only_responsePaginated() {
        val generated = generatedImplFile().readText()

        assertTrue("@OptIn(ExperimentalPagingApi::class)" in generated, generated)
        assertTrue(".responsePaginated<TodoDto, TodoDto>" in generated, generated)
        assertTrue("onlyApiCall = true" in generated, generated)
        assertTrue("defaultPageSize = 15" in generated, generated)
        assertTrue("pageQueryName" !in generated, "no pageQueryName line expected:\n$generated")
    }

    private fun generatedImplFile(): File {
        val roots = listOf(
            File("build/generated/ksp/metadata/commonMain/kotlin"),
            File("sample/build/generated/ksp/metadata/commonMain/kotlin"),
        )
        val root = roots.firstOrNull { it.isDirectory }
            ?: error("KSP generated sources dir not found (cwd=${File(".").absolutePath})")
        return root.walkTopDown().first { it.name == "_TodoApiImpl.kt" }
    }
}
