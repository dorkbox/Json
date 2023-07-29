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

/**
 * Container for a JSON object, array, string, double, long, boolean, or null.
 *
 * JsonValue children are a linked list. Iteration of arrays or objects is easily done using a for loop, either with the enhanced
 * for loop syntactic sugar or like the example below. This is much more efficient than accessing children by index when there are
 * many children.
 *
 * <pre>
 * JsonValue map = ...;
 * for (JsonValue entry = map.child; entry != null; entry = entry.next)
 * System.out.println(entry.name + " = " + entry.asString());
 *
 * @author Nathan Sweet
 */
class JsonValue : Iterable<JsonValue?> {
    private var type: ValueType? = null

    /** May be null.  */
    private var stringValue: String? = null
    private var doubleValue = 0.0
    private var longValue: Long = 0
    var name: String? = null

    /** May be null.  */
    var child: JsonValue? = null
    var parent: JsonValue? = null

    /** May be null. When changing this field the parent [.size] may need to be changed.  */
    var next: JsonValue? = null
    var prev: JsonValue? = null
    var size = 0

    constructor(type: ValueType?) {
        this.type = type
    }

    /**
     * @param value May be null.
     */
    constructor(value: String?) {
        set(value)
    }

    constructor(value: Double) {
        set(value, null)
    }

    constructor(value: Long) {
        set(value, null)
    }

    constructor(value: Double, stringValue: String?) {
        set(value, stringValue)
    }

    constructor(value: Long, stringValue: String?) {
        set(value, stringValue)
    }

    constructor(value: Boolean) {
        set(value)
    }

    /**
     * Returns the child at the specified index. This requires walking the linked list to the specified entry, see
     * [JsonValue] for how to iterate efficiently.
     *
     * @return May be null.
     */
    operator fun get(index: Int): JsonValue? {
        var index = index
        var current = child
        while (current != null && index > 0) {
            index--
            current = current.next
        }
        return current
    }

    /**
     * Returns the child with the specified name.
     *
     * @return May be null.
     */
    operator fun get(name: String): JsonValue? {
        var current = child
        while (current != null && (current.name == null || !current.name.equals(name, ignoreCase = true))) current = current.next
        return current
    }

    /**
     * Returns true if a child with the specified name exists.  */
    fun has(name: String): Boolean {
        return get(name) != null
    }

    /**
     * Returns the child at the specified index. This requires walking the linked list to the specified entry, see
     * [JsonValue] for how to iterate efficiently.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun require(index: Int): JsonValue {
        var index = index
        var current = child
        while (current != null && index > 0) {
            index--
            current = current.next
        }
        requireNotNull(current) { "Child not found with index: $index" }
        return current
    }

    /**
     * Returns the child with the specified name.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun require(name: String): JsonValue {
        var current = child
        while (current != null && (current.name == null || !current.name.equals(name, ignoreCase = true))) current = current.next
        requireNotNull(current) { "Child not found with name: $name" }
        return current
    }

    /**
     * Removes the child with the specified index. This requires walking the linked list to the specified entry, see
     * [JsonValue] for how to iterate efficiently.
     *
     * @return May be null.
     */
    fun remove(index: Int): JsonValue? {
        val child = get(index) ?: return null
        if (child.prev == null) {
            this.child = child.next
            if (this.child != null) this.child!!.prev = null
        } else {
            child.prev!!.next = child.next
            if (child.next != null) child.next!!.prev = child.prev
        }
        size--
        return child
    }

    /**
     * Removes the child with the specified name.
     *
     * @return May be null.
     */
    fun remove(name: String): JsonValue? {
        val child = get(name) ?: return null
        if (child.prev == null) {
            this.child = child.next
            if (this.child != null) this.child!!.prev = null
        } else {
            child.prev!!.next = child.next
            if (child.next != null) child.next!!.prev = child.prev
        }
        size--
        return child
    }

    /** Removes this value from its parent.  */
    fun remove() {
        checkNotNull(parent)
        if (prev == null) {
            parent!!.child = next
            if (parent!!.child != null) parent!!.child!!.prev = null
        } else {
            prev!!.next = next
            if (next != null) next!!.prev = prev
        }
        parent!!.size--
    }

