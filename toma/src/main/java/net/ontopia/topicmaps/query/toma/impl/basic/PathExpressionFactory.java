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

import net.ontopia.topicmaps.query.toma.impl.basic.expression.PathExpression;
import net.ontopia.topicmaps.query.toma.impl.basic.path.*;
import net.ontopia.topicmaps.query.toma.parser.PathExpressionFactoryIF;
import net.ontopia.topicmaps.query.toma.parser.ast.AbstractTopic;
import net.ontopia.topicmaps.query.toma.parser.ast.PathElementIF;
import net.ontopia.topicmaps.query.toma.parser.ast.PathExpressionIF;
import net.ontopia.topicmaps.query.toma.parser.ast.VariableDecl;
import net.ontopia.topicmaps.query.toma.parser.ast.VariableIF;

/**
 * INTERNAL: Implementation of {@link PathExpressionFactoryIF} to create
 * appropriate AST path expression elements for the {@link BasicQueryProcessor}.
 */
public class PathExpressionFactory implements PathExpressionFactoryIF {

  Map<String, Class<? extends PathElementIF>> elements;

  public PathExpressionFactory() {
    elements = new HashMap<String, Class<? extends PathElementIF>>();
    elements.put("DATA", DataPath.class);
    elements.put("ID", ItemIDPath.class);
    elements.put("INSTANCE", InstancePath.class);
    elements.put("NAME", NamePath.class);
    elements.put("OC", OccurrencePath.class);
    elements.put("PLAYER", PlayerPath.class);
    elements.put("REF", ReferencePath.class);
    elements.put("REIFIER", ReifierPath.class);
    elements.put("ROLE", RolePath.class);
    elements.put("SC", ScopePath.class);
    elements.put("SI", SubjectIDPath.class);
    elements.put("SL", SubjectLocatorPath.class);
    elements.put("SUB", SubTypePath.class);
    elements.put("SUPER", SuperTypePath.class);
    elements.put("TYPE", TypePath.class);
    elements.put("VAR", VariantPath.class);
    elements.put("ASSOC", AssocPath.class);
  }

  public PathElementIF createElement(String name) {
    Class<? extends PathElementIF> c = elements.get(name.toUpperCase());
    if (c != null) {
      try {
        return c.newInstance();
      } catch (Exception e) {
        return null;
      }
    } else {
      return null;
    }
  }

  public PathExpressionIF createPathExpression() {
    return new PathExpression();
  }

  public PathElementIF createTopic(String type, String id) {
    if (type.equals("IID")) {
      return new TopicPath(AbstractTopic.IDTYPE.IID, id);
    } else if (type.equals("NAME")) {
      return new TopicPath(AbstractTopic.IDTYPE.NAME, id);
    } else if (type.equals("VAR")) {
      return new TopicPath(AbstractTopic.IDTYPE.VAR, id);
    } else if (type.equals("SUBJID")) {
      return new TopicPath(AbstractTopic.IDTYPE.SI, id);
    } else if (type.equals("SUBJLOC")) {
      return new TopicPath(AbstractTopic.IDTYPE.SL, id);
    } else {
      return new TopicPath(AbstractTopic.IDTYPE.IID, id);
    }
  }

  public VariableIF createVariable(VariableDecl decl) {
    return new VariablePath(decl);
  }
}
