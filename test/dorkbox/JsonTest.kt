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

// From libGDX tests:
// https://github.com/libgdx/libgdx/blob/master/tests/gdx-tests/src/com/badlogic/gdx/tests/JsonTest.java
package dorkbox

import dorkbox.collections.ObjectMap
import dorkbox.json.Json
import dorkbox.json.OutputType
import org.junit.Test

class JsonTest {

    @Test
    fun create() {
        val json = Json()

        var test = Test1()
        test.booleanField = true
        test.byteField = 123
        test.charField = 'Z'
        test.shortField = 12345
        test.intField = 123456
        test.longField = 123456789
        test.floatField = 123.456f
        test.doubleField = 1.23456
        test.BooleanField = true
        test.ByteField = -12
        test.CharacterField = 'X'
        test.ShortField = -12345
        test.IntegerField = -123456
        test.LongField = -123456789L
        test.FloatField = -123.3f
        test.DoubleField = -0.121231
        test.stringField = "stringvalue"
        test.byteArrayField = byteArrayOf(2, 1, 0, -1, -2)
        test.map = ObjectMap()
        test.map!!.put("one", 1)
        test.map!!.put("two", 2)
        test.map!!.put("nine", 9)
        test.stringArray = arrayOf("meow", "moo")
        test.objectArray = arrayOf("meow", Test1())
//        test.longMap = LongMap<String>(4)
//        test.longMap.put(42L, "The Answer")
//        test.longMap.put(-0x61c8864680b583ebL, "Golden Ratio")
//        test.stringFloatMap = ObjectFloatMap<String>(4)
//        test.stringFloatMap.put("point one", 0.1f)
//        test.stringFloatMap.put("point double oh seven", 0.007f)
        test.someEnum = SomeEnum.b

        // IntIntMap can be written, but only as a normal object, not as a kind of map.
//        test.intsToIntsUnboxed = IntIntMap()
//        test.intsToIntsUnboxed.put(102, 14)
//        test.intsToIntsUnboxed.put(107, 1)
//        test.intsToIntsUnboxed.put(10, 2)
//        test.intsToIntsUnboxed.put(2, 1)
//        test.intsToIntsUnboxed.put(7, 3)
//        test.intsToIntsUnboxed.put(101, 63)
//        test.intsToIntsUnboxed.put(4, 2)
//        test.intsToIntsUnboxed.put(106, 4)
//        test.intsToIntsUnboxed.put(1, 1)
//        test.intsToIntsUnboxed.put(103, 2)
//        test.intsToIntsUnboxed.put(6, 2)
//        test.intsToIntsUnboxed.put(3, 1)
//        test.intsToIntsUnboxed.put(105, 6)
//        test.intsToIntsUnboxed.put(8, 2)
        // The above "should" print like this:
        // {size:14,keyTable:[0,0,102,0,0,0,0,0,107,0,0,10,0,0,0,2,0,0,0,0,7,0,0,0,0,0,101,0,0,0,4,0,106,0,0,0,0,0,0,1,0,0,103,0,0,6,0,0,0,0,0,0,0,0,3,0,0,105,0,0,8,0,0,0],valueTable:[0,0,14,0,0,0,0,0,1,0,0,2,0,0,0,1,0,0,0,0,3,0,0,0,0,0,63,0,0,0,2,0,4,0,0,0,0,0,0,1,0,0,2,0,0,2,0,0,0,0,0,0,0,0,1,0,0,6,0,0,2,0,0,0]}
        // This is potentially correct, but also quite large considering the contents.
        // It would be nice to have IntIntMap look like IntMap<Integer> does, below.

        // IntMap gets special treatment and is written as a kind of map.
//        test.intsToIntsBoxed = IntMap<Int>()
//        test.intsToIntsBoxed.put(102, 14)
//        test.intsToIntsBoxed.put(107, 1)
//        test.intsToIntsBoxed.put(10, 2)
//        test.intsToIntsBoxed.put(2, 1)
//        test.intsToIntsBoxed.put(7, 3)
//        test.intsToIntsBoxed.put(101, 63)
//        test.intsToIntsBoxed.put(4, 2)
//        test.intsToIntsBoxed.put(106, 4)
//        test.intsToIntsBoxed.put(1, 1)
//        test.intsToIntsBoxed.put(103, 2)
//        test.intsToIntsBoxed.put(6, 2)
//        test.intsToIntsBoxed.put(3, 1)
//        test.intsToIntsBoxed.put(105, 6)
//        test.intsToIntsBoxed.put(8, 2)
        // The above should print like this:
        // {102:14,107:1,10:2,2:1,7:3,101:63,4:2,106:4,1:1,103:2,6:2,3:1,105:6,8:2}
        roundTrip(json, test)
        var sum = 0
//        // iterate over an IntIntMap so one of its Entries is instantiated
//        for (e in test.intsToIntsUnboxed) {
//            sum += e.value + 1
//        }
        // also iterate over an Array, which does not have any problems
        var concat = ""
        for (s in test.stringArray!!) {
            concat += s
        }
        // by round-tripping again, we verify that the Entries is correctly skipped
        roundTrip(json, test)
        var sum2 = 0
//        // check and make sure that no entries are skipped over or incorrectly added
//        for (e in test.intsToIntsUnboxed) {
//            sum2 += e.value + 1
//        }
        var concat2 = ""
        // also check the Array again
        for (s in test.stringArray!!) {
            concat2 += s
        }

        println("before: $sum, after: $sum2")
        println("before: $concat, after: $concat2")

        test.someEnum = null
        roundTrip(json, test)
        test = Test1()
        roundTrip(json, test)

        test.stringArray = arrayOf("", "")
        roundTrip(json, test)

        test.stringArray!![0] = ("meow")
        roundTrip(json, test)

        test.stringArray!![1] = ("moo")
        roundTrip(json, test)

        val objectGraph = TestMapGraph()
        testObjectGraph(objectGraph, "exoticTypeName")
        test = Test1()
        test.map = ObjectMap()
        roundTrip(json, test)
        test.map!!.put("one", 1)
        roundTrip(json, test)

        test.map!!.put("two", 2)
        test.map!!.put("nine", 9)
        roundTrip(json, test)

        test.map!!.put("\nst\nuff\n", 9)
        test.map!!.put("\r\nst\r\nuff\r\n", 9)
        roundTrip(json, test)

        equals(json.toJson("meow"), "meow")
        equals(json.toJson("meow "), "\"meow \"")
        equals(json.toJson(" meow"), "\" meow\"")
        equals(json.toJson(" meow "), "\" meow \"")
        equals(json.toJson("\nmeow\n"), "\\nmeow\\n")

        equals(json.toJson(arrayOf(1, 2, 3), null, Int::class.javaPrimitiveType), "[1,2,3]")
        equals(json.toJson(arrayOf("1", "2", "3"), null, String::class.java), "[1,2,3]")
        equals(json.toJson(arrayOf(" 1", "2 ", " 3 "), null, String::class.java), "[\" 1\",\"2 \",\" 3 \"]")
        equals(json.toJson(arrayOf("1", "", "3"), null, String::class.java), "[1,\"\",3]")
        println()
        println("Success!")
    }

