/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.container.persistence;

import org.apache.commons.io.IOUtils;
import org.hibernate.hql.internal.antlr.SqlStatementLexer;
import org.hibernate.hql.internal.antlr.SqlStatementParser;
import org.hibernate.tool.hbm2ddl.ImportScriptException;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Load and parse import SQL files, either the whole file as a single statement if its first line
 * is <code>-- importOneStatementOnly</code>, or as a semicolon-separated list of statements.
 */
public class EnhancedImportSqlCommandExtractor implements ImportSqlCommandExtractor {

    private static final Logger LOG = Logger.getLogger(EnhancedImportSqlCommandExtractor.class.getName());

    @Override
    public String[] extractCommands(Reader reader) {
        try {
            String sql = IOUtils.toString(reader);
            if (sql.startsWith("-- importOneStatementOnly")) {
                return new String[] {sql};
            } else {
                return extractMultiLine(new StringReader(sql));
            }
        } catch (IOException e) {
            throw new ImportScriptException("Error during import script parsing.", e);
        }
    }

    protected String[] extractMultiLine(Reader reader) {
        final SqlStatementLexer lexer = new SqlStatementLexer(reader);
        final SqlStatementParser parser = new SqlStatementParser(lexer);
        try {
            parser.script(); // Parse script.
            parser.throwExceptionIfErrorOccurred();
        } catch (Exception e) {
            throw new ImportScriptException("Error during import script parsing.", e);
        }
        List<String> statementList = parser.getStatementList();
        String[] result = statementList.toArray(new String[statementList.size()]);
        LOG.info("Importing multiline SQL: " + Arrays.toString(result));
        return result;
    }
}
