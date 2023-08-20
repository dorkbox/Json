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

import java.io.*
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight JSON parser.
 *
 * The default behavior is to parse the JSON into a DOM containing [JsonValue] objects.
 *
 * Extend this class and override methods to perform event driven parsing. When this is done, the parse methods will return null.
 *
 * @author Nathan Sweet, dorkbox, llc
 */
class JsonReader {
    fun parse(json: String): JsonValue {
        val data = json.toCharArray()
        return parse(data, 0, data.size)
    }

    fun parse(reader: Reader): JsonValue {
        return try {
            var data = CharArray(1024)
            var offset = 0
            while (true) {
                val length = reader.read(data, offset, data.size - offset)
                if (length == -1) break
                if (length == 0) {
                    val newData = CharArray(data.size * 2)
                    System.arraycopy(data, 0, newData, 0, data.size)
                    data = newData
                } else offset += length
            }

            parse(data, 0, offset)
        } catch (ex: IOException) {
            throw JsonException(ex)
        } finally {
            try {
                reader.close()
            } catch (ignored: IOException) {
            }
        }
    }

    fun parse(input: InputStream): JsonValue {
        return try {
            parse(InputStreamReader(input, "UTF-8"))
        } catch (ex: IOException) {
            throw JsonException(ex)
        } finally {
            try {
                input.close()
            } catch (ignored: IOException) {
            }
        }
    }

    fun parse(file: File): JsonValue {
        return try {
            parse(InputStreamReader(FileInputStream(file), "UTF-8"))
        } catch (ex: Exception) {
            throw JsonException("Error parsing file: $file", ex)
        }
    }

