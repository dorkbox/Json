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

import java.util.regex.*

enum class OutputType {
    /** Normal JSON, with all its double quotes.  */
    json,

    /** Like JSON, but names are only double-quoted if necessary.  */
    javascript,

    /** Like JSON, but:
     *
     *  * Names only require double quotes if they start with `space` or any of `":,}/` or they contain
     *      `//` or `/ *` or `:`.
     *  * Values only require double quotes if they start with `space` or any of `":,{[]/` or they contain
     *      `//` or `/ *` or any of `}],` or they are equal to `true`, `false` , or `null`.
     *  * Newlines are treated as commas, making commas optional in many cases.
     *  * C style comments may be used: `//...` or `/*...*/`
     *
     */
    minimal;


    fun quoteValue(value: Any?): String {
        if (value == null) return "null"
        val string = value.toString()
        if (value is Number || value is Boolean) return string

        val buffer = StringBuilder(string)
        replace(buffer, '\\', "\\\\")
        replace(buffer, '\r', "\\r")
        replace(buffer, '\n', "\\n")
        replace(buffer, '\t', "\\t")
        if (this == minimal && string != "true" && string != "false" && string != "null" && !string.contains("//") && !string.contains(
                "/*"
            )
        ) {
            val length = buffer.length
            if (length > 0 && buffer[length - 1] != ' ' && minimalValuePattern.matcher(buffer).matches()) return buffer.toString()
        }
        return '"'.toString() + replace(buffer, '"', "\\\"").toString() + '"'
    }

    fun quoteName(value: String): String {
        val buffer = StringBuilder(value)
        replace(buffer, '\\', "\\\\")
        replace(buffer, '\r', "\\r")
        replace(buffer, '\n', "\\n")
        replace(buffer, '\t', "\\t")

        when (this) {
            json -> {} // do nothing
            javascript -> if (javascriptPattern.matcher(buffer).matches()) return buffer.toString()
            minimal -> {
                if (!value.contains("//") && !value.contains("/*") && minimalNamePattern.matcher(buffer).matches()) return buffer.toString()
                if (javascriptPattern.matcher(buffer).matches()) return buffer.toString()
            }

        }
        return '"'.toString() + replace(buffer, '"', "\\\"").toString() + '"'
    }

    companion object {
        private val javascriptPattern = Pattern.compile("^[a-zA-Z_$][a-zA-Z_$0-9]*$")
        private val minimalNamePattern = Pattern.compile("^[^\":,}/ ][^:]*$")
        private val minimalValuePattern = Pattern.compile("^[^\":,{\\[\\]/ ][^}\\],]*$")
        private fun replace(buffer: StringBuilder, find: Char, replace: String): StringBuilder {
            val replaceLength = replace.length
            var index = 0
            while (true) {
                while (true) {
                    if (index == buffer.length) return buffer
                    if (buffer[index] == find) break
                    index++
                }
                buffer.replace(index, index + 1, replace)
                index += replaceLength
            }
        }
    }
}