    private fun roundTrip(json: Json, `object`: Any): String {
        var text = json.toJson(`object`)
        println(text)
        test(json, text, `object`)
        text = json.prettyPrint(`object`, 130)
        test(json, text, `object`)
        return text
    }

    private fun testObjectGraph(`object`: TestMapGraph, typeName: String) {
        val json = Json()
        json.typeName = typeName
        json.usePrototypes = false
        json.ignoreUnknownFields = true
        json.outputType = OutputType.json
        val text = json.prettyPrint(`object`)
        val object2 = json.fromJson(TestMapGraph::class.java, text)!!
        if (object2.map.size != `object`.map.size) {
            throw RuntimeException("Too many items in deserialized json map.")
        }
        if (object2.objectMap.size != `object`.objectMap.size) {
            throw RuntimeException("Too many items in deserialized json object map.")
        }
//        if (object2.arrayMap.size !== `object`.arrayMap.size) {
//            throw RuntimeException("Too many items in deserialized json map.")
//        }
    }

    private fun test(json: Json, text: String, `object`: Any) {
        var text = text
        check(json, text, `object`)
        text = text.replace("{", "/*moo*/{/*moo*/")
        check(json, text, `object`)
        text = text.replace("}", "/*moo*/}/*moo*/")
        text = text.replace("[", "/*moo*/[/*moo*/")
        text = text.replace("]", "/*moo*/]/*moo*/")
        text = text.replace(":", "/*moo*/:/*moo*/")
        text = text.replace(",", "/*moo*/,/*moo*/")
        check(json, text, `object`)
        text = text.replace("/*moo*/", " /*moo*/ ")
        check(json, text, `object`)
        text = text.replace("/*moo*/", "// moo\n")
        check(json, text, `object`)
        text = text.replace("\n", "\r\n")
        check(json, text, `object`)
        text = text.replace(",", "\n")
        check(json, text, `object`)
        text = text.replace("\n", "\r\n")
        check(json, text, `object`)
        text = text.replace("\r\n", "\r\n\r\n")
        check(json, text, `object`)
    }

    private fun check(json: Json, text: String, `object`: Any) {
        val object2 = json.fromJson(`object`.javaClass, text)
        equals(`object`, object2)
    }

    private fun equals(a: Any, b: Any?) {
        if (a != b) throw RuntimeException("Fail!\n$a\n!=\n$b")
    }

    class Test1 {
        // Primitives.
        var booleanField = false
        var byteField: Byte = 0
        var charField = 0.toChar()
        var shortField: Short = 0
        var intField = 0
        var longField: Long = 0
        var floatField = 0f
        var doubleField = 0.0

