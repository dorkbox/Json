/*
 * Copyright 2024 dorkbox, llc
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

package dorkbox.json

import java.lang.reflect.*

abstract class TypeReference<T> : Comparable<TypeReference<T>> {
    companion object {
        fun getRawType(type: Type?): Class<*> {
            // - Moshi - A modern JSON library for Kotlin and Java
            //  [The Apache Software License, Version 2.0]
            //  https://github.com/square/moshi
            //  Copyright 2020
            //    Square, Inc
            if (type is Class<*>) {
                // type is a normal class.
                return type
            }
            else if (type is ParameterizedType) {
                // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
                // suspects some pathological case related to nested classes exists.
                val rawType = type.rawType
                return rawType as Class<*>
            }
            else if (type is GenericArrayType) {
                val componentType = type.genericComponentType
                return java.lang.reflect.Array.newInstance(getRawType(componentType), 0).javaClass
            }
            else if (type is TypeVariable<*>) {
                // We could use the variable's bounds, but that won't work if there are multiple. having a raw
                // type that's more general than necessary is okay.
                return Any::class.java
            }
            else if (type is WildcardType) {
                return getRawType(type.upperBounds[0])
            }
            else {
                val className = if (type == null) "null" else type.javaClass.name
                throw IllegalArgumentException("Expected a Class, ParameterizedType, or " + "GenericArrayType, but <" + type + "> is of type " + className)
            }
        }
    }

    // gets the parameter type, if there is one
    val parameterType: Class<*>? get() {
        if (javaClass.genericSuperclass is ParameterizedType) {
            val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
            if (type is ParameterizedType) {
                val actualTypes = type.actualTypeArguments
                val x = actualTypes[0]
                return getRawType(x)
            }
        }
        return null
    }

    override fun compareTo(other: TypeReference<T>) = 0
}