    /** Returns true if there are one or more children in the array or object.  */
    fun notEmpty(): Boolean {
        return size > 0
    }

    /** Returns true if there are not children in the array or object.  */
    val isEmpty: Boolean
        get() = size == 0

    @Deprecated("Use {@link #size} instead. Returns this number of children in the array or object. ")
    fun size(): Int {
        return size
    }

    /**
     * Returns this value as a string.
     *
     * @return May be null if this value is null.
     * @throws IllegalStateException if this an array or object.
     */
    fun asString(): String? {
        return when (type) {
            ValueType.stringValue -> stringValue
            ValueType.doubleValue -> if (stringValue != null) stringValue else doubleValue.toString()
            ValueType.longValue -> if (stringValue != null) stringValue else longValue.toString()
            ValueType.booleanValue -> if (longValue != 0L) "true" else "false"
            ValueType.nullValue -> null
            else -> throw IllegalStateException("Value cannot be converted to string: $type")
        }
    }

    /**
     * Returns this value as a float.
     *
     * @throws IllegalStateException if this an array or object.
     */
    fun asFloat(): Float {
        return when (type) {
            ValueType.stringValue -> stringValue!!.toFloat()
            ValueType.doubleValue -> doubleValue.toFloat()
            ValueType.longValue -> longValue.toFloat()
            ValueType.booleanValue -> if (longValue != 0L) 1.toFloat() else 0.toFloat()
            else -> throw IllegalStateException("Value cannot be converted to float: $type")
        }
    }

    /**
     * Returns this value as a double.
     *
     * @throws IllegalStateException if this an array or object.
     */
    fun asDouble(): Double {
        return when (type) {
            ValueType.stringValue -> stringValue!!.toDouble()
            ValueType.doubleValue -> doubleValue
            ValueType.longValue -> longValue.toDouble()
            ValueType.booleanValue -> if (longValue != 0L) 1.toDouble() else 0.toDouble()
            else -> throw IllegalStateException("Value cannot be converted to double: $type")
        }

    }

    /**
     * Returns this value as a long.
     *
     * @throws IllegalStateException if this an array or object.
     */
    fun asLong(): Long {
        return when (type) {
            ValueType.stringValue -> stringValue!!.toLong()
            ValueType.doubleValue -> doubleValue.toLong()
            ValueType.longValue -> longValue
            ValueType.booleanValue -> if (longValue != 0L) 1L else 0L
            else -> throw IllegalStateException("Value cannot be converted to long: $type")
        }
    }

    /**
     * Returns this value as an int.
     *
     * @throws IllegalStateException if this an array or object.
     */
    fun asInt(): Int {
        return when (type) {
            ValueType.stringValue -> stringValue!!.toInt()
            ValueType.doubleValue -> doubleValue.toInt()
            ValueType.longValue -> longValue.toInt()
            ValueType.booleanValue -> if (longValue != 0L) 1 else 0
            else -> throw IllegalStateException("Value cannot be converted to int: $type")
        }

    }

    /**
     * Returns this value as a boolean.
     *
     * @throws IllegalStateException if this an array or object.
     */
    fun asBoolean(): Boolean {
        return when (type) {
            ValueType.stringValue -> stringValue.equals("true", ignoreCase = true)
            ValueType.doubleValue -> doubleValue != 0.0
            ValueType.longValue -> longValue != 0L
            ValueType.booleanValue -> longValue != 0L
            else -> throw IllegalStateException("Value cannot be converted to boolean: $type")
        }
    }

    /**
     * Returns this value as a byte.
     *
     * @throws IllegalStateException if this an array or object.
     */
    fun asByte(): Byte {
        return when (type) {
            ValueType.stringValue -> stringValue!!.toByte()
            ValueType.doubleValue -> doubleValue.toInt().toByte()
            ValueType.longValue -> longValue.toByte()
            ValueType.booleanValue -> if (longValue != 0L) 1.toByte() else 0.toByte()
            else -> throw IllegalStateException("Value cannot be converted to byte: $type")
        }
    }

