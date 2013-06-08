/*
 * Copyright 2013 creeperde14 [creeperdedpt@gmail.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.creeperded14.myauth;

import java.util.logging.Logger;

public class ConsoleLogger {

    private static final Logger log = Logger.getLogger("Minecraft");

    public static void info(String message) {
        log.info("[MyAuth] " + message);
    }

    public static void showError(String message) {
        log.severe("[MyAuth] ERROR: " + message);
    }
}
