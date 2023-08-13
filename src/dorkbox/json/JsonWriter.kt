/*
 * Copyright 2023 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*******************************************************************************
 * Copyright 2011 Mario Zechner, Nathan Sweet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.json

import java.io.IOException
import java.io.Writer
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Builder style API for emitting JSON.
 *
 * @author Nathan Sweet
 */
class JsonWriter(val writer: Writer) : Writer() {

    private val stack = ArrayList<JsonObject>()
    private var current: JsonObject? = null
    private var named = false
    private var outputType = OutputType.json
    private var quoteLongValues = false

    /** Sets the type of JSON output. Default is [OutputType.minimal].  */
    fun setOutputType(outputType: OutputType) {
        this.outputType = outputType
    }

    /**
     * When true, quotes long, double, BigInteger, BigDecimal types to prevent truncation in languages like JavaScript and PHP.
     * This is not necessary when using libgdx, which handles these types without truncation. Default is false.
     */
    fun setQuoteLongValues(quoteLongValues: Boolean) {
        this.quoteLongValues = quoteLongValues
    }

    @Throws(IOException::class)
    fun name(name: String): JsonWriter {
        check(!(current == null || current!!.array)) { "Current item must be an object." }

        if (!current!!.needsComma) {
            current!!.needsComma = true
        } else {
            writer.write(','.code)
        }

        writer.write(outputType.quoteName(name))
        writer.write(':'.code)
        named = true
        return this
    }

    @Throws(IOException::class)
    fun `object`(): JsonWriter {
        requireCommaOrName()
        stack.add(JsonObject(false).also { current = it })
        return this
    }

    @Throws(IOException::class)
    fun array(): JsonWriter {
        requireCommaOrName()
        stack.add(JsonObject(true).also { current = it })
        return this
    }

    @Throws(IOException::class)
    fun value(value: Any?): JsonWriter {
        @Suppress("NAME_SHADOWING")
        var value = value

        if (quoteLongValues && (value is Long || value is Double || value is BigDecimal || value is BigInteger)) {
            value = value.toString()
        } else if (value is Number) {
            val number = value
            val longValue = number.toLong()

            if (number.toDouble() == longValue.toDouble()) {
                value = longValue
            }
        }
        requireCommaOrName()
        writer.write(outputType.quoteValue(value))
        return this
    }

    /** Writes the specified JSON value, without quoting or escaping.  */
    @Throws(IOException::class)
    fun json(json: String): JsonWriter {
        requireCommaOrName()
        writer.write(json)
        return this
    }

    @Throws(IOException::class)
    private fun requireCommaOrName() {
        if (current == null) return

        if (current!!.array) {
            if (!current!!.needsComma) {
                current!!.needsComma = true
            } else {
                writer.write(','.code)
            }
        } else {
            check(named) { "Name must be set." }
            named = false
        }
    }

    @Throws(IOException::class)
    fun `object`(name: String): JsonWriter {
        return name(name).`object`()
    }

    @Throws(IOException::class)
    fun array(name: String): JsonWriter {
        return name(name).array()
    }

    @Throws(IOException::class)
    operator fun set(name: String, value: Any): JsonWriter {
        return name(name).value(value)
    }

    /** Writes the specified JSON value, without quoting or escaping.  */
    @Throws(IOException::class)
    fun json(name: String, json: String): JsonWriter {
        return name(name).json(json)
    }

    @Throws(IOException::class)
    fun pop(): JsonWriter {
        check(!named) { "Expected an object, array, or value since a name was set." }

        stack.removeAt(stack.size - 1).close()
        current = if (stack.size == 0) {
            null
        } else {
            stack[stack.size - 1]
        }
        return this
    }

    @Throws(IOException::class)
    override fun write(cbuf: CharArray, off: Int, len: Int) {
        writer.write(cbuf, off, len)
    }

    @Throws(IOException::class)
    override fun flush() {
        writer.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        while (stack.size > 0) pop()
        writer.close()
    }

    private inner class JsonObject(val array: Boolean) {
        var needsComma = false

        init {
            writer.write((if (array) '[' else '{').code)
        }

        @Throws(IOException::class)
        fun close() {
            writer.write((if (array) ']' else '}').code)
        }
    }
}
