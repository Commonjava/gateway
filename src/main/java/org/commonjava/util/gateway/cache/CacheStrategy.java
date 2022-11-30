/**
 * Copyright (C) 2020 John Casey (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.util.gateway.cache;

import org.commonjava.util.gateway.config.CacheConfiguration;

import java.io.File;

public interface CacheStrategy
{
    /**
     * Whether the specific path should be read from cache.
     */
    boolean isCache( CacheConfiguration cache, String path );

    /**
     * Whether the specific path should be written to cache after a successful download.
     */
    boolean isCacheForWrite( CacheConfiguration cache, String path );

    /**
     * Get the file to read/write from/to the cache.
     */
    File getCachedFile( CacheConfiguration cache, String path );
}
