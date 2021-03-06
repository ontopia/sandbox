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
package net.ontopia.topicmaps.query.toma.parser.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ontopia.topicmaps.query.core.InvalidQueryException;
import net.ontopia.topicmaps.query.toma.parser.AntlrWrapException;
import net.ontopia.topicmaps.query.toma.parser.ast.PathElementIF.TYPE;
import net.ontopia.topicmaps.query.toma.util.IndentedStringBuilder;

/**
 * INTERNAL: Represents a single select statement within a TOMA query. A Toma
 * query can contain multiple nested select statements as well as select
 * statements combined in a union-type style.
 */
public class SelectStatement implements ASTElementIF {
  public enum UNION_TYPE {
    NOUNION, UNION, UNIONALL, INTERSECT, EXCEPT
  };

  private boolean distinct;
  private List<ExpressionIF> selects;
  private ExpressionIF clause;
  private UNION_TYPE unionType;
  private Map<String, VariableDecl> variables;

  /**
   * Create a new empty Select Statement.
   */
  public SelectStatement() {
    distinct = false;
    selects = new ArrayList<ExpressionIF>();
    clause = null;
    unionType = UNION_TYPE.NOUNION;
    variables = new HashMap<String, VariableDecl>();
  }

  /**
   * Set the distinct flag for this select statement, indicating whether
   * duplicate rows are present in the output or not.
   * 
   * @param enabled whether distinct behavior should be enabled or not.
   */
  public void setDistinct(boolean enabled) {
    distinct = enabled;
  }

  /**
   * Check whether distinct behavior is enabled or not.
   * 
   * @return the current distinct behavior.
   */
  public boolean isDistinct() {
    return distinct;
  }

  /**
   * Set the union type for this select statement.
   * 
   * @param type the union type to be set.
   */
  public void setUnionType(UNION_TYPE type) {
    unionType = type;
  }

  /**
   * Get the union type for this select statement.
   * 
   * @return the union type.
   */
  public UNION_TYPE getUnionType() {
    return unionType;
  }

  /**
   * Add another select clause expression to this statement.
   * 
   * @param sp the select expression to be added.
   */
  public void addSelect(ExpressionIF sp) {
    selects.add(sp);
  }

  /**
   * Get the number of select clauses present.
   * 
   * @return the number of select clauses.
   */
  public int getSelectCount() {
    return selects.size();
  }

  public int getAggregatedSelectCount() {
    int numAggregate = 0;
    for (ExpressionIF expr : selects) {
      if (expr instanceof FunctionIF) {
        if (((FunctionIF) expr).isAggregateFunction()) {
          numAggregate++;
        }
      }
    }

    return numAggregate;
  }
  
  /**
   * Get the select clause at the given index.
   * 
   * @param index the specified index.
   * @return the select clause at the given index or null if the index is out of
   *         range.
   */
  public ExpressionIF getSelect(int index) {
    try {
      return selects.get(index);
    } catch (IndexOutOfBoundsException e) {
      return null;
    }
  }

  /**
   * Set the where clause for this select statement. As the where clauses are
   * structured in a tree, there is only one root clause that contains all other
   * available clauses.
   * 
   * @param clause the expression to be used as a where clause.
   */
  public void setClause(ExpressionIF clause) {
    this.clause = clause;
  }

  /**
   * Get the where clause for this select statement.
   * 
   * @return the where clause expression.
   */
  public ExpressionIF getClause() {
    return this.clause;
  }

  /**
   * Gets a variable declaration for the given variable name. If there is no
   * variable declaration available yet, a new one will be created.
   * 
   * @param varName the name of the variable.
   * @return the variable declaration for this variable.
   */
  public VariableDecl getVariableDeclaration(String varName) {
    String name = varName.toUpperCase();
    VariableDecl vDecl = variables.get(name);
    if (vDecl == null) {
      vDecl = new VariableDecl(name);
      variables.put(name, vDecl);
    }
    return vDecl;
  }

  public void addDeclarations(Map<String, VariableDecl> declarations) {
    variables.putAll(declarations);
  }
  
  public boolean isAggregated() {
    // validate all select projections
    int numAggregate = 0;
    for (ExpressionIF expr : selects) {
      if (expr instanceof FunctionIF) {
        if (((FunctionIF) expr).isAggregateFunction()) {
          numAggregate++;
        }
      }
    }

    return (numAggregate > 0);
  }
  
  public boolean validate() throws AntlrWrapException {
    // validate all select projections
    int numAggregate = 0;
    for (ExpressionIF expr : selects) {
      if (expr instanceof FunctionIF) {
        if (((FunctionIF) expr).isAggregateFunction()) {
          numAggregate++;
        }
      }
      expr.validate();
    }

    // check that no aggregate function is used in the where clause.
    checkAggregate(clause);
    
    // validate the where clauses
    clause.validate();
    
    // validate that all variables are known
    for (VariableDecl var : variables.values()) {
      if (var.getValidTypes().size() != 1) {
        // if a variable has no qualified type up to now, 
        // we assume it's of type topic.
        try {
          var.constrainTypes(TYPE.TOPIC);
        } catch (InvalidQueryException e) {
          // if this fails -> throw error that variable could not 
          // be qualified to a type.
          throw new AntlrWrapException(new InvalidQueryException("Variable '"
            + var.getVariableName() + "' could not be qualified to a type."));
        }
      }
    }
    
    return true;
  }

  private void checkAggregate(ExpressionIF expr) throws AntlrWrapException {
    if (expr instanceof FunctionIF) {
      if (((FunctionIF) expr).isAggregateFunction()) {
        throw new AntlrWrapException(new InvalidQueryException("Aggregate Function '"
            + expr.toString() + "' not allowed in where clause."));
      }
    }
    
    for (int i = 0; i < expr.getChildCount(); i++) {
      checkAggregate(expr.getChild(i));
    }
  }
  
  public void fillParseTree(IndentedStringBuilder buf, int level) {
    switch (unionType) {
    case UNION:
      buf.append("(     UNION)", level);
      break;

    case UNIONALL:
      buf.append("(     UNION) [ALL]", level);
      break;

    case INTERSECT:
      buf.append("(    EXCEPT)", level);
      break;

    case EXCEPT:
      buf.append("( INTERSECT)", level);
      break;
    }

    buf.append("(    SELECT) [" + (distinct ? "DISTINCT" : "ALL") + "]", level);
    for (ExpressionIF path : selects) {
      path.fillParseTree(buf, level + 1);
    }

    if (clause != null) {
      buf.append("(     WHERE)", level);
      clause.fillParseTree(buf, level + 1);
    }
  }
}
