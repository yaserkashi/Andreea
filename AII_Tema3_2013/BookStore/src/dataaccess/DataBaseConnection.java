package dataaccess;

import general.Constants;
import general.ReferencedTable;
import general.Utilities;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DataBaseConnection {
    public static Connection        dbConnection;
    public static DatabaseMetaData  dbMetaData;
    public static Statement         stmt;

    public DataBaseConnection() { }

    public static void openConnection() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver"); 
        } catch (ClassNotFoundException exception) {
            System.out.println ("exceptie: "+exception.getMessage());
            if (Constants.DEBUG)
                exception.printStackTrace();
        }
        dbConnection    = DriverManager.getConnection(general.Constants.DATABASE_CONNECTION, general.Constants.DATABASE_USER, general.Constants.DATABASE_PASSWORD);
        dbMetaData      = dbConnection.getMetaData();
        stmt            = dbConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
    }

    public static void closeConnection() throws SQLException {
        stmt.close();
        dbConnection.close();
    }
 
    public static ArrayList<String> getTableNames() throws SQLException {
        openConnection();
        ArrayList<String> result = new ArrayList<>();
        ResultSet RS = dbMetaData.getTables(Constants.DATABASE_NAME, null, null, null);
        while (RS.next()) {
            result.add(RS.getString("TABLE_NAME"));
        }
        closeConnection();
        return result;
    }    
    
    public static int getTableNumberOfRows(String tableName) throws SQLException {
        openConnection();
        String query = "SELECT COUNT(*) FROM "+tableName;
        ResultSet RS = stmt.executeQuery(query);
        RS.next();
        int numberOfRows = RS.getInt(1);
        closeConnection();
        return numberOfRows;
    }
    
    public static int getTablePrimaryKeyMaxValue(String tableName) throws SQLException {
        String primaryKey = getTablePrimaryKey(tableName);
        String query = "SELECT MAX("+primaryKey+") FROM "+tableName;
        openConnection();
        ResultSet RS = stmt.executeQuery(query);
        RS.next();
        int result = RS.getInt(1);
        closeConnection();
        return result;
    }
    
    public static int getTableNumberOfColumns(String tableName) throws SQLException {
        int result = 0;
        openConnection();
        ResultSet RS = dbMetaData.getColumns(Constants.DATABASE_NAME, null, tableName, null);
        while (RS.next()) 
            result++;
        closeConnection();
        return result;
    }

    public static String getTablePrimaryKey(String tableName) throws SQLException {
        String result = new String();
        openConnection();
        ResultSet RS = dbMetaData.getPrimaryKeys(Constants.DATABASE_NAME, null, tableName);
        while (RS.next())
            result += RS.getString("COLUMN_NAME")+" ";
        closeConnection();
        return result != null ? result.trim() : result;
    }   
    
    public static int getAttributeIndexInTable(String tableName, String attributeName) {
        int result = -1;
        try {
            for (String tableAttribute: getTableAttributes(tableName)) {
                result++;
                if (tableAttribute.equals(attributeName))
                    return result;
            }
        } catch (Exception exception) {
            System.out.println ("Exceptie: "+exception.getMessage());
        }
        return -1;
    }            
    
    public static ArrayList<String> getTableAttributes(String tableName) throws SQLException {
        ArrayList<String> result = new ArrayList<>();
        openConnection();
        ResultSet RS = dbMetaData.getColumns(Constants.DATABASE_NAME, null, tableName, null);
        while (RS.next())
            result.add(RS.getString("COLUMN_NAME"));
        closeConnection();
        return result;
    }

    public static ArrayList<ArrayList<Object>> executeQuery(String query, int numberOfColumns) throws SQLException {
        openConnection();
        ArrayList<ArrayList<Object>> dataBaseContent = new ArrayList<>();
        ResultSet result = stmt.executeQuery(query);  
        int currentRow = 0;
        while (result.next()) {
            dataBaseContent.add(new ArrayList<>());
            for (int currentColumn = 0; currentColumn < numberOfColumns; currentColumn++) {
                dataBaseContent.get(currentRow).add(result.getString(currentColumn+1));
            }
            currentRow++;
        }
        closeConnection();
        return dataBaseContent;
    }

    public static void insertValuesIntoTable(String tableName, ArrayList<String> attributes, ArrayList<String> values, boolean skipPrimaryKey) throws Exception {
        String expression = "INSERT INTO "+tableName+" (";
        if (attributes == null) {
            attributes = getTableAttributes(tableName);
            if (skipPrimaryKey) {
                attributes.remove(0);
            }
        }
        if (attributes.size() != values.size()) {
            throw new Exception ("Attributes size does not match values size !"+attributes.size()+" "+values.size());
        }
        for (String attribute:attributes) {
            expression += attribute + ", ";
        }      
        expression = expression.substring(0,expression.length()-2);
        expression += ") VALUES (";
        for (String currentValue: values) {
           if (Utilities.isSystemFunction(currentValue))
                expression += currentValue+",";
            else
                expression += "\'"+currentValue+"\',";
        }
        expression = expression.substring(0,expression.length()-1);
        expression += ")";
        if (general.Constants.DEBUG) {
            System.out.println("query: "+expression);
        }
        openConnection();
        stmt.execute(expression);
        closeConnection();
    }

    public static void updateRecordsIntoTable(String tableName, ArrayList<String> attributes, ArrayList<String> values, String whereClause) throws Exception {
        String expression = "UPDATE "+tableName+" SET ";
        if (attributes == null) {
            attributes = getTableAttributes(tableName);
        }
        if (attributes.size() != values.size()) {
            throw new Exception ("Attributes size does not match values size.");
        }
        for (int currentIndex = 0; currentIndex < values.size(); currentIndex++)
            expression += attributes.get(currentIndex)+"=\'"+values.get(currentIndex)+"\', ";
        expression = expression.substring(0,expression.length()-2);
        expression += " WHERE ";
        if (whereClause != null ) {
            expression += whereClause;
        } else {
            expression += getTablePrimaryKey(tableName)+"=\'"+values.get(0)+"\'";
        }
        if (general.Constants.DEBUG) {
            System.out.println("query: "+expression);
        }
        openConnection();
        stmt.execute(expression);
        closeConnection();
    }

    public static void deleteRecordsFromTable(String tableName, ArrayList<String> attributes, ArrayList<String> values, String whereClause) throws Exception {
        String expression = "DELETE FROM "+tableName+" WHERE ";
        if (whereClause != null) {
            expression += whereClause;
        } else {
            if (attributes.size() != values.size()) {
                if (Constants.DEBUG) {
                    System.out.print("attributes: ");
                    for (String attribute:attributes)
                        System.out.print(attribute+" ");
                    System.out.println();
                    System.out.print("values: ");
                    for (String value:values)
                        System.out.print(value+" ");
                    System.out.println();               
                }
                throw new Exception ("Attributes size does not match values size !");
            }
            for (int currentIndex = 0; currentIndex < values.size(); currentIndex++) {
                expression += attributes.get(currentIndex)+"=\'"+values.get(currentIndex)+"\' AND";
            }
            expression = expression.substring(0,expression.length()-4);
        }
        if (general.Constants.DEBUG) {
            System.out.println("query: "+expression);
        }
        openConnection();        
        stmt.execute(expression);
        closeConnection();
    }
    
    public static String executeProcedure(String procedureName, ArrayList<String> parameterTypes, ArrayList<String> parameterValues, ArrayList<Integer> parameterDataTypes) throws SQLException {
        String result = new String();
        int resultIndex = -1;
        openConnection();
        String query = "{CALL "+procedureName+"(";
        int parameterNumber = parameterTypes.size();
        for (int count = 1; count <= parameterNumber; count++)
            query += "?, ";
        if (parameterNumber != 0)
            query = query.substring(0,query.length()-2);
        query += ")}";
        CallableStatement cstmt = dbConnection.prepareCall(query);
        int i = 0, j = 0, k = 1;
        for (String parameterType:parameterTypes) {
            switch (parameterType) {
                case "IN":
                    cstmt.setString(k,parameterValues.get(i++));
                    break;
                case "OUT":
                    cstmt.registerOutParameter(k,parameterDataTypes.get(j++).intValue());
                    resultIndex = k;
                    break;
                case "INOUT":
                    cstmt.setString(k,parameterValues.get(i++));
                    cstmt.registerOutParameter(k, parameterDataTypes.get(j++).intValue());
                    resultIndex = k;
                    break;
            }
            k++;
        }
        cstmt.execute();
        result = cstmt.getString(resultIndex);
        closeConnection();
        return result;
    }
    
    public static ArrayList<ReferencedTable> getReferrencedTables(String tableName) throws SQLException {
        ArrayList<ReferencedTable> result = new ArrayList<>();
        openConnection();
        ResultSet RS = dbMetaData.getImportedKeys(Constants.DATABASE_NAME, null, tableName);
        while (RS.next())
            result.add(new ReferencedTable(RS.getString("PKTABLE_NAME"),RS.getString("PKCOLUMN_NAME")));
        closeConnection();
        return result;
    } 
    
    public static String foreignKeyParentTable(String attribute, ArrayList<ReferencedTable> referrencedTables) {
        for (ReferencedTable referrencedTable:referrencedTables) {
            if (attribute.equals(referrencedTable.getAttributeName())) {
                return referrencedTable.getParentTable();
            }
        }
        return null;
    }
    
    public static String getTableDescription(String tableName) {
        switch(tableName) {
            case "edituri":
                return "CONCAT(denumire,'-',CIF)";
            case "colectii":
                return "denumire";
            case "domenii":
                return "denumire";
            case "carti":
                return "titlu";
            case "scriitori":
                return "CONCAT(nume,' ',prenume)";
            case "autori":
                return "CONCAT(idscriitor,'->',idcarte)";
            case "comenzi_aprovizionare":
                return "CONCAT(serienumar,'/',data)";
            case "detalii_comanda_aprovizionare":
                return "CONCAT(idcomandaaprovizionare,' ',idcarte";
            case "utilizatori":
                return "CONCAT(prenume,' ',nume)";
            case "facturi":
                return "CONCAT(serienumar,'/',data)";
            case "detalii_factura":
                return "CONCAT(idfactura,' ',idcarte)";               
        }
        return null;
    }
    
    public static ArrayList<ArrayList<Object>> getTableContent(String tableName, ArrayList<String> attributes, String whereClause, String orderByClause, String groupByClause) throws SQLException {
        String expression = "SELECT ";
        int numberOfColumns = -1;
        if (attributes == null) {
            numberOfColumns = getTableNumberOfColumns(tableName);
            expression += "*";            
        }
        else {
            numberOfColumns = attributes.size();
            for (String attribute:attributes) {
                expression += attribute+", ";
            }
            expression = expression.substring(0,expression.length()-2);            
        }
        expression += " FROM "+tableName;
        if (whereClause != null) {
            expression += " WHERE "+whereClause;
        }
        if (groupByClause != null) {
            expression += " GROUP BY "+groupByClause;
        }
        if (orderByClause != null) {
            expression += " ORDER BY "+orderByClause;
        }
        if (general.Constants.DEBUG) {
            System.out.println("query: "+expression);
        }
        if (numberOfColumns == -1) {
            return null;
        }      
        openConnection();
        ArrayList<ArrayList<Object>> dataBaseContent = new ArrayList<>();
        ResultSet result = stmt.executeQuery(expression);  
        int currentRow = 0;
        while (result.next()) {
            dataBaseContent.add(new ArrayList<>());
            for (int currentColumn = 0; currentColumn < numberOfColumns; currentColumn++) {
                dataBaseContent.get(currentRow).add(result.getString(currentColumn+1));
            }
            currentRow++;
        }
        closeConnection();
        return dataBaseContent;
    }
    
    public static void insertQuery(String expression) throws SQLException {
    	System.out.println("SQLexpression: " + expression);
    	openConnection();
        stmt.execute(expression);
        closeConnection();
    }
} 