    fun parse(data: CharArray, offset: Int, length: Int): JsonValue {
        var cs: Int
        var p = offset
        val pe = length
        val eof = pe
        var top = 0
        var stack = IntArray(4)

        var s = 0
        val names = ArrayList<String>(8)

        var needsUnescape = false
        var stringIsName = false
        var stringIsUnquoted = false
        var parseRuntimeEx: RuntimeException? = null

        val debug = false
        val debugChars = false
        if (debug) println()

        try {
            // line 3 "JsonReader.rl"
            cs = json_start
            top = 0

            // line 8 "JsonReader.rl"
            var _klen: Int
            var _trans = 0
            var _acts: Int
            var _nacts: Int
            var _keys: Int

            var _goto_targ = 0

            _goto@while (true) {
                when (_goto_targ) {
                    0 -> {
                        if (p == length) {
                            _goto_targ = 4
                            continue@_goto
                        }
                        if (cs == 0) {
                            _goto_targ = 5
                            continue@_goto
                        }

                        _goto_targ = 1 // goto the next switch
                        continue@_goto
                    }

                    1 -> {
                        _match@ do {
                            _keys = _json_key_offsets[cs]
                            _trans = _json_index_offsets[cs]
                            _klen = _json_single_lengths[cs]

                            if (_klen > 0) {
                                var _lower = _keys
                                var _mid: Int
                                var _upper = _keys + _klen - 1

                                while (true) {
                                    if (_upper < _lower) break

                                    _mid = _lower + (_upper - _lower shr 1)
                                    if (data[p] < _json_trans_keys[_mid]) {
                                        _upper = _mid - 1
                                    } else if (data[p] > _json_trans_keys[_mid]) {
                                        _lower = _mid + 1
                                    } else {
                                        _trans += _mid - _keys
                                        break@_match
                                    }
                                }
                                _keys += _klen
                                _trans += _klen
                            }

                            _klen = _json_range_lengths[cs]
                            if (_klen > 0) {
                                var _lower = _keys
                                var _mid: Int
                                var _upper = _keys + (_klen shl 1) - 2
                                while (true) {
                                    if (_upper < _lower) break

                                    _mid = _lower + (_upper - _lower shr 1 and 1.inv())
                                    if (data[p] < _json_trans_keys[_mid]) {
                                        _upper = _mid - 2
                                    } else if (data[p] > _json_trans_keys[_mid + 1]) {
                                            _lower = _mid + 2
                                    } else {
                                        _trans += _mid - _keys shr 1
                                        break@_match
                                    }
                                }
                                _trans += _klen
                            }
                        } while (false)

                        _trans = _json_indicies[_trans]
                        cs = _json_trans_targs[_trans]
                        if (_json_trans_actions[_trans] != 0) {
                            _acts = _json_trans_actions[_trans]
                            _nacts = _json_actions[_acts++]

                            while (_nacts-- > 0) {
                                when (_json_actions[_acts++]) {
                                    0 -> {
                                        // line 110 "JsonReader.rl"
                                        stringIsName = true
                                    }

                                    1 -> {
                                        // line 113 "JsonReader.rl"
                                        var value = String(data, s, p - s)
                                        if (needsUnescape) value = unescape(value)

                                        outer@
                                        while(true) {
                                            if (stringIsName) {
                                                stringIsName = false
                                                if (debug) println("name: $value")
                                                names.add(value)
                                            } else {
                                                val name = if (names.size > 0) {
                                                    names.removeAt(names.size - 1)
                                                } else {
                                                    null
                                                }

                                                if (stringIsUnquoted) {
                                                    if (value == "true") {
                                                        if (debug) println("boolean: $name=true")
                                                        bool(name, true)
                                                        break@outer
                                                    } else if (value == "false") {
                                                        if (debug) println("boolean: $name=false")
                                                        bool(name, false)
                                                        break@outer
                                                    } else if (value == "null") {
                                                        string(name, null)
                                                        break@outer
                                                    }
                                                    var couldBeDouble = false
                                                    var couldBeLong = true
                                                    var i = s

                                                    outer2@
                                                    while (i < p) {
                                                        when (data[i]) {
                                                            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+' -> {}
                                                            '.', 'e', 'E' -> {
                                                                couldBeDouble = true
                                                                couldBeLong = false
                                                            }
                                                            else -> {
                                                                couldBeDouble = false
                                                                couldBeLong = false
                                                                break@outer2
                                                            }
                                                        }
                                                        i++
                                                    }

                                                    if (couldBeDouble) {
                                                        try {
                                                            if (debug) println("double: " + name + "=" + value.toDouble())
                                                            number(name, value.toDouble(), value)
                                                            break@outer
                                                        } catch (ignored: NumberFormatException) {
                                                        }
                                                    } else if (couldBeLong) {
                                                        if (debug) println("double: " + name + "=" + value.toDouble())
                                                        try {
                                                            number(name, value.toLong(), value)
                                                            break@outer
                                                        } catch (ignored: NumberFormatException) {
                                                        }
                                                    }
                                                }

                                                if (debug) println("string: $name=$value")
                                                string(name, value)
                                            }
                                            stringIsUnquoted = false
                                            s = p

                                            break@outer
                                        }
                                    }

                                    2 -> {
                                        // line 187 "JsonReader.rl"
                                        val name = if (names.size > 0) {
                                            names.removeAt(names.size - 1)
                                        } else {
                                            null
                                        }

                                        if (debug) println("startObject: $name")
                                        startObject(name)

                                        if (top == stack.size) {
                                            val newStack = IntArray(stack.size * 2)
                                            System.arraycopy(stack, 0, newStack, 0, stack.size)
                                            stack = newStack
                                        }

                                        stack[top++] = cs
                                        cs = 5
                                        _goto_targ = 2
                                        continue@_goto
                                    }

                                    3 -> {
                                        // line 193 "JsonReader.rl"
                                        if (debug) println("endObject")
                                        pop()
                                        cs = stack[--top]
                                        _goto_targ = 2
                                        continue@_goto
                                    }

                                    4 -> {
                                        // line 198 "JsonReader.rl"
                                        val name = if (names.size > 0) {
                                            names.removeAt(names.size - 1)
                                        } else {
                                            null
                                        }

                                        if (debug) println("startArray: $name")
                                        startArray(name)

                                        if (top == stack.size) {
                                            val newStack = IntArray(stack.size * 2)
                                            System.arraycopy(stack, 0, newStack, 0, stack.size)
                                            stack = newStack
                                        }

                                        stack[top++] = cs
                                        cs = 23
                                        _goto_targ = 2
                                        continue@_goto
                                    }

                                    5 -> {
                                        // line 204 "JsonReader.rl"
                                        if (debug) println("endArray")
                                        pop()

                                        cs = stack[--top]
                                        _goto_targ = 2
                                        continue@_goto
                                    }

                                    6 -> {
                                        // line 209 "JsonReader.rl"
                                        val start = p - 1
                                        if (data[p++] == '/') {
                                            while (p != length && data[p] != '\n') {
                                                p++
                                            }
                                            p--
                                        } else {
                                            while (p + 1 < length && data[p] != '*' || data[p + 1] != '/') {
                                                p++
                                            }
                                            p++
                                        }
                                        if (debug) println("comment " + String(data, start, p - start))
                                    }

                                    7 -> {
                                        // line 222 "JsonReader.rl"
                                        if (debugChars) println("unquotedChars")
                                        s = p
                                        needsUnescape = false
                                        stringIsUnquoted = true

                                        if (stringIsName) {
                                            outer@while (true) {
                                                when (data[p]) {
                                                    '\\' -> needsUnescape = true
                                                    '/' -> {
                                                        if (p + 1 == length) break
                                                        val c = data[p + 1]
                                                        if (c == '/' || c == '*') break@outer
                                                    }

                                                    ':', '\r', '\n' -> break@outer
                                                }
                                                if (debugChars) println("unquotedChar (name): '" + data[p] + "'")
                                                p++
                                                if (p == length) break
                                            }
                                        } else {
                                            outer@while (true) {
                                                when (data[p]) {
                                                    '\\' -> needsUnescape = true
                                                    '/' -> {
                                                        if (p + 1 == length) break
                                                        val c = data[p + 1]
                                                        if (c == '/' || c == '*') break@outer
                                                    }

                                                    '}', ']', ',', '\r', '\n' -> break@outer
                                                }
                                                if (debugChars) println("unquotedChar (value): '" + data[p] + "'")
                                                p++
                                                if (p == length) break
                                            }
                                        }
                                        p--
                                        while (Character.isWhitespace(data[p])) p--
                                    }

                                    8 -> {
                                        // line 276 "JsonReader.rl"
                                        if (debugChars) println("quotedChars")
                                        s = ++p
                                        needsUnescape = false

                                        outer@
                                        while (true) {
                                            when (data[p]) {
                                                '\\' -> {
                                                    needsUnescape = true
                                                    p++
                                                }

                                                '"' -> break@outer
                                            }
                                            if (debugChars) println("quotedChar: '${data[p]}'")
                                            p++
                                            if (p == length) break
                                        }
                                        p--
                                    }
                                    // line 313 "JsonReader.rl"
                                }
                            }
                        }

                        _goto_targ = 2 // goto the next switch
                        continue@_goto
                    }

                    2 -> {
                        if (cs == 0) {
                            _goto_targ = 5
                            continue@_goto
                        }
                        if (++p != length) {
                            _goto_targ = 1
                            continue@_goto
                        }

                        _goto_targ = 4 // goto the next switch
                        continue@_goto
                    }

                    4 -> {
                        if (p == eof) {
                            var __acts = _json_eof_actions[cs]
                            var __nacts = _json_actions[__acts++]
                            while (__nacts-- > 0) {
                                when (_json_actions[__acts++]) {
                                    1 -> {
                                        // line 113 "JsonReader.rl"
                                        var value = String(data, s, p - s)
                                        if (needsUnescape) value = unescape(value)

                                        outer@while(true) {
                                            if (stringIsName) {
                                                stringIsName = false
                                                if (debug) println("name: $value")
                                                names.add(value)
                                            } else {
                                                val name = if (names.size > 0) {
                                                    names.removeAt(names.size - 1)
                                                } else {
                                                    null
                                                }

                                                if (stringIsUnquoted) {
                                                    if (value == "true") {
                                                        if (debug) println("boolean: $name=true")
                                                        bool(name, true)
                                                        break@outer
                                                    } else if (value == "false") {
                                                        if (debug) println("boolean: $name=false")
                                                        bool(name, false)
                                                        break@outer
                                                    } else if (value == "null") {
                                                        string(name, null)
                                                        break@outer
                                                    }

                                                    var couldBeDouble = false
                                                    var couldBeLong = true
                                                    var i = s
                                                    outer2@while (i < p) {
                                                        when (data[i]) {
                                                            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+' -> {}
                                                            '.', 'e', 'E' -> {
                                                                couldBeDouble = true
                                                                couldBeLong = false
                                                            }

                                                            else -> {
                                                                couldBeDouble = false
                                                                couldBeLong = false
                                                                break@outer2
                                                            }
                                                        }
                                                        i++
                                                    }
                                                    if (couldBeDouble) {
                                                        try {
                                                            if (debug) println("double: $name=${value.toDouble()}")
                                                            number(name, value.toDouble(), value)
                                                            break@outer
                                                        } catch (ignored: NumberFormatException) {
                                                        }
                                                    } else if (couldBeLong) {
                                                        if (debug) println("double: $name=${value.toDouble()}")
                                                        try {
                                                            number(name, value.toLong(), value)
                                                            break@outer
                                                        } catch (ignored: NumberFormatException) {
                                                        }
                                                    }
                                                }
                                                if (debug) println("string: $name=$value")
                                                string(name, value)
                                            }
                                            stringIsUnquoted = false
                                            s = p

                                            break@outer
                                        }
                                    }
                                    // line 411 "JsonReader.rl"
                                }
                            }
                        }

                        _goto_targ = 5 // goto the next switch
                        continue@_goto
                    }

                    5 -> {
                        break@_goto
                    }
                }
            }

            // line 312 "JsonReader.rl"
        } catch (ex: RuntimeException) {
            parseRuntimeEx = ex
        }

        val root = this.root
        this.root = null
        current = null
        lastChild.clear()

        if (p < length) {
            var lineNumber = 1
            for (i in 0 until p) if (data[i] == '\n') lineNumber++
            val start = max(0.0, (p - 32).toDouble()).toInt()

            throw JsonException(
                "Error parsing JSON on line $lineNumber near: " +
                        String(data, start, p - start) + "*ERROR*" +
                        String(data, p, min(64, (pe - p)) ),
                parseRuntimeEx
            )
        }

        if (elements.size != 0) {
            val element = elements[elements.size - 1]
            elements.clear()

            if (element.isObject) {
                throw JsonException("Error parsing JSON, unmatched brace.")
            } else {
                throw JsonException("Error parsing JSON, unmatched bracket.")
            }
        }

        if (parseRuntimeEx != null) throw JsonException("Error parsing JSON: " + String(data), parseRuntimeEx)

        return root!!
    }

