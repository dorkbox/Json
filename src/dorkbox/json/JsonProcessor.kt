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
 * Copyright 2011 LibGDX.
 * Mario Zechner <badlogicgames></badlogicgames>@gmail.com>
 * Nathan Sweet <nathan.sweet></nathan.sweet>@gmail.com>
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
@file:Suppress("DuplicatedCode")

package dorkbox.json

import dorkbox.collections.ObjectMap
import dorkbox.collections.OrderedMap
import java.io.*
import java.lang.Class
import java.lang.ClassNotFoundException
import java.lang.Deprecated
import java.lang.IllegalAccessException
import java.lang.SecurityException
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.security.AccessControlException
import java.util.*
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Byte
import kotlin.Char
import kotlin.CharArray
import kotlin.CharSequence
import kotlin.Double
import kotlin.Enum
import kotlin.Exception
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.NumberFormatException
import kotlin.RuntimeException
import kotlin.Short
import kotlin.String
import kotlin.Suppress
import kotlin.arrayOf
import kotlin.arrayOfNulls
import kotlin.let
import kotlin.toString

/**
 * Reads/writes Java objects to/from JSON, automatically. See the wiki for usage:
 * https://github.com/libgdx/libgdx/wiki/Reading-and-writing-JSON
 *
 * @author Nathan Sweet
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
open class JsonProcessor {
    private var writer: JsonWriter? = null

    /**
     * Sets the name of the JSON field to store the Java class name or class tag when required to avoid ambiguity during
     * deserialization. Set to null to never output this information, but be warned that deserialization may fail.
     *
     * Default is "class".
     */
    var typeName: String? = "class"

    /**
     * When true, field values that are identical to a newly constructed instance are not written.
     *
     * Default is true.
     */
    var usePrototypes = true

    /**
     * Sets the type of JSON output.
     *
     * Default is [OutputType.minimal].
     *
     * @see JsonWriter.setOutputType
     */
    var outputType = OutputType.minimal

    /**
     * When true, quotes long, double, BigInteger, BigDecimal types to prevent truncation in languages like JavaScript and PHP.
     * This is not necessary when using libgdx, which handles these types without truncation.
     *
     * Default is false.
     */
    var quoteLongValues = false

    /**
     * When true, fields in the JSON that are not found on the class will not throw a [JsonException].
     *
     * Default is false.
     */
    var ignoreUnknownFields = false

    /**
     * When true, fields with the [Deprecated] annotation will not be read or written.
     *
     * Default is false.
     */
    var ignoreDeprecated = false

    /**
     * When true, fields with the [Deprecated] annotation will be read (but not written) when [ignoreDeprecated] is true.
     *
     * Default is false.
     */
    var readDeprecated = false

    /**
     * When true, [Enum.name] is used to write enum values. When false, [Enum.toString] is used which may not be unique.
     *
     * Default is true.
     */
    var enumNames = true

    /**
     * When true, fields are sorted alphabetically when written, otherwise the source code order is used.
     *
     * Default is false.
     */
    var sortFields = false

    /**
     * Sets the serializer to use when the type being deserialized is not known (null).
     */
    var defaultSerializer: JsonSerializer<Any>? = null

    /**
     * When true, will write the default fields to the json output.
     *
     * Default is true
     */
    var writeDefaultFields = true

    private val typeToFields = ObjectMap<Class<*>, OrderedMap<String, FieldMetadata>>()
    private val tagToClass = ObjectMap<String, Class<*>?>()
    private val classToTag = ObjectMap<Class<*>, String>()
    private val classToSerializer = ObjectMap<Class<*>, JsonSerializer<Any>?>()
    private val classToDefaultValues = ObjectMap<Class<*>, Array<Any?>>()

    private val equals1 = arrayOf<Any?>(null)
    private val equals2 = arrayOf<Any?>(null)

    constructor()

    constructor(outputType: OutputType) {
        this.outputType = outputType
    }

    /** Sets a tag to use instead of the fully qualifier class name. This can make the JSON easier to read.  */
    fun addClassTag(tag: String, type: Class<Any>) {
        tagToClass.put(tag, type)
        classToTag.put(type, tag)
    }

    /** Returns the class for the specified tag, or null.  */
    fun getClass(tag: String): Class<*>? {
        return tagToClass.get(tag)
    }

    /** Returns the tag for the specified class, or null.  */
    fun getTag(type: Class<*>): String? {
        return classToTag.get(type)
    }

    /**
     * Registers a serializer to use for the specified type instead of the default behavior of serializing all of an objects
     * fields.
     */
    fun <T: Any> setSerializer(type: Class<T>, serializer: JsonSerializer<T>) {
        @Suppress("UNCHECKED_CAST")
        classToSerializer.put(type, serializer as JsonSerializer<Any>)
    }

    fun <T> getSerializer(type: Class<T>): JsonSerializer<T> {
        @Suppress("UNCHECKED_CAST")
        return classToSerializer.get(type) as JsonSerializer<T>
    }

    /**
     * Sets the type of elements in a collection. When the element type is known, the class for each element in the collection
     * does not need to be written unless different from the element type.
     */
    fun setElementType(type: Class<*>, fieldName: String, elementType: Class<*>?) {
        val metadata = getFields(type).get(fieldName) ?: throw JsonException("Field not found: " + fieldName + " (" + type.getName() + ")")
        metadata.elementType = elementType
    }

    /**
     * The specified field will be treated as if it has or does not have the [Deprecated] annotation.
     *
     * @see .setIgnoreDeprecated
     * @see .setReadDeprecated
     */
    fun setDeprecated(type: Class<*>, fieldName: String, deprecated: Boolean) {
        val metadata = getFields(type).get(fieldName) ?: throw JsonException("Field not found: " + fieldName + " (" + type.getName() + ")")
        metadata.deprecated = deprecated
    }

    private fun getFields(type: Class<*>): OrderedMap<String, FieldMetadata> {
        val fields = typeToFields[type]
        if (fields != null) return fields
        val classHierarchy = ArrayList<Class<*>>()

        var nextClass = type
        while (nextClass != Any::class.java) {
            classHierarchy.add(nextClass)
            nextClass = nextClass.superclass
        }

        val allFields = ArrayList<Field>()
        for (i in classHierarchy.indices.reversed()) {
            Collections.addAll(allFields, *classHierarchy[i].getDeclaredFields())
        }

        val nameToField = OrderedMap<String, FieldMetadata>(allFields.size)
        var i = 0
        val n = allFields.size
        while (i < n) {
            val field = allFields[i]
            val modifiers = field.modifiers
            if (Modifier.isTransient(modifiers)) {
                i++
                continue
            }
            if (Modifier.isStatic(modifiers)) {
                i++
                continue
            }
            if (field.isSynthetic) {
                i++
                continue
            }
            if (!field.isAccessible) {
                try {
                    field.setAccessible(true)
                } catch (ex: AccessControlException) {
                    i++
                    continue
                }
            }
            val fieldJsonName = field.annotations.filterIsInstance<Json>().firstOrNull()?.alias ?: field.name

            nameToField.put(fieldJsonName, FieldMetadata(field))
            i++
        }

        if (sortFields) nameToField.sort()
        typeToFields.put(type, nameToField)
        return nameToField
    }

    /**
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun toJson(
        `object`: Any,
        knownType: Class<*>? = `object`.javaClass,
        elementType: Class<*>? = null
    ): String {
        val buffer = StringWriter()
        toJson(`object`, knownType, elementType, buffer)
        return buffer.toString()
    }

    fun toJson(`object`: Any, file: File) {
        toJson(`object`, `object`.javaClass, null, file)
    }

    /**
     * @param knownType May be null if the type is unknown.
     */
    fun toJson(`object`: Any, knownType: Class<*>?, file: File) {
        toJson(`object`, knownType, null, file)
    }

    /**
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun toJson(`object`: Any, knownType: Class<*>?, elementType: Class<*>?, file: File) {
        var writer: Writer? = null
        try {
            writer = OutputStreamWriter(FileOutputStream(file), "UTF-8")
            toJson(`object`, knownType, elementType, writer)
        } catch (ex: Exception) {
            throw JsonException("Error writing file: $file", ex)
        } finally {
            try {
                writer?.close()
            } catch (ignored: IOException) {
            }
        }
    }

    fun toJson(`object`: Any, writer: Writer) {
        toJson(`object`, `object`.javaClass, null, writer)
    }

    /**
     * @param knownType May be null if the type is unknown.
     */
    fun toJson(`object`: Any, knownType: Class<*>?, writer: Writer) {
        toJson(`object`, knownType, null, writer)
    }

    /**
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun toJson(`object`: Any, knownType: Class<*>?, elementType: Class<*>?, writer: Writer) {
        setWriter(writer)

        try {
            writeValue(`object`, knownType, elementType)
        } finally {
            try {
                this.writer?.close()
            } catch (ignored: IOException) {
            }

            this.writer = null
        }
    }

    /** Sets the writer where JSON output will be written. This is only necessary when not using the toJson methods.  */
    fun setWriter(writer: Writer) {
        @Suppress("NAME_SHADOWING")
        var writer = writer
        if (writer !is JsonWriter) {
            writer = JsonWriter(writer)
        }

        writer.setOutputType(outputType)
        writer.setQuoteLongValues(quoteLongValues)
        this.writer = writer
    }

    fun getWriter(): JsonWriter? {
        return writer
    }

    /** Writes all fields of the specified object to the current JSON object.  */
    fun writeFields(`object`: Any) {
        val type: Class<*> = `object`.javaClass
        val defaultValues = getDefaultValues(type)
        val fields = getFields(type)
        var defaultIndex = 0

        val fieldNames = fields.orderedKeys()
        fieldNames.forEach { fieldName ->
            val metadata = fields[fieldName]
            if (ignoreDeprecated && metadata!!.deprecated) {
                return
            }

            val field = metadata!!.field
            try {
                val value = field[`object`]
                if (!writeDefaultFields && defaultValues != null) {
                    val defaultValue = defaultValues[defaultIndex++]
                    if (value == null && defaultValue == null) {
                        return
                    }

                    if (value != null && defaultValue != null) {
                        if (value == defaultValue) {
                            return
                        }
                        if (value.javaClass.isArray && defaultValue.javaClass.isArray) {
                            equals1[0] = value
                            equals2[0] = defaultValue
                            if (equals1.contentDeepEquals(equals2)) {
                                return
                            }
                        }
                    }
                }

                val fieldJsonName = field.annotations.filterIsInstance<Json>().firstOrNull()?.alias ?: field.name

                if (debug) println("Writing field: ${field.name} (json=$fieldJsonName) (${type.getName()})")
                writer!!.name(fieldJsonName)
                writeValue(value, field.type, metadata.elementType)

            } catch (ex: IllegalAccessException) {
                throw JsonException("Error accessing field: ${field.name} (${type.getName()})", ex)
            } catch (ex: JsonException) {
                ex.addTrace("$field (${type.getName()})")
                throw ex
            } catch (runtimeEx: Exception) {
                val ex = JsonException(runtimeEx)
                ex.addTrace("$field (${type.getName()})")
                throw ex
            }
        }
    }

    private fun getDefaultValues(type: Class<*>): Array<Any?>? {
        if (!usePrototypes) {
            return null
        }

        if (classToDefaultValues.containsKey(type)) {
            return classToDefaultValues[type]
        }

        val `object` = try {
            newInstance(type)
        } catch (ex: Exception) {
            classToDefaultValues[type] = arrayOfNulls(0)
            return null
        }

        val fields = getFields(type)
        val values = arrayOfNulls<Any>(fields.size)
        classToDefaultValues[type] = values

        var defaultIndex = 0
        val fieldNames = fields.orderedKeys()
        var i = 0
        val n = fieldNames.size
        while (i < n) {
            val metadata = fields.get(fieldNames[i])
            if (ignoreDeprecated && metadata!!.deprecated) {
                i++
                continue
            }

            val field = metadata!!.field
            try {
                values[defaultIndex++] = field[`object`]
            } catch (ex: IllegalAccessException) {
                throw JsonException("""Error accessing field: ${field.name} (${type.getName()})""", ex)
            } catch (ex: JsonException) {
                ex.addTrace("$field (${type.getName()})")
                throw ex
            } catch (runtimeEx: RuntimeException) {
                val ex = JsonException(runtimeEx)
                ex.addTrace("$field (${type.getName()})")
                throw ex
            }
            i++
        }

        return values
    }

    /**
     *  @see .writeField
     */
    fun writeField(`object`: Any, name: String) {
        writeField(`object`, name, name, null)
    }

    /**
     * @param elementType May be null if the type is unknown.
     *
     * @see .writeField
     */
    fun writeField(`object`: Any, name: String, elementType: Class<*>?) {
        writeField(`object`, name, name, elementType)
    }

    /**
     * Writes the specified field to the current JSON object.
     *
     * @param elementType May be null if the type is unknown.
     *
     * @see .writeField
     */
    fun writeField(`object`: Any, fieldName: String, jsonName: String, elementType: Class<*>? = null) {
        @Suppress("NAME_SHADOWING")
        var elementType = elementType
        val type: Class<*> = `object`.javaClass

        val metadata = getFields(type).get(fieldName) ?:
            throw JsonException("""Field not found: $fieldName (${type.getName()})""")

        val field = metadata.field
        if (elementType == null) {
            elementType = metadata.elementType
        }

        try {
            if (debug) println("Writing field: " + field.name + " (" + type.getName() + ")")
            writer!!.name(jsonName)
            writeValue(field[`object`], field.type, elementType)
        } catch (ex: IllegalAccessException) {
            throw JsonException("Error accessing field: " + field.name + " (" + type.getName() + ")", ex)
        } catch (ex: JsonException) {
            ex.addTrace(field.toString() + " (" + type.getName() + ")")
            throw ex
        } catch (runtimeEx: Exception) {
            val ex = JsonException(runtimeEx)
            ex.addTrace(field.toString() + " (" + type.getName() + ")")
            throw ex
        }
    }

    /**
     * Writes the value as a field on the current JSON object, without writing the actual class.
     *
     * @param value May be null.
     *
     * @see .writeValue
     */
    fun writeValue(name: String, value: Any?) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw JsonException(ex)
        }

        if (value == null) {
            writeValue(value as Any?, null, null)
        } else {
            writeValue(value, value.javaClass, null)
        }
    }

    /**
     * Writes the value as a field on the current JSON object, writing the class of the object if it differs from the specified
     * known type.
     *
     * @param value May be null.
     * @param knownType May be null if the type is unknown.
     *
     * @see .writeValue
     */
    fun writeValue(name: String, value: Any?, knownType: Class<*>?) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw JsonException(ex)
        }

        writeValue(value, knownType, null)
    }

    /**
     * Writes the value as a field on the current JSON object, writing the class of the object if it differs from the specified
     * known type. The specified element type is used as the default type for collections.
     *
     * @param value May be null.
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun writeValue(name: String, value: Any?, knownType: Class<*>?, elementType: Class<*>?) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw JsonException(ex)
        }

        writeValue(value, knownType, elementType)
    }

    /**
     * Writes the value, without writing the class of the object.
     *
     * @param value May be null.
     */
    fun writeValue(value: Any?) {
        if (value == null) {
            writeValue(value as Any?, null, null)
        } else {
            writeValue(value, value.javaClass, null)
        }
    }


    /**
     * Writes the value, writing the class of the object if it differs from the specified known type. The specified element type
     * is used as the default type for collections.
     *
     * @param value May be null.
     * @param knownType May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun writeValue(value: Any?, knownType: Class<*>?, elementType: Class<*>? = null) {
        @Suppress("NAME_SHADOWING")
        var knownType = knownType

        @Suppress("NAME_SHADOWING")
        var elementType = elementType

        try {
            if (value == null) {
                writer!!.value(null)
                return
            }

            if (knownType != null && knownType.isPrimitive ||
                isString(knownType) ||
                isInt(knownType) ||
                isBoolean(knownType) ||
                isFloat(knownType) ||
                isLong(knownType) ||
                isDouble(knownType) ||
                isShort(knownType) ||
                isByte(knownType) ||
                isChar(knownType)) {

                writer!!.value(value)
                return
            }

            var actualType: Class<*> = value.javaClass
            if (actualType.isPrimitive ||
                isString(actualType) ||
                isInt(knownType) ||
                isBoolean(knownType) ||
                isFloat(actualType) ||
                isLong(actualType) ||
                isDouble(actualType) ||
                isShort(actualType) ||
                isByte(actualType) ||
                isChar(actualType)) {

                writeObjectStart(actualType, null)
                writeValue("value", value)
                writeObjectEnd()
                return
            }

            if (value is JsonSerializable) {

                writeObjectStart(actualType, knownType)
                value.write(this)
                writeObjectEnd()
                return
            }

            val serializer = classToSerializer.get(actualType)
            if (serializer != null) {
                @Suppress("UNCHECKED_CAST")
                serializer.write(this, value, knownType as Class<Any>)
                return
            }

            // JSON array special cases.
            if (value is ArrayList<*>) {
                if (knownType != null && actualType != knownType && actualType != ArrayList::class.java) {
                    throw JsonException(
                        """
    Serialization of an Array other than the known type is not supported.
    Known type: $knownType
    Actual type: $actualType
    """.trimIndent()
                    )
                }

                writeArrayStart()
                val array = value
                var i = 0
                val n = array.size
                while (i < n) {
                    writeValue(array[i], elementType, null)
                    i++
                }
                writeArrayEnd()
                return
            }

            if (value is Collection<*>) {
                if (typeName != null &&
                    actualType != ArrayList::class.java &&
                    (knownType == null || knownType != actualType)) {

                    writeObjectStart(actualType, knownType)
                    writeArrayStart("items")
                    for (item in value) {
                        writeValue(item, elementType, null)
                    }
                    writeArrayEnd()
                    writeObjectEnd()
                } else {
                    writeArrayStart()
                    for (item in value) {
                        writeValue(item, elementType, null)
                    }
                    writeArrayEnd()
                }
                return
            }

            if (actualType.isArray) {
                if (elementType == null) {
                    elementType = actualType.componentType
                }

                val length = java.lang.reflect.Array.getLength(value)
                writeArrayStart()
                for (i in 0 until length) {
                    writeValue(java.lang.reflect.Array.get(value, i), elementType, null)
                }
                writeArrayEnd()
                return
            }

            // JSON object special cases.
            if (value is ObjectMap<*, *>) {
                if (knownType == null) {
                    knownType = ObjectMap::class.java
                }

                writeObjectStart(actualType, knownType)
                for (entry in value.entries()) {
                    writer!!.name(convertToString(entry.key))
                    writeValue(entry.value, elementType, null)
                }
                writeObjectEnd()
                return
            }

            if (value is Map<*, *>) {
                if (knownType == null) {
                    knownType = HashMap::class.java
                }
                writeObjectStart(actualType, knownType)
                for ((key, value1) in value) {
                    writer!!.name(convertToString(key!!))
                    writeValue(value1, elementType, null)
                }
                writeObjectEnd()
                return
            }

            // Enum special case.
            if (Enum::class.java.isAssignableFrom(actualType)) {
                if (typeName != null && (knownType == null || knownType != actualType)) {
                    // Ensures that enums with specific implementations (abstract logic) serialize correctly.
                    if (actualType.getEnumConstants() == null) {
                        actualType = actualType.superclass
                    }

                    writeObjectStart(actualType, null)
                    writer!!.name("value")
                    writer!!.value(convertToString(value as Enum<*>))
                    writeObjectEnd()
                } else {
                    writer!!.value(convertToString(value as Enum<*>))
                }
                return
            }


            writeObjectStart(actualType, knownType)
            writeFields(value)
            writeObjectEnd()
        } catch (ex: IOException) {
            throw JsonException(ex)
        }
    }

    fun writeObjectStart(name: String) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw JsonException(ex)
        }
        writeObjectStart()
    }

    /**
     * @param knownType May be null if the type is unknown.
     */
    fun writeObjectStart(name: String, actualType: Class<*>, knownType: Class<*>?) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw JsonException(ex)
        }
        writeObjectStart(actualType, knownType)
    }

    fun writeObjectStart() {
        try {
            writer!!.`object`()
        } catch (ex: IOException) {
            throw JsonException(ex)
        }
    }

    /**
     * Starts writing an object, writing the actualType to a field if needed.
     *
     * @param knownType May be null if the type is unknown.
     */
    fun writeObjectStart(actualType: Class<*>, knownType: Class<*>?) {
        try {
            writer!!.`object`()
        } catch (ex: IOException) {
            throw JsonException(ex)
        }
        if (knownType == null || knownType != actualType) writeType(actualType)
    }

    fun writeObjectEnd() {
        try {
            writer!!.pop()
        } catch (ex: IOException) {
            throw JsonException(ex)
        }
    }

    fun writeArrayStart(name: String) {
        try {
            writer!!.name(name)
            writer!!.array()
        } catch (ex: IOException) {
            throw JsonException(ex)
        }
    }

    fun writeArrayStart() {
        try {
            writer!!.array()
        } catch (ex: IOException) {
            throw JsonException(ex)
        }
    }

    fun writeArrayEnd() {
        try {
            writer!!.pop()
        } catch (ex: IOException) {
            throw JsonException(ex)
        }
    }

    fun writeType(type: Class<*>) {
        if (typeName == null) return

        var className = getTag(type)
        if (className == null) {
            className = type.getName()
        }

        try {
            writer!![typeName!!] = className!!
        } catch (ex: IOException) {
            throw JsonException(ex)
        }

        if (debug) println("Writing type: " + type.getName())
    }

    /**
     * @param type May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, reader: Reader): T? {
        return readValue(type, null, JsonReader().parse(reader))
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, elementType: Class<*>?, reader: Reader): T? {
        return readValue(type, elementType, JsonReader().parse(reader))
    }

    /**
     * @param type May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, input: InputStream): T? {
        return readValue(type, null, JsonReader().parse(input))
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, elementType: Class<*>?, input: InputStream): T? {
        return readValue(type, elementType, JsonReader().parse(input))
    }

    /**
     * @param type May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, file: File): T? {
        return try {
            readValue(type, null, JsonReader().parse(file))
        } catch (ex: Exception) {
            throw JsonException("Error reading file: $file", ex)
        }
    }

    /** @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, elementType: Class<*>?, file: File): T? {
        return try {
            readValue(type, elementType, JsonReader().parse(file))
        } catch (ex: Exception) {
            throw JsonException("Error reading file: $file", ex)
        }
    }

    /** @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, data: CharArray, offset: Int, length: Int): T? {
        return readValue(type, null, JsonReader().parse(data, offset, length))
    }

    /** @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, elementType: Class<*>?, data: CharArray, offset: Int, length: Int): T? {
        return readValue(type, elementType, JsonReader().parse(data, offset, length))
    }

    /** @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, json: String): T? {
        return readValue(type, null, JsonReader().parse(json))
    }

    /** @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: Class<T>?, elementType: Class<*>?, json: String): T? {
        return readValue(type, elementType, JsonReader().parse(json))
    }

    fun readField(`object`: Any, name: String, jsonData: JsonValue) {
        readField(`object`, name, name, null, jsonData)
    }

    fun readField(`object`: Any, name: String, elementType: Class<*>?, jsonData: JsonValue) {
        readField(`object`, name, name, elementType, jsonData)
    }

    fun readField(`object`: Any, fieldName: String, jsonName: String, jsonData: JsonValue) {
        readField(`object`, fieldName, jsonName, null, jsonData)
    }

    /** @param elementType May be null if the type is unknown.
     */
    fun readField(`object`: Any, fieldName: String, jsonName: String, elementType: Class<*>?, jsonMap: JsonValue) {
        @Suppress("NAME_SHADOWING")
        var elementType = elementType
        val type: Class<*> = `object`.javaClass

        val metadata = getFields(type).get(fieldName) ?:
            throw JsonException("Field not found: $fieldName (${type.getName()})")

        val field = metadata.field
        if (elementType == null) {
            elementType = metadata.elementType
        }

        readField(`object`, field, jsonName, elementType, jsonMap)
    }

    /**
     * @param object May be null if the field is static.
     * @param elementType May be null if the type is unknown.
     */
    fun readField(`object`: Any?, field: Field, jsonName: String, elementType: Class<*>?, jsonMap: JsonValue) {
        val jsonValue = jsonMap[jsonName] ?: return

        try {
            field[`object`] = readValue(field.type, elementType, jsonValue)
        } catch (ex: IllegalAccessException) {
            throw JsonException(
                "Error accessing field: ${field.name} (${field.declaringClass.getName()})", ex
            )
        } catch (ex: JsonException) {
            ex.addTrace("${field.name} (${field.declaringClass.getName()})")
            throw ex
        } catch (runtimeEx: RuntimeException) {
            val ex = JsonException(runtimeEx)
            ex.addTrace(jsonValue.trace())
            ex.addTrace("${field.name} (${field.declaringClass.getName()})")
            throw ex
        }
    }

    fun readFields(`object`: Any, jsonMap: JsonValue) {
        val type: Class<*> = `object`.javaClass
        val fields = getFields(type)
        var child = jsonMap.child

        while (child != null) {
            val metadata = fields.get(child.name!!.replace(" ", "_"))
            if (metadata == null) {
                if (child.name == typeName) {
                    child = child.next
                    continue
                }

                if (ignoreUnknownFields || ignoreUnknownField(type, child.name!!)) {
                    if (debug) println("Ignoring unknown field: ${child.name} (${type.getName()})")
                    child = child.next
                    continue
                } else {
                    val ex = JsonException("Field not found: ${child.name} (${type.getName()})")
                    ex.addTrace(child.trace())
                    throw ex
                }
            } else {
                if (ignoreDeprecated && !readDeprecated && metadata.deprecated) {
                    child = child.next
                    continue
                }
            }

            val field = metadata.field
            try {
                field[`object`] = readValue(field.type, metadata.elementType, child)
            } catch (ex: IllegalAccessException) {
                throw JsonException("Error accessing field: ${field.name} (${type.getName()})", ex)
            } catch (ex: JsonException) {
                ex.addTrace("${field.name} (${type.getName()})")
                throw ex
            } catch (runtimeEx: RuntimeException) {
                val ex = JsonException(runtimeEx)
                ex.addTrace(child.trace())
                ex.addTrace("${field.name} (${type.getName()})")
                throw ex
            }

            child = child.next
        }
    }

    /**
     * Called for each unknown field name encountered by [.readFields] when [.ignoreUnknownFields]
     * is false to determine whether the unknown field name should be ignored.
     *
     * @param type The object type being read.
     * @param fieldName A field name encountered in the JSON for which there is no matching class field.
     *
     * @return true if the field name should be ignored and an exception won't be thrown by
     * [.readFields].
     */
    @Suppress("UNUSED_PARAMETER")
    protected fun ignoreUnknownField(type: Class<*>, fieldName: String): Boolean {
        return false
    }

    /**
     * @param type May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> readValue(name: String, type: Class<T>?, jsonMap: JsonValue): T? {
        return readValue(type, null, jsonMap[name])
    }

    /**
     * @param type May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> readValue(name: String, type: Class<T>?, defaultValue: T, jsonMap: JsonValue): T? {
        val jsonValue = jsonMap[name] ?: return defaultValue
        return readValue(type, null, jsonValue)
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> readValue(name: String, type: Class<T>, elementType: Class<*>?, jsonMap: JsonValue): T? {
        return readValue(type, elementType, jsonMap[name])
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> readValue(name: String, type: Class<T>?, elementType: Class<*>?, defaultValue: T, jsonMap: JsonValue): T? {
        val jsonValue = jsonMap[name]
        return readValue(type, elementType, defaultValue, jsonValue)
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> readValue(type: Class<T>?, elementType: Class<*>?, defaultValue: T, jsonData: JsonValue?): T? {
        return jsonData?.let { readValue(type, elementType, it) } ?: defaultValue
    }

    /**
     * @param type May be null if the type is unknown.
     *
     * @return May be null.
     */
    fun <T> readValue(type: Class<T>?, jsonData: JsonValue): T? {
        return readValue(type, null, jsonData)
    }

    /**
     * @param type May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     *
     * @return May be null.
     */
    @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
    fun <T> readValue(type: Class<T>?, elementType: Class<*>?, jsonData: JsonValue?): T? {
        var type = type
        var elementType = elementType
        var jsonData = jsonData ?: return null

        if (jsonData.isObject) {
            val className = if (typeName == null) {
                null
            } else {
                jsonData.getString(typeName!!, null)
            }

            if (className != null) {
                type = getClass(className) as Class<T>?

                if (type == null) {
                    type = try {
                        Class.forName(className) as Class<T>
                    } catch (ex: ClassNotFoundException) {
                        throw JsonException(ex)
                    }
                }
            }

            if (type == null) {
                return if (defaultSerializer != null) {
                    defaultSerializer!!.read(this, jsonData, type) as T
                } else {
                    jsonData as T
                }
            }

            if (typeName != null && MutableCollection::class.java.isAssignableFrom(type)) {
                // JSON object wrapper to specify type.
                val data = jsonData["items"] ?:
                    throw JsonException("Unable to convert object to collection: $jsonData (${type.getName()})")

                jsonData = data
            } else {
                val serializer = classToSerializer.get(type)
                if (serializer != null) {
                    return serializer.read(this, jsonData, type as Class<Any>) as T
                }

                if (isString(type) ||
                    isInt(type) ||
                    isBoolean(type) ||
                    isFloat(type) ||
                    isLong(type) ||
                    isDouble(type) ||
                    isShort(type) ||
                    isByte(type) ||
                    isChar(type) ||

                    Enum::class.java.isAssignableFrom(type)) {

                    return readValue("value", type, jsonData)
                }

                val `object` = newInstance(type)
                if (`object` is JsonSerializable) {
                    `object`.read(this, jsonData)
                    return `object` as T
                }

                // JSON object special cases.
                if (`object` is ObjectMap<*, *>) {
                    val result = `object` as ObjectMap<String, Any?>
                    var child = jsonData.child
                    while (child != null) {
                        result.put(child.name!!, readValue(elementType, null, child))
                        child = child.next
                    }
                    return result as T
                }

                if (`object` is Map<*, *>) {
                    val result = `object` as MutableMap<String, Any?>
                    var child = jsonData.child
                    while (child != null) {
                        if (child.name == typeName) {
                            child = child.next
                            continue
                        }

                        result[child.name!!] = readValue(elementType, null, child)
                        child = child.next
                    }
                    return result as T
                }
                readFields(`object`, jsonData)
                return `object` as T
            }
        }

        if (type != null) {
            val serializer = classToSerializer.get(type)
            if (serializer != null) {
                return serializer.read(this, jsonData, type as Class<Any>) as T
            }

            if (JsonSerializable::class.java.isAssignableFrom(type)) {
                // A Serializable may be read as an array, string, etc, even though it will be written as an object.
                val `object` = newInstance(type)
                (`object` as JsonSerializable).read(this, jsonData)
                return `object` as T
            }
        }

        if (jsonData.isArray) {
            // JSON array special cases.
            if (type == null || type == Any::class.java) {
                type = ArrayList::class.java as Class<T>
            }

            if (Collection::class.java.isAssignableFrom(type)) {
                val result = if (type.isInterface) {
                    ArrayList<Any?>()
                } else {
                    newInstance(type) as MutableCollection<Any?>
                }

                var child = jsonData.child
                while (child != null) {
                    result.add(readValue(elementType as Class<Any>, null, child))
                    child = child.next
                }
                return result as T
            }

            if (type.isArray) {
                val componentType = type.componentType
                if (elementType == null) {
                    elementType = componentType
                }
                val result = java.lang.reflect.Array.newInstance(componentType, jsonData.size)

                var i = 0
                var child = jsonData.child
                while (child != null) {
                    java.lang.reflect.Array.set(result, i++, readValue(elementType, null, child))
                    child = child.next
                }
                return result as T
            }

            throw JsonException("""Unable to convert value to required type: $jsonData (${type.getName()})""")
        }

        if (jsonData.isNumber) {
            try {
                // default is float
                if (type == null ||
                    isFloat(type)) {
                    return jsonData.asFloat() as T
                }
                if (isInt(type)) {
                    return jsonData.asInt() as T
                }
                if (isLong(type)) {
                    return jsonData.asLong() as T
                }
                if (isDouble(type)) {
                    return jsonData.asDouble() as T
                }
                if (isString(type)) {
                    return jsonData.asString() as T?
                }
                if (isShort(type)) {
                    return jsonData.asShort() as T
                }
                if (isByte(type)) {
                    return jsonData.asByte() as T
                }
                if (isShort(type)) {
                    return jsonData.asShort() as T
                }
            } catch (ignored: NumberFormatException) {
            }

            jsonData = JsonValue(jsonData.asString())
        }

        if (jsonData.isBoolean) {
            try {
                if (type == null ||
                    isBoolean(type)) {

                    return jsonData.asBoolean() as T
                }
            } catch (ignored: NumberFormatException) {
            }

            jsonData = JsonValue(jsonData.asString())
        }

        if (jsonData.isString) {
            val string = jsonData.asString()!!
            if (type == null || isString(type)) {
                return string as T?
            }

            try {
                if (isInt(type)) return string.toInt() as T

                if (isFloat(type)) return string.toFloat() as T
                if (isLong(type)) return string.toLong() as T
                if (isDouble(type)) return string.toDouble() as T
                if (isShort(type)) return string.toShort() as T
                if (isByte(type)) return string.toByte() as T
            } catch (ignored: NumberFormatException) {
            }

            if (isBoolean(type)) return string.toBoolean() as T
            if (isChar(type)) return string[0] as T

            if (Enum::class.java.isAssignableFrom(type)) {
                val constants = type.getEnumConstants() as Array<Enum<*>>
                var i = 0
                val n = constants.size

                while (i < n) {
                    val e = constants[i]
                    if (string == convertToString(e)) {
                        return e as T
                    }
                    i++
                }
            }
            if (type == CharSequence::class.java) {
                return string as T?
            }
            throw JsonException("Unable to convert value to required type: $jsonData (${type.getName()})")
        }
        return null
    }

    /** Each field on the `to` object is set to the value for the field with the same name on the `from`
     * object. The `to` object must have at least all the fields of the `from` object with the same name and
     * type.  */
    fun copyFields(from: Any, to: Any) {
        val toFields = getFields(to.javaClass)
        for (entry in getFields(from.javaClass)) {
            val toField = toFields.get(entry.key)
            val fromField = entry.value!!.field
            if (toField == null) throw JsonException("To object is missing field: " + entry.key)
            try {
                toField.field[to] = fromField[from]
            } catch (ex: IllegalAccessException) {
                throw JsonException("Error copying field: " + fromField.name, ex)
            }
        }
    }

    private fun convertToString(e: Enum<*>): String {
        return if (enumNames) e.name else e.toString()
    }

    private fun convertToString(`object`: Any): String {
        if (`object` is Enum<*>) return convertToString(`object`)
        return if (`object` is Class<*>) `object`.getName() else `object`.toString()
    }

    @Suppress("NAME_SHADOWING")
    protected fun newInstance(type: Class<*>): Any {
        var type = type

        return try {
            type.newInstance()
        } catch (ex: Exception) {
            var ex = ex

            try {
                // Try a private constructor.
                val constructor = type.getDeclaredConstructor()
                constructor.setAccessible(true)
                return constructor.newInstance()
            } catch (ignored: SecurityException) {
            } catch (ignored: IllegalAccessException) {
                if (Enum::class.java.isAssignableFrom(type)) {
                    if (type.getEnumConstants() == null) {
                        type = type.superclass
                    }
                    return type.getEnumConstants()[0]
                }

                if (type.isArray) {
                    throw JsonException("Encountered JSON object when expected array of type: " + type.getName(), ex)
                } else if (type.isMemberClass && !Modifier.isStatic(type.modifiers)) {
                    throw JsonException("Class cannot be created (non-static member class): " + type.getName(), ex)
                } else {
                    throw JsonException("Class cannot be created (missing no-arg constructor): " + type.getName(), ex)
                }
            } catch (privateConstructorException: Exception) {
                ex = privateConstructorException
            }

            throw JsonException("Error constructing instance of class: " + type.getName(), ex)
        }
    }

    fun prettyPrint(`object`: Any, singleLineColumns: Int = 0): String {
        return prettyPrint(toJson(`object`), singleLineColumns)
    }

    fun prettyPrint(json: String, singleLineColumns: Int = 0): String {
        return JsonReader().parse(json).prettyPrint(outputType, singleLineColumns)
    }

    fun prettyPrint(`object`: Any, settings: JsonValue.PrettyPrintSettings): String {
        return prettyPrint(toJson(`object`), settings)
    }

    fun prettyPrint(json: String, settings: JsonValue.PrettyPrintSettings): String {
        return JsonReader().parse(json).prettyPrint(settings)
    }

    private class FieldMetadata(val field: Field) {
        var elementType: Class<*>?
        var deprecated: Boolean

        init {
            val index = if (
                ObjectMap::class.java.isAssignableFrom(field.type) ||
                MutableMap::class.java.isAssignableFrom(field.type))
            {
                1
            } else {
                0
            }
            elementType = getElementType(field, index)
            deprecated = field.isAnnotationPresent(Deprecated::class.java)
        }

        companion object {
            /**
             * If the type of the field is parameterized, returns the Class object representing the parameter type at the specified
             * index, null otherwise.
             */
            private fun getElementType(field: Field, index: Int): Class<*>? {
                val genericType = field.genericType
                if (genericType is ParameterizedType) {
                    val actualTypes = genericType.actualTypeArguments
                    if (actualTypes.size - 1 >= index) {

                        val actualType = actualTypes[index]
                        if (actualType is Class<*>) {
                            return actualType
                        } else if (actualType is ParameterizedType) {
                            return actualType.rawType as Class<*>
                        } else if (actualType is GenericArrayType) {
                            val componentType = actualType.genericComponentType

                            if (componentType is Class<*>) {
                                return java.lang.reflect.Array.newInstance(componentType, 0).javaClass
                            }
                        }
                    }
                }
                return null
            }
        }
    }

    companion object {
        /**
         * Gets the version number.
         */
        const val version = "1.3"

        init {
            // Add this project to the updates system, which verifies this class + UUID + version information
            dorkbox.updates.Updates.add(JsonProcessor::class.java, "898c34c06cf044eba523f23c1590f003", version)
        }


        private const val debug = false
        private val locale = Locale.getDefault()


        fun isString(type: Class<*>?): Boolean {
            return type == String::class.java || type == java.lang.String::class.java
        }

        fun isInt(type: Class<*>?): Boolean {
            return type == Int::class.javaPrimitiveType || type == Int::class.java || type == java.lang.Integer::class.java
        }

        fun isBoolean(type: Class<*>?): Boolean {
            return type == Boolean::class.javaPrimitiveType || type == Boolean::class.java || type == java.lang.Boolean::class.java
        }

        fun isFloat(type: Class<*>?): Boolean {
            return type == Float::class.javaPrimitiveType || type == Float::class.java || type == java.lang.Float::class.java
        }

        fun isLong(type: Class<*>?): Boolean {
            return type == Long::class.javaPrimitiveType || type == Long::class.java || type == java.lang.Long::class.java
        }

        fun isDouble(type: Class<*>?): Boolean {
            return type == Double::class.javaPrimitiveType || type == Double::class.java || type == java.lang.Double::class.java
        }

        fun isShort(type: Class<*>?): Boolean {
            return type == Short::class.javaPrimitiveType || type == Short::class.java || type == java.lang.Short::class.java
        }

        fun isByte(type: Class<*>?): Boolean {
            return type == Byte::class.javaPrimitiveType || type == Byte::class.java || type == java.lang.Byte::class.java
        }

        fun isChar(type: Class<*>?): Boolean {
            return type == Char::class.javaPrimitiveType || type == Char::class.java ||  type == java.lang.Character::class.java
        }
    }
}