    /**
     * Returns this value as a short.
     *
     * @throws IllegalStateException if this an array or object.
     */
    fun asShort(): Short {
        return when (type) {
            ValueType.stringValue -> stringValue!!.toShort()
            ValueType.doubleValue -> doubleValue.toInt().toShort()
            ValueType.longValue -> longValue.toShort()
            ValueType.booleanValue -> if (longValue != 0L) 1.toShort() else 0.toShort()
            else -> throw IllegalStateException("Value cannot be converted to short: $type")
        }

    }

    /**
     * Returns this value as a char.
     *
     * @throws IllegalStateException if this an array or object.
     */
    fun asChar(): Char {
        return when (type) {
            ValueType.stringValue -> if (stringValue!!.length == 0) 0.toChar() else stringValue!![0]
            ValueType.doubleValue -> doubleValue.toInt().toChar()
            ValueType.longValue -> Char(longValue.toUShort())
            ValueType.booleanValue -> if (longValue != 0L) 1.toChar() else 0.toChar()
            else -> throw IllegalStateException("Value cannot be converted to char: $type")
        }
    }

    /**
     * Returns the children of this value as a newly allocated String array.
     *
     * @throws IllegalStateException if this is not an array.
     */
    fun asStringArray(): Array<String?> {
        check(type == ValueType.array) { "Value is not an array: $type" }
        val array = arrayOfNulls<String>(size)
        var i = 0
        var value = child
        while (value != null) {
            var v: String?
            v = when (value.type) {
                ValueType.stringValue -> value.stringValue
                ValueType.doubleValue -> if (stringValue != null) stringValue else value.doubleValue.toString()
                ValueType.longValue -> if (stringValue != null) stringValue else value.longValue.toString()
                ValueType.booleanValue -> if (value.longValue != 0L) "true" else "false"
                ValueType.nullValue -> null
                else -> throw IllegalStateException("Value cannot be converted to string: " + value.type)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /**
     * Returns the children of this value as a newly allocated float array.
     *
     * @throws IllegalStateException if this is not an array.
     */
    fun asFloatArray(): FloatArray {
        check(type == ValueType.array) { "Value is not an array: $type" }
        val array = FloatArray(size)
        var i = 0
        var value = child
        while (value != null) {
            var v: Float
            v = when (value.type) {
                ValueType.stringValue -> value.stringValue!!.toFloat()
                ValueType.doubleValue -> value.doubleValue.toFloat()
                ValueType.longValue -> value.longValue.toFloat()
                ValueType.booleanValue -> (if (value.longValue != 0L) 1 else 0).toFloat()
                else -> throw IllegalStateException("Value cannot be converted to float: " + value.type)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /**
     * Returns the children of this value as a newly allocated double array.
     *
     * @throws IllegalStateException if this is not an array.
     */
    fun asDoubleArray(): DoubleArray {
        check(type == ValueType.array) { "Value is not an array: $type" }
        val array = DoubleArray(size)
        var i = 0
        var value = child
        while (value != null) {
            var v: Double
            v = when (value.type) {
                ValueType.stringValue -> value.stringValue!!.toDouble()
                ValueType.doubleValue -> value.doubleValue
                ValueType.longValue -> value.longValue.toDouble()
                ValueType.booleanValue -> (if (value.longValue != 0L) 1 else 0).toDouble()
                else -> throw IllegalStateException("Value cannot be converted to double: " + value.type)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /**
     * Returns the children of this value as a newly allocated long array.
     *
     * @throws IllegalStateException if this is not an array.
     */
    fun asLongArray(): LongArray {
        check(type == ValueType.array) { "Value is not an array: $type" }
        val array = LongArray(size)
        var i = 0
        var value = child
        while (value != null) {
            var v: Long
            v = when (value.type) {
                ValueType.stringValue -> value.stringValue!!.toLong()
                ValueType.doubleValue -> value.doubleValue.toLong()
                ValueType.longValue -> value.longValue
                ValueType.booleanValue -> (if (value.longValue != 0L) 1 else 0).toLong()
                else -> throw IllegalStateException("Value cannot be converted to long: " + value.type)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /**
     * Returns the children of this value as a newly allocated int array.
     *
     * @throws IllegalStateException if this is not an array.
     */
    fun asIntArray(): IntArray {
        check(type == ValueType.array) { "Value is not an array: $type" }
        val array = IntArray(size)
        var i = 0
        var value = child
        while (value != null) {
            var v: Int
            v = when (value.type) {
                ValueType.stringValue -> value.stringValue!!.toInt()
                ValueType.doubleValue -> value.doubleValue.toInt()
                ValueType.longValue -> value.longValue.toInt()
                ValueType.booleanValue -> if (value.longValue != 0L) 1 else 0
                else -> throw IllegalStateException("Value cannot be converted to int: " + value.type)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /**
     * Returns the children of this value as a newly allocated boolean array.
     *
     * @throws IllegalStateException if this is not an array.
     */
    fun asBooleanArray(): BooleanArray {
        check(type == ValueType.array) { "Value is not an array: $type" }
        val array = BooleanArray(size)
        var i = 0
        var value = child
        while (value != null) {
            var v: Boolean
            v = when (value.type) {
                ValueType.stringValue -> value.stringValue.toBoolean()
                ValueType.doubleValue -> value.doubleValue == 0.0
                ValueType.longValue -> value.longValue == 0L
                ValueType.booleanValue -> value.longValue != 0L
                else -> throw IllegalStateException("Value cannot be converted to boolean: " + value.type)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /**
     * Returns the children of this value as a newly allocated byte array.
     *
     * @throws IllegalStateException if this is not an array.
     */
    fun asByteArray(): ByteArray {
        check(type == ValueType.array) { "Value is not an array: $type" }
        val array = ByteArray(size)
        var i = 0
        var value = child
        while (value != null) {
            var v: Byte
            v = when (value.type) {
                ValueType.stringValue -> value.stringValue!!.toByte()
                ValueType.doubleValue -> value.doubleValue.toInt().toByte()
                ValueType.longValue -> value.longValue.toByte()
                ValueType.booleanValue -> if (value.longValue != 0L) 1.toByte() else 0
                else -> throw IllegalStateException("Value cannot be converted to byte: " + value.type)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /**
     * Returns the children of this value as a newly allocated short array.
     *
     * @throws IllegalStateException if this is not an array.
     */
    fun asShortArray(): ShortArray {
        check(type == ValueType.array) { "Value is not an array: $type" }
        val array = ShortArray(size)
        var i = 0
        var value = child
        while (value != null) {
            var v: Short
            v = when (value.type) {
                ValueType.stringValue -> value.stringValue!!.toShort()
                ValueType.doubleValue -> value.doubleValue.toInt().toShort()
                ValueType.longValue -> value.longValue.toShort()
                ValueType.booleanValue -> if (value.longValue != 0L) 1.toShort() else 0
                else -> throw IllegalStateException("Value cannot be converted to short: " + value.type)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /**
     * Returns the children of this value as a newly allocated char array.
     *
     * @throws IllegalStateException if this is not an array.
     */
    fun asCharArray(): CharArray {
        check(type == ValueType.array) { "Value is not an array: $type" }
        val array = CharArray(size)
        var i = 0
        var value = child

        while (value != null) {
            var v: Char
            v = when (value.type) {
                ValueType.stringValue -> if (value.stringValue!!.length == 0) 0.toChar() else value.stringValue!![0]
                ValueType.doubleValue -> value.doubleValue.toInt().toChar()
                ValueType.longValue -> Char(value.longValue.toUShort())
                ValueType.booleanValue -> if (value.longValue != 0L) 1.toChar() else 0.toChar()
                else -> throw IllegalStateException("Value cannot be converted to char: " + value.type)
            }
            array[i] = v
            value = value.next
            i++
        }

        return array
    }

    /** Returns true if a child with the specified name exists and has a child.  */
    fun hasChild(name: String): Boolean {
        return getChild(name) != null
    }

    /**
     * Finds the child with the specified name and returns its first child.
     *
     * @return May be null.
     */
    fun getChild(name: String): JsonValue? {
        val child = get(name)
        return child?.child
    }

    /**
     * Finds the child with the specified name and returns it as a string. Returns defaultValue if not found.
     *
     * @param defaultValue May be null.
     */
    fun getString(name: String, defaultValue: String?): String? {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asString()
    }

    /**
     * Finds the child with the specified name and returns it as a float. Returns defaultValue if not found.
     */
    fun getFloat(name: String, defaultValue: Float): Float {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asFloat()
    }

    /**
     * Finds the child with the specified name and returns it as a double. Returns defaultValue if not found.
     */
    fun getDouble(name: String, defaultValue: Double): Double {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asDouble()
    }

    /** Finds the child with the specified name and returns it as a long. Returns defaultValue if not found.  */
    fun getLong(name: String, defaultValue: Long): Long {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asLong()
    }

    /** Finds the child with the specified name and returns it as an int. Returns defaultValue if not found.  */
    fun getInt(name: String, defaultValue: Int): Int {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asInt()
    }

    /** Finds the child with the specified name and returns it as a boolean. Returns defaultValue if not found.  */
    fun getBoolean(name: String, defaultValue: Boolean): Boolean {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asBoolean()
    }

    /** Finds the child with the specified name and returns it as a byte. Returns defaultValue if not found.  */
    fun getByte(name: String, defaultValue: Byte): Byte {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asByte()
    }

    /** Finds the child with the specified name and returns it as a short. Returns defaultValue if not found.  */
    fun getShort(name: String, defaultValue: Short): Short {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asShort()
    }

    /** Finds the child with the specified name and returns it as a char. Returns defaultValue if not found.  */
    fun getChar(name: String, defaultValue: Char): Char {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asChar()
    }

    /**
     * Finds the child with the specified name and returns it as a string.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getString(name: String): String? {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asString()
    }

    /**
     * Finds the child with the specified name and returns it as a float.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getFloat(name: String): Float {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asFloat()
    }

    /**
     * Finds the child with the specified name and returns it as a double.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getDouble(name: String): Double {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asDouble()
    }

    /**
     * Finds the child with the specified name and returns it as a long.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getLong(name: String): Long {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asLong()
    }

    /**
     * Finds the child with the specified name and returns it as an int.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getInt(name: String): Int {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asInt()
    }

    /**
     * Finds the child with the specified name and returns it as a boolean.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getBoolean(name: String): Boolean {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asBoolean()
    }

    /**
     * Finds the child with the specified name and returns it as a byte.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getByte(name: String): Byte {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asByte()
    }

    /**
     * Finds the child with the specified name and returns it as a short.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getShort(name: String): Short {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asShort()
    }

    /**
     * Finds the child with the specified name and returns it as a char.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getChar(name: String): Char {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asChar()
    }

    /**
     * Finds the child with the specified index and returns it as a string.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getString(index: Int): String? {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: $name")
        return child.asString()
    }

    /**
     * Finds the child with the specified index and returns it as a float.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getFloat(index: Int): Float {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: $name")
        return child.asFloat()
    }

    /**
     * Finds the child with the specified index and returns it as a double.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getDouble(index: Int): Double {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: $name")
        return child.asDouble()
    }

    /**
     * Finds the child with the specified index and returns it as a long.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getLong(index: Int): Long {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: $name")
        return child.asLong()
    }

    /**
     * Finds the child with the specified index and returns it as an int.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getInt(index: Int): Int {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: $name")
        return child.asInt()
    }

    /**
     * Finds the child with the specified index and returns it as a boolean.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getBoolean(index: Int): Boolean {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: $name")
        return child.asBoolean()
    }

    /**
     * Finds the child with the specified index and returns it as a byte.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getByte(index: Int): Byte {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: $name")
        return child.asByte()
    }

    /**
     * Finds the child with the specified index and returns it as a short.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getShort(index: Int): Short {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: $name")
        return child.asShort()
    }

    /**
     * Finds the child with the specified index and returns it as a char.
     *
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getChar(index: Int): Char {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: $name")
        return child.asChar()
    }

    fun type(): ValueType? {
        return type
    }

    fun setType(type: ValueType?) {
        requireNotNull(type) { "type cannot be null." }
        this.type = type
    }

    val isArray: Boolean
        get() = type == ValueType.array

    val isObject: Boolean
        get() = type == ValueType.`object`

    val isString: Boolean
        get() = type == ValueType.stringValue

    val isNumber: Boolean
        /** Returns true if this is a double or long value.  */
        get() = type == ValueType.doubleValue || type == ValueType.longValue

    val isDouble: Boolean
        get() = type == ValueType.doubleValue

    val isLong: Boolean
        get() = type == ValueType.longValue

    val isBoolean: Boolean
        get() = type == ValueType.booleanValue

    val isNull: Boolean
        get() = type == ValueType.nullValue

    val isValue: Boolean
        /** Returns true if this is not an array or object.  */
        get() {
            return when (type) {
                ValueType.stringValue, ValueType.doubleValue, ValueType.longValue, ValueType.booleanValue, ValueType.nullValue -> true
                else -> false
            }
        }


    /** Sets the name of the specified value and adds it after the last child.  */
    fun addChild(name: String?, value: JsonValue) {
        requireNotNull(name) { "name cannot be null." }
        value.name = name
        addChild(value)
    }

    /** Adds the specified value after the last child.  */
    fun addChild(value: JsonValue) {
        value.parent = this
        size++
        var current = child
        if (current == null) child = value else {
            while (true) {
                if (current!!.next == null) {
                    current.next = value
                    value.prev = current
                    return
                }
                current = current.next
            }
        }
    }

    /**
     * @param value May be null.
     */
    fun set(value: String?) {
        stringValue = value
        type = if (value == null) ValueType.nullValue else ValueType.stringValue
    }

    /**
     * @param stringValue May be null if the string representation is the string value of the double (eg, no leading zeros).
     */
    operator fun set(value: Double, stringValue: String?) {
        doubleValue = value
        longValue = value.toLong()
        this.stringValue = stringValue
        type = ValueType.doubleValue
    }

    /**
     * @param stringValue May be null if the string representation is the string value of the long (eg, no leading zeros).
     */
    operator fun set(value: Long, stringValue: String?) {
        longValue = value
        doubleValue = value.toDouble()
        this.stringValue = stringValue
        type = ValueType.longValue
    }

    fun set(value: Boolean) {
        longValue = (if (value) 1 else 0).toLong()
        type = ValueType.booleanValue
    }

    fun toJson(outputType: OutputType): String? {
        if (isValue) return asString()
        val buffer = StringBuilder(512)
        json(this, buffer, outputType)
        return buffer.toString()
    }

    private fun json(`object`: JsonValue, buffer: StringBuilder, outputType: OutputType) {
        if (`object`.isObject) {
            if (`object`.child == null) buffer.append("{}") else {
                val start = buffer.length
                while (true) {
                    buffer.append('{')
                    val i = 0
                    run {
                        var child = `object`.child
                        while (child != null) {
                            buffer.append(outputType.quoteName(child.name!!))
                            buffer.append(':')
                            json(child, buffer, outputType)
                            if (child.next != null) buffer.append(',')
                            child = child.next
                        }
                    }
                    break
                }
                buffer.append('}')
            }
        } else if (`object`.isArray) {
            if (`object`.child == null) buffer.append("[]") else {
                val start = buffer.length
                while (true) {
                    buffer.append('[')
                    run {
                        var child = `object`.child
                        while (child != null) {
                            json(child!!, buffer, outputType)
                            if (child!!.next != null) buffer.append(',')
                            child = child!!.next
                        }
                    }
                    break
                }
                buffer.append(']')
            }
        } else if (`object`.isString) {
            buffer.append(outputType.quoteValue(`object`.asString()))
        } else if (`object`.isDouble) {
            val doubleValue = `object`.asDouble()
            val longValue = `object`.asLong()
            buffer.append(if (doubleValue == longValue.toDouble()) longValue else doubleValue)
        } else if (`object`.isLong) {
            buffer.append(`object`.asLong())
        } else if (`object`.isBoolean) {
            buffer.append(`object`.asBoolean())
        } else if (`object`.isNull) {
            buffer.append("null")
        } else throw JsonException("Unknown object type: $`object`")
    }

    override fun iterator(): JsonIterator {
        return JsonIterator()
    }

    override fun toString(): String {
        return if (isValue) if (name == null) asString()!! else name + ": " + asString() else (if (name == null) "" else "$name: ") + prettyPrint(
            OutputType.minimal, 0
        )
    }

    /** Returns a human readable string representing the path from the root of the JSON object graph to this value.  */
    fun trace(): String {
        if (parent == null) {
            if (type == ValueType.array) return "[]"
            return if (type == ValueType.`object`) "{}" else ""
        }
        var trace: String
        if (parent!!.type == ValueType.array) {
            trace = "[]"
            var i = 0
            run {
                var child = parent!!.child
                while (child != null) {
                    if (child === this) {
                        trace = "[$i]"
                        break
                    }
                    child = child.next
                    i++
                }
            }
        } else if (name!!.indexOf('.') != -1) trace = ".\"" + name!!.replace("\"", "\\\"") + "\"" else trace = ".$name"
        return parent!!.trace() + trace
    }

    fun prettyPrint(outputType: OutputType?, singleLineColumns: Int): String {
        val settings = PrettyPrintSettings()
        settings.outputType = outputType
        settings.singleLineColumns = singleLineColumns
        return prettyPrint(settings)
    }

    fun prettyPrint(settings: PrettyPrintSettings): String {
        val buffer = StringBuilder(512)
        prettyPrint(this, buffer, 0, settings)
        return buffer.toString()
    }

    private fun prettyPrint(`object`: JsonValue, buffer: StringBuilder, indent: Int, settings: PrettyPrintSettings) {
        val outputType = settings.outputType
        if (`object`.isObject) {
            if (`object`.child == null) buffer.append("{}") else {
                var newLines = !isFlat(`object`)
                val start = buffer.length

                outer@ while (true) {
                    buffer.append(if (newLines) "{\n" else "{ ")
                    val i = 0

                    var child = `object`.child
                    while (child != null) {
                        if (newLines) indent(indent, buffer)

                        buffer.append(outputType!!.quoteName(child.name!!))
                        buffer.append(": ")
                        prettyPrint(child, buffer, indent + 1, settings)
                        if ((!newLines || outputType !== OutputType.minimal) && child.next != null) buffer.append(',')
                        buffer.append(if (newLines) '\n' else ' ')
                        if (!newLines && buffer.length - start > settings.singleLineColumns) {
                            buffer.setLength(start)
                            newLines = true
                            continue@outer
                        }
                        child = child.next
                    }

                    break
                }
                if (newLines) indent(indent - 1, buffer)
                buffer.append('}')
            }
        } else if (`object`.isArray) {
            if (`object`.child == null) buffer.append("[]") else {
                var newLines = !isFlat(`object`)
                val wrap = settings.wrapNumericArrays || !isNumeric(`object`)
                val start = buffer.length
                outer@ while (true) {
                    buffer.append(if (newLines) "[\n" else "[ ")


                    var child = `object`.child
                    while (child != null) {
                        if (newLines) indent(indent, buffer)
                        prettyPrint(child!!, buffer, indent + 1, settings)
                        if ((!newLines || outputType !== OutputType.minimal) && child!!.next != null) buffer.append(',')
                        buffer.append(if (newLines) '\n' else ' ')
                        if (wrap && !newLines && buffer.length - start > settings.singleLineColumns) {
                            buffer.setLength(start)
                            newLines = true
                            continue@outer
                        }
                        child = child!!.next
                    }


                    break
                }
                if (newLines) indent(indent - 1, buffer)
                buffer.append(']')
            }
        } else if (`object`.isString) {
            buffer.append(outputType!!.quoteValue(`object`.asString()))
        } else if (`object`.isDouble) {
            val doubleValue = `object`.asDouble()
            val longValue = `object`.asLong()
            buffer.append(if (doubleValue == longValue.toDouble()) longValue else doubleValue)
        } else if (`object`.isLong) {
            buffer.append(`object`.asLong())
        } else if (`object`.isBoolean) {
            buffer.append(`object`.asBoolean())
        } else if (`object`.isNull) {
            buffer.append("null")
        } else throw JsonException("Unknown object type: $`object`")
    }

    /** More efficient than [.prettyPrint] but [PrettyPrintSettings.singleLineColumns] and
     * [PrettyPrintSettings.wrapNumericArrays] are not supported.  */
    @Throws(IOException::class)
    fun prettyPrint(outputType: OutputType?, writer: Writer) {
        val settings = PrettyPrintSettings()
        settings.outputType = outputType
        prettyPrint(this, writer, 0, settings)
    }

    @Throws(IOException::class)
    private fun prettyPrint(`object`: JsonValue, writer: Writer, indent: Int, settings: PrettyPrintSettings) {
        val outputType = settings.outputType
        if (`object`.isObject) {
            if (`object`.child == null) writer.append("{}") else {
                val newLines = !isFlat(`object`) || `object`.size > 6
                writer.append(if (newLines) "{\n" else "{ ")
                val i = 0
                run {
                    var child = `object`.child
                    while (child != null) {
                        if (newLines) indent(indent, writer)
                        writer.append(outputType!!.quoteName(child.name!!))
                        writer.append(": ")
                        prettyPrint(child, writer, indent + 1, settings)
                        if ((!newLines || outputType !== OutputType.minimal) && child.next != null) writer.append(',')
                        writer.append(if (newLines) '\n' else ' ')
                        child = child.next
                    }
                }
                if (newLines) indent(indent - 1, writer)
                writer.append('}')
            }
        } else if (`object`.isArray) {
            if (`object`.child == null) writer.append("[]") else {
                val newLines = !isFlat(`object`)
                writer.append(if (newLines) "[\n" else "[ ")
                val i = 0
                run {
                    var child = `object`.child
                    while (child != null) {
                        if (newLines) indent(indent, writer)
                        prettyPrint(child!!, writer, indent + 1, settings)
                        if ((!newLines || outputType !== OutputType.minimal) && child!!.next != null) writer.append(',')
                        writer.append(if (newLines) '\n' else ' ')
                        child = child!!.next
                    }
                }
                if (newLines) indent(indent - 1, writer)
                writer.append(']')
            }
        } else if (`object`.isString) {
            writer.append(outputType!!.quoteValue(`object`.asString()))
        } else if (`object`.isDouble) {
            val doubleValue = `object`.asDouble()
            val longValue = `object`.asLong()
            writer.append((if (doubleValue == longValue.toDouble()) longValue else doubleValue).toString())
        } else if (`object`.isLong) {
            writer.append(`object`.asLong().toString())
        } else if (`object`.isBoolean) {
            writer.append(`object`.asBoolean().toString())
        } else if (`object`.isNull) {
            writer.append("null")
        } else throw JsonException("Unknown object type: $`object`")
    }

    inner class JsonIterator : MutableIterator<JsonValue>, Iterable<JsonValue?> {
        var entry = child
        var current: JsonValue? = null
        override fun hasNext(): Boolean {
            return entry != null
        }

        override fun next(): JsonValue {
            val current = entry
            this.current = current
            if (current == null) throw NoSuchElementException()
            entry = current.next
            return current
        }

        override fun remove() {
            if (current!!.prev == null) {
                child = current!!.next
                if (child != null) child!!.prev = null
            } else {
                current!!.prev!!.next = current!!.next
                if (current!!.next != null) current!!.next!!.prev = current!!.prev
            }
            size--
        }

        override fun iterator(): MutableIterator<JsonValue> {
            return this
        }
    }

    enum class ValueType {
        `object`, array, stringValue, doubleValue, longValue, booleanValue, nullValue
    }

    class PrettyPrintSettings {
        var outputType: OutputType? = null

        /** If an object on a single line fits this many columns, it won't wrap.  */
        var singleLineColumns = 0

        /** Arrays of floats won't wrap.  */
        var wrapNumericArrays = false
    }

    companion object {
        private fun isFlat(`object`: JsonValue): Boolean {
            run {
                var child = `object`.child
                while (child != null) {
                    if (child.isObject || child.isArray) return false
                    child = child.next
                }
            }
            return true
        }

        private fun isNumeric(`object`: JsonValue): Boolean {
            run {
                var child = `object`.child
                while (child != null) {
                    if (!child.isNumber) return false
                    child = child.next
                }
            }
            return true
        }

        private fun indent(count: Int, buffer: StringBuilder) {
            for (i in 0 until count) buffer.append('\t')
        }

        @Throws(IOException::class)
        private fun indent(count: Int, buffer: Writer) {
            for (i in 0 until count) buffer.append('\t')
        }
    }
}
