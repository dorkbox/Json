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
package dorkbox.json.annotation

/**
 * Annotation to create an alias for field names serialized to/from json
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Json(
    /**
     * This is used to create an alias for a JSON java/kotlin field, to translate between JSON data and Java object.
     */
    val name: String = "",

    /**
     * This is used to signify that we will ignore this field when reading/writing JSON
     */
    val ignore: Boolean = false
)
