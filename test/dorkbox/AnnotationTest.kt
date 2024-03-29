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
package dorkbox

import dorkbox.json.Json
import org.junit.Assert
import org.junit.Test

/**
 *
 */
class AnnotationTest {

    @Test
    fun annotationTest() {
        val json = Json()

        val conf = Config()
        val jsonString = json.to(conf)

        // NOTE: hotDog is ignored!!
        Assert.assertEquals("{ip_address:127.0.0.1,server:false,json_client_val:true}", jsonString)

        val newConf = json.fromJson(Config::class.java, jsonString)

        Assert.assertTrue(newConf != null)
        newConf!!
        Assert.assertTrue(conf.ip == newConf.ip)
        Assert.assertTrue(conf.server == newConf.server)
        Assert.assertTrue(conf.client == newConf.client)
    }

    class Config {
        @dorkbox.json.annotation.Json("ip_address")
        var ip = "127.0.0.1"

        var server = false

        @dorkbox.json.annotation.Json("json_client_val")
        var client = true

        @dorkbox.json.annotation.Json("hot", ignore = true)
        var hotDogs = true
    }
}


