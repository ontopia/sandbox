package net.ontopia.topicmaps.query.toma.impl.basic.expression;

import net.ontopia.topicmaps.query.core.InvalidQueryException;
import net.ontopia.topicmaps.query.toma.impl.basic.BasicExpressionIF;
import net.ontopia.topicmaps.query.toma.impl.basic.LocalContext;
import net.ontopia.topicmaps.query.toma.impl.basic.ResultSet;
import net.ontopia.topicmaps.query.toma.impl.basic.Row;

/**
 * INTERNAL: Not exists expression, returns all non-valid (null) results of
 * a specified child expression.  
 */
public class NotExistsExpression extends AbstractUnaryExpression {

  public NotExistsExpression() {
    super("NOTEXISTS");
  }

  public ResultSet evaluate(LocalContext context) throws InvalidQueryException {
    if (getChildCount() != 1)
      return null;

    BasicExpressionIF child = (BasicExpressionIF) getChild(0);
    ResultSet rs = child.evaluate(context);

    ResultSet result = new ResultSet(rs);

    for (Row row : rs) {
      Object val = row.getValue(rs.getLastIndex());
      if (val == null) {
        result.addRow(row);
      }
    }

    context.addResultSet(result);
    return result;
  }
}
