/**
 * Copyright (C) 2009 Space Applications Services
 *   <thomas.neidhart@spaceapplications.com>
 *
 * This file is part of the Ontopia project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ontopia.topicmaps.query.toma.impl.utils;

import java.util.Collection;

import net.ontopia.infoset.core.LocatorIF;
import net.ontopia.topicmaps.core.OccurrenceIF;
import net.ontopia.topicmaps.core.TopicIF;
import net.ontopia.topicmaps.core.TopicNameIF;
import net.ontopia.topicmaps.core.VariantNameIF;

/**
 * INTERNAL: utility class to convert Topic Map constructs into a 
 * string representation.
 */
public class Stringifier {

  /**
   * Returns a string representation of the given object.
   * 
   * <ul>
   * <li>Topic: the address of the first item identifier
   * <li>TopicName: the value of the topic name
   * <li>VariantName: the value of the variant name
   * <li>Locator: the address of the locator
   * <li>Occurrence: the value of the occurrence
   * <li>any other object: the result of toString()
   * </ul>
   * 
   * @param o the object to be converted.
   * @return a string representation of the object
   */
  @SuppressWarnings("unchecked")
  public static String toString(Object o) {
    if (o == null)
      return null;

    if (o instanceof TopicIF) {
      Collection<LocatorIF> iids = ((TopicIF) o).getItemIdentifiers();
      if (iids.isEmpty()) {
        return ((TopicIF) o).getObjectId();
      } else {
        return iids.iterator().next().getAddress();        
      }
    } else if (o instanceof TopicNameIF) {
      return ((TopicNameIF) o).getValue();
    } else if (o instanceof VariantNameIF) {
      return ((VariantNameIF) o).getValue();
    } else if (o instanceof LocatorIF) {
      return ((LocatorIF) o).getAddress();
    } else if (o instanceof OccurrenceIF) {
      return ((OccurrenceIF) o).getValue();
    } else {
      return o.toString();
    }
  }

  /**
   * Returns a string representation of the given object to be used for
   * comparison.
   * 
   * <ul>
   * <li>Topic: the object id
   * <li>TopicName: the value of the topic name
   * <li>VariantName: the value of the variant name
   * <li>Locator: the address of the locator
   * <li>Occurrence: the value of the occurrence
   * <li>any other object: the result of toString()
   * </ul>
   * 
   * @param o the object to be converted.
   * @return a string representation of the object
   */
  public static String toCompare(Object o) {
    if (o == null)
      return null;

    if (o instanceof TopicIF) {
      return ((TopicIF) o).getObjectId();
    } else if (o instanceof TopicNameIF) {
      return ((TopicNameIF) o).getValue();
    } else if (o instanceof VariantNameIF) {
      return ((VariantNameIF) o).getValue();
    } else if (o instanceof LocatorIF) {
      return ((LocatorIF) o).getAddress();
    } else if (o instanceof OccurrenceIF) {
      return ((OccurrenceIF) o).getValue();
    } else {
      return o.toString();
    }
  }
  
  /**
   * Returns a string representation of the given object to be used for
   * comparison.
   * 
   * <ul>
   * <li>Topic: the object id
   * <li>TopicName: the value of the topic name
   * <li>VariantName: the value of the variant name
   * <li>Locator: the address of the locator
   * <li>Occurrence: the value of the occurrence
   * <li>any other object: the result of toString()
   * </ul>
   * 
   * @param o the object to be converted.
   * @return a string representation of the object
   */
  @SuppressWarnings("unchecked")
  public static Comparable toSort(Object o) {
    if (o == null)
      return null;

    if (o instanceof TopicIF) {
      Collection<LocatorIF> iids = ((TopicIF) o).getItemIdentifiers();
      if (iids.isEmpty()) {
        return ((TopicIF) o).getObjectId();
      } else {
        return iids.iterator().next().getAddress();        
      }
    } else if (o instanceof TopicNameIF) {
      return ((TopicNameIF) o).getValue();
    } else if (o instanceof VariantNameIF) {
      return ((VariantNameIF) o).getValue();
    } else if (o instanceof LocatorIF) {
      return ((LocatorIF) o).getAddress();
    } else if (o instanceof OccurrenceIF) {
      return ((OccurrenceIF) o).getValue();
    } else if (o instanceof Integer || o instanceof Double){
      return (Comparable) o;
    } else {
      return o.toString();
    }
  }
}