        // Primitive wrappers.
        var BooleanField: Boolean? = null
        var ByteField: Byte? = null
        var CharacterField: Char? = null
        var ShortField: Short? = null
        var IntegerField: Int? = null
        var LongField: Long? = null
        var FloatField: Float? = null
        var DoubleField: Double? = null

        // Other.
        var stringField: String? = null
        var byteArrayField: ByteArray? = null
        var `object`: Any? = null
        var map: ObjectMap<String, Int>? = null
        var stringArray: Array<String>? = null
        var objectArray: Array<Any>? = null
//        var longMap: LongMap<String>? = null
//        var stringFloatMap: ObjectFloatMap<String>? = null
        var someEnum: SomeEnum? = null
//        var intsToIntsBoxed: IntMap<Int>? = null
//        var intsToIntsUnboxed: IntIntMap? = null

        override fun equals(obj: Any?): Boolean {
            if (this === obj) return true
            if (obj == null) return false
            if (javaClass != obj.javaClass) return false
            val other = obj as Test1
            if (BooleanField == null) {
                if (other.BooleanField != null) return false
            }
            else if (BooleanField != other.BooleanField) return false
            if (ByteField == null) {
                if (other.ByteField != null) return false
            }
            else if (ByteField != other.ByteField) return false
            if (CharacterField == null) {
                if (other.CharacterField != null) return false
            }
            else if (CharacterField != other.CharacterField) return false
            if (DoubleField == null) {
                if (other.DoubleField != null) return false
            }
            else if (DoubleField != other.DoubleField) return false
            if (FloatField == null) {
                if (other.FloatField != null) return false
            }
            else if (FloatField != other.FloatField) return false
            if (IntegerField == null) {
                if (other.IntegerField != null) return false
            }
            else if (IntegerField != other.IntegerField) return false
            if (LongField == null) {
                if (other.LongField != null) return false
            }
            else if (LongField != other.LongField) return false
            if (ShortField == null) {
                if (other.ShortField != null) return false
            }
            else if (ShortField != other.ShortField) return false
            if (stringField == null) {
                if (other.stringField != null) return false
            }
            else if (stringField != other.stringField) return false
            if (booleanField != other.booleanField) return false
            val list1 = byteArrayField?.toList()
            val list2 = other.byteArrayField?.toList()
            if (list1 !== list2) {
                if (list1 == null || list2 == null) return false
                if (list1 != list2) return false
            }
            if (`object` !== other.`object`) {
                if (`object` == null || other.`object` == null) return false
                if (`object` !== this && `object` != other.`object`) return false
            }
            if (map !== other.map) {
                if (map == null || other.map == null) return false
                if (!map!!.keys().toArray().contentEquals(other.map!!.keys().toArray())) return false
                if (!map!!.values().toArray().contentEquals(other.map!!.values().toArray())) return false
            }
            if (stringArray !== other.stringArray) {
                if (stringArray == null || other.stringArray == null) return false
                if (!stringArray!!.contentEquals(other.stringArray)) return false
            }
            if (objectArray !== other.objectArray) {
                if (objectArray == null || other.objectArray == null) return false
                if (!objectArray!!.contentEquals(other.objectArray)) return false
            }
//            if (longMap !== other.longMap) {
//                if (longMap == null || other.longMap == null) return false
//                if (!longMap.equals(other.longMap)) return false
//            }
//            if (stringFloatMap !== other.stringFloatMap) {
//                if (stringFloatMap == null || other.stringFloatMap == null) return false
//                if (!stringFloatMap.equals(other.stringFloatMap)) return false
//            }
//            if (intsToIntsBoxed !== other.intsToIntsBoxed) {
//                if (intsToIntsBoxed == null || other.intsToIntsBoxed == null) return false
//                if (!intsToIntsBoxed.equals(other.intsToIntsBoxed)) return false
//            }
//            if (intsToIntsUnboxed !== other.intsToIntsUnboxed) {
//                if (intsToIntsUnboxed == null || other.intsToIntsUnboxed == null) return false
//                if (!intsToIntsUnboxed.equals(other.intsToIntsUnboxed)) return false
//            }
            if (byteField != other.byteField) return false
            if (charField != other.charField) return false
            if (java.lang.Double.doubleToLongBits(doubleField) != java.lang.Double.doubleToLongBits(other.doubleField)) return false
            if (java.lang.Float.floatToIntBits(floatField) != java.lang.Float.floatToIntBits(other.floatField)) return false
            if (intField != other.intField) return false
            if (longField != other.longField) return false
            return if (shortField != other.shortField) false else true
        }
    }

    class TestMapGraph {
        var map: MutableMap<String, String> = HashMap()
        var objectMap = ObjectMap<String, String>()
//        var arrayMap: ArrayMap<String, String> = ArrayMap<String, String>()

        init {
            map["a"] = "b"
            map["c"] = "d"
            objectMap.put("a", "b")
            objectMap.put("c", "d")
//            arrayMap.put("a", "b")
//            arrayMap.put("c", "d")
        }
    }

    enum class SomeEnum {
        a, b, c
    }
}
