/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.appengine.api.datastore;

import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.SortPredicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A class that holds information about a given query component that will later be converted into a
 * {@link MultiQueryComponent}.
 *
 */
class QuerySplitComponent implements Comparable<QuerySplitComponent> {
  enum Order {
    SEQUENTIAL,
    ARBITRARY
  }

  private final Order order;
  private final String propertyName;
  private final int sortIndex;
  private final SortDirection direction;
  private final List<List<FilterPredicate>> filters = new ArrayList<>();

  /**
   * Constructs a new component and uses {@code sorts} to determine how this property should be
   * handled in {@link QuerySplitHelper}
   *
   * @param propertyName the name of the property to which this component applies
   * @param sorts the sort values from the original query
   */
  public QuerySplitComponent(String propertyName, List<SortPredicate> sorts) {
    this.propertyName = propertyName;
    for (int i = 0; i < sorts.size(); ++i) {
      if (sorts.get(i).getPropertyName().equals(propertyName)) {
        this.order = Order.SEQUENTIAL;
        this.sortIndex = i;
        this.direction = sorts.get(i).getDirection();
        return;
      }
    }
    this.order = Order.ARBITRARY;
    this.sortIndex = -1;
    this.direction = null;
  }

  /**
   * Adds a set of filters that are then applied to {@link Query}s generated by {@link
   * MultiQueryBuilder} (in order if order = {@link Order#SEQUENTIAL}).
   */
  public void addFilters(FilterPredicate... filters) {
    this.filters.add(Arrays.asList(filters));
  }

  public List<List<FilterPredicate>> getFilters() {
    return filters;
  }

  public int getSortIndex() {
    return sortIndex;
  }

  public SortDirection getDirection() {
    return direction;
  }

  @Override
  public int compareTo(QuerySplitComponent o) {
    if (!order.equals(o.order)) {
      return order.compareTo(o.order);
    } else {
      return Integer.compare(sortIndex, o.sortIndex);
    }
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {direction, filters, order, propertyName, sortIndex});
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof QuerySplitComponent)) {
      return false;
    }

    QuerySplitComponent other = (QuerySplitComponent) obj;

    return Objects.equals(direction, other.direction)
        && Objects.equals(filters, other.filters)
        && Objects.equals(order, other.order)
        && Objects.equals(propertyName, other.propertyName)
        && sortIndex == other.sortIndex;
  }

  @Override
  public String toString() {
    String result = "QuerySplitComponent [filters=" + filters;
    if (direction != null) {
      result += ", direction=" + direction + ", " + "sortIndex=" + sortIndex;
    }
    return result + "]";
  }
}
