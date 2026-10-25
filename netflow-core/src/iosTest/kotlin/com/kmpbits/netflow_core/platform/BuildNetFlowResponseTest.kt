package com.kmpbits.netflow_core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuildNetFlowResponseTest {

    @Test
    fun successPayloadGoesToBody() {
        val response = buildNetFlowResponse(200, emptyList(), "ok")

        assertEquals("ok", response.body)
        assertNull(response.errorBody)
    }

    @Test
    fun errorPayloadGoesToErrorBody() {
        val response = buildNetFlowResponse(422, emptyList(), """{"error":{"code":"x"}}""")

        assertNull(response.body)
        assertEquals("""{"error":{"code":"x"}}""", response.errorBody)
    }

    @Test
    fun redirectIsTreatedAsError() {
        val response = buildNetFlowResponse(302, emptyList(), "moved")

        assertEquals("moved", response.errorBody)
    }
}
