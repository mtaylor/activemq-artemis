/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.core.management.impl.view;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import org.apache.activemq.artemis.core.management.impl.view.predicate.ActiveMQFilterPredicate;
import org.apache.activemq.artemis.core.management.impl.view.predicate.SessionFilterPredicate;
import org.apache.activemq.artemis.utils.JsonLoader;

public abstract class ActiveMQAbstractView<T> {

   protected Collection<T> collection;

   protected ActiveMQFilterPredicate<T> predicate;

   protected String sortColumn;

   protected String sortOrder;

   protected String filter;

   private Method getter;

   public ActiveMQAbstractView() {
      this.sortColumn = getDefaultOrderColumn();
      this.sortOrder = "asc";
   }

   public void setCollection(Collection<T> collection) {
      this.collection = collection;
   }

   public String getResultsAsJson(int page, int pageSize) {
      JsonArrayBuilder array = JsonLoader.createArrayBuilder();
      for (T element : getPagedResult(page, pageSize)) {
         array.add(toJson(element));
      }
      return array.build().toString();
   }

   public List<T> getPagedResult(int page, int pageSize) {
      ImmutableList.Builder<T> builder = ImmutableList.builder();
      int start = (page - 1) * pageSize;
      int end = Math.min(page * pageSize, collection.size());
      int i = 0;
      for (T e : getOrdering().sortedCopy(collection)) {
         if (i >= start && i < end) {
            builder.add(e);
         }
         i++;
      }
      return builder.build();
   }

   public Predicate getPredicate() {
      return predicate;
   }

   private Method getGetter() {
      if (getter == null) {
         getter = findGetterMethod(getClassT(), sortColumn);
      }
      return getter;
   }

   public Ordering<T> getOrdering() {
      return new Ordering<T>() {

         @Override
         public int compare(T left, T right) {
            Method getter = getGetter();
            try {
               if (getter != null) {
                  Object leftValue = getter.invoke(left);
                  Object rightValue = getter.invoke(right);
                  if (leftValue instanceof Comparable && rightValue instanceof Comparable) {
                     if (sortOrder.equals("desc")) {
                        return ((Comparable) rightValue).compareTo(leftValue);
                     } else {
                        return ((Comparable) leftValue).compareTo(rightValue);
                     }
                  }
               }
               return 0;
            } catch (Exception e) {
               //LOG.info("Exception sorting destinations", e);
               return 0;
            }
         }
      };
   }

   public static Method findGetterMethod(Class clazz, String sortColumn) {
      // Build the method name.
      // TODO (mtaylor) This is pretty brittle only works with camel case names
      String name = "get" + Character.toUpperCase(sortColumn.charAt(0)) + sortColumn.substring(1);
      Method[] methods = clazz.getMethods();
      for (Method method : methods) {
         Class<?>[] params = method.getParameterTypes();
         if (method.getName().equals(name) && params.length == 0) {
            return method;
         }
      }
      return null;
   }

   public void setFilter(String filter) {
      this.filter = filter;
   }

   public String getFilter() {
      return filter;
   }

   public abstract Class getClassT();

   public abstract JsonObjectBuilder toJson(T obj);

   public abstract String getDefaultOrderColumn();
}