    // line 343 "JsonReader.rl"
    private val elements = ArrayList<JsonValue>(8)
    private val lastChild = ArrayList<JsonValue>(8)
    private var root: JsonValue? = null
    private var current: JsonValue? = null

    /**
     * @param name May be null.
     */
    private fun addChild(name: String?, child: JsonValue) {
        child.name = name
        if (current == null) {
            current = child
            root = child
        } else if (current!!.isArray || current!!.isObject) {
            child.parent = current
            if (current!!.size == 0) current!!.child = child else {
                val last = lastChild.removeAt(elements.size - 1)
                last.next = child
                child.prev = last
            }
            lastChild.add(child)
            current!!.size++
        } else root = current
    }

    /**
     * @param name May be null.
     */
    protected fun startObject(name: String?) {
        val value = JsonValue(JsonValue.ValueType.`object`)
        if (current != null) addChild(name, value)
        elements.add(value)
        current = value
    }

    /**
     * @param name May be null.
     */
    protected fun startArray(name: String?) {
        val value = JsonValue(JsonValue.ValueType.array)
        if (current != null) addChild(name, value)
        elements.add(value)
        current = value
    }

    protected fun pop() {
        root = elements.removeAt(elements.size - 1)
        if (current!!.size > 0) lastChild.removeAt(lastChild.size - 1)
        current = if (elements.size > 0) elements[elements.size - 1] else null
    }

