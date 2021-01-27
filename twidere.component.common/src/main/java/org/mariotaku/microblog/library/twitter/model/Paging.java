/*
 *         Twidere - Twitter client for Android
 *
 * Copyright 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package org.mariotaku.microblog.library.twitter.model;

import org.mariotaku.restfu.http.SimpleValueMap;

/**
 * Created by mariotaku on 15/2/6.
 */
public class Paging extends SimpleValueMap {

    public void setMinPosition(long minPosition) {
        put("min_position", minPosition);
    }

    public void setMaxPosition(long maxPosition) {
        put("max_position", maxPosition);
    }

    public void setCount(int count) {
        put("count", count);
    }

    public void setCursor(long cursor) {
        put("cursor", cursor);
    }

    public void setCursor(String cursor) {
        put("cursor", cursor);
    }

    public void setLatestResults(boolean latestResults) {
        put("latest_results", latestResults);
    }

    public Paging sinceId(String sinceId) {
        put("since_id", sinceId);
        return this;
    }

    public Paging latestResults(boolean latestResults) {
        setLatestResults(latestResults);
        return this;
    }

    public Paging maxId(String maxId) {
        put("max_id", maxId);
        return this;
    }

    public Paging maxPosition(long maxPosition) {
        setMaxPosition(maxPosition);
        return this;
    }

    public Paging minPosition(long minPosition) {
        setMinPosition(minPosition);
        return this;
    }

    public Paging count(int count) {
        setCount(count);
        return this;
    }

    public Paging page(int page) {
        put("page", page);
        return this;
    }

    public Paging cursor(long cursor) {
        setCursor(cursor);
        return this;
    }

    public Paging cursor(String cursor) {
        setCursor(cursor);
        return this;
    }

    public Paging limit(int limit) {
        put("limit", limit);
        return this;
    }

    public Paging rpp(int rpp) {
        put("rpp", rpp);
        return this;
    }

    @Override
    public String toString() {
        String res = "Paging:";
        if (has("min_position")) res += " [min_position] " + get("min_position");
        if (has("max_position")) res += " [max_position] " + get("max_position");
        if (has("count")) res += " [count] " + get("count");
        if (has("cursor")) res += " [cursor] " + get("cursor");
        if (has("latest_results")) res += " [latest_results] " + get("latest_results");
        if (has("since_id")) res += " [since_id] " + get("since_id");
        if (has("max_id")) res += " [max_id] " + get("max_id");
        if (has("page")) res += " [page] " + get("page");
        if (has("limit")) res += " [limit] " + get("limit");
        if (has("rpp")) res += " [rpp] " + get("rpp");
        return res;
    }
}
