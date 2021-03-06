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

import java.util.HashMap;
import java.util.Map;

import net.ontopia.topicmaps.query.toma.impl.basic.expression.*;
import net.ontopia.topicmaps.query.toma.impl.basic.function.*;
import net.ontopia.topicmaps.query.toma.parser.ExpressionFactoryIF;
import net.ontopia.topicmaps.query.toma.parser.ast.ExpressionIF;
import net.ontopia.topicmaps.query.toma.parser.ast.FunctionIF;

/**
 * INTERNAL: Implementation of {@link ExpressionFactoryIF} to create appropriate
 * AST elements for the {@link BasicQueryProcessor}.
 */
public class ExpressionFactory implements ExpressionFactoryIF {

  private Map<String, Class<? extends ExpressionIF>> expressions;
  private Map<String, Class<? extends FunctionIF>> functions;

  public ExpressionFactory() {
    expressions = new HashMap<String, Class<? extends ExpressionIF>>();
    expressions.put("AND", AndExpression.class);
    expressions.put("OR", OrExpression.class);
    expressions.put("EXISTS", ExistsExpression.class);
    expressions.put("NOTEXISTS", NotExistsExpression.class);
    expressions.put("=", EqualsExpression.class);
    expressions.put("!=", NotEqualsExpression.class);
    expressions.put(">", GreaterThanExpression.class);
    expressions.put(">=", GreaterThanEqualsExpression.class);
    expressions.put("<", LessThanExpression.class);
    expressions.put("<=", LessThanEqualsExpression.class);
    expressions.put("~", RegExpMatchExpression.class);
    expressions.put("~*", RegExpIMatchExpression.class);
    expressions.put("!~", NRegExpMatchExpression.class);
    expressions.put("!~*", NRegExpIMatchExpression.class);
    expressions.put("IN", InExpression.class);
    expressions.put("||", ConcatStringExpression.class);

    functions = new HashMap<String, Class<? extends FunctionIF>>();
    functions.put("UPPERCASE", UpperCaseFunction.class);
    functions.put("LOWERCASE", LowerCaseFunction.class);
    functions.put("LENGTH", LengthFunction.class);
    functions.put("SUBSTR", SubstrFunction.class);
    functions.put("TRIM", TrimFunction.class);
    functions.put("TITLECASE", TitleCaseFunction.class);
    functions.put("TO_NUM", ToNumFunction.class);
    
    functions.put("COUNT", CountFunction.class);
    functions.put("SUM", SumFunction.class);
    functions.put("MIN", MinFunction.class);
    functions.put("MAX", MaxFunction.class);
    functions.put("AVG", AvgFunction.class);
    functions.put("CONCAT", ConcatFunction.class);
  }

  public ExpressionIF createExpression(String name, ExpressionIF... childs) {
    Class<? extends ExpressionIF> c = expressions.get(name.toUpperCase());
    if (c != null) {
      try {
        ExpressionIF expr = c.newInstance();
        for (ExpressionIF child : childs) {
          expr.addChild(child);
        }
        return expr;
      } catch (Exception e) {
        return null;
      }
    } else {
      return null;
    }
  }

  public ExpressionIF createLiteral(String value) {
    return new LiteralExpression(value);
  }

  public FunctionIF createFunction(String name) {
    Class<? extends FunctionIF> c = functions.get(name.toUpperCase());
    if (c != null) {
      try {
        FunctionIF fun = c.newInstance();
        return fun;
      } catch (Exception e) {
        return null;
      }
    } else {
      return null;
    }
  }
}