    protected fun string(name: String?, value: String?) {
        addChild(name, JsonValue(value))
    }

    protected fun number(name: String?, value: Double, stringValue: String?) {
        addChild(name, JsonValue(value, stringValue))
    }

    protected fun number(name: String?, value: Long, stringValue: String?) {
        addChild(name, JsonValue(value, stringValue))
    }

    protected fun bool(name: String?, value: Boolean) {
        addChild(name, JsonValue(value))
    }

    private fun unescape(value: String): String {
        val length = value.length
        val buffer = StringBuilder(length + 16)
        var i = 0
        while (i < length) {
            var c = value[i++]
            if (c != '\\') {
                buffer.append(c)
                continue
            }
            if (i == length) break
            c = value[i++]
            if (c == 'u') {
                buffer.append(Character.toChars(value.substring(i, i + 4).toInt(16)))
                i += 4
                continue
            }
            when (c) {
                '"', '\\', '/' -> {}
                'b' -> c = '\b'
                'f' -> c = '\u000c'
                'n' -> c = '\n'
                'r' -> c = '\r'
                't' -> c = '\t'
                else -> throw JsonException("Illegal escaped character: \\$c")
            }
            buffer.append(c)
        }
        return buffer.toString()
    }

    companion object {
        // line 421 "JsonReader.rl"
        private val _json_actions = intArrayOf(0, 1, 1, 1, 2, 1, 3, 1, 4, 1, 5, 1, 6, 1, 7, 1, 8, 2, 0, 7, 2, 0, 8, 2, 1, 3, 2, 1, 5)

        private val _json_key_offsets = intArrayOf(
            0,
            0,
            11,
            13,
            14,
            16,
            25,
            31,
            37,
            39,
            50,
            57,
            64,
            73,
            74,
            83,
            85,
            87,
            96,
            98,
            100,
            101,
            103,
            105,
            116,
            123,
            130,
            141,
            142,
            153,
            155,
            157,
            168,
            170,
            172,
            174,
            179,
            184,
            184
        )

        private val _json_trans_keys = charArrayOf(
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            91.toChar(),
            93.toChar(),
            123.toChar(),
            9.toChar(),
            10.toChar(),
            42.toChar(),
            47.toChar(),
            34.toChar(),
            42.toChar(),
            47.toChar(),
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            125.toChar(),
            9.toChar(),
            10.toChar(),
            13.toChar(),
            32.toChar(),
            47.toChar(),
            58.toChar(),
            9.toChar(),
            10.toChar(),
            13.toChar(),
            32.toChar(),
            47.toChar(),
            58.toChar(),
            9.toChar(),
            10.toChar(),
            42.toChar(),
            47.toChar(),
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            91.toChar(),
            93.toChar(),
            123.toChar(),
            9.toChar(),
            10.toChar(),
            9.toChar(),
            10.toChar(),
            13.toChar(),
            32.toChar(),
            44.toChar(),
            47.toChar(),
            125.toChar(),
            9.toChar(),
            10.toChar(),
            13.toChar(),
            32.toChar(),
            44.toChar(),
            47.toChar(),
            125.toChar(),
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            125.toChar(),
            9.toChar(),
            10.toChar(),
            34.toChar(),
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            125.toChar(),
            9.toChar(),
            10.toChar(),
            42.toChar(),
            47.toChar(),
            42.toChar(),
            47.toChar(),
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            125.toChar(),
            9.toChar(),
            10.toChar(),
            42.toChar(),
            47.toChar(),
            42.toChar(),
            47.toChar(),
            34.toChar(),
            42.toChar(),
            47.toChar(),
            42.toChar(),
            47.toChar(),
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            91.toChar(),
            93.toChar(),
            123.toChar(),
            9.toChar(),
            10.toChar(),
            9.toChar(),
            10.toChar(),
            13.toChar(),
            32.toChar(),
            44.toChar(),
            47.toChar(),
            93.toChar(),
            9.toChar(),
            10.toChar(),
            13.toChar(),
            32.toChar(),
            44.toChar(),
            47.toChar(),
            93.toChar(),
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            91.toChar(),
            93.toChar(),
            123.toChar(),
            9.toChar(),
            10.toChar(),
            34.toChar(),
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            91.toChar(),
            93.toChar(),
            123.toChar(),
            9.toChar(),
            10.toChar(),
            42.toChar(),
            47.toChar(),
            42.toChar(),
            47.toChar(),
            13.toChar(),
            32.toChar(),
            34.toChar(),
            44.toChar(),
            47.toChar(),
            58.toChar(),
            91.toChar(),
            93.toChar(),
            123.toChar(),
            9.toChar(),
            10.toChar(),
            42.toChar(),
            47.toChar(),
            42.toChar(),
            47.toChar(),
            42.toChar(),
            47.toChar(),
            13.toChar(),
            32.toChar(),
            47.toChar(),
            9.toChar(),
            10.toChar(),
            13.toChar(),
            32.toChar(),
            47.toChar(),
            9.toChar(),
            10.toChar(),
            0.toChar()
        )

        private val _json_single_lengths = intArrayOf(
            0, 9, 2, 1, 2, 7, 4, 4, 2, 9, 7, 7, 7, 1, 7, 2, 2, 7, 2, 2, 1, 2, 2, 9, 7, 7, 9, 1, 9, 2, 2, 9, 2, 2, 2, 3, 3, 0, 0
        )

        private val _json_range_lengths = intArrayOf(
            0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0
        )

        private val _json_index_offsets = intArrayOf(
            0,
            0,
            11,
            14,
            16,
            19,
            28,
            34,
            40,
            43,
            54,
            62,
            70,
            79,
            81,
            90,
            93,
            96,
            105,
            108,
            111,
            113,
            116,
            119,
            130,
            138,
            146,
            157,
            159,
            170,
            173,
            176,
            187,
            190,
            193,
            196,
            201,
            206,
            207
        )

        private val _json_indicies = intArrayOf(
            1,
            1,
            2,
            3,
            4,
            3,
            5,
            3,
            6,
            1,
            0,
            7,
            7,
            3,
            8,
            3,
            9,
            9,
            3,
            11,
            11,
            12,
            13,
            14,
            3,
            15,
            11,
            10,
            16,
            16,
            17,
            18,
            16,
            3,
            19,
            19,
            20,
            21,
            19,
            3,
            22,
            22,
            3,
            21,
            21,
            24,
            3,
            25,
            3,
            26,
            3,
            27,
            21,
            23,
            28,
            29,
            29,
            28,
            30,
            31,
            32,
            3,
            33,
            34,
            34,
            33,
            13,
            35,
            15,
            3,
            34,
            34,
            12,
            36,
            37,
            3,
            15,
            34,
            10,
            16,
            3,
            36,
            36,
            12,
            3,
            38,
            3,
            3,
            36,
            10,
            39,
            39,
            3,
            40,
            40,
            3,
            13,
            13,
            12,
            3,
            41,
            3,
            15,
            13,
            10,
            42,
            42,
            3,
            43,
            43,
            3,
            28,
            3,
            44,
            44,
            3,
            45,
            45,
            3,
            47,
            47,
            48,
            49,
            50,
            3,
            51,
            52,
            53,
            47,
            46,
            54,
            55,
            55,
            54,
            56,
            57,
            58,
            3,
            59,
            60,
            60,
            59,
            49,
            61,
            52,
            3,
            60,
            60,
            48,
            62,
            63,
            3,
            51,
            52,
            53,
            60,
            46,
            54,
            3,
            62,
            62,
            48,
            3,
            64,
            3,
            51,
            3,
            53,
            62,
            46,
            65,
            65,
            3,
            66,
            66,
            3,
            49,
            49,
            48,
            3,
            67,
            3,
            51,
            52,
            53,
            49,
            46,
            68,
            68,
            3,
            69,
            69,
            3,
            70,
            70,
            3,
            8,
            8,
            71,
            8,
            3,
            72,
            72,
            73,
            72,
            3,
            3,
            3,
            0
        )

        private val _json_trans_targs = intArrayOf(
            35,
            1,
            3,
            0,
            4,
            36,
            36,
            36,
            36,
            1,
            6,
            5,
            13,
            17,
            22,
            37,
            7,
            8,
            9,
            7,
            8,
            9,
            7,
            10,
            20,
            21,
            11,
            11,
            11,
            12,
            17,
            19,
            37,
            11,
            12,
            19,
            14,
            16,
            15,
            14,
            12,
            18,
            17,
            11,
            9,
            5,
            24,
            23,
            27,
            31,
            34,
            25,
            38,
            25,
            25,
            26,
            31,
            33,
            38,
            25,
            26,
            33,
            28,
            30,
            29,
            28,
            26,
            32,
            31,
            25,
            23,
            2,
            36,
            2
        )

        private val _json_trans_actions = intArrayOf(
            13,
            0,
            15,
            0,
            0,
            7,
            3,
            11,
            1,
            11,
            17,
            0,
            20,
            0,
            0,
            5,
            1,
            1,
            1,
            0,
            0,
            0,
            11,
            13,
            15,
            0,
            7,
            3,
            1,
            1,
            1,
            1,
            23,
            0,
            0,
            0,
            0,
            0,
            0,
            11,
            11,
            0,
            11,
            11,
            11,
            11,
            13,
            0,
            15,
            0,
            0,
            7,
            9,
            3,
            1,
            1,
            1,
            1,
            26,
            0,
            0,
            0,
            0,
            0,
            0,
            11,
            11,
            0,
            11,
            11,
            11,
            1,
            0,
            0
        )

        private val _json_eof_actions = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0
        )

        const val json_start = 1
        const val json_first_final = 35
        const val json_error = 0
        const val json_en_object = 5
        const val json_en_array = 23
        const val json_en_main = 1
    }
}
