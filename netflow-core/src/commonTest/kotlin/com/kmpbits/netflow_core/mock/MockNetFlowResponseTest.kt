package com.kmpbits.netflow_core.mock

import com.kmpbits.netflow_core.enums.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MockNetFlowResponseTest {

    @Test
    fun `unauthorized has code 401`() {
        val r = NetFlowMockResponse.unauthorized()
        assertEquals(401, r.code)
    }

    @Test
    fun `MockResponseQueue serves responses in order then repeats last`() {
        val q = MockResponseQueue(
            NetFlowMockResponse.unauthorized(),
            NetFlowMockResponse.success("""{"ok":true}"""),
        )
        assertEquals(401, q.next().code)
        assertEquals(200, q.next().code)
        assertEquals(200, q.next().code)
    }

    @Test
    fun `queue drives a MockNetFlowClient across retries`() = runTest {
        val q = MockResponseQueue(
            NetFlowMockResponse.unauthorized(),
            NetFlowMockResponse.success("""{"id":1}"""),
        )
        val client = MockNetFlowClient { q.next() }

        val first = client.call { path = "thing" }.response()
        val second = client.call { path = "thing" }.response()

        assertEquals(401, first.code)
        assertEquals(200, second.code)
        client.assertCalledTimes("thing", HttpMethod.Get, times = 2)
    }
}
