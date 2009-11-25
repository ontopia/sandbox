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
package net.ontopia.topicmaps.query.toma.impl.basic;

import net.ontopia.topicmaps.query.core.InvalidQueryException;
import net.ontopia.topicmaps.query.toma.parser.ast.ExpressionIF;

/**
 * INTERNAL: Derived interface for expressions that are being evaluated
 * by the {@link BasicQueryProcessor}.
 */
public interface BasicExpressionIF extends ExpressionIF {
  /**
   * Evaluate the expression based on the local context.
   * 
   * @param context the local context to be used for evaluation.
   * @return the result of the evaluation as a {@link ResultSet}.
   */
  public ResultSet evaluate(LocalContext context) throws InvalidQueryException;
}
