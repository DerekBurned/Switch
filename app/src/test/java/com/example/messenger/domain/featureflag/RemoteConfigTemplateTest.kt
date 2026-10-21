package com.example.messenger.domain.featureflag

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import java.io.File

class RemoteConfigTemplateTest {

    private val parameters: List<Pair<String, JsonObject>> = run {
        val template = Json.parseToJsonElement(templateFile().readText()).jsonObject
        val ungrouped = template["parameters"]?.jsonObject.orEmpty().entries
        val grouped = template["parameterGroups"]?.jsonObject.orEmpty().values
            .flatMap { group -> group.jsonObject["parameters"]?.jsonObject.orEmpty().entries }
        (ungrouped + grouped).map { it.key to it.value.jsonObject }
    }

    @Test
    fun `the template lists every flag key exactly once and nothing else`() {
        assertThat(parameters.map { it.first })
            .containsExactlyInAnyOrder(*FeatureFlag.entries.map { it.key }.toTypedArray())
    }

    @Test
    fun `every parameter is a boolean that keeps the in-app default`() {
        val overriding = parameters.filterNot { (_, parameter) ->
            parameter["valueType"]?.jsonPrimitive?.content == "BOOLEAN" &&
                parameter["defaultValue"]?.jsonObject?.get("useInAppDefault")?.jsonPrimitive?.booleanOrNull == true &&
                parameter["conditionalValues"] == null
        }.map { it.first }

        assertThat(overriding).isEmpty()
    }

    private fun templateFile(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, TEMPLATE_NAME) }
            .first { it.isFile }

    private companion object {
        const val TEMPLATE_NAME = "remoteconfig.template.json"
    }
}
